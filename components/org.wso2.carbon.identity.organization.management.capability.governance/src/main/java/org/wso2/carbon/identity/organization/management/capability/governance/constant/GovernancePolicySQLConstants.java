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

/**
 * SQL constants for organization capability governance management.
 */
public class GovernancePolicySQLConstants {

    // Org governance policy.

    public static final String INSERT_ORG_GOVERNANCE_POLICY =
            "INSERT INTO UM_ORG_GOVERNANCE_POLICY " +
                    "(UM_RESOURCE_TYPE, UM_CAPABILITY, UM_GOVERNING_ORG_ID, UM_POLICY) " +
                    "VALUES (:UM_RESOURCE_TYPE;, :UM_CAPABILITY;, :UM_GOVERNING_ORG_ID;, :UM_POLICY;)";

    public static final String SELECT_ORG_GOVERNANCE_POLICIES_BY_GOVERNING_ORG =
            "SELECT UM_ID, UM_RESOURCE_TYPE, UM_CAPABILITY, UM_GOVERNING_ORG_ID, UM_POLICY " +
                    "FROM UM_ORG_GOVERNANCE_POLICY " +
                    "WHERE UM_GOVERNING_ORG_ID = :UM_GOVERNING_ORG_ID;";

    public static final String SELECT_ORG_GOVERNANCE_POLICY_BY_KEY =
            "SELECT UM_ID, UM_RESOURCE_TYPE, UM_CAPABILITY, UM_GOVERNING_ORG_ID, UM_POLICY " +
                    "FROM UM_ORG_GOVERNANCE_POLICY " +
                    "WHERE UM_GOVERNING_ORG_ID = :UM_GOVERNING_ORG_ID; " +
                    "AND UM_CAPABILITY = :UM_CAPABILITY; " +
                    "AND UM_RESOURCE_TYPE = :UM_RESOURCE_TYPE;";

    public static final String UPDATE_ORG_GOVERNANCE_POLICY =
            "UPDATE UM_ORG_GOVERNANCE_POLICY " +
                    "SET UM_POLICY = :UM_POLICY; " +
                    "WHERE UM_GOVERNING_ORG_ID = :UM_GOVERNING_ORG_ID; " +
                    "AND UM_RESOURCE_TYPE = :UM_RESOURCE_TYPE; " +
                    "AND UM_CAPABILITY = :UM_CAPABILITY;";

    public static final String DELETE_ORG_GOVERNANCE_POLICY_BY_KEY =
            "DELETE FROM UM_ORG_GOVERNANCE_POLICY " +
                    "WHERE UM_GOVERNING_ORG_ID = :UM_GOVERNING_ORG_ID; " +
                    "AND UM_RESOURCE_TYPE = :UM_RESOURCE_TYPE; " +
                    "AND UM_CAPABILITY = :UM_CAPABILITY;";

    // Org governance selected organizations.

    public static final String INSERT_ORG_GOVERNANCE_ORG_SELECTED =
            "INSERT INTO UM_ORG_GOVERNANCE_ORG_SELECTED (UM_POLICY_ID, UM_TARGET_ORG_ID) " +
                    "VALUES (:UM_POLICY_ID;, :UM_TARGET_ORG_ID;)";

    public static final String SELECT_ORG_GOVERNANCE_ORG_SELECTED_BY_POLICY =
            "SELECT UM_ID, UM_POLICY_ID, UM_TARGET_ORG_ID " +
                    "FROM UM_ORG_GOVERNANCE_ORG_SELECTED " +
                    "WHERE UM_POLICY_ID = :UM_POLICY_ID;";

    public static final String DELETE_ORG_GOVERNANCE_ORG_SELECTED_BY_POLICY =
            "DELETE FROM UM_ORG_GOVERNANCE_ORG_SELECTED " +
                    "WHERE UM_POLICY_ID = :UM_POLICY_ID;";

    private GovernancePolicySQLConstants() {

    }
}
