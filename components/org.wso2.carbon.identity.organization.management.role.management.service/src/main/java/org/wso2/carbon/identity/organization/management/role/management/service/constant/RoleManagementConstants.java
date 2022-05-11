/*
 *
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.management.service.constant;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains the constant of Role Management.
 */
public class RoleManagementConstants {

    public static final String ORGANIZATION_RESOURCE_PATH = "/v1.0/organizations/%s";
    public static final String TENANT_CONTEXT_PATH_COMPONENT = "/t/%s";
    public static final String ORGANIZATION_CONTEXT_PATH_COMPONENT = "/o/%s";
    public static final String ORGANIZATION_MANAGEMENT_API_PATH_COMPONENT = "/api/identity/organization-mgt";

    public static final String ROLE_ACTION = "ui.execute";

    public static final String PATCH_OP_ADD = "ADD";
    public static final String PATCH_OP_REMOVE = "REMOVE";
    public static final String PATCH_OP_REPLACE = "REPLACE";

    public static final String GROUPS = "GROUPS";
    public static final String USERS = "USERS";
    public static final String PERMISSIONS = "PERMISSIONS";
    public static final String DISPLAY_NAME = "displayName";
    public static final String AND = "and";

    public static final String FILTER_ID_PLACEHOLDER = "FILTER_ID_%d";

    public static final String ROLE_ID_FIELD = "id";
    public static final String ROLE_NAME_FIELD = "name";

    public static final String COMMA_SEPARATOR = ",";

    public static final Map<String, String> ATTRIBUTE_COLUMN_MAP = Stream.of(new String[][]{
            {"name", "UM_ROLE_NAME"},
            {"id", "UM_ROLE_ID"},
            {"orgId", "UM_ORG_ID"}
    }).collect(Collectors.collectingAndThen(Collectors.toMap(data -> data[0], data -> data[1]),
            Collections::<String, String>unmodifiableMap));


    /**
     * Error messages for Organization Management - Role Management.
     */
    public enum ErrorMessages {

        // Client Errors (ORM-60200 - ORM-60999)
        ERROR_CODE_INVALID_REQUEST_BODY("ORM-60201", "Invalid request body.",
                "The format of the provided request body is incorrect."),
        ERROR_REMOVING_REQUIRED_ATTRIBUTE("ORM-60202", "Cannot remove the required attribute %s.",
                "Cannot remove the required attribute %s with operation %s."),
        ERROR_REMOVING_INVALID_ATTRIBUTE("ORM-60203", "Invalid attribute %s.",
                "Invalid attribute %s for operation %s."),
        ERROR_ADDING_REQUIRED_ATTRIBUTE("ORM-60204",
                "Cannot add the required attribute %s. This should be added at the initialization.",
                "Cannot add the required attribute %s with operation %s."),
        ERROR_ADDING_INVALID_ATTRIBUTE("ORM-60205", "Invalid attribute %s.",
                "Invalid attribute %s for operation %s."),
        ERROR_DISPLAY_NAME_MULTIPLE_VALUES("ORM-60206", "The display name cannot have multiple values.",
                "The display name should have single value."),
        ERROR_CODE_INVALID_FILTER_FORMAT("ORM-60207", "Unable to retrieve roles.", "Invalid" +
                " format used for filtering."),
        ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION("ORM-60208", "Unable to retrieve organizations.", "Invalid " +
                "cursor used for pagination."),
        ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE("ORM-60209", "Unsupported filter attribute.",
                "The filter attribute '%s' is not supported."),
        ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER("ORM-60210", "Unsupported filter.",
                "The complex query used for filtering is not supported."),
        ERROR_CODE_ERROR_BUILDING_ROLE_URI("ORM-65011", "Unable to build create role URI.",
                "Server encountered an error while building URL for role with roleId %s"),
        ERROR_CODE_ERROR_BUILDING_GROUP_URI("ORM-65012", "Unable to build create group URI.",
                "Server encountered an error while building URL for group with groupId %s"),
        ERROR_CODE_ERROR_BUILDING_USER_URI("ORM-65013", "Unable to build create user URI.",
                "Server encountered an error while building URL for user with userId %s"),
        ERROR_CODE_INVALID_PAGINATION_PARAMETER_NEGATIVE_LIMIT("ORM-65014",
                "Invalid pagination parameters.", "The 'limit' should not be negative."),
        ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL("65015", "Unable to retrieve the roles.",
                "Server encountered an error while building paginated response URL."),
        // Server Errors (ORM-65200 - ORM-65999)
        ERROR_ADDING_ROLE_TO_ORGANIZATION("ORM-65201", "Error adding role to the organization : %s.",
                "Server encountered an error while adding a role to an organization."),
        ERROR_ADDING_GROUP_TO_ROLE("ORM-65202", "Error adding group(s) to role: %s.",
                "Server encountered an error while adding a group to role."),
        ERROR_ADDING_USER_TO_ROLE("ORM-65603", "Error adding user(s) to role: %s.",
                "Server encountered an error while adding a user to role."),
        ERROR_ADDING_PERMISSION_TO_ROLE("ORM-65204", "Error adding permission(s) to role %s.",
                "Server encountered an error while adding a permission to role"),
        ERROR_GETTING_ROLE_FROM_ID("ORM-65205", "Error getting role from role Id: %s",
                "Server encountered an error while retrieving role from role id."),
        ERROR_GETTING_USERS_USING_ROLE_ID("ORM-65206", "Error getting users of role Id: %s",
                "Server encountered an error while retrieving user(s) from role id."),
        ERROR_GETTING_GROUPS_USING_ROLE_ID("ORM-65207", "Error getting users of role Id: %s",
                "Server encountered an error while retrieving user(s) from role id."),
        ERROR_GETTING_PERMISSIONS_USING_ROLE_ID("ORM-65208", "Error getting permissions of role ID: %s",
                "Server encountered an error while retrieving permission(s) from role id."),
        ERROR_GETTING_ROLES_FROM_ORGANIZATION("ORM-65209", "Error getting roles from organization: %s",
                "Server encountered an error while retrieving role(s) from organization id."),
        ERROR_PATCHING_ROLE("ORM-65210", "Error patching a role from organization: %s",
                "Server encountered an error while patching a role."),
        ERROR_REMOVING_USERS_FROM_ROLE("ORM-65211", "Error removing a user from role: %s",
                "Server encountered an error while removing users from a role"),
        ERROR_REMOVING_GROUPS_FROM_ROLE("ORM-65212", "Error removing a group from role: %s",
                "Server encountered an error while removing groups from a role"),
        ERROR_REMOVING_PERMISSIONS_FROM_ROLE("ORM-65213", "Error removing a permission from role: %s",
                "Server encountered an error while removing permissions from a role."),
        ERROR_REMOVING_ROLE_FROM_ORGANIZATION("ORM-65214",
                "Error removing a role: %s from organization: %s",
                "Server encountered an error while removing a role from organization."),
        ERROR_REPLACING_DISPLAY_NAME_OF_ROLE("ORM-65215", "Error replacing display name %s of role %",
                "Server encountered an error while replacing the display name."),
        ERROR_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING("ORM-65216",
                "Error getting permission ids using resource id.",
                "Server encountered an error while retrieving the permission ids");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }

    /**
     * Error Codes of Forbidden Error Messages.
     */
    public enum ForbiddenErrorMessages {

    }

    /**
     * Error Codes of Conflict Error Messages.
     */
    public enum ConflictErrorMessages {

    }

    /**
     * Error Codes of NotFound Error Messages.
     */
    public enum NotFoundErrorMessages {

    }

    /**
     * Enum for Query Filter Operations.
     */
    public enum FilterOperator {

        EQ("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " = :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        SW("", "%") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        EW("%", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        CO("%", "%") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        GE("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " >= :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        LE("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " <= :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        GT("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " > :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        },
        LT("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " < :" + String.format(FILTER_ID_PLACEHOLDER, count) + "; AND ";
            }
        };

        private final String prefix;
        private final String suffix;

        FilterOperator(String prefix, String suffix) {

            this.prefix = prefix;
            this.suffix = suffix;
        }

        public String getPrefix() {

            return prefix;
        }

        public String getSuffix() {

            return suffix;
        }

        /**
         * Abstract class for filter builder functions.
         *
         * @param count filter amount.
         * @return The filter string.
         */
        public abstract String applyFilterBuilder(int count);
    }
}
