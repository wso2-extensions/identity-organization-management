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
 * Constants for organization capability governance management.
 */
public class GovernancePolicyConstants {

    public static final String ERROR_PREFIX = "OCGM-";

    // Column names.
    public static final String COL_UM_ID = "UM_ID";
    public static final String COL_UM_POLICY_ID = "UM_POLICY_ID";
    public static final String COL_UM_SELECTED_ORG_ID = "UM_SELECTED_ORG_ID";
    public static final String COL_UM_POLICY = "UM_POLICY";
    public static final String COL_UM_CAPABILITY = "UM_CAPABILITY";
    public static final String COL_UM_RESOURCE_TYPE = "UM_RESOURCE_TYPE";
    public static final String COL_UM_GOVERNING_ORG_ID = "UM_GOVERNING_ORG_ID";

    /**
     * Error messages.
     */
    public enum ErrorMessage {

        // Client errors.
        ERROR_CODE_POLICY_NOT_FOUND("60001",
                "Policy not found.",
                "No governance policy found for the given governing org, resource type, and capability."),
        ERROR_CODE_POLICY_ALREADY_EXISTS("60002",
                "Policy already exists.",
                "A governance policy already exists for the given governing org, resource type, and capability."),
        ERROR_CODE_POLICY_MANAGEMENT_NOT_PERMITTED("60003",
                "Policy management not permitted.",
                "Only the primary organization is allowed to manage governance policies."),
        ERROR_CODE_INVALID_SELECTED_ORG("60004",
                "Invalid selected organization.",
                "Organization '%s' is not a descendant of the governing organization."),
        ERROR_CODE_POLICY_EVALUATION_NOT_SUPPORTED("60005",
                "Policy evaluation not permitted.",
                "Governance policy evaluation is only applicable to sub-organizations."),

        // Server errors.
        ERROR_CODE_ADD_ORG_POLICY_FAILED("65001",
                "Failed to add org governance policy.",
                "An error occurred while adding the org governance policy to the database."),
        ERROR_CODE_GET_ORG_POLICY_FAILED("65002",
                "Failed to retrieve org governance policy.",
                "An error occurred while retrieving the org governance policy from the database."),
        ERROR_CODE_UPDATE_ORG_POLICY_FAILED("65003",
                "Failed to update org governance policy.",
                "An error occurred while updating the org governance policy in the database."),
        ERROR_CODE_DELETE_ORG_POLICY_FAILED("65004",
                "Failed to delete org governance policy.",
                "An error occurred while deleting the org governance policy from the database."),
        ERROR_CODE_HIERARCHY_TRAVERSAL_FAILED("65005",
                "Hierarchy traversal failed.",
                "An error occurred while traversing the organization hierarchy."),
        ERROR_CODE_ORG_CHECK_FAILED("65006",
                "Organization check failed.",
                "An error occurred while checking whether given organization is a sub organization."),
        ERROR_CODE_GET_DESCENDANT_ORGS_FAILED("65007",
                "Failed to retrieve descendant organizations.",
                "An error occurred while retrieving descendant organizations of the governing organization.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return ERROR_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }

        @Override
        public String toString() {

            return String.format("ErrorMessage{code='%s', message='%s', description='%s'}",
                    getCode(), getMessage(), getDescription());
        }
    }

    private GovernancePolicyConstants() {

    }
}
