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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
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
import org.wso2.carbon.user.core.UserStoreClientException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.model.UniqueIDUserClaimSearchEntry;
import org.wso2.carbon.user.core.model.UserClaimSearchEntry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SHARED_PROFILE_VALUE_RESOLVING_METHOD;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SharedProfileValueResolvingMethod.FROM_FIRST_FOUND_IN_HIERARCHY;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SharedProfileValueResolvingMethod.FROM_SHARED_PROFILE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_SHARED_USER_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * User operation event listener for shared user management.
 */
public class SharedUserOperationEventListener extends AbstractIdentityUserOperationEventListener {

    private static final Log LOG = LogFactory.getLog(SharedUserOperationEventListener.class);
    private static final String INSIDE_CLAIM_RESOLVER_FLAG = "insideClaimResolverFunction";
    private final OrganizationUserSharingService organizationUserSharingService =
            new OrganizationUserSharingServiceImpl();
    private final SharedUserProfileUpdateGovernanceEventListener sharedUserProfileUpdateGovernanceEventListener =
            new SharedUserProfileUpdateGovernanceEventListener();

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
            String orgId = getOrganizationId();

            if (associatedOrgId != null) {
                // Retrieve the user association details for the given user and organization.
                UserAssociation userAssociation = getUserAssociation(userID, orgId);
                String sharedType = userAssociation.getSharedType();

                // Restrict deletion if the shared type is not "INVITED".
                if (SharedType.valueOf(sharedType) != SharedType.INVITED) {
                    throw new UserStoreClientException(
                            ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_USER.getDescription(),
                            ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_USER.getCode());
                }

                // Delete the user's association with the organization.
                return organizationUserSharingService.deleteUserAssociation(userID, associatedOrgId);
            }

            // Delete all the user associations of the user.
            return organizationUserSharingService.unshareOrganizationUsers(userID, orgId);
        } catch (OrganizationManagementException e) {
            throw new UserStoreException(e.getMessage(), e.getErrorCode(), e);
        }
    }

    @Override
    public boolean doPostGetUserClaimValuesWithID(String userID, String[] claims, String profileName,
                                                  Map<String, String> claimMap, UserStoreManager userStoreManager)
            throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        /*
        If shared user profile update governance event listener is not enabled, skip shared profile resolver.
         */
        if (!sharedUserProfileUpdateGovernanceEventListener.isEnable()) {
            return true;
        }
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            if (!OrganizationManagementUtil.isOrganization(currentTenantDomain)) {
                // There is no shared users in root organizations. Hence, return.
                return true;
            }
            String currentOrganizationId = resolveOrganizationId(currentTenantDomain);
            UserAssociation userAssociation = getUserAssociation(userID, currentOrganizationId);
            if (userAssociation == null) {
                // User is not a shared user. Hence, return.
                return true;
            }
            // Analyse SharedProfileValueResolvingMethod value of claim and categorize.
            Map<ClaimConstants.SharedProfileValueResolvingMethod, List<String>> claimsByResolvingMethod =
                    categorizeClaimsByResolvingMethod(Arrays.asList(claims), currentTenantDomain);
            Map<String, String> resolvedClaimsFromOrigin =
                    resolveClaimsFromOrigin(userAssociation, claimsByResolvingMethod.get(FROM_ORIGIN), profileName);
            Map<String, String> resolvedClaimsFromSharedProfile =
                    resolveClaimsFromSharedProfile(claimMap, claimsByResolvingMethod.get(FROM_SHARED_PROFILE));
            Map<String, String> resolvedClaimsFromHierarchy = resolveClaimsFromHierarchy(userAssociation,
                    claimsByResolvingMethod.get(FROM_FIRST_FOUND_IN_HIERARCHY), currentOrganizationId);

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
        /*
        If shared user profile update governance event listener is not enabled, skip shared profile resolver.
         */
        if (!sharedUserProfileUpdateGovernanceEventListener.isEnable()) {
            return true;
        }
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            if (!OrganizationManagementUtil.isOrganization(currentTenantDomain)) {
                // There is no shared users in root organizations. Hence, return.
                return true;
            }
            String currentOrganizationId = resolveOrganizationId(currentTenantDomain);
            // Analyse SharedProfileValueResolvingMethod value of claim and categorize.
            Map<ClaimConstants.SharedProfileValueResolvingMethod, List<String>>
                    claimsByResolvingMethod = categorizeClaimsByResolvingMethod(claims, currentTenantDomain);
            for (UniqueIDUserClaimSearchEntry userClaimSearchEntry : userClaimSearchEntries) {
                User user = userClaimSearchEntry.getUser();
                UserAssociation userAssociation = getUserAssociation(user.getUserID(), currentOrganizationId);
                if (userAssociation == null) {
                    // User is not a shared user. Hence, return.
                    continue;
                }
                Map<String, String> resolvedClaimsFromOrigin =
                        resolveClaimsFromOrigin(userAssociation, claimsByResolvingMethod.get(FROM_ORIGIN), profileName);
                Map<String, String> resolvedClaimsFromSharedProfile =
                        resolveClaimsFromSharedProfile(userClaimSearchEntry.getClaims(),
                                claimsByResolvingMethod.get(FROM_SHARED_PROFILE));
                Map<String, String> resolvedClaimsFromHierarchy = resolveClaimsFromHierarchy(userAssociation,
                        claimsByResolvingMethod.get(FROM_FIRST_FOUND_IN_HIERARCHY), currentOrganizationId);

                Map<String, String> aggregatedProfileClaims = new HashMap<>();
                aggregatedProfileClaims.putAll(resolvedClaimsFromOrigin);
                aggregatedProfileClaims.putAll(resolvedClaimsFromHierarchy);
                aggregatedProfileClaims.putAll(resolvedClaimsFromSharedProfile);

                userClaimSearchEntry.setClaims(aggregatedProfileClaims);
                UserClaimSearchEntry searchEntryObject = userClaimSearchEntry.getUserClaimSearchEntry();
                if (searchEntryObject != null) {
                    searchEntryObject.setClaims(aggregatedProfileClaims);
                } else {
                    UserClaimSearchEntry userClaimSearchEntryForUser = new UserClaimSearchEntry();
                    userClaimSearchEntryForUser.setClaims(aggregatedProfileClaims);
                    userClaimSearchEntry.setUserClaimSearchEntry(userClaimSearchEntryForUser);
                }
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
        /*
        If shared user profile update governance event listener is not enabled, skip shared profile resolver.
         */
        if (!sharedUserProfileUpdateGovernanceEventListener.isEnable()) {
            return true;
        }
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            if (!OrganizationManagementUtil.isOrganization(currentTenantDomain)) {
                // There is no shared users in root organizations. Hence, return.
                return true;
            }
            String currentOrganizationId = resolveOrganizationId(currentTenantDomain);
            UserAssociation userAssociation = getUserAssociation(userID, currentOrganizationId);
            if (userAssociation == null) {
                // User is not a shared user. Hence, return.
                return true;
            }
            ClaimMetadataManagementService claimManagementService =
                    OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
            Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claim, currentTenantDomain);
            if (!localClaim.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Claim not found in the tenant domain: " + currentTenantDomain);
                }
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
                    boolean insideClaimResolverFunction =
                            IdentityUtil.threadLocalProperties.get().containsKey(INSIDE_CLAIM_RESOLVER_FLAG);
                    if (insideClaimResolverFunction) {
                        break;
                    }
                    try {
                        IdentityUtil.threadLocalProperties.get().put(INSIDE_CLAIM_RESOLVER_FLAG, "true");
                        OrgResourceResolverService orgResourceResolverService =
                                OrganizationUserSharingDataHolder.getInstance().getOrgResourceResolverService();
                        String resolvedClaimValueFromOrgHierarchy =
                                orgResourceResolverService.getResourcesFromOrgHierarchy(currentOrganizationId,
                                        LambdaExceptionUtils.rethrowFunction(
                                                orgId -> claimResolver(userAssociation.getAssociatedUserId(),
                                                        userAssociation.getUserResidentOrganizationId(), claim, orgId)),
                                        new FirstFoundAggregationStrategy<>());
                        claimValue.clear();
                        claimValue.add(resolvedClaimValueFromOrgHierarchy);
                    } finally {
                        IdentityUtil.threadLocalProperties.get().remove(INSIDE_CLAIM_RESOLVER_FLAG);
                    }
                    break;
            }
        } catch (OrganizationManagementException | ClaimMetadataException |
                 org.wso2.carbon.user.api.UserStoreException | OrgResourceHierarchyTraverseException e) {
            throw new UserStoreException(e.getMessage(), e);
        }
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValuesWithID(String userID, Map<String, String> claims, String profileName,
                                                 UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        // If the shared user profile update governance is enabled, skip the claim update blocking.
        if (sharedUserProfileUpdateGovernanceEventListener.isEnable()) {
            return true;
        }
        blockClaimUpdatesForSharedUser((AbstractUserStoreManager) userStoreManager, userID);
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
        // If the shared user profile update governance is enabled, skip the claim update blocking.
        if (sharedUserProfileUpdateGovernanceEventListener.isEnable()) {
            return true;
        }
        blockClaimUpdatesForSharedUser((AbstractUserStoreManager) userStoreManager, userID);
        if (CLAIM_MANAGED_ORGANIZATION.equals(claimURI)) {
            throw new UserStoreClientException(
                    String.format(ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getDescription(),
                            CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        return true;
    }

    /**
     * Resolve the claim value of the shared user in the given organization.
     *
     * @param associatedUserId                      The user id of the associated root or parent level user.
     * @param associationUserResidentOrganizationId The organization id where the associated user is resident.
     * @param claimURI                              The URI of the claim to be resolved.
     * @param organizationId                        The organization id where the current shared profile exists.
     * @return The resolved claim value.
     * @throws org.wso2.carbon.user.api.UserStoreException If an error occurs while resolving the claim value.
     */
    private Optional<String> claimResolver(String associatedUserId, String associationUserResidentOrganizationId,
                                           String claimURI, String organizationId)
            throws org.wso2.carbon.user.api.UserStoreException {

        try {
            OrganizationManager organizationManager =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
            String tenantDomainOfOrg = organizationManager.resolveTenantDomain(organizationId);
            AbstractUserStoreManager userStoreManager =
                    getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(tenantDomainOfOrg));
            // If associationUserResidentOrganizationId and organizationId are same, it should be a resident user.
            if (associationUserResidentOrganizationId != null &&
                    associationUserResidentOrganizationId.equals(organizationId)) {
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
            // If the searching org is not the user resident org, get the shared user id in given org.
            UserAssociation userAssociationOfAssociatedUserByOrgId =
                    OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                            .getUserAssociationOfAssociatedUserByOrgId(associatedUserId, organizationId);
            if (userAssociationOfAssociatedUserByOrgId == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("A shared user is not found for the user: %s in the organization: %s",
                            associatedUserId, organizationId));
                }
                return Optional.empty();
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

    private Map<ClaimConstants.SharedProfileValueResolvingMethod, List<String>> categorizeClaimsByResolvingMethod(
            List<String> claims, String tenantDomain) throws ClaimMetadataException {

        ClaimMetadataManagementService claimManagementService =
                OrganizationUserSharingDataHolder.getInstance().getClaimManagementService();
        Map<ClaimConstants.SharedProfileValueResolvingMethod, List<String>> claimsByResolvingMethod =
                new EnumMap<>(ClaimConstants.SharedProfileValueResolvingMethod.class);
        claimsByResolvingMethod.put(FROM_ORIGIN, new ArrayList<>());
        claimsByResolvingMethod.put(FROM_SHARED_PROFILE, new ArrayList<>());
        claimsByResolvingMethod.put(FROM_FIRST_FOUND_IN_HIERARCHY, new ArrayList<>());

        for (String claim : claims) {
            Optional<LocalClaim> localClaim = claimManagementService.getLocalClaim(claim, tenantDomain);
            if (!localClaim.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Claim: %s is not available in the tenant: %s.", claim, tenantDomain));
                }
                continue;
            }
            String resolvingMethod =
                    localClaim.map(claimEntry -> claimEntry.getClaimProperty(SHARED_PROFILE_VALUE_RESOLVING_METHOD))
                            .orElse(FROM_ORIGIN.getName());
            claimsByResolvingMethod.get(ClaimConstants.SharedProfileValueResolvingMethod.fromName(resolvingMethod))
                    .add(claim);
        }
        return claimsByResolvingMethod;
    }

    private Map<String, String> resolveClaimsFromHierarchy(UserAssociation userAssociation, List<String> claimURIs,
                                                           String currentOrganizationId)
            throws OrgResourceHierarchyTraverseException {

        Map<String, String> resolvedClaimsFromHierarchy = new HashMap<>();
        String associatedUserId = userAssociation.getAssociatedUserId();
        String associationUserResidentOrganizationId = userAssociation.getUserResidentOrganizationId();
        OrgResourceResolverService orgResourceResolverService =
                OrganizationUserSharingDataHolder.getInstance().getOrgResourceResolverService();
        for (String claim : claimURIs) {
            String resolvedClaimValueFromOrgHierarchy =
                    orgResourceResolverService.getResourcesFromOrgHierarchy(currentOrganizationId,
                            LambdaExceptionUtils.rethrowFunction(
                                    orgId -> claimResolver(associatedUserId, associationUserResidentOrganizationId,
                                            claim, orgId)),
                            new FirstFoundAggregationStrategy<>());
            resolvedClaimsFromHierarchy.put(claim, resolvedClaimValueFromOrgHierarchy);
        }
        return resolvedClaimsFromHierarchy;
    }

    private Map<String, String> resolveClaimsFromOrigin(UserAssociation userAssociation, List<String> claimURIs,
                                                        String profileName)
            throws OrganizationManagementException, org.wso2.carbon.user.api.UserStoreException {

        String associatedUserId = userAssociation.getAssociatedUserId();
        String associationUserResidentOrganizationId = userAssociation.getUserResidentOrganizationId();
        String userResidentTenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                .resolveTenantDomain(associationUserResidentOrganizationId);
        AbstractUserStoreManager residentUserStoreManager =
                getAbstractUserStoreManager(IdentityTenantUtil.getTenantId(userResidentTenantDomain));
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(userResidentTenantDomain, true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext()
                    .setOrganizationId(associationUserResidentOrganizationId);
            return residentUserStoreManager.getUserClaimValuesWithID(associatedUserId, claimURIs.toArray(new String[0]),
                    profileName);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private Map<String, String> resolveClaimsFromSharedProfile(Map<String, String> claimMap, List<String> claimURIs) {

        Map<String, String> resolveClaimsFromSharedProfile = new HashMap<>();
        for (String claimURI : claimURIs) {
            resolveClaimsFromSharedProfile.put(claimURI, claimMap.get(claimURI));
        }
        return resolveClaimsFromSharedProfile;
    }

    private void blockClaimUpdatesForSharedUser(AbstractUserStoreManager userStoreManager, String userID)
            throws UserStoreException {

        if (StringUtils.isEmpty(OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager, userID))) {
            return;
        }
        throw new UserStoreClientException(ERROR_CODE_SHARED_USER_CLAIM_UPDATE_NOT_ALLOWED.getMessage(),
                ERROR_CODE_SHARED_USER_CLAIM_UPDATE_NOT_ALLOWED.getCode());
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId)
            throws org.wso2.carbon.user.api.UserStoreException {

        RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    /**
     * Check whether the organizationID is properly found from PrivilegedCarbonContext, if not resolve it.
     *
     * @param currentTenantDomain   Current tenant domain.
     * @return Resolved organization id.
     * @throws OrganizationManagementException If an error occurs while resolving the organization id.
     */
    private String resolveOrganizationId(String currentTenantDomain) throws OrganizationManagementException {

        String currentOrganizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        if (StringUtils.isBlank(currentOrganizationId)) {
            currentOrganizationId = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                    .resolveOrganizationId(currentTenantDomain);
        }
        return currentOrganizationId;
    }

    /**
     * Find the user association from a given user id in the current organization.
     *
     * @param userID                User id in current organization.
     * @param currentOrganizationId Current organization id.
     * @return Resolved user association details.
     * @throws OrganizationManagementException If an error occurs while resolving the user association.
     */
    private UserAssociation getUserAssociation(String userID, String currentOrganizationId)
            throws OrganizationManagementException {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService()
                .getUserAssociation(userID, currentOrganizationId);
    }
}
