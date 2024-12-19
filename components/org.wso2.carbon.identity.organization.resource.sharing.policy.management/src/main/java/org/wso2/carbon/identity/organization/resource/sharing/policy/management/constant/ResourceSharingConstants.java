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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

/**
 * Constants for organization user sharing.
 */
public class ResourceSharingConstants {

    public static final String SHARING_ERROR_PREFIX = "RSPM-";

    /**
     * Error messages for organization user sharing management related errors.
     */
    public enum ErrorMessage {

        // Client Errors.
        ERROR_CODE_MISSING_MANDATORY_FIELDS("60001",
                "All fields are mandatory and must be provided.",
                "One or more mandatory field is empty."),
        ERROR_CODE_INAPPLICABLE_RESOURCE_TYPE_TO_POLICY("60002",
                "The specified resource type is not supported by the selected sharing policy.",
                "The resource sharing policy does not allow sharing for the provided resource type."),
        ERROR_CODE_NULL_OR_EMPTY_INPUTS("60003",
                "Input values cannot be null or empty.",
                "One or more input values are null or empty, which is not allowed."),

        // Server Errors.
        ERROR_CODE_RESOURCE_SHARING_POLICY_CREATION_FAILED("65001",
                "Failed to create resource sharing policy.",
                "An error occurred while creating the resource sharing policy in the database."),
        ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED("65002",
                "Failed to delete resource sharing policy.",
                "An error occurred while deleting the resource sharing policy from the database."),
        ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED("65003",
                "Failed to add shared resource attributes for policy ID: %d. Failed attributes: %s",
                "An error occurred while creating the shared resource attributes in the database."),
        ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_DELETION_FAILED("65004",
                "Failed to delete shared resource attributes for policy ID: %d. Failed attributes: %s",
                "An error occurred while deleting the shared resource attributes in the database."),
        ERROR_CODE_RESOURCE_SHARING_POLICY_AND_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED("65005",
                "Failed to create resource sharing policy and shared resource attributes.",
                "An error occurred while creating the resource sharing policy and shared resource " +
                        "attributes in the database."),
        ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_FAILED("65006",
                "Failed to retrieve shared resource attributes.",
                "An error occurred while retrieving the shared resource attributes from the database."),
        ERROR_CODE_CREATION_OF_SHARED_RESOURCE_ATTRIBUTE_BUILDER_FAILED("65007",
                "Failed to create shared resource attributes builder.",
                "An error occurred while creating the shared resource attributes builder from the database."),
        ERROR_CODE_RETRIEVING_RESOURCE_SHARING_POLICY_FAILED("65008",
                "Failed to retrieve resource sharing policies.",
                "An error occurred while retrieving the resource sharing policies from the database."),
        ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_BY_RESOURCE_ID_AND_TYPE_FAILED("65009",
                "Failed to retrieve shared resource attributes by resource ID and type.",
                "An error occurred while retrieving shared resource attributes from the database by " +
                        "resource ID and type."),
        ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_BY_RESOURCE_TYPE_AND_ID_FAILED("65010",
                "Failed to delete resource sharing policy by type and ID.",
                "An error occurred while deleting the resource sharing policy by resource type and ID " +
                        "from the database."),
        ERROR_CODE_SHARED_RESOURCE_ATTRIBUTE_DELETION_BY_ATTRIBUTE_TYPE_AND_ID_FAILED("65011",
                "Failed to delete shared resource attribute by type and ID.",
                "An error occurred while deleting the shared resource attribute by attribute type and ID " +
                        "from the database.");

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

        @Override
        public String toString() {

            return String.format("ErrorMessage{code='%s', message='%s', description='%s'}",
                    getCode(), getMessage(), getDescription());
        }
    }
}
