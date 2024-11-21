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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

/**
 * Constants for organization user sharing.
 */
public class ResourceSharingConstants {

    public static final String USER = "USER";
    public static final String APPLICATION = "APPLICATION";
    public static final String IDP = "IDP";

    public static final String SHARING_ERROR_PREFIX = "RSPM-";

    /**
     * Enum for assignmentType.
     */
    public enum AssignmentType {
        ROLE,
        GROUP
    }

    /**
     * Error messages for organization user sharing management related errors.
     */
    public enum ErrorMessage {

        // Client Errors
        ERROR_CODE_MISSING_MANDATORY_FIELDS("60001",
                "All fields are mandatory and must be provided.",
                "One or more mandatory field is empty."),

        // Server Errors
        ERROR_CODE_RESOURCE_SHARING_POLICY_CREATION_FAILED("65001",
                "Failed to create resource sharing policy.",
                "An error occurred while creating the resource sharing policy in the database."),
        ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED("65002",
                "Failed to delete resource sharing policy.",
                "An error occurred while deleting the resource sharing policy from the database."),
        ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED("65003",
                "Failed to add shared resource attributes for policy ID: %d. Failed attributes: %s",
                "An error occurred while creating the shared resource attributes in the database.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return SHARING_ERROR_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}
