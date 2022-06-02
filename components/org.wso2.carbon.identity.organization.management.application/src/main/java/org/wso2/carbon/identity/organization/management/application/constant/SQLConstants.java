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

package org.wso2.carbon.identity.organization.management.application.constant;

/**
 * This class contains database queries related to application management CRUD operations for enterprise login.
 */
public class SQLConstants {

    public static final String INSERT_SHARED_APP = "INSERT INTO SP_SHARED_APP (PARENT_APP_ID, PARENT_TENANT_ID, " +
            "SHARED_APP_ID, SHARED_TENANT_ID) VALUES (:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_APP_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_TENANT_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_TENANT_ID + ";);";

    public static final String GET_SHARED_APP_ID = "SELECT SHARED_APP_ID FROM SP_SHARED_APP WHERE " +
            "PARENT_TENANT_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_TENANT_ID + "; AND " +
            "SHARED_TENANT_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_TENANT_ID + "; AND " +
            "PARENT_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_APP_ID + ";";

    /**
     * SQL Placeholders.
     */
    public static final class SQLPlaceholders {
        public static final String DB_SCHEMA_COLUMN_NAME_PARENT_APP_ID = "PARENT_APP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_PARENT_TENANT_ID = "PARENT_TENANT_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID = "SHARED_APP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARED_TENANT_ID = "SHARED_TENANT_ID";
    }
}


