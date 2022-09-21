/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.constant;

/**
 * This class contains database queries related to application management CRUD operations for organization login.
 */
public class SQLConstants {

    public static final String INSERT_SHARED_APP = "INSERT INTO SP_SHARED_APP (MAIN_APP_ID, OWNER_ORG_ID, " +
            "SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_SUB_ORGS) VALUES (:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARE_WITH_SUB_ORGS + ";);";

    public static final String GET_SHARED_APP_ID = "SELECT SHARED_APP_ID FROM SP_SHARED_APP WHERE " +
            "OWNER_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID + "; AND " +
            "MAIN_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + "; AND " +
            "SHARED_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";";

    public static final String HAS_FRAGMENT_APPS = "SELECT COUNT(1) FROM SP_SHARED_APP WHERE " +
            "MAIN_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + ";";

    public static final String GET_SHARED_APPLICATIONS = "SELECT SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_SUB_ORGS " +
            "FROM SP_SHARED_APP WHERE OWNER_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID +
            "; AND MAIN_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + ";";

    public static final String GET_SHARED_APPLICATION = "SELECT SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_SUB_ORGS " +
            "FROM SP_SHARED_APP JOIN SP_APP ON SP_APP.UUID = SP_SHARED_APP.SHARED_APP_ID WHERE SHARED_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + "; AND SP_APP.ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_APP_ID + ";";

    public static final String GET_MAIN_APPLICATION = "SELECT MAIN_APP_ID, OWNER_ORG_ID FROM SP_SHARED_APP WHERE " +
            "SHARED_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID + "; AND SHARED_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";";

    public static final String IS_FRAGMENT_APPLICATION = "SELECT COUNT(1) FROM SP_METADATA WHERE " +
            "SP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_ID + "; AND NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_NAME + "; AND `VALUE` = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_VALUE + ";";

    private SQLConstants() {

    }

    /**
     * SQL Placeholders.
     */
    public static final class SQLPlaceholders {

        public static final String DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID = "MAIN_APP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID = "OWNER_ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID = "SHARED_APP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID = "SHARED_ORG_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_SHARE_WITH_SUB_ORGS = "SHARE_WITH_SUB_ORGS";
        public static final String DB_SCHEMA_COLUMN_NAME_SP_ID = "SP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_METADATA_NAME = "METADATA_NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_METADATA_VALUE = "METADATA_VALUE";
        public static final String DB_SCHEMA_COLUMN_NAME_SP_APP_ID = "SP_APP_ID";

        private SQLPlaceholders() {

        }
    }
}


