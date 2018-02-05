/*
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
package org.apache.drill.exec.physical.impl.join;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JVar;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.sig.GeneratorMapping;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.BatchReference;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.LateralContract;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.physical.impl.filter.ReturnValueExpression;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.record.AbstractBinaryRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.ExpandableHyperContainer;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractContainerVector;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

/*
 * RecordBatch implementation for the lateral join operator
 */
public class LateralJoinBatch extends AbstractBinaryRecordBatch<LateralJoinPOP> implements LateralContract {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LateralJoinBatch.class);

  // Maximum number records in the outgoing batch
  protected static final int MAX_BATCH_SIZE = 4096;

  // Input indexes to correctly update the stats
  protected static final int LEFT_INPUT = 0;
  protected static final int RIGHT_INPUT = 1;

  // Schema on the left side
  private BatchSchema leftSchema = null;

  // Schema on the right side
  private BatchSchema rightSchema = null;

  // Runtime generated class implementing the LateralJoin interface
  private LateralJoin lateralJoiner = null;

  // Number of output records in the current outgoing batch
  private int outputRecords = 0;

  // Current index of record in left incoming which is being processed
  private int joinIndex = -1;

  // We accumulate all the batches on the right side in a hyper container.
  private ExpandableHyperContainer rightContainer = new ExpandableHyperContainer();

  // Record count of the individual batches in the right hyper container
  private LinkedList<Integer> rightCounts = new LinkedList<>();

  // Generator mapping for the right side
  private static final GeneratorMapping EMIT_RIGHT =
    GeneratorMapping.create("doSetup"/* setup method */,"emitRight" /* eval method */,
      null /* reset */,null /* cleanup */);

  // Generator mapping for the right side : constant
  private static final GeneratorMapping EMIT_RIGHT_CONSTANT =
    GeneratorMapping.create("doSetup"/* setup method */,"doSetup" /* eval method */,
      null /* reset */, null /* cleanup */);

  // Generator mapping for the left side : scalar
  private static final GeneratorMapping EMIT_LEFT =
    GeneratorMapping.create("doSetup" /* setup method */, "emitLeft" /* eval method */,
      null /* reset */, null /* cleanup */);

  // Generator mapping for the left side : constant
  private static final GeneratorMapping EMIT_LEFT_CONSTANT =
    GeneratorMapping.create("doSetup" /* setup method */,"doSetup" /* eval method */,
      null /* reset */, null /* cleanup */);

  // Mapping set for the right side
  private static final MappingSet emitRightMapping =
    new MappingSet("rightCompositeIndex" /* read index */, "outIndex" /* write index */,
      "rightContainer" /* read container */,"outgoing" /* write container */,
      EMIT_RIGHT_CONSTANT, EMIT_RIGHT);

  // Mapping set for the left side
  private static final MappingSet emitLeftMapping =
    new MappingSet("leftIndex" /* read index */, "outIndex" /* write index */,
      "leftBatch" /* read container */,"outgoing" /* write container */,
      EMIT_LEFT_CONSTANT, EMIT_LEFT);

  protected LateralJoinBatch(LateralJoinPOP popConfig, FragmentContext context,
                             RecordBatch left, RecordBatch right) throws OutOfMemoryException {
    super(popConfig, context, left, right);
    Preconditions.checkNotNull(left);
    Preconditions.checkNotNull(right);
  }

  /**
   * Method that drains all the right side input until it see's EMIT outcome and accumulates
   * the data in a hyper container. Once it has all the data from right side, it processes
   * left side one row at a time and produce output batch.
   * @return IterOutcome state of the lateral join batch
   */
  @Override
  public IterOutcome innerNext() {

    // Accumulate batches on the right in a hyper container
    if (state == BatchState.FIRST) {

      // exit if we have an empty left batch
      if (leftUpstream == IterOutcome.NONE) {
        // inform upstream that we don't need anymore data and make sure we clean up any batches already in queue
        killAndDrainRight();
        return IterOutcome.NONE;
      }

      boolean drainRight = rightUpstream != IterOutcome.NONE ? true : false;
      while (drainRight) {
        rightUpstream = next(RIGHT_INPUT, right);
        switch (rightUpstream) {
          case OK_NEW_SCHEMA:
            if (!right.getSchema().equals(rightSchema)) {
              throw new DrillRuntimeException("Lateral join does not handle schema change. Schema change" +
                  " found on the right side of Lateral Join.");
            }
            // fall through
          case OK:
            addBatchToHyperContainer(right);
            break;
          case OUT_OF_MEMORY:
            return IterOutcome.OUT_OF_MEMORY;
          case NONE:
          case STOP:
            //TODO we got a STOP, shouldn't we stop immediately ?
          case NOT_YET:
            drainRight = false;
            break;
        }
      }
      lateralJoiner.setupLateralJoin(context, left, rightContainer, rightCounts, this);
      state = BatchState.NOT_FIRST;
    }

    // allocate space for the outgoing batch
    allocateVectors();

    // invoke the runtime generated method to emit records in the output batch
    outputRecords = lateralJoiner.outputRecords(popConfig.getJoinType());

    // Set the record count
    for (final VectorWrapper<?> vw : container) {
      vw.getValueVector().getMutator().setValueCount(outputRecords);
    }

    // Set the record count in the container
    container.setRecordCount(outputRecords);
    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);

    logger.debug("Number of records emitted: " + outputRecords);

    return (outputRecords > 0) ? IterOutcome.OK : IterOutcome.NONE;
  }

  private void killAndDrainRight() {
    if (!hasMore(rightUpstream)) {
      return;
    }
    right.kill(true);
    while (hasMore(rightUpstream)) {
      for (final VectorWrapper<?> wrapper : right) {
        wrapper.getValueVector().clear();
      }
      rightUpstream = next(HashJoinHelper.RIGHT_INPUT, right);
    }
  }

  private boolean hasMore(IterOutcome outcome) {
    return outcome == IterOutcome.OK || outcome == IterOutcome.OK_NEW_SCHEMA;
  }

  /**
   * Method generates the runtime code needed for LateralJoin. Other than the setup method to set the input and output
   * value vector references we implement three more methods
   * 1. doEval() -> Evaluates if record from left side matches record from the right side
   * 2. emitLeft() -> Project record from the left side
   * 3. emitRight() -> Project record from the right side (which is a hyper container)
   * @return the runtime generated class that implements the LateralJoin interface
   */
  private LateralJoin setupWorker() throws IOException, ClassTransformationException, SchemaChangeException {
    final CodeGenerator<LateralJoin> nLJCodeGenerator = CodeGenerator.get(
      LateralJoin.TEMPLATE_DEFINITION, context.getOptions());
    nLJCodeGenerator.plainJavaCapable(true);
    // Uncomment out this line to debug the generated code. Make this configurable
    // nLJCodeGenerator.saveCodeForDebugging(true);
    final ClassGenerator<LateralJoin> nLJClassGenerator = nLJCodeGenerator.getRoot();

    // generate doEval
    final ErrorCollector collector = new ErrorCollectorImpl();

    /*
        Logical expression may contain fields from left and right batches. During code generation (materialization)
        we need to indicate from which input field should be taken.

        Non-equality joins can belong to one of below categories. For example:
        1. Join on non-equality join predicates:
        select * from t1 inner join t2 on (t1.c1 between t2.c1 AND t2.c2) AND (...)
        2. Join with an OR predicate:
        select * from t1 inner join t2 on on t1.c1 = t2.c1 OR t1.c2 = t2.c2
     */
    Map<VectorAccessible, BatchReference> batches = ImmutableMap
        .<VectorAccessible, BatchReference>builder()
        .put(left, new BatchReference("leftBatch", "leftIndex"))
        .put(rightContainer,
          new BatchReference("rightContainer", "rightBatchIndex",
            "rightRecordIndexWithinBatch"))
        .build();

    LogicalExpression materialize = ExpressionTreeMaterializer.materialize(
        popConfig.getCondition(),
        batches,
        collector,
        context.getFunctionRegistry(),
        false,
        false);

    if (collector.hasErrors()) {
      throw new SchemaChangeException(String.format("Failure while trying to materialize join condition. Errors:\n %s.",
          collector.toErrorString()));
    }

    nLJClassGenerator.addExpr(new ReturnValueExpression(materialize), ClassGenerator.BlkCreateMode.FALSE);

    // generate emitLeft
    nLJClassGenerator.setMappingSet(emitLeftMapping);
    JExpression outIndex = JExpr.direct("outIndex");
    JExpression leftIndex = JExpr.direct("leftIndex");

    int fieldId = 0;
    int outputFieldId = 0;
    if (leftSchema != null) {
      // Set the input and output value vector references corresponding to the left batch
      for (MaterializedField field : leftSchema) {
        final TypeProtos.MajorType fieldType = field.getType();

        // Add the vector to the output container
        container.addOrGet(field);

        JVar inVV = nLJClassGenerator.declareVectorValueSetupAndMember("leftBatch",
            new TypedFieldId(fieldType, false, fieldId));
        JVar outVV = nLJClassGenerator.declareVectorValueSetupAndMember("outgoing",
            new TypedFieldId(fieldType, false, outputFieldId));

        nLJClassGenerator.getEvalBlock().add(outVV.invoke("copyFromSafe").arg(leftIndex).arg(outIndex).arg(inVV));
        nLJClassGenerator.rotateBlock();
        fieldId++;
        outputFieldId++;
      }
    }

    // generate emitRight
    fieldId = 0;
    nLJClassGenerator.setMappingSet(emitRightMapping);
    JExpression batchIndex = JExpr.direct("batchIndex");
    JExpression recordIndexWithinBatch = JExpr.direct("recordIndexWithinBatch");

    if (rightSchema != null) {
      // Set the input and output value vector references corresponding to the right batch
      for (MaterializedField field : rightSchema) {

        final TypeProtos.MajorType inputType = field.getType();
        TypeProtos.MajorType outputType;
        // if join type is LEFT, make sure right batch output fields data mode is optional
        if (popConfig.getJoinType() == JoinRelType.LEFT && inputType.getMode() == TypeProtos.DataMode.REQUIRED) {
          outputType = Types.overrideMode(inputType, TypeProtos.DataMode.OPTIONAL);
        } else {
          outputType = inputType;
        }

        MaterializedField newField = MaterializedField.create(field.getName(), outputType);
        container.addOrGet(newField);

        JVar inVV = nLJClassGenerator.declareVectorValueSetupAndMember("rightContainer",
            new TypedFieldId(inputType, true, fieldId));
        JVar outVV = nLJClassGenerator.declareVectorValueSetupAndMember("outgoing",
            new TypedFieldId(outputType, false, outputFieldId));
        nLJClassGenerator.getEvalBlock().add(outVV.invoke("copyFromSafe")
            .arg(recordIndexWithinBatch)
            .arg(outIndex)
            .arg(inVV.component(batchIndex)));
        nLJClassGenerator.rotateBlock();
        fieldId++;
        outputFieldId++;
      }
    }

    return context.getImplementationClass(nLJCodeGenerator);
  }

  /**
   * Simple method to allocate space for all the vectors in the container.
   */
  private void allocateVectors() {
    for (final VectorWrapper<?> vw : container) {
      AllocationHelper.allocateNew(vw.getValueVector(), MAX_BATCH_SIZE);
    }
  }

  /**
   * Builds the output container's schema. Goes over the left and the right
   * batch and adds the corresponding vectors to the output container.
   * @throws SchemaChangeException if batch schema was changed during execution
   */
  @Override
  protected void buildSchema() throws SchemaChangeException {
    try {
      if (! prefetchFirstBatchFromBothSides()) {
        return;
      }

      // Get the left schema and set it in container
      if (leftUpstream != IterOutcome.NONE) {
        leftSchema = left.getSchema();
        for (final VectorWrapper<?> vectorWrapper : left) {
          container.addOrGet(vectorWrapper.getField());
        }
      }

      // Get the right schema and set it in container
      if (rightUpstream != IterOutcome.NONE) {
        for (final VectorWrapper<?> vectorWrapper : right) {
          MaterializedField rightField = vectorWrapper.getField();
          TypeProtos.MajorType rightFieldType = vectorWrapper.getField().getType();

          // make right input schema optional if we have LEFT join
          if (popConfig.getJoinType() == JoinRelType.LEFT
            && rightFieldType.getMode() == TypeProtos.DataMode.REQUIRED) {
            final TypeProtos.MajorType outputType =
              Types.overrideMode(rightField.getType(), TypeProtos.DataMode.OPTIONAL);

            // Create the right field with optional type. This will also take care of creating
            // children fields in case of ValueVectors of map type
            rightField = rightField.withType(outputType);
          }
          container.addOrGet(rightField);
        }
        rightSchema = right.getSchema();
        addBatchToHyperContainer(right);
      }

      // Allocate memory for all the value vectors inside output batch
      allocateVectors();
      lateralJoiner = setupWorker();

      // if left batch is empty, fetch next
      if (leftUpstream != IterOutcome.NONE && left.getRecordCount() == 0) {
        leftUpstream = next(LEFT_INPUT, left);
      }

      container.setRecordCount(0);
      container.buildSchema(BatchSchema.SelectionVectorMode.NONE);

    } catch (ClassTransformationException | IOException e) {
      throw new SchemaChangeException(e);
    }
  }

  private void addBatchToHyperContainer(RecordBatch inputBatch) {
    final RecordBatchData batchCopy = new RecordBatchData(inputBatch, oContext.getAllocator());
    boolean success = false;
    try {
      rightCounts.addLast(inputBatch.getRecordCount());
      rightContainer.addBatch(batchCopy.getContainer());
      success = true;
    } finally {
      if (!success) {
        batchCopy.clear();
      }
    }
  }

  @Override
  public void close() {
    rightContainer.clear();
    rightCounts.clear();
    super.close();
  }

  @Override
  protected void killIncoming(boolean sendUpstream) {
    this.left.kill(sendUpstream);
    this.right.kill(sendUpstream);
  }

  @Override
  public int getRecordCount() {
    return outputRecords;
  }

  /**
   * Returns the left side incoming for the Lateral Join. Used by right branch leaf operator of Lateral
   * to process the records at joinIndex.
   *
   * @return - RecordBatch received as left side incoming
   */
  @Override
  public RecordBatch getIncoming() {
    assert (left != null);
    return left;
  }

  /**
   * Returns the current row index which the calling operator should process in current incoming record batch
   *
   * @return - int - index of row to process.
   */
  @Override
  public int getRecordIndex() {
    assert (joinIndex < left.getRecordCount());
    return joinIndex;
  }
}
