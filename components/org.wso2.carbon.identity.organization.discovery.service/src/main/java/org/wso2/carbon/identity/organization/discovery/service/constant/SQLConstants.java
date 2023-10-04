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

package org.wso2.carbon.identity.organization.discovery.service.constant;

/**
 * This class contains database queries related to organization discovery management CRUD operations.
 */
public class SQLConstants {

    public static final String DISCOVERY_ATTRIBUTE_VALUE_LIST_PLACEHOLDER = "_DISCOVERY_ATTRIBUTE_VALUE_LIST_";

    public static final String INSERT_ORGANIZATION_DISCOVERY_ATTRIBUTES = "INSERT INTO UM_ORG_DISCOVERY (UM_ORG_ID, " +
            "UM_ROOT_ORG_ID, UM_DISCOVERY_TYPE, UM_DISCOVERY_VALUE) VALUES (:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";, :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROOT_ID +
            ";, :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TYPE + ";, :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE +
            ";)";

    public static final String CHECK_DISCOVERY_ATTRIBUTE_EXIST_IN_HIERARCHY = "SELECT COUNT(1) FROM UM_ORG_DISCOVERY" +
            " WHERE UM_ROOT_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROOT_ID + "; AND UM_DISCOVERY_TYPE = :"
            + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TYPE + "; AND UM_DISCOVERY_VALUE IN (" +
            DISCOVERY_ATTRIBUTE_VALUE_LIST_PLACEHOLDER + ")";

    public static final String EXCLUDE_CURRENT_ORGANIZATION_FROM_CHECK_DISCOVERY_ATTRIBUTE_EXIST = " AND " +
            "UM_ORG_ID != :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String CHECK_DISCOVERY_ATTRIBUTE_ADDED_IN_ORGANIZATION = "SELECT COUNT(1) FROM " +
            "UM_ORG_DISCOVERY WHERE UM_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String GET_ORGANIZATION_DISCOVERY_ATTRIBUTES = "SELECT UM_DISCOVERY_TYPE, UM_DISCOVERY_VALUE " +
            "FROM UM_ORG_DISCOVERY WHERE UM_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String DELETE_ORGANIZATION_DISCOVERY_ATTRIBUTES = "DELETE FROM UM_ORG_DISCOVERY WHERE " +
            "UM_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID + ";";

    public static final String GET_ORGANIZATIONS_DISCOVERY_ATTRIBUTES = "SELECT UM_ORG_ID, UM_DISCOVERY_TYPE, " +
            "UM_DISCOVERY_VALUE FROM UM_ORG_DISCOVERY WHERE UM_ROOT_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROOT_ID + ";";

    /**
     * SQL placeholders related to organization discovery management SQL operations.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_NAME_ID = "ID";
        public static final String DB_SCHEMA_COLUMN_NAME_TYPE = "TYPE";
        public static final String DB_SCHEMA_COLUMN_NAME_VALUE = "VALUE";
        public static final String DB_SCHEMA_COLUMN_NAME_ROOT_ID = "ROOT_ID";
    }
}
