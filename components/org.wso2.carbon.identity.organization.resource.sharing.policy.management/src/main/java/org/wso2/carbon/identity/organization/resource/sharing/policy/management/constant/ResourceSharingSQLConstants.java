/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

/**
 * SQL constants for resource sharing policy management.
 */
public class ResourceSharingSQLConstants {

    // SQL for creating a resource sharing policy
    public static final String CREATE_RESOURCE_SHARING_POLICY =
            "INSERT INTO UM_RESOURCE_SHARING_POLICY (UM_RESOURCE_ID, UM_RESOURCE_TYPE, " +
                    "UM_INITIATING_ORG_ID, UM_POLICY_HOLDING_ORG_ID, UM_SHARING_POLICY) VALUES (" +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARING_POLICY + ";)";

    // SQL constant for inserting a shared resource attribute
    public static final String INSERT_SHARED_RESOURCE_ATTRIBUTE =
            "INSERT INTO UM_SHARED_RESOURCE_ATTRIBUTES " +
                    "(UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_ID, UM_SHARED_ATTRIBUTE_TYPE) VALUES (" +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + ";)";

    // SQL for deleting resource sharing policy
    public static final String DELETE_RESOURCE_SHARING_POLICY =
            "DELETE FROM UM_RESOURCE_SHARING_POLICY WHERE UM_RESOURCE_SHARING_POLICY_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + ";";

    // SQL for deleting shared resource attributes
    public static final String DELETE_SHARED_RESOURCE_ATTRIBUTE =
            "DELETE FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE UM_RESOURCE_SHARING_POLICY_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + " AND " +
                    "UM_SHARED_ATTRIBUTE_TYPE = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + ";";

    // SQL for retrieving shared resource attributes.
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE UM_RESOURCE_SHARING_POLICY_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + ";";

    // SQL for retrieving shared resource attributes by attribute type
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_TYPE = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + ";";

    // SQL for retrieving shared resource attributes by attribute ID
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_ID =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";";

    // SQL for retrieving shared resource attributes by attribute type and ID
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_ID_AND_TYPE =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_TYPE = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + "; AND " +
                    "UM_SHARED_ATTRIBUTE_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";";

    // SQL HEAD constant for retrieving resource sharing policies by organization IDs
    public static final String GET_RESOURCE_SHARING_POLICIES_BY_ORG_IDS_HEAD =
            "SELECT UM_ID, UM_RESOURCE_ID, UM_RESOURCE_TYPE, UM_INITIATING_ORG_ID, UM_POLICY_HOLDING_ORG_ID," +
                    " UM_SHARING_POLICY FROM UM_RESOURCE_SHARING_POLICY WHERE UM_POLICY_HOLDING_ORG_ID IN ";

    private ResourceSharingSQLConstants() {

    }

    /**
     * SQL Placeholders.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_NAME_UM_ID = "UM_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_RESOURCE_ID = "UM_RESOURCE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE = "UM_RESOURCE_TYPE";
        public static final String DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID = "UM_INITIATING_ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID = "UM_POLICY_HOLDING_ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARING_POLICY = "UM_SHARING_POLICY";
        public static final String DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID = "UM_RESOURCE_SHARING_POLICY_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID = "UM_SHARED_ATTRIBUTE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE = "UM_SHARED_ATTRIBUTE_TYPE";

        private SQLPlaceholders() {

        }
    }

}

