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

/**
 * SQL constants for organization user sharing.
 */
public class SQLConstants {

    public static final String CREATE_ORGANIZATION_USER_ASSOCIATION = "INSERT INTO UM_ORG_USER_ASSOCIATION(" +
            "UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID) VALUES(?, ?, ?, ?)";
    public static final String CREATE_ORGANIZATION_USER_ASSOCIATION_EXTENDED = "INSERT INTO UM_ORG_USER_ASSOCIATION(" +
            "UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, " +
            "UM_ASSOCIATION_INITIATED_ORG_ID, UM_ASSOCIATION_TYPE) VALUES(?, ?, ?, ?, ?, ?)";
    public static final String DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER = "DELETE FROM " +
            "UM_ORG_USER_ASSOCIATION WHERE UM_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ?";
    public static final String DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_ROOT_USER = "DELETE FROM " +
            "UM_ORG_USER_ASSOCIATION WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER = "SELECT UM_ID, UM_USER_ID, UM_ORG_ID, " +
            "UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE" +
            "FROM UM_ORG_USER_ASSOCIATION WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER_BY_SHARED_TYPE = "SELECT UM_ID, " +
            "UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE " +
            "FROM UM_ORG_USER_ASSOCIATION WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ASSOCIATED_ORG_ID = ? AND " +
            "UM_SHARED_TYPE = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATION_FOR_ROOT_USER_IN_ORG = "SELECT UM_ID, UM_USER_ID, " +
            "UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE FROM UM_ORG_USER_ASSOCIATION " +
            "WHERE UM_ASSOCIATED_USER_ID = ? AND UM_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER = "SELECT UM_ID, UM_USER_ID, " +
            "UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID, UM_SHARED_TYPE FROM UM_ORG_USER_ASSOCIATION " +
            "WHERE UM_USER_ID = ? AND UM_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER_BY_USER_ID =
            "SELECT UM_USER_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID " +
                    "FROM UM_ORG_USER_ASSOCIATION WHERE UM_USER_ID = ?";
    public static final String CHECK_COLUMN_EXISTENCE_IN_TABLE =
            "SELECT COUNT(*) AS count FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
    public static final String GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG_HEAD =
            "SELECT r.UM_USER_NAME FROM UM_HYBRID_USER_ROLE r "
                    + "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS p "
                    + "ON r.UM_ID = p.UM_HYBRID_USER_ROLE_ID AND r.UM_TENANT_ID = p.UM_HYBRID_USER_ROLE_TENANT_ID "
                    + "WHERE r.UM_ROLE_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_ROLE_ID + "; AND r.UM_USER_NAME IN (";
    public static final String GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG_TAIL =
            "); AND r.UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID +
                    "; AND p.UM_PERMITTED_ORG_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID +
                    "; AND r.UM_EDIT_OPERATION = 'DELETE';";
    public static final String GET_SHARED_USER_ROLES_HEAD =
            "SELECT DISTINCT hr.UM_UUID FROM UM_HYBRID_ROLE hr "
                    + "INNER JOIN UM_HYBRID_USER_ROLE ur ON hr.UM_ID = ur.UM_ROLE_ID AND hr.UM_TENANT_ID = ur.UM_TENANT_ID "
                    + "INNER JOIN UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS rep "
                    + "ON ur.UM_ID = rep.UM_HYBRID_USER_ROLE_ID AND ur.UM_TENANT_ID = rep.UM_HYBRID_USER_ROLE_TENANT_ID "
                    + "WHERE hr.UM_UUID IN (";

    public static final String GET_SHARED_USER_ROLES_TAIL =
            ") AND hr.UM_TENANT_ID = :" + SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID + ";";


    public static final String DEFAULT_VALUE_NOT_SPECIFIED = "NOT_SPECIFIED";

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
        public static final String COLUMN_NAME_UM_PERMITTED_ORG_ID = "UM_PERMITTED_ORG_ID";
        public static final String COLUMN_NAME_UM_ID = "UM_ID";
        public static final String COLUMN_NAME_UM_SHARED_TYPE = "UM_SHARED_TYPE";
        public static final String COLUMN_NAME_UM_UUID = "UM_UUID";
    }

}
