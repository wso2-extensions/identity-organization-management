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

package org.wso2.carbon.identity.organization.management.service.constant;

/**
 * This class contains database queries related to organization management CRUD operations.
 */
public class SQLConstants {

    public static final String OR = " OR ";
    public static final String AND = " AND ";
    public static final String SCIM_GROUP_ROLE_NAME = "IDN_SCIM_GROUP.ROLE_NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_NAME + "%d;";
    public static final String PERMISSION_LIST_PLACEHOLDER = "_PERMISSION_LIST_";

    public static final String VIEW_ROLE_NAME_COLUMN = "UM_ROLE_NAME";
    public static final String VIEW_SCIM_ATTR_VALUE_COLUMN = "ATTR_VALUE";

    public static final String VIEW_ID_COLUMN = "UM_ID";
    public static final String VIEW_USER_ID_COLUMN = "UM_USER_ID";
    public static final String VIEW_ROLE_ID_COLUMN = "UM_ROLE_ID";
    public static final String VIEW_ASSIGNED_AT_COLUMN = "ASSIGNED_AT";
    public static final String VIEW_FORCED_COLUMN = "FORCED";

    public static final String VIEW_NAME_COLUMN = "UM_ORG_NAME";
    public static final String VIEW_DESCRIPTION_COLUMN = "UM_ORG_DESCRIPTION";
    public static final String VIEW_CREATED_TIME_COLUMN = "UM_CREATED_TIME";
    public static final String VIEW_LAST_MODIFIED_COLUMN = "UM_LAST_MODIFIED";
    public static final String VIEW_PARENT_ID_COLUMN = "UM_PARENT_ID";
    public static final String VIEW_ATTR_KEY_COLUMN = "UM_ATTRIBUTE_KEY";
    public static final String VIEW_ATTR_VALUE_COLUMN = "UM_ATTRIBUTE_VALUE";

    public static final String INSERT_ORGANIZATION = "INSERT INTO UM_ORG (UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, " +
            "UM_CREATED_TIME, UM_LAST_MODIFIED, UM_TENANT_ID, UM_PARENT_ID) VALUES (:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";, :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_NAME + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_DESCRIPTION + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_CREATED_TIME + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";, :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID
            + ";)";

    public static final String CHECK_ORGANIZATION_EXIST_BY_NAME = "SELECT COUNT(1) FROM UM_ORG WHERE UM_TENANT_ID = " +
            ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND UM_ORG_NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_NAME + ";";

    public static final String CHECK_ORGANIZATION_EXIST_BY_ID = "SELECT COUNT(1) FROM UM_ORG WHERE UM_TENANT_ID = " +
            ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND UM_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String GET_ORGANIZATION_ID_BY_NAME = "SELECT UM_ID FROM UM_ORG WHERE UM_TENANT_ID = " +
            ":" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND UM_ORG_NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_NAME + ";";

    public static final String INSERT_ATTRIBUTE = "INSERT INTO UM_ORG_ATTRIBUTE (UM_ORG_ID, UM_ATTRIBUTE_KEY, " +
            "UM_ATTRIBUTE_VALUE) VALUES (:" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_KEY + ";, :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE + ";)";

    public static final String GET_ORGANIZATION_BY_ID = "SELECT UM_ORG.UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, " +
            "UM_CREATED_TIME, UM_LAST_MODIFIED, UM_PARENT_ID, UM_ATTRIBUTE_KEY, UM_ATTRIBUTE_VALUE FROM UM_ORG " +
            "LEFT OUTER JOIN UM_ORG_ATTRIBUTE ON UM_ORG.UM_ID = UM_ORG_ATTRIBUTE.UM_ORG_ID WHERE UM_ORG.UM_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "; AND UM_TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";

    public static final String GET_ORGANIZATIONS_BY_TENANT_ID = "SELECT DISTINCT UM_ORG.UM_ID, UM_ORG.UM_ORG_NAME, " +
            "UM_ORG.UM_CREATED_TIME FROM UM_ORG INNER JOIN UM_USER_ROLE_ORG ON UM_USER_ROLE_ORG.ORG_ID = " +
            "UM_ORG.UM_ID WHERE ";

    public static final String GET_ORGANIZATIONS_BY_TENANT_ID_TAIL = "UM_USER_ROLE_ORG.UM_USER_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "; AND UM_USER_ROLE_ORG.UM_TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; %s AND UM_ORG.UM_TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; ORDER BY UM_ORG.UM_CREATED_TIME %s LIMIT:" +
            SQLPlaceholders.DB_SCHEMA_LIMIT + ";";


    public static final String GET_ROLE_NAMES = "SELECT UM_ROLE_NAME " +
            "FROM UM_ROLE_PERMISSION WHERE UM_PERMISSION_ID IN (SELECT UM_ID FROM UM_PERMISSION WHERE UM_RESOURCE_ID " +
            "IN (" + PERMISSION_LIST_PLACEHOLDER + "))";

    public static final String GET_ROLE_IDS_FOR_TENANT = "SELECT ATTR_VALUE FROM IDN_SCIM_GROUP WHERE " +
            "IDN_SCIM_GROUP.TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND IDN_SCIM_GROUP.ATTR_NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_NAME + "; AND ";

    public static final String DELETE_ORGANIZATION_BY_ID = "DELETE FROM UM_ORG WHERE UM_TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; AND UM_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String DELETE_ORGANIZATION_ATTRIBUTES_BY_ID = "DELETE FROM UM_ORG_ATTRIBUTE WHERE " +
            "UM_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String CHECK_CHILD_ORGANIZATIONS_EXIST = "SELECT COUNT(1) FROM UM_ORG WHERE UM_PARENT_ID = :"
            + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID + ";";

    public static final String PATCH_ORGANIZATION = "UPDATE UM_ORG SET ";

    public static final String PATCH_ORGANIZATION_CONCLUDE = " = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE +
            "; WHERE UM_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String UPDATE_ORGANIZATION_LAST_MODIFIED =
            "UPDATE UM_ORG SET UM_LAST_MODIFIED = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED +
                    "; WHERE UM_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String UPDATE_ORGANIZATION = "UPDATE UM_ORG SET UM_ORG_NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_NAME + ";, UM_ORG_DESCRIPTION = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_DESCRIPTION + ";, UM_LAST_MODIFIED = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED + "; WHERE UM_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String CHECK_ORGANIZATION_ATTRIBUTE_KEY_EXIST = "SELECT COUNT(1) FROM UM_ORG_ATTRIBUTE WHERE" +
            " UM_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "; AND UM_ATTRIBUTE_KEY = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_KEY + ";";

    public static final String UPDATE_ORGANIZATION_ATTRIBUTE_VALUE = "UPDATE UM_ORG_ATTRIBUTE SET " +
            "UM_ATTRIBUTE_VALUE = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE + "; WHERE UM_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "; AND UM_ATTRIBUTE_KEY = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_KEY + ";";

    public static final String DELETE_ORGANIZATION_ATTRIBUTE = "DELETE FROM UM_ORG_ATTRIBUTE WHERE UM_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "; AND UM_ATTRIBUTE_KEY = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_KEY + ";";

    public static final String GET_CHILD_ORGANIZATIONS = "SELECT UM_ID FROM UM_ORG WHERE UM_PARENT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID + "; AND UM_TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + ";";

    public static final String GET_ALL_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS = "SELECT * FROM UM_USER_ROLE_ORG" +
            " WHERE ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID +
            "; AND UM_TENANT_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID +
            "; AND FORCED = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED + ";";

    public static final String ADD_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS = "INSERT INTO UM_USER_ROLE_ORG (UM_ID, " +
            "UM_USER_ID, UM_ROLE_ID, " +
            "UM_TENANT_ID, ORG_ID, ASSIGNED_AT, FORCED) VALUES ";

    public static final String ADD_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS_MAPPING = "(:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%1$d;,:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED + "%1$d;)";

    /**
     * SQL Placeholders
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_NAME_ID = "ID";
        public static final String DB_SCHEMA_COLUMN_NAME_NAME = "NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_DESCRIPTION = "DESCRIPTION";
        public static final String DB_SCHEMA_COLUMN_NAME_CREATED_TIME = "CREATED_TIME";
        public static final String DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED = "LAST_MODIFIED";
        public static final String DB_SCHEMA_COLUMN_NAME_TENANT_ID = "TENANT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_PARENT_ID = "PARENT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_KEY = "KEY";
        public static final String DB_SCHEMA_COLUMN_NAME_VALUE = "VALUE";
        public static final String DB_SCHEMA_COLUMN_NAME_USER_ID = "USER_ID";
        public static final String DB_SCHEMA_LIMIT = "LIMIT";
        public static final String DB_SCHEMA_COLUMN_NAME_FORCED = "FORCED";
        public static final String DB_SCHEMA_COLUMN_NAME_ROLE_ID = "ROLE_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ORG_ID = "ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT = "ASSIGNED_AT";
        public static final String DB_SCHEMA_COLUMN_NAME_ATTR_NAME = "ATTR_NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_ROLE_NAME = "ROLE_NAME";
    }
}
