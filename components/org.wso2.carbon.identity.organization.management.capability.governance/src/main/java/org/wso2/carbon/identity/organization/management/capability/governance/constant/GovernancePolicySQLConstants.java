/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.capability.governance.constant;

import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_ALLOW_OVERRIDE;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_CAPABILITY;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_GOVERNING_ORG_ID;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_ID;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_POLICY_ID;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_POLICY_TYPE;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_RESOURCE_ID;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_RESOURCE_OWNER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_RESOURCE_TYPE;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.COL_UM_TARGET_ORG_ID;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.TABLE_ORG_GOVERNANCE_ORG_SELECTED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.TABLE_ORG_GOVERNANCE_POLICY;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.TABLE_RESOURCE_GOVERNANCE_ORG_SELECTED;
import static org.wso2.carbon.identity.organization.management.capability.governance.constant.GovernancePolicyConstants.TABLE_RESOURCE_GOVERNANCE_POLICY;

/**
 * SQL constants for organization capability governance management.
 */
public class GovernancePolicySQLConstants {

    // -------------------------------------------------------------------------
    // UM_ORG_GOVERNANCE_POLICY
    // -------------------------------------------------------------------------

    public static final String INSERT_ORG_GOVERNANCE_POLICY =
            "INSERT INTO " + TABLE_ORG_GOVERNANCE_POLICY + " (" +
                    COL_UM_RESOURCE_TYPE + ", " + COL_UM_CAPABILITY + ", " +
                    COL_UM_GOVERNING_ORG_ID + ", " + COL_UM_POLICY_TYPE + ", " + COL_UM_ALLOW_OVERRIDE + ") " +
                    "VALUES (:" + COL_UM_RESOURCE_TYPE + ";, :" + COL_UM_CAPABILITY + ";, " +
                    ":" + COL_UM_GOVERNING_ORG_ID + ";, :" + COL_UM_POLICY_TYPE + ";, :" + COL_UM_ALLOW_OVERRIDE + ";)";

    public static final String SELECT_ORG_GOVERNANCE_POLICIES_BY_GOVERNING_ORG =
            "SELECT " + COL_UM_ID + ", " + COL_UM_RESOURCE_TYPE + ", " + COL_UM_CAPABILITY + ", " +
                    COL_UM_GOVERNING_ORG_ID + ", " + COL_UM_POLICY_TYPE + ", " + COL_UM_ALLOW_OVERRIDE +
                    " FROM " + TABLE_ORG_GOVERNANCE_POLICY +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";";

    public static final String SELECT_ORG_GOVERNANCE_POLICY_BY_GOVERNING_ORG_AND_CAPABILITY =
            "SELECT " + COL_UM_ID + ", " + COL_UM_RESOURCE_TYPE + ", " + COL_UM_CAPABILITY + ", " +
                    COL_UM_GOVERNING_ORG_ID + ", " + COL_UM_POLICY_TYPE + ", " + COL_UM_ALLOW_OVERRIDE +
                    " FROM " + TABLE_ORG_GOVERNANCE_POLICY +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";" +
                    " AND " + COL_UM_CAPABILITY + " = :" + COL_UM_CAPABILITY + ";" +
                    " AND " + COL_UM_RESOURCE_TYPE + " = :" + COL_UM_RESOURCE_TYPE + ";";

    public static final String UPDATE_ORG_GOVERNANCE_POLICY =
            "UPDATE " + TABLE_ORG_GOVERNANCE_POLICY + " SET " +
                    COL_UM_POLICY_TYPE + " = :" + COL_UM_POLICY_TYPE + ";, " +
                    COL_UM_ALLOW_OVERRIDE + " = :" + COL_UM_ALLOW_OVERRIDE + ";" +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";" +
                    " AND " + COL_UM_RESOURCE_TYPE + " = :" + COL_UM_RESOURCE_TYPE + ";" +
                    " AND " + COL_UM_CAPABILITY + " = :" + COL_UM_CAPABILITY + ";";

    public static final String DELETE_ORG_GOVERNANCE_POLICY_BY_KEY =
            "DELETE FROM " + TABLE_ORG_GOVERNANCE_POLICY +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";" +
                    " AND " + COL_UM_RESOURCE_TYPE + " = :" + COL_UM_RESOURCE_TYPE + ";" +
                    " AND " + COL_UM_CAPABILITY + " = :" + COL_UM_CAPABILITY + ";";

    // -------------------------------------------------------------------------
    // UM_ORG_GOVERNANCE_ORG_SELECTED
    // -------------------------------------------------------------------------

    public static final String INSERT_ORG_GOVERNANCE_ORG_SELECTED =
            "INSERT INTO " + TABLE_ORG_GOVERNANCE_ORG_SELECTED + " (" +
                    COL_UM_POLICY_ID + ", " + COL_UM_TARGET_ORG_ID + ", " + COL_UM_ALLOW_OVERRIDE + ") " +
                    "VALUES (:" + COL_UM_POLICY_ID + ";, :" + COL_UM_TARGET_ORG_ID + ";, " +
                    ":" + COL_UM_ALLOW_OVERRIDE + ";)";

    public static final String SELECT_ORG_GOVERNANCE_ORG_SELECTED_BY_POLICY =
            "SELECT " + COL_UM_ID + ", " + COL_UM_POLICY_ID + ", " + COL_UM_TARGET_ORG_ID + ", " +
                    COL_UM_ALLOW_OVERRIDE +
                    " FROM " + TABLE_ORG_GOVERNANCE_ORG_SELECTED +
                    " WHERE " + COL_UM_POLICY_ID + " = :" + COL_UM_POLICY_ID + ";";

    public static final String DELETE_ORG_GOVERNANCE_ORG_SELECTED_BY_POLICY =
            "DELETE FROM " + TABLE_ORG_GOVERNANCE_ORG_SELECTED +
                    " WHERE " + COL_UM_POLICY_ID + " = :" + COL_UM_POLICY_ID + ";";

    // -------------------------------------------------------------------------
    // UM_RESOURCE_GOVERNANCE_POLICY
    // -------------------------------------------------------------------------

    public static final String INSERT_RESOURCE_GOVERNANCE_POLICY =
            "INSERT INTO " + TABLE_RESOURCE_GOVERNANCE_POLICY + " (" +
                    COL_UM_RESOURCE_TYPE + ", " + COL_UM_RESOURCE_ID + ", " +
                    COL_UM_RESOURCE_OWNER_ORG_ID + ", " + COL_UM_GOVERNING_ORG_ID + ", " +
                    COL_UM_CAPABILITY + ", " + COL_UM_POLICY_TYPE + ", " + COL_UM_ALLOW_OVERRIDE + ") " +
                    "VALUES (:" + COL_UM_RESOURCE_TYPE + ";, :" + COL_UM_RESOURCE_ID + ";, " +
                    ":" + COL_UM_RESOURCE_OWNER_ORG_ID + ";, :" + COL_UM_GOVERNING_ORG_ID + ";, " +
                    ":" + COL_UM_CAPABILITY + ";, :" + COL_UM_POLICY_TYPE + ";, :" + COL_UM_ALLOW_OVERRIDE + ";)";

    public static final String SELECT_RESOURCE_GOVERNANCE_POLICIES_BY_GOVERNING_ORG =
            "SELECT " + COL_UM_ID + ", " + COL_UM_RESOURCE_TYPE + ", " + COL_UM_RESOURCE_ID + ", " +
                    COL_UM_RESOURCE_OWNER_ORG_ID + ", " + COL_UM_GOVERNING_ORG_ID + ", " +
                    COL_UM_CAPABILITY + ", " + COL_UM_POLICY_TYPE + ", " + COL_UM_ALLOW_OVERRIDE +
                    " FROM " + TABLE_RESOURCE_GOVERNANCE_POLICY +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";";

    public static final String SELECT_RESOURCE_GOVERNANCE_POLICY_BY_GOVERNING_ORG_AND_CAPABILITY =
            "SELECT " + COL_UM_ID + ", " + COL_UM_RESOURCE_TYPE + ", " + COL_UM_RESOURCE_ID + ", " +
                    COL_UM_RESOURCE_OWNER_ORG_ID + ", " + COL_UM_GOVERNING_ORG_ID + ", " +
                    COL_UM_CAPABILITY + ", " + COL_UM_POLICY_TYPE + ", " + COL_UM_ALLOW_OVERRIDE +
                    " FROM " + TABLE_RESOURCE_GOVERNANCE_POLICY +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";" +
                    " AND " + COL_UM_CAPABILITY + " = :" + COL_UM_CAPABILITY + ";" +
                    " AND " + COL_UM_RESOURCE_TYPE + " = :" + COL_UM_RESOURCE_TYPE + ";" +
                    " AND " + COL_UM_RESOURCE_ID + " = :" + COL_UM_RESOURCE_ID + ";";

    public static final String UPDATE_RESOURCE_GOVERNANCE_POLICY =
            "UPDATE " + TABLE_RESOURCE_GOVERNANCE_POLICY + " SET " +
                    COL_UM_POLICY_TYPE + " = :" + COL_UM_POLICY_TYPE + ";, " +
                    COL_UM_ALLOW_OVERRIDE + " = :" + COL_UM_ALLOW_OVERRIDE + ";" +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";" +
                    " AND " + COL_UM_RESOURCE_TYPE + " = :" + COL_UM_RESOURCE_TYPE + ";" +
                    " AND " + COL_UM_RESOURCE_ID + " = :" + COL_UM_RESOURCE_ID + ";" +
                    " AND " + COL_UM_CAPABILITY + " = :" + COL_UM_CAPABILITY + ";";

    public static final String DELETE_RESOURCE_GOVERNANCE_POLICY_BY_KEY =
            "DELETE FROM " + TABLE_RESOURCE_GOVERNANCE_POLICY +
                    " WHERE " + COL_UM_GOVERNING_ORG_ID + " = :" + COL_UM_GOVERNING_ORG_ID + ";" +
                    " AND " + COL_UM_RESOURCE_TYPE + " = :" + COL_UM_RESOURCE_TYPE + ";" +
                    " AND " + COL_UM_RESOURCE_ID + " = :" + COL_UM_RESOURCE_ID + ";" +
                    " AND " + COL_UM_CAPABILITY + " = :" + COL_UM_CAPABILITY + ";";

    // -------------------------------------------------------------------------
    // UM_RESOURCE_GOVERNANCE_ORG_SELECTED
    // -------------------------------------------------------------------------

    public static final String INSERT_RESOURCE_GOVERNANCE_ORG_SELECTED =
            "INSERT INTO " + TABLE_RESOURCE_GOVERNANCE_ORG_SELECTED + " (" +
                    COL_UM_POLICY_ID + ", " + COL_UM_TARGET_ORG_ID + ", " + COL_UM_ALLOW_OVERRIDE + ") " +
                    "VALUES (:" + COL_UM_POLICY_ID + ";, :" + COL_UM_TARGET_ORG_ID + ";, " +
                    ":" + COL_UM_ALLOW_OVERRIDE + ";)";

    public static final String SELECT_RESOURCE_GOVERNANCE_ORG_SELECTED_BY_POLICY =
            "SELECT " + COL_UM_ID + ", " + COL_UM_POLICY_ID + ", " + COL_UM_TARGET_ORG_ID + ", " +
                    COL_UM_ALLOW_OVERRIDE +
                    " FROM " + TABLE_RESOURCE_GOVERNANCE_ORG_SELECTED +
                    " WHERE " + COL_UM_POLICY_ID + " = :" + COL_UM_POLICY_ID + ";";

    public static final String DELETE_RESOURCE_GOVERNANCE_ORG_SELECTED_BY_POLICY =
            "DELETE FROM " + TABLE_RESOURCE_GOVERNANCE_ORG_SELECTED +
                    " WHERE " + COL_UM_POLICY_ID + " = :" + COL_UM_POLICY_ID + ";";

    private GovernancePolicySQLConstants() {

    }
}
