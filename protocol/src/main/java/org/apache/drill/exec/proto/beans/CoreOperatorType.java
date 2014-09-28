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

public enum CoreOperatorType implements com.dyuproject.protostuff.EnumLite<CoreOperatorType>
{
    SINGLE_SENDER(0),
    BROADCAST_SENDER(1),
    FILTER(2),
    HASH_AGGREGATE(3),
    HASH_JOIN(4),
    MERGE_JOIN(5),
    HASH_PARTITION_SENDER(6),
    LIMIT(7),
    MERGING_RECEIVER(8),
    ORDERED_PARTITION_SENDER(9),
    PROJECT(10),
    UNORDERED_RECEIVER(11),
    RANGE_SENDER(12),
    SCREEN(13),
    SELECTION_VECTOR_REMOVER(14),
    STREAMING_AGGREGATE(15),
    TOP_N_SORT(16),
    EXTERNAL_SORT(17),
    TRACE(18),
    UNION(19),
    OLD_SORT(20),
    PARQUET_ROW_GROUP_SCAN(21),
    HIVE_SUB_SCAN(22),
    SYSTEM_TABLE_SCAN(23),
    MOCK_SUB_SCAN(24),
    PARQUET_WRITER(25),
    DIRECT_SUB_SCAN(26),
    TEXT_WRITER(27),
    TEXT_SUB_SCAN(28),
    JSON_SUB_SCAN(29),
    INFO_SCHEMA_SUB_SCAN(30),
    COMPLEX_TO_JSON(31),
    PRODUCER_CONSUMER(32),
    SPARK_SUB_SCAN(33);
    
    public final int number;
    
    private CoreOperatorType (int number)
    {
        this.number = number;
    }
    
    public int getNumber()
    {
        return number;
    }
    
    public static CoreOperatorType valueOf(int number)
    {
        switch(number) 
        {
            case 0: return SINGLE_SENDER;
            case 1: return BROADCAST_SENDER;
            case 2: return FILTER;
            case 3: return HASH_AGGREGATE;
            case 4: return HASH_JOIN;
            case 5: return MERGE_JOIN;
            case 6: return HASH_PARTITION_SENDER;
            case 7: return LIMIT;
            case 8: return MERGING_RECEIVER;
            case 9: return ORDERED_PARTITION_SENDER;
            case 10: return PROJECT;
            case 11: return UNORDERED_RECEIVER;
            case 12: return RANGE_SENDER;
            case 13: return SCREEN;
            case 14: return SELECTION_VECTOR_REMOVER;
            case 15: return STREAMING_AGGREGATE;
            case 16: return TOP_N_SORT;
            case 17: return EXTERNAL_SORT;
            case 18: return TRACE;
            case 19: return UNION;
            case 20: return OLD_SORT;
            case 21: return PARQUET_ROW_GROUP_SCAN;
            case 22: return HIVE_SUB_SCAN;
            case 23: return SYSTEM_TABLE_SCAN;
            case 24: return MOCK_SUB_SCAN;
            case 25: return PARQUET_WRITER;
            case 26: return DIRECT_SUB_SCAN;
            case 27: return TEXT_WRITER;
            case 28: return TEXT_SUB_SCAN;
            case 29: return JSON_SUB_SCAN;
            case 30: return INFO_SCHEMA_SUB_SCAN;
            case 31: return COMPLEX_TO_JSON;
            case 32: return PRODUCER_CONSUMER;
            case 33: return SPARK_SUB_SCAN;
            default: return null;
        }
    }
}
