/*
 * Copyright (c) 2022-2024, WSO2 LLC. (http://www.wso2.com).
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
            "SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_ALL_CHILDREN) VALUES (:" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";, :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN + ";)";

    public static final String GET_SHARED_APP_ID = "SELECT SHARED_APP_ID FROM SP_SHARED_APP WHERE " +
            "OWNER_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID + "; AND " +
            "MAIN_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + "; AND " +
            "SHARED_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";";

    public static final String HAS_FRAGMENT_APPS = "SELECT COUNT(1) FROM SP_SHARED_APP WHERE " +
            "MAIN_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + ";";

    public static final String GET_SHARED_APPLICATIONS = "SELECT SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_ALL_CHILDREN "
            + "FROM SP_SHARED_APP WHERE OWNER_ORG_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID +
            "; AND MAIN_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + ";";

    public static final String GET_SHARED_APPLICATION = "SELECT SHARED_APP_ID, SHARED_ORG_ID, SHARE_WITH_ALL_CHILDREN "
            + "FROM SP_SHARED_APP JOIN SP_APP ON SP_APP.UUID = SP_SHARED_APP.SHARED_APP_ID WHERE SHARED_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + "; AND SP_APP.ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_APP_ID + ";";

    public static final String GET_MAIN_APPLICATION = "SELECT MAIN_APP_ID, OWNER_ORG_ID FROM SP_SHARED_APP WHERE " +
            "SHARED_APP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID + "; AND SHARED_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";";

    public static final String IS_FRAGMENT_APPLICATION = "SELECT COUNT(1) FROM SP_METADATA WHERE " +
            "SP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_ID + "; AND NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_NAME + "; AND VALUE = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_VALUE + ";";

    public static final String IS_FRAGMENT_APPLICATION_H2 = "SELECT COUNT(1) FROM SP_METADATA WHERE " +
            "SP_ID = :" + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_ID + "; AND NAME = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_NAME + "; AND `VALUE` = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_VALUE + ";";

    public static final String UPDATE_SHARE_WITH_ALL_CHILDREN = "UPDATE SP_SHARED_APP SET SHARE_WITH_ALL_CHILDREN = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN + "; WHERE MAIN_APP_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + "; AND OWNER_ORG_ID = :"
            + SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID + ";";

    public static final String GET_FILTERED_SHARED_APPLICATIONS =
            "SELECT SHARED_ORG_ID, SHARED_APP_ID FROM SP_SHARED_APP WHERE MAIN_APP_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID + "; AND OWNER_ORG_ID = :" +
                    SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID + "; AND SHARED_ORG_ID IN (" +
                    SQLPlaceholders.SHARED_ORG_ID_LIST_PLACEHOLDER + ")";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_MYSQL =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND ssa.OWNER_ORG_ID = ? ) SELECT ID, APP_NAME, " +
            "DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM DiscoverableApps WHERE " +
            "rn = 1 ORDER BY ID DESC LIMIT ?, ?";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_POSTGRES =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND ssa.OWNER_ORG_ID = ? ) SELECT ID, APP_NAME, " +
            "DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM DiscoverableApps WHERE " +
            "rn = 1 ORDER BY ID DESC OFFSET ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_ORACLE =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY " +
            "sa.APP_NAME ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn " +
            "FROM SP_APP sa JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND ssa.OWNER_ORG_ID = ? ) SELECT ID, APP_NAME, " +
            "DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM DiscoverableApps WHERE " +
            "rn = 1 BETWEEN ? AND ? ORDER BY ID DESC";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_MSSQL =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND ssa.OWNER_ORG_ID = ? ) SELECT ID, APP_NAME, " +
            "DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM DiscoverableApps WHERE " +
            "rn = 1 ORDER BY ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_INFORMIX =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND ssa.OWNER_ORG_ID = ? ) SELECT ID, APP_NAME, " +
            "DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM DiscoverableApps WHERE " +
            "rn = 1 ORDER BY ID DESC SKIP ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_AND_APP_NAME_MYSQL =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND sa.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? ) " +
            "SELECT ID, APP_NAME, DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM " +
            "DiscoverableApps WHERE rn = 1 ORDER BY ID DESC LIMIT ?, ?";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_AND_APP_NAME_POSTGRESL =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND sa.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? ) " +
            "SELECT ID, APP_NAME, DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM " +
            "DiscoverableApps WHERE rn = 1 ORDER BY ID DESC OFFSET ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_AND_APP_NAME_ORACLE =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND sa.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? ) " +
            "SELECT ID, APP_NAME, DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM " +
            "DiscoverableApps WHERE rn = 1 BETWEEN ? AND ? ORDER BY ID DESC";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_AND_APP_NAME_MSSQL =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND sa.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? ) " +
            "SELECT ID, APP_NAME, DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM " +
            "DiscoverableApps WHERE rn = 1 ORDER BY ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    public static final String LOAD_DISCOVERABLE_APPS_BY_TENANT_AND_APP_NAME_INFORMIX =
            "WITH DiscoverableApps AS ( SELECT sa.ID, sa.APP_NAME, sa.DESCRIPTION, sa.UUID, sa.IMAGE_URL, " +
            "sa.ACCESS_URL, sa.USERNAME, sa.USER_STORE, sa.TENANT_ID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND sa.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? ) " +
            "SELECT ID, APP_NAME, DESCRIPTION, UUID, IMAGE_URL, ACCESS_URL, USERNAME, USER_STORE, TENANT_ID FROM " +
            "DiscoverableApps WHERE rn = 1 ORDER BY ID DESC SKIP ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_APP_COUNT_BY_TENANT =
            "WITH DiscoverableApps AS ( SELECT sa.UUID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME " +
            "ORDER BY CASE WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa " +
            "JOIN SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND ssa.OWNER_ORG_ID = ? ) SELECT count(UUID) " +
            "FROM DiscoverableApps WHERE rn = 1";

    public static final String LOAD_DISCOVERABLE_APP_COUNT_BY_APP_NAME_AND_TENANT =
            "WITH DiscoverableApps AS ( SELECT sa.UUID, ROW_NUMBER() OVER ( PARTITION BY sa.APP_NAME ORDER BY CASE " +
            "WHEN sa.UUID = ssa.SHARED_APP_ID THEN 1 ELSE 2 END, sa.ID DESC ) AS rn FROM SP_APP sa JOIN " +
            "SP_SHARED_APP ssa ON sa.UUID = ssa.SHARED_APP_ID OR sa.UUID = ssa.MAIN_APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa.IS_DISCOVERABLE = '1' AND sa.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? ) " +
            "SELECT count(UUID) FROM DiscoverableApps WHERE rn = 1";

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
        public static final String DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN = "SHARE_WITH_ALL_CHILDREN";
        public static final String DB_SCHEMA_COLUMN_NAME_SP_ID = "SP_ID";
        public static final String DB_SCHEMA_COLUMN_NAME_METADATA_NAME = "METADATA_NAME";
        public static final String DB_SCHEMA_COLUMN_NAME_METADATA_VALUE = "METADATA_VALUE";
        public static final String DB_SCHEMA_COLUMN_NAME_SP_APP_ID = "SP_APP_ID";

        public static final String SHARED_ORG_ID_LIST_PLACEHOLDER = "_SHARED_ORG_ID_LIST_";
        public static final String SHARED_ORG_ID_PLACEHOLDER_PREFIX = "SHARED_ORG_ID_";

        private SQLPlaceholders() {

        }
    }
}


