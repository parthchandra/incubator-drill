/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.drill.exec.physical.impl.unnest;

import org.apache.drill.categories.OperatorTest;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.physical.rowSet.impl.TestResultSetLoaderMapArray;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TupleMetadata;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.store.mock.MockStorePOP;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSet;
import org.apache.drill.test.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.SchemaBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

@Category(OperatorTest.class) public class TestUnnestCorrectness extends SubOperatorTest {


  // Operator Context for mock batch
  public static OperatorContext operatorContext;

  // use MockLateralJoinPop for MockRecordBatch ??
  public static PhysicalOperator mockPopConfig;


  @BeforeClass public static void setUpBeforeClass() throws Exception {
    mockPopConfig = new MockStorePOP(null);
    operatorContext = fixture.newOperatorContext(mockPopConfig);
  }

  @AfterClass public static void tearDownAfterClass() throws Exception {
    operatorContext.close();
  }

  @Test
  public void testUnnestFixedWidthColumn() {

    Object[][] data = {
        { (Object) new int[] {1, 2},
          (Object) new int[] {3, 4, 5}},
        { (Object) new int[] {6, 7, 8, 9},
          (Object) new int[] {10, 11, 12, 13, 14}}
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("rowNumber", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    Integer[][] baseline = {{}, {1, 2}, {3, 4, 5}, {6, 7, 8, 9}, {10, 11, 12, 13, 14}};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestVarWidthColumn() {

    Object[][] data = {
        { (Object) new String[] {"", "zero"},
          (Object) new String[] {"one", "two", "three"}},
        { (Object) new String[] {"four", "five", "six", "seven"},
          (Object) new String[] {"eight", "nine", "ten", "eleven", "twelve"}}
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    String[][] baseline = {{}, {"", "zero"}, {"one", "two", "three"}, {"four", "five", "six", "seven"},
        {"eight", "nine", "ten", "eleven", "twelve"}};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestMapColumn() {

    Object[][] data = getMapData();

    // Create input schema
    TupleMetadata incomingSchema = getRepeatedMapSchema();

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    Object[][] baseline = getMapBaseline();

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestEmptyList() {

    Object[][] data = {
        { (Object) new String[] {},
          (Object) new String[] {}
        },
        { (Object) new String[] {},
          (Object) new String[] {}
        }
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    // All subsequent batches are also empty
    String[][] baseline = {{}, {}, {}, {}, {}};

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK_NEW_SCHEMA, RecordBatch.IterOutcome.OK};

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestMultipleNewSchemaIncoming() {

    // Schema changes in incoming have no effect on unnest unless the type of the
    // unnest column itself has changed
    Object[][] data = {
        {
            (Object) new String[] {"0", "1"},
            (Object) new String[] {"2", "3", "4"}
        },
        {
            (Object) new String[] {"5", "6" },
        },
        {
            (Object) new String[] {"9"}
        }
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    String[][] baseline = {{}, {"0", "1"}, {"2", "3", "4"}, {"5", "6" }, {"9"} };

    RecordBatch.IterOutcome[] iterOutcomes = {
        RecordBatch.IterOutcome.OK_NEW_SCHEMA,
        RecordBatch.IterOutcome.OK,
        RecordBatch.IterOutcome.OK_NEW_SCHEMA};

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Ignore
  @Test
  public void testUnnestSchemaChange() {
    Object[][] data = {
        {
            (Object) new String[] {"0", "1"},
            (Object) new String[] {"2", "3", "4"}
        },
        {
            (Object) new String[] {"5", "6" },
        },
        {
            (Object) new int[] {9}
        }
    };

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("someColumn", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.VARCHAR).buildSchema();

    // First batch in baseline is an empty batch corresponding to OK_NEW_SCHEMA
    // Another empty batch introduced by the schema change in the last batch
    Object[][] baseline = {{}, {"0", "1"}, {"2", "3", "4"}, {"5", "6" }, {}, {9} };

    RecordBatch.IterOutcome[] iterOutcomes = {
        RecordBatch.IterOutcome.OK_NEW_SCHEMA,
        RecordBatch.IterOutcome.OK,
        RecordBatch.IterOutcome.OK_NEW_SCHEMA};

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    }

  }

  @Test
  public void testUnnestLimitBatchSize() {

    final int limitedOutputBatchSize = 1024;
    final int limitedOutputBatchSizeBytes = 1024*4*1; // num rows * size of int
    final int inputBatchSize = 1024+1;
    // single record batch with single row. The unnest column has one
    // more record than the batch size we want in the output
    Object[][] data = new Object[1][1];

    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[i].length; j++) {
        data[i][j] = new int[inputBatchSize];
        for (int k =0; k < inputBatchSize; k++) {
          ((int[])data[i][j])[k] = k;
        }
      }
    }
    Integer[][] baseline = new Integer[3][];
    baseline[0] = new Integer[] {};
    baseline[1] = new Integer[limitedOutputBatchSize];
    baseline[2] = new Integer[1];
    for (int i = 0; i < limitedOutputBatchSize; i++) {
      baseline[1][i] = i;
    }
    baseline[2][0] = limitedOutputBatchSize;

    // Create input schema
    TupleMetadata incomingSchema = new SchemaBuilder()
        .add("rowNumber", TypeProtos.MinorType.INT)
        .addArray("unnestColumn", TypeProtos.MinorType.INT).buildSchema();

    RecordBatch.IterOutcome[] iterOutcomes = {RecordBatch.IterOutcome.OK};

    final long outputBatchSize = fixture.getFragmentContext().getOptions().getOption(ExecConstants
        .OUTPUT_BATCH_SIZE_VALIDATOR);
    fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, limitedOutputBatchSizeBytes);

    try {
      testUnnestSingleSchema(incomingSchema, iterOutcomes, data, baseline);
    } catch (Exception e) {
      fail("Failed due to exception: " + e.getMessage());
    } finally {
      fixture.getFragmentContext().getOptions().setLocalOption(ExecConstants.OUTPUT_BATCH_SIZE, outputBatchSize);
    }

  }

  private <T> void testUnnestSingleSchema( TupleMetadata incomingSchema,
      RecordBatch.IterOutcome[] iterOutcomes,
      T[][] data,
      T[][] baseline) throws Exception {

    // Get the incoming container with dummy data for LJ
    final List<VectorContainer> incomingContainer = new ArrayList<>(data.length);

    // Create data
    ArrayList<RowSet.SingleRowSet> rowSets = new ArrayList<>();

    for ( Object[] recordBatch : data) {
      RowSetBuilder rowSetBuilder = fixture.rowSetBuilder(incomingSchema);
      int rowNumber = 0;
      for ( Object rowData : recordBatch) {
        rowSetBuilder.addRow(++rowNumber, rowData);
      }
      RowSet.SingleRowSet rowSet = rowSetBuilder.build();
      rowSets.add(rowSet);
      incomingContainer.add(rowSet.container());
    }

    // Get the unnest POPConfig
    final UnnestPOP unnestPopConfig = new UnnestPOP(null, new SchemaPath(new PathSegment.NameSegment("unnestColumn")));

    // Get the IterOutcomes for LJ
    final List<RecordBatch.IterOutcome> outcomes = new ArrayList<>(iterOutcomes.length);
    for(RecordBatch.IterOutcome o : iterOutcomes) {
      outcomes.add(o);
    }

    // Create incoming MockRecordBatch
    final MockRecordBatch incomingMockBatch =
        new MockRecordBatch(fixture.getFragmentContext(), operatorContext, incomingContainer, outcomes,
            incomingContainer.get(0).getSchema());

    final MockLateralJoinBatch lateralJoinBatch =
        new MockLateralJoinBatch(fixture.getFragmentContext(), operatorContext, incomingMockBatch);

    // set pointer to Lateral in unnest pop config
    unnestPopConfig.setLateral(lateralJoinBatch);

    // setup Unnest record batch
    final UnnestRecordBatch unnestBatch =
        new UnnestRecordBatch(unnestPopConfig, incomingMockBatch, fixture.getFragmentContext());

    // set backpointer to lateral join in unnest
    lateralJoinBatch.setUnnest(unnestBatch);

    // Simulate the pipeline by calling next on the incoming
    ExpandableHyperContainer results = null;

    try {

      while (!isTerminal(lateralJoinBatch.next())) {
        // nothing to do
      }

      // Check results against baseline
      results = lateralJoinBatch.getResults();

      int i = 0;
      for (VectorWrapper vw : results) {
        for (ValueVector vv : vw.getValueVectors()) {
          int valueCount = vv.getAccessor().getValueCount();
          if (valueCount != baseline[i].length) {
            fail("Test failed in validating unnest output. Value count mismatch.");
          }
          for (int j = 0; j < valueCount; j++) {

            if (vv instanceof MapVector) {
              if (!compareMapBaseline((Object) baseline[i][j], vv.getAccessor().getObject(j))) {
                fail("Test failed in validating unnest(Map) output. Value mismatch");
              }
            } else  if (vv instanceof VarCharVector) {
              Object val = vv.getAccessor().getObject(j);
              if (((String) baseline[i][j]).compareTo(val.toString()) != 0) {
                fail("Test failed in validating unnest output. Value mismatch. Baseline value[]" + i + "][" + j + "]"
                    + ": " + baseline[i][j] + "   VV.getObject(j): " + val);
              }
            } else {
              Object val = vv.getAccessor().getObject(j);
              if (!baseline[i][j].equals(val)) {
                fail("Test failed in validating unnest output. Value mismatch. Baseline value["+i+"]["+j+"]"+": "+
                    baseline[i][j] + "   VV.getObject(j): " + val);
              }
            }
          }
          i++;

        }
      }

      assertTrue(((MockLateralJoinBatch) lateralJoinBatch).isCompleted());

    } catch (Exception e) {
      fail("Test failed in validating unnest output. Exception : " + e.getMessage());
    } finally {
      // Close all the resources for this test case
      unnestBatch.close();
      lateralJoinBatch.close();
      incomingMockBatch.close();

      if (results != null) {
        results.clear();
      }
      for(RowSet.SingleRowSet rowSet: rowSets) {
        rowSet.clear();
      }
    }

  }

  /**
   * Build a schema with a repeated map -
   *
   *  {
   *    rowNum,
   *    mapColumn : [
   *       {
   *         colA,
   *         colB : [
   *            varcharCol
   *         ]
   *       }
   *    ]
   *  }
   *
   * @see TestResultSetLoaderMapArray TestResultSetLoaderMapArray for similar schema and data
   * @return TupleMetadata corresponding to the schema
   */
  private TupleMetadata getRepeatedMapSchema() {
    TupleMetadata schema = new SchemaBuilder()
        .add("rowNum", TypeProtos.MinorType.INT)
        .addMapArray("unnestColumn")
        .add("colA", TypeProtos.MinorType.INT)
        .addArray("colB", TypeProtos.MinorType.VARCHAR)
        .buildMap()
        .buildSchema();
    return schema;
  }

  private Object[][] getMapData( ) {

    Object[][] d = {
      {
          new Object[] {},
          new Object[] {
              new Object[] {11, new String[] {"1.1.1", "1.1.2" }},
              new Object[] {12, new String[] {"1.2.1", "1.2.2" }}
          },

          new Object[] {
              new Object[] {21, new String[] {"2.1.1", "2.1.2" }},
              new Object[] {22, new String[] {}},
              new Object[] {23, new String[] {"2.3.1", "2.3.2" }}
          }
      },
      {
        new Object[] {
            new Object[] {31, new String[] {"3.1.1", "3.1.2" }},
            new Object[] {32, new String[] {"3.2.1", "3.2.2" }}
        }
      }
    };

    return d;
  }

  private Object[][] getMapBaseline() {

    Object[][] d = {
        new Object[] {},    // Empty record batch returned by OK_NEW_SCHEMA
        new Object[] {},    // First incoming batch is empty
        new Object[] {
            "{\"colA\":11,\"colB\":[\"1.1.1\",\"1.1.2\"]}",
            "{\"colA\":12,\"colB\":[\"1.2.1\",\"1.2.2\"]}"
        },
        new Object[] {
            "{\"colA\":21,\"colB\":[\"2.1.1\",\"2.1.2\"]}",
            "{\"colA\":22,\"colB\":[]}",
            "{\"colA\":23,\"colB\":[\"2.3.1\",\"2.3.2\"]}"
        },
        new Object[] {
            "{\"colA\":31,\"colB\":[\"3.1.1\",\"3.1.2\"]}",
            "{\"colA\":32,\"colB\":[\"3.2.1\",\"3.2.2\"]}"
        }
    };
    return d;
  }

  private boolean compareMapBaseline(Object baselineValue, Object vector) {
    String vv = vector.toString();
    String b = (String)baselineValue;
    return vv.equalsIgnoreCase(b);
  }

  private boolean isTerminal(RecordBatch.IterOutcome outcome) {
    return (outcome == RecordBatch.IterOutcome.NONE || outcome == RecordBatch.IterOutcome.STOP) || (outcome
        == RecordBatch.IterOutcome.OUT_OF_MEMORY);
  }

}

