/**
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
 */
package org.apache.drill.exec.store.mapr.db.json;

import java.util.List;

import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.physical.AbstractRangePartitionFunction;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScan;
import org.apache.drill.exec.vector.ValueVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.mapr.db.Table;
import com.mapr.db.impl.IdCodec;
import com.mapr.db.impl.TabletInfoImpl;
import com.mapr.db.scan.ScanRange;

@JsonTypeName("jsontable-range-partition-function")
public class JsonTableRangePartitionFunction extends AbstractRangePartitionFunction {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonTableRangePartitionFunction.class);

  @JsonProperty("refList")
  protected List<FieldReference> refList;

  @JsonProperty("tableName")
  protected String tableName;

  @JsonProperty("subScan")
  protected MapRDBSubScan subScan = null;

  @JsonIgnore
  protected ValueVector partitionKeyVector = null;

  @JsonIgnore
  protected Table table = null;

  @JsonIgnore
  List<ScanRange> scanRanges = null;

  @JsonCreator
  public JsonTableRangePartitionFunction(
      @JsonProperty("refList") List<FieldReference> refList,
      @JsonProperty("tableName") String tableName) {
    this.refList = refList;
    this.tableName = tableName;
  }

  @JsonProperty("refList")
  @Override
  public List<FieldReference> getPartitionRefList() {
	  return refList;
	}

	@Override
  public void setup(List<VectorWrapper<?>> partitionKeys) {
    if (partitionKeys.size() != 1) {
      throw new UnsupportedOperationException(
          "Range partitioning function supports exactly one partition column; encountered " + partitionKeys.size());
    }

    VectorWrapper<?> v = partitionKeys.get(0);

    partitionKeyVector = v.getValueVector();

    assert subScan != null : "subScan has not yet been initialized";

    // initialize the table handle from the table cache
    table = subScan.getFormatPlugin().getJsonTableCache().getTable(tableName, subScan.getUserName());

    // Set the condition to null such that all scan ranges are retrieved for the primary table.
    // The reason is the row keys could typically belong to any one of the tablets of the table, so
    // there is no use trying to get only limited set of scan ranges (note that applying a non-null
    // condition to getScanRanges() also has a cost associated with it).
    scanRanges = table.getMetaTable().getScanRanges();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof JsonTableRangePartitionFunction) {
      JsonTableRangePartitionFunction rpf = (JsonTableRangePartitionFunction) obj;
      List<FieldReference> thisPartRefList = this.getPartitionRefList();
      List<FieldReference> otherPartRefList = rpf.getPartitionRefList();
      if (thisPartRefList.size() != otherPartRefList.size()) {
        return false;
      }
      for (int refIdx=0; refIdx<thisPartRefList.size(); refIdx++) {
        if (!thisPartRefList.get(refIdx).equals(otherPartRefList.get(refIdx))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int eval(int index, int numPartitions) {
	  assert partitionKeyVector != null;

	  String key = partitionKeyVector.getAccessor().getObject(index).toString();
	  byte[] encodedKey = IdCodec.encodeAsBytes(key);

    // Assign a default partition id of 0 for the case where the row key was not found
    // in any of the tablets; this could happen if the primary table row was deleted and we
    // have stale information. Such discrepancy will be handled by the join operator itself but
    // from a range partition function viewpoint, it should just put it in a default bucket.
    int tabletId = 0;
    TabletInfoImpl tablet = null;

    // TODO: currently this is doing a linear search through the tablet ranges; For N rows and M tablets,
    // this will be an O(NxM) operation. This search should ideally be delegated to MapR-DB once an
    // appropriate API is available to optimize this
    for (int i = 0; i < scanRanges.size(); i++) {
      tablet = (TabletInfoImpl) (scanRanges.get(i));
      if (tablet.containsRow(encodedKey)) {
         tabletId = i;
         break;
      }
    }

    logger.debug("Key = {}, tablet id = {}", key, tabletId);

    // for debugging only
    // if (tablet != null) {
    //   logger.debug("Tablet range: {}", tablet.getRowKeyRange().toString());
    // }

    return tabletId % numPartitions;
  }

  @Override
  public void setSubScan(SubScan subScan) {
    this.subScan = (MapRDBSubScan) subScan;
  }

}