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

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_MYSQL =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT JOIN APP_GROUP_ASSOCIATION ag_assoc " +
            "ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND " +
            "ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC LIMIT ?, ?";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_POSTGRES =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT JOIN APP_GROUP_ASSOCIATION ag_assoc " +
            "ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND ssa.OWNER_ORG_ID = ? AND " +
            "(sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC OFFSET ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_ORACLE =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT OUTER JOIN APP_GROUP_ASSOCIATION " +
            "ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND ssa.OWNER_ORG_ID = ? AND " +
            "(sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "BETWEEN ? AND ? ORDER BY ID DESC";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_MSSQL =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT OUTER JOIN APP_GROUP_ASSOCIATION " +
            "ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND ssa.OWNER_ORG_ID = ? AND " +
            "(sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_INFORMIX =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT OUTER JOIN APP_GROUP_ASSOCIATION " +
            "ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND ssa.OWNER_ORG_ID = ? AND " +
            "(sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC SKIP ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_MYSQL =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT JOIN APP_GROUP_ASSOCIATION ag_assoc " +
            "ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND sa_shared.APP_NAME LIKE ? AND " +
            "ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC LIMIT ?, ?";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_POSTGRESL =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT JOIN APP_GROUP_ASSOCIATION ag_assoc " +
            "ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND sa_shared.APP_NAME LIKE ? AND " +
            "ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC OFFSET ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_ORACLE =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT OUTER JOIN APP_GROUP_ASSOCIATION " +
            "ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND sa_shared.APP_NAME LIKE ? " +
            "AND ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "BETWEEN ? AND ? ORDER BY ID DESC";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_MSSQL =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT OUTER JOIN APP_GROUP_ASSOCIATION " +
            "ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND sa_shared.APP_NAME LIKE ? " +
            "AND ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    public static final String LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_INFORMIX =
            "SELECT sa_shared.ID, sa_shared.APP_NAME, sa_shared.DESCRIPTION, sa_shared.UUID, sa_shared.IMAGE_URL, " +
            "CASE WHEN sa_shared.ACCESS_URL IS NOT NULL THEN sa_shared.ACCESS_URL ELSE sa_main.ACCESS_URL END AS " +
            "ACCESS_URL, sa_shared.USERNAME, sa_shared.USER_STORE, sa_shared.TENANT_ID, ag_assoc.GROUP_ID FROM " +
            "SP_SHARED_APP ssa JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID LEFT OUTER JOIN APP_GROUP_ASSOCIATION " +
            "ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE ssa.SHARED_ORG_ID = ? AND sa_shared.APP_NAME LIKE ? " +
            "AND ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER + "ORDER BY ID DESC SKIP ? LIMIT ?";

    public static final String LOAD_DISCOVERABLE_SHARED_APP_COUNT_BY_TENANT =
            "SELECT COUNT(sa_shared.UUID) FROM SP_SHARED_APP ssa " +
            "JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID " +
            "LEFT OUTER JOIN APP_GROUP_ASSOCIATION ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND ssa.OWNER_ORG_ID = ? AND (sa_main.IS_DISCOVERABLE = '1' OR " +
            "sa_shared.IS_DISCOVERABLE = '1')" + SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER;

    public static final String LOAD_DISCOVERABLE_SHARED_APP_COUNT_BY_APP_NAME_AND_TENANT =
            "SELECT COUNT(sa_shared.UUID) FROM SP_SHARED_APP ssa " +
            "JOIN SP_APP sa_main ON ssa.MAIN_APP_ID = sa_main.UUID " +
            "JOIN SP_APP sa_shared ON ssa.SHARED_APP_ID = sa_shared.UUID " +
            "LEFT OUTER JOIN APP_GROUP_ASSOCIATION ag_assoc ON sa_shared.ID = ag_assoc.APP_ID WHERE " +
            "ssa.SHARED_ORG_ID = ? AND sa_shared.APP_NAME LIKE ? AND ssa.OWNER_ORG_ID = ? AND " +
            "(sa_main.IS_DISCOVERABLE = '1' OR sa_shared.IS_DISCOVERABLE = '1')" +
            SQLPlaceholders.GROUP_ID_CONDITION_PLACEHOLDER;

    public static final String DISCOVERABLE_BY_ANY_USER = " AND ag_assoc.GROUP_ID IS NULL ";

    public static final String DISCOVERABLE_BY_USER_GROUPS = " AND (ag_assoc.GROUP_ID IS NULL OR ag_assoc.GROUP_ID " +
            "IN (" + SQLPlaceholders.GROUP_ID_LIST_PLACEHOLDER + ")) ";

    public static final String DELETE_SHARED_APP_LINKS_OF_ORG = "DELETE FROM SP_SHARED_APP WHERE SHARED_ORG_ID = :" +
            SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID + ";";

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

        // Related to APP_GROUP_ASSOCIATION table.
        public static final String GROUP_ID_CONDITION_PLACEHOLDER = "_GROUP_ID_CONDITION_";
        public static final String GROUP_ID_LIST_PLACEHOLDER = "_GROUP_ID_LIST_";

        private SQLPlaceholders() {

        }
    }
}


