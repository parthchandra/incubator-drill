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
// Generated by http://code.google.com/p/protostuff/ ... DO NOT EDIT!
// Generated from protobuf

package org.apache.drill.exec.proto.beans;

public enum RpcType implements com.dyuproject.protostuff.EnumLite<RpcType>
{
    HANDSHAKE(0),
    ACK(1),
    GOODBYE(2),
    RUN_QUERY(3),
    CANCEL_QUERY(4),
    REQUEST_RESULTS(5),
    QUERY_RESULT(6),
    QUERY_HANDLE(7),
    REQ_META_FUNCTIONS(8),
    RESP_FUNCTION_LIST(9),
    GET_QUERY_PLAN_FRAGMENTS(10),
    QUERY_PLAN_FRAGMENTS(11),
    READ_FRAGMENT_DATA(12),
    WRITE_FRAGMENT_DATA(13);
    
    public final int number;
    
    private RpcType (int number)
    {
        this.number = number;
    }
    
    public int getNumber()
    {
        return number;
    }
    
    public static RpcType valueOf(int number)
    {
        switch(number) 
        {
            case 0: return HANDSHAKE;
            case 1: return ACK;
            case 2: return GOODBYE;
            case 3: return RUN_QUERY;
            case 4: return CANCEL_QUERY;
            case 5: return REQUEST_RESULTS;
            case 6: return QUERY_RESULT;
            case 7: return QUERY_HANDLE;
            case 8: return REQ_META_FUNCTIONS;
            case 9: return RESP_FUNCTION_LIST;
            case 10: return GET_QUERY_PLAN_FRAGMENTS;
            case 11: return QUERY_PLAN_FRAGMENTS;
            case 12: return READ_FRAGMENT_DATA;
            case 13: return WRITE_FRAGMENT_DATA;
            default: return null;
        }
    }
}
