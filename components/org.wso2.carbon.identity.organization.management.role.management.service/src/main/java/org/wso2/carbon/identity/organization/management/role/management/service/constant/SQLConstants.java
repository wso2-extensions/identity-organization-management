/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.role.management.service.constant;

/**
 * SQL constant for Role Management.
 */
public class SQLConstants {

    public static final String AND = " AND ";
    public static final String OR = " OR ";

    public static final String ADD_ROLE_UM_ORG_ROLE = "INSERT INTO UM_ORG_ROLE (UM_ROLE_ID, UM_ROLE_NAME, UM_ORG_ID) " +
            "VALUES (:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID +
            ";,:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME +
            ";,:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + ";)";

    public static final String ADD_ROLE_GROUP_MAPPING = "INSERT INTO UM_ORG_ROLE_GROUP (UM_GROUP_ID, UM_ROLE_ID) " +
            "VALUES ";

    public static final String CHECK_ROLE_NAME_EXISTS = "SELECT COUNT(1) FROM UM_ORG_ROLE WHERE UM_ROLE_NAME=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME + "; AND UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + ";";

    public static final String CHECK_ROLE_EXISTS = "SELECT COUNT(1) FROM UM_ORG_ROLE WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "; AND UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + ";";

    public static final String ADD_ROLE_GROUP_MAPPING_INSERT_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d;)";

    public static final String ADD_ROLE_USER_MAPPING = "INSERT INTO UM_ORG_ROLE_USER (UM_USER_ID, UM_ROLE_ID) VALUES ";

    public static final String ADD_ROLE_USER_MAPPING_INSERT_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d;)";

    public static final String ADD_ROLE_PERMISSION_MAPPING = "INSERT INTO UM_ORG_ROLE_PERMISSION " +
            "(UM_PERMISSION_ID, UM_ROLE_ID) VALUES ";

    public static final String ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d;)";

    public static final String GET_PERMISSION_ID_FROM_STRING = "SELECT UM_ID FROM UM_ORG_PERMISSION WHERE ";

    public static final String GET_PERMISSION_ID_FROM_STRING_VALUES = "UM_RESOURCE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + "%d;";

    public static final String TENANT_ID_APPENDER = "UM_TENANT_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + ";";

    public static final String UM_ACTION_APPENDER = "UM_ACTION=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ACTION +
            ";";

    public static final String CHECK_PERMISSION_EXISTS = "SELECT COUNT(1) FROM UM_ORG_PERMISSION WHERE " +
            "UM_RESOURCE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + "; AND UM_TENANT_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + "; AND UM_ACTION=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ACTION + ";";

    public static final String CHECK_USER_ROLE_MAPPING_EXISTS = "SELECT COUNT(1) FROM UM_ORG_ROLE_USER WHERE " +
            "UM_USER_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "; AND UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String CHECK_GROUP_ROLE_MAPPING_EXISTS = "SELECT COUNT(1) FROM UM_ORG_ROLE_GROUP WHERE " +
            "UM_GROUP_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + "; AND UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String CHECK_PERMISSION_ROLE_MAPPING_EXISTS = "SELECT COUNT(1) FROM UM_ORG_ROLE_PERMISSION " +
            "WHERE UM_ROLE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "; AND UM_PERMISSION_ID " +
            "IN (SELECT UM_ID FROM UM_ORG_PERMISSION WHERE UM_RESOURCE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + "; AND UM_TENANT_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + "; AND UM_ACTION=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ACTION + ";)";

    public static final String CHECK_USER_EXISTS = "SELECT COUNT(1) FROM UM_USER WHERE UM_USER_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "; AND UM_TENANT_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + ";";

    public static final String ADD_PERMISSION_IF_NOT_EXISTS = "INSERT INTO UM_ORG_PERMISSION (UM_RESOURCE_ID, " +
            "UM_ACTION, UM_TENANT_ID) VALUES ";

    public static final String ADD_PERMISSION_IF_NOT_EXISTS_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID + "%1$d;, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ACTION + "%1$d;, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + "%1$d;)";

    public static final String GET_ROLE_FROM_ID = "SELECT * FROM UM_ORG_ROLE WHERE UM_ROLE_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "; AND UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + ";";

    public static final String GET_USERS_FROM_ROLE_ID = "SELECT * FROM UM_ORG_ROLE_USER WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String GET_MEMBER_GROUP_IDS_FROM_ROLE_ID = "SELECT UM_GROUP_ID FROM UM_ORG_ROLE_GROUP WHERE " +
            "UM_ROLE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String GET_PERMISSIONS_FROM_ROLE_ID = "SELECT UM_RESOURCE_ID FROM UM_ORG_PERMISSION WHERE " +
            "UM_ID IN (SELECT UM_PERMISSION_ID FROM UM_ORG_ROLE_PERMISSION WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";)";

    public static final String GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID = "SELECT UM_ID, UM_RESOURCE_ID FROM " +
            "UM_ORG_PERMISSION WHERE UM_ID IN (SELECT UM_PERMISSION_ID FROM UM_ORG_ROLE_PERMISSION WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";)";

    public static final String GET_ROLES_FROM_ORGANIZATION_ID_FORWARD = "SELECT DISTINCT UM_ROLE_ID, UM_ROLE_NAME " +
            "FROM UM_ORG_ROLE WHERE ";

    public static final String GET_ROLES_FROM_ORGANIZATION_ID_BACKWARD = "SELECT DISTINCT R.UM_ROLE_ID, " +
            "R.UM_ROLE_NAME FROM (SELECT UM_ROLE_ID, UM_ROLE_NAME FROM UM_ORG_ROLE WHERE ";

    public static final String GET_ROLES_COUNT_FROM_ORGANIZATION_ID = "SELECT COUNT(1) FROM UM_ORG_ROLE WHERE ";

    public static final String GET_USER_ORGANIZATION_PERMISSIONS =
            "SELECT UM_RESOURCE_ID FROM UM_ORG_PERMISSION JOIN UM_ORG_ROLE_PERMISSION ON " +
                    "UM_ORG_ROLE_PERMISSION.UM_PERMISSION_ID = UM_ORG_PERMISSION.UM_ID JOIN UM_ORG_ROLE_USER ON " +
                    "UM_ORG_ROLE_USER.UM_ROLE_ID = UM_ORG_ROLE_PERMISSION.UM_ROLE_ID JOIN UM_ORG_ROLE ON " +
                    "UM_ORG_ROLE.UM_ROLE_ID = UM_ORG_ROLE_PERMISSION.UM_ROLE_ID WHERE UM_ORG_ROLE_USER.UM_USER_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "; AND UM_ORG_ROLE.UM_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + ";";

    public static final String GET_GROUP_IDS_FROM_ROLE_ID = "SELECT UM_GROUP_ID FROM UM_ORG_ROLE_GROUP WHERE ";

    public static final String GET_USER_IDS_FROM_ROLE_ID = "SELECT UM_USER_ID FROM UM_ORG_ROLE_USER WHERE ";

    public static final String GET_PERMISSION_STRINGS_FROM_ROLE_ID = "SELECT UM_ID FROM UM_ORG_PERMISSION WHERE ";

    public static final String GET_IDS_FROM_ROLE_ID_TAIL = "UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String GET_PERMISSION_STRINGS_FROM_ROLE_ID_TAIL = "UM_ID IN (SELECT UM_PERMISSION_ID FROM " +
            "UM_ORG_ROLE_PERMISSION WHERE UM_ROLE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";)";

    public static final String GET_ROLES_FROM_ORGANIZATION_ID_FORWARD_TAIL = "UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + "; AND UM_ROLE_NAME > :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME + "; ORDER BY UM_ROLE_NAME ASC LIMIT :"  +
            SQLPlaceholders.DB_SCHEMA_LIMIT + ";";

    public static final String GET_ROLES_FROM_ORGANIZATION_ID_BACKWARD_TAIL = "UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + "; AND UM_ROLE_NAME <= :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME + "; ORDER BY UM_ROLE_NAME DESC LIMIT :"  +
            SQLPlaceholders.DB_SCHEMA_LIMIT + "; ) AS R ORDER BY R.UM_ROLE_NAME ASC";

    public static final String GET_ROLES_COUNT_FROM_ORGANIZATION_ID_TAIL = "UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + ";";

    public static final String DELETE_USERS_FROM_ROLE = "DELETE FROM UM_ORG_ROLE_USER WHERE ";

    public static final String DELETE_USERS_FROM_ROLE_MAPPING = "(UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d; AND UM_USER_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "%1$d;)";

    public static final String DELETE_USERS_FROM_ROLE_USING_ROLE_ID = "DELETE FROM UM_ORG_ROLE_USER WHERE " +
            "UM_ROLE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String DELETE_GROUPS_FROM_ROLE = "DELETE FROM UM_ORG_ROLE_GROUP WHERE ";

    public static final String DELETE_GROUPS_FROM_ROLE_MAPPING = "(UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d; AND UM_GROUP_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + "%1$d;)";

    public static final String DELETE_GROUPS_FROM_ROLE_USING_ROLE_ID = "DELETE FROM UM_ORG_ROLE_GROUP WHERE " +
            "UM_ROLE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String DELETE_PERMISSIONS_FROM_ROLE = "DELETE FROM UM_ORG_ROLE_PERMISSION WHERE ";

    public static final String DELETE_PERMISSIONS_FROM_ROLE_MAPPING = "(UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d; AND UM_PERMISSION_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + "%1$d;)";

    public static final String DELETE_PERMISSIONS_FROM_ROLE_USING_ROLE_ID = "DELETE FROM UM_ORG_ROLE_PERMISSION " +
            "WHERE UM_ROLE_ID=:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String DELETE_ROLE_FROM_ORGANIZATION = "DELETE FROM UM_ORG_ROLE WHERE UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + "; AND UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String UPDATE_ROLE_DISPLAY_NAME = "UPDATE UM_ORG_ROLE SET UM_ROLE_NAME=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME + "; WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    /**
     * Placeholders to be used in NamedJdbcTemplate.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_NAME_UM_ID = "UM_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID = "UM_ROLE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME = "UM_ROLE_NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_ORG_ID = "UM_ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID = "UM_TENANT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID = "UM_GROUP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_GROUP_NAME = "UM_GROUP_NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID = "UM_PERMISSION_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID = "UM_RESOURCE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_USER_ID = "UM_USER_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_ACTION = "UM_ACTION";

        public static final String DB_SCHEMA_LIMIT = "LIMIT";
    }
}
