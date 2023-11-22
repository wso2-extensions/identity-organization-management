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

package org.wso2.carbon.identity.organization.user.invitation.management.constant;

/**
 * Constants for organization user invitation management.
 */
public class UserInvitationMgtConstants {

    public static final String CLAIM_EMAIL_ADDRESS = "http://wso2.org/claims/emailaddress";
    public static final String CLAIM_MANAGED_ORGANIZATION = "http://wso2.org/claims/identity/managedOrg";
    public static final String ID_CLAIM_READ_ONLY = "http://wso2.org/claims/identity/isReadOnlyUser";
    public static final String INVITATION_ERROR_PREFIX = "OUI-";
    public static final String DEFAULT_USER_STORE_DOMAIN = "DEFAULT";
    public static final String DEFAULT_PROFILE = "default";
    public static final String INVITED_USER_GROUP_NAME_PREFIX = "invitedOrgUserGroup-";

    // Filter Constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String FILTER_STATUS_EQ = "status eq ";
    public static final String FILTER_STATUS = "status";
    public static final String OPERATION_EQ = "eq";

    // Event Handling related Constants
    public static final String EVENT_PROP_USER_NAME = "user-name";
    public static final String EVENT_PROP_EMAIL_ADDRESS = "email-address";
    public static final String EVENT_PROP_CONFIRMATION_CODE = "confirmation-code";
    public static final String EVENT_PROP_TENANT_DOMAIN = "tenant-domain";
    public static final String EVENT_PROP_REDIRECT_URL = "redirect-url";
    public static final String EVENT_PROP_SEND_TO = "send-to";
    public static final String EVENT_PROP_TEMPLATE_TYPE = "TEMPLATE_TYPE";
    public static final String ORGANIZATION_USER_INVITATION_EMAIL_TEMPLATE_TYPE = "OrganizationUserInvitation";
    public static final String EVENT_NAME_POST_ADD_INVITATION = "POST_ADD_ORGANIZATION_USER_INVITATION";
    public static final String EVENT_PROP_ORG_ID = "org-id";
    public static final String EVENT_PROP_GROUP_NAME = "group-name";
    public static final String EVENT_PROP_ROLE_ASSIGNMENTS = "role-assignments";
    public static final String EVENT_POST_ADD_INVITED_ORG_USER = "POST_ADD_INVITED_ORG_USER";
    public static final int SQL_FK_CONSTRAINT_VIOLATION_ERROR_CODE = 547;
    public static final String INVITATION_EVENT_HANDLER_ENABLED = "UserInvitationEventHandler.enable";

    // Configurations
    public static final String ORG_USER_INVITATION_USER_DOMAIN = "OrganizationUserInvitation.PrimaryUserDomain";
    public static final String ORG_USER_INVITATION_DEFAULT_REDIRECT_URL =
            "OrganizationUserInvitation.DefaultAcceptURL";

    /**
     * Error messages for organization user invitation management related errors.
     */
    public enum ErrorMessage {

        // Service layer errors
        ERROR_CODE_USER_NOT_FOUND("10011",
                "Invalid user identification provided.",
                "Could not find an user with given username %s."),
        ERROR_CODE_CREATE_INVITATION("10012",
                "Unable to create the invitation.",
                "Could not create the invitation to the user %s."),
        ERROR_CODE_INVALID_CONFIRMATION_CODE("10013",
                "Invalid confirmation code.",
                "Could not validate the confirmation code %s."),
        ERROR_CODE_INVALID_INVITATION_ID("10014",
                "Invalid invitation id.",
                "Could not delete an invitation with the id %s."),
        ERROR_CODE_EVENT_HANDLE("10015",
                "Unable to handle the invitation create event.",
                "Could not handle the event triggered to create invitation for username %s."),
        ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE_VALUE("10016",
                "Unsupported filter attribute value.",
                "The filter attribute value '%s' is not supported."),
        ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE("10017",
                "Unsupported filter attribute.",
                "The filter attribute '%s' is not supported."),
        ERROR_CODE_ACTIVE_INVITATION_EXISTS("10018",
                "Invitation already exists.",
                "An active invitation already exists for the user %s."),
        ERROR_CODE_INVITATION_EXPIRED("10019",
                "Invitation expired.",
                "The invitation for the user %s has expired."),
        ERROR_CODE_NO_INVITATION_FOR_USER("10020",
                "No invitation available.",
                "No invitation is available for the user %s."),
        ERROR_CODE_UNABLE_TO_RESEND_INVITATION("10021",
                "Unable to resend.",
                "Could not resend the invitation to user %s."),
        ERROR_CODE_INVALID_FILTER("10022",
                "Invalid filter.",
                "The filter '%s' is invalid."),
        ERROR_CODE_USER_ALREADY_EXISTS("10023",
                "User already exists.",
                "User %s is already exists in the organization %s."),
        ERROR_CODE_ACCEPT_INVITATION("10024",
                "Unable to accept the invitation.",
                "Could not accept the invitation for the user %s."),
        ERROR_CODE_CONSTRUCT_REDIRECT_URL("10025",
                "Unable to construct the redirect URL.",
                "Unable to construct the redirect URL for invitation acceptance."),
        ERROR_CODE_GET_USER_STORE_MANAGER("10026",
                "Unable to get the user store manager.",
                "Unable to get the user store manager for the tenant."),
        ERROR_CODE_GET_TENANT_FROM_ORG("10027",
                "Unable to get the tenant domain.",
                "Unable to get the tenant domain for the organization %s."),
        ERROR_CODE_INVALID_USER("10028",
                "Invalid user identification provided.",
                "Authenticated user %s is not entitled for the invitation."),
        ERROR_CODE_INVALID_ROLE("10029",
                "Invalid role identification provided.",
                "Could not find a role with given roleId %s."),
        ERROR_CODE_INVITED_USER_EMAIL_NOT_FOUND("10030",
                "Failed to resolve the email of the invited user.",
                "Could not find the email of the invited user %s."),
        ERROR_CODE_CONSOLE_ACCESS_RESTRICTED("10031",
                "The console access is restricted to the user.",
                "Could not find any role with a console access to create an invitation."),

        // DAO layer errors
        ERROR_CODE_STORE_INVITATION("10501",
                "Unable to store the invitation.",
                "Could not store the invitation details for user %s."),
        ERROR_CODE_STORE_ROLE_ASSIGNMENTS("10502",
                "Unable to store the role assignments of the invitation.",
                "Could not store the role assignment details for user %s."),
        ERROR_CODE_COMMIT_INVITATION("10503",
                "Unable to store the invitation.",
                "Could not store the invitation details."),
        ERROR_CODE_RETRIEVE_INVITATION_DETAILS("10504",
                "Unable to retrieve the invitation.",
                "Could not retrieve the invitation details for invitation id %s."),
        ERROR_CODE_RETRIEVE_ROLE_ASSIGNMENTS("10505",
                "Unable to retrieve the role assignments of the invitation.",
                "Could not retrieve the role assignments of the invitation for invitation id %s."),
        ERROR_CODE_RETRIEVE_INVITATION_BY_CONFIRMATION_ID("10506",
                "Unable to retrieve the invitation from confirmation code.",
                "Could not retrieve the invitation details for confirmation code %s."),
        ERROR_CODE_RETRIEVE_INVITATION_BY_ORG_ID("10507",
                "Unable to retrieve invitations.",
                "Could not retrieve the invitations details for organization id %s."),
        ERROR_CODE_RETRIEVE_ROLE_ASSIGNMENTS_FOR_INVITATION_BY_ORG_ID("10508",
                "Unable to retrieve role assignments.",
                "Could not retrieve the role assignments details for invitation belongs to organization id %s."),
        ERROR_CODE_RETRIEVE_INVITATIONS_FOR_ORG_ID("10509",
                "Unable to retrieve invitations for organization.",
                "Could not retrieve the invitations for organization id %s."),
        ERROR_CODE_DELETE_ROLE_ASSIGNMENTS_BY_INVITATION("10510",
                "Unable to delete role assignments.",
                "Could not delete role assignments for invitation id %s."),
        ERROR_CODE_DELETE_INVITATION_DETAILS("10511",
                "Unable to delete invitation details.",
                "Could not delete invitation details for invitation id %s."),
        ERROR_CODE_DELETE_INVITATION_BY_ID("10512",
                "Unable to delete invitation.",
                "Could not delete invitation for invitation id %s."),
        ERROR_CODE_GET_INVITATION_BY_USER("10513",
                "Unable to retrieve invitation.",
                "Could not retrieve invitation for username %s."),
        ERROR_CODE_MULTIPLE_INVITATIONS_FOR_USER("10514",
                "Multiple invitations found.",
                "Multiple invitations found for username %s."),
        ERROR_CODE_GET_INVITATION("10515",
                "Unable to get the invitation.",
                "Could not get the invitation for invitation id %s."),
        ERROR_CODE_GET_INVITATION_BY_CONF_CODE("10516",
                "Unable to get the invitation.",
                "Could not get the invitation with role assignments for confirmation code %s."),
        ERROR_CODE_STORE_ROLES_APP_ID_INVALID("10517",
                "Unable to store the role assignments.",
                "Provided application/s is/are not valid."),
        ERROR_CODE_GET_ORG_ASSOCIATIONS_FOR_USER("10518",
                "Unable to get the organization associations.",
                "Unable to get the organization associations for the user %s."),
        ERROR_CODE_GET_ORG_ASSOCIATION_FOR_USER("10519",
                "Unable to get the organization association.",
                "Unable to get the organization association for the user %s."),
        ERROR_CODE_GET_APPLICATION_ID("10520",
                "Unable to retrieve the application id.",
                "Could not retrieve the applicationId for the roleId %s."),
        ERROR_CODE_GET_ROLE_ASSIGNMENTS_BY_ROLE_ID("10521",
                "Unable to retrieve the role assignments.",
                "Could not retrieve the role assignments for the roleId %s.");

        private final String code;
        private final String message;
        private final String description;

        ErrorMessage(String code, String message, String description) {

            this.code = code;
            this.message = message;
            this.description = description;
        }

        public String getCode() {

            return INVITATION_ERROR_PREFIX + code;
        }

        public String getMessage() {

            return message;
        }

        public String getDescription() {

            return description;
        }
    }

}
