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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for organization agent sharing.
 */
public class AgentSharingConstants {

    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String ORGANIZATION_ID_REPRESENTATION_1 = "orgId";
    public static final String ORGANIZATION_ID_REPRESENTATION_2 = "organizationId";
    public static final String ORGANIZATION_ID_REPRESENTATION_3 = "id";
    private static final Map<String, String> attributeColumnMap = new HashMap<>();

    static {
        attributeColumnMap.put(BEFORE, SQLConstants.ID_COLUMN_NAME);
        attributeColumnMap.put(AFTER, SQLConstants.ID_COLUMN_NAME);
        attributeColumnMap.put(ORGANIZATION_ID_REPRESENTATION_1, SQLConstants.SHARED_ORG_ID_COLUMN_NAME);
        attributeColumnMap.put(ORGANIZATION_ID_REPRESENTATION_2, SQLConstants.SHARED_ORG_ID_COLUMN_NAME);
        attributeColumnMap.put(ORGANIZATION_ID_REPRESENTATION_3, SQLConstants.SHARED_ORG_ID_COLUMN_NAME);
    }

    public static final String AGENT_IDS = "agentIds";
    public static final String ORGANIZATION = "organization";
    public static final String APPLICATION = "application";

    public static final String PATCH_PATH_ROLES = "roles";
    public static final String PATCH_PATH_PREFIX = "organizations[orgId eq ";
    public static final String PATCH_PATH_SUFFIX_ROLES = "].roles";
    public static final String SHARING_ERROR_PREFIX = "OAS-";
    public static final String LOG_WARN_SKIP_ORG_SHARE_MESSAGE =
            "Skipping agent share for organizations that are not immediate children: %s";
    public static final String LOG_WARN_NON_RESIDENT_AGENT =
            "Skipping agent: %s from sharing because it is not a resident agent in organization: %s.";

    public static final String ACTION_GENERAL_AGENT_SHARE = "general agent share";
    public static final String ACTION_SELECTIVE_AGENT_SHARE = "selective agent share";
    public static final String ACTION_SELECTIVE_AGENT_UNSHARE = "selective agent unshare";
    public static final String ACTION_GENERAL_AGENT_UNSHARE = "general agent unshare";
    public static final String ACTION_AGENT_SHARE_ROLE_ASSIGNMENT_UPDATE = "agent share role assignment update";

    public static final String SHARED_AGENT_ROLE_INCLUDED_KEY = "roles";
    public static final String SHARED_AGENT_SHARING_MODE_INCLUDED_KEY = "sharingMode";

    public static final String ASYNC_PROCESSING_LOG_TEMPLATE = "Processing async %s initiated by user: %s in " +
            "organization: %s.";

    public static final String DEFAULT_PROFILE = "default";
    public static final String CLAIM_MANAGED_ORGANIZATION = "http://wso2.org/claims/identity/managedOrg";
    public static final String ID_CLAIM_READ_ONLY = "http://wso2.org/claims/identity/isReadOnlyUser";
    public static final String PROCESS_ADD_SHARED_AGENT = "processAddSharedAgent";

    /**
     * Error messages for organization agent sharing management related errors.
     */
    public enum ErrorMessage {

        // Client errors.
        ERROR_CODE_AGENT_CRITERIA_INVALID("10001",
                "Agent criteria is invalid.",
                "Agent criteria must contain valid agent identifiers."),
        ERROR_CODE_AGENT_CRITERIA_MISSING("10002",
                "Agent criteria is missing AGENT_IDS.",
                "Agent criteria must contain AGENT_IDS."),
        ERROR_CODE_ORGANIZATIONS_NULL("10003",
                "Organizations list is null.",
                "Organizations list must be provided."),
        ERROR_CODE_POLICY_NULL("10004",
                "Policy is null.",
                "Policy must be provided."),
        ERROR_CODE_ROLES_NULL("10005",
                "Roles list is null.",
                "Roles list must be provided."),
        ERROR_CODE_ORG_DETAILS_NULL("10006",
                "Organization details are null.",
                "Organization details must be provided."),
        ERROR_CODE_ORG_ID_NULL("10007",
                "Organization ID is null.",
                "Organization ID must be provided."),
        ERROR_CODE_ROLE_NAME_NULL("10008",
                "Role name is null.",
                "Role name must be provided."),
        ERROR_CODE_AUDIENCE_NAME_NULL("10009",
                "Audience name is null.",
                "Audience name must be provided."),
        ERROR_CODE_AUDIENCE_TYPE_NULL("10010",
                "Audience type is null.",
                "Audience type must be provided."),
        ERROR_CODE_INVALID_AUDIENCE_TYPE("10011",
                "Invalid audience type provided.",
                "The audience type '%s' is invalid. Allowed types are 'ORGANIZATION' and 'APPLICATION'."),
        ERROR_CODE_INVALID_POLICY("10012",
                "Invalid policy provided: %s",
                "The policy '%s' is not recognized or supported for determining organizations to share " +
                        "the agent with."),
        ERROR_CODE_NULL_SHARE("10013",
                "Attempting to do a null agent share.",
                "The input provided for the agent share operation is null and must be valid."),
        ERROR_CODE_NULL_UNSHARE("10014",
                "Attempting to do a null agent unshare.",
                "The input provided for the agent unshare operation is null and must be valid."),
        ERROR_CODE_AUDIENCE_NOT_FOUND("10015",
                "Audience '%s' not found.",
                "The audience with the provided name and type could not be found."),
        ERROR_CODE_ROLE_NOT_FOUND("10016",
                "Role '%s' not found in audience '%s':'%s'.",
                "The role with the provided name and audience could not be found."),
        ERROR_CODE_PATCH_OPERATIONS_NULL("10017",
                "Patch operations list is null.",
                "Patch operations list must be provided."),
        ERROR_CODE_PATCH_OPERATION_NULL("10018",
                "Patch operation is null.",
                "A patch operation in the list is null and must be provided."),
        ERROR_CODE_PATCH_OPERATION_OP_NULL("10019",
                "Patch operation 'op' is null.",
                "The 'op' field of a patch operation must be provided."),
        ERROR_CODE_PATCH_OPERATION_OP_INVALID("10020",
                "Patch operation 'op' is invalid.",
                "The 'op' field must be one of: add, remove."),
        ERROR_CODE_PATCH_OPERATION_PATH_NULL("10021",
                "Patch operation 'path' is null.",
                "The 'path' field of a patch operation must be provided."),
        ERROR_CODE_PATCH_OPERATION_PATH_INVALID("10022",
                "Patch operation 'path' is invalid.",
                "The 'path' field format is invalid."),
        ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED("10023",
                "Patch operation 'path' is not supported.",
                "The 'path' field value is not supported."),
        ERROR_CODE_PATCH_OPERATION_VALUE_NULL("10024",
                "Patch operation 'value' is null.",
                "The 'value' field of a patch operation must be provided."),
        ERROR_CODE_PATCH_OPERATION_VALUE_TYPE_INVALID("10025",
                "Patch operation 'value' type is invalid.",
                "The 'value' field must be a list of role assignments."),
        ERROR_CODE_PATCH_OPERATION_ROLES_VALUE_CONTENT_INVALID("10026",
                "Patch operation 'value' roles content is invalid.",
                "Each role assignment must have valid roleName, audienceName, and audienceType fields."),
        ERROR_CODE_ORG_ID_INVALID_FORMAT("10027",
                "Organization ID format is invalid.",
                "The organization ID must be a valid UUID."),
        ERROR_CODE_GET_ATTRIBUTES_NULL("10028",
                "Attributes list is null.",
                "Attributes list must be provided when specified."),
        ERROR_CODE_GET_ATTRIBUTE_NULL("10029",
                "Attribute is null.",
                "An attribute in the list is null."),
        ERROR_CODE_GET_ATTRIBUTE_UNSUPPORTED("10030",
                "Attribute '%s' is not supported.",
                "The requested attribute is not supported."),
        ERROR_CODE_REQUEST_BODY_NULL("10031",
                "Request body is null.",
                "Request body must be provided."),

        // Server errors.
        ERROR_SELECTIVE_SHARE("15001",
                "Error occurred during selective agent share propagation for agentId: %s - %s",
                "Error occurred during selective agent share propagation for a given agent."),
        ERROR_GENERAL_SHARE("15002",
                "Error occurred during general agent share propagation for agentId: %s - %s",
                "Error occurred during general agent share propagation for a given agent."),
        ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS("15003",
                "Error occurred while retrieving immediate child organizations.",
                "An unexpected error occurred while fetching immediate child organizations for the " +
                        "sharing initiated organization with ID: %s."),
        ERROR_CODE_AGENT_SHARE("15004",
                "Error occurred during agent share propagation for agentId: %s - %s",
                "Error occurred during agent share propagation for a given agent."),
        ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT("15005",
                "Error occurred while retrieving roles shared with shared agent.",
                "Unable to retrieve the roles shared with the specified agent due to an internal error."),
        ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_AGENT("15006",
                "Error occurred while retrieving shared organizations of agent.",
                "Unable to retrieve the organizations shared with the agent due to an internal error."),
        ERROR_CODE_AGENT_UNSHARE("15007",
                "Error occurred during agent unshare operation.",
                "An unexpected error occurred while unsharing the agent."),
        ERROR_CODE_GET_ROLE_IDS("15008",
                "Error occurred while retrieving role IDs.",
                "An unexpected error occurred during the retrieval of role IDs for the provided roles with " +
                        "audience details."),
        ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID("15009",
                "Error occurred while retrieving the agent role ID.",
                "An unexpected error occurred while retrieving the role assignment ID for the agent."),
        ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION("15010",
                "Error occurred while inserting the restricted edit permission for the agent role.",
                "An unexpected error occurred while inserting the restricted edit permission for the shared " +
                        "agent role.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        /**
         * Returns the error code prefixed with {@code SHARING_ERROR_PREFIX}.
         *
         * @return the prefixed error code string.
         */
        public String getCode() {

            return SHARING_ERROR_PREFIX + code;
        }

        /**
         * Returns the human-readable message for this error enum constant.
         *
         * @return the error message string.
         */
        public String getMessage() {

            return message;
        }

        /**
         * Returns the detailed description for this error enum constant.
         *
         * @return the error description string.
         */
        public String getDescription() {

            return description;
        }

        /**
         * Returns a combined string representation containing the error code and message.
         *
         * @return a string in the format {@code <code> | <message>}.
         */
        @Override
        public String toString() {

            return getCode() + " | " + message;
        }
    }
}
