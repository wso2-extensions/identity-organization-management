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

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SHARED_PROFILE_VALUE_RESOLVING_METHOD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_SHARED_USER_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.user.core.UserCoreConstants.DEFAULT_PROFILE;

/**
 * Test cases for SharedUserOperationEventListener.
 */
public class SharedUserOperationEventListenerTest {

    private static final String ROOT_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String ROOT_TENANT_DOMAIN = "carbon.super";
    private static final String L1_ORG_ID = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String L1_ORG_TENANT_DOMAIN = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String L2_ORG_ID = "bf75bafa-605b-4d0b-add2-d5021217c5c4";
    private static final String L2_ORG_TENANT_DOMAIN = "bf75bafa-605b-4d0b-add2-d5021217c5c4";
    private static final String USER_1_USER_GIVEN_NAME = "John";
    private static final String USER_1_IN_ROOT = "user-id-1";
    private static final String SHARED_USER_OF_USER_1_IN_L1_ORG = "user-id-L1";
    private static final String SHARED_USER_OF_USER_1_IN_L2_ORG = "user-id-L2";
    private static final String USER_2_IN_L1_ORG = "user-id-2";
    private static final String GROUPS_CLAIM = "http://wso2.org/claims/groups";
    private static final String GIVEN_NAME_CLAIM = "http://wso2.org/claims/givenname";
    private static final String CUSTOM_CLAIM_1 = "http://wso2.org/claims/customAttribute1";
    private static final String CUSTOM_CLAIM_2 = "http://wso2.org/claims/customAttribute2";
    private static final String MANAGED_ORG_CLAIM = "http://wso2.org/claims/identity/managedOrg";

    @Mock
    OrganizationManager organizationManager;
    @Mock
    OrganizationUserSharingService organizationUserSharingService;
    @Mock
    ClaimMetadataManagementService claimManagementService;
    @Mock
    OrgResourceResolverService orgResourceResolverService;
    @Mock
    RealmService realmService;
    @Mock
    UserRealm tenantUserRealm;
    @Mock
    private AbstractUserStoreManager userStoreManager;
    @Mock
    IdentityEventListenerConfig identityEventListenerConfigForSharedUserProfileUpdateGovernanceEventListener;
    @Mock
    IdentityEventListenerConfig identityEventListenerConfigForSharedUserOperationEventListener;
    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilMockedStatic;
    private MockedStatic<PrivilegedCarbonContext> privilegedCarbonContext;
    private MockedStatic<IdentityTenantUtil> identityTenantUtil;

    @BeforeClass
    public void init() {

        // Open mock objects for the current test instance.
        openMocks(this);
        /*
        Set the OrganizationManager, ClaimMetadataManagementService, OrganizationUserSharingService, RealmService
        and OrgResourceResolverService to the data holder for use in tests.
         */
        OrganizationUserSharingDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationUserSharingDataHolder.getInstance().setClaimManagementService(claimManagementService);
        OrganizationUserSharingDataHolder.getInstance().setOrgResourceResolverService(orgResourceResolverService);
        OrganizationUserSharingDataHolder.getInstance()
                .setOrganizationUserSharingService(organizationUserSharingService);
        OrganizationUserSharingDataHolder.getInstance().setRealmService(realmService);
        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);
        setUpCarbonHome();
        privilegedCarbonContext = mockStatic(PrivilegedCarbonContext.class);
        identityTenantUtil = mockStatic(IdentityTenantUtil.class);
    }

    /**
     * Resets mock services after test class to ensure a clean state for subsequent tests.
     */
    @AfterClass
    public void tearDown() {

        reset(organizationManager);
        reset(claimManagementService);
        reset(orgResourceResolverService);
        reset(organizationUserSharingService);
        reset(realmService);
        organizationManagementUtilMockedStatic.close();
        privilegedCarbonContext.close();
        identityTenantUtil.close();
    }

    @Test
    public void testGetExecutionOrderId() {

        SharedUserOperationEventListener sharedUserOperationEventListener = new SharedUserOperationEventListener();
        assertEquals(sharedUserOperationEventListener.getExecutionOrderId(), 128);
    }

    @DataProvider(name = "dataProviderForTestSkippingClaimUpdateRestriction")
    public Object[][] dataProviderForTestSkippingClaimUpdateRestriction() {

        return new Object[][]{
                {false, false},
                {false, true},
                {true, true},
        };
    }

    @Test(dataProvider = "dataProviderForTestSkippingClaimUpdateRestriction")
    public void testSkippingClaimUpdateRestriction(boolean isSharedUserOperationEventListenerEnabled,
                                                   boolean isSharedUserProfileUpdateGovernanceEventListenerEnabled)
            throws UserStoreException {

        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            mockListenerEnabledStatus(isSharedUserOperationEventListenerEnabled,
                    isSharedUserProfileUpdateGovernanceEventListenerEnabled, identityUtil);

            SharedUserOperationEventListener sharedUserOperationEventListener = new SharedUserOperationEventListener();
            boolean listenerStatus =
                    sharedUserOperationEventListener.doPreSetUserClaimValueWithID(SHARED_USER_OF_USER_1_IN_L1_ORG,
                            MANAGED_ORG_CLAIM, "xyz", DEFAULT_PROFILE, userStoreManager);
            assertTrue(listenerStatus);

            HashMap<String, String> claimValueMap = new HashMap<>();
            claimValueMap.put(GIVEN_NAME_CLAIM, USER_1_USER_GIVEN_NAME);
            claimValueMap.put(MANAGED_ORG_CLAIM, ROOT_ORG_ID);
            listenerStatus =
                    sharedUserOperationEventListener.doPreSetUserClaimValuesWithID(SHARED_USER_OF_USER_1_IN_L1_ORG,
                            claimValueMap, DEFAULT_PROFILE, userStoreManager);
            assertTrue(listenerStatus);
        }
    }

    @DataProvider(name = "dataProviderForTestClaimUpdateRestriction")
    public Object[][] dataProviderForTestClaimUpdateRestriction() {

        Map<String, String> claimMapWithManagedOrg = new HashMap<>();
        claimMapWithManagedOrg.put(MANAGED_ORG_CLAIM, ROOT_ORG_ID);
        claimMapWithManagedOrg.put(GIVEN_NAME_CLAIM, USER_1_USER_GIVEN_NAME);

        Map<String, String> claimMapWithOutManagedOrg = new HashMap<>();
        claimMapWithOutManagedOrg.put(GIVEN_NAME_CLAIM, USER_1_USER_GIVEN_NAME);

        return new Object[][]{
                {USER_1_IN_ROOT, claimMapWithManagedOrg, true, false},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithManagedOrg, true, true},
                {USER_2_IN_L1_ORG, claimMapWithManagedOrg, true, false},
                {USER_1_IN_ROOT, claimMapWithOutManagedOrg, false, false},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithOutManagedOrg, true, true},
                {USER_2_IN_L1_ORG, claimMapWithOutManagedOrg, false, false},
        };
    }

    @Test(dataProvider = "dataProviderForTestClaimUpdateRestriction")
    public void testClaimUpdateRestrictionOnDoPreSetUserClaimValuesWithID(String userId, Map<String, String> claimMap,
                                                                          boolean exceptionExpected,
                                                                          boolean sharedUserUpdateBlockedException) {

        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class);
             MockedStatic<OrganizationSharedUserUtil> organizationSharedUserUtilMockedStatic = Mockito.mockStatic(
                     OrganizationSharedUserUtil.class)) {
            mockListenerEnabledStatus(true, false, identityUtil);
            mockManagedOrganizationClaimForUsers(organizationSharedUserUtilMockedStatic);
            SharedUserOperationEventListener sharedUserOperationEventListener = new SharedUserOperationEventListener();
            boolean listenerStatus =
                    sharedUserOperationEventListener.doPreSetUserClaimValuesWithID(userId, claimMap, DEFAULT_PROFILE,
                            userStoreManager);
            if (!exceptionExpected) {
                assertTrue(listenerStatus);
            }
        } catch (UserStoreException e) {
            if (sharedUserUpdateBlockedException) {
                assertEquals(e.getErrorCode(), ERROR_CODE_SHARED_USER_CLAIM_UPDATE_NOT_ALLOWED.getCode());
            } else {
                assertEquals(e.getErrorCode(), ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
            }
        }
    }

    @Test(dataProvider = "dataProviderForTestClaimUpdateRestriction")
    public void testClaimUpdateRestrictionOnDoPreSetUserClaimValueWithID(String userId, Map<String, String> claimMap,
                                                                         boolean exceptionExpected,
                                                                         boolean sharedUserUpdateBlockedException) {

        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class);
             MockedStatic<OrganizationSharedUserUtil> organizationSharedUserUtilMockedStatic = Mockito.mockStatic(
                     OrganizationSharedUserUtil.class)) {
            mockListenerEnabledStatus(true, false, identityUtil);
            mockManagedOrganizationClaimForUsers(organizationSharedUserUtilMockedStatic);
            SharedUserOperationEventListener sharedUserOperationEventListener = new SharedUserOperationEventListener();
            boolean listenerStatus = sharedUserOperationEventListener.doPreSetUserClaimValueWithID(userId,
                    claimMap.get(claimMap.keySet().iterator().next()), claimMap.get(0), DEFAULT_PROFILE,
                    userStoreManager);
            if (!exceptionExpected) {
                assertTrue(listenerStatus);
            }
        } catch (UserStoreException e) {
            if (sharedUserUpdateBlockedException) {
                assertEquals(e.getErrorCode(), ERROR_CODE_SHARED_USER_CLAIM_UPDATE_NOT_ALLOWED.getCode());
            } else {
                assertEquals(e.getErrorCode(), ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
            }
        }
    }

    @DataProvider(name = "dataProviderForTestSkippingSharedProfileResolvingCases")
    public Object[][] dataProviderForTestSkippingSharedProfileResolvingCases() {

        return new Object[][]{
                {false, false},
                {false, true},
                {true, false},
        };
    }

    @Test(dataProvider = "dataProviderForTestSkippingSharedProfileResolvingCases")
    public void testSkippingSharedProfileResolvingCases(boolean isSharedUserOperationEventListenerEnabled,
                                                        boolean isSharedUserProfileUpdateGovernanceEventListenerEnabled)
            throws UserStoreException {

        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            mockListenerEnabledStatus(isSharedUserOperationEventListenerEnabled,
                    isSharedUserProfileUpdateGovernanceEventListenerEnabled, identityUtil);
            SharedUserOperationEventListener sharedUserOperationEventListener = new SharedUserOperationEventListener();
            boolean listenerStatus =
                    sharedUserOperationEventListener.doPostGetUserClaimValueWithID(SHARED_USER_OF_USER_1_IN_L1_ORG,
                            CUSTOM_CLAIM_2, new ArrayList<>(), DEFAULT_PROFILE, userStoreManager);
            assertTrue(listenerStatus);

            listenerStatus =
                    sharedUserOperationEventListener.doPostGetUserClaimValuesWithID(SHARED_USER_OF_USER_1_IN_L1_ORG,
                            new String[]{}, DEFAULT_PROFILE, new HashMap<>(), userStoreManager);
            assertTrue(listenerStatus);

            listenerStatus = sharedUserOperationEventListener.doPostGetUsersClaimValuesWithID(new ArrayList<>(),
                    new ArrayList<>(), DEFAULT_PROFILE, new ArrayList<>(), userStoreManager);
            assertTrue(listenerStatus);
        }
    }

    @DataProvider(name = "dataProviderForTestDoPostGetUserClaimValueWithID")
    public Object[][] dataProviderForTestDoPostGetUserClaimValueWithID() {

        return new Object[][]{
                // Resolve from origin claim.
                {USER_1_IN_ROOT, ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false, GIVEN_NAME_CLAIM,  0},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true, GIVEN_NAME_CLAIM, 0},
                // Resolve from shared profile claim.
                {USER_1_IN_ROOT, ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false, GROUPS_CLAIM, 0},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true, GROUPS_CLAIM, 0},
                // Resolve from hierarchy claim.
                {USER_1_IN_ROOT, ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false, CUSTOM_CLAIM_1, 0},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true, CUSTOM_CLAIM_1, 1},
        };
    }

    @Test(dataProvider = "dataProviderForTestDoPostGetUserClaimValueWithID")
    public void testDoPostGetUserClaimValueWithID(String userId, String tenantDomain, String organizationId,
                                                  boolean isOrganization, String claimURI, int claimResolverCalledTimes)
            throws Exception {

        setUpClaims();
        setUpUserSharing();
        mockCarbonContextForTenant(tenantDomain, organizationId, privilegedCarbonContext);
        mockOrgIdResolverByTenantDomain();
        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(isOrganization);
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(1);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(tenantUserRealm);
        when(tenantUserRealm.getUserStoreManager()).thenReturn(userStoreManager);
        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            mockListenerEnabledStatus(true, true, identityUtil);
            SharedUserOperationEventListener sharedUserOperationEventListener = new SharedUserOperationEventListener();
            boolean listenerStatus =
                    sharedUserOperationEventListener.doPostGetUserClaimValueWithID(userId,
                            claimURI, new ArrayList<>(), DEFAULT_PROFILE, userStoreManager);
            verify(orgResourceResolverService, times(claimResolverCalledTimes)).getResourcesFromOrgHierarchy(
                    anyString(), any(), any());
            assertTrue(listenerStatus);
        }
    }

    private void mockOrgIdResolverByTenantDomain() throws OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(ROOT_TENANT_DOMAIN)).thenReturn(ROOT_ORG_ID);
        when(organizationManager.resolveOrganizationId(L1_ORG_TENANT_DOMAIN)).thenReturn(L1_ORG_ID);
        when(organizationManager.resolveOrganizationId(L2_ORG_TENANT_DOMAIN)).thenReturn(L2_ORG_ID);
    }

    private void mockManagedOrganizationClaimForUsers(
            MockedStatic<OrganizationSharedUserUtil> organizationSharedUserUtilMockedStatic) {

        organizationSharedUserUtilMockedStatic.when(
                () -> OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager, USER_1_IN_ROOT))
                .thenReturn(null);
        organizationSharedUserUtilMockedStatic.when(
                () -> OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager, USER_2_IN_L1_ORG))
                .thenReturn(null);
        organizationSharedUserUtilMockedStatic.when(
                () -> OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager,
                        SHARED_USER_OF_USER_1_IN_L1_ORG)).thenReturn(ROOT_ORG_ID);
    }

    private void mockListenerEnabledStatus(boolean isSharedUserOperationEventListenerEnabled,
                                           boolean isSharedUserProfileUpdateGovernanceEventListenerEnabled,
                                           MockedStatic<IdentityUtil> identityUtil) {

        identityUtil.when(() -> IdentityUtil.readEventListenerProperty(UserOperationEventListener.class.getName(),
                        SharedUserOperationEventListener.class.getName()))
                .thenReturn(identityEventListenerConfigForSharedUserOperationEventListener);
        when(identityEventListenerConfigForSharedUserOperationEventListener.getEnable()).thenReturn(
                String.valueOf(isSharedUserOperationEventListenerEnabled));

        identityUtil.when(() -> IdentityUtil.readEventListenerProperty(UserOperationEventListener.class.getName(),
                        SharedUserProfileUpdateGovernanceEventListener.class.getName()))
                .thenReturn(identityEventListenerConfigForSharedUserProfileUpdateGovernanceEventListener);
        when(identityEventListenerConfigForSharedUserProfileUpdateGovernanceEventListener.getEnable()).thenReturn(
                String.valueOf(isSharedUserProfileUpdateGovernanceEventListenerEnabled));
    }

    private void mockCarbonContextForTenant(String tenantDomain, String organizationId,
                                            MockedStatic<PrivilegedCarbonContext> privilegedCarbonContext) {

        PrivilegedCarbonContext mockPrivilegedCarbonContext = mock(PrivilegedCarbonContext.class);

        privilegedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                .thenReturn(mockPrivilegedCarbonContext);
        when(mockPrivilegedCarbonContext.getTenantDomain()).thenReturn(tenantDomain);
        when(mockPrivilegedCarbonContext.getOrganizationId()).thenReturn(organizationId);
        when(mockPrivilegedCarbonContext.getUsername()).thenReturn("admin");
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
    }

    private void setUpClaims() throws ClaimMetadataException {

        Map<String, String> claimPropertiesWithFromOriginResolvingMethod = new HashMap<>();
        claimPropertiesWithFromOriginResolvingMethod.put(SHARED_PROFILE_VALUE_RESOLVING_METHOD,
                ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN.getName());

        Map<String, String> claimPropertiesWithFromSharedProfileResolvingMethod = new HashMap<>();
        claimPropertiesWithFromSharedProfileResolvingMethod.put(SHARED_PROFILE_VALUE_RESOLVING_METHOD,
                ClaimConstants.SharedProfileValueResolvingMethod.FROM_SHARED_PROFILE.getName());

        Map<String, String> claimPropertiesWithFromHierarchyResolvingMethod = new HashMap<>();
        claimPropertiesWithFromHierarchyResolvingMethod.put(SHARED_PROFILE_VALUE_RESOLVING_METHOD,
                ClaimConstants.SharedProfileValueResolvingMethod.FROM_FIRST_FOUND_IN_HIERARCHY.getName());

        LocalClaim givenNameClaim =
                new LocalClaim(GIVEN_NAME_CLAIM, new ArrayList<>(), claimPropertiesWithFromOriginResolvingMethod);
        LocalClaim groupsClaim =
                new LocalClaim(GROUPS_CLAIM, new ArrayList<>(), claimPropertiesWithFromSharedProfileResolvingMethod);
        LocalClaim customClaim1 = new LocalClaim(CUSTOM_CLAIM_1, new ArrayList<>(),
                claimPropertiesWithFromHierarchyResolvingMethod);
        LocalClaim customClaim2 = new LocalClaim(CUSTOM_CLAIM_2, new ArrayList<>(), new HashMap<>());

        when(claimManagementService.getLocalClaim(GIVEN_NAME_CLAIM, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(givenNameClaim));
        when(claimManagementService.getLocalClaim(GIVEN_NAME_CLAIM, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(givenNameClaim));
        when(claimManagementService.getLocalClaim(GIVEN_NAME_CLAIM, L2_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(givenNameClaim));
        when(claimManagementService.getLocalClaim(GROUPS_CLAIM, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(groupsClaim));
        when(claimManagementService.getLocalClaim(GROUPS_CLAIM, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(groupsClaim));
        when(claimManagementService.getLocalClaim(GROUPS_CLAIM, L2_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(groupsClaim));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_1, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim1));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_1, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim1));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_1, L2_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim1));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_2, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim2));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_2, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim2));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_2, L2_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim2));
    }

    private void setUpUserSharing() throws OrganizationManagementException {

        UserAssociation userAssociationOfUser1InOrgL1 = new UserAssociation();
        userAssociationOfUser1InOrgL1.setUserId(SHARED_USER_OF_USER_1_IN_L1_ORG);
        userAssociationOfUser1InOrgL1.setOrganizationId(L1_ORG_ID);
        userAssociationOfUser1InOrgL1.setAssociatedUserId(USER_1_IN_ROOT);
        userAssociationOfUser1InOrgL1.setUserResidentOrganizationId(ROOT_ORG_ID);
        when(organizationUserSharingService.getUserAssociation(SHARED_USER_OF_USER_1_IN_L1_ORG, L1_ORG_ID)).thenReturn(
                userAssociationOfUser1InOrgL1);

        UserAssociation userAssociationOfUser1InOrgL2 = new UserAssociation();
        userAssociationOfUser1InOrgL2.setUserId(SHARED_USER_OF_USER_1_IN_L2_ORG);
        userAssociationOfUser1InOrgL2.setOrganizationId(L2_ORG_ID);
        userAssociationOfUser1InOrgL2.setAssociatedUserId(USER_1_IN_ROOT);
        userAssociationOfUser1InOrgL2.setUserResidentOrganizationId(ROOT_ORG_ID);
        when(organizationUserSharingService.getUserAssociation(SHARED_USER_OF_USER_1_IN_L2_ORG, L2_ORG_ID)).thenReturn(
                userAssociationOfUser1InOrgL2);

        when(organizationUserSharingService.getUserAssociation(USER_1_IN_ROOT, ROOT_ORG_ID)).thenReturn(null);
    }
}
