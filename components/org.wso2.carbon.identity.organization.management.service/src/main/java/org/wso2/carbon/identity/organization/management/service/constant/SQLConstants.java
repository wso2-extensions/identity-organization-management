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

    public static final String GET_ORGANIZATIONS_BY_TENANT_ID = "SELECT UM_ID FROM UM_ORG WHERE ";

    public static final String GET_ORGANIZATIONS_BY_TENANT_ID_TAIL = "UM_TENANT_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID + "; ORDER BY UM_CREATED_TIME";

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

    public static final String COUNT_COLUMN = "COUNT(1)";
    public static final String VIEW_ID_COLUMN = "UM_ID";

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
    }
}
