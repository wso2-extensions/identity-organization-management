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

package org.wso2.carbon.identity.organization.management.role.mgt.core.constants;

/**
 * Database constants for Role Management.
 */
public class DatabaseConstants {

    /**
     * Database Constant List.
     */
    public static final class SQLConstants {

        public static final int COUNT_COLUMN_INDEX = 1;
        public static final String VIEW_ID_COLUMN = "UM_ID";
        public static final String VIEW_USER_ID_COLUMN = "UM_USER_ID";
        public static final String VIEW_ROLE_ID_COLUMN = "UM_ROLE_ID";
        public static final String VIEW_FORCED_COLUMN = "FORCED";
        public static final String VIEW_ASSIGNED_AT_COLUMN = "ASSIGNED_AT";
        public static final String VIEW_ASSIGNED_AT_NAME_COLUMN = "UM_ORG_NAME";
        public static final String VIEW_ROLE_NAME_COLUMN = "ROLE_NAME";
        public static final String VIEW_ATTR_VALUE_COLUMN = "ATTR_VALUE";
        public static final String ASSIGNED_AT_ADDING = "ASSIGNED_AT = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + ";";
        public static final String FORCED_ADDING = "FORCED = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED + ";";
        public static final String AND = " AND ";
        public static final String OR = " OR ";
        public static final String INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING = "INSERT INTO UM_USER_ROLE_ORG (" +
                "UM_ID, UM_USER_ID, UM_ROLE_ID, UM_TENANT_ID, ORG_ID, ASSIGNED_AT, " +
                "FORCED) VALUES ";
        public static final String INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_VALUES = "(:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED + "%1$d;)";
        public static final String GET_USERS_BY_ORG_AND_ROLE = "SELECT URO.UM_USER_ID, URO.FORCED, " +
                "URO.ASSIGNED_AT, UO.UM_ORG_NAME FROM UM_USER_ROLE_ORG URO LEFT JOIN UM_ORG UO ON " +
                "URO.ASSIGNED_AT = UO.UM_ID WHERE URO.ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "; AND URO.UM_ROLE_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "; AND URO.UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";
        public static final String DELETE_ORGANIZATION_USER_ROLE_MAPPINGS_ASSIGNED_AT_ORG_LEVEL =
                "DELETE FROM UM_USER_ROLE_ORG WHERE ";
        public static final String DELETE_ORGANIZATION_USER_ROLE_MAPPING_VALUES =
                "ORG_ID= :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "%1$d; AND UM_USER_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "%1$d; AND UM_ROLE_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%1$d; AND UM_TENANT_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%1$d; AND ASSIGNED_AT = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%1$d; AND FORCED = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED + "%1$d;";
        public static final String DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID =
                "DELETE FROM UM_USER_ROLE_ORG WHERE UM_USER_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_TENANT_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";
        public static final String GET_ROLES_BY_ORG_AND_USER =
                "SELECT DISTINCT UM_ROLE_ID FROM UM_USER_ROLE_ORG WHERE ORG_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "; AND UM_USER_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_TENANT_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";
        public static final String GET_ORGANIZATION_USER_ROLE_MAPPING = "SELECT COUNT(1) FROM UM_USER_ROLE_ORG " +
                "WHERE UM_USER_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_ROLE_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "; AND UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + ";";
        public static final String GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_LINK = "SELECT FORCED " +
                "FROM UM_USER_ROLE_ORG WHERE UM_USER_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_ROLE_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "; AND UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "; AND ASSIGNED_AT = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + ";";
        public static final String FIND_ALL_CHILD_ORG_IDS =
                "WITH childOrgs(UM_ID, UM_PARENT_ID) AS ( SELECT UM_ID , UM_PARENT_ID FROM UM_ORG WHERE " +
                        "UM_PARENT_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID +
                        "; UNION ALL SELECT UO.UM_ID, UO.UM_PARENT_ID FROM UM_ORG UO JOIN childOrgs CO ON CO.UM_ID = " +
                        "UO.UM_PARENT_ID)" +
                        "SELECT UM_ID FROM childOrgs ORDER BY UM_ID";
        public static final String GET_ROLE_ID_AND_NAME = "SELECT ATTR_VALUE, ROLE_NAME FROM IDN_SCIM_GROUP WHERE " +
                "IDN_SCIM_GROUP.TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND IDN_SCIM_GROUP.ATTR_NAME = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_NAME + "; AND ";
        public static final String SCIM_GROUP_ATTR_VALUE = "IDN_SCIM_GROUP.ATTR_VALUE = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_VALUE + "%d;";
        public static final String GET_ORGANIZATION_ID = "SELECT UM_ID FROM UM_ORG WHERE UM_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + ";";
    }

    /**
     * Placeholders for using NamedJdbcTemplate.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_NAME_ID = "ID";
        public static final String DB_SCHEMA_COLUMN_NAME_USER_ID = "USER_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ROLE_ID = "ROLE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_TENANT_ID = "TENANT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ORG_ID = "ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT = "ASSIGNED_AT";
        public static final String DB_SCHEMA_COLUMN_NAME_FORCED = "FORCED";
        public static final String DB_SCHEMA_COLUMN_NAME_PARENT_ID = "PARENT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ROLE_NAME = "ROLE_NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_ATTR_VALUE = "ATTR_VALUE";
        public static final String DB_SCHEMA_COLUMN_NAME_ATTR_NAME = "ATTR_NAME";
    }
}
