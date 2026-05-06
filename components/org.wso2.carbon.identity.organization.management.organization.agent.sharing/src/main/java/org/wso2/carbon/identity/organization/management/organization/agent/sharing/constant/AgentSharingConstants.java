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

import java.util.Collections;
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
    public static final Map<String, String> SHARED_AGENT_SHARED_ATTRIBUTE_COLUMN_MAP =
            Collections.unmodifiableMap(attributeColumnMap);

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
    public static final String PATCH_PATH_NONE = "none";
    public static final String PATCH_PATH_PREFIX = "organizations[orgId eq ";
    public static final String PATCH_PATH_SUFFIX_ROLES = "].roles";
    public static final String SHARING_ERROR_PREFIX = "OAS-";
    public static final String LOG_WARN_SKIP_ORG_SHARE_MESSAGE =
            "Skipping agent share for organizations that are not immediate children: %s";

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

        ERROR_CODE_AGENT_CRITERIA_INVALID("10015",
                "Agent criteria is invalid.",
                "Agent criteria must contain valid agent identifiers."),
        ERROR_CODE_AGENT_CRITERIA_MISSING("10016",
                "Agent criteria is missing AGENT_IDS.",
                "Agent criteria must contain AGENT_IDS."),
        ERROR_CODE_ORGANIZATIONS_NULL("10017",
                "Organizations list is null.",
                "Organizations list must be provided."),
        ERROR_CODE_POLICY_NULL("10018",
                "Policy is null.",
                "Policy must be provided."),
        ERROR_CODE_ROLES_NULL("10019",
                "Roles list is null.",
                "Roles list must be provided."),
        ERROR_CODE_ORG_DETAILS_NULL("10021",
                "Organization details are null.",
                "Organization details must be provided."),
        ERROR_CODE_ORG_ID_NULL("10022",
                "Organization ID is null.",
                "Organization ID must be provided."),
        ERROR_SELECTIVE_SHARE("10026",
                "Error occurred during selective agent share propagation for agentId: %s - %s",
                "Error occurred during selective agent share propagation for a given agent."),
        ERROR_GENERAL_SHARE("10027",
                "Error occurred during general agent share propagation for agentId: %s - %s",
                "Error occurred during general agent share propagation for a given agent."),
        ERROR_CODE_ROLE_NAME_NULL("10030",
                "Role name is null.",
                "Role name must be provided."),
        ERROR_CODE_AUDIENCE_NAME_NULL("10031",
                "Audience name is null.",
                "Audience name must be provided."),
        ERROR_CODE_AUDIENCE_TYPE_NULL("10032",
                "Audience type is null.",
                "Audience type must be provided."),
        ERROR_CODE_INVALID_AUDIENCE_TYPE("10040",
                "Invalid audience type provided.",
                "The audience type '%s' is invalid. Allowed types are 'ORGANIZATION' and 'APPLICATION'."),
        ERROR_CODE_INVALID_POLICY("10041",
                "Invalid policy provided: %s",
                "The policy '%s' is not recognized or supported for determining organizations to share " +
                        "the agent with."),
        ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS("10042",
                "Error occurred while retrieving immediate child organizations.",
                "An unexpected error occurred while fetching immediate child organizations for the " +
                        "sharing initiated organization with ID: %s."),
        ERROR_CODE_NULL_SHARE("10043",
                "Attempting to do a null agent share.",
                "The input provided for the agent share operation is null and must be valid."),
        ERROR_CODE_NULL_UNSHARE("10044",
                "Attempting to do a null agent unshare.",
                "The input provided for the agent unshare operation is null and must be valid."),
        ERROR_CODE_AGENT_SHARE("10045",
                "Error occurred during agent share propagation for agentId: %s - %s",
                "Error occurred during agent share propagation for a given agent."),
        ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT("10047",
                "Error occurred while retrieving roles shared with shared agent.",
                "Unable to retrieve the roles shared with the specified agent due to an internal error."),
        ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_AGENT("10048",
                "Error occurred while retrieving shared organizations of agent.",
                "Unable to retrieve the organizations shared with the agent due to an internal error."),
        ERROR_CODE_AUDIENCE_NOT_FOUND("10050",
                "Audience '%s' not found.",
                "The audience with the provided name and type could not be found."),
        ERROR_CODE_ROLE_NOT_FOUND("10051",
                "Role '%s' not found in audience '%s':'%s'.",
                "The role with the provided name and audience could not be found."),
        ERROR_CODE_PATCH_OPERATIONS_NULL("10052",
                "Patch operations list is null.",
                "Patch operations list must be provided."),
        ERROR_CODE_PATCH_OPERATION_NULL("10053",
                "Patch operation is null.",
                "A patch operation in the list is null and must be provided."),
        ERROR_CODE_PATCH_OPERATION_OP_NULL("10054",
                "Patch operation 'op' is null.",
                "The 'op' field of a patch operation must be provided."),
        ERROR_CODE_PATCH_OPERATION_OP_INVALID("10055",
                "Patch operation 'op' is invalid.",
                "The 'op' field must be one of: add, remove."),
        ERROR_CODE_PATCH_OPERATION_PATH_NULL("10056",
                "Patch operation 'path' is null.",
                "The 'path' field of a patch operation must be provided."),
        ERROR_CODE_PATCH_OPERATION_PATH_INVALID("10057",
                "Patch operation 'path' is invalid.",
                "The 'path' field format is invalid."),
        ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED("10058",
                "Patch operation 'path' is not supported.",
                "The 'path' field value is not supported."),
        ERROR_CODE_PATCH_OPERATION_VALUE_NULL("10059",
                "Patch operation 'value' is null.",
                "The 'value' field of a patch operation must be provided."),
        ERROR_CODE_PATCH_OPERATION_VALUE_TYPE_INVALID("10060",
                "Patch operation 'value' type is invalid.",
                "The 'value' field must be a list of role assignments."),
        ERROR_CODE_PATCH_OPERATION_ROLES_VALUE_CONTENT_INVALID("10061",
                "Patch operation 'value' roles content is invalid.",
                "Each role assignment must have valid roleName, audienceName, and audienceType fields."),
        ERROR_CODE_ORG_ID_INVALID_FORMAT("10062",
                "Organization ID format is invalid.",
                "The organization ID must be a valid UUID."),
        ERROR_CODE_FILTER_NULL("10063",
                "Filter is null.",
                "Filter must be provided when specified."),
        ERROR_CODE_GET_ATTRIBUTES_NULL("10064",
                "Attributes list is null.",
                "Attributes list must be provided when specified."),
        ERROR_CODE_GET_ATTRIBUTE_NULL("10065",
                "Attribute is null.",
                "An attribute in the list is null."),
        ERROR_CODE_GET_ATTRIBUTE_UNSUPPORTED("10066",
                "Attribute '%s' is not supported.",
                "The requested attribute is not supported."),
        ERROR_CODE_REQUEST_BODY_NULL("10067",
                "Request body is null.",
                "Request body must be provided."),
        ERROR_CODE_AGENT_UNSHARE("10068",
                "Error occurred during agent unshare operation.",
                "An unexpected error occurred while unsharing the agent."),
        ERROR_CODE_GET_ROLE_IDS("10070",
                "Error occurred while retrieving role IDs.",
                "An unexpected error occurred during the retrieval of role IDs for the provided roles with " +
                        "audience details."),
        ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID("10071",
                "Error occurred while retrieving the agent role ID.",
                "An unexpected error occurred while retrieving the role assignment ID for the agent."),
        ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION("10072",
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

            return getCode() + " | " + message;
        }
    }
}
