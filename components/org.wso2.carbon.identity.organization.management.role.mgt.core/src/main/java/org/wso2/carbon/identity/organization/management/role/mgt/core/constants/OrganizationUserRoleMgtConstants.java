/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.role.mgt.core.constants;

/**
 * Constants for Organization User Role management.
 */
public class OrganizationUserRoleMgtConstants {

    public static final String PATCH_OP_REPLACE = "replace";
    public static final String INCLUDE_SUB_ORGS = "/includeSubOrganizations";
    public static final String IS_FORCED = "/isForced";
    public static final String SCIM_ROLE_ID_ATTR_NAME = "urn:ietf:params:scim:schemas:core:2.0:id";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String ORGANIZATION_NAME = "organizationName";

    /**
     * Error messages.
     */
    public enum ErrorMessages {
        // Role Mgt Client Errors (ORG-60200 - ORG-60999)
        INVALID_ROLE_NON_INTERNAL_ROLE("ORG-60200", "Invalid role", "%s"),
        INVALID_ROLE_ID("ORG-60201", "Invalid role", "%s"),
        INVALID_ORGANIZATION_ROLE_USERS_GET_REQUEST("ORG-60202",
                "Invalid users search/get request for an organization's role",
                "Invalid pagination arguments. 'limit' should be greater than 0 and 'offset' " +
                        "should be greater than -1"),
        INVALID_ORGANIZATION_ID("ORG-60203", "Invalid organization id.",
                "organization id %s does not exist."),
        ADD_ORG_ROLE_USER_REQUEST_INVALID_USER("ORG-60204", "Invalid user", "%s"),
        DELETE_ORG_ROLE_USER_REQUEST_INVALID_DIRECT_MAPPING("ORG-60205",
                "Invalid direct organization user role mapping.", "%s"),
        PATCH_ORG_ROLE_USER_REQUEST_TOO_MANY_OPERATIONS("ORG-60206", "Too many operations",
                "Only one patch operation is valid because only the includeSubOrganizations " +
                        "attribute can be changed."),
        PATCH_ORG_ROLE_USER_REQUEST_INVALID_MAPPING("ORG-60207", "Invalid mapping",
                "No matching role mapping to be updated."),
        PATCH_ORG_ROLE_USER_REQUEST_OPERATION_UNDEFINED("ORG-60208", "Operation undefined",
                "Patch operation is not defined"),
        PATCH_ORG_ROLE_USER_REQUEST_INVALID_OPERATION("ORG-60209", "Invalid operation",
                "Patch op must be 'replace'"),
        PATCH_ORG_ROLE_USER_REQUEST_PATH_UNDEFINED("ORG-60210", "Path undefined",
                "Patch operation path is not defined"),
        PATCH_ORG_ROLE_USER_REQUEST_INVALID_PATH("ORG-60211", "Invalid path",
                "Patch path must be '/includeSubOrganizations'"),
        PATCH_ORG_ROLE_USER_REQUEST_OPERATION_MISSING("ORG-60212",
                "Operation missing.", "The operation %s is missing."),
        PATCH_ORG_ROLE_USER_REQUEST_INVALID_BOOLEAN_VALUE("ORG-60213", "Invalid value",
                "Patch operation boolean value error"),
        ADD_ORG_ROLE_USER_REQUEST_MAPPING_EXISTS("ORG-60214", "Mapping already exists", "%s"),
        INVALID_REQUEST("ORG-60215", "Invalid request", "Error while processing the request."),
        ADD_ORG_ROLE_USER_REQUEST_INVALID_ORGANIZATION_PARAM("ORG-60216", "includeSubOrganizations value" +
                " must be true if forced value is true.", "Error while processing the request."),
        USER_ID_NULL("ORG-60217", "When adding a organization-user-role mapping user id should be there.",
                "UserId value should not be null when adding organization-user-role mapping for " +
                        "organization %s"),
        ADD_ORG_ROLE_USER_REQUEST_NULL_ROLE_ID("ORG-60218",
                "roleId cannot be null when adding a new mapping.",
                "roleId cannot be null when adding a new mapping for organization %s"),
        ADD_ORG_ROLE_USER_REQUEST_NULL_USERS("ORG-60219",
                "When adding a organization-user-role mapping users should be there.",
                "users should be there when adding a organization-user-role mapping for organization %s"),
        FORCED_FIELD_NULL("ORG-60220",
                "When adding a organization-user-role mapping forced value should be there.",
                "forced value should not be null when adding organization-user-role mapping for " +
                        "organization %s"),
        INVALID_FORCED_AND_INCLUDE_SUB_ORGS_VALUES("ORG-60221",
                "includeSubOrganizations value should be specified.",
                "If forced value is false, you have to specify includeSubOrganizations value."),


        // Role Mgt Server Errors (ORG-65200 - ORG-65999)
        ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_ADD_ERROR("ORG-65200",
                "Error while creating the role mappings",
                "Server encountered an error while creating the organization-user-role mappings."),
        ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_ERROR("ORG-65201",
                "Error while deleting the organization user role mapping.",
                "Server encountered an error while deleting the organization-user-role mappings."),
        ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_RETRIEVING_ERROR("ORG-65202",
                "Error while retrieving the role : %s, for user : %s for organization : %s",
                "Server encountered an error while retrieving the roles for a user of a " +
                        "particular organization."),
        ERROR_CODE_HYBRID_ROLE_NAMES_RETRIEVING_ERROR("ORG-65203",
                "Error while retrieving the hybrid role names for roleIds",
                "Server encountered an error while retrieving the role names using role ids."),
        ERROR_CODE_USERS_PER_ORG_ROLE_RETRIEVING_ERROR("ORG-65204",
                "Error while retrieving users for role: %s , organization : %s",
                "Server encountered an error while retrieving users of a particular role in a " +
                        "particular organization."),
        ERROR_CODE_ROLES_PER_ORG_USER_RETRIEVING_ERROR("ORG-65205",
                "Error while retrieving roles for user: %s , organization : %s", "Server encountered " +
                "an error while retrieving the roles of a user  for a particular organization. "),
        ERROR_CODE_EVENTING_ERROR("ORG-65206", "Error while handling the event : %s",
                "Server encountered an error while handling the event."),
        ERROR_CODE_USER_STORE_OPERATIONS_ERROR("ORG-65207", "Error accessing user store : %s",
                "Server encountered an error while accessing the user store."),
        ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_DELETE_PER_USER_ERROR("ORG-65208",
                "Error while deleting organization user role mappings for user : %s",
                "Server encountered an error while deleting the organization user role mappings."),
        ERROR_CODE_ORGANIZATION_USER_ROLE_MAPPINGS_UPDATE_ERROR("ORG-65209",
                "Error while updating forced property of organization mapping",
                "Server encountered an error while updating the forced property of " +
                        "organization-user-role mapping."),
        ERROR_CODE_UNEXPECTED("ORG-65210", "Unexpected Error",
                "Server encountered an unexpected error."),
        ERROR_CODE_ORGANIZATION_GET_CHILDREN_ERROR("ORG-65211",
                "Error while retrieving the child organizations for parent id: %s",
                "Server encountered an error while retrieving the child organizations."),
        ERROR_CODE_ORGANIZATION_GET_ORGANIZATION_ID_ERROR("ORG-65212",
                "Error while retrieving the organization id: %s",
                "Server encountered an error while retrieving the organization."),
        ERROR_CODE_BUILDING_RESPONSE_HEADER_URL_FOR_ORG_ROLES_ERROR("65017", "Unable to build create URL.",
                "Server encountered an error while building URL for response header."),
        ERROR_CODE_RETRIEVING_DATA_FROM_IDENTITY_DB_ERROR("65018",
                "Unable to retrieve data from Identity database.",
                "Server encountered an error while retrieving data from identity database.");

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
     * Forbidden Error Messages.
     */
    public enum ForbiddenErrorMessages {

    }

    /**
     * Not Found Error Messages.
     */
    public enum NotFoundErrorMessages {

        ORG_60203, ORG_60204, ORG_60205, ORG_60217
    }

    /**
     * Conflict Error Messages.
     */
    public enum ConflictErrorMessages {

        ORG_60213
    }
}
