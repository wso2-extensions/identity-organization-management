/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.constant;

/**
 * Constants defined for the organization resource hierarchy traverse service.
 */
public class OrgResourceHierarchyTraverseConstants {

    private static final String ORGANIZATION_RESOURCE_HIERARCHY_TRAVERSE_ERROR_CODE_PREFIX = "ORHT-";

    /**
     * Private constructor to prevent instantiation of this constant class.
     */
    private OrgResourceHierarchyTraverseConstants() {

    }

    /**
     * Enum which provides error codes and predefined error messages related to the traversal of
     * the organization resource hierarchy. It ensures that error handling within the service is consistent
     * and that detailed error descriptions are available for debugging and troubleshooting.
     */
    public enum ErrorMessages {

        // Server errors.
        ERROR_CODE_EMPTY_ORGANIZATION_ID(
                "65001",
                "Empty organization id.",
                "Organization id cannot be null or empty."),
        ERROR_CODE_INVALID_ANCESTOR_ORGANIZATION_ID_LIST(
                "65002",
                "Invalid ancestor organization id list.",
                "The ancestor organization id list cannot be empty for the organization with id: %s. " +
                        "At least the organization itself should be included in the list."),
        ERROR_CODE_SERVER_ERROR_WHILE_RESOLVING_ANCESTOR_ORGANIZATIONS(
                "65003",
                "Unable to resolve ancestor organizations.",
                "Unexpected server error occurred " +
                        "while resolving ancestor organizations for organization with id: %s."),
        ERROR_CODE_SERVER_ERROR_WHILE_RESOLVING_ANCESTOR_APPLICATIONS(
                "65004",
                "Unable to resolve ancestor applications.",
                "Unexpected server error occurred while resolving ancestor applications for organization " +
                        "with id: %s for application with id: %s.");

        private final String code;
        private final String message;
        private final String description;

        /**
         * Constructor for the ErrorMessages enum.
         * <p>
         * This constructor is used to define each error message with a unique error code, a brief message, and
         * a detailed description.
         *
         * @param code        The unique error code for the message (prefixed with "ORHT-").
         * @param message     A brief message describing the error.
         * @param description A detailed description of the error, often including placeholders for dynamic data.
         */
        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        /**
         * Gets the unique error code for the error message.
         * <p>
         * The error code is prefixed with "ORHT-" to ensure consistency in the error code system.
         *
         * @return The error code prefixed with "ORHT-".
         */
        public String getCode() {

            return ORGANIZATION_RESOURCE_HIERARCHY_TRAVERSE_ERROR_CODE_PREFIX + code;
        }

        /**
         * Gets the brief message for the error.
         * <p>
         * This message provides a short description of the error, usually without context-specific details.
         * It is used for logging or displaying to the user as part of the error response.
         *
         * @return A brief message describing the error.
         */
        public String getMessage() {

            return message;
        }

        /**
         * Gets the detailed description for the error message.
         * <p>
         * This description provides a more detailed explanation of the error and often contains placeholders
         * for dynamic data (e.g., organization or application IDs). It is typically used for logging or debugging.
         *
         * @return A detailed description of the error, including placeholders for dynamic data.
         */
        public String getDescription() {

            return description;
        }
    }
}
