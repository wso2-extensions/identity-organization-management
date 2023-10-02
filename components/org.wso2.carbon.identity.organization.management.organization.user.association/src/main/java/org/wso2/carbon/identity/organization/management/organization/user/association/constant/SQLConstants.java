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

package org.wso2.carbon.identity.organization.management.organization.user.association.constant;

/**
 * SQL constants for organization user association management.
 */
public class SQLConstants {

    public static final String CREATE_ORGANIZATION_USER_ASSOCIATION = "INSERT INTO IDN_ORG_USER_ASSOCIATION(" +
            "SHARED_USER_ID, SUB_ORG_ID, REAL_USER_ID, USER_RESIDENT_ORG_ID) VALUES(?, ?, ?, ?)";
    public static final String DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER = "DELETE FROM " +
            "IDN_ORG_USER_ASSOCIATION WHERE SHARED_USER_ID = ? AND USER_RESIDENT_ORG_ID = ?";
    public static final String DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER = "DELETE FROM " +
            "IDN_ORG_USER_ASSOCIATION WHERE REAL_USER_ID = ? AND USER_RESIDENT_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER = "SELECT SHARED_USER_ID, SUB_ORG_ID " +
            "FROM IDN_ORG_USER_ASSOCIATION WHERE REAL_USER_ID = ? AND USER_RESIDENT_ORG_ID = ?";
    public static final String GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG = "SELECT SHARED_USER_ID, " +
            "SUB_ORG_ID FROM IDN_ORG_USER_ASSOCIATION WHERE REAL_USER_ID = ? AND SUB_ORG_ID = ?";

    /**
     * SQL placeholders related to organization user association management SQL operations.
     */
    public static final class SQLPlaceholders {

        public static final String COLUMN_NAME_SHARED_USER_ID = "SHARED_USER_ID";
        public static final String COLUMN_NAME_SUB_ORG_ID = "SUB_ORG_ID";
    }

}
