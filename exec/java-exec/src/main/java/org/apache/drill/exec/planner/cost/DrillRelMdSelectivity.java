/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.drill.exec.planner.cost;

import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdSelectivity;
import org.apache.calcite.rel.metadata.RelMdUtil;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.drill.exec.physical.base.DbGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.physical.ScanPrel;

import java.util.List;

public class DrillRelMdSelectivity extends RelMdSelectivity {
  private static final DrillRelMdSelectivity INSTANCE = new DrillRelMdSelectivity();

  public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource(BuiltInMethod.SELECTIVITY.method, INSTANCE);

  @Override
  public Double getSelectivity(RelNode rel, RexNode predicate) {
    if (rel instanceof RelSubset) {
      return getSelectivity((RelSubset) rel, predicate);
    } else if (rel instanceof ScanPrel) {
      return getSelectivity((ScanPrel) rel, predicate);
    } else {
      return super.getSelectivity(rel, predicate);
    }
  }

  private Double getSelectivity(RelSubset rel, RexNode predicate) {
    if (rel.getBest() != null) {
      return getSelectivity(rel.getBest(), predicate);
    } else {
      List<RelNode> list = rel.getRelList();
      if (list != null && list.size() > 0) {
        return getSelectivity(list.get(0), predicate);
      }
    }
    return RelMdUtil.guessSelectivity(predicate);
  }

  private Double getSelectivity(ScanPrel rel, RexNode predicate) {
    double ROWCOUNT_UNKNOWN = -1;
    GroupScan scan = rel.getGroupScan();
    if (scan instanceof DbGroupScan) {
      double filterRows = ((DbGroupScan) scan).getRowCount(predicate, rel);
      double totalRows = ((DbGroupScan) scan).getRowCount(null, rel);
      if (filterRows != ROWCOUNT_UNKNOWN) {
        return filterRows/totalRows;
      }
    }
    return RelMdUtil.guessSelectivity(predicate);
  }
}
