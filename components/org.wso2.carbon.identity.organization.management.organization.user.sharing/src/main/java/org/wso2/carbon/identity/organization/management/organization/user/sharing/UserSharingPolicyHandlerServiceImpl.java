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

import org.apache.commons.collections.CollectionUtils;
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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseLinkDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedRolesDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.UserSharingResultDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIdList;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
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
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
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

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.API_REF_GET_SHARED_ROLES_OF_USER_IN_ORG;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.B2B_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.B2B_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CORRELATION_ID_MDC;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.EXISTING_USER_UNSHARE_FAIL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.EXISTING_USER_UNSHARE_SUCCESS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL;
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
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ROLE_UPDATE_FAIL_FOR_EXISTING_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ROLE_UPDATE_FAIL_FOR_NEW_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ROLE_UPDATE_SUCCESS_FOR_EXISTING_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Implementation of the user sharing policy handler service.
 */
public class UserSharingPolicyHandlerServiceImpl implements UserSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(UserSharingPolicyHandlerServiceImpl.class);
    private final UserIDResolver userIDResolver = new UserIDResolver();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);
    private final ConcurrentMap<String, SubOperationStatusQueue> asyncOperationStatusList = new ConcurrentHashMap<>();

    @Override
    public void populateSelectiveUserShare(SelectiveUserShareDO selectiveUserShareDO) throws UserSharingMgtException {

        validateUserShareInput(selectiveUserShareDO);
        String sharingInitiatedOrgId = getOrganizationId();

        List<SelectiveUserShareOrgDetailsDO> organizations = selectiveUserShareDO.getOrganizations();
        Map<String, UserCriteriaType> userCriteria = selectiveUserShareDO.getUserCriteria();

        List<SelectiveUserShareOrgDetailsDO> validOrganizations =
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
                restoreThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                        sharingInitiatedUsername, threadLocalProperties);
                processSelectiveUserShare(userCriteria, validOrganizations, sharingInitiatedOrgId,
                        sharingInitiatedUserId, getCorrelationId()); }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async user selective share processing.", ex);
                    return null;
                });
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

    @Override
    public void populateGeneralUserShare(GeneralUserShareDO generalUserShareDO) throws UserSharingMgtException {

        validateUserShareInput(generalUserShareDO);
        String sharingInitiatedOrgId = getOrganizationId();

        Map<String, UserCriteriaType> userCriteria = generalUserShareDO.getUserCriteria();
        PolicyEnum policy = generalUserShareDO.getPolicy();
        List<String> roleIds = getRoleIds(generalUserShareDO.getRoles(), sharingInitiatedOrgId);

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
                    restoreThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                            sharingInitiatedUsername, threadLocalProperties);
                    processGeneralUserShare(userCriteria, policy, roleIds, sharingInitiatedOrgId,
                            sharingInitiatedUserId, getCorrelationId()); }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general user share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateSelectiveUserUnshare(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtException {

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
    public void populateGeneralUserUnshare(GeneralUserUnshareDO generalUserUnshareDO) throws UserSharingMgtException {

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
    public ResponseSharedOrgsDO getSharedOrganizationsOfUser(String associatedUserId, String after, String before,
                                                             Integer limit, String filter, Boolean recursive)
            throws UserSharingMgtException {

        try {
            String sharingInitiatedOrgId = getOrganizationId();
            List<ResponseOrgDetailsDO> responseOrgDetailsDOS = new ArrayList<>();
            List<ResponseLinkDO> responseLinkList = Collections.singletonList(new ResponseLinkDO());
            List<UserAssociation> userAssociations =
                    getOrganizationUserSharingService().getUserAssociationsOfGivenUser(associatedUserId,
                            sharingInitiatedOrgId);

            for (UserAssociation userAssociation : userAssociations) {
                ResponseOrgDetailsDO responseOrgDetailsDO = new ResponseOrgDetailsDO();
                responseOrgDetailsDO.setOrganizationId(userAssociation.getOrganizationId());
                responseOrgDetailsDO.setOrganizationName(getOrganizationName(userAssociation.getOrganizationId()));
                responseOrgDetailsDO.setSharedUserId(userAssociation.getUserId());
                responseOrgDetailsDO.setSharedType(userAssociation.getSharedType());
                responseOrgDetailsDO.setRolesRef(getRolesRef(associatedUserId, userAssociation.getOrganizationId()));
                responseOrgDetailsDOS.add(responseOrgDetailsDO);
            }

            return new ResponseSharedOrgsDO(responseLinkList, responseOrgDetailsDOS);
        } catch (OrganizationManagementException e) {
            throw new UserSharingMgtClientException(ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER);
        }
    }

    @Override
    public ResponseSharedRolesDO getRolesSharedWithUserInOrganization(String associatedUserId, String orgId,
                                                                      String after, String before, Integer limit,
                                                                      String filter, Boolean recursive)
            throws UserSharingMgtException {

        try {
            List<RoleWithAudienceDO> roleWithAudienceList = new ArrayList<>();
            List<ResponseLinkDO> responseLinkList = Collections.singletonList(new ResponseLinkDO());
            UserAssociation userAssociation =
                    getOrganizationUserSharingService().getUserAssociationOfAssociatedUserByOrgId(associatedUserId,
                            orgId);

            if (userAssociation == null) {
                return new ResponseSharedRolesDO(responseLinkList, roleWithAudienceList);
            }

            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

            String usernameWithDomain = userIDResolver.getNameByID(userAssociation.getUserId(), tenantDomain);
            String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
            String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

            List<String> sharedRoleIdsInOrg =
                    getOrganizationUserSharingService().getRolesSharedWithUserInOrganization(username, tenantId,
                            domainName);

            if (CollectionUtils.isEmpty(sharedRoleIdsInOrg)) {
                return new ResponseSharedRolesDO(responseLinkList, roleWithAudienceList);
            }

            RoleManagementService roleManagementService = getRoleManagementService();

            for (String sharedRoleId : sharedRoleIdsInOrg) {
                Role role = roleManagementService.getRole(sharedRoleId, tenantDomain);
                RoleWithAudienceDO roleWithAudience = new RoleWithAudienceDO();
                roleWithAudience.setRoleName(role.getName());
                roleWithAudience.setAudienceName(role.getAudienceName());
                roleWithAudience.setAudienceType(role.getAudience());
                roleWithAudienceList.add(roleWithAudience);
            }

            return new ResponseSharedRolesDO(responseLinkList, roleWithAudienceList);
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            throw new UserSharingMgtClientException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER);
        }
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
                                           List<SelectiveUserShareOrgDetailsDO> organizations,
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
            if(LOG.isDebugEnabled()) {
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
                                         List<String> roleIds, String sharingInitiatedOrgId,
                                         String sharingInitiatedUserId, String correlationId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            generalUserShareByUserIds((UserIdList) criterionValues, policy, roleIds,
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
            if(LOG.isDebugEnabled()) {
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
            if(LOG.isDebugEnabled()) {
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
            if(LOG.isDebugEnabled()) {
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
    private void selectiveUserShareByUserIds(UserIdList userIds, List<SelectiveUserShareOrgDetailsDO> organizations,
                                             String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                             String correlationId) throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                if (isExistingUser(associatedUserId, sharingInitiatedOrgId) &&
                        isResidentUserInOrg(associatedUserId, sharingInitiatedOrgId)) {

                    List<BaseUserShare> selectiveUserShareObjectsInRequest = new ArrayList<>();
                    for (SelectiveUserShareOrgDetailsDO organization : organizations) {
                        SelectiveUserShare selectiveUserShare = new SelectiveUserShare.Builder()
                                .withUserId(associatedUserId)
                                .withOrganizationId(organization.getOrganizationId())
                                .withPolicy(organization.getPolicy())
                                .withRoles(getRoleIds(organization.getRoles(), sharingInitiatedOrgId))
                                .build();
                        selectiveUserShareObjectsInRequest.add(selectiveUserShare);
                    }
                    shareUser(associatedUserId, selectiveUserShareObjectsInRequest, sharingInitiatedOrgId,
                            sharingInitiatedUserId, correlationId);
                } else {
                    if(LOG.isDebugEnabled()) {
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
                                           String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                           String correlationId) throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                if (isExistingUser(associatedUserId, sharingInitiatedOrgId) &&
                        isResidentUserInOrg(associatedUserId, sharingInitiatedOrgId)) {
                    GeneralUserShare generalUserShare = new GeneralUserShare.Builder()
                            .withUserId(associatedUserId)
                            .withPolicy(policy)
                            .withRoles(roleIds)
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

                    getOrganizationUserSharingService().unshareOrganizationUserInSharedOrganization(associatedUserId,
                            organizationId);

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
            throws OrganizationManagementException,
            UserSharingMgtException, IdentityRoleManagementException, ResourceSharingPolicyMgtException {

        if (!baseUserShareObjects.isEmpty()) {
            Map<BaseUserShare, List<String>> userSharingOrgsForEachUserShareObject =
                    getUserSharingOrgsForEachUserShareObject(baseUserShareObjects, sharingInitiatedOrgId);

            try {
                if (isUserAlreadyShared(associatedUserId, sharingInitiatedOrgId)) {
                    handleExistingSharedUser(associatedUserId, sharingInitiatedOrgId,
                            userSharingOrgsForEachUserShareObject, sharingInitiatedUserId, correlationId);
                } else {
                    createNewUserShare(sharingInitiatedOrgId, userSharingOrgsForEachUserShareObject,
                            sharingInitiatedUserId, correlationId);
                }
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

    /**
     * Handles the case where the user is already shared with some organizations. It updates the sharing details
     * and roles for the user in the specified organizations and cleans up old associations if necessary.
     *
     * @param associatedUserId                      The ID of the user to be shared.
     * @param sharingInitiatedOrgId                 The ID of the organization initiating the sharing.
     * @param userSharingOrgsForEachUserShareObject A map containing user share objects and their corresponding
     *                                              organizations.
     * @param sharingInitiatedUserId                The ID of the user that initiated the user sharing.
     * @param correlationId                         The correlation ID to track down the user sharing.
     */
    private void handleExistingSharedUser(String associatedUserId, String sharingInitiatedOrgId, Map<BaseUserShare,
            List<String>> userSharingOrgsForEachUserShareObject, String sharingInitiatedUserId, String correlationId)
            throws UserSharingMgtException, IdentityRoleManagementException, OrganizationManagementException,
            ResourceSharingPolicyMgtException, AsyncOperationStatusMgtException {

        processUserSharingUpdates(userSharingOrgsForEachUserShareObject, associatedUserId,
                sharingInitiatedOrgId, sharingInitiatedUserId, correlationId);

        updateResourceSharingPolicies(userSharingOrgsForEachUserShareObject.keySet(), associatedUserId,
                sharingInitiatedOrgId);
    }

    /**
     * Processes user sharing updates by updating existing associations, sharing with new organizations,
     * and cleaning up old associations if necessary.
     *
     * @param associatedUserId                      The ID of the user to be shared.
     * @param sharingInitiatedOrgId                 The ID of the organization initiating the sharing.
     * @param userSharingOrgsForEachUserShareObject A map containing user share objects and their corresponding
     *                                              organizations.
     * @param sharingInitiatedUserId                The ID of the user that initiated the user sharing.
     * @param correlationId                         The correlation ID to track down the user sharing.
     */
    private void processUserSharingUpdates(Map<BaseUserShare, List<String>> userSharingOrgsForEachUserShareObject,
                                           String associatedUserId, String sharingInitiatedOrgId,
                                           String sharingInitiatedUserId, String correlationId)
            throws UserSharingMgtException, IdentityRoleManagementException, OrganizationManagementException,
            AsyncOperationStatusMgtException {

        List<String> userSharingAllOrgs = userSharingOrgsForEachUserShareObject.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        for (Map.Entry<BaseUserShare, List<String>> entry : userSharingOrgsForEachUserShareObject.entrySet()) {

            String operationId = registerOperationStatus(entry, sharingInitiatedOrgId, sharingInitiatedUserId,
                    correlationId);
            BaseUserShare baseUserShare = entry.getKey();
            List<String> userSharingOrgList = entry.getValue();
            List<String> retainedSharedOrgs = new ArrayList<>();
            List<UserAssociation> userAssociations =
                    getUserAssociationsOfGivenUserOnOrgTree(baseUserShare, sharingInitiatedOrgId);

            for (UserAssociation association : userAssociations) {

                if (!userSharingOrgList.contains(association.getOrganizationId())) {
                    try {
                        unshareUserFromPreviousOrg(association, sharingInitiatedOrgId);
                        registerOperationStatusUnit(operationId, association.getUserId(), sharingInitiatedOrgId,
                                OperationStatus.SUCCESS, EXISTING_USER_UNSHARE_SUCCESS);
                    } catch (UserSharingMgtException e) {
                        registerOperationStatusUnit(operationId, association.getUserId(), sharingInitiatedOrgId,
                                OperationStatus.PARTIALLY_COMPLETED, EXISTING_USER_UNSHARE_FAIL +
                                        e.getMessage());
                        throw e;
                    }
                } else {
                    retainedSharedOrgs.add(association.getOrganizationId());
                    UserSharingResultDO resultDO = new UserSharingResultDO(operationId, associatedUserId, true, false,
                            OperationStatus.SUCCESS, StringUtils.EMPTY);
                    updateRolesIfNecessary(association, baseUserShare.getRoles(), sharingInitiatedOrgId, resultDO);
                    updateSharedTypeOfExistingUserAssociation(association);
                }
            }
            shareWithNewOrganizations(baseUserShare, sharingInitiatedOrgId, userSharingOrgList, retainedSharedOrgs,
                    operationId);
            if (StringUtils.isNotBlank(operationId)) {
                getAsyncStatusMgtService().updateOperationStatus(operationId, getOperationStatus(operationId));
            }
        }
        cleanUpOldUserAssociationsIfExists(associatedUserId, sharingInitiatedOrgId, userSharingAllOrgs);
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
     * Retrieves the user associations of a given user within the organization tree.
     * The root organization of the tree is the policy-holding organization.
     * This method takes a base share object, which contains the policy-holding organization for the share,
     * and retrieves the user associations for the organizations in that tree.
     *
     * @param baseUserShare         The user share object, which can be either selective or general.
     * @param sharingInitiatedOrgId The ID of the organization from which the sharing request was initiated.
     * @return A list of user associations within the organization tree.
     */
    private List<UserAssociation> getUserAssociationsOfGivenUserOnOrgTree(BaseUserShare baseUserShare,
                                                                          String sharingInitiatedOrgId)
            throws OrganizationManagementException {

        String associatedUserId = baseUserShare.getUserId();
        String orgId = (baseUserShare instanceof SelectiveUserShare)
                ? ((SelectiveUserShare) baseUserShare).getOrganizationId()
                : sharingInitiatedOrgId;

        List<String> orgsInOrgTree = getOrganizationManager().getChildOrganizationsIds(orgId, true);
        orgsInOrgTree.add(orgId);

        return getOrganizationUserSharingService().getUserAssociationsOfGivenUserOnGivenOrgs(associatedUserId,
                orgsInOrgTree);
    }

    /**
     * Updates the resource sharing policies for a given user.
     * This method first deletes all existing resource sharing policies for the user,
     * and then saves new policies if applicable.
     *
     * @param baseUserShares        A set of {@link BaseUserShare} objects containing the new sharing policies.
     * @param associatedUserId      The unique identifier of the user whose policies are being updated.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @throws ResourceSharingPolicyMgtException If an error occurs while updating the resource sharing policies.
     */
    private void updateResourceSharingPolicies(Set<BaseUserShare> baseUserShares, String associatedUserId,
                                               String sharingInitiatedOrgId) throws ResourceSharingPolicyMgtException {

        deleteAllResourceSharingPoliciesOfUser(associatedUserId, sharingInitiatedOrgId);

        for (BaseUserShare baseUserShare : baseUserShares) {
            if (isApplicableOrganizationScopeForSavingPolicy(baseUserShare.getPolicy())) {
                saveUserSharingPolicy(baseUserShare, sharingInitiatedOrgId);
            }
        }
    }

    /**
     * In the cases where the user sharing is shifted from general to selective without unsharing the user from all
     * organizations, this method cleans up the old user associations and resource sharing policies under the
     * organizations which are not selected in the selective user share.
     *
     * @param associatedUserId      The ID of the associated user.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @param userSharingAllOrgs    The list of all organizations with which the user is shared.
     */
    private void cleanUpOldUserAssociationsIfExists(String associatedUserId, String sharingInitiatedOrgId,
                                                    List<String> userSharingAllOrgs)
            throws OrganizationManagementException, UserSharingMgtException {

        List<UserAssociation> allUserAssociations =
                getSharedUserAssociationsOfGivenUser(associatedUserId, sharingInitiatedOrgId);

        for (UserAssociation association : allUserAssociations) {
            if (!userSharingAllOrgs.contains(association.getOrganizationId())) {
                unshareUserFromPreviousOrg(association, sharingInitiatedOrgId);
            }
        }
    }

    /**
     * Updates the shared type of existing user association from INVITED to SHARED if applicable.
     * This method checks if the current user has previously been associated with the sharing organization via an
     * invitation, and if so, changes the type of the user association to SHARED. This is because the user
     * association of that particular user will now be considered as a shared user rather than an invited user.
     * This decision prioritizes the user share over the invitation since user sharing is done by a parent
     * organization's admin.
     *
     * @param association The user association to be updated.
     */
    private void updateSharedTypeOfExistingUserAssociation(UserAssociation association) {

        try {
            if (SharedType.INVITED.equals(association.getSharedType())) {
                getOrganizationUserSharingService().updateSharedTypeOfUserAssociation(association.getId(),
                        SharedType.SHARED);
            }
        } catch (OrganizationManagementException e) {
            LOG.error("Error occurred while converting the shared type of the user association of: " +
                    association.getAssociatedUserId() + " from Invited to shared", e);
        }
    }

    /**
     * Unshare a user from a previously shared organization.
     *
     * @param association           The user association to be unshared.
     * @param sharingInitiatedOrgId The ID of the organization initiating the unsharing.
     */
    private void unshareUserFromPreviousOrg(UserAssociation association, String sharingInitiatedOrgId)
            throws UserSharingMgtException {

        selectiveUserUnshareByUserIds(new UserIdList(Collections.singletonList(association.getAssociatedUserId())),
                Collections.singletonList(association.getOrganizationId()), sharingInitiatedOrgId);
    }

    /**
     * Shares the user with new organizations that are not already shared.
     *
     * @param baseUserShare         The base user share object containing sharing details.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @param userSharingOrgList    The list of organizations to share the user with.
     * @param alreadySharedOrgs     The list of organizations the user is already shared with.
     * @param operationId           The ID of the sharing operation.
     */
    private void shareWithNewOrganizations(BaseUserShare baseUserShare, String sharingInitiatedOrgId,
                                           List<String> userSharingOrgList, List<String> alreadySharedOrgs,
                                           String operationId) throws AsyncOperationStatusMgtException {

        List<String> newlySharedOrgs = new ArrayList<>(userSharingOrgList);
        newlySharedOrgs.removeAll(alreadySharedOrgs);

        for (String orgId : newlySharedOrgs) {
            shareAndAssignRolesIfPresent(orgId, baseUserShare, sharingInitiatedOrgId, operationId);
        }
    }

    /**
     * This method checks if at least one of the organizations has an association with this user.
     *
     * @param associatedUserId The ID of the user to check.
     * @param associatedOrgId  The ID of the organization.
     * @return {@code true} if the user has at least one associated organization, {@code false} otherwise.
     */
    private boolean isUserAlreadyShared(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        return getOrganizationUserSharingService().hasUserAssociations(associatedUserId, associatedOrgId);
    }

    /**
     * Retrieves the list of shared user associations for a given user.
     *
     * @param associatedUserId The ID of the associated user.
     * @param associatedOrgId  The ID of the associated organization.
     * @return A list of {@code UserAssociation} objects representing shared user associations.
     */
    private List<UserAssociation> getSharedUserAssociationsOfGivenUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        return getOrganizationUserSharingService().getUserAssociationsOfGivenUser(associatedUserId, associatedOrgId);
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
     * Retrieves the name of an organization based on its ID.
     *
     * @param organizationId The ID of the organization.
     * @return The name of the organization.
     */
    private String getOrganizationName(String organizationId) throws OrganizationManagementException {

        return getOrganizationManager().getOrganizationNameById(organizationId);
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
    private List<SelectiveUserShareOrgDetailsDO> filterValidOrganizations(
            List<SelectiveUserShareOrgDetailsDO> organizations, String sharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        List<String> immediateChildOrgs = getImmediateChildOrgsOfSharingInitiatedOrg(sharingInitiatedOrgId);

        List<SelectiveUserShareOrgDetailsDO> validOrganizations = organizations.stream()
                .filter(org -> immediateChildOrgs.contains(org.getOrganizationId()))
                .collect(Collectors.toList());

        List<String> skippedOrganizations = organizations.stream()
                .map(SelectiveUserShareOrgDetailsDO::getOrganizationId)
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
     * Deletes all resource sharing policies for a given user.
     *
     * @param associatedUserId      The ID of the associated user.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteAllResourceSharingPoliciesOfUser(String associatedUserId,
                                                        String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(ResourceType.USER,
                associatedUserId, sharingInitiatedOrgId);
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
     * Checks if there are any role changes when updating a shared user.
     *
     * @param oldSharedRoleIds The list of old shared role IDs.
     * @param newRoleIds       The list of new role IDs.
     * @return True if there are role changes, false otherwise.
     */
    private boolean hasRoleChanges(List<String> oldSharedRoleIds, List<String> newRoleIds) {

        return !new HashSet<>(oldSharedRoleIds).equals(new HashSet<>(newRoleIds));
    }

    /**
     * Updates the roles assigned to a shared user if necessary.
     * It retrieves the current shared roles and determines the roles to be added.
     * If there are any changes in the roles, the new roles are assigned.
     *
     * @param userAssociation       The user association object containing user and organization details.
     * @param roleIds               The list of role IDs to be updated.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param resultDO              The result data object containing sharing result details.
     */
    private void updateRolesIfNecessary(UserAssociation userAssociation, List<String> roleIds,
                                        String sharingInitiatedOrgId, UserSharingResultDO resultDO)
            throws OrganizationManagementException, IdentityRoleManagementException, AsyncOperationStatusMgtException {

        try {
            List<String> currentSharedRoleIds = getCurrentSharedRoleIdsForSharedUser(userAssociation);
            List<String> newSharedRoleIds =
                    getRolesToBeAddedAfterUpdate(userAssociation, currentSharedRoleIds, roleIds);

            if (hasRoleChanges(currentSharedRoleIds, newSharedRoleIds)) {
                assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, newSharedRoleIds, resultDO);
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            registerOperationStatusUnit(resultDO.getOperationId(), userAssociation.getUserId(), sharingInitiatedOrgId,
                    OperationStatus.PARTIALLY_COMPLETED, ROLE_UPDATE_FAIL_FOR_EXISTING_SHARED_USER + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves the role IDs currently assigned to a shared user within an organization.
     *
     * @param userAssociation The user association object containing user and organization details.
     * @return A list of role IDs currently assigned to the shared user.
     */
    private List<String> getCurrentSharedRoleIdsForSharedUser(UserAssociation userAssociation)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        List<String> allUserRolesOfSharedUser = getRoleManagementService().getRoleIdListOfUser(userId, tenantDomain);

        return getOrganizationUserSharingService().getSharedUserRolesFromUserRoles(allUserRolesOfSharedUser,
                tenantDomain);
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

        // Assign roles if any are present.
        assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, roleIds, resultDO);
    }

    /**
     * Determines the roles that need to be added after an update by comparing the current roles
     * with the new set of roles. Note that this will only be affected to the roles which have been assigned to user
     * with the sharing of that user. If this particular user has been assigned to some roles within the sub
     * organization, there won't be any effect to those roles.
     *
     * @param userAssociation The user association object containing user and organization details.
     * @param currentRoleIds  The list of role IDs currently assigned to the shared user.
     * @param newRoleIds      The list of new role IDs to be assigned.
     * @return A list of roles that need to be added.
     */
    private List<String> getRolesToBeAddedAfterUpdate(UserAssociation userAssociation, List<String> currentRoleIds,
                                                      List<String> newRoleIds)
            throws OrganizationManagementException, IdentityRoleManagementException {

        // Roles to be added are those in newRoleIds that are not in currentRoleIds.
        List<String> rolesToBeAdded = new ArrayList<>(newRoleIds);
        rolesToBeAdded.removeAll(currentRoleIds);

        // Roles to be removed are those in currentRoleIds that are not in newRoleIds.
        List<String> rolesToBeRemoved = new ArrayList<>(currentRoleIds);
        rolesToBeRemoved.removeAll(newRoleIds);

        deleteOldSharedRoles(userAssociation, rolesToBeRemoved);
        return rolesToBeAdded;
    }

    /**
     * Removes roles that are no longer assigned to the shared user.
     *
     * @param userAssociation  The user association object containing user and organization details.
     * @param rolesToBeRemoved The list of role IDs to be removed.
     */
    private void deleteOldSharedRoles(UserAssociation userAssociation, List<String> rolesToBeRemoved)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        for (String roleId : rolesToBeRemoved) {
            getRoleManagementService().updateUserListOfRole(roleId, Collections.emptyList(),
                    Collections.singletonList(userId), targetOrgTenantDomain);
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

    /**
     * Constructs a reference URL to retrieve the shared roles of a user within an organization.
     *
     * @param userId The ID of the user.
     * @param orgId  The ID of the organization.
     * @return The formatted reference URL for retrieving shared roles.
     */
    private String getRolesRef(String userId, String orgId) {

        return String.format(API_REF_GET_SHARED_ROLES_OF_USER_IN_ORG, userId, orgId);
    }

    // Validation Methods.

    private <T extends UserCriteriaType> void validateUserShareInput(BaseUserShareDO<T> baseUserShareDO)
            throws UserSharingMgtClientException {

        if (baseUserShareDO == null) {
            throwValidationException(ERROR_CODE_NULL_SHARE);
        }

        if (baseUserShareDO instanceof SelectiveUserShareDO) {
            validateSelectiveUserShareDO((SelectiveUserShareDO) baseUserShareDO);
        } else if (baseUserShareDO instanceof GeneralUserShareDO) {
            validateGeneralUserShareDO((GeneralUserShareDO) baseUserShareDO);
        }
    }

    private void validateSelectiveUserShareDO(SelectiveUserShareDO selectiveUserShareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(selectiveUserShareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!selectiveUserShareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserShareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        // Validate organizations list is not null.
        validateNotNull(selectiveUserShareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        // Validate each organization in the list.
        for (SelectiveUserShareOrgDetailsDO orgDetails : selectiveUserShareDO.getOrganizations()) {
            validateNotNull(orgDetails.getOrganizationId(), ERROR_CODE_ORG_ID_NULL);
            validateNotNull(orgDetails.getPolicy(), ERROR_CODE_POLICY_NULL);

            // Validate roles list is not null (it can be empty).
            if (orgDetails.getRoles() == null) {
                throwValidationException(ERROR_CODE_ROLES_NULL);
            } else {
                // Validate each role's properties if present.
                for (RoleWithAudienceDO role : orgDetails.getRoles()) {
                    validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
                    validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
                    validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
                }
            }
        }
    }

    private void validateGeneralUserShareDO(GeneralUserShareDO generalDO) throws UserSharingMgtClientException {

        validateNotNull(generalDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!generalDO.getUserCriteria().containsKey(USER_IDS) || generalDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
        validateNotNull(generalDO.getPolicy(), ERROR_CODE_POLICY_NULL);
        validateNotNull(generalDO.getRoles(), ERROR_CODE_ROLES_NULL);

        // Validate each role's properties if present.
        for (RoleWithAudienceDO role : generalDO.getRoles()) {
            validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
            validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
            validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
        }
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

    // Service getters.

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
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

    private AsyncOperationStatusMgtService getAsyncStatusMgtService() {

        return OrganizationUserSharingDataHolder.getInstance().getAsyncOperationStatusMgtService();
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
}
