/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.planner.index;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.Pair;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.IndexGroupScan;
import org.apache.drill.exec.physical.impl.join.JoinUtils.JoinControl;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait;
import org.apache.drill.exec.planner.physical.FilterPrel;
import org.apache.drill.exec.planner.physical.HashJoinPrel;
import org.apache.drill.exec.planner.physical.JoinPruleBase;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.physical.Prule;
import org.apache.drill.exec.planner.physical.ScanPrel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IndexScanIntersectGenerator is to generate index plan against multiple index tables,
 * the input indexes are assumed to be ranked by selectivity(low to high) already.
 */
public class IndexIntersectPlanGenerator extends AbstractIndexPlanGenerator {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IndexIntersectPlanGenerator.class);

  final Map<IndexDescriptor, IndexConditionInfo> indexInfoMap;

  public IndexIntersectPlanGenerator(RelOptRuleCall call,
                                     ProjectPrel origProject,
                                     ScanPrel origScan,
                                     Map<IndexDescriptor, IndexConditionInfo> indexInfoMap,
                                     RexBuilder builder) {
    super(call, origProject, origScan, null, null, builder);
    this.indexInfoMap = indexInfoMap;
  }

  public RelNode buildRowKeyJoin(RelNode left, RelNode right, boolean isRowKeyJoin, int htControl)
      throws InvalidRelException {
    final int leftRowKeyIdx = getRowKeyIndex(left.getRowType(), origScan);
    final int rightRowKeyIdx = 0; // only rowkey field is being projected from right side

    assert leftRowKeyIdx >= 0;

    List<Integer> leftJoinKeys = ImmutableList.of(leftRowKeyIdx);
    List<Integer> rightJoinKeys = ImmutableList.of(rightRowKeyIdx);

    logger.trace(String.format(
        "buildRowKeyJoin: leftIdx: %d, rightIdx: %d",
        leftRowKeyIdx, rightRowKeyIdx));
    RexNode joinCondition =
        RelOptUtil.createEquiJoinCondition(left, leftJoinKeys,
            right, rightJoinKeys, builder);

    HashJoinPrel hjPrel;
    hjPrel = new HashJoinPrel(origScan.getCluster(), origScan.getTraitSet(), left,
        right, joinCondition, JoinRelType.INNER, false, isRowKeyJoin, htControl);

    if (isRowKeyJoin == true) {
      // sine there is a restricted Scan on left side, assume original project
      return buildOriginalProject(hjPrel);
    } else {
      //there is no restricted scan on left, do a regular rowkey join
      return buildRowKeyProject(hjPrel, leftRowKeyIdx);
    }
  }

  public RelNode buildRowKeyProject(RelNode inputRel, int fieldIndex) {
    List<RelDataTypeField> inputFields = inputRel.getRowType().getFieldList();
    final RelDataTypeField rowKeyField = inputFields.get(fieldIndex);
      RexNode expr = builder.makeInputRef(rowKeyField.getType(), rowKeyField.getIndex());
    List<RexNode> exprs = Lists.newArrayList();
    exprs.add(expr);

    final RelDataTypeFactory.FieldInfoBuilder rightFieldTypeBuilder =
        inputRel.getCluster().getTypeFactory().builder();

    rightFieldTypeBuilder.add(rowKeyField);
    final RelDataType projectRowType = rightFieldTypeBuilder.build();

    ProjectPrel proj = new ProjectPrel(inputRel.getCluster(), inputRel.getTraitSet(), inputRel, exprs, projectRowType);

    return proj;
  }

  public RelNode buildOriginalProject (RelNode newRel) {
    RelDataType origRowType = origProject == null ? origScan.getRowType() : origProject.getRowType();

    final RelDataTypeFactory.FieldInfoBuilder finalFieldTypeBuilder =
        origScan.getCluster().getTypeFactory().builder();

    List<RelDataTypeField> hjRowFields = newRel.getRowType().getFieldList();
    int toRemoveRowKeyCount = 1;
    if (getRowKeyIndex(origRowType, origScan)  < 0 ) {
      toRemoveRowKeyCount = 2;
    }
    finalFieldTypeBuilder.addAll(hjRowFields.subList(0, hjRowFields.size()-toRemoveRowKeyCount));
    final RelDataType finalProjectRowType = finalFieldTypeBuilder.build();

    List<RexNode> resetExprs = Lists.newArrayList();
    for (int idx=0; idx<hjRowFields.size()-toRemoveRowKeyCount; ++idx) {
      resetExprs.add(RexInputRef.of(idx, newRel.getRowType()));
    }

    final ProjectPrel resetProjectPrel = new ProjectPrel(newRel.getCluster(), newRel.getTraitSet(),
        newRel, resetExprs, finalProjectRowType);
    newRel = resetProjectPrel;

    RelNode finalRel = Prule.convert(newRel, newRel.getTraitSet());
    return finalRel;
  }

  private FunctionalIndexInfo getFunctionalIndexInfo(IndexDescriptor index) {
    return index.getFunctionalInfo();
  }

  public RelNode buildIntersectPlan(Map.Entry<IndexDescriptor, RexNode> pair, RelNode right)
      throws InvalidRelException {
    IndexDescriptor index = pair.getKey();
    RexNode condition = pair.getValue();

    FunctionalIndexInfo functionInfo = getFunctionalIndexInfo(index);
    IndexGroupScan indexScan = index.getIndexGroupScan();
    RelDataType indexScanRowType = convertRowTypeForIndexScan(origScan, condition, indexScan, functionInfo);
    ScanPrel indexScanPrel = new ScanPrel(origScan.getCluster(),
        origScan.getTraitSet().plus(Prel.DRILL_PHYSICAL), indexScan, indexScanRowType);
    FilterPrel  indexFilterPrel = new FilterPrel(indexScanPrel.getCluster(), indexScanPrel.getTraitSet(),
        indexScanPrel, convertConditionForIndexScan(condition, indexScanRowType, functionInfo));
    // project the rowkey column from the index scan
    List<RexNode> indexProjectExprs = Lists.newArrayList();
    int rowKeyIndex = getRowKeyIndex(indexScanPrel.getRowType(), origScan);//indexGroupScan.getRowKeyOrdinal();
    assert rowKeyIndex >= 0;

    indexProjectExprs.add(RexInputRef.of(rowKeyIndex, indexScanPrel.getRowType()));

    final RelDataTypeFactory.FieldInfoBuilder rightFieldTypeBuilder =
        indexScanPrel.getCluster().getTypeFactory().builder();

    // build the row type for the right Project
    final List<RelDataTypeField> indexScanFields = indexScanPrel.getRowType().getFieldList();

    final RelDataTypeField rightRowKeyField = indexScanFields.get(rowKeyIndex);
    rightFieldTypeBuilder.add(rightRowKeyField);
    final RelDataType indexProjectRowType = rightFieldTypeBuilder.build();

    final ProjectPrel indexProjectPrel = new ProjectPrel(indexScanPrel.getCluster(), indexScanPrel.getTraitSet(),
        indexFilterPrel, indexProjectExprs, indexProjectRowType);

    RelTraitSet indexTraits = newTraitSet().plus(Prel.DRILL_PHYSICAL);

    //if build(right) side does not exist, this index scan is the right most.
    if(right == null) {
      final DrillDistributionTrait distRight =
          new DrillDistributionTrait(DrillDistributionTrait.DistributionType.BROADCAST_DISTRIBUTED);
      indexTraits = newTraitSet(distRight).plus(Prel.DRILL_PHYSICAL);
    }

    RelNode converted = Prule.convert(indexProjectPrel, indexTraits);

    if (right == null) {
      return converted;
    }

    //if build(right) side exist, the plan we got in 'converted' is left (probe). Intersect with right(build) side
    RelNode finalRel = buildRowKeyJoin(converted, right, false, JoinControl.INTERSECT_DISTINCT);

    logger.trace("IndexIntersectPlanGenerator got finalRel {} from origScan {}",
        finalRel.toString(), origScan.toString());
    return finalRel;
  }

  private Pair<RelNode, DbGroupScan> buildRestrictedDBScan(RexNode remnant) {

    DbGroupScan origDbGroupScan = (DbGroupScan)origScan.getGroupScan();
    List<SchemaPath> cols = new ArrayList<SchemaPath>(origDbGroupScan.getColumns());
    if (!checkRowKey(cols)) {
      cols.add(origDbGroupScan.getRowKeyPath());
    }

    // Create a restricted groupscan from the primary table's groupscan
    DbGroupScan restrictedGroupScan  = origDbGroupScan.getRestrictedScan(cols);
    if (restrictedGroupScan == null) {
      logger.error("Null restricted groupscan in IndexIntersectPlanGenerator.convertChild");
      return null;
    }

    RelNode lastRelNode;
    RelDataType dbscanRowType = convertRowType(origScan.getRowType(), origScan.getCluster().getTypeFactory());
    ScanPrel dbScan = new ScanPrel(origScan.getCluster(),
        origScan.getTraitSet(), restrictedGroupScan, dbscanRowType);
    lastRelNode = dbScan;
    // build the row type for the left Project
    List<RexNode> leftProjectExprs = Lists.newArrayList();
    int leftRowKeyIndex = getRowKeyIndex(dbScan.getRowType(), origScan);
    final RelDataTypeField leftRowKeyField = dbScan.getRowType().getFieldList().get(leftRowKeyIndex);
    final RelDataTypeFactory.FieldInfoBuilder leftFieldTypeBuilder =
        dbScan.getCluster().getTypeFactory().builder();

    FilterPrel leftIndexFilterPrel = null;
    if(remnant != null && !remnant.isAlwaysTrue()) {
      leftIndexFilterPrel = new FilterPrel(dbScan.getCluster(), dbScan.getTraitSet(),
          dbScan, remnant);
      lastRelNode = leftIndexFilterPrel;
    }

    // new Project's rowtype is original Project's rowtype [plus rowkey if rowkey is not in original rowtype]
    ProjectPrel leftIndexProjectPrel = null;
    if (origProject != null) {
      RelDataType origRowType = origProject.getRowType();
      List<RelDataTypeField> origProjFields = origRowType.getFieldList();
      leftFieldTypeBuilder.addAll(origProjFields);
      // get the exprs from the original Project IFF there is a project

      leftProjectExprs.addAll(origProject.getProjects());
      // add the rowkey IFF rowkey is not in orig scan
      if (getRowKeyIndex(origRowType, origScan) < 0) {
        leftFieldTypeBuilder.add(leftRowKeyField);
        leftProjectExprs.add(RexInputRef.of(leftRowKeyIndex, dbScan.getRowType()));
      }

      final RelDataType leftProjectRowType = leftFieldTypeBuilder.build();
      leftIndexProjectPrel = new ProjectPrel(dbScan.getCluster(), dbScan.getTraitSet(),
          leftIndexFilterPrel == null ? dbScan : leftIndexFilterPrel, leftProjectExprs, leftProjectRowType);
      lastRelNode = leftIndexProjectPrel;
    }

    final RelTraitSet leftTraits = dbScan.getTraitSet().plus(Prel.DRILL_PHYSICAL);
    // final RelNode convertedLeft = convert(leftIndexProjectPrel, leftTraits);
    final RelNode convertedLeft = Prule.convert(lastRelNode, leftTraits);

    return new Pair<>(convertedLeft, restrictedGroupScan);
  }

  @Override
  public RelNode convertChild(final FilterPrel filter, final RelNode input) throws InvalidRelException {
    Map<IndexDescriptor, RexNode> idxConditionMap = Maps.newLinkedHashMap();
    for(IndexDescriptor idx : indexInfoMap.keySet()) {
      idxConditionMap.put(idx, indexInfoMap.get(idx).indexCondition);
    }

    RelNode indexPlan = null;
    RexNode remnant = filter.getCondition();
    for (Map.Entry<IndexDescriptor, RexNode> pair : idxConditionMap.entrySet()) {
      indexPlan = buildIntersectPlan(pair, indexPlan);
      remnant = indexInfoMap.get(pair.getKey()).remainderCondition;
    }

    final RelDataTypeField rightRowKeyField = indexPlan.getRowType().getFieldList().get(0);
    final RelNode rangeDistRight = createRangeDistRight(indexPlan, rightRowKeyField, (DbGroupScan)origScan.getGroupScan());

    //now with index plan constructed, build plan of left(probe) side to use restricted db scan

    Pair<RelNode, DbGroupScan> leftRelAndScan = buildRestrictedDBScan(remnant);

    RelNode finalRel = buildRowKeyJoin(leftRelAndScan.left, rangeDistRight, true, JoinControl.DEFAULT);

    return finalRel;
  }

}
