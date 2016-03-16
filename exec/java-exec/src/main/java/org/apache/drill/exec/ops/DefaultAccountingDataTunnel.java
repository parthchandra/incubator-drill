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
package org.apache.drill.exec.ops;

import com.google.common.base.Preconditions;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.proto.GeneralRPCProtos;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.record.FragmentWritableBatch;
import org.apache.drill.exec.rpc.BaseRpcOutcomeListener;
import org.apache.drill.exec.rpc.CompositeRpcOutcomeListener;
import org.apache.drill.exec.rpc.RpcOutcomeListener;
import org.apache.drill.exec.rpc.data.DataTunnel;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ExecutionControls;
import org.apache.drill.exec.testing.ExecutionControlsInjector;
import org.slf4j.Logger;

/**
 * Wrapper around a {@link org.apache.drill.exec.rpc.data.DataTunnel} that tracks the status of batches sent to
 * to other Drillbits.
 */
public class DefaultAccountingDataTunnel implements AccountingDataTunnel {
  private final DataTunnel tunnel;
  private final SendingAccountor sendingAccountor;
  private final RpcOutcomeListener<Ack> statusHandler;
  private final MinorFragmentEndpoint endpoint;

  public DefaultAccountingDataTunnel(final DataTunnel tunnel, final MinorFragmentEndpoint endpoint, final SendingAccountor accountor, final RpcOutcomeListener<Ack> statusHandler) {
    this.tunnel = Preconditions.checkNotNull(tunnel, "tunnel is required");
    this.endpoint = Preconditions.checkNotNull(endpoint, "endpoint is required");
    this.sendingAccountor = Preconditions.checkNotNull(accountor, "accountor is required");
    this.statusHandler = Preconditions.checkNotNull(statusHandler, "statusHandler is required");
  }

  @Override
  public MinorFragmentEndpoint getRemoteEndpoint() {
    return endpoint;
  }

  @Override
  public RpcOutcomeListener<Ack> getStatusHandler() {
    return statusHandler;
  }

  @Override
  public boolean isSendingBufferAvailable() {
    return getSendingBufferAvailability() > 0;
  }

  public int getSendingBufferAvailability() {
    return tunnel.getSendingBufferAvailability();
  }

  @Override
  public void sendRecordBatch(FragmentWritableBatch batch) {
    sendRecordBatch(getStatusHandler(), batch);
  }

  @Override
  public void sendRecordBatch(RpcOutcomeListener<Ack> listener, FragmentWritableBatch batch) {
    sendingAccountor.increment();
    tunnel.sendRecordBatch(getStatusHandler(), batch);
  }

  @Override
  public void setTestInjectionControls(final ControlsInjector testInjector,
                                       final ExecutionControls testControls, final org.slf4j.Logger testLogger) {
    tunnel.setTestInjectionControls(testInjector, testControls, testLogger);
  }
}
