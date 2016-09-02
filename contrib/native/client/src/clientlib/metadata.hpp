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


#ifndef DRILL_METADATA_H
#define DRILL_METADATA_H

#include "drill/common.hpp"
#include "drill/drillClient.hpp"
#include "env.h"

namespace Drill {
namespace meta {
    class BasicMetadata: public Metadata {
        static const std::string s_connectorName; 
        static const std::string s_connectorVersion; 

        static const std::string s_serverName;
        static const std::string s_serverVersion;

        static const std::string s_catalogSeparator;
        static const std::string s_catalogTerm;

        static const std::string s_identifierQuoteString;
        static const std::vector<std::string> s_sqlKeywords;
        static const std::vector<std::string> s_numericFunctions;
        static const std::string s_schemaTerm;
        static const std::string s_searchEscapeString;
        static const std::string s_specialCharacters;
        static const std::vector<std::string> s_stringFunctions;
        static const std::vector<std::string> s_systemFunctions;
        static const std::string s_tableTerm;
        static const std::vector<std::string> s_dateTimeFunctions;

        const std::string& getConnectorName() const { return s_connectorName; };
        const std::string& getConnectorVersion() const { return s_connectorVersion; }
        uint32_t getConnectorMajorVersion() const { return DRILL_VERSION_MAJOR; } 
        uint32_t getConnectorMinorVersion() const { return DRILL_VERSION_MINOR; } 
        uint32_t getConnectorPatchVersion() const { return DRILL_VERSION_PATCH; } 

        const std::string& getServerName() const { return s_serverName; }
        const std::string& getServerVersion() const { return s_serverVersion; }
        uint32_t getServerMajorVersion() const { return 0; } 
        uint32_t getServerMinorVersion() const { return 0; } 
        uint32_t getServerPatchVersion() const { return 0; } 


        status_t getCatalogs(const std::string& catalogPattern, Metadata::pfnCatalogMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) { return QRY_FAILURE; }
        status_t getSchemas(const std::string& catalogPattern, const std::string& schemaPattern, Metadata::pfnSchemaMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) { return QRY_FAILURE; }
        status_t getTables(const std::string& catalogPattern, const std::string& schemaPattern, const std::string& tablePattern, Metadata::pfnTableMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) { return QRY_FAILURE; }
        status_t getColumns(const std::string& catalogPattern, const std::string& schemaPattern, const std:: string& tablePattern, const std::string& columnPattern, Metadata::pfnColumnMetadataListener listener, void* listenerCtx, QueryHandle_t* qHandle) { return QRY_FAILURE; }

        bool areAllTableSelectable() const { return false; }
        bool isCatalogAtStart() const { return true; }
        const std::string& getCatalogSeparator() const { return s_catalogSeparator; }
        const std::string& getCatalogTerm() const { return s_catalogTerm; }
        bool isColumnAliasingSupported() const { return true; }
        bool isNullPlusNonNullNull() const { return true; }
        bool isConvertSupported(common::MinorType from, common::MinorType to) const;
        meta::CorrelationNamesSupport getCorrelationNames() const { return meta::CN_ANY_NAMES; }
        bool isReadOnly() const { return false; }
        meta::DateTimeLiteralSupport getDateTimeLiteralsSupport() const {
            return DL_DATE
                | DL_TIME
                | DL_TIMESTAMP
                | DL_INTERVAL_YEAR
                | DL_INTERVAL_MONTH
                | DL_INTERVAL_DAY
                | DL_INTERVAL_HOUR
                | DL_INTERVAL_MINUTE
                | DL_INTERVAL_SECOND
                | DL_INTERVAL_YEAR_TO_MONTH
                | DL_INTERVAL_DAY_TO_HOUR
                | DL_INTERVAL_DAY_TO_MINUTE
                | DL_INTERVAL_DAY_TO_SECOND
                | DL_INTERVAL_HOUR_TO_MINUTE
                | DL_INTERVAL_HOUR_TO_SECOND
                | DL_INTERVAL_MINUTE_TO_SECOND;
        }

        meta::CollateSupport getCollateSupport() const { return meta::C_NONE; }// supported?
        meta::GroupBySupport getGroupBySupport() const { return meta::GB_UNRELATED; }
        meta::IdentifierCase getIdentifierCase() const { return meta::IC_STORES_UPPER; } // to check?

        const std::string& getIdentifierQuoteString() const { return s_identifierQuoteString; }
        const std::vector<std::string>& getSQLKeywords() const { return s_sqlKeywords; }
        bool isLikeEscapeClauseSupported() const { return true; }
        std::size_t getMaxBinaryLiteralLength() const { return 0; }
        std::size_t getMaxCatalogNameLength() const { return 0; }
        std::size_t getMaxCharLiteralLength() const { return 0; }
        std::size_t getMaxColumnNameLength() const { return 0; }
        std::size_t getMaxColumnsInGroupBy() const { return 0; }
        std::size_t getMaxColumnsInOrderBy() const { return 0; }
        std::size_t getMaxColumnsInSelect() const { return 0; }
        std::size_t getMaxCursorNameLength() const { return 0; }
        std::size_t getMaxLogicalLobSize() const { return 0; }
        std::size_t getMaxStatements() const { return 0; }
        std::size_t getMaxRowSize() const { return 0; }
        bool isBlobIncludedInMaxRowSize() const { return true; }
        std::size_t getMaxSchemaNameLength() const { return 0; }
        std::size_t getMaxStatementLength() const { return 0; }
        std::size_t getMaxTableNameLength() const { return 0; }
        std::size_t getMaxTablesInSelect() const { return 0; }
        std::size_t getMaxUserNameLength() const { return 0; }
        meta::NullCollation getNullCollation() const { return meta::NC_AT_END; }
        const std::vector<std::string>& getNumericFunctions() const { return s_numericFunctions; }
        meta::OuterJoinSupport getOuterJoinSupport() const { return meta::OJ_LEFT 
            | meta::OJ_RIGHT 
            | meta::OJ_FULL;
        }
        bool isUnrelatedColumnsInOrderBySupported() const { return true; }
        meta::QuotedIdentifierCase getQuotedIdentifierCase() const { return meta::QIC_SUPPORTS_MIXED; }
        const std::string& getSchemaTerm() const { return s_schemaTerm; }
        const std::string& getSearchEscapeString() const { return s_searchEscapeString; }
        const std::string& getSpecialCharacters() const { return s_specialCharacters; }
        const std::vector<std::string>& getStringFunctions() const { return s_stringFunctions; }
        meta::SubQuerySupport getSubQuerySupport() const { return SQ_CORRELATED
                | SQ_IN_COMPARISON
                | SQ_IN_EXISTS
                | SQ_IN_QUANTIFIED;
        }
        const std::vector<std::string>& getSystemFunctions() const { return s_systemFunctions; }
        const std::string& getTableTerm() const { return s_tableTerm; }
        const std::vector<std::string>& getDateTimeFunctions() const { return s_dateTimeFunctions; }
        bool isTransactionSupported() const { return false; }
        meta::UnionSupport getUnionSupport() const { return meta::U_UNION | meta::U_UNION_ALL; }
        bool isSelectForUpdateSupported() const { return false; }
    };
} // namespace meta
} // namespace Drill

#endif // DRILL_METADATA
