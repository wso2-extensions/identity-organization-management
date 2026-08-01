/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant;

/**
 * SQL constants for connection sharing association persistence (table {@code IDN_ORG_CONNECTION_ASSOCIATION}).
 */
public class ConnectionSharingSQLConstants {

    private ConnectionSharingSQLConstants() {

    }

    // Column / named-parameter names.
    public static final String COLUMN_NAME_ID = "ID";
    public static final String COLUMN_NAME_RESOURCE_TYPE = "RESOURCE_TYPE";
    public static final String COLUMN_NAME_SHARED_RESOURCE_UUID = "SHARED_RESOURCE_UUID";
    public static final String COLUMN_NAME_SHARED_ORG_ID = "SHARED_ORG_ID";
    public static final String COLUMN_NAME_ASSOCIATED_CONNECTION_UUID = "ASSOCIATED_CONNECTION_UUID";
    public static final String COLUMN_NAME_ASSOCIATED_ORG_ID = "ASSOCIATED_ORG_ID";

    public static final String SHARED_ORG_ID_PLACEHOLDER_PREFIX = "SHARED_ORG_ID_";
    public static final String SHARED_ORG_ID_LIST_PLACEHOLDER = "_SHARED_ORG_ID_LIST_";

    public static final String INSERT_CONNECTION_ASSOCIATION =
            "INSERT INTO IDN_ORG_CONNECTION_ASSOCIATION (RESOURCE_TYPE, ASSOCIATED_CONNECTION_UUID, " +
                    "ASSOCIATED_ORG_ID, SHARED_RESOURCE_UUID, SHARED_ORG_ID) VALUES (:RESOURCE_TYPE;, " +
                    ":ASSOCIATED_CONNECTION_UUID;, :ASSOCIATED_ORG_ID;, :SHARED_RESOURCE_UUID;, :SHARED_ORG_ID;)";

    public static final String GET_SHARED_CONNECTION_ID =
            "SELECT SHARED_RESOURCE_UUID FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE " +
                    "RESOURCE_TYPE = :RESOURCE_TYPE; AND ASSOCIATED_CONNECTION_UUID = :ASSOCIATED_CONNECTION_UUID; " +
                    "AND ASSOCIATED_ORG_ID = :ASSOCIATED_ORG_ID; AND SHARED_ORG_ID = :SHARED_ORG_ID;";

    public static final String GET_CONNECTION_ASSOCIATION_BY_SHARED_RESOURCE =
            "SELECT ID, RESOURCE_TYPE, ASSOCIATED_CONNECTION_UUID, ASSOCIATED_ORG_ID, SHARED_RESOURCE_UUID, " +
                    "SHARED_ORG_ID FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE RESOURCE_TYPE = :RESOURCE_TYPE; AND " +
                    "SHARED_RESOURCE_UUID = :SHARED_RESOURCE_UUID;";

    public static final String GET_CONNECTION_ASSOCIATIONS_BY_RESIDENT_ORG =
            "SELECT ID, RESOURCE_TYPE, ASSOCIATED_CONNECTION_UUID, ASSOCIATED_ORG_ID, SHARED_RESOURCE_UUID, " +
                    "SHARED_ORG_ID FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE ASSOCIATED_ORG_ID = :ASSOCIATED_ORG_ID;";

    public static final String GET_CONNECTION_ASSOCIATIONS_BY_SHARED_ORG =
            "SELECT ID, RESOURCE_TYPE, ASSOCIATED_CONNECTION_UUID, ASSOCIATED_ORG_ID, SHARED_RESOURCE_UUID, " +
                    "SHARED_ORG_ID FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE SHARED_ORG_ID = :SHARED_ORG_ID;";

    public static final String GET_CONNECTION_ASSOCIATIONS_BY_PARENT =
            "SELECT ID, RESOURCE_TYPE, ASSOCIATED_CONNECTION_UUID, ASSOCIATED_ORG_ID, SHARED_RESOURCE_UUID, " +
                    "SHARED_ORG_ID FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE RESOURCE_TYPE = :RESOURCE_TYPE; AND " +
                    "ASSOCIATED_CONNECTION_UUID = :ASSOCIATED_CONNECTION_UUID; " +
                    "AND ASSOCIATED_ORG_ID = :ASSOCIATED_ORG_ID;";

    public static final String DELETE_CONNECTION_ASSOCIATION_IN_ORG =
            "DELETE FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE RESOURCE_TYPE = :RESOURCE_TYPE; AND " +
                    "ASSOCIATED_CONNECTION_UUID = :ASSOCIATED_CONNECTION_UUID; AND " +
                    "ASSOCIATED_ORG_ID = :ASSOCIATED_ORG_ID; AND SHARED_ORG_ID = :SHARED_ORG_ID;";

    public static final String DELETE_CONNECTION_ASSOCIATIONS_BY_ORG =
            "DELETE FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE SHARED_ORG_ID = :SHARED_ORG_ID; OR " +
                    "ASSOCIATED_ORG_ID = :ASSOCIATED_ORG_ID;";

    // Filtered / paginated query (the IN clause and trailing conditions are appended at runtime).
    public static final String GET_CONNECTION_ASSOCIATIONS_BY_FILTERING_HEAD =
            "SELECT ID, RESOURCE_TYPE, ASSOCIATED_CONNECTION_UUID, ASSOCIATED_ORG_ID, SHARED_RESOURCE_UUID, " +
                    "SHARED_ORG_ID FROM IDN_ORG_CONNECTION_ASSOCIATION WHERE RESOURCE_TYPE = :RESOURCE_TYPE; AND " +
                    "ASSOCIATED_CONNECTION_UUID = :ASSOCIATED_CONNECTION_UUID; AND " +
                    "ASSOCIATED_ORG_ID = :ASSOCIATED_ORG_ID; AND SHARED_ORG_ID IN (" +
                    SHARED_ORG_ID_LIST_PLACEHOLDER + ")";

    public static final String GET_CONNECTION_ASSOCIATIONS_ORDER_BY = " ORDER BY ID %s";
    public static final String GET_CONNECTION_ASSOCIATIONS_LIMIT = " LIMIT %d";
}
