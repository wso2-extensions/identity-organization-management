/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.buffer.SubOperationStatusQueue;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.constants.OperationStatus;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.exception.AsyncOperationStatusMgtException;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.models.OperationInitDTO;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.models.UnitOperationInitDTO;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.service.AsyncOperationStatusMgtService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.RoleAssignmentMode;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.BaseUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.GeneralUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.SelectiveUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GetUserSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleAssignmentDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.UserSharePatchDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.UserSharingResultDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIdList;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.OrganizationScope;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.B2B_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.B2B_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CORRELATION_ID_MDC;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_DETAILS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_GENERAL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_SELECTIVE_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.LOG_WARN_NON_RESIDENT_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.LOG_WARN_SKIP_ORG_SHARE_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ROLE_UPDATE_FAIL_FOR_NEW_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ROLE_UPDATE_SUCCESS_FOR_EXISTING_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.util.OrgResourceHierarchyTraverseUtil.getOrganizationManager;

/**
 * Implementation of the user sharing policy handler service v2.
 */
public class UserSharingPolicyHandlerServiceImplV2 implements UserSharingPolicyHandlerServiceV2 {

    private static final Log LOG = LogFactory.getLog(UserSharingPolicyHandlerServiceImplV2.class);
    private final UserIDResolver userIDResolver = new UserIDResolver();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);
    private final ConcurrentMap<String, SubOperationStatusQueue> asyncOperationStatusList = new ConcurrentHashMap<>();

    @Override
    public void populateSelectiveUserShareV2(SelectiveUserShareV2DO selectiveUserShareV2DO)
            throws UserSharingMgtException {

        LOG.debug("Starting selective user share operation.");
        validateUserShareInput(selectiveUserShareV2DO);
        String sharingInitiatedOrgId = getOrganizationId();

        List<SelectiveUserShareOrgDetailsV2DO> organizations = selectiveUserShareV2DO.getOrganizations();
        Map<String, UserCriteriaType> userCriteria = selectiveUserShareV2DO.getUserCriteria();

        List<SelectiveUserShareOrgDetailsV2DO> validOrganizations =
                filterValidOrganizations(organizations, sharingInitiatedOrgId);

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUserId = carbonContext.getUserId();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        // Run the sharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    LOG.debug("Processing async selective user share for correlation ID: " + getCorrelationId());
                    restoreThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                            sharingInitiatedUsername, threadLocalProperties);
                    processSelectiveUserShare(userCriteria, validOrganizations, sharingInitiatedOrgId,
                            sharingInitiatedUserId, getCorrelationId());
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async user selective share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateGeneralUserShareV2(GeneralUserShareV2DO generalUserShareV2DO) throws UserSharingMgtException {

        LOG.debug("Starting general user share operation.");
        validateUserShareInput(generalUserShareV2DO);
        String sharingInitiatedOrgId = getOrganizationId();

        Map<String, UserCriteriaType> userCriteria = generalUserShareV2DO.getUserCriteria();
        PolicyEnum policy = generalUserShareV2DO.getPolicy();
        List<String> roleIds = getRoleIds(generalUserShareV2DO.getRoleAssignments().getRoles(), sharingInitiatedOrgId);
        RoleAssignmentMode roleAssignmentMode = generalUserShareV2DO.getRoleAssignments().getMode();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUserId = carbonContext.getUserId();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        // Run the sharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    LOG.debug("Processing async general user share for correlation ID: " + getCorrelationId());
                    restoreThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                            sharingInitiatedUsername, threadLocalProperties);
                    processGeneralUserShare(userCriteria, policy, roleIds, roleAssignmentMode, sharingInitiatedOrgId,
                            sharingInitiatedUserId, getCorrelationId());
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general user share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateSelectiveUserUnshareV2(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtException {

        LOG.debug("Starting selective user unshare operation.");
        validateUserUnshareInput(selectiveUserUnshareDO);
        String sharingInitiatedOrgId = getOrganizationId();

        Map<String, UserCriteriaType> userCriteria = selectiveUserUnshareDO.getUserCriteria();
        List<String> organizations = selectiveUserUnshareDO.getOrganizations();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        // Run the unsharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    LOG.debug("Processing async selective user unshare for correlation ID: " + getCorrelationId());
                    restoreThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                            sharingInitiatedUsername, threadLocalProperties);
                    processSelectiveUserUnshare(userCriteria, organizations, sharingInitiatedOrgId);
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async user selective unshare processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateGeneralUserUnshareV2(GeneralUserUnshareDO generalUserUnshareDO) throws UserSharingMgtException {

        LOG.debug("Starting general user unshare operation.");
        validateUserUnshareInput(generalUserUnshareDO);
        String sharingInitiatedOrgId = getOrganizationId();

        Map<String, UserCriteriaType> userCriteria = generalUserUnshareDO.getUserCriteria();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        // Run the unsharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    LOG.debug("Processing async general user unshare for correlation ID: " + getCorrelationId());
                    restoreThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                            sharingInitiatedUsername, threadLocalProperties);
                    processGeneralUserUnshare(userCriteria, sharingInitiatedOrgId);
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general user unshare processing.", ex);
                    return null;
                });
    }

    @Override
    public void updateRoleAssignmentV2(UserSharePatchDO userSharePatchDO) throws UserSharingMgtException {

        // todo: Implement the logic to update role assignments for shared users in v2.
    }

    @Override
    public ResponseSharedOrgsV2DO getUserSharedOrganizationsV2(GetUserSharedOrgsDO getUserSharedOrgsDO)
            throws UserSharingMgtException {

        // todo: Implement the logic to get user shared organizations in v2.
        return null;
    }

    // Asynchronous Processing Methods.

    /**
     * Processes selective user sharing based on the provided user criteria and organization details.
     * This method iterates over the user criteria map and shares users selectively with the specified organizations.
     *
     * @param userCriteria           A map containing user criteria, such as user IDs.
     * @param organizations          A list of organizations to which users will be shared selectively.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the user sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     * @param correlationId          The correlation ID to track down the user sharing.
     */
    private void processSelectiveUserShare(Map<String, UserCriteriaType> userCriteria,
                                           List<SelectiveUserShareOrgDetailsV2DO> organizations,
                                           String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                           String correlationId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            selectiveUserShareByUserIds((UserIdList) criterionValues, organizations,
                                    sharingInitiatedOrgId, sharingInitiatedUserId, correlationId);
                        } else {
                            LOG.error("Invalid user criteria provided for selective user share: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for selective user share: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while sharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed selective user share initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Processes general user sharing based on the provided user criteria and sharing policy.
     * This method iterates over the user criteria map and shares users according to the specified policy.
     *
     * @param userCriteria           A map containing user criteria, such as user IDs.
     * @param policy                 The sharing policy defining the scope of sharing.
     * @param roleIds                A list of role IDs to be assigned during sharing.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the user sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     * @param correlationId          The correlation ID to track down the user sharing.
     */
    private void processGeneralUserShare(Map<String, UserCriteriaType> userCriteria, PolicyEnum policy,
                                         List<String> roleIds, RoleAssignmentMode roleAssignmentMode,
                                         String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                         String correlationId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            generalUserShareByUserIds((UserIdList) criterionValues, policy, roleIds, roleAssignmentMode,
                                    sharingInitiatedOrgId, sharingInitiatedUserId, correlationId);
                        } else {
                            LOG.error("Invalid user criteria provided for general user share: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for general user share: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while sharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed general user share initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Processes selective user unsharing based on the provided user criteria and organization details.
     * This method iterates over the user criteria map and unshare users selectively from the specified organizations.
     *
     * @param userCriteria          A map containing user criteria, such as user IDs.
     * @param organizations         A list of organizations from which users will be unshared selectively.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the user unsharing.
     */
    private void processSelectiveUserUnshare(Map<String, UserCriteriaType> userCriteria, List<String> organizations,
                                             String sharingInitiatedOrgId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            selectiveUserUnshareByUserIds((UserIdList) criterionValues, organizations,
                                    sharingInitiatedOrgId);
                        } else {
                            LOG.error("Invalid user criteria provided for selective user unshare: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for selective user unshare: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while unsharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed selective user unshare initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Processes general user unsharing based on the provided user criteria.
     * This method iterates over the user criteria map and unshare users from all associated organizations.
     *
     * @param userCriteria          A map containing user criteria, such as user IDs.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the user unsharing.
     */
    private void processGeneralUserUnshare(Map<String, UserCriteriaType> userCriteria, String sharingInitiatedOrgId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            generalUserUnshareByUserIds((UserIdList) criterionValues, sharingInitiatedOrgId);
                        } else {
                            LOG.error("Invalid user criteria provided for general user unshare: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for general user unshare: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while unsharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed general user unshare initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    // User Sharing & Unsharing Helper Methods.

    /**
     * Shares a user with selected organizations based on the provided user list and sharing policies.
     * If the user is not a resident user in the initiating organization, the sharing is skipped.
     * Each organization is processed with the appropriate role and policy before sharing.
     *
     * @param userIds                The list of user IDs to be selectively shared.
     * @param organizations          The list of organizations where the user should be shared.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     * @param correlationId          The correlation ID to track down the user sharing.
     */
    private void selectiveUserShareByUserIds(UserIdList userIds, List<SelectiveUserShareOrgDetailsV2DO> organizations,
                                             String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                             String correlationId) throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                if (isExistingUser(associatedUserId, sharingInitiatedOrgId) &&
                        isResidentUserInOrg(associatedUserId, sharingInitiatedOrgId)) {

                    List<BaseUserShare> selectiveUserShareObjectsInRequest = new ArrayList<>();
                    for (SelectiveUserShareOrgDetailsV2DO organization : organizations) {
                        SelectiveUserShare selectiveUserShare = new SelectiveUserShare.Builder()
                                .withUserId(associatedUserId)
                                .withOrganizationId(organization.getOrganizationId())
                                .withPolicy(organization.getPolicy())
                                .withRoles(
                                        getRoleIds(organization.getRoleAssignments().getRoles(), sharingInitiatedOrgId))
                                .withRoleAssignmentMode(organization.getRoleAssignments().getMode())
                                .build();
                        selectiveUserShareObjectsInRequest.add(selectiveUserShare);
                    }
                    shareUser(associatedUserId, selectiveUserShareObjectsInRequest, sharingInitiatedOrgId,
                            sharingInitiatedUserId, correlationId);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format(LOG_WARN_NON_RESIDENT_USER, associatedUserId, sharingInitiatedOrgId));
                    }
                }
            } catch (OrganizationManagementException | IdentityRoleManagementException |
                     ResourceSharingPolicyMgtException e) {
                String errorMessage =
                        String.format(ERROR_SELECTIVE_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserSharingMgtServerException(ERROR_SELECTIVE_SHARE, errorMessage);
            }
        }
    }

    /**
     * Shares a user with all applicable organizations based on the provided policy.
     * If the user is not a resident user in the initiating organization, the sharing is skipped.
     *
     * @param userIds                The list of user IDs to be shared.
     * @param policy                 The policy defining the scope of sharing.
     * @param roleIds                The list of role IDs to be assigned during sharing.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     * @param correlationId          The correlation ID to track down the user sharing.
     */
    private void generalUserShareByUserIds(UserIdList userIds, PolicyEnum policy, List<String> roleIds,
                                           RoleAssignmentMode roleAssignmentMode, String sharingInitiatedOrgId,
                                           String sharingInitiatedUserId, String correlationId)
            throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                if (isExistingUser(associatedUserId, sharingInitiatedOrgId) &&
                        isResidentUserInOrg(associatedUserId, sharingInitiatedOrgId)) {
                    GeneralUserShare generalUserShare = new GeneralUserShare.Builder()
                            .withUserId(associatedUserId)
                            .withPolicy(policy)
                            .withRoles(roleIds)
                            .withRoleAssignmentMode(roleAssignmentMode)
                            .build();
                    List<BaseUserShare> generalUserShareObjectsInRequest = Collections.singletonList(generalUserShare);
                    shareUser(associatedUserId, generalUserShareObjectsInRequest, sharingInitiatedOrgId,
                            sharingInitiatedUserId, correlationId);
                }
            } catch (OrganizationManagementException | IdentityRoleManagementException |
                     ResourceSharingPolicyMgtException e) {
                String errorMessage = String.format(ERROR_GENERAL_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserSharingMgtServerException(ERROR_GENERAL_SHARE, errorMessage);
            }
        }
    }

    /**
     * Unshare a user from selected organizations based on the provided user list.
     * If a resource-sharing policy exists for the user, it is deleted.
     *
     * @param userIds                 The list of user IDs to be unshared.
     * @param organizations           The list of organizations from which the user should be unshared.
     * @param unsharingInitiatedOrgId The ID of the organization that initiated the unsharing.
     */
    private void selectiveUserUnshareByUserIds(UserIdList userIds, List<String> organizations,
                                               String unsharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                for (String organizationId : organizations) {
                    // Unshare user from the organization and its child organizations.
                    List<String> orgTreeInclusive = new ArrayList<>();
                    orgTreeInclusive.add(organizationId);
                    orgTreeInclusive.addAll(getOrganizationManager().getChildOrganizationsIds(organizationId, true));

                    for (String eachOrg : orgTreeInclusive) {
                        getOrganizationUserSharingService().unshareOrganizationUserInSharedOrganization(
                                associatedUserId, eachOrg);
                    }

                    // Delete resource sharing policy if it has been stored for future shares.
                    deleteResourceSharingPolicyOfUserInOrg(organizationId, associatedUserId, unsharingInitiatedOrgId);
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_UNSHARE);
            }
        }
    }

    /**
     * Unshare a user from all applicable organizations based on the provided user list.
     * If a resource-sharing policy exists for the user, it is deleted.
     *
     * @param userIds                 The list of user IDs to be unshared.
     * @param unsharingInitiatedOrgId The ID of the organization that initiated the unsharing.
     */
    private void generalUserUnshareByUserIds(UserIdList userIds, String unsharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                getOrganizationUserSharingService().unshareOrganizationUsers(associatedUserId, unsharingInitiatedOrgId);

                // Delete all resource sharing policies if it has been stored for future shares.
                deleteAllResourceSharingPoliciesOfUser(associatedUserId, unsharingInitiatedOrgId);
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_UNSHARE);
            }
        }
    }

    // Business Logic Methods.

    /**
     * Shares a user with the specified organizations.
     *
     * @param associatedUserId       The ID of the user to be shared.
     * @param baseUserShareObjects   The list of user share objects containing sharing details.
     * @param sharingInitiatedOrgId  The ID of the organization initiating the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     * @param correlationId          The correlation ID to track down the user sharing.
     */
    private void shareUser(String associatedUserId, List<BaseUserShare> baseUserShareObjects,
                           String sharingInitiatedOrgId, String sharingInitiatedUserId, String correlationId)
            throws OrganizationManagementException, UserSharingMgtException, IdentityRoleManagementException,
            ResourceSharingPolicyMgtException {

        if (!baseUserShareObjects.isEmpty()) {
            Map<BaseUserShare, List<String>> userSharingOrgsForEachUserShareObject =
                    getUserSharingOrgsForEachUserShareObject(baseUserShareObjects, sharingInitiatedOrgId);

            try {
                createNewUserShare(sharingInitiatedOrgId, userSharingOrgsForEachUserShareObject,
                        sharingInitiatedUserId, correlationId);
            } catch (AsyncOperationStatusMgtException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creates a new user share by sharing the user with the specified organizations based on the sharing policy of
     * each user share object and saving the sharing policy if applicable.
     *
     * @param sharingInitiatedOrgId                 The ID of the organization initiating the sharing.
     * @param userSharingOrgsForEachUserShareObject A map containing user share objects and their corresponding
     *                                              organizations.
     * @param sharingInitiatedUserId                The ID of the user that initiated the user sharing.
     * @param correlationId                         The correlation ID to track down the user sharing.
     */
    private void createNewUserShare(String sharingInitiatedOrgId, Map<BaseUserShare,
            List<String>> userSharingOrgsForEachUserShareObject, String sharingInitiatedUserId, String correlationId)
            throws ResourceSharingPolicyMgtException, AsyncOperationStatusMgtException {

        for (Map.Entry<BaseUserShare, List<String>> entry : userSharingOrgsForEachUserShareObject.entrySet()) {

            String operationId = registerOperationStatus(entry, sharingInitiatedOrgId, sharingInitiatedUserId,
                    correlationId);
            if (isApplicableOrganizationScopeForSavingPolicy(entry.getKey().getPolicy())) {
                saveUserSharingPolicy(entry.getKey(), sharingInitiatedOrgId);
            }
            for (String orgId : entry.getValue()) {
                shareAndAssignRolesIfPresent(orgId, entry.getKey(), sharingInitiatedOrgId, operationId);
            }
            if (StringUtils.isNotBlank(operationId)) {
                getAsyncStatusMgtService().updateOperationStatus(operationId, getOperationStatus(operationId));
            }
        }
    }

    private String registerOperationStatus(Map.Entry<BaseUserShare, List<String>> entry, String sharingInitiatedOrgId,
                                           String sharingInitiatedUserId, String correlationId)
            throws AsyncOperationStatusMgtException {

        BaseUserShare baseUserShare = entry.getKey();
        String operationId;
        if (baseUserShare instanceof SelectiveUserShare) {
            SelectiveUserShare selectiveUserShare = (SelectiveUserShare) baseUserShare;
            operationId = getAsyncStatusMgtService().registerOperationStatus(
                    new OperationInitDTO(correlationId, B2B_USER_SHARE, B2B_USER, entry.getKey().getUserId(),
                            selectiveUserShare.getOrganizationId(), sharingInitiatedUserId,
                            entry.getKey().getPolicy().getValue()), false);
        } else {
            operationId = getAsyncStatusMgtService().registerOperationStatus(
                    new OperationInitDTO(correlationId, B2B_USER_SHARE, B2B_USER, entry.getKey().getUserId(),
                            sharingInitiatedOrgId, sharingInitiatedUserId,
                            entry.getKey().getPolicy().getValue()), false);
        }
        if (StringUtils.isNotBlank(operationId)) {
            // Registering an operation requires a valid operation ID.
            SubOperationStatusQueue statusQueue = new SubOperationStatusQueue();
            asyncOperationStatusList.put(operationId, statusQueue);
        }
        return operationId;
    }

    private void registerOperationStatusUnit(String operationId, String resourceId, String targetOrgId, OperationStatus
            status, String message) throws AsyncOperationStatusMgtException {

        if (StringUtils.isBlank(operationId)) {
            // Skipping registering of unit operations since the operationId is null.
            return;
        }
        UnitOperationInitDTO statusDTO = new UnitOperationInitDTO(operationId, resourceId, targetOrgId, status,
                message);
        getAsyncStatusMgtService().registerUnitOperationStatus(statusDTO);
        asyncOperationStatusList.get(operationId).add(status);
    }

    /**
     * Retrieves a map of user share objects and their corresponding organizations to share user based on the sharing
     * policy and type.
     *
     * @param baseUserShareObjects  The list of user share objects containing sharing details.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @return A map containing user share objects and their corresponding organizations.
     */
    private Map<BaseUserShare, List<String>> getUserSharingOrgsForEachUserShareObject(
            List<BaseUserShare> baseUserShareObjects, String sharingInitiatedOrgId)
            throws OrganizationManagementException {

        Map<BaseUserShare, List<String>> userSharingOrgsForEachUserShareObject = new HashMap<>();
        for (BaseUserShare baseUserShare : baseUserShareObjects) {
            userSharingOrgsForEachUserShareObject.put(baseUserShare,
                    extractOrgListBasedOnSharingPolicyAndType(baseUserShare, sharingInitiatedOrgId));
        }
        return userSharingOrgsForEachUserShareObject;
    }

    /**
     * Extracts a list of organization with which the user is expected to be shared according to the given policy of
     * the base user share object.
     *
     * @param baseUserShare         The base user share object containing sharing details.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @return A list of organization IDs based on the sharing policy and type.
     */
    private List<String> extractOrgListBasedOnSharingPolicyAndType(BaseUserShare baseUserShare,
                                                                   String sharingInitiatedOrgId)
            throws OrganizationManagementException {

        if (baseUserShare instanceof SelectiveUserShare) {
            return extractOrgListBasedOnSharingPolicy(((SelectiveUserShare) baseUserShare).getOrganizationId(),
                    baseUserShare.getPolicy());
        }
        return extractOrgListBasedOnSharingPolicy(sharingInitiatedOrgId, baseUserShare.getPolicy());
    }

    /**
     * Extracts a list of organizations based on the given sharing policy.
     * Depending on the policy, this method retrieves child organizations that should be included
     * in the user-sharing scope.
     *
     * @param policyHoldingOrgId The ID of the organization holding the policy.
     * @param policy             The sharing policy that determines which organizations to include.
     * @return A list of organization IDs that should be included in the sharing scope.
     */
    private List<String> extractOrgListBasedOnSharingPolicy(String policyHoldingOrgId, PolicyEnum policy)
            throws OrganizationManagementException {

        Set<String> userSharingOrgList = new HashSet<>();

        switch (policy) {
            case ALL_EXISTING_ORGS_ONLY:
            case ALL_EXISTING_AND_FUTURE_ORGS:
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case IMMEDIATE_EXISTING_ORGS_ONLY:
            case IMMEDIATE_EXISTING_AND_FUTURE_ORGS:
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case SELECTED_ORG_ONLY:
                userSharingOrgList.add(policyHoldingOrgId);
                break;

            case SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN:
                userSharingOrgList.add(policyHoldingOrgId);
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN:
                userSharingOrgList.add(policyHoldingOrgId);
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case NO_SHARING:
                break;

            default:
                throw new OrganizationManagementClientException(
                        String.format(ERROR_CODE_INVALID_POLICY.getMessage(), policy.getPolicyName()),
                        ERROR_CODE_INVALID_POLICY.getDescription(),
                        ERROR_CODE_INVALID_POLICY.getCode());
        }

        return new ArrayList<>(userSharingOrgList);
    }

    /**
     * Shares a user with a specified organization and returns the user association.
     * This is where the user association will be created.
     *
     * @param orgId            The ID of the organization to share the user with.
     * @param associatedUserId The ID of the user to be shared.
     * @param associatedOrgId  The ID of the organization that initiated the sharing.
     * @return A {@code UserAssociation} representing the created association.
     */
    private UserAssociation shareUserWithOrganization(String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        OrganizationUserSharingService organizationUserSharingService = getOrganizationUserSharingService();
        organizationUserSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId,
                SharedType.SHARED);
        return organizationUserSharingService.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
    }

    /**
     * Checks if the specified user is a resident user in the given organization.
     *
     * @param userId The ID of the user.
     * @param orgId  The ID of the organization.
     * @return {@code true} if the user is a resident user, {@code false} otherwise.
     */
    private boolean isResidentUserInOrg(String userId, String orgId) {

        try {
            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(tenantId);
            String associatedOrgId =
                    OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager, userId);
            return associatedOrgId == null;
        } catch (UserStoreException | OrganizationManagementException e) {
            LOG.error("Error occurred while checking if the user is a resident user in the organization.", e);
            return false;
        }
    }

    /**
     * Checks if the specified user is an existing user in the given organization.
     *
     * @param userId The ID of the user.
     * @param orgId  The ID of the organization.
     * @return {@code true} if the user is an exiting user in the given organization, {@code false} otherwise.
     */
    private boolean isExistingUser(String userId, String orgId) {

        try {
            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(tenantId);
            return userStoreManager.isExistingUserWithID(userId);
        } catch (UserStoreException | OrganizationManagementException e) {
            LOG.error("Error occurred while checking if the user is an existing user.", e);
            return false;
        }
    }

    /**
     * Filters the list of organizations to include only those that are immediate child organizations
     * of the sharing-initiated organization.
     * Any organizations that do not meet this criterion will be logged as skipped.
     *
     * @param organizations         The list of organizations to be filtered.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing process.
     * @return A list of organizations that are valid based on the immediate child organization criteria.
     */
    private List<SelectiveUserShareOrgDetailsV2DO> filterValidOrganizations(
            List<SelectiveUserShareOrgDetailsV2DO> organizations, String sharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        List<String> immediateChildOrgs = getImmediateChildOrgsOfSharingInitiatedOrg(sharingInitiatedOrgId);

        List<SelectiveUserShareOrgDetailsV2DO> validOrganizations = organizations.stream()
                .filter(org -> immediateChildOrgs.contains(org.getOrganizationId()))
                .collect(Collectors.toList());

        List<String> skippedOrganizations = organizations.stream()
                .map(SelectiveUserShareOrgDetailsV2DO::getOrganizationId)
                .filter(orgId -> !immediateChildOrgs.contains(orgId))
                .collect(Collectors.toList());

        if (!skippedOrganizations.isEmpty() && LOG.isDebugEnabled()) {
            LOG.debug(String.format(LOG_WARN_SKIP_ORG_SHARE_MESSAGE, skippedOrganizations));
        }

        return validOrganizations;
    }

    /**
     * Retrieves the list of immediate child organizations for a given organization.
     *
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @return A list of organization IDs representing the immediate child organizations.
     */
    private List<String> getImmediateChildOrgsOfSharingInitiatedOrg(String sharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        try {
            return getOrganizationManager().getChildOrganizationsIds(getOrganizationId(), false);
        } catch (OrganizationManagementException e) {
            String errorMessage = String.format(
                    ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS.getMessage(), sharingInitiatedOrgId);
            throw new UserSharingMgtServerException(ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS, errorMessage);
        }
    }

    // Resource Sharing Policy Management Methods.

    /**
     * Saves a new resource sharing policy for a user.
     *
     * @param baseUserShare         The user share details containing policy information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void saveUserSharingPolicy(BaseUserShare baseUserShare, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                getResourceSharingPolicyHandlerService();

        ResourceSharingPolicy resourceSharingPolicy =
                new ResourceSharingPolicy.Builder().withResourceType(ResourceType.USER)
                        .withResourceId(baseUserShare.getUserId())
                        .withInitiatingOrgId(sharingInitiatedOrgId)
                        .withPolicyHoldingOrgId(getPolicyHoldingOrgId(baseUserShare, sharingInitiatedOrgId))
                        .withSharingPolicy(baseUserShare.getPolicy()).build();

        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
        for (String roleId : baseUserShare.getRoles()) {
            SharedResourceAttribute sharedResourceAttribute =
                    new SharedResourceAttribute.Builder().withSharedAttributeType(SharedAttributeType.ROLE)
                            .withSharedAttributeId(roleId).build();
            sharedResourceAttributes.add(sharedResourceAttribute);
        }

        resourceSharingPolicyHandlerService.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                sharedResourceAttributes);
    }

    /**
     * Determines the policy-holding organization ID based on the type of user share.
     * For a selective user share, the policy-holding organization is the organization specified in the selective
     * share request.
     * For a general user share, the policy-holding organization is the organization from which the
     * sharing request was initiated.
     *
     * @param baseUserShare         The user share object, which can be either selective or general.
     * @param sharingInitiatedOrgId The ID of the organization from which the sharing request was initiated.
     * @return The ID of the policy-holding organization based on the type of user share.
     */
    private String getPolicyHoldingOrgId(BaseUserShare baseUserShare, String sharingInitiatedOrgId) {

        if (baseUserShare instanceof SelectiveUserShare) {
            return ((SelectiveUserShare) baseUserShare).getOrganizationId();
        }
        return sharingInitiatedOrgId;
    }

    /**
     * Determines whether a given policy scope allows saving the resource sharing policy.
     *
     * @param policy The policy enumeration containing the organization scope.
     * @return {@code true} if the policy allows saving, {@code false} otherwise.
     */
    private boolean isApplicableOrganizationScopeForSavingPolicy(PolicyEnum policy) {

        return OrganizationScope.EXISTING_ORGS_AND_FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope()) ||
                OrganizationScope.FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope());
    }

    /**
     * Deletes the resource sharing policy for a given user in an organization.
     *
     * @param policyHoldingOrgId    The ID of the organization holding the policy.
     * @param associatedUserId      The ID of the associated user.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteResourceSharingPolicyOfUserInOrg(String policyHoldingOrgId, String associatedUserId,
                                                        String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                policyHoldingOrgId, ResourceType.USER, associatedUserId, sharingInitiatedOrgId);
    }

    /**
     * Deletes all resource sharing policies for a given user.
     *
     * @param associatedUserId      The ID of the associated user.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteAllResourceSharingPoliciesOfUser(String associatedUserId, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(ResourceType.USER,
                associatedUserId, sharingInitiatedOrgId);
    }

    // Role Management Helper Methods.

    /**
     * Retrieves a list of role IDs based on the provided role and audience details.
     * This will return the role Ids in the parent organization that match to the given role-audience combination.
     *
     * @param rolesWithAudience     A list of roles with associated audience information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @return A list of role IDs that match the given role-audience combination.
     */
    private List<String> getRoleIds(List<RoleWithAudienceDO> rolesWithAudience, String sharingInitiatedOrgId)
            throws UserSharingMgtException {

        try {
            String sharingInitiatedTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);

            List<String> list = new ArrayList<>();
            for (RoleWithAudienceDO roleWithAudienceDO : rolesWithAudience) {
                String audienceId =
                        getAudienceId(roleWithAudienceDO, sharingInitiatedOrgId, sharingInitiatedTenantDomain);
                Optional<String> roleId =
                        getRoleIdFromAudience(roleWithAudienceDO.getRoleName(), roleWithAudienceDO.getAudienceType(),
                                audienceId, sharingInitiatedTenantDomain);
                roleId.ifPresent(list::add);
            }
            return list;
        } catch (OrganizationManagementException e) {
            throw new UserSharingMgtServerException(ERROR_CODE_GET_ROLE_IDS);
        }
    }

    /**
     * Determines the audience ID based on the role's audience type.
     * If the audience is an organization, it returns the given organization ID.
     * If the audience is an application, it retrieves the application's resource ID.
     *
     * @param role          The role with audience details.
     * @param originalOrgId The ID of the organization where the role is being shared.
     * @param tenantDomain  The tenant domain associated with the organization.
     * @return The audience ID associated with the role.
     */
    private String getAudienceId(RoleWithAudienceDO role, String originalOrgId, String tenantDomain) {

        if (role == null || role.getAudienceType() == null) {
            return null;
        }

        try {
            if (StringUtils.equalsIgnoreCase(ORGANIZATION, role.getAudienceType())) {
                return originalOrgId;
            }
            if (StringUtils.equalsIgnoreCase(APPLICATION, role.getAudienceType())) {
                return getApplicationResourceId(role.getAudienceName(), tenantDomain);
            }
            LOG.warn(String.format(ERROR_CODE_INVALID_AUDIENCE_TYPE.getDescription(), role.getAudienceType()));
        } catch (IdentityApplicationManagementException e) {
            LOG.warn(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), role.getAudienceName()));
        }
        return null;
    }

    /**
     * Retrieves the resource ID of an application based on its name within the specified tenant domain.
     *
     * @param audienceName The name of the application.
     * @param tenantDomain The tenant domain where the application exists.
     * @return The resource ID of the application, or null if not found.
     */
    private String getApplicationResourceId(String audienceName, String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationBasicInfo applicationBasicInfo = getApplicationManagementService()
                .getApplicationBasicInfoByName(audienceName, tenantDomain);

        if (applicationBasicInfo != null) {
            return applicationBasicInfo.getApplicationResourceId();
        }
        LOG.warn(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), audienceName));
        return null;
    }

    /**
     * Retrieves the role ID associated with a given role name and audience within a specific tenant domain.
     *
     * @param roleName     The name of the role.
     * @param audienceType The type of audience (organization or application).
     * @param audienceId   The audience ID.
     * @param tenantDomain The tenant domain where the role exists.
     * @return An {@code Optional<String>} containing the role ID if found, otherwise empty.
     */
    private Optional<String> getRoleIdFromAudience(String roleName, String audienceType, String audienceId,
                                                   String tenantDomain) {

        if (audienceId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    getRoleManagementService().getRoleIdByName(roleName, audienceType, audienceId, tenantDomain));
        } catch (IdentityRoleManagementException e) {
            LOG.warn(String.format(ERROR_CODE_ROLE_NOT_FOUND.getMessage(), roleName, audienceType, audienceId));
            return Optional.empty();
        }
    }

    /**
     * Shares a user with a specified organization and assigns roles if present.
     * This is where the user association will be created, and roles will be assigned to the shared user if any
     * roles are present.
     * It attempts to share the user with the specified organization and assigns roles accordingly.
     *
     * @param orgId                 The ID of the organization to share the user with.
     * @param baseUserShare         The base user share object containing user and role information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param operationId           The ID of the sharing operation.
     */
    private void shareAndAssignRolesIfPresent(String orgId, BaseUserShare baseUserShare,
                                              String sharingInitiatedOrgId, String operationId)
            throws AsyncOperationStatusMgtException {

        String associatedUserId = baseUserShare.getUserId();
        List<String> roleIds = baseUserShare.getRoles();
        RoleAssignmentMode roleAssignmentMode = baseUserShare.getRoleAssignmentMode();
        UserAssociation userAssociation;

        UserSharingResultDO resultDO = new UserSharingResultDO(operationId, associatedUserId, false, false,
                OperationStatus.SUCCESS, StringUtils.EMPTY);

        try {
            userAssociation = shareUserWithOrganization(orgId, associatedUserId, sharingInitiatedOrgId);

        } catch (OrganizationManagementException e) {

            String errorMessage = String.format(ERROR_CODE_USER_SHARE.getMessage(), associatedUserId, e.getMessage());
            LOG.error(errorMessage, e);

            // Both User Share and Role Assignment Failed.
            registerOperationStatusUnit(operationId, associatedUserId, orgId, OperationStatus.FAILED,
                    errorMessage);
            return;
        }

        // Assign roles if RoleAssignmentMode is not NONE and if any roles are present.
        if (roleAssignmentMode != RoleAssignmentMode.NONE) {
            assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, roleIds, resultDO);
        }
    }

    /**
     * Assigns roles to the shared user if any roles are present.
     *
     * @param userAssociation       The user association object containing user and organization details.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     * @param resultDO              The result data object containing sharing result details.
     */
    private void assignRolesIfPresent(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                      List<String> roleIds, UserSharingResultDO resultDO)
            throws AsyncOperationStatusMgtException {

        if (!roleIds.isEmpty()) {
            UserSharingResultDO responseResultDO =
                    assignRolesToTheSharedUser(userAssociation, sharingInitiatedOrgId, roleIds, resultDO);

            registerOperationStatusUnit(resultDO.getOperationId(), resultDO.getAssociatedUserId(),
                    userAssociation.getOrganizationId(), responseResultDO.getOperationStatus(),
                    responseResultDO.getOperationStatusMessage());
        } else {
            registerOperationStatusUnit(resultDO.getOperationId(), resultDO.getAssociatedUserId(),
                    userAssociation.getOrganizationId(), resultDO.getOperationStatus(),
                    resultDO.getOperationStatusMessage());
        }
    }

    /**
     * Assigns the specified roles to the shared user within the target organization.
     * This method ensures that the shared user receives the appropriate roles (the roles ids of the corresponding
     * roles in the sub organization) based on the mappings between shared and main roles.
     *
     * @param userAssociation       The user association object containing user and organization details.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     * @param resultDO              The result data object containing sharing result details.
     */
    private UserSharingResultDO assignRolesToTheSharedUser(UserAssociation userAssociation,
                                                           String sharingInitiatedOrgId,
                                                           List<String> roleIds, UserSharingResultDO resultDO) {

        List<String> failedAssignedRoles = new ArrayList<>();
        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        try {
            String sharingInitiatedOrgTenantDomain =
                    getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
            String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

            String usernameWithDomain = userIDResolver.getNameByID(userId, targetOrgTenantDomain);
            String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
            String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

            RoleManagementService roleManagementService = getRoleManagementService();
            Map<String, String> sharedRoleToMainRoleMappingsBySubOrg =
                    roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(roleIds,
                            sharingInitiatedOrgTenantDomain);

            List<String> mainRoles = new ArrayList<>();
            for (String roleId : roleIds) {
                mainRoles.add(sharedRoleToMainRoleMappingsBySubOrg.getOrDefault(roleId, roleId));
            }

            Map<String, String> mainRoleToSharedRoleMappingsBySubOrg =
                    roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(mainRoles, targetOrgTenantDomain);
            for (String mainRoleId : mainRoles) {
                if (!mainRoleToSharedRoleMappingsBySubOrg.containsKey(mainRoleId)) {
                    failedAssignedRoles.add(roleManagementService.getRoleNameByRoleId(mainRoleId,
                            getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId)));
                }
            }
            for (String role : mainRoleToSharedRoleMappingsBySubOrg.values()) {
                try {
                    roleManagementService.updateUserListOfRole(role, Collections.singletonList(userId),
                            Collections.emptyList(), targetOrgTenantDomain);
                    roleManagementService.getRoleListOfUser(userId, targetOrgTenantDomain);

                    getOrganizationUserSharingService().addEditRestrictionsForSharedUserRole(role, username,
                            targetOrgTenantDomain, domainName, EditOperation.DELETE, sharingInitiatedOrgId);
                } catch (IdentityRoleManagementException | UserSharingMgtException e) {
                    failedAssignedRoles.add(role);
                    resultDO.setIsUserRoleAssignedIfPresentSuccess(false);
                }
            }
            if (!failedAssignedRoles.isEmpty()) {
                resultDO.setOperationStatus(OperationStatus.PARTIALLY_COMPLETED);
                resultDO.setOperationStatusMessage(buildPartialResultMessageForFailedRoles(failedAssignedRoles));
            } else if (resultDO.getIsUserSharedSuccess()) {
                resultDO.setOperationStatus(OperationStatus.SUCCESS);
                resultDO.setOperationStatusMessage(ROLE_UPDATE_SUCCESS_FOR_EXISTING_SHARED_USER);
            }
            return resultDO;
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            resultDO.setOperationStatus(OperationStatus.PARTIALLY_COMPLETED);
            resultDO.setOperationStatusMessage(ROLE_UPDATE_FAIL_FOR_NEW_SHARED_USER + e);
            resultDO.setIsUserRoleAssignedIfPresentSuccess(false);
            return resultDO;
        }
    }

    private String buildPartialResultMessageForFailedRoles(List<String> failedAssignedRoles) {

        StringBuilder error = new StringBuilder("User shared and Failed assigning roles: ");
        for (int i = 0; i < failedAssignedRoles.size(); i++) {
            error.append(failedAssignedRoles.get(i));
            if (i < failedAssignedRoles.size() - 1) {
                error.append(", ");
            }
        }
        return error.toString();
    }

    // Async helpers.

    /**
     * Restores thread-local properties for async execution.
     */
    private void restoreThreadLocalContext(String tenantDomain, int tenantId, String username,
                                           Map<String, Object> threadLocalProperties) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();

        carbonContext.setTenantDomain(tenantDomain, true);
        carbonContext.setTenantId(tenantId);
        carbonContext.setUsername(username);

        // Restore all thread-local properties.
        IdentityUtil.threadLocalProperties.get().putAll(threadLocalProperties);
    }

    private OperationStatus getOperationStatus(String operationId) {

        SubOperationStatusQueue list = asyncOperationStatusList.get(operationId);
        OperationStatus status = list.getOperationStatus();
        asyncOperationStatusList.remove(operationId);
        return status;
    }

    private String getCorrelationId() {

        String ref;
        if (isCorrelationIDPresent()) {
            ref = MDC.get(CORRELATION_ID_MDC);
        } else {
            ref = UUID.randomUUID().toString();
        }
        return ref;
    }

    private boolean isCorrelationIDPresent() {

        return MDC.get(CORRELATION_ID_MDC) != null;
    }

    // Validation Methods.

    private <T extends UserCriteriaType> void validateUserShareInput(BaseUserShareDO<T> baseUserShareDO)
            throws UserSharingMgtClientException {

        if (baseUserShareDO == null) {
            throwValidationException(ERROR_CODE_NULL_SHARE);
        }

        if (baseUserShareDO instanceof SelectiveUserShareV2DO) {
            validateSelectiveUserShareV2DO((SelectiveUserShareV2DO) baseUserShareDO);
        } else if (baseUserShareDO instanceof GeneralUserShareV2DO) {
            validateGeneralUserShareV2DO((GeneralUserShareV2DO) baseUserShareDO);
        }
    }

    private void validateSelectiveUserShareV2DO(SelectiveUserShareV2DO selectiveUserShareV2DO)
            throws UserSharingMgtClientException {

        validateNotNull(selectiveUserShareV2DO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!selectiveUserShareV2DO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserShareV2DO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
        validateNotNull(selectiveUserShareV2DO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        for (SelectiveUserShareOrgDetailsV2DO orgDetails : selectiveUserShareV2DO.getOrganizations()) {
            validateNotNull(orgDetails, ERROR_CODE_ORG_DETAILS_NULL);
            validateNotNull(orgDetails.getOrganizationId(), ERROR_CODE_ORG_ID_NULL);
            validateNotNull(orgDetails.getPolicy(), ERROR_CODE_POLICY_NULL);
            validateRoleAssignments(orgDetails.getRoleAssignments());
        }
    }

    private void validateGeneralUserShareV2DO(GeneralUserShareV2DO generalUserShareV2DO)
            throws UserSharingMgtClientException {

        validateNotNull(generalUserShareV2DO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!generalUserShareV2DO.getUserCriteria().containsKey(USER_IDS) ||
                generalUserShareV2DO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
        validateNotNull(generalUserShareV2DO.getPolicy(), ERROR_CODE_POLICY_NULL);
        validateRoleAssignments(generalUserShareV2DO.getRoleAssignments());
    }

    private <T extends UserCriteriaType> void validateUserUnshareInput(BaseUserUnshareDO<T> userUnshareDO)
            throws UserSharingMgtClientException {

        if (userUnshareDO == null) {
            throwValidationException(ERROR_CODE_NULL_UNSHARE);
        }

        if (userUnshareDO instanceof SelectiveUserUnshareDO) {
            validateSelectiveUserUnshareDO((SelectiveUserUnshareDO) userUnshareDO);
        } else if (userUnshareDO instanceof GeneralUserUnshareDO) {
            validateGeneralUserUnshareDO((GeneralUserUnshareDO) userUnshareDO);
        }
    }

    private void validateSelectiveUserUnshareDO(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(selectiveUserUnshareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!selectiveUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        // Validate organizations list is not null.
        validateNotNull(selectiveUserUnshareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        for (String organization : selectiveUserUnshareDO.getOrganizations()) {
            validateNotNull(organization, ERROR_CODE_ORG_ID_NULL);
        }
    }

    private void validateGeneralUserUnshareDO(GeneralUserUnshareDO generalUserUnshareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(generalUserUnshareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!generalUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                generalUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
    }

    private void validateRoleAssignments(RoleAssignmentDO roleAssignments) throws UserSharingMgtClientException {

        validateNotNull(roleAssignments, ERROR_CODE_ROLES_NULL);
        validateNotNull(roleAssignments.getMode(), ERROR_CODE_ROLES_NULL);

        if (RoleAssignmentMode.SELECTED == roleAssignments.getMode()) {
            validateNotNull(roleAssignments.getRoles(), ERROR_CODE_ROLES_NULL);
        }

        if (roleAssignments.getRoles() != null) {
            for (RoleWithAudienceDO role : roleAssignments.getRoles()) {
                validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
                validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
                validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
            }
        }
    }

    private void validateNotNull(Object obj, UserSharingConstants.ErrorMessage error)
            throws UserSharingMgtClientException {

        if (obj == null) {
            throwValidationException(error);
        }
    }

    private void throwValidationException(UserSharingConstants.ErrorMessage error)
            throws UserSharingMgtClientException {

        throw new UserSharingMgtClientException(error.getCode(), error.getMessage(), error.getDescription());
    }

    // Service getters.

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private AsyncOperationStatusMgtService getAsyncStatusMgtService() {

        return OrganizationUserSharingDataHolder.getInstance().getAsyncOperationStatusMgtService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService();
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        try {
            RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
            UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
            return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException("Error occurred while retrieving the user store manager.", e);
        }
    }
}
