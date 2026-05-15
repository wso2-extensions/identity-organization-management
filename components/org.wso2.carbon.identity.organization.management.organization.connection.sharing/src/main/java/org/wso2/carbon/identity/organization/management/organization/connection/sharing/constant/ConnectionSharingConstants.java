/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant;

/**
 * Constants for connection sharing management.
 */
public class ConnectionSharingConstants {

    public static final String ERROR_PREFIX = "CSH-";

    public static final String CONNECTION_IDS = "CONNECTION_IDS";
    public static final String CONNECTION_NAMES = "CONNECTION_NAMES";
    public static final String SHARING_MODE_ATTRIBUTE = "sharingMode";

    public static final String AUDIT_MESSAGE =
            "Initiator : %s | Action : %s | Target : %s | Data : { %s } | Result : %s ";
    public static final String AUDIT_SUCCESS = "Success";
    public static final String AUDIT_FAILURE = "Failure";

    public static final String ACTION_SELECTIVE_CONNECTION_SHARE = "selective connection share";
    public static final String ACTION_GENERAL_CONNECTION_SHARE = "general connection share";
    public static final String ACTION_SELECTIVE_CONNECTION_UNSHARE = "selective connection unshare";
    public static final String ACTION_GENERAL_CONNECTION_UNSHARE = "general connection unshare";

    public static final String ASYNC_PROCESSING_LOG_TEMPLATE =
            "Processing async %s initiated by user: %s in organization: %s.";
    public static final String LOG_WARN_SKIP_ORG_SHARE_MESSAGE =
            "Skipping connection share for organizations that are not immediate children: %s";

    /**
     * Error messages for connection sharing management.
     */
    public enum ErrorMessage {

        ERROR_CODE_NULL_INPUT("10001",
                "Input is null.",
                "The provided input is null and must be provided."),
        ERROR_CODE_CONNECTION_ID_NULL("10002",
                "Connection ID is null.",
                "Connection ID must be provided."),
        ERROR_CODE_ORG_ID_NULL("10003",
                "Organization ID is null.",
                "Organization ID must be provided."),
        ERROR_CODE_POLICY_NULL("10004",
                "Policy is null.",
                "Policy must be provided."),
        ERROR_CODE_ORGANIZATIONS_NULL("10005",
                "Organizations list is null.",
                "Organizations list must be provided."),
        ERROR_CODE_CONNECTION_CRITERIA_NULL("10006",
                "Connection criteria is null.",
                "Connection criteria must be provided."),
        ERROR_CODE_INTERNAL_ERROR("10007",
                "Internal server error.",
                "An unexpected error occurred during the connection sharing operation."),
        ERROR_CODE_CONNECTION_NOT_FOUND("10008",
                "Connection not found.",
                "The specified connection was not found in the organization."),
        ERROR_CODE_GET_CHILD_ORGS("10009",
                "Failed to retrieve child organizations.",
                "An error occurred while retrieving child organizations of the initiating organization."),
        ERROR_CODE_GET_SHARED_CONNECTIONS("10010",
                "Failed to retrieve shared connection organizations.",
                "An error occurred while retrieving the organizations a connection has been shared with."),
        ERROR_CODE_UNSUPPORTED_GET_ATTRIBUTE("10011",
                "Unsupported attribute.",
                "The specified attribute is not supported for the get shared organizations operation.");

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
    }
}
