/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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
     * H2 Database Constant List.
     */
    public static final class H2Constants {
        public static final String COUNT_COLUMN_NAME = "COUNT(1)";
        public static final String VIEW_ID_COLUMN = "UM_ID";
        public static final String VIEW_PARENT_ID_COLUMN = "UM_PARENT_ID";
        public static final String VIEW_USER_ID_COLUMN = "UM_USER_ID";
        public static final String VIEW_ROLE_ID_COLUMN = "UM_ROLE_ID";
        public static final String VIEW_ROLE_NAME_COLUMN = "UM_ROLE_NAME";
        public static final String VIEW_MANDATORY_COLUMN = "MANDATORY";
        public static final String VIEW_ASSIGNED_AT_COLUMN = "ASSIGNED_AT";
        public static final String VIEW_ASSIGNED_AT_NAME_COLUMN = "UM_ORG_NAME";
        public static final String ORG_ID_ADDING = "ORG_ID = ?";
        public static final String ASSIGNED_AT_ADDING = "ASSIGNED_AT = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + ";";
        public static final String MANDATORY_ADDING = "MANDATORY = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MANDATORY + ";";
        public static final String AND = " AND ";
        public static final String OR = " OR ";
        public static final String INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING = "INSERT INTO UM_USER_ROLE_ORG (" +
                "UM_ID, UM_USER_ID, UM_ROLE_ID, UM_HYBRID_ROLE_ID, UM_TENANT_ID, ORG_ID, ASSIGNED_AT, " +
                "MANDATORY) VALUES ";
        public static final String INSERT_INTO_ORGANIZATION_USER_ROLE_MAPPING_VALUES = "(:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_HYBRID_ROLE_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%1$d;,:" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MANDATORY + ";)";
        public static final String GET_USERS_BY_ORG_AND_ROLE = "SELECT URO.UM_USER_ID, URO.MANDATORY, " +
                "URO.ASSIGNED_AT, UO.UM_ORG_NAME FROM UM_USER_ROLE_ORG URO LEFT JOIN UM_ORG UO ON " +
                "URO.ASSIGNED_AT = UO.UM_ID WHERE URO.ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "; " +
                "AND URO.UM_ROLE_ID = :" +
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
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%1$d; AND " +
                        "MANDATORY = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MANDATORY + "%1$d;";
        public static final String DELETE_ALL_ORGANIZATION_USER_ROLE_MAPPINGS_BY_USERID =
                "DELETE FROM UM_USER_ROLE_ORG WHERE UM_USER_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_TENANT_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";
        public static final String GET_ROLES_BY_ORG_AND_USER =
                "SELECT DISTINCT UM_ROLE_ID, UM_ROLE_NAME FROM ORG_AUTHZ_VIEW WHERE ORG_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "; AND UM_USER_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_TENANT_ID = :" +
                        SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";
        public static final String GET_ROLE_ID_BY_SCIM_GROUP_NAME = "SELECT UM_ID FROM UM_HYBRID_ROLE WHERE " +
                "UM_ROLE_NAME = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_NAME + "; AND UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";
        public static final String GET_ORGANIZATION_USER_ROLE_MAPPING = "SELECT COUNT(1) FROM UM_USER_ROLE_ORG " +
                "WHERE UM_USER_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_ROLE_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "; AND UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + ";";
        public static final String GET_DIRECTLY_ASSIGNED_ORGANIZATION_USER_ROLE_MAPPING_LINK = "SELECT MANDATORY " +
                "FROM UM_USER_ROLE_ORG WHERE UM_USER_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_ROLE_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "; AND UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "; AND ASSIGNED_AT = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + ";";
        public static final String GET_ASSIGNED_AT_VALUE_OF_ORGANIZATION_USER_ROLE_MAPPING_LINK = "SELECT " +
                "ASSIGNED_AT FROM UM_USER_ROLE_ORG WHERE UM_USER_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_ROLE_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "; AND UM_TENANT_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND ORG_ID = :" +
                SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + ";";
        public static final String FIND_ALL_CHILD_ORG_IDS =
                "WITH childOrgs(UM_ID, UM_PARENT_ID) AS ( SELECT UM_ID , UM_PARENT_ID FROM UM_ORG WHERE " +
                        "UM_PARENT_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID +
                        "; UNION ALL SELECT UO.UM_ID, UO.UM_PARENT_ID FROM UM_ORG UO JOIN childOrgs CO ON CO.UM_ID = " +
                        "UO.UM_PARENT_ID)" +
                        "SELECT UM_ID, UM_PARENT_ID FROM childOrgs ORDER BY UM_ID";
    }

    /**
     * Placeholders for using NamedJdbcTemplate.
     */
    public static final class SQLPlaceholders {
        public static final String DB_SCHEMA_COLUMN_NAME_ID = "ID";
        public static final String DB_SCHEMA_COLUMN_NAME_USER_ID = "USER_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ROLE_ID = "ROLE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_HYBRID_ROLE_ID = "HYBRID_ROLE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_TENANT_ID = "TENANT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ORG_ID = "ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT = "ASSIGNED_AT";
        public static final String DB_SCHEMA_COLUMN_NAME_MANDATORY = "MANDATORY";
        public static final String DB_SCHEMA_COLUMN_NAME_PARENT_ID = "PARENT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ROLE_NAME = "ROLE_NAME";
    }
}
