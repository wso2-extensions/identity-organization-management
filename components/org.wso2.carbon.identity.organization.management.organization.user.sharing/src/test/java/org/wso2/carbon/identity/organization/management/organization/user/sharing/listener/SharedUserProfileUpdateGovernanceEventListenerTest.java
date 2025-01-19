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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.SHARED_PROFILE_VALUE_RESOLVING_METHOD;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.user.core.UserCoreConstants.DEFAULT_PROFILE;

/**
 * Test cases for SharedUserProfileUpdateGovernanceEventListener.
 */
public class SharedUserProfileUpdateGovernanceEventListenerTest {

    private static final String ROOT_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String ROOT_TENANT_DOMAIN = "carbon.super";
    private static final String L1_ORG_ID = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String L1_ORG_TENANT_DOMAIN = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String USER_1_IN_ROOT = "user-id-1";
    private static final String SHARED_USER_OF_USER_1_IN_L1_ORG = "user-id-L1";
    private static final String GROUPS_CLAIM = "http://wso2.org/claims/groups";
    private static final String GIVEN_NAME_CLAIM = "http://wso2.org/claims/givenname";
    private static final String CUSTOM_CLAIM_1 = "http://wso2.org/claims/customAttribute1";
    private static final String CUSTOM_CLAIM_2 = "http://wso2.org/claims/customAttribute2";
    private static final String INVALID_CLAIM = "http://wso2.org/claims/invalidClaim";

    @Mock
    OrganizationManager organizationManager;
    @Mock
    ClaimMetadataManagementService claimManagementService;
    @Mock
    OrganizationUserSharingService organizationUserSharingService;
    @Mock
    private UserStoreManager userStoreManager;
    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilMockedStatic;
    MockedStatic<PrivilegedCarbonContext> privilegedCarbonContext;

    @BeforeClass
    public void init() {

        // Open mock objects for the current test instance.
        openMocks(this);
        /*
        Set the OrganizationManager, ClaimMetadataManagementService and OrganizationUserSharingService
        to the data holder for use in tests.
         */
        OrganizationUserSharingDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationUserSharingDataHolder.getInstance().setClaimManagementService(claimManagementService);
        OrganizationUserSharingDataHolder.getInstance()
                .setOrganizationUserSharingService(organizationUserSharingService);
        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);
        setUpCarbonHome();
        privilegedCarbonContext = mockStatic(PrivilegedCarbonContext.class);
    }

    /**
     * Resets mock services after test class to ensure a clean state for subsequent tests.
     */
    @AfterClass
    public void tearDown() {

        reset(organizationManager);
        reset(claimManagementService);
        reset(organizationUserSharingService);
        organizationManagementUtilMockedStatic.close();
        privilegedCarbonContext.close();
    }

    @DataProvider(name = "dataProviderForUpdateManagedOrgClaim")
    public Object[][] dataProviderForUpdateManagedOrgClaim() {

        Map<String, String> claimMapWithManagedOrganization = new HashMap<>();
        claimMapWithManagedOrganization.put(CLAIM_MANAGED_ORGANIZATION, "8d9ad9eba869-a5ba-4275-a52b-ad9ad9eba869");

        return new Object[][]{
                {USER_1_IN_ROOT, claimMapWithManagedOrganization},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithManagedOrganization},
        };
    }

    @Test(dataProvider = "dataProviderForUpdateManagedOrgClaim")
    public void testUpdateManagedOrgClaim(String userID, Map<String, String> claims) {

        SharedUserProfileUpdateGovernanceEventListener sharedUserProfileUpdateGovernanceEventListener =
                new SharedUserProfileUpdateGovernanceEventListener();
        try {
            sharedUserProfileUpdateGovernanceEventListener.doPreSetUserClaimValuesWithID(userID, claims,
                    DEFAULT_PROFILE,
                    userStoreManager);
        } catch (UserStoreException e) {
            assertEquals(e.getErrorCode(), ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
    }

    @DataProvider(name = "dataProviderForDoPreSetUserClaimValuesWithID")
    public Object[][] dataProviderForDoPreSetUserClaimValuesWithID() {

        Map<String, String> claimMapWithFromOriginResolvedClaim = new HashMap<>();
        claimMapWithFromOriginResolvedClaim.put(GIVEN_NAME_CLAIM, "John");

        Map<String, String> claimMapWithFromSharedProfileResolvingClaim = new HashMap<>();
        claimMapWithFromSharedProfileResolvingClaim.put(GROUPS_CLAIM, "group1,group2");

        Map<String, String> claimMapWithFromHierarchyResolvingClaim = new HashMap<>();
        claimMapWithFromHierarchyResolvingClaim.put(CUSTOM_CLAIM_1, "value1");

        Map<String, String> claimMapWithBlankResolvingMethodClaim = new HashMap<>();
        claimMapWithBlankResolvingMethodClaim.put(CUSTOM_CLAIM_2, "value2");

        Map<String, String> claimMapWithInvalidClaim = new HashMap<>();
        claimMapWithInvalidClaim.put(INVALID_CLAIM, "invalid");

        return new Object[][]{
                {USER_1_IN_ROOT, claimMapWithFromOriginResolvedClaim, ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false, true},
                {USER_1_IN_ROOT, claimMapWithFromHierarchyResolvingClaim, ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false,
                        true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithFromSharedProfileResolvingClaim, L1_ORG_TENANT_DOMAIN,
                        L1_ORG_ID, true, true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithFromHierarchyResolvingClaim, L1_ORG_TENANT_DOMAIN,
                        L1_ORG_ID, true, true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithInvalidClaim, L1_ORG_TENANT_DOMAIN, L1_ORG_ID,
                        true, true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithFromOriginResolvedClaim, L1_ORG_TENANT_DOMAIN, L1_ORG_ID,
                        true, false},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, claimMapWithBlankResolvingMethodClaim, L1_ORG_TENANT_DOMAIN,
                        L1_ORG_ID, true, false},
        };
    }

    @Test(dataProvider = "dataProviderForDoPreSetUserClaimValuesWithID")
    public void testDoPreSetUserClaimValuesWithID(String userID, Map<String, String> claims, String tenantDomain,
                                                  String organizationId, boolean isOrganization,
                                                  boolean successfullyInvokedListener) throws Exception {

        setUpUserSharing();
        setUpClaims();
        mockCarbonContextForTenant(tenantDomain, organizationId, privilegedCarbonContext);
        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(isOrganization);

        SharedUserProfileUpdateGovernanceEventListener sharedUserProfileUpdateGovernanceEventListener =
                new SharedUserProfileUpdateGovernanceEventListener();
        try {
            boolean listenerStatus =
                    sharedUserProfileUpdateGovernanceEventListener.doPreSetUserClaimValuesWithID(userID, claims,
                            DEFAULT_PROFILE, userStoreManager);
            if (successfullyInvokedListener) {
                assertTrue(listenerStatus);
            }
        } catch (UserStoreException e) {
            if (!successfullyInvokedListener) {
                assertEquals(e.getMessage(), String.format(
                        String.format("Claim: %s is not allowed to be updated for shared users.",
                                claims.keySet().iterator().next())));
            }
        }
    }

    @DataProvider(name = "dataProviderForUpdateManagedOrgClaimThroughSetUserClaimValueWithID")
    public Object[][] dataProviderForUpdateManagedOrgClaimThroughSetUserClaimValueWithID() {

        return new Object[][]{
                {USER_1_IN_ROOT, CLAIM_MANAGED_ORGANIZATION, L1_ORG_ID},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, CLAIM_MANAGED_ORGANIZATION, L1_ORG_ID},
        };
    }

    @Test(dataProvider = "dataProviderForUpdateManagedOrgClaimThroughSetUserClaimValueWithID")
    public void testUpdateManagedOrgClaimThroughSetUserClaimValueWithID(String userID, String claimURI,
                                                                        String organizationId) {

        SharedUserProfileUpdateGovernanceEventListener sharedUserProfileUpdateGovernanceEventListener =
                new SharedUserProfileUpdateGovernanceEventListener();
        try {
            sharedUserProfileUpdateGovernanceEventListener.doPreSetUserClaimValueWithID(userID, claimURI,
                    organizationId,
                    DEFAULT_PROFILE, userStoreManager);
        } catch (UserStoreException e) {
            assertEquals(e.getErrorCode(), ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
    }

    @DataProvider(name = "dataProviderForDoPreSetUserClaimValueWithID")
    public Object[][] dataProviderForDoPreSetUserClaimValueWithID() {

        return new Object[][]{
                {USER_1_IN_ROOT, GIVEN_NAME_CLAIM, "John", ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false, true},
                {USER_1_IN_ROOT, CUSTOM_CLAIM_1, "value1", ROOT_TENANT_DOMAIN, ROOT_ORG_ID, false, true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, GROUPS_CLAIM, "group1,group2", L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true,
                        true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, CUSTOM_CLAIM_1, "value1", L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true,
                        true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, INVALID_CLAIM, "invalid", L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true,
                        true},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, GIVEN_NAME_CLAIM, "John", L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true,
                        false},
                {SHARED_USER_OF_USER_1_IN_L1_ORG, CUSTOM_CLAIM_2, "value2", L1_ORG_TENANT_DOMAIN, L1_ORG_ID, true,
                        false},
        };
    }

    @Test(dataProvider = "dataProviderForDoPreSetUserClaimValueWithID")
    public void testDoPreSetUserClaimValueWithID(String userID, String claimURI, String claimValue, String tenantDomain,
                                                 String organizationId, boolean isOrganization,
                                                 boolean successfullyInvokedListener) throws Exception {

        setUpUserSharing();
        setUpClaims();
        mockCarbonContextForTenant(tenantDomain, organizationId, privilegedCarbonContext);
        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(isOrganization);

        SharedUserProfileUpdateGovernanceEventListener sharedUserProfileUpdateGovernanceEventListener =
                new SharedUserProfileUpdateGovernanceEventListener();
        try {
            boolean listenerStatus =
                    sharedUserProfileUpdateGovernanceEventListener.doPreSetUserClaimValueWithID(userID, claimURI,
                            claimValue, DEFAULT_PROFILE, userStoreManager);
            if (successfullyInvokedListener) {
                assertTrue(listenerStatus);
            }
        } catch (UserStoreException e) {
            if (!successfullyInvokedListener) {
                assertEquals(e.getMessage(), String.format(
                        String.format("Claim: %s is not allowed to be updated for shared users.", claimURI)));
            }
        }
    }

    private void setUpClaims() throws ClaimMetadataException {

        Map<String, String> claimPropertiesWithFromOriginResolvingMethod = new HashMap<>();
        claimPropertiesWithFromOriginResolvingMethod.put(SHARED_PROFILE_VALUE_RESOLVING_METHOD,
                ClaimConstants.SharedProfileValueResolvingMethod.FROM_ORIGIN.getName());

        Map<String, String> claimPropertiesWithFromSharedProfileResolvingMethod = new HashMap<>();
        claimPropertiesWithFromSharedProfileResolvingMethod.put(SHARED_PROFILE_VALUE_RESOLVING_METHOD,
                ClaimConstants.SharedProfileValueResolvingMethod.FROM_SHARED_PROFILE.getName());

        Map<String, String> claimPropertiesWithFromHierarchyProfileResolvingMethod = new HashMap<>();
        claimPropertiesWithFromHierarchyProfileResolvingMethod.put(SHARED_PROFILE_VALUE_RESOLVING_METHOD,
                ClaimConstants.SharedProfileValueResolvingMethod.FROM_FIRST_FOUND_IN_HIERARCHY.getName());

        LocalClaim givenNameClaim =
                new LocalClaim(GIVEN_NAME_CLAIM, new ArrayList<>(), claimPropertiesWithFromOriginResolvingMethod);
        LocalClaim groupsClaim =
                new LocalClaim(GROUPS_CLAIM, new ArrayList<>(), claimPropertiesWithFromSharedProfileResolvingMethod);
        LocalClaim customClaim1 = new LocalClaim(CUSTOM_CLAIM_1, new ArrayList<>(),
                claimPropertiesWithFromHierarchyProfileResolvingMethod);
        LocalClaim customClaim2 = new LocalClaim(CUSTOM_CLAIM_2, new ArrayList<>(), new HashMap<>());

        when(claimManagementService.getLocalClaim(GIVEN_NAME_CLAIM, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(givenNameClaim));
        when(claimManagementService.getLocalClaim(GIVEN_NAME_CLAIM, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(givenNameClaim));
        when(claimManagementService.getLocalClaim(GROUPS_CLAIM, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(groupsClaim));
        when(claimManagementService.getLocalClaim(GROUPS_CLAIM, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(groupsClaim));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_1, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim1));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_1, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim1));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_2, ROOT_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim2));
        when(claimManagementService.getLocalClaim(CUSTOM_CLAIM_2, L1_ORG_TENANT_DOMAIN)).thenReturn(
                Optional.of(customClaim2));
    }

    private void setUpUserSharing() throws OrganizationManagementException {

        UserAssociation userAssociation = new UserAssociation();
        userAssociation.setUserId(SHARED_USER_OF_USER_1_IN_L1_ORG);
        userAssociation.setOrganizationId(L1_ORG_ID);
        userAssociation.setAssociatedUserId(USER_1_IN_ROOT);
        userAssociation.setUserResidentOrganizationId(ROOT_ORG_ID);
        when(organizationUserSharingService.getUserAssociation(SHARED_USER_OF_USER_1_IN_L1_ORG, L1_ORG_ID)).thenReturn(
                userAssociation);
        when(organizationUserSharingService.getUserAssociation(USER_1_IN_ROOT, ROOT_ORG_ID)).thenReturn(null);
    }

    private void mockCarbonContextForTenant(String tenantDomain, String organizationId,
                                            MockedStatic<PrivilegedCarbonContext> privilegedCarbonContext) {

        PrivilegedCarbonContext mockPrivilegedCarbonContext = mock(PrivilegedCarbonContext.class);

        privilegedCarbonContext.when(
                PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(mockPrivilegedCarbonContext);
        when(mockPrivilegedCarbonContext.getTenantDomain()).thenReturn(tenantDomain);
        when(mockPrivilegedCarbonContext.getOrganizationId()).thenReturn(organizationId);
        when(mockPrivilegedCarbonContext.getUsername()).thenReturn("admin");
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
    }
}
