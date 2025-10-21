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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserStoreClientException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SHARED_PROFILE_VALUE_RESOLVING_METHOD;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_LAST_PASSWORD_UPDATE_TIME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.PROCESS_ADD_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;

/**
 * Shared user profile update governing event listener.
 */
public class SharedUserProfileUpdateGovernanceEventListener extends AbstractIdentityUserOperationEventListener {

    private static final Log LOG = LogFactory.getLog(SharedUserProfileUpdateGovernanceEventListener.class);
    private final Set<String> resolvingMethodNeutralClaimsForSharedUser =
            new HashSet<>(Collections.singletonList(CLAIM_LAST_PASSWORD_UPDATE_TIME));

    @Override
    public int getExecutionOrderId() {

        int orderId = getOrderId();
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        // The order of this listener should be higher than the IdentityStoreEventListener(100).
        return 8;
    }

    @Override
    public boolean doPreSetUserClaimValuesWithID(String userID, Map<String, String> claims, String profileName,
                                                 UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        if (!claims.isEmpty() && claims.containsKey(CLAIM_MANAGED_ORGANIZATION)) {
            throw new UserStoreClientException(
                    String.format(ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getDescription(),
                            CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        /*
        If the update is for a shared profile, check whether the claim's SharedProfileValueResolvingMethod is
        set to "FromOrigin" or blank. If yes, it is not allowed to update the claim value.
         */
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        if (!isSharedUserProfile(userID, currentTenantDomain)) {
            return true;
        }
        ClaimMetadataManagementService claimManagementService =
                OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
        for (Map.Entry<String, String> claim : claims.entrySet()) {
            String claimURI = claim.getKey();
            try {
                if (isResolvingMethodNeutralClaimForSharedUser(claimURI)) {
                    continue;
                }
                Optional<LocalClaim> localClaim =
                        claimManagementService.getLocalClaim(claimURI, currentTenantDomain);
                if (!localClaim.isPresent()) {
                    LOG.debug(String.format("Claim: %s is not available in the tenant: %s", claimURI,
                            currentTenantDomain));
                    continue;
                }
                String sharedProfileValueResolvingMethod =
                        localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
                if (StringUtils.isBlank(sharedProfileValueResolvingMethod) ||
                        StringUtils.equals(ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN.getName(),
                                sharedProfileValueResolvingMethod)) {
                    throw new UserStoreClientException(
                            String.format("Claim: %s is not allowed to be updated for shared users.", claimURI));
                }
            } catch (ClaimMetadataException e) {
                throw new UserStoreClientException(
                        String.format(
                                "Error while checking the SharedProfileValueResolvingMethod value of the claim: %s",
                                claimURI), e);
            }
        }
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValueWithID(String userID, String claimURI, String claimValue, String profileName,
                                                UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        // The managedOrg identity claim can't be edited by the user.
        if (CLAIM_MANAGED_ORGANIZATION.equals(claimURI)) {
            throw new UserStoreClientException(
                    String.format(ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getDescription(),
                            CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }

        /*
        If the update is for a shared profile, check whether the claim's SharedProfileValueResolvingMethod
        is set to "FromOrigin" or blank. If yes, it is not allowed to update the claim value.
         */
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        if (!isSharedUserProfile(userID, currentTenantDomain)) {
            return true;
        }
        try {
            if (isResolvingMethodNeutralClaimForSharedUser(claimURI)) {
                return true;
            }
            ClaimMetadataManagementService claimManagementService =
                    OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
            Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claimURI, currentTenantDomain);
            if (!localClaim.isPresent()) {
                LOG.debug(String.format("Claim: %s is not available in the tenant: %s", claimURI, currentTenantDomain));
                return true;
            }
            String sharedProfileValueResolvingMethod =
                    localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
            if (StringUtils.isBlank(sharedProfileValueResolvingMethod) ||
                    StringUtils.equals(ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN.getName(),
                            sharedProfileValueResolvingMethod)) {
                throw new UserStoreClientException(
                        String.format("Claim: %s is not allowed to be updated for shared users.", claimURI));
            }
        } catch (ClaimMetadataException e) {
            throw new UserStoreClientException(
                    String.format("Error while checking the SharedProfileValueResolvingMethod value of the claim: %s",
                            claimURI), e);
        }
        return true;
    }

    private static boolean isSharedUserProfile(String userID, String currentTenantDomain) throws UserStoreException {

        return hasUserAssociation(userID, currentTenantDomain) ||
                hasManagedOrgClaim(userID, currentTenantDomain) || isSharedUserAddProcess();
    }

    private static boolean hasUserAssociation(String userID, String currentTenantDomain)
            throws UserStoreException {

        String currentOrganizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        try {
            // There is no shared users in root organizations. Hence, return false.
            if (isRootOrg(currentTenantDomain)) {
                return false;
            }
            if (StringUtils.isBlank(currentOrganizationId)) {
                currentOrganizationId = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                        .resolveOrganizationId(currentTenantDomain);
            }
            UserAssociation userAssociation =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                            .getUserAssociation(userID, currentOrganizationId);
            if (userAssociation == null) {
                // User is not a shared user. Hence, return false.
                return false;
            }
        } catch (OrganizationManagementException e) {
            throw new UserStoreClientException(
                    "Error while checking the user association of the user: " + userID + " with the organization: " +
                            currentOrganizationId, e);
        }
        return true;
    }

    private static boolean hasManagedOrgClaim(String userID, String currentTenantDomain) throws UserStoreException {

        // Root organization users cannot have managedOrg claim.
        if (isRootOrg(currentTenantDomain)) {
            return false;
        }
        int currentTenantId = IdentityTenantUtil.getTenantId(currentTenantDomain);
        AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(currentTenantId);
        return StringUtils.isNotEmpty(
                OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager, userID));
    }

    private static boolean isRootOrg(String currentTenantDomain) throws UserStoreException {

        try {
            return !OrganizationManagementUtil.isOrganization(currentTenantDomain);
        } catch (OrganizationManagementException e) {
            throw new UserStoreException(
                    "Error occurred while checking if the organization is a root organization.", e);
        }
    }

    /**
     * Checks if the current flow is a shared user addition process.
     * During the shared user addition flow, the thread has the {@code PROCESS_ADD_SHARED_USER} property set to
     * {@code true}.
     * This method verifies that the property is present and evaluates to {@code true} to determine whether the flow
     * is a shared user addition process.
     *
     * @return {@code true} if the current flow is a shared user addition process, otherwise {@code false}.
     */
    private static boolean isSharedUserAddProcess() {

        return Boolean.TRUE.equals(IdentityUtil.threadLocalProperties.get().get(PROCESS_ADD_SHARED_USER));
    }

    private boolean isResolvingMethodNeutralClaimForSharedUser(String claimURI) {

        return resolvingMethodNeutralClaimsForSharedUser.contains(claimURI);
    }

    private static AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        try {
            RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
            if (realmService == null) {
                throw new UserStoreException("RealmService is not initialized.");
            }
            UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
            if (tenantUserRealm == null) {
                throw new UserStoreException("UserRealm is null for tenant: " + tenantId);
            }
            org.wso2.carbon.user.api.UserStoreManager userStoreManager = tenantUserRealm.getUserStoreManager();
            if (!(userStoreManager instanceof AbstractUserStoreManager)) {
                throw new UserStoreException("UserStoreManager is not an instance of AbstractUserStoreManager.");
            }
            return (AbstractUserStoreManager) userStoreManager;
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException("Error while retrieving UserStoreManager for tenant: " + tenantId, e);
        }
    }
}
