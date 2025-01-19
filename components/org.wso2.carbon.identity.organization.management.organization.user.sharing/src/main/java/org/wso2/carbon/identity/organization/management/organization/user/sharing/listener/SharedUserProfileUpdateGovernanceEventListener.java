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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.core.UserStoreClientException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SHARED_PROFILE_VALUE_RESOLVING_METHOD;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;

/**
 * Shared user profile update governing event listener.
 */
public class SharedUserProfileUpdateGovernanceEventListener extends AbstractIdentityUserOperationEventListener {

    private static final Log LOG = LogFactory.getLog(SharedUserProfileUpdateGovernanceEventListener.class);

    @Override
    public int getExecutionOrderId() {

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
        set to "FromOrigin". If yes stop updating.
         */
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        if (!isSharedUserProfile(userID, currentTenantDomain)) {
            return true;
        }
        ClaimMetadataManagementService claimManagementService =
                OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
        for (Map.Entry<String, String> claim : claims.entrySet()) {
            try {
                Optional<LocalClaim> localClaim =
                        claimManagementService.getLocalClaim(claim.getKey(), currentTenantDomain);
                if (!localClaim.isPresent()) {
                    LOG.debug(String.format("Claim: %s is not available in the tenant: %s", claim.getKey(),
                            currentTenantDomain));
                    continue;
                }
                String sharedProfileValueResolvingMethod =
                        localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
                if (StringUtils.equals(ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN.getName(),
                        sharedProfileValueResolvingMethod)) {
                    throw new UserStoreClientException(
                            String.format("Claim: %s is not allowed to be updated for shared users.", claim.getKey()));
                }
            } catch (ClaimMetadataException e) {
                throw new UserStoreClientException(
                        String.format(
                                "Error while checking the SharedProfileValueResolvingMethod value of the claim: %s",
                                claim.getKey()), e);
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
        is set to "FromOrigin". If yes stop updating.
         */
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        if (!isSharedUserProfile(userID, currentTenantDomain)) {
            return true;
        }
        try {
            ClaimMetadataManagementService claimManagementService =
                    OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
            Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claimURI, currentTenantDomain);
            if (!localClaim.isPresent()) {
                LOG.debug(String.format("Claim: %s is not available in the tenant: %s", claimURI, currentTenantDomain));
                return true;
            }
            String sharedProfileValueResolvingMethod =
                    localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
            if (StringUtils.equals(ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN.getName(),
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


    private static boolean isSharedUserProfile(String userID, String currentTenantDomain)
            throws UserStoreClientException {

        String currentOrganizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        try {
            if (!OrganizationManagementUtil.isOrganization(currentTenantDomain)) {
                // There is no shared users in root organizations. Hence, return.
                return true;
            }
            if (StringUtils.isBlank(currentOrganizationId)) {
                currentOrganizationId = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                        .resolveOrganizationId(currentTenantDomain);
            }
            UserAssociation userAssociation =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                            .getUserAssociation(userID, currentOrganizationId);
            if (userAssociation == null) {
                // User is not a shared user. Hence, return.
                return true;
            }
        } catch (OrganizationManagementException e) {
            throw new UserStoreClientException(
                    "Error while checking the user association of the user: " + userID + " with the organization: " +
                            currentOrganizationId, e);
        }
        return false;
    }
}
