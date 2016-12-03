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

import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.fs.Path;
import org.ojai.store.QueryCondition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mapr.fs.tables.IndexDesc;
import com.mapr.fs.tables.IndexFieldDesc;
import com.mapr.fs.proto.Common.FidMsg;
import com.mapr.db.MapRDB;
import com.mapr.db.impl.ConditionImpl;

import java.util.List;

public class JsonScanSpec {
	protected String tableName;
  protected IndexDesc indexDesc;
	protected QueryCondition condition;
	
	@JsonCreator
	public JsonScanSpec(@JsonProperty("tableName") String tableName,
                      @JsonProperty("indexDesc") IndexDesc indexDesc,
	                    @JsonProperty("condition") QueryCondition condition) {
	  this.tableName = tableName;
    this.indexDesc = indexDesc;
	  this.condition = condition;
  }

  public String getTableName() {
    return this.tableName;
  }

  public IndexDesc getIndexDesc() {
    return this.indexDesc;
  }

  public byte[] getStartRow() {
    if (condition == null) {
      return HConstants.EMPTY_START_ROW;
    }
    return ((ConditionImpl)this.condition).getRowkeyRanges().get(0).getStartRow();
  }

  public byte[] getStopRow() {
    if (condition == null) {
      return HConstants.EMPTY_END_ROW;
    }
    
    return ((ConditionImpl)this.condition).getRowkeyRanges().get(0).getStopRow();
  }

  public Object getSerializedFilter() {
    if (this.condition != null) {
      return ((ConditionImpl)this.condition).getDescriptor().getSerialized();
    }

    return null;
  }

  public void setCondition(QueryCondition condition) {
    this.condition = condition;
  }

  @JsonIgnore
  public QueryCondition getCondition() {
    return this.condition;
  }

  public boolean isSecondaryIndex() {
    return (this.indexDesc != null);
  }

  @JsonIgnore
  public Path getPrimaryTablePath() {
    return (this.indexDesc == null) ? null : new Path(this.indexDesc.getPrimaryTablePath());
  }

  @JsonIgnore
  public String getIndexName() {
    return (this.indexDesc == null) ? null : this.indexDesc.getIndexName();
  }

  @JsonIgnore
  public String getIndexFid() {
    return (this.indexDesc == null) ? null : this.indexDesc.getIndexFid();
  }

  @JsonIgnore
  public IndexFieldDesc[] getIndexedFields() {
    if (this.indexDesc == null) {
      return null;
    }

    List<IndexFieldDesc> indexedFields = indexDesc.getIndexedFields();
    int size = indexedFields.size();

    if (size == 0) {
      return null;
    }

    IndexFieldDesc[] a = new IndexFieldDesc[size];
    indexedFields.toArray(a);
    return a;
  }

  public void mergeScanSpec(String functionName, JsonScanSpec scanSpec) {

    if (this.condition != null && scanSpec.getCondition() != null) {
      QueryCondition newCond = MapRDB.newCondition();
      switch (functionName) {
      case "booleanAnd":
        newCond.and();
        break;
      case "booleanOr":
        newCond.or();
        break;
      default:
          assert(false);
      }

      newCond.condition(this.condition)
             .condition(scanSpec.getCondition())
             .close()
             .build();

      this.condition = newCond;
    } else if (scanSpec.getCondition() != null){
      this.condition = scanSpec.getCondition();
    }
  }
  
  @Override
  public String toString() {
    return "JsonScanSpec [tableName=" + tableName
        + ", condition=" + (condition == null ? null : condition.toString())
        + "]";
  }

}
