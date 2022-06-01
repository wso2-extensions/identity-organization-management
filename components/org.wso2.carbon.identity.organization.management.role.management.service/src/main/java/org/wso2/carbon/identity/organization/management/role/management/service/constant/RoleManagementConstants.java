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

import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.role.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME;

/**
 * Contains the constant of Role Management.
 */
public class RoleManagementConstants {

    public static final String TENANT_CONTEXT_PATH_COMPONENT = "/t/%s";
    public static final String ORGANIZATION_MANAGEMENT_API_PATH_COMPONENT = "/api/identity/organization-mgt";

    public static final String ROLE_ACTION = "ui.execute";

    public static final String PATCH_OP_ADD = "add";
    public static final String PATCH_OP_REMOVE = "remove";
    public static final String PATCH_OP_REPLACE = "replace";

    public static final String GROUPS = "groups";
    public static final String USERS = "users";
    public static final String PERMISSIONS = "permissions";

    public static final String DISPLAY_NAME = "displayName";
    public static final String AND_OPERATOR = "and";
    public static final String OR_OPERATOR = "or";

    public static final String FILTER_ID_PLACEHOLDER = "FILTER_ID_%d";

    public static final String ROLE_ID_FIELD = "id";
    public static final String ROLE_NAME_FIELD = "name";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";

    public static final String COMMA_SEPARATOR = ",";

    public static final Map<String, String> ATTRIBUTE_COLUMN_MAP = Stream.of(new String[][]{
            {ROLE_NAME_FIELD, DB_SCHEMA_COLUMN_NAME_UM_ROLE_NAME},
            {ROLE_ID_FIELD, DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID},
            {BEFORE, DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID},
            {AFTER, DB_SCHEMA_COLUMN_NAME_UM_ROLE_ID}
    }).collect(Collectors.collectingAndThen(Collectors.toMap(data -> data[0], data -> data[1]),
            Collections::<String, String>unmodifiableMap));

    /**
     * Error messages for Organization Management - Role Management.
     */
    public enum ErrorMessages {

        // Client Errors (ORM-60200 - ORM-60999)
        ERROR_CODE_INVALID_REQUEST_BODY("ORM-60201", "Invalid request body.",
                "The format of the provided request body is incorrect."),
        ERROR_CODE_REMOVING_REQUIRED_ATTRIBUTE("ORM-60202", "Cannot remove a required attribute.",
                "Cannot remove the required attribute %s with operation %s."),
        ERROR_CODE_INVALID_ATTRIBUTE_PATCHING("ORM-60203", "Invalid attribute.",
                "Invalid attribute %s for operation %s."),
        ERROR_CODE_DISPLAY_NAME_MULTIPLE_VALUES("ORM-60204", "The display name cannot have multiple values.",
                "The display name should have single value."),
        ERROR_CODE_INVALID_FILTER_FORMAT("ORM-60205", "Invalid filter format.", "Invalid" +
                " format used for filtering."),
        ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE("ORM-60206", "Unsupported filter attribute.",
                "The filter attribute '%s' is not supported."),
        ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER("ORM-60207", "Unsupported filter.",
                "The complex query used for filtering is not supported."),
        ERROR_CODE_INVALID_ORGANIZATION("ORM-60208", "Invalid organization.",
                "Organization with ID: %s doesn't exist."),
        ERROR_CODE_ROLE_NAME_ALREADY_EXISTS("ORM-60209", "Role name already exists.",
                "Role name %s exists in organization %s"),
        ERROR_CODE_INVALID_ROLE("ORM-60210", "The role doesn't exist.",
                "The role: %s doesn't exist."),
        ERROR_CODE_ROLE_NAME_OR_ID_REQUIRED("ORM-60211", "Role name or id is required.",
                "Role name or id is required to check whether the role exists in organization %s."),
        ERROR_CODE_ROLE_NAME_NOT_NULL("ORM-60212", "Role name cannot be null", "Role name cannot be null."),
        ERROR_CODE_REMOVE_OP_VALUES("ORM-60213",
                "Remove patch operation values are passed with the path.",
                "Remove patch operation values are passed along with the path."),
        ERROR_CODE_INVALID_USER_ID("ORM-60214", "Invalid user.", "Invalid user %s."),
        ERROR_CODE_INVALID_GROUP_ID("ORM-60215", "Invalid group.", "Invalid group %s."),
        // Server Errors (ORM-65200 - ORM-65999)
        ERROR_CODE_ADDING_ROLE_TO_ORGANIZATION("ORM-65201", "Error adding role to the organization.",
                "Server encountered an error while adding a role to an organization %s."),
        ERROR_CODE_ADDING_GROUP_TO_ROLE("ORM-65202", "Error adding group(s) to role.",
                "Server encountered an error while adding a group to the role %s."),
        ERROR_CODE_ADDING_USER_TO_ROLE("ORM-65603", "Error adding user(s) to role.",
                "Server encountered an error while adding a user to the role."),
        ERROR_CODE_ADDING_PERMISSION_TO_ROLE("ORM-65204", "Error adding permission(s) to role.",
                "Server encountered an error while adding a permission to the role %s"),
        ERROR_CODE_GETTING_ROLE_FROM_ID("ORM-65205", "Error getting role.",
                "Server encountered an error while retrieving role from role id %s."),
        ERROR_CODE_GETTING_USERS_USING_ROLE_ID("ORM-65206", "Error getting users.",
                "Server encountered an error while retrieving user(s) from role id %s."),
        ERROR_CODE_GETTING_GROUPS_USING_ROLE_ID("ORM-65207", "Error getting users of role Id.",
                "Server encountered an error while retrieving user(s) from role id %s."),
        ERROR_CODE_GETTING_PERMISSIONS_USING_ROLE_ID("ORM-65208", "Error getting permissions for the role.",
                "Server encountered an error while retrieving permission(s) from role id %s."),
        ERROR_CODE_GETTING_ROLES_FROM_ORGANIZATION("ORM-65209", "Error getting roles from organization.",
                "Server encountered an error while retrieving role(s) from organization %s."),
        ERROR_CODE_PATCHING_ROLE("ORM-65210", "Error patching a role from organization.",
                "Server encountered an error while patching a role in the organization %s."),
        ERROR_CODE_REMOVING_USERS_FROM_ROLE("ORM-65211", "Error removing a user from role.",
                "Server encountered an error while removing users from the role %s."),
        ERROR_CODE_REMOVING_GROUPS_FROM_ROLE("ORM-65212", "Error removing a group from role.",
                "Server encountered an error while removing groups from the role %s."),
        ERROR_CODE_REMOVING_PERMISSIONS_FROM_ROLE("ORM-65213", "Error removing a permission from role.",
                "Server encountered an error while removing permissions from the role %s."),
        ERROR_CODE_REMOVING_ROLE_FROM_ORGANIZATION("ORM-65214",
                "Error removing a role from an organization.",
                "Server encountered an error while removing a role %s from organization %s."),
        ERROR_CODE_REPLACING_DISPLAY_NAME_OF_ROLE("ORM-65215", "Error replacing display name of role.",
                "Server encountered an error while replacing the display name %s of role %s."),
        ERROR_CODE_GETTING_PERMISSION_IDS_USING_PERMISSION_STRING("ORM-65216",
                "Error getting permission ids using resource id.",
                "Server encountered an error while retrieving the permission ids."),
        ERROR_CODE_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_NAME("ORM-65217",
                "Error getting role from role name and organization id.",
                "Server encountered an error while retrieving a role %s from organization %s."),
        ERROR_CODE_GETTING_ROLE_FROM_ORGANIZATION_ID_ROLE_ID("ORM-65218",
                "Error getting role from role id and organization id.",
                "Sever encountered an error while retrieving a role %s from organization %s."),
        ERROR_CODE_GETTING_USER_VALIDITY("ORM-65219", "Error getting user from user id.",
                "Server encountered an error while retrieving a user %s."),
        ERROR_CODE_GETTING_GROUP_VALIDITY("ORM-65220", "Error getting group from group id.",
                "Server encountered an error while retrieving a group %s."),
        ERROR_CODE_ERROR_BUILDING_ROLE_URI("ORM-65221", "Unable to build create role URI.",
                "Server encountered an error while building URL for role with roleId %s."),
        ERROR_CODE_ERROR_BUILDING_GROUP_URI("ORM-65222", "Unable to build create group URI.",
                "Server encountered an error while building URL for group with groupId %s."),
        ERROR_CODE_ERROR_BUILDING_USER_URI("ORM-65223", "Unable to build create user URI.",
                "Server encountered an error while building URL for user with userId %s.");

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
     * Enum for Filter Operations.
     */
    public enum FilterOperator {

        EQ("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " = :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        SW("", "%") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        EW("%", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        CO("%", "%") {
            @Override
            public String applyFilterBuilder(int count) {

                return " like :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        GE("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " >= :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        LE("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " <= :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        GT("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " > :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
            }
        },
        LT("", "") {
            @Override
            public String applyFilterBuilder(int count) {

                return " < :" + String.format(FILTER_ID_PLACEHOLDER, count) + ";";
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
