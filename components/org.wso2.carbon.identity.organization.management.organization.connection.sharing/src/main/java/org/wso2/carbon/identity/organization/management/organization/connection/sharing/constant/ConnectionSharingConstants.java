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
                "An unexpected error occurred during the connection sharing operation.");

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
