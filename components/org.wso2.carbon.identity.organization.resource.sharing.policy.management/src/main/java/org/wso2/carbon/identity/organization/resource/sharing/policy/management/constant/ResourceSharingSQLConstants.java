/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

    // SQL for creating a resource sharing policy.
    public static final String CREATE_RESOURCE_SHARING_POLICY =
            "INSERT INTO UM_RESOURCE_SHARING_POLICY (UM_RESOURCE_ID, UM_RESOURCE_TYPE, " +
                    "UM_INITIATING_ORG_ID, UM_POLICY_HOLDING_ORG_ID, UM_SHARING_POLICY) VALUES (" +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARING_POLICY + ";)";

    // SQL for retrieving resource sharing policy by resource sharing policy ID.
    public static final String GET_RESOURCE_SHARING_POLICY_BY_ID =
            "SELECT UM_ID, UM_RESOURCE_ID, UM_RESOURCE_TYPE, UM_INITIATING_ORG_ID, " +
                    "UM_POLICY_HOLDING_ORG_ID, UM_SHARING_POLICY " +
                    "FROM UM_RESOURCE_SHARING_POLICY WHERE UM_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ID + ";";

    // SQL for retrieving resource sharing policy by resource type, ID, initiating org ID and policy holding org ID.
    public static final String GET_RESOURCE_SHARING_POLICY_BY_RESOURCE_KEYS =
            "SELECT * FROM UM_RESOURCE_SHARING_POLICY " +
                    "WHERE UM_RESOURCE_TYPE = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE + "; AND " +
                    "UM_RESOURCE_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + "; AND " +
                    "UM_INITIATING_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + "; AND " +
                    "UM_POLICY_HOLDING_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID + ";";

    // SQL HEAD constant for retrieving resource sharing policies by organization IDs.
    public static final String GET_RESOURCE_SHARING_POLICIES_BY_ORG_IDS_HEAD =
            "SELECT UM_ID, UM_RESOURCE_ID, UM_RESOURCE_TYPE, UM_INITIATING_ORG_ID, UM_POLICY_HOLDING_ORG_ID," +
                    " UM_SHARING_POLICY FROM UM_RESOURCE_SHARING_POLICY WHERE UM_POLICY_HOLDING_ORG_ID IN ";

    // SQL tail for resource type filter.
    public static final String RESOURCE_TYPE_FILTER = " AND UM_RESOURCE_TYPE = ?";

    public static final String GET_RESOURCE_SHARING_POLICIES_WITH_SHARED_ATTRIBUTES_BY_POLICY_HOLDING_ORGS_HEAD =
            "SELECT rsp.UM_POLICY_HOLDING_ORG_ID, " +
                    "rsp.UM_ID AS " +
                    SQLPlaceholders.JOIN_COLUMN_UM_ID_OF_UM_RESOURCE_SHARING_POLICY_TABLE + ", " +
                    "rsp.UM_RESOURCE_ID, rsp.UM_RESOURCE_TYPE, rsp.UM_INITIATING_ORG_ID, " +
                    "rsp.UM_POLICY_HOLDING_ORG_ID, rsp.UM_SHARING_POLICY, " +
                    "attr.UM_ID AS " +
                    SQLPlaceholders.JOIN_COLUMN_UM_ID_OF_UM_SHARED_RESOURCE_ATTRIBUTES_TABLE + ", " +
                    "attr.UM_RESOURCE_SHARING_POLICY_ID, attr.UM_SHARED_ATTRIBUTE_TYPE, attr.UM_SHARED_ATTRIBUTE_ID " +
                    "FROM " +
                    "UM_RESOURCE_SHARING_POLICY rsp LEFT JOIN UM_SHARED_RESOURCE_ATTRIBUTES attr " +
                    "ON rsp.UM_ID = attr.UM_RESOURCE_SHARING_POLICY_ID " +
                    "WHERE rsp.UM_POLICY_HOLDING_ORG_ID IN (%s)";

    public static final String GET_RESOURCE_SHARING_POLICIES_WITH_INITIATING_ORG_ID =
            "SELECT rsp.UM_POLICY_HOLDING_ORG_ID, " +
                    "rsp.UM_ID, rsp.UM_RESOURCE_ID, rsp.UM_RESOURCE_TYPE, rsp.UM_INITIATING_ORG_ID, " +
                    "rsp.UM_POLICY_HOLDING_ORG_ID, rsp.UM_SHARING_POLICY, " +
                    "attr.UM_ID AS " + SQLPlaceholders.JOIN_COLUMN_UM_ID_OF_UM_SHARED_RESOURCE_ATTRIBUTES_TABLE + ", " +
                    "attr.UM_RESOURCE_SHARING_POLICY_ID, attr.UM_SHARED_ATTRIBUTE_TYPE, attr.UM_SHARED_ATTRIBUTE_ID " +
                    "FROM " +
                    "UM_RESOURCE_SHARING_POLICY rsp LEFT JOIN UM_SHARED_RESOURCE_ATTRIBUTES attr " +
                    "ON rsp.UM_ID = attr.UM_RESOURCE_SHARING_POLICY_ID " +
                    "WHERE rsp.UM_INITIATING_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID
                    + "; AND rsp.UM_RESOURCE_TYPE = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE +
                    "; AND rsp.UM_RESOURCE_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + ";";

    // SQL for deleting resource sharing policy.
    public static final String DELETE_RESOURCE_SHARING_POLICY =
            "DELETE FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ID + "; AND " +
                    "UM_INITIATING_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";";

    // SQL for deleting resource sharing policy by resource type and ID.
    public static final String DELETE_RESOURCE_SHARING_POLICY_BY_RESOURCE_TYPE_AND_ID =
            "DELETE FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_RESOURCE_TYPE = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE + "; AND " +
                    "UM_RESOURCE_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + "; AND " +
                    "UM_INITIATING_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";";

    // SQL for deleting resource sharing policy in an organization by resource type and ID.
    public static final String DELETE_RESOURCE_SHARING_POLICY_IN_ORG_BY_RESOURCE_TYPE_AND_ID =
            "DELETE FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_POLICY_HOLDING_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID + "; AND " +
                    "UM_RESOURCE_TYPE = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE + "; AND " +
                    "UM_RESOURCE_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + "; AND " +
                    "UM_INITIATING_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";";

    // SQL constant for inserting a shared resource attribute.
    public static final String CREATE_SHARED_RESOURCE_ATTRIBUTE =
            "INSERT INTO UM_SHARED_RESOURCE_ATTRIBUTES " +
                    "(UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_ID, UM_SHARED_ATTRIBUTE_TYPE) VALUES (" +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";, " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + ";)";

    // SQL for retrieving shared resource attributes.
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE UM_RESOURCE_SHARING_POLICY_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + ";";

    // SQL for retrieving shared resource attributes by attribute type.
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_TYPE = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + ";";

    // SQL for retrieving shared resource attributes by attribute ID.
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_ID =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";";

    // SQL for retrieving shared resource attributes by attribute type and ID.
    public static final String GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE_AND_ID =
            "SELECT UM_ID, UM_RESOURCE_SHARING_POLICY_ID, UM_SHARED_ATTRIBUTE_TYPE, UM_SHARED_ATTRIBUTE_ID " +
                    "FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_TYPE = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + "; AND " +
                    "UM_SHARED_ATTRIBUTE_ID = " +
                    ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";";

    // SQL for deleting shared resource attributes.
    public static final String DELETE_SHARED_RESOURCE_ATTRIBUTE =
            "DELETE FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_RESOURCE_SHARING_POLICY_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID + "; AND " +
                    "UM_SHARED_ATTRIBUTE_TYPE = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + "; AND " +
                    "UM_RESOURCE_SHARING_POLICY_ID IN (SELECT UM_ID FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_INITIATING_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";)";

    // SQL for deleting shared resource attribute by attribute type and ID.
    public static final String DELETE_SHARED_RESOURCE_ATTRIBUTE_BY_ATTRIBUTE_TYPE_AND_ID =
            "DELETE FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_TYPE = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + "; AND " +
                    "UM_SHARED_ATTRIBUTE_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + "; AND " +
                    "UM_RESOURCE_SHARING_POLICY_ID IN (SELECT UM_ID FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_INITIATING_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + ";)";

    // SQL for deleting resource sharing policy by resource type and ID at Resource deletion.
    public static final String DELETE_RESOURCE_SHARING_POLICY_BY_RESOURCE_TYPE_AND_ID_AT_RESOURCE_DELETION =
            "DELETE FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_RESOURCE_TYPE = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE + "; AND " +
                    "UM_RESOURCE_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID + ";";

    // SQL for deleting shared resource attribute by attribute type and ID at Attribute deletion.
    public static final String DELETE_SHARED_RESOURCE_ATTRIBUTE_BY_ATTRIBUTE_TYPE_AND_ID_AT_ATTRIBUTE_DELETION =
            "DELETE FROM UM_SHARED_RESOURCE_ATTRIBUTES WHERE " +
                    "UM_SHARED_ATTRIBUTE_TYPE = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE + "; AND " +
                    "UM_SHARED_ATTRIBUTE_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID + ";";

    // SQL for deleting resource sharing policy by org ID.
    public static final String DELETE_RESOURCE_SHARING_POLICY_BY_ORG_ID_AT_ATTRIBUTE_DELETION =
            "DELETE FROM UM_RESOURCE_SHARING_POLICY WHERE " +
                    "UM_INITIATING_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID + "; OR " +
                    "UM_POLICY_HOLDING_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID + ";";

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

        public static final String JOIN_COLUMN_UM_ID_OF_UM_RESOURCE_SHARING_POLICY_TABLE = "POLICY_ID";
        public static final String JOIN_COLUMN_UM_ID_OF_UM_SHARED_RESOURCE_ATTRIBUTES_TABLE = "ATTRIBUTE_ID";

        private SQLPlaceholders() {

        }
    }

}

