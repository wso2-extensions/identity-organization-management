/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_DOMAIN_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_UUID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_NAME_USER_NAMES;

/**
 * SQL constants for organization user sharing.
 */
public class SQLConstants {

    public static final String ID_COLUMN_NAME = "UM_ID";
    public static final String SHARED_ORG_ID_COLUMN_NAME = "UM_ORG_ID";

    public static final String CREATE_ORGANIZATION_USER_ASSOCIATION = "INSERT INTO UM_ORG_USER_ASSOCIATION(" +
            "UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID) VALUES(?, ?, ?, ?)";
    public static final String CREATE_ORGANIZATION_USER_ASSOCIATION_WITH_TYPE = "INSERT INTO UM_ORG_USER_ASSOCIATION(" +
            "UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE) VALUES(" +
            ":" + SQLPlaceholders.COLUMN_NAME_USER_ID + ";, " +
            ":" + SQLPlaceholders.COLUMN_NAME_ORG_ID + ";, " +
            ":" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + ";, " +
            ":" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + ";, " +
            ":" + SQLPlaceholders.COLUMN_NAME_UM_SHARED_TYPE + ";)";
    public static final String DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER = "DELETE FROM " +
            "UM_ORG_USER_ASSOCIATION WHERE UM_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ?";
    public static final String DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_ROOT_USER = "DELETE FROM " +
            "UM_ORG_USER_ASSOCIATION WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER = "SELECT UM_ID, UM_USER_ID, UM_ORG_ID, " +
            "UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE " +
            "FROM UM_ORG_USER_ASSOCIATION WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER_BY_SHARED_TYPE = "SELECT UM_ID, " +
            "UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE " +
            "FROM UM_ORG_USER_ASSOCIATION WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ? AND " +
            "UM_SHARED_TYPE = ?";
    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS =
            "SELECT EXISTS ( " +
                    "    SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "    WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "    AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    ") AS has_user_associations;";
    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_ORACLE =
            "SELECT CASE " +
                    "    WHEN EXISTS ( " +
                    "        SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "        WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "        AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "    ) THEN 1 ELSE 0 " +
                    "END AS has_user_associations FROM DUAL;";
    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_MSSQL =
            "SELECT CASE " +
                    "    WHEN EXISTS ( " +
                    "        SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "        WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "        AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "    ) THEN 1 ELSE 0 " +
                    "END AS has_user_associations;";
    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_DB2 =
            "SELECT CASE " +
                    "    WHEN EXISTS ( " +
                    "        SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "        WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "        AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "    ) THEN 1 ELSE 0 " +
                    "END AS has_user_associations FROM SYSIBM.SYSDUMMY1;";
    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE =
            "SELECT EXISTS ( " +
                    "    SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "    WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "    AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "    AND UM_ORG_ID IN (:" + SQLPlaceholders.COLUMN_NAME_ORG_ID + ";) " +
                    ") AS has_user_associations;";

    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_ORACLE =
            "SELECT CASE " +
                    "    WHEN EXISTS ( " +
                    "        SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "        WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "        AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "        AND UM_ORG_ID IN (:" + SQLPlaceholders.COLUMN_NAME_ORG_ID + ";) " +
                    "    ) THEN 1 ELSE 0 " +
                    "END AS has_user_associations FROM DUAL;";

    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_MSSQL =
            "SELECT CASE " +
                    "    WHEN EXISTS ( " +
                    "        SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "        WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "        AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "        AND UM_ORG_ID IN (:" + SQLPlaceholders.COLUMN_NAME_ORG_ID + ";) " +
                    "    ) THEN 1 ELSE 0 " +
                    "END AS has_user_associations;";

    public static final String CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_DB2 =
            "SELECT CASE " +
                    "    WHEN EXISTS ( " +
                    "        SELECT 1 FROM UM_ORG_USER_ASSOCIATION " +
                    "        WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "        AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; " +
                    "        AND UM_ORG_ID IN (:" + SQLPlaceholders.COLUMN_NAME_ORG_ID + ";) " +
                    "    ) THEN 1 ELSE 0 " +
                    "END AS has_user_associations FROM SYSIBM.SYSDUMMY1;";
    public static final String GET_ORGANIZATION_USER_ASSOCIATION_FOR_ROOT_USER_IN_ORG = "SELECT UM_ID, UM_USER_ID, " +
            "UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE FROM UM_ORG_USER_ASSOCIATION " +
            "WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER = "SELECT UM_ID, UM_USER_ID, " +
            "UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE FROM UM_ORG_USER_ASSOCIATION " +
            "WHERE UM_USER_ID = ? AND UM_ORG_ID = ?";
    public static final String GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG =
            "SELECT r.UM_USER_NAME FROM UM_HYBRID_USER_ROLE r "
                    + "INNER JOIN UM_DOMAIN d "
                    + "ON r.UM_DOMAIN_ID = d.UM_DOMAIN_ID AND r.UM_TENANT_ID = d.UM_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS p "
                    + "ON r.UM_ID = p.UM_HYBRID_USER_ROLE_ID AND r.UM_TENANT_ID = p.UM_HYBRID_USER_ROLE_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_ROLE h "
                    + "ON r.UM_ROLE_ID = h.UM_ID AND r.UM_TENANT_ID = h.UM_TENANT_ID "
                    + "WHERE h.UM_UUID = :" + COLUMN_NAME_UM_UUID + "; "
                    + "AND d.UM_DOMAIN_NAME = :" + COLUMN_NAME_UM_DOMAIN_NAME + "; "
                    + "AND r.UM_USER_NAME IN (" + PLACEHOLDER_NAME_USER_NAMES + ") "
                    + "AND r.UM_TENANT_ID = :" + COLUMN_NAME_UM_TENANT_ID + "; "
                    + "AND p.UM_PERMITTED_ORG_ID != :" + COLUMN_NAME_UM_PERMITTED_ORG_ID + "; "
                    + "AND p.UM_EDIT_OPERATION = :" + COLUMN_NAME_UM_EDIT_OPERATION + ";";
    public static final String GET_SHARED_USER_ROLES =
             "SELECT DISTINCT hr.UM_UUID FROM UM_HYBRID_ROLE hr "
                    + "INNER JOIN UM_HYBRID_USER_ROLE ur "
                    + "ON hr.UM_ID = ur.UM_ROLE_ID "
                    + "AND hr.UM_TENANT_ID = ur.UM_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS rep "
                    + "ON ur.UM_ID = rep.UM_HYBRID_USER_ROLE_ID "
                    + "AND ur.UM_TENANT_ID = rep.UM_HYBRID_USER_ROLE_TENANT_ID "
                    + "WHERE hr.UM_UUID IN (" + SQLPlaceholders.PLACEHOLDER_ROLE_IDS + ") "
                    + "AND hr.UM_TENANT_ID = :" + COLUMN_NAME_UM_TENANT_ID + ";";
    public static final String GET_USER_ROLE_IN_TENANT =
            "SELECT UR.UM_ID FROM UM_HYBRID_USER_ROLE UR " +
                    "INNER JOIN UM_HYBRID_ROLE H " +
                    "ON UR.UM_ROLE_ID = H.UM_ID " +
                    "AND UR.UM_TENANT_ID = H.UM_TENANT_ID " +
                    "INNER JOIN UM_DOMAIN D " +
                    "ON UR.UM_DOMAIN_ID = D.UM_DOMAIN_ID " +
                    "AND UR.UM_TENANT_ID = D.UM_TENANT_ID " +
                    "WHERE UR.UM_USER_NAME = :" + SQLPlaceholders.COLUMN_NAME_UM_USER_NAME + "; " +
                    "AND H.UM_UUID = :" + SQLPlaceholders.COLUMN_NAME_UM_UUID + "; " +
                    "AND UR.UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID + "; " +
                    "AND D.UM_DOMAIN_NAME = :" + COLUMN_NAME_UM_DOMAIN_NAME + ";";
    public static final String INSERT_RESTRICTED_EDIT_PERMISSION =
            "INSERT INTO UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS (" +
                    "UM_HYBRID_USER_ROLE_ID, UM_HYBRID_USER_ROLE_TENANT_ID, " +
                    "UM_EDIT_OPERATION, UM_PERMITTED_ORG_ID) VALUES (" +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_ID + ";, " +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID + ";, " +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION + ";, " +
                    ":" + SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID + ";)";
    public static final String GET_SHARED_ROLES_OF_SHARED_USER =
            "SELECT H.UM_UUID " +
                    "FROM UM_HYBRID_USER_ROLE UHR " +
                    "INNER JOIN UM_HYBRID_ROLE H " +
                    "ON UHR.UM_ROLE_ID = H.UM_ID " +
                    "AND UHR.UM_TENANT_ID = H.UM_TENANT_ID " +
                    "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS UHRREP " +
                    "ON UHR.UM_ID = UHRREP.UM_HYBRID_USER_ROLE_ID " +
                    "AND UHR.UM_TENANT_ID = UHRREP.UM_HYBRID_USER_ROLE_TENANT_ID " +
                    "WHERE UHR.UM_USER_NAME = :" + SQLPlaceholders.COLUMN_NAME_UM_USER_NAME + "; " +
                    "AND UHR.UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID + "; " +
                    "AND UHR.UM_DOMAIN_ID = (SELECT UM_DOMAIN_ID FROM UM_DOMAIN WHERE " +
                    "UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID + "; " +
                    "AND UM_DOMAIN_NAME = :" + COLUMN_NAME_UM_DOMAIN_NAME + ";);";
    public static final String GET_USER_ASSOCIATIONS_OF_USER_IN_GIVEN_ORGS =
            "SELECT UM_ID, UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE " +
                    "FROM UM_ORG_USER_ASSOCIATION " +
                    "WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "AND UM_ORG_ID IN (" + SQLPlaceholders.PLACEHOLDER_ORG_IDS + ");";
    public static final String UPDATE_USER_ASSOCIATION_SHARED_TYPE =
            "UPDATE UM_ORG_USER_ASSOCIATION " +
                    "SET UM_SHARED_TYPE = :" + SQLPlaceholders.COLUMN_NAME_UM_SHARED_TYPE + "; " +
                    "WHERE UM_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_ID + ";";

    public static final String GET_LATEST_RECORD_BY_RESIDENT_RESOURCE_ID =
            "SELECT UM_SHARING_OPERATION_ID FROM UM_SHARING_OPERATION " +
                    "WHERE " + SQLPlaceholders.COLUMN_NAME_RESIDENT_RESOURCE_ID + " = ? " +
                    "AND " + SQLPlaceholders.COLUMN_NAME_CREATED_TIME + " = (" +
                    "    SELECT MAX(" + SQLPlaceholders.COLUMN_NAME_CREATED_TIME + ") " +
                    "    FROM UM_SHARING_OPERATION " +
                    "    WHERE " + SQLPlaceholders.COLUMN_NAME_RESIDENT_RESOURCE_ID + " = ?" +
                    ") " +
                    "ORDER BY " + SQLPlaceholders.COLUMN_NAME_CREATED_TIME + " DESC " +
                    "LIMIT 1;";

    public static final String GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_HEAD =
            "SELECT UM_ID, UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE " +
                    "FROM UM_ORG_USER_ASSOCIATION " +
                    "WHERE UM_ASSOCIATED_USER_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID + "; " +
                    "AND UM_ASSOCIATED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID + "; ";

    public static final String GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL =
            "UM_ORG_ID IN (" + SQLPlaceholders.PLACEHOLDER_ORG_IDS + ") " +
                    "ORDER BY UM_ID %s";

    public static final String GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT =
            "UM_ORG_ID IN (" + SQLPlaceholders.PLACEHOLDER_ORG_IDS + ") " +
                    "ORDER BY UM_ID %s LIMIT %d";

    public static final String GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT_ORACLE =
            "UM_ORG_ID IN (" + SQLPlaceholders.PLACEHOLDER_ORG_IDS + ") " +
                    "ORDER BY UM_ID %s FETCH FIRST %d ROWS ONLY";

    public static final String GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT_MSSQL =
            "UM_ORG_ID IN (" + SQLPlaceholders.PLACEHOLDER_ORG_IDS + ") " +
                    "ORDER BY UM_ID %s OFFSET 0 ROWS FETCH NEXT %d ROWS ONLY";

    /**
     * SQL placeholders related to organization user sharing SQL operations.
     */
    public static final class SQLPlaceholders {

        public static final String TABLE_NAME_UM_ORG_USER_ASSOCIATION = "UM_ORG_USER_ASSOCIATION";
        public static final String COLUMN_NAME_USER_ID = "UM_USER_ID";
        public static final String COLUMN_NAME_ORG_ID = "UM_ORG_ID";
        public static final String COLUMN_NAME_ASSOCIATED_USER_ID = "UM_ASSOCIATED_USER_ID";
        public static final String COLUMN_NAME_ASSOCIATED_ORG_ID = "UM_ASSOCIATED_ORG_ID";
        public static final String COLUMN_NAME_ASSOCIATION_INITIATED_ORG_ID = "UM_ASSOCIATION_INITIATED_ORG_ID";
        public static final String COLUMN_NAME_ASSOCIATION_TYPE = "UM_ASSOCIATION_TYPE";
        public static final String COUNT_ALIAS = "count";
        public static final String COLUMN_NAME_UM_USER_NAME = "UM_USER_NAME";
        public static final String COLUMN_NAME_UM_ROLE_ID = "UM_ROLE_ID";
        public static final String COLUMN_NAME_UM_TENANT_ID = "UM_TENANT_ID";
        public static final String COLUMN_NAME_UM_ID = "UM_ID";
        public static final String COLUMN_NAME_UM_SHARED_TYPE = "UM_SHARED_TYPE";
        public static final String COLUMN_NAME_UM_UUID = "UM_UUID";
        public static final String COLUMN_NAME_UM_DOMAIN_NAME = "UM_DOMAIN_NAME";
        public static final String COLUMN_NAME_UM_DOMAIN_ID = "UM_DOMAIN_ID";
        public static final String COLUMN_NAME_UM_HYBRID_USER_ROLE_ID = "UM_HYBRID_USER_ROLE_ID";
        public static final String COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID = "UM_HYBRID_USER_ROLE_TENANT_ID";
        public static final String COLUMN_NAME_UM_EDIT_OPERATION = "UM_EDIT_OPERATION";
        public static final String COLUMN_NAME_UM_PERMITTED_ORG_ID = "UM_PERMITTED_ORG_ID";
        public static final String COLUMN_NAME_UM_ROLE_UUID = "UM_UUID";
        public static final String HAS_USER_ASSOCIATIONS = "has_user_associations";

        public static final String PLACEHOLDER_NAME_USER_NAMES = "USER_NAMES";
        public static final String PLACEHOLDER_ROLE_IDS = "ROLE_IDS";
        public static final String PLACEHOLDER_ORG_IDS = "ORG_IDS";

        public static final String ASC_SORT_ORDER = "ASC";
        public static final String DESC_SORT_ORDER = "DESC";

        public static final String AND = "AND";
        public static final String OR = "OR";
        public static final String WHITE_SPACE = " ";

        // Prefix for dynamic named params: :orgId0; :orgId1; ...
        public static final String ORG_ID_SCOPE_PLACEHOLDER_PREFIX = "orgId";

        /**
         * SQL column names for resource sharing status management.
         */
        public static final String COLUMN_NAME_RESIDENT_RESOURCE_ID = "UM_RESIDENT_RESOURCE_ID";
        public static final String COLUMN_NAME_CREATED_TIME = "UM_CREATED_TIME";
    }

    /**
     * Database types related to organization user sharing SQL operations.
     */
    public static final class DBTypes {

        public static final String DB_TYPE_DB2 = "db2";
        public static final String DB_TYPE_MSSQL = "mssql";
        public static final String DB_TYPE_MYSQL = "mysql";
        public static final String DB_TYPE_ORACLE = "oracle";
        public static final String DB_TYPE_POSTGRESQL = "postgresql";
        public static final String DB_TYPE_DEFAULT = "default";
    }

}
