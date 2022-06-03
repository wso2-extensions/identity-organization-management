/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains constants related to organization management.
 */
public class OrganizationManagementConstants {

    public static final String ROOT = "ROOT";
    public static final String ORGANIZATION_RESOURCE_PATH = "v1.0/organizations/%s";
    public static final String TENANT_CONTEXT_PATH_COMPONENT = "/t/%s";
    public static final String ORGANIZATION_MANAGEMENT_API_PATH_COMPONENT = "/api/identity/organization-mgt/";
    private static final String ORGANIZATION_MANAGEMENT_ERROR_CODE_PREFIX = "ORG-";

    public static final String VIEW_ID_COLUMN = "UM_ID";
    public static final String VIEW_NAME_COLUMN = "UM_ORG_NAME";
    public static final String VIEW_DESCRIPTION_COLUMN = "UM_ORG_DESCRIPTION";
    public static final String VIEW_CREATED_TIME_COLUMN = "UM_CREATED_TIME";
    public static final String VIEW_LAST_MODIFIED_COLUMN = "UM_LAST_MODIFIED";
    public static final String VIEW_STATUS_COLUMN = "UM_STATUS";
    public static final String VIEW_PARENT_ID_COLUMN = "UM_PARENT_ID";
    public static final String VIEW_ATTR_KEY_COLUMN = "UM_ATTRIBUTE_KEY";
    public static final String VIEW_ATTR_VALUE_COLUMN = "UM_ATTRIBUTE_VALUE";
    public static final String VIEW_TYPE_COLUMN = "UM_ORG_TYPE";
    public static final String VIEW_TENANT_ID_COLUMN = "UM_TENANT_ID";

    public static final String PATCH_OP_ADD = "ADD";
    public static final String PATCH_OP_REMOVE = "REMOVE";
    public static final String PATCH_OP_REPLACE = "REPLACE";
    public static final String PATCH_PATH_ORG_NAME = "/name";
    public static final String PATCH_PATH_ORG_DESCRIPTION = "/description";
    public static final String PATCH_PATH_ORG_STATUS = "/status";
    public static final String PATCH_PATH_ORG_ATTRIBUTES = "/attributes/";

    public static final String PARENT_ID_FIELD = "parentId";
    public static final String ORGANIZATION_NAME_FIELD = "name";
    public static final String ORGANIZATION_ID_FIELD = "id";
    public static final String ORGANIZATION_DESCRIPTION_FIELD = "description";
    public static final String ORGANIZATION_CREATED_TIME_FIELD = "created";
    public static final String ORGANIZATION_LAST_MODIFIED_FIELD = "lastModified";
    public static final String ORGANIZATION_STATUS_FIELD = "status";

    public static final String PAGINATION_AFTER = "after";
    public static final String PAGINATION_BEFORE = "before";

    public static final String CREATE_ORGANIZATION_ADMIN_PERMISSION = "/permission/admin/";
    public static final String CREATE_ORGANIZATION_PERMISSION = "/permission/admin/manage/identity/organizationmgt/" +
            "create";
    public static final String VIEW_ORGANIZATION_PERMISSION = "/permission/admin/manage/identity/organizationmgt/" +
            "view";

    public static final String EQ = "eq";
    public static final String CO = "co";
    public static final String SW = "sw";
    public static final String EW = "ew";
    public static final String GE = "ge";
    public static final String LE = "le";
    public static final String GT = "gt";
    public static final String LT = "lt";
    public static final String AND = "and";

    private static Map<String, String> attributeColumnMap = new HashMap<>();

    static {

        attributeColumnMap.put(ORGANIZATION_NAME_FIELD, VIEW_NAME_COLUMN);
        attributeColumnMap.put(ORGANIZATION_ID_FIELD, "UM_ORG." + VIEW_ID_COLUMN);
        attributeColumnMap.put(ORGANIZATION_DESCRIPTION_FIELD, VIEW_DESCRIPTION_COLUMN);
        attributeColumnMap.put(ORGANIZATION_CREATED_TIME_FIELD, VIEW_CREATED_TIME_COLUMN);
        attributeColumnMap.put(ORGANIZATION_LAST_MODIFIED_FIELD, VIEW_LAST_MODIFIED_COLUMN);
        attributeColumnMap.put(PARENT_ID_FIELD, VIEW_PARENT_ID_COLUMN);
        attributeColumnMap.put(PAGINATION_AFTER, VIEW_CREATED_TIME_COLUMN);
        attributeColumnMap.put(PAGINATION_BEFORE, VIEW_CREATED_TIME_COLUMN);
    }

    public static final Map<String, String> ATTRIBUTE_COLUMN_MAP = Collections.unmodifiableMap(attributeColumnMap);

    /**
     * Enum for organization types.
     */
    public enum OrganizationTypes {

        STRUCTURAL,
        TENANT
    }

    /**
     * Enum for organization status.
     */
    public enum OrganizationStatus {

        ACTIVE,
        DISABLED
    }

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
        ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ORGANIZATION("60021", "Unable to create the organization.",
                "Unauthorized request to add an organization to parent organization with ID: %s."), // 403
        ERROR_CODE_INVALID_FILTER_FORMAT("60022", "Unable to retrieve organizations.", "Invalid " +
                "format used for filtering."),
        ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE("60023", "Unsupported filter attribute.",
                "The filter attribute '%s' is not supported."),
        ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER("60024", "Unsupported filter.",
                "The complex query used for filtering is not supported."),
        ERROR_CODE_INVALID_PAGINATION_PARAMETER_NEGATIVE_LIMIT("60025", "Invalid pagination parameters.",
                "'limit' shouldn't be negative."),
        ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION("60026", "Unable to retrieve organizations.", "Invalid " +
                "cursor used for pagination."),
        ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ROOT_ORGANIZATION("60027", "Unable to create the organization.",
                "User is not authorized to create the root organization in tenant: %s."), // 403
        ERROR_CODE_UNSUPPORTED_ORGANIZATION_STATUS("60028", "Unsupported status provided.",
                "Organization status must be 'ACTIVE' or 'DISABLED'."),
        ERROR_CODE_ACTIVE_CHILD_ORGANIZATIONS_EXIST("60029", "Active child organizations exist.",
                "Organization with ID: %s can't be disabled as there are active child organizations."),
        ERROR_CODE_PARENT_ORGANIZATION_IS_DISABLED("60030", "Parent organization is disabled.",
                "To set the child organization status as active, parent organization should be in active status."),
        ERROR_CODE_CREATE_REQUEST_PARENT_ORGANIZATION_IS_DISABLED("60031", "Parent organization is disabled.",
                "To create a child organization in organization with ID: %s, it should be in active status."),
        ERROR_CODE_INVALID_TENANT_TYPE_ORGANIZATION("60032", "Unable to create the organization.",
                "Invalid request body for tenant type organization."),
        ERROR_CODE_ORGANIZATION_TYPE_UNDEFINED("60033", "Unable to create the organization.",
                "Organization type should be defined."),
        ERROR_CODE_INVALID_ORGANIZATION_TYPE("60034", "Invalid organization type.", "The organization " +
                "type should be 'TENANT' or 'STRUCTURAL'."),
        ERROR_CODE_INVALID_APPLICATION("60035", "Invalid application", "The requested application %s is invalid"),
        ERROR_CODE_ORG_PARAMETER_NOT_FOUND("60036", "Organization parameter could not be found.", "The organization " +
                "parameter for shared application authentication is not found"),
        ERROR_CODE_APPLICATION_NOT_SHARED("60037", "Application not shared with organization.", "The " +
                "application %s is not shared with organization with ID %s"),

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
                "Server encountered an error while checking if the organization with ID: %s exists."),
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
                "Server encountered an error while creating the organization."),
        ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL("65017", "Unable to build created organization URL.",
                "Server encountered an error while building URL for response header."),
        ERROR_CODE_ERROR_BUILDING_URL_FOR_RESPONSE_BODY("65018", "Unable to build the URL.",
                "Server encountered an error while building URL for response body."),
        ERROR_CODE_ERROR_EVALUATING_ADD_ORGANIZATION_AUTHORIZATION("65019", "Unable to create the organization.",
                "Server encountered an error while evaluating authorization of user to create the " +
                        "organization in parent organization with ID: %s."),
        ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL("65020", "Unable to retrieve the organizations.",
                "Server encountered an error while building paginated response URL."),
        ERROR_CODE_ERROR_EVALUATING_ADD_ROOT_ORGANIZATION_AUTHORIZATION("65021", "Unable to create the organization.",
                "Server encountered an error while evaluating authorization of user to create the root " +
                        "organization in tenant: %s."),
        ERROR_CODE_ERROR_EVALUATING_ADD_ORGANIZATION_TO_ROOT_AUTHORIZATION("65022", "Unable to create the " +
                "organization.", "Server encountered an error while evaluating authorization of user to create " +
                "a child organization in root of tenant: %s."),
        ERROR_CODE_ERROR_CHECKING_ACTIVE_CHILD_ORGANIZATIONS("65023", "Unable to retrieve active child " +
                "organizations.", "Server encountered an error while retrieving the active child organizations " +
                "of organization with ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_PARENT_ORGANIZATION_STATUS("65024", "Unable to retrieve the status of " +
                "the parent organization.", "Server encountered an error while checking the status of the parent " +
                "organization of organization with ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_STATUS("65025", "Unable to retrieve the status of the organization.",
                "Server encountered an error while checking the status of the organization with ID: %s."),
        ERROR_CODE_ERROR_ADDING_TENANT_TYPE_ORGANIZATION("65026", "Unable to create the organization.",
                "Server encountered an error while creating the tenant."),
        ERROR_CODE_ERROR_DEACTIVATING_ORGANIZATION_TENANT("65027", "Unable to deactivate the tenant.",
                "Server encountered an error while deactivating the tenant of the tenant type organization with " +
                        "ID: %s."),
        ERROR_CODE_ERROR_ACTIVATING_ORGANIZATION_TENANT("65028", "Unable to activate the tenant.",
                "Server encountered an error while activating the tenant of the tenant type organization with " +
                        "ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_TYPE("65029", "Unable to retrieve the organization type.",
                "Server encountered an error while retrieving the type of the organization with ID: %s."),
        ERROR_CODE_ERROR_RETRIEVING_APPLICATION("65030", "Unable to retrieve the application.",
                "Server encountered an error while retrieving the application with ID: %s in tenant: %s."),
        ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION("65031", "Unable to resolve the shared application",
                "Server encountered an error while resolving the shared application for application: %s in tenant: %s"),
        ERROR_CODE_ERROR_SHARING_APPLICATION("65032", "Unable to share the application",
                "Server encountered an error when sharing application: %s to organization: %s"),
        ERROR_CODE_ERROR_LINK_APPLICATIONS("65033", "Unable to link the shared application.",
                "Server encountered an error when linking the application: %s to shared application: %s"),
        ERROR_CODE_ERROR_RESOLVING_ENTERPRISE_IDP_LOGIN("65034", "Unable to resolve the enterpriseIDP shared app." +
                "login", "Server encountered an error when resolving enterpriseIDP login for application: %s"),
        ERROR_CODE_ERROR_REQUEST_ORGANIZATION_REDIRECT("65035", "Unable to redirect to request organization.",
                "Server encountered an error when redirecting enterpriseIDP login to request organization"),
        ;

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
