/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.management.service.constants;

/**
 * SQL constants for Role Management.
 */
public class SQLConstants {

    public static final String AND = " AND ";
    public static final String OR = " OR ";

    public static final String ADD_ROLE_UM_RM_ROLE = "INSERT INTO UM_RM_ROLE (UM_ROLE_ID, UM_ROLE_NAME, UM_ORG_ID, " +
            "UM_TENANT_ID) VALUES (:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID +
            ";,:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME +
            ";,:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID +
            ";,:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + ";)";

    public static final String ADD_ROLE_GROUP_MAPPING = "INSERT INTO UM_RM_GROUP_ROLE (UM_GROUP_ID, UM_ROLE_ID) " +
            "VALUES ";

    public static final String ADD_ROLE_GROUP_MAPPING_INSERT_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d;)";

    public static final String ADD_ROLE_USER_MAPPING = "INSERT INTO UM_RM_USER_ROLE (UM_USER_ID, UM_ROLE_ID) VALUES ";

    public static final String ADD_ROLE_USER_MAPPING_INSERT_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d;)";

    public static final String ADD_ROLE_PERMISSION_MAPPING = "INSERT INTO UM_RM_PERMISSION_ROLE " +
            "(UM_PERMISSION_ID, UM_ROLE_ID) VALUES ";

    public static final String ADD_ROLE_PERMISSION_MAPPING_INSERT_VALUES = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + "%$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%$d;)";

    public static final String GET_ROLE_FROM_ID = "SELECT * FROM UM_RM_ROLE WHERE UM_ROLE_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "; AND UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + "; AND UM_TENANT_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + ";";

    public static final String GET_USERS_FROM_ROLE_ID = "SELECT * FROM UM_RM_USER_ROLE WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String GET_GROUPS_FROM_ROLE_ID = "SELECT UM_GROUP_ID, UM_DISPLAY_NAME FROM UM_RM_GROUP_ROLE " +
            "(SELECT * FROM UM_RM_GROUP WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";)";

    public static final String GET_PERMISSIONS_FROM_ROLE_ID = "SELECT UM_RESOURCE_ID FROM UM_PERMISSION WHERE " +
            "UM_ID IN (SELECT UM_ID FROM UM_RM_PERMISSION_ROLE WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";)";

    public static final String GET_PERMISSIONS_WITH_ID_FROM_ROLE_ID = "SELECT UM_ID, UM_RESOURCE_ID FROM " +
            "UM_PERMISSION WHERE UM_ID IN (SELECT UM_ID FROM UM_RM_PERMISSION_ROLE WHERE UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";)";

    public static final String GET_ROLES_FROM_ORGANIZATION_ID = "SELECT DISTINCT UM_ROLE_ID, UM_ROLE_NAME FROM " +
            "UM_RM_ROLE WHERE ";

    public static final String GET_ROLES_FROM_ORGANIZATION_ID_TAIL = "UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + "; AND UM_TENANT_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_TENANT_ID + "; ORDER BY UM_ROLE_NAME %s LIMIT :" +
            SQLPlaceholders.DB_SCHEMA_LIMIT + ";";

    public static final String DELETE_USERS_FROM_ROLE = "DELETE FROM UM_RM_USER_ROLE WHERE ";

    public static final String DELETE_USERS_FROM_ROLE_MAPPING = "(UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d; AND UM_USER_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_USER_ID + "%1$d;)";

    public static final String DELETE_GROUPS_FROM_ROLE = "DELETE FROM UM_RM_GROUP_ROLE WHERE ";

    public static final String DELETE_GROUPS_FROM_ROLE_MAPPING = "(UM_ROLE_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d; AND UM_GROUP_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_GROUP_ID + "%1$d)";

    public static final String DELETE_PERMISSIONS_FROM_ROLE = "DELETE FROM UM_RM_PERMISSION_ROLE WHERE ";

    public static final String DELETE_PERMISSIONS_FROM_ROLE_MAPPING = "(UM_ROLE_ID=:)" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + "%1$d; AND UM_PERMISSION_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID + "%1$d";

    public static final String DELETE_ROLE_FROM_ORGANIZATION = "DELETE FROM UM_RM_ROLE WHERE UM_ORG_ID=:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ORG_ID + "; AND UM_ROLE_ID:=" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID + ";";

    public static final String REPLACE_DISPLAY_NAME_OF_ROLE = "UPDATE UM_RM_ROLE SET UM_ROLE_NAME=:" +
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
        public static final String DB_SCHEMA_COLUMN_NAME_UM_PERMISSION = "UM_PERMISSION";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_PERMISSION_ID = "UM_PERMISSION_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_RESOURCE_ID = "UM_RESOURCE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_USER_ID = "UM_USER_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_UM_USER_NAME = "UM_USER_NAME";

        public static final String DB_SCHEMA_LIMIT = "LIMIT";
    }
}
