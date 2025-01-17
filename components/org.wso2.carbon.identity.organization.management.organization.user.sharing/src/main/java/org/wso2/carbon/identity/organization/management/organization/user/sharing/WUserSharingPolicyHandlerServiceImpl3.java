/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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
 *//*


package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.ResourceSharingPolicyHandlerDAO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.ResourceSharingPolicyHandlerDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.SelectiveUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserSharingDetails;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIds;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_SKIP_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.LOG_INFO_SELECTIVE_SHARE_COMPLETED;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.NULL_INPUT_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.POLICY_CODE_FOR_EXISTING_AND_FUTURE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.POLICY_CODE_FOR_FUTURE_ONLY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.SHARING_TYPE_SHARED;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getUserStoreManager;

*/
/**
 * Service implementation for handling user sharing policies.
 *//*

public class WUserSharingPolicyHandlerServiceImpl3 implements WUserSharingPolicyHandlerService2 {

    private static final Log LOG = LogFactory.getLog(WUserSharingPolicyHandlerServiceImpl3.class);
    private static final ResourceSharingPolicyHandlerDAO resourceSharingPolicyHandlerDAO =
            new ResourceSharingPolicyHandlerDAOImpl();
    private static ConcurrentLinkedQueue<String> errorMessages;

    */
/**
     * Propagates the selective share of a user to specific organizations.
     *
     * @param selectiveUserShareDO Contains details for selective sharing.
     *//*

    @Override
    public void populateSelectiveUserShare(SelectiveUserShareDO selectiveUserShareDO)
            throws UserShareMgtServerException, OrganizationManagementException, IdentityRoleManagementException,
            UserStoreException, IdentityApplicationManagementException, ResourceSharingPolicyMgtException,
            DataAccessException {

        validateInput(selectiveUserShareDO);
        List<SelectiveUserShareOrgDetailsDO> organizations = selectiveUserShareDO.getOrganizations();
        Map<String, UserCriteriaType> userCriteria = selectiveUserShareDO.getUserCriteria();

        List<String> sharingInitiatedOrg =
                getOrganizationManager().getChildOrganizationsIds(getOrganizationId(), false);

        for (SelectiveUserShareOrgDetailsDO organization : organizations) {
            if (sharingInitiatedOrg.contains(organization.getOrganizationId())) {
                populateSelectiveUserShareByCriteria(organization, userCriteria);
            } else {
                LOG.info(ERROR_SKIP_SHARE.getMessage());
                errorMessages.offer(ERROR_SKIP_SHARE.getMessage());
            }
        }

        LOG.info(LOG_INFO_SELECTIVE_SHARE_COMPLETED);

        // After parallel processing, check for errors and handle them.
        if (!errorMessages.isEmpty()) {
            throw new OrganizationManagementException(
                    "Failed to share user with some organizations: " + String.join(", ", errorMessages));
        }

    }

    private void populateSelectiveUserShareByCriteria(SelectiveUserShareOrgDetailsDO organization,
                                                      Map<String, UserCriteriaType> userCriteria)
            throws OrganizationManagementException, IdentityApplicationManagementException,
            IdentityRoleManagementException, UserStoreException, ResourceSharingPolicyMgtException,
            DataAccessException {

        for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            UserCriteriaType criterionValues = criterion.getValue();

            switch (criterionKey) {
                case USER_IDS:
                    if (criterionValues instanceof UserIds) {
                        populateSelectiveUserShareByUserIds((UserIds) criterionValues, organization);
                    } else {
                        throw new OrganizationManagementException("Invalid type for USER_IDS criterion.");
                    }
                    break;
                default:
                    throw new OrganizationManagementException("Invalid user criterion provided: " + criterionKey);
            }
        }
    }

    private void populateSelectiveUserShareByUserIds(UserIds userIds,
                                                     SelectiveUserShareOrgDetailsDO organization)
            throws IdentityApplicationManagementException, OrganizationManagementException, UserStoreException,
            IdentityRoleManagementException, ResourceSharingPolicyMgtException, DataAccessException {

        for (String userId : userIds.getIds()) {
            processSelectiveUserShare(userId, organization);
        }
    }

    private UserSharingDetails getUserShareDetails(SelectiveUserShare selectiveUserShare,
                                                   AbstractUserStoreManager userStoreManager)
            throws OrganizationManagementException, UserStoreException {

        String sharingInitOrgId = getOrganizationId();
        String userIdOfSharingUser = selectiveUserShare.getUserId();

        Map<String, String> detailsOfMainUser = getOrganizationUserSharingService().getOriginalUserDetailsFromSharingUser(userIdOfSharingUser);
        String orgIdOfMainUser = detailsOfMainUser.get(ORG_ID);
        String userIdOfMainUser = detailsOfMainUser.get(USER_ID);
        String usernameOfMainUser = userStoreManager.getUserNameFromUserID(userIdOfSharingUser);

        return new UserSharingDetails.Builder()
                .withUserIdOfSharingUser(userIdOfSharingUser)
                .withSharingInitiatedOrgId(sharingInitOrgId)
                .withUserIdOfMainUser(userIdOfMainUser)
                .withOrganizationIdOfMainUser(orgIdOfMainUser)
                .withUsernameOfMainUser(usernameOfMainUser)
                .withSharingType(SHARING_TYPE_SHARED)
                .withRoleIds(selectiveUserShare.getRoles())
                .withPolicy(selectiveUserShare.getPolicy()).build();

    }

    private void processSelectiveUserShare(String userId, SelectiveUserShareOrgDetailsDO organization)
            throws IdentityApplicationManagementException, OrganizationManagementException,
            IdentityRoleManagementException, UserStoreException, ResourceSharingPolicyMgtException,
            DataAccessException {

        SelectiveUserShare selectiveUserShare = createSelectiveUserShare(userId, organization);
        String policyHoldingOrgId = organization.getOrganizationId();

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        AbstractUserStoreManager userStoreManager = getUserStoreManager(tenantId);

        UserSharingDetails userSharingDetails = getUserShareDetails(selectiveUserShare, userStoreManager);

        if (getPoliciesForFuturePropagation().contains(organization.getPolicy().getPolicyCode())) {
            ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder().
                    withResourceId(userId).
                    withResourceType(ResourceType.USER).
                    withInitiatingOrgId(getOrganizationId()).
                    withPolicyHoldingOrgId(policyHoldingOrgId).
                    withSharingPolicy(organization.getPolicy()).build();
            int resourceSharingPolicyRecordId =
                    getResourceSharingPolicyHandlerService().addResourceSharingPolicy(resourceSharingPolicy);

//            SharedResourceAttribute sharedResourceAttribute1 = new SharedResourceAttribute.Builder()
//                    .withResourceSharingPolicyId(resourceSharingPolicyRecordId)
//                    .withSharedAttributeType(SharedAttributeType.ROLE)
//                    .withSharedAttributes(userSharingDetails.getRoleIds()).build();

            List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
            for (String role : userSharingDetails.getRoleIds()) {
                SharedResourceAttribute sharedResourceAttribute = new SharedResourceAttribute.Builder()
                        .withResourceSharingPolicyId(resourceSharingPolicyRecordId)
                        .withSharedAttributeType(SharedAttributeType.ROLE)
                        .withSharedAttributeId(role).build();
                sharedResourceAttributes.add(sharedResourceAttribute);
            }


            saveSharedResourceAttributes(sharedResourceAttributes);

        }

        List<String> targetOrganizations =
                getOrgsToShareUserWithPerPolicy(policyHoldingOrgId, selectiveUserShare.getPolicy());

        // Thread-safe set to track processed organizations
        Set<String> processedOrgs = ConcurrentHashMap.newKeySet();

        // Create a thread pool for parallel execution
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String targetOrg : targetOrganizations) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {

                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId, true);

                    if (processedOrgs.add(targetOrg)) {
                        try {
                            LOG.info("Processing sharing for target organization: " + targetOrg);
                            UserSharingDetails detailsCopy = userSharingDetails.copy(); // Defensive copy
                            detailsCopy.setTargetOrgId(targetOrg);
                            shareUser(detailsCopy);
                            LOG.info("Completed sharing for target organization: " + targetOrg);
                        } catch (OrganizationManagementException | UserStoreException e) {
                            handleErrorWhileSharingUser(targetOrg, e);
                        }
                    }

                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }

            }, executor);

            futures.add(future);
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Shutdown the executor
        executor.shutdown();

        // Final log after all processing is complete
        LOG.info("PP - Selective share completed in PP.");
    }

    private void saveSharedResourceAttributes(List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().addSharedResourceAttributes(sharedResourceAttributes);
    }

    private void shareUser(UserSharingDetails userSharingDetails)
            throws UserStoreException, OrganizationManagementException {

        // Keep a String to save sharingUserId which equals to userSharingDetails.getSharingUserId()
        String sharingInitiatedOrgId = userSharingDetails.getSharingInitOrgId();
        String targetOrgId = userSharingDetails.getTargetOrgId();
        String userIdOfMainUser = userSharingDetails.getUserIdOfMainUser();
        String usernameOfMainUser = userSharingDetails.getUsernameOfMainUser();
        String orgIdOfMainUser = userSharingDetails.getOrgIdOfMainUser();
        String sharingType = userSharingDetails.getSharingType();
        List<String> roleIds = userSharingDetails.getRoleIds();
        //PolicyEnum policy = userSharingDetails.getPolicy();

        if (isExistingUserInTargetOrg(usernameOfMainUser, targetOrgId)) {
            errorMessages.add(
                    "User under the username: " + usernameOfMainUser +
                            " is already shared with organization: " + targetOrgId);
            return;
        }

        String sharedUserId = null;
        try {

            // Share the user with the target organization and get shared user ID for further operations
            sharedUserId = shareUserWithTargetOrg(userIdOfMainUser, orgIdOfMainUser,
                    targetOrgId, sharingInitiatedOrgId, sharingType);

            // Assign roles if any are present
            assignRolesIfPresent(sharedUserId, targetOrgId, sharingInitiatedOrgId, roleIds);

            // Handle future propagation if policy indicates it is required
            //TODO: Save the roles as well in
            //storeSharingPolicyAndDetails(USER, originalUserId, originalUserResidenceOrgId, targetOrg, policy);

        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            handleErrorWhileSharingUser(targetOrgId, e);
            rollbackSharingIfNecessary(sharedUserId, targetOrgId);
        }
    }

    private String shareUserWithTargetOrg(String originalUserId, String originalUserResidenceOrgId,
                                          String targetOrg, String sharingInitiatedOrgId, String sharingType)
            throws OrganizationManagementException, UserStoreException {

        OrganizationUserSharingService sharingService = getOrganizationUserSharingService();
        sharingService.shareOrganizationUser(targetOrg, originalUserId, originalUserResidenceOrgId,
                sharingInitiatedOrgId, sharingType);
        return sharingService.getUserAssociationOfAssociatedUserByOrgId(originalUserId, targetOrg).getUserId();
    }

    private void handleErrorWhileSharingUser(String targetOrg, Exception e) {

        errorMessages.add("Error while sharing user with organization: " + targetOrg + " - " + e.getMessage());
    }

    private void rollbackSharingIfNecessary(String sharedUserId,
                                            String targetOrg) {

        if (sharedUserId != null) {
            try {
                OrganizationUserSharingService sharingService = getOrganizationUserSharingService();
                sharingService.unshareOrganizationUsers(sharedUserId, targetOrg);
            } catch (OrganizationManagementException rollbackException) {
                errorMessages.add(
                        "Failed to rollback sharing for user: " + sharedUserId + " from organization: " + targetOrg +
                                " - " + rollbackException.getMessage());
            }
        }
    }

    private void assignRolesIfPresent(String sharedUserId, String targetOrgId, String sharingInitiatedOrgId,
                                      List<String> roleIds)
            throws IdentityRoleManagementException, OrganizationManagementException {

        if (!roleIds.isEmpty()) {
            assignRolesToTheSharedUser(sharedUserId, targetOrgId, sharingInitiatedOrgId, roleIds);
        }
    }

    private void storeSharingPolicyAndDetails(String resourceType, String originalUserId,
                                              String originalUserResidenceOrgId, String targetOrg,
                                              PolicyEnum policy)
            throws OrganizationManagementServerException {

        if (getPoliciesForFuturePropagation().contains(policy.getPolicyCode())) {
            saveForFuturePropagations(resourceType, originalUserId, originalUserResidenceOrgId, targetOrg, policy);
        }
    }

    //TODO: Make names readable and make comment
    private SelectiveUserShare createSelectiveUserShare(String userId, SelectiveUserShareOrgDetailsDO orgDetails)
            throws OrganizationManagementException, IdentityApplicationManagementException,
            IdentityRoleManagementException {

        return new SelectiveUserShare.Builder()
                .withUserId(userId)
                .withOrganizationId(orgDetails.getOrganizationId())
                .withPolicy(orgDetails.getPolicy())
                .withRoles(getRoleIdsFromRoleNameAndAudience(orgDetails.getRoles())).build();
    }

    /// ////////

    private List<String> getPoliciesForFuturePropagation() {

        List<String> policiesForFuturePropagation = new ArrayList<>();

        for (PolicyEnum policy : PolicyEnum.values()) {
            if (policy.getPolicyCode().contains(POLICY_CODE_FOR_EXISTING_AND_FUTURE) ||
                    policy.getPolicyCode().contains(POLICY_CODE_FOR_FUTURE_ONLY)) {
                policiesForFuturePropagation.add(policy.getPolicyCode());
            }
        }

        return policiesForFuturePropagation;
    }

    private void saveForFuturePropagations(String resourceType, String originalUser, String initiatedOrg,
                                           String policyHoldingOrg, PolicyEnum policy)
            throws OrganizationManagementServerException {

        resourceSharingPolicyHandlerDAO.createResourceSharingPolicyRecord(originalUser, resourceType, initiatedOrg,
                policyHoldingOrg, policy.getPolicyCode());

    }

    private void assignRolesToTheSharedUser(String sharedUser, String targetOrgId, String sharingInitiatedOrgId,
                                            List<String> roles)
            throws IdentityRoleManagementException, OrganizationManagementException {

        String sharingInitiatedOrgTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(targetOrgId);

        //TODO: Update the query

        Map<String, String> sharedRoleToMainRoleMappingsBySubOrg =
                getRoleManagementService().getSharedRoleToMainRoleMappingsBySubOrg(roles,
                        sharingInitiatedOrgTenantDomain);

        List<String> mainRoles = new ArrayList<>();
        for (String role : roles) {
            mainRoles.add(sharedRoleToMainRoleMappingsBySubOrg.getOrDefault(role, role));
        }

        Map<String, String> mainRoleToSharedRoleMappingsBySubOrg =
                getRoleManagementService().getMainRoleToSharedRoleMappingsBySubOrg(mainRoles,
                        targetOrgTenantDomain);

        //TODO: Since we are going only with POST, even for role updates, we have to get the earlier roles and delete it
        //TODO: Handle sub-org role assignments (consider only roles assigned from parents)
        for (String role : mainRoleToSharedRoleMappingsBySubOrg.values()) {
            getRoleManagementService().updateUserListOfRole(role, Collections.singletonList(sharedUser),
                    Collections.emptyList(), targetOrgTenantDomain);
        }

    }

    private boolean isExistingUserInTargetOrg(String userName, String organizationId)
            throws OrganizationManagementException, UserStoreException {
        //Need to decide how the usher share is handled in the duplicate user issue.
        //TODO: Secondary user stores

        String tenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        AbstractUserStoreManager userStoreManager = getUserStoreManager(tenantId);

        return userStoreManager.isExistingUser(userName);
    }

    private List<String> getRoleIdsFromRoleNameAndAudience(List<RoleWithAudienceDO> rolesWithAudience)
            throws OrganizationManagementException, IdentityApplicationManagementException,
            IdentityRoleManagementException {

        String sharingInitiatedOrgId = getOrganizationId();
        String sharingInitiatedTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);

        List<String> list = new ArrayList<>();
        for (RoleWithAudienceDO roleWithAudienceDO : rolesWithAudience) {
            String audienceId = getAudienceId(roleWithAudienceDO, sharingInitiatedOrgId, sharingInitiatedTenantDomain);
            String roleId = getRoleIdFromAudience(
                    roleWithAudienceDO.getRoleName(),
                    roleWithAudienceDO.getAudienceType(),
                    audienceId,
                    sharingInitiatedTenantDomain);
            list.add(roleId);
        }
        return list;

    }

    private String getRoleIdFromAudience(String roleName, String audienceType, String audienceId, String tenantDomain)
            throws IdentityRoleManagementException {

        return getRoleManagementService().getRoleIdByName(roleName, audienceType, audienceId, tenantDomain);
    }

    private String getAudienceId(RoleWithAudienceDO role, String originalOrgId, String tenantDomain)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        switch (role.getAudienceType()) {
            case ORGANIZATION:
                return originalOrgId;
            case APPLICATION:
                return getApplicationManagementService()
                        .getApplicationBasicInfoByName(role.getAudienceName(), tenantDomain)
                        .getApplicationResourceId();
            default:
                throw new OrganizationManagementException("Invalid audience type: " + role.getAudienceType());
        }
    }

    private List<String> getOrgsToShareUserWithPerPolicy(String policyHoldingOrgId, PolicyEnum policy)
            throws OrganizationManagementException {

        Set<String> organizationsToShareWithPerPolicy = new HashSet<>();

        switch (policy) {
            case ALL_EXISTING_ORGS_ONLY:
            case ALL_EXISTING_AND_FUTURE_ORGS:
                organizationsToShareWithPerPolicy.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case IMMEDIATE_EXISTING_ORGS_ONLY:
            case IMMEDIATE_EXISTING_AND_FUTURE_ORGS:
                organizationsToShareWithPerPolicy.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case SELECTED_ORG_ONLY:
                organizationsToShareWithPerPolicy.add(policyHoldingOrgId);
                break;

            case SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN:
                organizationsToShareWithPerPolicy.add(policyHoldingOrgId);
                organizationsToShareWithPerPolicy.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN:
                organizationsToShareWithPerPolicy.add(policyHoldingOrgId);
                organizationsToShareWithPerPolicy.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case NO_SHARING:
                break;

            default:
                throw new OrganizationManagementException("Invalid policy provided: " + policy.getPolicyName());
        }

        return new ArrayList<>(organizationsToShareWithPerPolicy);
    }

    //Get Services

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    //Validation methods

    private <T extends UserCriteriaType> void validateInput(BaseUserShareDO<T> userShareDO)
            throws UserShareMgtServerException {

        if (userShareDO == null) {
            throwValidationException(NULL_INPUT_MESSAGE,
                    UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT.getDescription());
        }

        if (userShareDO instanceof SelectiveUserShareDO) {
            validateSelectiveDO((SelectiveUserShareDO) userShareDO);
        } else if (userShareDO instanceof GeneralUserShareDO) {
            validateGeneralDO((GeneralUserShareDO) userShareDO);
        }
    }

    private void validateSelectiveDO(SelectiveUserShareDO selectiveDO) throws UserShareMgtServerException {

        // Validate userCriteria is not null
        validateNotNull(selectiveDO.getUserCriteria(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID.getCode());

        // Validate that userCriteria contains the required USER_IDS key and is not null
        if (!selectiveDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getDescription());
        }

        // Validate organizations list is not null
        validateNotNull(selectiveDO.getOrganizations(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL.getCode());

        // Validate each organization in the list
        for (SelectiveUserShareOrgDetailsDO orgDetails : selectiveDO.getOrganizations()) {
            validateNotNull(orgDetails.getOrganizationId(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL.getCode());

            validateNotNull(orgDetails.getPolicy(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getCode());

            // Validate roles list is not null (it can be empty)
            if (orgDetails.getRoles() == null) {
                throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getMessage(),
                        UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getCode(),
                        UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getDescription());
            } else {
                // Validate each role's properties if present
                for (RoleWithAudienceDO role : orgDetails.getRoles()) {
                    validateNotNull(role.getRoleName(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getMessage(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getCode());

                    validateNotNull(role.getAudienceName(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getMessage(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getCode());

                    validateNotNull(role.getAudienceType(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getMessage(),
                            UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getCode());
                }
            }
        }
    }

    private void validateGeneralDO(GeneralUserShareDO generalDO) throws UserShareMgtServerException {

        validateNotNull(generalDO.getUserCriteria(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID.getCode());
        if (!generalDO.getUserCriteria().containsKey(USER_IDS) || generalDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getCode(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING.getDescription());
        }

        validateNotNull(generalDO.getPolicy(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL.getCode());

        validateNotNull(generalDO.getRoles(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getMessage(),
                UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL.getCode());

        // Validate each role's properties if present
        for (RoleWithAudienceDO role : generalDO.getRoles()) {
            validateNotNull(role.getRoleName(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL.getCode());

            validateNotNull(role.getAudienceName(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL.getCode());

            validateNotNull(role.getAudienceType(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getMessage(),
                    UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL.getCode());
        }
    }

    private void validateNotNull(Object obj, String errorMessage, String errorCode)
            throws UserShareMgtServerException {

        if (obj == null) {
            throwValidationException(errorMessage, errorCode, errorMessage);
        }
    }

    private void throwValidationException(String message, String errorCode, String description)
            throws UserShareMgtServerException {

        throw new UserShareMgtServerException(message, new NullPointerException(message), errorCode, description);
    }

    //Business Logics

    @Override
    public void populateGeneralUserShare(GeneralUserShareDO generalUserShareDO) {

    }

    @Override
    public void populateSelectiveUserUnshare(SelectiveUserUnshareDO selectiveUserUnshareDO) {

    }

    @Override
    public void populateGeneralUserUnshare(GeneralUserUnshareDO generalUserUnshareDO) {

    }

}
*/
