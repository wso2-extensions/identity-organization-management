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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.listener;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
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
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.model.UniqueIDUserClaimSearchEntry;
import org.wso2.carbon.user.core.model.UserClaimSearchEntry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;

/**
 * User operation event listener for shared user management.
 */
public class SharedUserOperationEventListener extends AbstractIdentityUserOperationEventListener {

    private final OrganizationUserSharingService organizationUserSharingService =
            new OrganizationUserSharingServiceImpl();

    private final List<String> sharedUserSpecificClaims =
            Arrays.asList("http://wso2.org/claims/groups", "http://wso2.org/claims/roles");

    @Override
    public int getExecutionOrderId() {

        return 8;
    }

    public boolean doPostGetUserClaimValueWithID(String userID, String claim, List<String> claimValue,
                                                 String profileName, UserStoreManager userStoreManager)
            throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        // If claimValue is not empty, it means the claim value is already set. Hence, return.
        if (!claimValue.isEmpty()) {
            return true;
        }
        // Get the claim trying to retrieve if shared user specific one, no need to check it from root user.
        if (sharedUserSpecificClaims.contains(claim)) {
            return true;
        }

        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String currentOrganizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        try {
            if (!OrganizationManagementUtil.isOrganization(currentTenantDomain)) {
                // There is no shared users in root organizations. Hence, return.
                return true;
            }
            UserAssociation userAssociation =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                            .getUserAssociation(userID, currentOrganizationId);
            if (userAssociation == null) {
                // User is not a shared user. Hence, return.
                return true;
            }
            // Get the associated user id and user's managed organization.
            String associatedUserId = userAssociation.getAssociatedUserId();
            String userResidentOrganizationId = userAssociation.getUserResidentOrganizationId();
            String userResidentTenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                    .resolveTenantDomain(userResidentOrganizationId);

            // Get the resident user claim value.
            AbstractUserStoreManager residentUserStoreManager =
                    getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
            String residentUserClaimValueWithID =
                    residentUserStoreManager.getUserClaimValueWithID(associatedUserId, claim,
                            profileName);
            if (residentUserClaimValueWithID != null) {
                claimValue.add(residentUserClaimValueWithID);
            }
        } catch (OrganizationManagementException | org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    public boolean doPostGetUsersClaimValuesWithID(List<String> userIDs, List<String> claims, String profileName,
                                                   List<UniqueIDUserClaimSearchEntry> userClaimSearchEntries,
                                                   UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String currentOrganizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        try {
            if (!OrganizationManagementUtil.isOrganization(currentTenantDomain)) {
                // There is no shared users in root organizations. Hence, return.
                return true;
            }
            String[] claimsArray = claims.toArray(new String[0]);
            for (UniqueIDUserClaimSearchEntry userClaimSearchEntry : userClaimSearchEntries) {
                User user = userClaimSearchEntry.getUser();
                UserAssociation userAssociation =
                        OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                                .getUserAssociation(user.getUserID(), currentOrganizationId);
                if (userAssociation == null) {
                    // User is not a shared user. Hence, return.
                    continue;
                }
                // Get the associated user id and user's managed organization.
                String associatedUserId = userAssociation.getAssociatedUserId();
                String userResidentOrganizationId = userAssociation.getUserResidentOrganizationId();
                String userResidentTenantDomain =
                        OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                                .resolveTenantDomain(userResidentOrganizationId);

                // Get the resident user claim values.
                AbstractUserStoreManager residentUserStoreManager =
                        getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
                Map<String, String> residentUserClaimValuesWithID =
                        residentUserStoreManager.getUserClaimValuesWithID(associatedUserId, claimsArray, profileName);
                Map<String, String> aggregatedProfileClaims =
                        getAggregatedProfile(userClaimSearchEntry.getClaims(), residentUserClaimValuesWithID);
                userClaimSearchEntry.setClaims(aggregatedProfileClaims);
                UserClaimSearchEntry searchEntryObject = userClaimSearchEntry.getUserClaimSearchEntry();
                searchEntryObject.setClaims(aggregatedProfileClaims);
            }
        } catch (OrganizationManagementException | org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    public boolean doPostGetUserClaimValuesWithID(String userID, String[] claims, String profileName,
                                                  Map<String, String> claimMap, UserStoreManager userStoreManager)
            throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
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
            // Get the associated user id and user's managed organization.
            String associatedUserId = userAssociation.getAssociatedUserId();
            String userResidentOrganizationId = userAssociation.getUserResidentOrganizationId();
            String userResidentTenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                    .resolveTenantDomain(userResidentOrganizationId);

            // Get the resident user claim values.
            AbstractUserStoreManager residentUserStoreManager =
                    getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
            Map<String, String> residentUserClaimValuesWithID =
                    residentUserStoreManager.getUserClaimValuesWithID(associatedUserId, claims, profileName);
            claimMap.putAll(getAggregatedProfile(claimMap, residentUserClaimValuesWithID));
        } catch (OrganizationManagementException | org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    private Map<String, String> getAggregatedProfile(Map<String, String> sharedUserClaims,
                                                     Map<String, String> residentUserClaims) {

        if (sharedUserClaims == null) {
            return residentUserClaims;
        } else if (residentUserClaims == null) {
            return sharedUserClaims;
        }
        // Start with all entries from residentUserClaims.
        Map<String, String> aggregatedResult = new HashMap<>(residentUserClaims);
        // Remove sharedUserSpecificClaims from aggregatedResult.
        sharedUserSpecificClaims.forEach(aggregatedResult::remove);
        // Add entries from sharedUserClaims, overriding duplicates
        aggregatedResult.putAll(sharedUserClaims);
        return aggregatedResult;
    }

    @Override
    public boolean doPreDeleteUserWithID(String userID, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        try {
            // The organization where the user identity is managed. Clear all the associations of the user.
            String associatedOrgId = OrganizationSharedUserUtil
                    .getUserManagedOrganizationClaim((AbstractUserStoreManager) userStoreManager, userID);
            if (associatedOrgId != null) {
                // User is associated only for shared users. Hence, delete the user association.
                return organizationUserSharingService.deleteUserAssociation(userID, associatedOrgId);
            }

            String orgId = getOrganizationId();
            if (orgId == null) {
                orgId = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                        .resolveOrganizationId(getTenantDomain());
            }
            // Delete all the user associations of the user.
            return organizationUserSharingService.unshareOrganizationUsers(userID, orgId);
        } catch (OrganizationManagementException e) {
            throw new UserStoreException(e.getMessage(), e.getErrorCode(), e);
        }
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
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValueWithID(String userID, String claimURI, String claimValue, String profileName,
                                                UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        if (CLAIM_MANAGED_ORGANIZATION.equals(claimURI)) {
            throw new UserStoreClientException(
                    String.format(ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getDescription(),
                            CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        return true;
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId)
            throws org.wso2.carbon.user.api.UserStoreException {

        RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }
}
