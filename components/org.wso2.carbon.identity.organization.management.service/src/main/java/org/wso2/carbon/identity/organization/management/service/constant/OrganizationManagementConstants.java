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

package org.wso2.carbon.identity.organization.management.service.constant;

/**
 * Contains constants related to organization management.
 */
public class OrganizationManagementConstants {

    public static final String ROOT = "ROOT";
    public static final String ORGANIZATION_RESOURCE_PATH = "v1.0/organizations/%s";
    public static final String TENANT_CONTEXT_PATH_COMPONENT = "/t/%s";
    public static final String ORGANIZATION_MANAGEMENT_API_PATH_COMPONENT = "/api/identity/organization-mgt/";
    private static final String ORGANIZATION_MANAGEMENT_ERROR_CODE_PREFIX = "ORG-";

    public static final String VIEW_NAME_COLUMN = "UM_ORG_NAME";
    public static final String VIEW_DESCRIPTION_COLUMN = "UM_ORG_DESCRIPTION";
    public static final String VIEW_CREATED_TIME_COLUMN = "UM_CREATED_TIME";
    public static final String VIEW_LAST_MODIFIED_COLUMN = "UM_LAST_MODIFIED";
    public static final String VIEW_PARENT_ID_COLUMN = "UM_PARENT_ID";
    public static final String VIEW_ATTR_KEY_COLUMN = "UM_ATTRIBUTE_KEY";
    public static final String VIEW_ATTR_VALUE_COLUMN = "UM_ATTRIBUTE_VALUE";

    public static final String PATCH_OP_ADD = "ADD";
    public static final String PATCH_OP_REMOVE = "REMOVE";
    public static final String PATCH_OP_REPLACE = "REPLACE";
    public static final String PATCH_PATH_ORG_NAME = "/name";
    public static final String PATCH_PATH_ORG_DESCRIPTION = "/description";
    public static final String PATCH_PATH_ORG_ATTRIBUTES = "/attributes/";

    public static final String PARENT_ID_FIELD = "parentId";
    public static final String ORGANIZATION_NAME_FIELD = "name";

    /**
     * Enum for error messages related to organization management.
     */
    public enum ErrorMessages {

        // Client errors.
        ERROR_CODE_INVALID_REQUEST_BODY("60001", "Invalid request.", "Provided request body content " +
                "is not in the expected format."),
        ERROR_CODE_REQUIRED_FIELDS_MISSING("60002", "Invalid request body.", "Missing required field : %s"),
        ERROR_CODE_ATTRIBUTE_KEY_MISSING("60003", "Invalid request body.",
                "Attribute keys cannot be empty."),
        ERROR_CODE_DUPLICATE_ATTRIBUTE_KEYS("60004", "Invalid request body.",
                "Attribute keys cannot be duplicated."),
        ERROR_CODE_INVALID_PARENT_ORGANIZATION("60005", "Invalid parent organization.",
                "Defined parent organization doesn't exist in tenant: %s."),
        ERROR_CODE_ORGANIZATION_NAME_CONFLICT("60006", "Organization name unavailable.",
                "Provided organization name: %s already exists in tenant: %s"), // 409
        ERROR_CODE_ORGANIZATION_HAS_CHILD_ORGANIZATIONS("60007", "Unable to delete the organization.",
                "Organization with ID: %s in tenant: %s has one or more child organizations."),
        ERROR_CODE_PATCH_OPERATION_UNDEFINED("60008", "Unable to patch the organization.",
                "Missing patch operation in the patch request sent for organization with ID: %s in tenant: %s"),
        ERROR_CODE_INVALID_PATCH_OPERATION("60009", "Unable to patch the organization.",
                "Invalid patch operation: %s. Patch operation must be one of ['add', 'replace', 'remove']."),
        ERROR_CODE_PATCH_REQUEST_PATH_UNDEFINED("60010", "Unable to patch the organization.",
                "Patch path is not defined."),
        ERROR_CODE_PATCH_REQUEST_INVALID_PATH("60011", "Unable to patch the organization.",
                "Provided path :%s is invalid."),
        ERROR_CODE_PATCH_REQUEST_VALUE_UNDEFINED("60012", "Missing required value.",
                "Value is mandatory for 'add' and 'replace' operations."),
        ERROR_CODE_PATCH_REQUEST_MANDATORY_FIELD_INVALID_OPERATION("60013", "Unable to patch the organization.",
                "Mandatory fields can only be replaced. Provided op : %s, path : %s"),
        ERROR_CODE_ORGANIZATION_ID_UNDEFINED("60014", "Invalid request.",
                "The organization ID can't be empty."),
        ERROR_CODE_INVALID_ORGANIZATION("60015", "Invalid organization.",
                "Organization with ID: %s doesn't exist in tenant %s."), // 404
        ERROR_CODE_ATTRIBUTE_VALUE_MISSING("60016", "Invalid request body.",
                "Attribute value is required for all attributes."),
        ERROR_CODE_ORGANIZATION_NAME_RESERVED("60017", "Organization name unavailable.",
                "Creating an organization with name: %s is restricted. Use a different organization name."),
        ERROR_CODE_PATCH_REQUEST_ATTRIBUTE_KEY_UNDEFINED("60018", "Unable to patch the organization.",
                "Missing attribute key."),
        ERROR_CODE_PATCH_REQUEST_REMOVE_NON_EXISTING_ATTRIBUTE("60019", "Unable to patch the organization.",
                "Cannot remove non existing attribute key: %s"),
        ERROR_CODE_PATCH_REQUEST_REPLACE_NON_EXISTING_ATTRIBUTE("60020", "Unable to patch the organization.",
                "Cannot replace non existing attribute key: %s"),

        // Server errors.
        ERROR_CODE_UNEXPECTED("65001", "Unexpected processing error",
                "Server encountered an error while serving the request."),
        ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS("65002", "Unable to retrieve the organizations.",
                "Server encountered an error while retrieving the organizations in tenant: %s"),
        ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_BY_ID("65003", "Unable to retrieve the organization.",
                "Server encountered an error while retrieving the organization with ID: %s in tenant: %s"),
        ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_ID_BY_NAME("65004", "Unable to retrieve the organization.",
                "Server encountered an error while retrieving organization with name: %s in tenant: %s."),
        ERROR_CODE_ERROR_RETRIEVING_CHILD_ORGANIZATIONS("65005", "Unable to retrieve child organizations.",
                "Server encountered an error while retrieving the child organizations of organization " +
                        "with ID: %s in tenant: %s"),
        ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_NAME("65006", "Unable to check if the organization" +
                " name is available.", "Server encountered an error while checking if an organization with " +
                "name: %s exists in tenant: %s"),
        ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_ID("65007",
                "Error while checking if the organization exists.",
                "Server encountered an error while checking if the organization with ID: %s exists in tenant: %s."),
        ERROR_CODE_ERROR_PATCHING_ORGANIZATION("65008", "Unable to patch the organization.",
                "Server encountered an error while patching the organization with ID: %s in tenant: %s."),
        ERROR_CODE_ERROR_UPDATING_ORGANIZATION("65009", "Unable to update the organization.",
                "Server encountered an error while updating the organization with ID: %s in tenant: %s."),
        ERROR_CODE_ERROR_DELETING_ORGANIZATION("65010", "Unable to delete the organization.",
                "Server encountered an error while deleting the organization with ID: %s in tenant: %s."),
        ERROR_CODE_ERROR_DELETING_ORGANIZATION_ATTRIBUTES("65011", "Unable to delete organization " +
                "attributes.", "Server encountered an error while deleting the attributes of " +
                "organization : %s in tenant: %s."),
        ERROR_CODE_ERROR_CHECKING_ORGANIZATION_ATTRIBUTE_KEY_EXIST("65012", "Error while checking if the " +
                "attribute exists.", "Server encountered an error while checking if the attribute : %s exists" +
                " for organization with ID: %s in tenant: %s"),
        ERROR_CODE_ERROR_PATCHING_ORGANIZATION_ADD_ATTRIBUTE("65013", "Unable to patch the organization.",
                "Server encountered an error while adding the attribute: %s to organization with ID: %s in " +
                        "tenant: %s."),
        ERROR_CODE_ERROR_PATCHING_ORGANIZATION_DELETE_ATTRIBUTE("65014", "Unable to patch the organization.",
                "Server encountered an error while deleting the attribute: %s of organization with ID: %s in " +
                        "tenant: %s."),
        ERROR_CODE_ERROR_PATCHING_ORGANIZATION_UPDATE_ATTRIBUTE("65015", "Unable to patch the organization.",
                "Server encountered an error while updating attribute: %s of organization with ID: %s in tenant: %s."),
        ERROR_CODE_ERROR_ADDING_ORGANIZATION("65016", "Unable to create the organization.",
                "Server encountered an error while creating the organization in tenant: %s."),
        ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL("65017", "Unable to build created organization URL.",
                "Server encountered an error while building URL for response header."),
        ERROR_CODE_ERROR_BUILDING_URL_FOR_RESPONSE_BODY("65018", "Unable to build the URL.",
                "Server encountered an error while building URL for response body.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessages(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return ORGANIZATION_MANAGEMENT_ERROR_CODE_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }
}
