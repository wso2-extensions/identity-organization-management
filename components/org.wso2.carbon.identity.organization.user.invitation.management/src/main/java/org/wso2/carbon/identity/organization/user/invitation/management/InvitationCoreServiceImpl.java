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

package org.wso2.carbon.identity.organization.user.invitation.management;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAO;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAOImpl;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtClientException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtServerException;
import org.wso2.carbon.identity.organization.user.invitation.management.internal.UserInvitationMgtDataHolder;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;
import org.wso2.carbon.identity.organization.user.invitation.management.models.RoleAssignments;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.CLAIM_EMAIL_ADDRESS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.DEFAULT_PROFILE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.DEFAULT_USER_STORE_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_NAME_POST_ADD_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_POST_ADD_INVITED_ORG_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_EMAIL_ADDRESS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_GROUP_NAME;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_REDIRECT_URL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_ROLE_ASSIGNMENTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_USER_NAME;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_ACCEPT_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_ACTIVE_INVITATION_EXISTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_CREATE_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_EVENT_HANDLE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_FILTER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVITATION_EXPIRED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_NO_INVITATION_FOR_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_UNABLE_TO_RESEND_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE_VALUE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_USER_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_USER_NOT_FOUND;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.FILTER_STATUS_EQ;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ID_CLAIM_READ_ONLY;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.INVITED_USER_GROUP_NAME_PREFIX;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.STATUS_EXPIRED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.STATUS_PENDING;

/**
 * Implementation of the invitation core service which manages the invitations of the organization users.
 */
public class InvitationCoreServiceImpl implements InvitationCoreService {

    private static final Log LOG = LogFactory.getLog(InvitationCoreServiceImpl.class);
    private static final UserInvitationDAO userInvitationDAO = new UserInvitationDAOImpl();

    @Override
    public Invitation createInvitation(Invitation invitation) throws UserInvitationMgtException {

        String organizationId = Utils.getOrganizationId();
        OrganizationManager organizationManager = UserInvitationMgtDataHolder.getInstance()
                .getOrganizationManagerService();
        Invitation createdInvitation;
        try {
            // Checking the parent organization id
            String parentOrgId = organizationManager.getParentOrganizationId(organizationId);
            // Picking the parent organization's tenant domain
            String parentTenantDomain = organizationManager
                    .resolveTenantDomain(parentOrgId);
            boolean isActiveInvitationAvailable = isActiveInvitationAvailable(invitation.getUsername(),
                    invitation.getUserDomain(), parentOrgId, organizationId);
            if (isActiveInvitationAvailable) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Active invitation is already available for the user: " + invitation.getUsername()
                            + " in the organization: " + organizationId);
                }
                throw new UserInvitationMgtClientException(ERROR_CODE_ACTIVE_INVITATION_EXISTS.getCode(),
                        ERROR_CODE_ACTIVE_INVITATION_EXISTS.getMessage(),
                        String.format(ERROR_CODE_ACTIVE_INVITATION_EXISTS.getDescription(), invitation.getUsername()));
            }
            int parentTenantId = IdentityTenantUtil.getTenantId(parentTenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(parentTenantId);
            String userDomainQualifiedUserName = UserCoreUtil
                    .addDomainToName(invitation.getUsername(), invitation.getUserDomain());
            if (!userStoreManager.isExistingUser(userDomainQualifiedUserName)) {
                LOG.error("User: " + invitation.getUsername() + " is not exists in the organization: "
                        + organizationId);
                throw new UserInvitationMgtClientException(ERROR_CODE_USER_NOT_FOUND.getCode(),
                        ERROR_CODE_USER_NOT_FOUND.getMessage(),
                        String.format(ERROR_CODE_USER_NOT_FOUND.getDescription(), invitation.getUsername()));
            }
            invitation.setEmail(userStoreManager.getUserClaimValue(userDomainQualifiedUserName,
                    CLAIM_EMAIL_ADDRESS, null));
            invitation.setUserOrganizationId(parentOrgId);
            invitation.setInvitedOrganizationId(organizationId);
            invitation.setStatus(STATUS_PENDING);
            if (ArrayUtils.isNotEmpty(invitation.getRoleAssignments())) {
                // ToDO : Check the role existence by introducing a role validation service.

            }
            invitation.setInvitationId(UUID.randomUUID().toString());
            invitation.setConfirmationCode(UUID.randomUUID().toString());
            userInvitationDAO.createInvitation(invitation);
            createdInvitation = userInvitationDAO.getInvitationByInvitationId(invitation.getInvitationId());
            // Trigger the event for invitation creation
            triggerInvitationAddNotification(createdInvitation);
        } catch (UserStoreException | OrganizationManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_CREATE_INVITATION.getCode(),
                    ERROR_CODE_CREATE_INVITATION.getMessage(),
                    String.format(ERROR_CODE_CREATE_INVITATION.getDescription(), invitation.getUsername()), e);
        }
        return createdInvitation;
    }

    @Override
    public boolean acceptInvitation(String confirmationCode) throws UserInvitationMgtException {

        String authenticatedUser = Utils.getAuthenticatedUsername();
        Invitation invitation = userInvitationDAO.getInvitationWithRolesByConfirmationCode(confirmationCode);
        if (invitation != null) {
            String userDomainQualifiedUserName = UserCoreUtil
                    .addDomainToName(invitation.getUsername(), invitation.getUserDomain());
            if (userDomainQualifiedUserName.equals(authenticatedUser)) {
                if (invitation.getExpiredAt().getTime() > Instant.now().toEpochMilli()) {
                    try {
                        OrganizationManager organizationManager = UserInvitationMgtDataHolder.getInstance()
                                .getOrganizationManagerService();
                        String invitedOrganizationId = invitation.getInvitedOrganizationId();
                        String invitedTenantDomain = organizationManager.resolveTenantDomain(invitedOrganizationId);
                        int invitedTenantId = IdentityTenantUtil.getTenantId(invitedTenantDomain);
                        AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(invitedTenantId);
                        String userNameInInvitedOrg = UserCoreUtil
                                .addDomainToName(invitation.getUsername(), DEFAULT_USER_STORE_DOMAIN);
                        if (!userStoreManager.isExistingUser(userNameInInvitedOrg)) {
                            HashMap<String, String> userClaims = new HashMap<>();
                            userClaims.put(CLAIM_MANAGED_ORGANIZATION, invitation.getUserOrganizationId());
                            userClaims.put(ID_CLAIM_READ_ONLY, "true");
                            UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(true);
                            userStoreManager.addUser(userNameInInvitedOrg, generatePassword(), null,
                                    userClaims, DEFAULT_PROFILE);
                            // Adding the association with the organization.
                            String sharedUserId = userStoreManager.getUserIDFromUserName(userNameInInvitedOrg);
                            // Get the available group name for the group in the invited organization.
                            String availableGroupName = getAvailableGroupName();
                            userStoreManager.addRole(availableGroupName, new String[]{userNameInInvitedOrg}, null);
                            String realUserId = Utils.getUserId();
                            userInvitationDAO.createOrganizationAssociation(realUserId,
                                    invitation.getUserOrganizationId(), sharedUserId, invitedOrganizationId);
                            // Trigger event to add the role assignments if any available in the invitation.
                            if (ArrayUtils.isNotEmpty(invitation.getRoleAssignments())) {
                                triggerRoleAssignmentEvent(invitedOrganizationId, availableGroupName,
                                        Arrays.asList(invitation.getRoleAssignments()));
                            }
                            // Removing the invitation since the user is added to the organization.
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("User: " + invitation.getUsername() + " is added to the organization: "
                                        + invitedOrganizationId + ". Hence deleting the invitation with the " +
                                        "confirmation code: " + confirmationCode);
                            }
                            userInvitationDAO.deleteInvitation(invitation.getInvitationId());
                            return true;
                        }
                        LOG.error("User: " + invitation.getUsername() + " exists in the organization: "
                                + invitedOrganizationId + ". Hence deleting the invitation with the " +
                                "confirmation code: " + confirmationCode);
                        userInvitationDAO.deleteInvitation(invitation.getInvitationId());
                        throw new UserInvitationMgtClientException(ERROR_CODE_USER_ALREADY_EXISTS.getCode(),
                                ERROR_CODE_USER_ALREADY_EXISTS.getMessage(),
                                String.format(ERROR_CODE_USER_ALREADY_EXISTS.getDescription(),
                                        invitation.getUsername(), invitedOrganizationId));

                    } catch (UserStoreException | OrganizationManagementException e) {
                        UserCoreUtil.removeSkipPasswordPatternValidationThreadLocal();
                        throw new UserInvitationMgtServerException(ERROR_CODE_ACCEPT_INVITATION.getCode(),
                                ERROR_CODE_ACCEPT_INVITATION.getMessage(),
                                String.format(ERROR_CODE_ACCEPT_INVITATION.getDescription(), confirmationCode), e);
                    }
                } else {
                    // Removing the invitation since the invitation is expired.
                    if (LOG.isDebugEnabled()) {
                        LOG.error("Invitation with the confirmation code: " + confirmationCode + " is expired. Hence " +
                                "deleting the invitation.");
                    }
                    userInvitationDAO.deleteInvitation(invitation.getInvitationId());
                }
            }
        }
        throw new UserInvitationMgtException(ERROR_CODE_INVALID_CONFIRMATION_CODE.getCode(),
                ERROR_CODE_INVALID_CONFIRMATION_CODE.getMessage(),
                String.format(ERROR_CODE_INVALID_CONFIRMATION_CODE.getDescription(), confirmationCode));
    }

    @Override
    public Invitation introspectInvitation(String confirmationCode) throws UserInvitationMgtException {

        Invitation invitation = userInvitationDAO.getInvitationByConfirmationCode(confirmationCode);
        if (invitation != null) {
            Instant currentTime = Instant.now();
            invitation.setStatus(STATUS_PENDING);
            if (invitation.getExpiredAt().getTime() < currentTime.toEpochMilli()) {
                invitation.setStatus(STATUS_EXPIRED);
            }
            return invitation;
        }
        throw new UserInvitationMgtClientException(ERROR_CODE_INVALID_CONFIRMATION_CODE.getCode(),
                ERROR_CODE_INVALID_CONFIRMATION_CODE.getMessage(),
                String.format(ERROR_CODE_INVALID_CONFIRMATION_CODE.getDescription(), confirmationCode));
    }

    @Override
    public List<Invitation> getInvitations(String filter) throws UserInvitationMgtException {

        String[] filterSplits;
        String filterParam = null;
        String filterOperation = null;
        String filterValue = null;
        if (StringUtils.isNotBlank(filter)) {
            if (isFilteringAttributeSupported(filter)) {
                filterSplits = filter.split(" ");
                if (filterSplits.length != 3) {
                    throw new UserInvitationMgtClientException(ERROR_CODE_INVALID_FILTER.getCode(),
                            ERROR_CODE_INVALID_FILTER.getMessage(),
                            String.format(ERROR_CODE_INVALID_FILTER.getDescription(), filter));
                }
                filterParam = filterSplits[0];
                filterOperation = filterSplits[1];
                filterValue = filterSplits[2];
            }
        }

        String organizationId = Utils.getOrganizationId();
        List<Invitation> invitations = userInvitationDAO.getInvitationsByOrganization(organizationId, filterParam,
                filterOperation, filterValue);
        Instant currentTime = Instant.now();
        for (Invitation invitation : invitations) {
            invitation.setStatus(STATUS_PENDING);
            if (invitation.getExpiredAt().getTime() < currentTime.toEpochMilli()) {
                invitation.setStatus(STATUS_EXPIRED);
            }
        }
        return invitations;
    }

    @Override
    public boolean deleteInvitation(String invitationId) throws UserInvitationMgtException {

        Invitation invitation = userInvitationDAO.getInvitationByInvitationId(invitationId);
        String organizationId = Utils.getOrganizationId();
        if (invitation != null) {
            if (invitation.getInvitedOrganizationId().equals(organizationId)) {
                return userInvitationDAO.deleteInvitation(invitationId);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invitation with id: " + invitationId + " is not belongs to the organization: " +
                        organizationId);
            }
        }
        throw new UserInvitationMgtClientException(ERROR_CODE_INVALID_INVITATION_ID.getCode(),
                ERROR_CODE_INVALID_INVITATION_ID.getMessage(),
                String.format(ERROR_CODE_INVALID_INVITATION_ID.getDescription(), invitationId));
    }

    @Override
    public Invitation resendInvitation(String username, String domain) throws UserInvitationMgtException {

        OrganizationManager organizationManager = UserInvitationMgtDataHolder.getInstance()
                .getOrganizationManagerService();
        String organizationId = Utils.getOrganizationId();
        try {
            String parentOrgId = organizationManager.getParentOrganizationId(organizationId);
            Invitation invitation = userInvitationDAO.getActiveInvitationByUser(username, domain, parentOrgId,
                    organizationId);
            if (invitation != null) {
                Timestamp currentTime = new Timestamp(new Date().getTime());
                if (invitation.getExpiredAt().after(currentTime)) {
                    // Trigger the event for invitation resend
                    triggerInvitationAddNotification(invitation);
                    return invitation;
                }
                throw new UserInvitationMgtClientException(ERROR_CODE_INVITATION_EXPIRED.getCode(),
                        ERROR_CODE_INVITATION_EXPIRED.getMessage(),
                        String.format(ERROR_CODE_INVITATION_EXPIRED.getDescription(), username));
            }
            throw new UserInvitationMgtClientException(ERROR_CODE_NO_INVITATION_FOR_USER.getCode(),
                    ERROR_CODE_NO_INVITATION_FOR_USER.getMessage(),
                    String.format(ERROR_CODE_NO_INVITATION_FOR_USER.getDescription(), username));
        } catch (OrganizationManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_UNABLE_TO_RESEND_INVITATION.getCode(),
                    ERROR_CODE_UNABLE_TO_RESEND_INVITATION.getMessage(),
                    String.format(ERROR_CODE_UNABLE_TO_RESEND_INVITATION.getDescription(), username), e);
        }
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = UserInvitationMgtDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    private void triggerInvitationAddNotification(Invitation invitation)
            throws UserInvitationMgtServerException {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(EVENT_PROP_USER_NAME, invitation.getUsername());
        properties.put(EVENT_PROP_EMAIL_ADDRESS, invitation.getEmail());
        properties.put(EVENT_PROP_CONFIRMATION_CODE, invitation.getConfirmationCode());
        properties.put(EVENT_PROP_TENANT_DOMAIN, invitation.getInvitedOrganizationId());
        properties.put(EVENT_PROP_REDIRECT_URL, invitation.getUserRedirectUrl());

        Event invitationEvent = new Event(EVENT_NAME_POST_ADD_INVITATION, properties);
        try {
            UserInvitationMgtDataHolder.getInstance().getIdentityEventService().handleEvent(invitationEvent);
        } catch (IdentityEventException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_EVENT_HANDLE.getCode(),
                    ERROR_CODE_EVENT_HANDLE.getMessage(),
                    String.format(ERROR_CODE_EVENT_HANDLE.getDescription(), invitation.getUsername()), e);
        }
    }

    private boolean isFilteringAttributeSupported(String filter) throws UserInvitationMgtClientException {

        String filterParamValue;
        List<String> allowedStatusFilters = Arrays.asList(STATUS_PENDING, STATUS_EXPIRED);
        if (!StringUtils.contains(filter, FILTER_STATUS_EQ)) {
            throw new UserInvitationMgtClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE.getCode(),
                    ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE.getMessage(),
                    String.format(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE.getDescription(), filter));
        }
        filterParamValue = StringUtils.substringAfter(filter, FILTER_STATUS_EQ);
        if (!allowedStatusFilters.contains(filterParamValue)) {
            throw new UserInvitationMgtClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE_VALUE.getCode(),
                    ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE_VALUE.getMessage(),
                    String.format(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE_VALUE.getDescription(),
                            filterParamValue));
        }
        return true;
    }

    private boolean isActiveInvitationAvailable(String username, String domain, String userOrgId,
                                                String invitedOrgId) throws UserInvitationMgtException {

        Invitation invitation = userInvitationDAO.getActiveInvitationByUser(username, domain, userOrgId, invitedOrgId);
        if (invitation != null) {
            Timestamp currentTime = new Timestamp(new Date().getTime());
            return invitation.getExpiredAt().after(currentTime);
        }
        return false;
    }

    private String generatePassword() {

        UUID uuid = UUID.randomUUID();
        return uuid.toString().substring(0, 12);
    }

    private String getAvailableGroupName() throws UserStoreException {

        SecureRandom rnd = new SecureRandom();
        int number = rnd.nextInt(999999);
        // This will convert any number sequence into 6 character.
        return DEFAULT_USER_STORE_DOMAIN + "/" + INVITED_USER_GROUP_NAME_PREFIX + String.format("%06d", number);
    }

    private void triggerRoleAssignmentEvent(String orgId, String groupName,
                                                   List<RoleAssignments> roleAssignmentsList) {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(EVENT_PROP_ORG_ID, orgId);
        properties.put(EVENT_PROP_GROUP_NAME, groupName);
        properties.put(EVENT_PROP_ROLE_ASSIGNMENTS, roleAssignmentsList);

        Event roleAssignmentEvent = new Event(EVENT_POST_ADD_INVITED_ORG_USER, properties);
        try {
            UserInvitationMgtDataHolder.getInstance().getIdentityEventService().handleEvent(roleAssignmentEvent);
        } catch (IdentityEventException e) {
            LOG.error("Error while triggering role assignment event for group: " + groupName + " in organization: " +
                    orgId, e);
        }
    }
}
