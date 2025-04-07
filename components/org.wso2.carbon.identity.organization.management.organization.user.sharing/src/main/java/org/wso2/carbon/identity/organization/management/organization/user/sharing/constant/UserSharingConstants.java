/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constants for organization user sharing.
 */
public class UserSharingConstants {

    public static final String USER_ID = "userId";
    public static final String USER_IDS = "userIds";
    public static final String USER_GROUPS = "userGroups";
    public static final String ORG_ID = "orgId";
    public static final String POLICY = "policy";
    public static final String ROLES = "roles";
    public static final String ORGANIZATION = "organization";
    public static final String APPLICATION = "application";
    public static final String USER = "User";
    public static final String BLANK = "";

    public static final String SHARING_TYPE_SHARED = "Shared";
    public static final String SHARING_TYPE_INVITED = "Invited";
    public static final String SHARING_TYPE_INHERITED = "Inherited"; //Sub-org Owner.

    public static final String NULL_POLICY = "Policy is null";

    public static final String SHARING_ERROR_PREFIX = "OUS-";

    public static final String NULL_INPUT_MESSAGE_SUFFIX = " is null";
    public static final String NULL_SHARE_INPUT_MESSAGE = "Attempting to do a null share";
    public static final String NULL_UNSHARE_INPUT_MESSAGE = "Attempting to do a null unshare";

    public static final String POLICY_CODE_FOR_EXISTING_AND_FUTURE = "-EF-";
    public static final String POLICY_CODE_FOR_FUTURE_ONLY = "-FO-";

    public static final String VALIDATION_CONTEXT_USER_SHARE_SELECTIVE_DO = "SelectiveUserShareDO";
    public static final String VALIDATION_CONTEXT_USER_SHARE_GENERAL_DO = "GeneralUserShareDO";

    public static final String LOG_INFO_SELECTIVE_SHARE_COMPLETED = "Selective share completed.";
    public static final String LOG_INFO_GENERAL_SHARE_COMPLETED = "General share completed.";

    public static final String LOG_WARN_SKIP_ORG_SHARE_MESSAGE =
            "Skipping user share for organizations that are not immediate children: %s";
    public static final String LOG_WARN_NON_RESIDENT_USER =
            "Skipping user share for user: %s since the user is not managed by the sharing initiated org: %s";

    public static final String DEFAULT_PROFILE = "default";
    public static final String CLAIM_MANAGED_ORGANIZATION = "http://wso2.org/claims/identity/managedOrg";
    public static final String ID_CLAIM_READ_ONLY = "http://wso2.org/claims/identity/isReadOnlyUser";
    public static final String CLAIM_USER_SHARED_TYPE = "http://wso2.org/claims/identity/sharedType";
    public static final String CLAIM_LAST_PASSWORD_UPDATE_TIME =
            "http://wso2.org/claims/identity/lastPasswordUpdateTime";

    public static final String ORG_MGT_PERMISSION = "/permission/admin/manage/identity/organizationmgt";
    public static final String ORG_ROLE_MGT_PERMISSION = "/permission/admin/manage/identity/rolemgt";
    public static final String SESSION_MGT_VIEW_PERMISSION =
            "/permission/admin/manage/identity/authentication/session/view";
    public static final String GROUP_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/groupmgt/view";
    public static final String GOVERNANCE_VIEW_PERMISSION = "/permission/admin/manage/identity/governance/view";
    public static final String USER_STORE_CONFIG_VIEW_PERMISSION =
            "/permission/admin/manage/identity/userstore/config/view";
    public static final String USER_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/usermgt/view";
    public static final String USER_MGT_LIST_PERMISSION = "/permission/admin/manage/identity/usermgt/list";
    public static final String APPLICATION_MGT_VIEW_PERMISSION =
            "/permission/admin/manage/identity/applicationmgt/view";
    public static final String CORS_CONFIG_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/cors/origins/view";
    public static final String IDP_MGT_VIEW_PERMISSION = "/permission/admin/manage/identity/idpmgt/view";
    public static final String CLAIM_META_DATA_MGT_VIEW_PERMISSION =
            "/permission/admin/manage/identity/claimmgt/metadata/view";
    public static final String USER_MGT_CREATE_PERMISSION = "/permission/admin/manage/identity/usermgt/create";
    public static final String ADMINISTRATOR_ROLE_PERMISSION = "/permission";
    public static final String PRIMARY_DOMAIN = "PRIMARY";
    public static final String AUTHENTICATION_TYPE = "authenticationType";
    public static final String APPLICATION_AUTHENTICATION_TYPE = "APPLICATION";
    public static final String B2B_USER_SHARE = "B2B_USER_SHARE";
    public static final String B2B_USER_UNSHARE = "B2B_USER_UNSHARE";
    public static final String B2B_USER = "USER";

    public static final String API_REF_GET_SHARED_ROLES_OF_USER_IN_ORG =
            "/api/server/v1/users/%s/shared-roles?orgId=%s";

    public static final String PROCESS_ADD_SHARED_USER = "processAddSharedUser";

    /*
    Minimum permissions required for org creator to logged in to the console and view user, groups, roles, SP,
    IDP sections.
    */
    public static final List<String> MINIMUM_PERMISSIONS_REQUIRED_FOR_ORG_CREATOR_VIEW =
            Collections.unmodifiableList(Arrays
                    .asList(SESSION_MGT_VIEW_PERMISSION, GROUP_MGT_VIEW_PERMISSION, GOVERNANCE_VIEW_PERMISSION,
                            USER_STORE_CONFIG_VIEW_PERMISSION, USER_MGT_VIEW_PERMISSION, USER_MGT_LIST_PERMISSION,
                            APPLICATION_MGT_VIEW_PERMISSION, CORS_CONFIG_MGT_VIEW_PERMISSION, IDP_MGT_VIEW_PERMISSION,
                            CLAIM_META_DATA_MGT_VIEW_PERMISSION));

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

        // Service layer errors
        ERROR_CODE_USER_NOT_FOUND("10011",
                "Invalid user identification provided.",
                "Could not find a user with given username."),
        ERROR_PROPAGATE_SELECTIVE_SHARE("10012",
                "Error occurred during selective share propagation.",
                "Unexpected error occurred during selective share propagation."),
        ERROR_PROPAGATE_GENERAL_SHARE("10013",
                "Error occurred during general share propagation.",
                "Unexpected error occurred during general share propagation."),
        ERROR_CODE_NULL_INPUT("10014",
                "Input is null.",
                "The provided input is null and must be provided."),
        ERROR_CODE_USER_CRITERIA_INVALID("10015",
                "User criteria is invalid.",
                "User criteria must contain valid user identifiers."),
        ERROR_CODE_USER_CRITERIA_MISSING("10016",
                "User criteria is missing USER_IDS.",
                "User criteria must contain USER_IDS."),
        ERROR_CODE_ORGANIZATIONS_NULL("10017",
                "Organizations list is null.",
                "Organizations list must be provided."),
        ERROR_CODE_POLICY_NULL("10018",
                "Policy is null.",
                "Policy must be provided."),
        ERROR_CODE_ROLES_NULL("10019",
                "Roles list is null.",
                "Roles list must be provided."),
        ERROR_CODE_USER_ID_NULL("10020",
                "User ID is null.",
                "User ID must be provided."),
        ERROR_CODE_ORG_DETAILS_NULL("10021",
                "Organization details are null.",
                "Organization details must be provided."),
        ERROR_CODE_ORG_ID_NULL("10022",
                "Organization ID is null.",
                "Organization ID must be provided."),
        ERROR_INVALID_ROLES_FORMAT("10025",
                "Invalid roles format.",
                "The roles provided are in an invalid format and cannot be processed."),
        ERROR_SELECTIVE_SHARE("10026",
                "Error occurred during selective share propagation for userId: %s - %s",
                "Error occurred during selective share propagation for a given user."),
        ERROR_GENERAL_SHARE("10027",
                "Error occurred during general share propagation for userId: %s - %s",
                "Error occurred during general share propagation for a given user."),
        ERROR_CODE_GET_TENANT_FROM_ORG("10029",
                "Unable to get the tenant domain.",
                "Unable to get the tenant domain for the organization %s."),
        ERROR_CODE_ROLE_NAME_NULL("10030",
                "Role name is null.",
                "Role name must be provided."),
        ERROR_CODE_AUDIENCE_NAME_NULL("10031",
                "Audience name is null.",
                "Audience name must be provided."),
        ERROR_CODE_AUDIENCE_TYPE_NULL("10032",
                "Audience type is null.",
                "Audience type must be provided."),
        ERROR_CODE_USER_ID_MISSING("10033",
                "userId is mandatory and cannot be null or empty",
                "User ID must be provided."),
        ERROR_CODE_ORGANIZATION_ID_MISSING("10034",
                "organizationId is mandatory and cannot be null or empty",
                "Organization ID must be provided."),
        ERROR_CODE_POLICY_MISSING("10035",
                "policy is mandatory and cannot be null",
                "Policy must be provided."),
        ERROR_CODE_BUILD_USER_SHARE("10036",
                "userId, organizationId, and policy are mandatory fields and must be provided",
                "All required fields must be provided for building a SelectiveUserShare object."),
        ERROR_SKIP_SHARE("10037",
                "Sharing beyond immediate child organizations is not allowed.",
                "User sharing is only permitted with immediate child organizations. " +
                        "Attempts to share beyond this scope are restricted."),
        ERROR_CODE_USER_UNSHARE("10038",
                "Error occurred during user unshare operation.",
                "An unexpected error occurred while unsharing the user."),
        ERROR_CODE_GET_ROLE_IDS("10039",
                "Error occurred while retrieving role IDs.",
                "An unexpected error occurred during the retrieval of role IDs for the provided roles with " +
                        "audience details."),
        ERROR_CODE_INVALID_AUDIENCE_TYPE("10040",
                "Invalid audience type provided.",
                "The audience type '%s' is invalid. Allowed types are 'ORGANIZATION' and 'APPLICATION'."),
        ERROR_CODE_INVALID_POLICY("10041",
                "Invalid policy provided: %s",
                "The policy '%s' is not recognized or supported for determining organizations to share " +
                        "the user with."),
        ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS("10042",
                "Error occurred while retrieving immediate child organizations.",
                "An unexpected error occurred while fetching immediate child organizations for the " +
                        "sharing initiated organization with ID: %s."),
        ERROR_CODE_NULL_SHARE("10043",
                "Attempting to do a null share.",
                "The input provided for the share operation is null and must be valid."),
        ERROR_CODE_NULL_UNSHARE("10044",
                "Attempting to do a null unshare.",
                "The input provided for the unshare operation is null and must be valid."),
        ERROR_CODE_USER_SHARE("10044",
                "Error occurred during user share propagation for userId: %s - %s",
                "Error occurred during user share propagation for a given user."),
        ERROR_CODE_ERROR_RETRIEVING_USER_ROLE_ID("10045",
                "Error retrieving user role ID for user: %s",
                "Unable to fetch the role ID for the specified user in the given context."),
        ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION("10046",
                "Error inserting restricted permission for user: %s",
                "Failed to insert restricted permission during user role management operation."),
        ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER("10047",
                "Error occurred while retrieving roles shared with shared user.",
                "Unable to retrieve the roles shared with the specified user due to an internal error."),
        ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER("10048",
                "Error occurred while retrieving shared organizations of user.",
                "Unable to retrieve the organizations shared with the user due to an internal error."),
        ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_USER("10049",
                "Unauthorized deletion of shared user.",
                "Users shared by ancestor organization requests can only be deleted by that particular " +
                        "user's resident organization."),
        ERROR_CODE_AUDIENCE_NOT_FOUND("10050",
                "Audience '%s' not found.",
                "The audience with the provided name and type could not be found."),
        ERROR_CODE_ROLE_NOT_FOUND("10051",
                "Role '%s' not found in audience '%s':'%s'.",
                "The role with the provided name and audience could not be found.");

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
