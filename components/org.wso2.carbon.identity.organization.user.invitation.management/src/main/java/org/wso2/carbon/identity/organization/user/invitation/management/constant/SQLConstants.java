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

package org.wso2.carbon.identity.organization.user.invitation.management.constant;

/**
 * SQL constants for organization user invitation management.
 */
public class SQLConstants {

    /**
     * SQL queries related to organization user invitation management.
     */
    public static final class SQLQueries {

        public static final String STORE_INVITATION = "INSERT INTO IDN_ORG_USER_INVITATION(INVITATION_ID, " +
                "CONFIRMATION_CODE, USER_NAME, EMAIL, DOMAIN_NAME, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, " +
                "EXPIRED_AT, USER_REDIRECT_URL) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        public static final String STORE_ROLE_ASSIGNMENTS = "INSERT INTO IDN_ORG_USER_INVITE_ROLE_ASSIGNMENT(" +
                "INVITATION_ID, APPLICATION_ID, ROLE_ID) VALUES(?, ?, ?)";
        public static final String GET_INVITATION_FROM_CONFIRMATION_CODE = "SELECT INVITATION_ID, " +
                "CONFIRMATION_CODE, USER_NAME, EMAIL, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, " +
                "EXPIRED_AT, USER_REDIRECT_URL FROM IDN_ORG_USER_INVITATION WHERE CONFIRMATION_CODE = ?";
        public static final String GET_INVITATION_ID_FROM_CONFIRMATION_CODE = "SELECT INVITATION_ID FROM " +
                "IDN_ORG_USER_INVITATION WHERE CONFIRMATION_CODE = ?";
        public static final String GET_INVITATIONS_BY_INVITED_ORG_ID = "SELECT INVITATION_ID, CONFIRMATION_CODE, " +
                "USER_NAME, EMAIL, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, EXPIRED_AT, USER_REDIRECT_URL " +
                "FROM IDN_ORG_USER_INVITATION WHERE INVITED_ORG_ID = ?";
        public static final String GET_INVITATIONS_BY_INVITED_ORG_ID_WITH_STATUS_FILTER_PENDING =
                "SELECT INVITATION_ID, CONFIRMATION_CODE, " +
                "USER_NAME, EMAIL, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, EXPIRED_AT, USER_REDIRECT_URL " +
                "FROM IDN_ORG_USER_INVITATION WHERE INVITED_ORG_ID = ? AND EXPIRED_AT > CURRENT_TIMESTAMP()";
        public static final String GET_INVITATIONS_BY_INVITED_ORG_ID_WITH_STATUS_FILTER_EXPIRED =
                "SELECT INVITATION_ID, CONFIRMATION_CODE, " +
                "USER_NAME, EMAIL, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, EXPIRED_AT, USER_REDIRECT_URL " +
                "FROM IDN_ORG_USER_INVITATION WHERE INVITED_ORG_ID = ? AND EXPIRED_AT <= CURRENT_TIMESTAMP";
        public static final String GET_INVITATION_BY_INVITATION_ID = "SELECT INVITATION_ID, CONFIRMATION_CODE, " +
                "USER_NAME, DOMAIN_NAME, EMAIL, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, EXPIRED_AT, " +
                "USER_REDIRECT_URL FROM IDN_ORG_USER_INVITATION WHERE INVITATION_ID = ?";
        public static final String GET_ROLE_ASSIGNMENTS_BY_INVITATION_ID = "SELECT INVITATION_ID, APPLICATION_ID, " +
                "ROLE_ID FROM IDN_ORG_USER_INVITE_ROLE_ASSIGNMENT WHERE INVITATION_ID = ?";
        public static final String DELETE_INVITATION_BY_INVITATION_ID = "DELETE FROM IDN_ORG_USER_INVITATION " +
                "WHERE INVITATION_ID = ?";
        public static final String DELETE_ROLE_ASSIGNMENTS_BY_INVITATION_ID = "DELETE FROM " +
                "IDN_ORG_USER_INVITE_ROLE_ASSIGNMENT WHERE INVITATION_ID = ?";
        public static final String GET_ACTIVE_INVITATION_BY_USER = "SELECT INVITATION_ID, CONFIRMATION_CODE, " +
                "USER_NAME, DOMAIN_NAME, EMAIL, USER_ORG_ID, INVITED_ORG_ID, STATUS, CREATED_AT, EXPIRED_AT, " +
                "USER_REDIRECT_URL FROM IDN_ORG_USER_INVITATION WHERE USER_NAME = ? AND DOMAIN_NAME = ? AND " +
                "USER_ORG_ID = ? AND INVITED_ORG_ID = ? AND EXPIRED_AT > CURRENT_TIMESTAMP";
        public static final String CREATE_USER_ASSOCIATION_TO_ORG = "INSERT INTO IDN_ORG_USER_ASSOCIATION(" +
                "SHARED_USER_ID, SUB_ORG_ID, REAL_USER_ID, USER_RESIDENT_ORG_ID) VALUES(?, ?, ?, ?)";
        public static final String DELETE_ORG_ASSOCIATION_FOR_SHARED_USER = "DELETE FROM " +
                "IDN_ORG_USER_ASSOCIATION WHERE SHARED_USER_ID = ? AND USER_RESIDENT_ORG_ID = ?";
        public static final String DELETE_ALL_ORG_ASSOCIATIONS_FOR_SHARED_USER = "DELETE FROM " +
                "IDN_ORG_USER_ASSOCIATION WHERE REAL_USER_ID = ? AND USER_RESIDENT_ORG_ID = ?";
        public static final String GET_ALL_ORG_ASSOCIATIONS_FOR_SHARED_USER = "SELECT SHARED_USER_ID, SUB_ORG_ID " +
                "FROM IDN_ORG_USER_ASSOCIATION WHERE REAL_USER_ID = ? AND USER_RESIDENT_ORG_ID = ?";
        public static final String GET_ORG_ASSOCIATIONS_FOR_ACTUAL_USER = "SELECT SHARED_USER_ID, SUB_ORG_ID " +
                "FROM IDN_ORG_USER_ASSOCIATION WHERE REAL_USER_ID = ? AND SUB_ORG_ID = ?";
    }

    /**
     * SQL placeholders related to organization user invitation management SQL operations.
     */
    public static final class SQLPlaceholders {

        public static final String COLUMN_NAME_INVITATION_ID = "INVITATION_ID";
        public static final String COLUMN_NAME_CONFIRMATION_CODE = "CONFIRMATION_CODE";
        public static final String COLUMN_NAME_USER_NAME = "USER_NAME";
        public static final String COLUMN_NAME_DOMAIN = "DOMAIN_NAME";
        public static final String COLUMN_NAME_EMAIL = "EMAIL";
        public static final String COLUMN_NAME_USER_ORG_ID = "USER_ORG_ID";
        public static final String COLUMN_NAME_INVITED_ORG_ID = "INVITED_ORG_ID";
        public static final String COLUMN_NAME_STATUS = "STATUS";
        public static final String COLUMN_NAME_CREATED_AT = "CREATED_AT";
        public static final String COLUMN_NAME_EXPIRED_AT = "EXPIRED_AT";
        public static final String COLUMN_NAME_REDIRECT_URL = "USER_REDIRECT_URL";
        public static final String COLUMN_NAME_APP_ID = "APPLICATION_ID";
        public static final String COLUMN_NAME_ROLE_ID = "ROLE_ID";
        public static final String COLUMN_NAME_SHARED_USER_ID = "SHARED_USER_ID";
        public static final String COLUMN_NAME_SUB_ORG_ID = "SUB_ORG_ID";
    }
}
