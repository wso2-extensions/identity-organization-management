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

import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedRolesDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APPLICATION_AUDIENCE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APP_1_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APP_2_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APP_ROLE_1_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APP_ROLE_1_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APP_ROLE_2_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.APP_ROLE_2_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.FIELD_USER_ID_RESOLVER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORGANIZATION_AUDIENCE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_1_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_1_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_2_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_2_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_3_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_3_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_ROLE_1_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_ROLE_1_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_ROLE_2_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_ROLE_2_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.ORG_SUPER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.PATH_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_1_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_2_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_3_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_4_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_5_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_DOMAIN_PRIMARY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.USER_NAME_PREFIX;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_EXCEPTION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_RESPONSE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_RESPONSE_SHARED_ORGS_COUNT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_RESPONSE_SHARED_ROLES_COUNT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_ORG_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_ROLE_AUDIENCE_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_ROLE_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.TestUserSharingConstants.VALIDATE_MSG_SHARED_USER_ID;

/**
 * Unit tests for UserSharingPolicyHandlerServiceImpl.
 */
public class UserSharingPolicyHandlerServiceImplTest {

    @InjectMocks
    private UserSharingPolicyHandlerServiceImpl userSharingPolicyHandlerService;

    private MockedStatic<UserSharingPolicyHandlerServiceImpl> userSharingPolicyHandlerServiceMockStatic;
    private MockedStatic<OrganizationUserSharingDataHolder> dataHolderMockStatic;
    private MockedStatic<Utils> utilsMockedStatic;
    private MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic;
    private MockedStatic<UserCoreUtil> userCoreUtilMockedStatic;

    @BeforeMethod
    public void setUp() {

        openMocks(this);
        userSharingPolicyHandlerServiceMockStatic = mockStatic(UserSharingPolicyHandlerServiceImpl.class);
        dataHolderMockStatic = mockStatic(OrganizationUserSharingDataHolder.class);
        utilsMockedStatic = mockStatic(Utils.class);
        identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
        userCoreUtilMockedStatic = mockStatic(UserCoreUtil.class);
    }

    @AfterMethod
    public void tearDown() {

        userSharingPolicyHandlerServiceMockStatic.close();
        dataHolderMockStatic.close();
        utilsMockedStatic.close();
        identityTenantUtilMockedStatic.close();
        userCoreUtilMockedStatic.close();
    }

    @DataProvider(name = "sharedOrgsDataProvider")
    public Object[][] sharedOrgsDataProvider() {

        return new Object[][]{
                {USER_1_ID, setExpectedResultsForGetSharedOrgsTestCase1()}, // Having many shared orgs.
                {USER_2_ID, setExpectedResultsForGetSharedOrgsTestCase2()}, // Having a single shared org.
                {USER_3_ID, Collections.emptyMap()} // Having no shared orgs.
        };
    }

    @Test(dataProvider = "sharedOrgsDataProvider")
    public void testGetSharedOrganizationsOfUser(String associatedUserId, Map<String, UserAssociation> expectedResults)
            throws Exception {

        utilsMockedStatic.when(Utils::getOrganizationId).thenReturn(ORG_SUPER_ID);

        OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
        when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);
        OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);
        OrganizationManager mockOrgManager = mock(OrganizationManager.class);
        when(dataHolder.getOrganizationManager()).thenReturn(mockOrgManager);

        List<UserAssociation> mockUserAssociations = new ArrayList<>();
        for (Map.Entry<String, UserAssociation> entry : expectedResults.entrySet()) {
            String orgName = entry.getKey();
            UserAssociation userAssociation = entry.getValue();
            when(mockOrgManager.getOrganizationNameById(userAssociation.getOrganizationId())).thenReturn(orgName);
            mockUserAssociations.add(userAssociation);
        }

        when(mockOrgUserSharingService.getUserAssociationsOfGivenUser(associatedUserId, ORG_SUPER_ID)).thenReturn(
                mockUserAssociations);

        // Call the method.
        ResponseSharedOrgsDO response =
                userSharingPolicyHandlerService.getSharedOrganizationsOfUser(associatedUserId, null, null, null, null,
                        false);

        // Validate response.
        assertNotNull(response, VALIDATE_MSG_RESPONSE);
        assertEquals(response.getSharedOrgs().size(), mockUserAssociations.size(),
                VALIDATE_MSG_RESPONSE_SHARED_ORGS_COUNT);
        for (int i = 0; i < mockUserAssociations.size(); i++) {
            UserAssociation expectedAssociation = mockUserAssociations.get(i);
            assertEquals(response.getSharedOrgs().get(i).getOrganizationId(), expectedAssociation.getOrganizationId(),
                    VALIDATE_MSG_SHARED_ORG_ID);
            assertEquals(response.getSharedOrgs().get(i).getSharedUserId(), expectedAssociation.getUserId(),
                    VALIDATE_MSG_SHARED_USER_ID);
            assertEquals(response.getSharedOrgs().get(i).getSharedType(), SharedType.SHARED, VALIDATE_MSG_SHARED_TYPE);
            assertTrue(expectedResults.containsKey(response.getSharedOrgs().get(i).getOrganizationName()),
                    VALIDATE_MSG_SHARED_ORG_NAME);
        }
    }

    @Test
    public void testGetSharedOrganizationsOfUserWithClientException() throws Exception {

        utilsMockedStatic.when(Utils::getOrganizationId).thenReturn(ORG_SUPER_ID);

        OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
        when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);
        OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);

        when(mockOrgUserSharingService.getUserAssociationsOfGivenUser(anyString(), anyString())).thenThrow(
                new OrganizationManagementException(VALIDATE_MSG_EXCEPTION));

        // Assert that the expected exception is thrown
        assertThrows(UserSharingMgtClientException.class,
                () -> userSharingPolicyHandlerService.getSharedOrganizationsOfUser(USER_1_ID, null, null, null, null,
                        false));
    }

    @DataProvider(name = "roleSharingDataProvider")
    public Object[][] roleSharingDataProvider() {

        return new Object[][]{
                {USER_1_ID, ORG_1_ID, setExpectedResultsForGetSharedRolesTestCase1()}, // Having many shared App Roles.
                {USER_2_ID, ORG_2_ID, setExpectedResultsForGetSharedRolesTestCase2()}, // Having one shared App Role.
                {USER_3_ID, ORG_3_ID, setExpectedResultsForGetSharedRolesTestCase3()}, // Having many shared Org Roles.
                {USER_1_ID, ORG_1_ID, setExpectedResultsForGetSharedRolesTestCase4()}, // Having one shared Org Role.
                {USER_1_ID, ORG_1_ID, setExpectedResultsForGetSharedRolesTestCase5()}, // Having many App and Org Roles.
                {USER_1_ID, ORG_1_ID, Collections.emptyList()} // Having no shared roles.
        };
    }

    @Test(dataProvider = "roleSharingDataProvider")
    public void testGetRolesSharedWithUserInOrganization(String userId, String orgId,
                                                         List<Role> expectedRoles) throws Exception {

        List<String> sharedRoleIds = expectedRoles.stream().map(Role::getId).collect(Collectors.toList());

        OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
        when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);
        OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);
        RoleManagementService mockRoleMgtService = mock(RoleManagementService.class);
        when(dataHolder.getRoleManagementService()).thenReturn(mockRoleMgtService);
        OrganizationManager mockOrgManager = mock(OrganizationManager.class);
        when(dataHolder.getOrganizationManager()).thenReturn(mockOrgManager);

        UserIDResolver mockUserIDResolver = mock(UserIDResolver.class);
        Field field = UserSharingPolicyHandlerServiceImpl.class.getDeclaredField(FIELD_USER_ID_RESOLVER);
        field.setAccessible(true);
        field.set(userSharingPolicyHandlerService, mockUserIDResolver);

        UserAssociation userAssociation = createUserAssociation(userId, orgId);
        when(mockOrgUserSharingService.getUserAssociationOfAssociatedUserByOrgId(userId, orgId)).thenReturn(
                userAssociation);

        when(mockOrgManager.resolveTenantDomain(anyString())).thenReturn(TENANT_DOMAIN);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);

        String domainQualifiedUserName = USER_NAME_PREFIX + USER_DOMAIN_PRIMARY + PATH_SEPARATOR + userId;
        String userName = USER_NAME_PREFIX + userId;
        when(mockUserIDResolver.getNameByID(userId, TENANT_DOMAIN)).thenReturn(domainQualifiedUserName);
        userCoreUtilMockedStatic.when(() -> UserCoreUtil.removeDomainFromName(domainQualifiedUserName))
                .thenReturn(userName);
        userCoreUtilMockedStatic.when(() -> UserCoreUtil.extractDomainFromName(domainQualifiedUserName))
                .thenReturn(USER_DOMAIN_PRIMARY);

        when(mockOrgUserSharingService.getRolesSharedWithUserInOrganization(any(), anyInt(), any())).thenReturn(
                sharedRoleIds);

        for (Role role : expectedRoles) {
            when(mockRoleMgtService.getRole(role.getId(), TENANT_DOMAIN)).thenReturn(role);
        }

        // Call the method.
        ResponseSharedRolesDO response =
                userSharingPolicyHandlerService.getRolesSharedWithUserInOrganization(userId, orgId, null, null, null,
                        null, false);

        // Validate response.
        assertNotNull(response, VALIDATE_MSG_RESPONSE);
        assertEquals(response.getSharedRoles().size(), expectedRoles.size(), VALIDATE_MSG_RESPONSE_SHARED_ROLES_COUNT);
        for (int i = 0; i < expectedRoles.size(); i++) {
            assertEquals(response.getSharedRoles().get(i).getRoleName(), expectedRoles.get(i).getName(),
                    VALIDATE_MSG_SHARED_ROLE_NAME);
            assertEquals(response.getSharedRoles().get(i).getAudienceName(), expectedRoles.get(i).getAudienceName(),
                    VALIDATE_MSG_SHARED_ROLE_AUDIENCE_NAME);
            assertEquals(response.getSharedRoles().get(i).getAudienceType(), expectedRoles.get(i).getAudience(),
                    VALIDATE_MSG_SHARED_ROLE_AUDIENCE_TYPE);
        }
    }

    @DataProvider(name = "roleSharingForUnSharedUserDataProvider")
    public Object[][] roleSharingForUnSharedUserDataProvider() {

        return new Object[][]{
                {USER_4_ID, ORG_1_ID, Collections.emptyList()}, // Having no shared user association in Org 1.
                {USER_5_ID, ORG_2_ID, Collections.emptyList()} // Having no shared user association in Org 2.
        };
    }

    @Test(dataProvider = "roleSharingForUnSharedUserDataProvider")
    public void testGetRolesSharedWithUserForUnSharedUserInOrganization(String userId, String orgId,
                                                                        List<Role> expectedRoles) throws Exception {

        OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
        when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);
        OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);
        RoleManagementService mockRoleMgtService = mock(RoleManagementService.class);
        when(dataHolder.getRoleManagementService()).thenReturn(mockRoleMgtService);
        OrganizationManager mockOrgManager = mock(OrganizationManager.class);
        when(dataHolder.getOrganizationManager()).thenReturn(mockOrgManager);

        UserIDResolver mockUserIDResolver = mock(UserIDResolver.class);
        Field field = UserSharingPolicyHandlerServiceImpl.class.getDeclaredField(FIELD_USER_ID_RESOLVER);
        field.setAccessible(true);
        field.set(userSharingPolicyHandlerService, mockUserIDResolver);

        // Call the method.
        ResponseSharedRolesDO response =
                userSharingPolicyHandlerService.getRolesSharedWithUserInOrganization(userId, orgId, null, null, null,
                        null, false);

        // Validate response.
        assertNotNull(response, VALIDATE_MSG_RESPONSE);
        assertEquals(response.getSharedRoles().size(), expectedRoles.size(), VALIDATE_MSG_RESPONSE_SHARED_ROLES_COUNT);
    }

    @Test
    public void testGetRolesSharedWithUserInOrganizationWithClientException() throws Exception {

        OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
        when(OrganizationUserSharingDataHolder.getInstance()).thenReturn(dataHolder);

        OrganizationUserSharingService mockOrgUserSharingService = mock(OrganizationUserSharingService.class);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(mockOrgUserSharingService);

        OrganizationManager mockOrgManager = mock(OrganizationManager.class);
        when(dataHolder.getOrganizationManager()).thenReturn(mockOrgManager);

        UserAssociation userAssociation = createUserAssociation(USER_1_ID, ORG_1_ID);
        when(mockOrgUserSharingService.getUserAssociationOfAssociatedUserByOrgId(USER_1_ID, ORG_1_ID)).thenReturn(
                userAssociation);

        when(mockOrgManager.resolveTenantDomain(anyString())).thenThrow(
                new OrganizationManagementException(VALIDATE_MSG_EXCEPTION));

        // Assert that the expected exception is thrown
        assertThrows(UserSharingMgtClientException.class,
                () -> userSharingPolicyHandlerService.getRolesSharedWithUserInOrganization(USER_1_ID, ORG_1_ID, null,
                        null, null, null, false));
    }

    // Test case Builders.

    private Map<String, UserAssociation> setExpectedResultsForGetSharedOrgsTestCase1() {

        Map<String, UserAssociation> expectedResults = new HashMap<>();
        expectedResults.put(ORG_1_NAME, createUserAssociation(USER_1_ID, ORG_1_ID));
        expectedResults.put(ORG_2_NAME, createUserAssociation(USER_1_ID, ORG_2_ID));
        expectedResults.put(ORG_3_NAME, createUserAssociation(USER_1_ID, ORG_3_ID));
        return expectedResults;
    }

    private Map<String, UserAssociation> setExpectedResultsForGetSharedOrgsTestCase2() {

        Map<String, UserAssociation> expectedResults = new HashMap<>();
        expectedResults.put(ORG_1_NAME, createUserAssociation(USER_3_ID, ORG_1_ID));
        return expectedResults;
    }

    private static List<Role> setExpectedResultsForGetSharedRolesTestCase1() {

        Role role1 = new Role();
        role1.setId(APP_ROLE_1_ID);
        role1.setName(APP_ROLE_1_NAME);
        role1.setAudienceName(APP_1_NAME);
        role1.setAudience(APPLICATION_AUDIENCE);

        Role role2 = new Role();
        role2.setId(APP_ROLE_2_ID);
        role2.setName(APP_ROLE_2_NAME);
        role2.setAudienceName(APP_1_NAME);
        role2.setAudience(APPLICATION_AUDIENCE);

        return Arrays.asList(role1, role2);
    }

    private static List<Role> setExpectedResultsForGetSharedRolesTestCase2() {

        Role role = new Role();
        role.setId(APP_ROLE_2_ID);
        role.setName(APP_ROLE_2_NAME);
        role.setAudienceName(APP_1_NAME);
        role.setAudience(APPLICATION_AUDIENCE);

        return Collections.singletonList(role);
    }

    private static List<Role> setExpectedResultsForGetSharedRolesTestCase3() {

        Role role1 = new Role();
        role1.setId(ORG_ROLE_1_ID);
        role1.setName(ORG_ROLE_1_NAME);
        role1.setAudienceName(APP_2_NAME);
        role1.setAudience(ORGANIZATION_AUDIENCE);

        Role role2 = new Role();
        role2.setId(ORG_ROLE_2_ID);
        role2.setName(ORG_ROLE_2_NAME);
        role2.setAudienceName(APP_2_NAME);
        role2.setAudience(ORGANIZATION_AUDIENCE);

        return Arrays.asList(role1, role2);
    }

    private static List<Role> setExpectedResultsForGetSharedRolesTestCase4() {

        Role role = new Role();
        role.setId(ORG_ROLE_2_ID);
        role.setName(ORG_ROLE_2_NAME);
        role.setAudienceName(APP_2_NAME);
        role.setAudience(ORGANIZATION_AUDIENCE);

        return Collections.singletonList(role);
    }

    private static List<Role> setExpectedResultsForGetSharedRolesTestCase5() {

        Role role1 = new Role();
        role1.setId(ORG_ROLE_2_ID);
        role1.setName(ORG_ROLE_2_NAME);
        role1.setAudienceName(APP_2_NAME);
        role1.setAudience(ORGANIZATION_AUDIENCE);

        Role role2 = new Role();
        role2.setId(APP_ROLE_1_ID);
        role2.setName(APP_ROLE_1_NAME);
        role2.setAudienceName(APP_1_NAME);
        role2.setAudience(APPLICATION_AUDIENCE);

        return Arrays.asList(role1, role2);
    }

    // Helper Methods.

    private UserAssociation createUserAssociation(String userId, String organizationId) {

        UserAssociation userAssociation = new UserAssociation();
        userAssociation.setUserId(userId);
        userAssociation.setOrganizationId(organizationId);
        userAssociation.setSharedType(SharedType.SHARED);
        return userAssociation;
    }
}
