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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.constant;

/**
 * Constants for connection sharing management.
 */
public class ConnectionSharingConstants {

    public static final String ERROR_PREFIX = "CONNECTION_SHARE-";

    public static final String SHARING_MODE_ATTRIBUTE = "sharingMode";

    // Audit log result values (v2 audit logs are published via LoggerUtils.triggerAuditLogEvent).
    public static final String AUDIT_SUCCESS = "Success";
    public static final String AUDIT_FAILURE = "Failure";

    public static final String ACTION_SELECTIVE_CONNECTION_SHARE = "selective connection share";
    public static final String ACTION_GENERAL_CONNECTION_SHARE = "general connection share";
    public static final String ACTION_SELECTIVE_CONNECTION_UNSHARE = "selective connection unshare";
    public static final String ACTION_GENERAL_CONNECTION_UNSHARE = "general connection unshare";

    public static final String ASYNC_PROCESSING_LOG_TEMPLATE =
            "Processing async %s initiated by user: %s in organization: %s.";

    // Server Configuration properties.
    public static final String ENABLE_IDP_SHARING_PROPERTY = "ConnectionSharing.IDP.Enable";
    public static final String DISABLED_AUTHENTICATORS_PROPERTY =
            "ConnectionSharing.IDP.DisabledAuthenticators.Authenticator";

    /**
     * Error messages for connection sharing management.
     */
    public enum ErrorMessage {

        // Client errors (60xxx).
        ERROR_CODE_NULL_INPUT("60001",
                "Input is null.",
                "The provided input is null and must be provided."),
        ERROR_CODE_CONNECTION_ID_NULL("60002",
                "Connection ID is null.",
                "Connection ID must be provided."),
        ERROR_CODE_CONNECTION_TYPE_NULL("60003",
                "Connection type is null.",
                "Connection type must be provided."),
        ERROR_CODE_NO_HANDLER_FOR_CONNECTION_TYPE("60004",
                "Unsupported connection type.",
                "No handler is registered for the provided connection type."),
        ERROR_CODE_ORG_ID_NULL("60005",
                "Organization ID is null.",
                "Organization ID must be provided."),
        ERROR_CODE_POLICY_NULL("60006",
                "Policy is null.",
                "Policy must be provided."),
        ERROR_CODE_ORGANIZATIONS_NULL("60007",
                "Organizations list is null.",
                "Organizations list must be provided."),
        ERROR_CODE_UNSUPPORTED_POLICY("60008",
                "Policy is not supported.",
                "The provided policy is not supported for the connection sharing operation."),
        ERROR_CODE_CONNECTION_NOT_FOUND("60009",
                "Connection not found.",
                "The specified connection was not found in the organization."),
        ERROR_CODE_UNSUPPORTED_GET_ATTRIBUTE("60010",
                "Unsupported attribute.",
                "The specified attribute is not supported for the get shared organizations operation."),
        ERROR_CODE_CONNECTION_SHARE_CLIENT_ERROR("60011",
                "Connection share client error.",
                "%s"),
        ERROR_CODE_CONNECTION_NOT_SHAREABLE("60012",
                "Connection cannot be shared.",
                "%s"),
        ERROR_CODE_PARENT_NOT_SHARED("60013",
                "Connection is not shared with Immediate parent.",
                "Cannot share the connection with organization: %s without also sharing it with its immediate " +
                        "parent organization: %s."),

        // Server errors (65xxx).
        ERROR_CODE_INTERNAL_ERROR("65001",
                "Internal server error.",
                "An unexpected error occurred during the connection sharing operation."),
        ERROR_CODE_GET_CHILD_ORGS("65002",
                "Failed to retrieve child organizations of the organization: %s.",
                "An error occurred while retrieving child organizations of the initiating organization."),
        ERROR_CODE_GET_SHARED_CONNECTIONS("65003",
                "Failed to retrieve shared connection organizations.",
                "An error occurred while retrieving the organizations a connection has been shared with.");

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
