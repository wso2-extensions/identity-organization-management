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
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.core.util.LambdaExceptionUtils;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception.OrgResourceHierarchyTraverseException;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.strategy.FirstFoundAggregationStrategy;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.model.UniqueIDUserClaimSearchEntry;
import org.wso2.carbon.user.core.model.UserClaimSearchEntry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SHARED_PROFILE_VALUE_RESOLVING_METHOD;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;

/**
 * User operation event listener for shared user management.
 */
public class SharedUserOperationEventListener extends AbstractIdentityUserOperationEventListener {

    private static final Log LOG = LogFactory.getLog(SharedUserOperationEventListener.class);
    private final OrganizationUserSharingService organizationUserSharingService =
            new OrganizationUserSharingServiceImpl();

    @Override
    public int getExecutionOrderId() {

        return 128;
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

            // Analyse SharedProfileValueResolvingMethod value of claim and decide.
            ClaimMetadataManagementService claimManagementService =
                    OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
            List<String> resolveFromOriginClaims = new ArrayList<>();
            List<String> resolveFromSharedProfileClaims = new ArrayList<>();
            List<String> resolveFromHierarchyClaims = new ArrayList<>();
            /*
             For each claim in claims get the value of "SharedProfileValueResolvingMethod" property and add to
             above lists.
             */
            for (String claim : claims) {
                Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claim, currentTenantDomain);
                if (!localClaim.isPresent()) {
                    continue;
                }
                String sharedProfileValueResolvingMethod =
                        localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
                if (StringUtils.isNotBlank(sharedProfileValueResolvingMethod)) {
                    ClaimConstants.SharedProfileValueResolvingMethod sharedProfileValueResolvingMethodEnum =
                            ClaimConstants.SharedProfileValueResolvingMethod.fromName(
                                    sharedProfileValueResolvingMethod);
                    switch (sharedProfileValueResolvingMethodEnum) {
                        case FROM_ORIGIN:
                            resolveFromOriginClaims.add(claim);
                            break;
                        case FROM_SHARED_PROFILE:
                            resolveFromSharedProfileClaims.add(claim);
                            break;
                        case FROM_FIRST_FOUND_IN_HIERARCHY:
                            resolveFromHierarchyClaims.add(claim);
                            break;
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("SharedProfileValueResolvingMethod is not resolved for the claim: " + claim);
                    }
                    resolveFromOriginClaims.add(claim);
                }
            }

            // Resolve Claims from Origin.
            AbstractUserStoreManager residentUserStoreManager =
                    getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
            Map<String, String> resolvedClaimsFromOrigin =
                    residentUserStoreManager.getUserClaimValuesWithID(associatedUserId,
                            resolveFromOriginClaims.toArray(new String[0]), profileName);

            // Resolve Claims from Shared Profile.
            Map<String, String> resolvedClaimsFromSharedProfile = new HashMap<>();
            for (String claim : resolveFromSharedProfileClaims) {
                resolvedClaimsFromSharedProfile.put(claim, claimMap.get(claim));
            }

            // Resolve Claims from Hierarchy.
            Map<String, String> resolvedClaimsFromHierarchy = new HashMap<>();
            OrgResourceResolverService orgResourceResolverService =
                    OrganizationUserSharingDataHolder.getInstance().getOrgResourceResolverService();
            for (String claim : resolveFromHierarchyClaims) {
                String resolvedClaimValueFromOrgHierarchy =
                        orgResourceResolverService.getResourcesFromOrgHierarchy(currentOrganizationId,
                                LambdaExceptionUtils.rethrowFunction(
                                        orgId -> claimResolver(associatedUserId, claim, orgId)),
                                new FirstFoundAggregationStrategy<>());
                resolvedClaimsFromHierarchy.put(claim, resolvedClaimValueFromOrgHierarchy);
            }

            // Set resolvedClaimsFromOrigin,resolvedClaimsFromSharedProfile and resolvedClaimsFromHierarchy to claimMap.
            claimMap.putAll(resolvedClaimsFromOrigin);
            claimMap.putAll(resolvedClaimsFromHierarchy);
            claimMap.putAll(resolvedClaimsFromSharedProfile);
        } catch (OrganizationManagementException | org.wso2.carbon.user.api.UserStoreException |
                 ClaimMetadataException | OrgResourceHierarchyTraverseException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    @Override
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
            if (StringUtils.isBlank(currentOrganizationId)) {
                currentOrganizationId = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                        .resolveOrganizationId(currentTenantDomain);
            }
            // Analyse SharedProfileValueResolvingMethod value of claim and decide.
            ClaimMetadataManagementService claimManagementService =
                    OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
            List<String> resolveFromOriginClaims = new ArrayList<>();
            List<String> resolveFromSharedProfileClaims = new ArrayList<>();
            List<String> resolveFromHierarchyClaims = new ArrayList<>();
            /*
            For each claim in claims get the value of "SharedProfileValueResolvingMethod" property and add to
             above lists.
             */
            for (String claim : claims) {
                Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claim, currentTenantDomain);
                if (!localClaim.isPresent()) {
                    continue;
                }
                String sharedProfileValueResolvingMethod =
                        localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
                if (StringUtils.isBlank(sharedProfileValueResolvingMethod)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("SharedProfileValueResolvingMethod is not resolved for the claim: " + claim);
                    }
                    resolveFromOriginClaims.add(claim);
                }
                ClaimConstants.SharedProfileValueResolvingMethod sharedProfileValueResolvingMethodEnum =
                        ClaimConstants.SharedProfileValueResolvingMethod.fromName(
                                sharedProfileValueResolvingMethod);
                switch (sharedProfileValueResolvingMethodEnum) {
                    case FROM_ORIGIN:
                        resolveFromOriginClaims.add(claim);
                        break;
                    case FROM_SHARED_PROFILE:
                        resolveFromSharedProfileClaims.add(claim);
                        break;
                    case FROM_FIRST_FOUND_IN_HIERARCHY:
                        resolveFromHierarchyClaims.add(claim);
                        break;
                }
            }

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

                // Resolve Claims from Origin.
                AbstractUserStoreManager residentUserStoreManager =
                        getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
                Map<String, String> resolvedClaimsFromOrigin =
                        residentUserStoreManager.getUserClaimValuesWithID(associatedUserId,
                                resolveFromOriginClaims.toArray(new String[0]), profileName);

                // Resolve Claims from Shared Profile.
                Map<String, String> resolvedClaimsFromSharedProfile = new HashMap<>();
                for (String claim : resolveFromSharedProfileClaims) {
                    resolvedClaimsFromSharedProfile.put(claim, userClaimSearchEntry.getClaims().get(claim));
                }

                // Resolve Claims from Hierarchy.
                Map<String, String> resolvedClaimsFromHierarchy = new HashMap<>();
                OrgResourceResolverService orgResourceResolverService =
                        OrganizationUserSharingDataHolder.getInstance().getOrgResourceResolverService();
                for (String claim : resolveFromHierarchyClaims) {
                    String resolvedClaimValueFromOrgHierarchy =
                            orgResourceResolverService.getResourcesFromOrgHierarchy(currentOrganizationId,
                                    LambdaExceptionUtils.rethrowFunction(
                                            orgId -> claimResolver(associatedUserId, claim, orgId)),
                                    new FirstFoundAggregationStrategy<>());
                    resolvedClaimsFromHierarchy.put(claim, resolvedClaimValueFromOrgHierarchy);
                }

                Map<String, String> aggregatedProfileClaims = new HashMap<>();
                aggregatedProfileClaims.putAll(resolvedClaimsFromOrigin);
                aggregatedProfileClaims.putAll(resolvedClaimsFromHierarchy);
                aggregatedProfileClaims.putAll(resolvedClaimsFromSharedProfile);

                userClaimSearchEntry.setClaims(aggregatedProfileClaims);
                UserClaimSearchEntry searchEntryObject = userClaimSearchEntry.getUserClaimSearchEntry();
                searchEntryObject.setClaims(aggregatedProfileClaims);
            }
        } catch (OrganizationManagementException | org.wso2.carbon.user.api.UserStoreException |
                 ClaimMetadataException | OrgResourceHierarchyTraverseException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    @Override
    public boolean doPostGetUserClaimValueWithID(String userID, String claim, List<String> claimValue,
                                                 String profileName, UserStoreManager userStoreManager)
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
            ClaimMetadataManagementService claimManagementService =
                    OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
            Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claim, currentTenantDomain);
            if (!localClaim.isPresent()) {
                LOG.debug("Claim not found in the tenant domain: " + currentTenantDomain);
            }
            String sharedProfileValueResolvingMethod =
                    localClaim.get().getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD);
            // If sharedProfileValueResolvingMethod is not defined in the claim, treat as FromOrigin.
            if (StringUtils.isBlank(sharedProfileValueResolvingMethod)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("SharedProfileValueResolvingMethod is not resolved for the claim: " + claim);
                }
                sharedProfileValueResolvingMethod = FROM_ORIGIN.name();
            }
            ClaimConstants.SharedProfileValueResolvingMethod sharedProfileValueResolvingMethodEnum =
                    ClaimConstants.SharedProfileValueResolvingMethod.fromName(
                            sharedProfileValueResolvingMethod);
            switch (sharedProfileValueResolvingMethodEnum) {
                case FROM_ORIGIN:
                    // Get the associated user id and user's managed organization.
                    String associatedUserId = userAssociation.getAssociatedUserId();
                    String userResidentOrganizationId = userAssociation.getUserResidentOrganizationId();
                    String userResidentTenantDomain =
                            OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                                    .resolveTenantDomain(userResidentOrganizationId);
                    AbstractUserStoreManager residentUserStoreManager =
                            getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
                    String userClaimValueWithID =
                            residentUserStoreManager.getUserClaimValueWithID(associatedUserId, claim, profileName);
                    // Reset claimValue with userClaimValueWithID.
                    claimValue.clear();
                    claimValue.add(userClaimValueWithID);
                    break;
                case FROM_SHARED_PROFILE:
                    // Do nothing as the claim value is already set.
                    break;
                case FROM_FIRST_FOUND_IN_HIERARCHY:
                    /*
                    If the flow is invoked by a claim resolver function, don't execute the following to avoid recursion.
                     */
                    boolean inClaimResolverFunction =
                            IdentityUtil.threadLocalProperties.get().containsKey("InClaimResolverFunction");
                    if (inClaimResolverFunction) {
                        break;
                    }
                    try {
                        IdentityUtil.threadLocalProperties.get().put("InClaimResolverFunction", "true");
                        OrgResourceResolverService orgResourceResolverService =
                                OrganizationUserSharingDataHolder.getInstance().getOrgResourceResolverService();
                        String resolvedClaimValueFromOrgHierarchy =
                                orgResourceResolverService.getResourcesFromOrgHierarchy(currentOrganizationId,
                                        LambdaExceptionUtils.rethrowFunction(
                                                orgId -> claimResolver(userAssociation.getAssociatedUserId(), claim,
                                                        orgId)),
                                        new FirstFoundAggregationStrategy<>());
                        claimValue.clear();
                        claimValue.add(resolvedClaimValueFromOrgHierarchy);
                    } finally {
                        IdentityUtil.threadLocalProperties.get().remove("InClaimResolverFunction");
                    }
                    break;
            }
        } catch (OrganizationManagementException | ClaimMetadataException |
                 org.wso2.carbon.user.api.UserStoreException | OrgResourceHierarchyTraverseException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    private Optional<String> claimResolver(String associatedUserId, String claimURI, String organizationId)
            throws org.wso2.carbon.user.api.UserStoreException {

        try {
            // Get the shared user id in given org.
            UserAssociation userAssociationOfAssociatedUserByOrgId =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                            .getUserAssociationOfAssociatedUserByOrgId(associatedUserId, organizationId);
            OrganizationManager organizationManager =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
            String tenantDomainOfOrg = organizationManager.resolveTenantDomain(organizationId);
            AbstractUserStoreManager userStoreManager =
                    getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(tenantDomainOfOrg));
            if (userAssociationOfAssociatedUserByOrgId == null) {
                // It should be a resident user. Hence, return the claim value from the resident user store.
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomainOfOrg, true);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(organizationId);
                    String userClaimValue =
                            userStoreManager.getUserClaimValueWithID(associatedUserId, claimURI, null);
                    return Optional.ofNullable(userClaimValue);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
            String sharedUserIdInSearchOrg = userAssociationOfAssociatedUserByOrgId.getUserId();
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomainOfOrg, true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(organizationId);
                String userClaimValue =
                        userStoreManager.getUserClaimValueWithID(sharedUserIdInSearchOrg, claimURI, null);
                return Optional.ofNullable(userClaimValue);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (OrganizationManagementException e) {
            throw new UserStoreException(e.getErrorCode(), e.getMessage());
        }
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId)
            throws org.wso2.carbon.user.api.UserStoreException {

        RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }
}
