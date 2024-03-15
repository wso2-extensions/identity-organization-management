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
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAO;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAOImpl;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtClientException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtServerException;
import org.wso2.carbon.identity.organization.user.invitation.management.internal.UserInvitationMgtDataHolder;
import org.wso2.carbon.identity.organization.user.invitation.management.models.AudienceInfo;
import org.wso2.carbon.identity.organization.user.invitation.management.models.GroupAssignments;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;
import org.wso2.carbon.identity.organization.user.invitation.management.models.InvitationDO;
import org.wso2.carbon.identity.organization.user.invitation.management.models.InvitationResult;
import org.wso2.carbon.identity.organization.user.invitation.management.models.RoleAssignments;
import org.wso2.carbon.identity.recovery.IdentityRecoveryConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.Group;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.CLAIM_EMAIL_ADDRESS;
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
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_CONSOLE_ACCESS_RESTRICTED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_CONSTRUCT_REDIRECT_URL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_CREATE_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_EVENT_HANDLE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_GROUP_ASSIGNMENTS_BY_GROUP_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_ROLE_ASSIGNMENTS_BY_ROLE_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_TENANT_FROM_ORG;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GET_USER_STORE_MANAGER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_GROUP_EXISTENCE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_FILTER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_GROUP;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVALID_ROLE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVITATION_EXPIRED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_INVITED_USER_EMAIL_NOT_FOUND;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_NO_INVITATION_FOR_USER;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_ROLE_EXISTENCE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_UNABLE_TO_RESEND_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE_VALUE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_USER_ALREADY_EXISTS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_USER_ALREADY_EXISTS_INVITED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_USER_NOT_FOUND;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.FAIL_STATUS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.FILTER_STATUS_EQ;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.INVITED_USER_GROUP_NAME_PREFIX;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ORG_USER_INVITATION_DEFAULT_REDIRECT_URL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ORG_USER_INVITATION_USER_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.STATUS_EXPIRED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.STATUS_PENDING;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.SUCCESS_STATUS;

/**
 * Implementation of the invitation core service which manages the invitations of the organization users.
 */
public class InvitationCoreServiceImpl implements InvitationCoreService {

    private static final Log LOG = LogFactory.getLog(InvitationCoreServiceImpl.class);
    private static final UserInvitationDAO userInvitationDAO = new UserInvitationDAOImpl();

    @Override
    public List<InvitationResult> createInvitations(InvitationDO invitationDO) throws UserInvitationMgtException {

        List<InvitationResult> createdInvitationsList = new ArrayList<>();
        Invitation invitation = new Invitation();
        String orgId = Utils.getOrganizationId();
        String invitedTenantDomain = resolveTenantDomain(orgId);
        validateInvitationPayload(invitationDO, orgId);
        validateRoleAssignments(invitationDO, invitedTenantDomain);
        validateGroupAssignments(invitationDO, invitedTenantDomain);
        invitation.setInvitedOrganizationId(orgId);
        try {
            String parentOrgId = getOrganizationManager().getParentOrganizationId(orgId);
            String parentTenantDomain = resolveTenantDomain(parentOrgId);
            int parentTenantId = IdentityTenantUtil.getTenantId(parentTenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(parentTenantId);
            for (String username : invitationDO.getUsernamesList()) {
                String userDomainQualifiedUserName = UserCoreUtil
                        .addDomainToName(username, invitationDO.getUserDomain());
                String invitedUserId = userStoreManager.getUserIDFromUserName(userDomainQualifiedUserName);
                String managedOrganization = null;
                if (StringUtils.isNotBlank(invitedUserId)) {
                    managedOrganization = OrganizationSharedUserUtil
                            .getUserManagedOrganizationClaim(userStoreManager, invitedUserId);
                }
                    /* If the invited user is a shared user, get the corresponding user store manager of the shared user
                    managed organization. */
                if (StringUtils.isNotEmpty(managedOrganization)) {
                    String userResidentTenantDomain = getOrganizationManager()
                            .resolveTenantDomain(managedOrganization);
                    userStoreManager = getAbstractUserStoreManager(IdentityTenantUtil.
                            getTenantId(userResidentTenantDomain));
                }
                InvitationResult validationResult = userValidationResult(invitationDO, userStoreManager,
                        userDomainQualifiedUserName, invitedUserId, username, parentOrgId, orgId, parentTenantDomain);
                if (SUCCESS_STATUS.equals(validationResult.getStatus())) {
                    String emailClaim = userStoreManager
                            .getUserClaimValue(userDomainQualifiedUserName, CLAIM_EMAIL_ADDRESS, null);
                    invitation.setUsername(username);
                    invitation.setEmail(emailClaim);
                    invitation.setUserDomain(invitationDO.getUserDomain());
                    invitation.setUserRedirectUrl(invitationDO.getUserRedirectUrl());
                    invitation.setRoleAssignments(invitationDO.getRoleAssignments());
                    invitation.setGroupAssignments(invitationDO.getGroupAssignments());
                    invitation.setUserOrganizationId(parentOrgId);
                    invitation.setStatus(STATUS_PENDING);
                    invitation.setInvitationId(UUID.randomUUID().toString());
                    String confirmationCode = UUID.randomUUID().toString();
                    invitation.setConfirmationCode(confirmationCode);
                    userInvitationDAO.createInvitation(invitation);
                    Invitation createdInvitationInfo = userInvitationDAO
                            .getInvitationByInvitationId(invitation.getInvitationId());
                    if (isNotificationsInternallyManaged(orgId)) {
                        // Trigger the event for invitation creation to send notification internally.
                        triggerInvitationAddNotification(createdInvitationInfo);
                    } else {
                        // Send the confirmation code via the response to manage notification externally.
                        validationResult.setConfirmationCode(confirmationCode);
                    }
                }
                createdInvitationsList.add(validationResult);
            }
        } catch (UserStoreException | OrganizationManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_CREATE_INVITATION.getCode(),
                    ERROR_CODE_CREATE_INVITATION.getMessage(),
                    String.format(ERROR_CODE_CREATE_INVITATION.getDescription(), invitationDO.getUsernamesList()), e);
        }
        return createdInvitationsList;
    }

    @Override
    public boolean acceptInvitation(String confirmationCode) throws UserInvitationMgtException {

        Invitation invitation = userInvitationDAO.getInvitationWithAssignmentsByConfirmationCode(confirmationCode);
        if (invitation != null) {
            if (invitation.getExpiredAt().getTime() > Instant.now().toEpochMilli()) {
                try {
                    String invitedOrganizationId = invitation.getInvitedOrganizationId();
                    String invitedTenantDomain = resolveTenantDomain(invitedOrganizationId);
                    int invitedTenantId = IdentityTenantUtil.getTenantId(invitedTenantDomain);
                    AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(invitedTenantId);
                    String userDomainQualifiedUserName = UserCoreUtil
                            .addDomainToName(invitation.getUsername(), invitation.getUserDomain());
                    if (userStoreManager.isExistingUser(userDomainQualifiedUserName)) {
                        LOG.error("User: " + invitation.getUsername() + " exists in the organization: "
                                + invitedOrganizationId + ". Hence deleting the invitation with the " +
                                "confirmation code: " + confirmationCode);
                        userInvitationDAO.deleteInvitation(invitation.getInvitationId());
                        throw new UserInvitationMgtClientException(ERROR_CODE_USER_ALREADY_EXISTS.getCode(),
                                ERROR_CODE_USER_ALREADY_EXISTS.getMessage(),
                                String.format(ERROR_CODE_USER_ALREADY_EXISTS.getDescription(),
                                        invitation.getUsername(), invitedOrganizationId));
                    }

                    String userId = getInvitedUserId(invitation);
                    getOrganizationUserSharingService().shareOrganizationUser(invitedOrganizationId, userId,
                            invitation.getUserOrganizationId());
                    String associatedUserId = getOrganizationUserSharingService()
                            .getUserAssociationOfAssociatedUserByOrgId(userId,
                                    invitedOrganizationId).getUserId();
                    // Trigger event to add the role assignments if any available in the invitation.
                    if (ArrayUtils.isNotEmpty(invitation.getRoleAssignments())) {
                        for (RoleAssignments roleAssignments : invitation.getRoleAssignments()) {
                            if (getRoleManagementService().isExistingRole(roleAssignments.getRoleId(),
                                    invitedTenantDomain)) {
                                getRoleManagementService().updateUserListOfRole(roleAssignments.getRoleId(),
                                        Collections.singletonList(associatedUserId), Collections.emptyList(),
                                        invitedTenantDomain);
                            } else {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Role: " + roleAssignments.getRoleId()
                                            + " is not exist in the invitedTenantDomain : " + invitedTenantDomain);
                                }
                            }
                        }
                    }
                    if (ArrayUtils.isNotEmpty(invitation.getGroupAssignments())) {
                        for (GroupAssignments groupAssignments : invitation.getGroupAssignments()) {
                            if (userStoreManager.isGroupExist(groupAssignments.getGroupId())) {
                                String groupName =
                                        userStoreManager.getGroupNameByGroupId(groupAssignments.getGroupId());
                                userStoreManager.updateUserListOfRoleWithID(groupName, new String[0],
                                        new String[]{associatedUserId});
                            } else {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Group: " + groupAssignments.getGroupId()
                                            + " does not exist in the invitedTenantDomain : " + invitedTenantDomain);
                                }
                            }
                        }
                    }
                    // Removing the invitation since the user is added to the organization.
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("User: " + invitation.getUsername() + " is added to the organization: "
                                + invitedOrganizationId + ". Hence deleting the invitation with the " +
                                "confirmation code: " + confirmationCode);
                    }
                    userInvitationDAO.deleteInvitation(invitation.getInvitationId());
                    return true;
                } catch (UserStoreException | OrganizationManagementException | IdentityRoleManagementException e) {
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
        for (Invitation invitation : invitations) {
            RoleAssignments[] processedRoleAssignments =
                    processingRoleAssignments(invitation.getRoleAssignments(), invitation.getInvitedOrganizationId());
            invitation.setRoleAssignments(processedRoleAssignments);
            GroupAssignments[] processedGroupAssignments =
                    processingGroupAssignments(invitation.getGroupAssignments(), invitation.getInvitedOrganizationId());
            invitation.setGroupAssignments(processedGroupAssignments);
        }
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

    private void validateInvitationPayload(InvitationDO invitation, String invitedOrgId)
            throws UserInvitationMgtServerException {

        if (StringUtils.isEmpty(invitation.getUserDomain())) {
            invitation.setUserDomain(IdentityUtil.getProperty(ORG_USER_INVITATION_USER_DOMAIN));
        }
        if (StringUtils.isEmpty(invitation.getUserRedirectUrl())) {
            String defaultInvitationAcceptanceURL = IdentityUtil.getProperty(ORG_USER_INVITATION_DEFAULT_REDIRECT_URL);
            try {
                String invitationAcceptanceURL = ServiceURLBuilder.create()
                        .addPath(defaultInvitationAcceptanceURL)
                        .setOrganization(invitedOrgId)
                        .build()
                        .getAbsolutePublicURL();
                invitation.setUserRedirectUrl(invitationAcceptanceURL);
            } catch (URLBuilderException e) {
                throw new UserInvitationMgtServerException(ERROR_CODE_CONSTRUCT_REDIRECT_URL.getCode(),
                        ERROR_CODE_CONSTRUCT_REDIRECT_URL.getDescription(), e);
            }
        }
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = UserInvitationMgtDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    private String getInvitedUserId(Invitation invitation) throws UserInvitationMgtServerException {

        String userName = UserCoreUtil.addDomainToName(invitation.getUsername(), invitation.getUserDomain());

        try {
            String userTenantDomain = resolveTenantDomain(invitation.getUserOrganizationId());
            int userTenantId = IdentityTenantUtil.getTenantId(userTenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(userTenantId);
            String userId = userStoreManager.getUserIDFromUserName(userName);
            String userManagedOrganizationClaim = OrganizationSharedUserUtil
                    .getUserManagedOrganizationClaim(userStoreManager, userId);
            if (userManagedOrganizationClaim != null) {
                // UserManagedOrg claim exist for the shared users. Hence, required to find the associated user ID.
                String orgId = invitation.getUserOrganizationId();
                invitation.setUserOrganizationId(userManagedOrganizationClaim);
                return getOrganizationUserSharingService().getUserAssociation(userId, orgId).getAssociatedUserId();
            }
            return userId;
        } catch (UserStoreException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_GET_USER_STORE_MANAGER.getCode(),
                    ERROR_CODE_GET_USER_STORE_MANAGER.getMessage(), ERROR_CODE_GET_USER_STORE_MANAGER.
                    getDescription(), e);
        } catch (OrganizationManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_GET_TENANT_FROM_ORG.getCode(),
                    ERROR_CODE_GET_TENANT_FROM_ORG.getMessage(),
                    String.format(ERROR_CODE_GET_TENANT_FROM_ORG.getDescription(), invitation.getUserOrganizationId()),
                    e);
        }
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

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return UserInvitationMgtDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private OrganizationManager getOrganizationManager() {

        return UserInvitationMgtDataHolder.getInstance().getOrganizationManagerService();
    }

    private RoleManagementService getRoleManagementService() {

        return UserInvitationMgtDataHolder.getInstance().getRoleManagementService();
    }

    private RoleAssignments[] processingRoleAssignments(RoleAssignments[] roleAssignments, String invitedTenantId)
            throws UserInvitationMgtServerException {

        Role roleInfo;
        String invitedTenantDomain = resolveTenantDomain(invitedTenantId);
        List<RoleAssignments> roleAssignmentsList = new ArrayList<>(Arrays.asList(roleAssignments));

        // Using iterator to remove elements from the array
        Iterator<RoleAssignments> iterator = roleAssignmentsList.iterator();
        while (iterator.hasNext()) {
            RoleAssignments roleAssignment = iterator.next();
            try {
                if (getRoleManagementService().isExistingRole(roleAssignment.getRoleId(), invitedTenantDomain)) {
                    roleInfo = getRoleManagementService().getRoleWithoutUsers(roleAssignment.getRoleId(),
                            invitedTenantId);
                    AudienceInfo audienceInfo = new AudienceInfo();
                    audienceInfo.setApplicationType(roleInfo.getAudience());
                    audienceInfo.setApplicationId(roleInfo.getAudienceId());
                    audienceInfo.setApplicationName(roleInfo.getAudienceName());
                    roleAssignment.setAudience(audienceInfo);
                    roleAssignment.setRoleName(roleInfo.getName());
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Role: " + roleAssignment.getRoleId()
                                + " does not exist in the invitedTenantDomain : " + invitedTenantDomain);
                    }
                    // Remove the roleAssignment element if the role does not exist.
                    iterator.remove();
                }
            } catch (IdentityRoleManagementException e) {
                throw new UserInvitationMgtServerException(ERROR_CODE_GET_ROLE_ASSIGNMENTS_BY_ROLE_ID.getCode(),
                        ERROR_CODE_GET_ROLE_ASSIGNMENTS_BY_ROLE_ID.getMessage(),
                        String.format(ERROR_CODE_GET_ROLE_ASSIGNMENTS_BY_ROLE_ID.getDescription(),
                                roleAssignment.getRoleId()), e);
            }
        }
        roleAssignments = roleAssignmentsList.toArray(new RoleAssignments[0]);
        return roleAssignments;
    }

    private GroupAssignments[] processingGroupAssignments(GroupAssignments[] groupAssignments, String invitedOrgId)
            throws UserInvitationMgtServerException {

        Group group;
        try {
            String invitedTenantDomain = resolveTenantDomain(invitedOrgId);
            AbstractUserStoreManager userStoreManager =
                    getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(invitedTenantDomain));

            List<GroupAssignments> groupAssignmentsList = new ArrayList<>(Arrays.asList(groupAssignments));
            Iterator<GroupAssignments> iterator = groupAssignmentsList.iterator();
            while (iterator.hasNext()) {
                GroupAssignments groupAssignment = iterator.next();
                try {
                    if (userStoreManager.isGroupExist(groupAssignment.getGroupId())) {
                        group = userStoreManager.getGroup(groupAssignment.getGroupId(),
                                Collections.singletonList("displayName"));
                        groupAssignment.setGroupId(group.getGroupID());
                        groupAssignment.setDisplayName(group.getDisplayName());
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Group: " + groupAssignment.getGroupId()
                                    + " does not exist in the invitedTenantDomain : " + invitedTenantDomain);
                        }
                        // Remove the groupAssignment element if the group does not exist.
                        iterator.remove();
                    }
                } catch (org.wso2.carbon.user.core.UserStoreException e) {
                    throw new UserInvitationMgtServerException(ERROR_CODE_GET_GROUP_ASSIGNMENTS_BY_GROUP_ID.getCode(),
                            ERROR_CODE_GET_GROUP_ASSIGNMENTS_BY_GROUP_ID.getMessage(),
                            String.format(ERROR_CODE_GET_GROUP_ASSIGNMENTS_BY_GROUP_ID.getDescription(),
                                    groupAssignment.getGroupId()), e);
                }
            }
            groupAssignments = groupAssignmentsList.toArray(new GroupAssignments[0]);
        } catch (UserStoreException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_GET_USER_STORE_MANAGER.getCode(),
                    ERROR_CODE_GET_USER_STORE_MANAGER.getMessage(), ERROR_CODE_GET_USER_STORE_MANAGER.
                    getDescription(), e);
        }
        return groupAssignments;
    }

    private boolean isUserExistAtInvitedOrganization(String domainQualifiedUserName)
            throws UserStoreException {

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(tenantId);
        if (userStoreManager.isExistingUser(domainQualifiedUserName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("User: " + domainQualifiedUserName + " is already exists in the organization.");
            }
            return true;
        }
        return false;
    }

    private boolean isConsoleAudienceAvailableInRole(InvitationDO invitation, String invitedTenantDomain)
            throws IdentityRoleManagementException {

        if (ArrayUtils.isNotEmpty(invitation.getRoleAssignments())) {
            for (RoleAssignments roleAssignments : invitation.getRoleAssignments()) {
                Role roleInfo = getRoleManagementService()
                        .getRoleWithoutUsers(roleAssignments.getRole(), invitedTenantDomain);
                if (roleInfo != null
                        && FrameworkConstants.Application.CONSOLE_APP.equals(roleInfo.getAudienceName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateRoleAssignments(InvitationDO invitation, String invitedTenantDomain)
            throws UserInvitationMgtException {

        try {
            if (ArrayUtils.isNotEmpty(invitation.getRoleAssignments())) {
                for (RoleAssignments roleAssignment : invitation.getRoleAssignments()) {
                    if (!getRoleManagementService().isExistingRole(roleAssignment.getRole(), invitedTenantDomain)) {
                        throw new UserInvitationMgtClientException(ERROR_CODE_INVALID_ROLE.getCode(),
                                ERROR_CODE_INVALID_ROLE.getMessage(),
                                String.format(ERROR_CODE_INVALID_ROLE.getDescription(), roleAssignment.getRole()));
                    }
                }
            }
        } catch (IdentityRoleManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_ROLE_EXISTENCE.getCode(),
                    ERROR_CODE_ROLE_EXISTENCE.getMessage(), ERROR_CODE_ROLE_EXISTENCE.getDescription());
        }
    }

    private void validateGroupAssignments(InvitationDO invitation, String invitedTenantDomain)
            throws UserInvitationMgtException {

        try {
            if (ArrayUtils.isNotEmpty(invitation.getGroupAssignments())) {
                AbstractUserStoreManager userStoreManager =
                        getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(invitedTenantDomain));
                for (GroupAssignments groupAssignments : invitation.getGroupAssignments()) {
                    if (!userStoreManager.isGroupExist(groupAssignments.getGroupId())) {
                        throw new UserInvitationMgtClientException(ERROR_CODE_INVALID_GROUP.getCode(),
                                ERROR_CODE_INVALID_GROUP.getMessage(),
                                String.format(ERROR_CODE_INVALID_GROUP.getDescription(),
                                        groupAssignments.getGroupId()));
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_GROUP_EXISTENCE.getCode(),
                    ERROR_CODE_GROUP_EXISTENCE.getMessage(), ERROR_CODE_GROUP_EXISTENCE.getDescription());
        }
    }

    private String resolveTenantDomain(String orgId) throws UserInvitationMgtServerException {

        try {
            return getOrganizationManager().resolveTenantDomain(orgId);
        } catch (OrganizationManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_GET_TENANT_FROM_ORG.getCode(),
                    ERROR_CODE_GET_TENANT_FROM_ORG.getMessage(),
                    String.format(ERROR_CODE_GET_TENANT_FROM_ORG.getDescription(), orgId), e);
        }
    }

    private boolean isInvitedUserHasConsoleAccess(String userId, String tenantDomain)
            throws IdentityRoleManagementException {

        List<RoleBasicInfo> roleList = getRoleManagementService().getRoleListOfUser(userId, tenantDomain);
        return roleList.stream().anyMatch(p ->
                FrameworkConstants.Application.CONSOLE_APP.equals(p.getAudienceName()));
    }

    private InvitationResult userValidationResult(InvitationDO invitation, AbstractUserStoreManager userStoreManager,
                                                  String userDomainQualifiedUserName, String invitedUserId,
                                                  String username, String parentOrgId, String invitedOrgId,
                                                  String parentTenantDomain)
            throws UserInvitationMgtException {

        InvitationResult result = new InvitationResult();
        result.setUsername(username);
        try {
            if (isUserExistAtInvitedOrganization(userDomainQualifiedUserName)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("User " + invitedUserId + " is already exists in the organization "
                            + invitedOrgId);
                }
                result.setStatus(FAIL_STATUS);
                result.setErrorMsg(ERROR_CODE_USER_ALREADY_EXISTS_INVITED_ORGANIZATION);
                return result;
            }
            boolean isActiveInvitationAvailable = isActiveInvitationAvailable(username,
                    invitation.getUserDomain(), parentOrgId, invitedOrgId);
            if (isActiveInvitationAvailable) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Active invitation is already available for the user: " + invitedUserId
                            + " in the organization: " + invitedOrgId);
                }
                result.setStatus(FAIL_STATUS);
                result.setErrorMsg(ERROR_CODE_ACTIVE_INVITATION_EXISTS);
                return result;
            }
            if (!userStoreManager.isExistingUser(userDomainQualifiedUserName)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("User: " + invitedUserId + " is not exists in the organization: "
                            + parentOrgId);
                }
                result.setStatus(FAIL_STATUS);
                result.setErrorMsg(ERROR_CODE_USER_NOT_FOUND);
                return result;
            }
            String emailClaim = userStoreManager
                    .getUserClaimValue(userDomainQualifiedUserName, CLAIM_EMAIL_ADDRESS, null);
            if (StringUtils.isEmpty(emailClaim)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Couldn't resolve the email claim of the invited user :" + invitedUserId);
                }
                result.setStatus(FAIL_STATUS);
                result.setErrorMsg(ERROR_CODE_INVITED_USER_EMAIL_NOT_FOUND);
                return result;
            }
            String invitedTenantDomain = resolveTenantDomain(invitedOrgId);
            if (isConsoleAudienceAvailableInRole(invitation, invitedTenantDomain) &&
                    !isInvitedUserHasConsoleAccess(invitedUserId, parentTenantDomain)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The user: " + invitedUserId + " is not having the console access.");
                }
                result.setStatus(FAIL_STATUS);
                result.setErrorMsg(ERROR_CODE_CONSOLE_ACCESS_RESTRICTED);
                return result;
            }
        } catch (UserStoreException | IdentityRoleManagementException e) {
            throw new UserInvitationMgtServerException(ERROR_CODE_CREATE_INVITATION.getCode(),
                    ERROR_CODE_CREATE_INVITATION.getMessage(),
                    String.format(ERROR_CODE_CREATE_INVITATION.getDescription(), username), e);
        }
        result.setStatus(SUCCESS_STATUS);
        return result;
    }

    private boolean isNotificationsInternallyManaged(String organizationId) throws UserInvitationMgtServerException {

        try {
            // Get root organization of the given org.
            String primaryOrganizationId = getOrganizationManager().getPrimaryOrganizationId(organizationId);
            String rootOrgTenantDomain = resolveTenantDomain(primaryOrganizationId);
            boolean manageNotificationsInternally = Boolean.parseBoolean(org.wso2.carbon.identity.recovery.util.Utils.getConnectorConfig
                    (IdentityRecoveryConstants.ConnectorConfig.EMAIL_VERIFICATION_NOTIFICATION_INTERNALLY_MANAGE, rootOrgTenantDomain));
            if (LOG.isDebugEnabled()) {
                if (manageNotificationsInternally) {
                    LOG.debug("Notification will be managed internally");
                } else {
                    LOG.debug("Notification will be managed externally");
                }
            }
            return manageNotificationsInternally;
        } catch (OrganizationManagementServerException e) {
            throw new UserInvitationMgtServerException("Error occurred while resolving the root organization.");
        } catch (IdentityEventException e) {
            throw new UserInvitationMgtServerException("Error occurred while retrieving the notification management configurations.");
        }
    }
}
