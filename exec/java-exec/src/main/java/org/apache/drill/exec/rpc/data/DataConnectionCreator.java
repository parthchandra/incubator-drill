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
package org.apache.drill.exec.rpc.data;

import io.netty.channel.EventLoop;

import java.util.concurrent.ConcurrentMap;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.control.WorkEventBus;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.work.WorkManager.WorkerBee;

import com.google.common.collect.Maps;

/**
 * Manages a connection for each endpoint.
 */
public class DataConnectionCreator implements AutoCloseable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataConnectionCreator.class);

  private volatile DataServer server;
  private final ConcurrentMap<DrillbitEndpoint, DataConnectionManager> connectionManager = Maps.newConcurrentMap();

  private final DataConnectionConfig config;

  // we share one event loop for all local events to ensure that events never get out of order.
  private final EventLoop localEventLoop;
  private DrillbitEndpoint localIdentity;


  public DataConnectionCreator(BootStrapContext context, BufferAllocator allocator, WorkEventBus workBus,
                               WorkerBee bee) throws DrillbitStartupException {
    config = new DataConnectionConfig(allocator, context, new DataServerRequestHandler(workBus, bee));
    this.localEventLoop = context.getBitLoopGroup().next();

  }

  public DrillbitEndpoint start(DrillbitEndpoint partialEndpoint, boolean allowPortHunting) {
    server = new DataServer(config);
    int port = partialEndpoint.getControlPort() + 1;
    if (config.getBootstrapContext().getConfig().hasPath(ExecConstants.INITIAL_DATA_PORT)) {
      port = config.getBootstrapContext().getConfig().getInt(ExecConstants.INITIAL_DATA_PORT);
    }
    port = server.bind(port, allowPortHunting);
    localIdentity = partialEndpoint.toBuilder().setDataPort(port).build();
    return localIdentity;
  }

  public DataTunnel getTunnel(DrillbitEndpoint endpoint) {
    if (endpoint.equals(localIdentity)) {
      return new LocalDataTunnel(server, localEventLoop);
    } else {
      DataConnectionManager newManager = new DataConnectionManager(endpoint, context);
      DataConnectionManager oldManager = connectionManager.putIfAbsent(endpoint, newManager);
      if (oldManager != null) {
        newManager = oldManager;
      }
      return new RemoteDataTunnel(newManager);
    }
  }

  @Override
  public void close() throws Exception {
    AutoCloseables.close(server, config.getAllocator());
  }

}
