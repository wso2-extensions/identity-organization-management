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

package org.wso2.carbon.identity.organization.user.invitation.management;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAO;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAOImpl;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtClientException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.internal.UserInvitationMgtDataHolder;
import org.wso2.carbon.identity.organization.user.invitation.management.models.GroupAssignments;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;
import org.wso2.carbon.identity.organization.user.invitation.management.models.InvitationDO;
import org.wso2.carbon.identity.organization.user.invitation.management.models.InvitationResult;
import org.wso2.carbon.identity.organization.user.invitation.management.models.RoleAssignments;
import org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils;
import org.wso2.carbon.identity.recovery.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.CLAIM_EMAIL_ADDRESS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_01_CONF_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_01_EMAIL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_01_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_01_INV_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_01_UN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_01_USER_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_02_CONF_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_02_EMAIL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_02_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_02_INV_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_02_UN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_02_USER_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_03_CONF_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_03_EMAIL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_03_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_03_INV_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_03_UN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_03_USER_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_04_CONF_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_04_EMAIL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_04_INVITATION_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_04_INV_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_04_UN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INV_04_USER_ORG_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils.closeH2Base;
import static org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils.getConnection;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ConnectorConfig.EMAIL_VERIFICATION_NOTIFICATION_INTERNALLY_MANAGE;

public class InvitationCoreServiceImplTest {

    private final UserInvitationDAO userInvitationDAO = new UserInvitationDAOImpl();
    private InvitationCoreServiceImpl invitationCoreService;
    private final String [] roleList = {"1224", "12345"};

    private final String [] groupList = {"4321", "54321"};

    @Mock
    private RealmService realmService;
    @Mock
    private UserRealm userRealm;
    @Mock
    private AbstractUserStoreManager userStoreManager;
    @Mock
    private IdentityEventService identityEventService;

    private MockedStatic<IdentityUtil> identityUtilMockedStatic;
    private MockedStatic<OrganizationSharedUserUtil> organizationSharedUserUtilMockedStatic;
    private MockedStatic<Utils> utilsMockedStatic;

    @BeforeClass
    public void setUp() throws Exception {

        // Initialize mocks
        MockitoAnnotations.initMocks(this);

        invitationCoreService = new InvitationCoreServiceImpl();
        TestUtils.initiateH2Base();
        setUpCarbonHome();
        mockCarbonContextForTenant();

        identityUtilMockedStatic = mockStatic(IdentityUtil.class);
        identityUtilMockedStatic.when(() -> IdentityUtil.getProperty(anyString())).thenReturn("1440");

        organizationSharedUserUtilMockedStatic = mockStatic(OrganizationSharedUserUtil.class);
        utilsMockedStatic = mockStatic(Utils.class);

        Invitation invitation1 = buildInvitation(INV_01_INVITATION_ID, INV_01_CONF_CODE, INV_01_UN,
                "DEFAULT", INV_01_EMAIL,
                "https://localhost:8080/travel-manager-001/invitations/accept", INV_01_USER_ORG_ID,
                INV_01_INV_ORG_ID, null,  null, "PENDING");
        Invitation invitation2 = buildInvitation(INV_02_INVITATION_ID, INV_02_CONF_CODE, INV_02_UN,
                "DEFAULT", INV_02_EMAIL,
                "https://localhost:8080/travel-manager-001/invitations/accept",
                INV_02_USER_ORG_ID, INV_02_INV_ORG_ID, null, null, "PENDING");
        RoleAssignments roleAssignments2 = buildRoleAssignments(roleList);
        GroupAssignments groupAssignments = buildGroupAssignments(groupList);
        Invitation invitation3 = buildInvitation(INV_03_INVITATION_ID, INV_03_CONF_CODE, INV_03_UN,
                "DEFAULT", INV_03_EMAIL,
                "https://localhost:8080/travel-manager-001/invitations/accept", INV_03_USER_ORG_ID,
                INV_03_INV_ORG_ID, new RoleAssignments[]{roleAssignments2},  null, "PENDING");
        Invitation invitation4 = buildInvitation(INV_04_INVITATION_ID, INV_04_CONF_CODE, INV_04_UN,
                "DEFAULT", INV_04_EMAIL,
                "https://localhost:8080/travel-manager-001/invitations/accept", INV_04_USER_ORG_ID,
                INV_04_INV_ORG_ID, new RoleAssignments[]{roleAssignments2}, new GroupAssignments[]{groupAssignments},
                "PENDING");

        storeParentUserInvitation(invitation1);
        storeParentUserInvitation(invitation2);
        storeParentUserInvitation(invitation3);
        storeParentUserInvitation(invitation4);

        UserInvitationMgtDataHolder.getInstance().setRealmService(realmService);
        UserInvitationMgtDataHolder.getInstance().setIdentityEventService(identityEventService);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(userStoreManager.getSecondaryUserStoreManager(anyString())).thenReturn(userStoreManager);
        doNothing().when(identityEventService).handleEvent(isA(Event.class));
    }

    private Role buildRoleInfo() {

        Role roleInfo = new Role();
        roleInfo.setAudience("application");
        roleInfo.setAudienceId("98765");
        roleInfo.setAudienceName("Console");
        roleInfo.setName("testApp");
        return roleInfo;
    }

    @AfterClass
    public void tearDown() throws Exception {

        closeH2Base();
        Mockito.reset(realmService);
        Mockito.reset(userRealm);
        Mockito.reset(userStoreManager);
        Mockito.reset(identityEventService);
    }


    @DataProvider(name = "getInvitationFilter")
    public Object[][] getInvitationFilter() {

        return new Object[][]{
                {"status eq PENDING SAMPLE"},
                {"status sw PENDING"}
        };
    }

    @Test(priority = 1)
    public void testGetInvitation() throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class);
             MockedStatic<IdentityTenantUtil> identityUtil = Mockito.mockStatic(IdentityTenantUtil.class)) {

            identityUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            RoleManagementService roleManagementService = mock(RoleManagementService.class);
            UserInvitationMgtDataHolder.getInstance().setRoleManagementService(roleManagementService);
            when(roleManagementService.getRoleWithoutUsers(anyString(), anyString())).thenReturn(buildRoleInfo());
            OrganizationManager organizationManager = mock(OrganizationManager.class);
            UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);
            when(organizationManager.resolveTenantDomain(anyString())).thenReturn("carbon.super");

            List<Invitation> invitationList = invitationCoreService.getInvitations(null);
            // Checking whether the size of the Invitation list is not empty.
            assertFalse(invitationList.isEmpty());

            Invitation invitation0 = invitationList.get(0);
            assertEquals(invitation0.getInvitationId(), INV_02_INVITATION_ID);
            assertEquals(invitation0.getConfirmationCode(), INV_02_CONF_CODE);
            assertEquals(invitation0.getUsername(), INV_02_UN);

            Invitation invitation1 = invitationList.get(1);
            assertEquals(invitation1.getInvitationId(), INV_03_INVITATION_ID);
            assertEquals(invitation1.getConfirmationCode(), INV_03_CONF_CODE);
            assertEquals(invitation1.getUsername(), INV_03_UN);
        }
    }

    @Test(priority = 2)
    public void testIntrospectInvitation() throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {

            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            Invitation invitation = invitationCoreService.introspectInvitation(INV_01_CONF_CODE);
            assertEquals(invitation.getInvitationId(), INV_01_INVITATION_ID);
            assertEquals(invitation.getUsername(), INV_01_UN);
            assertEquals(invitation.getEmail(), INV_01_EMAIL);
            assertEquals(invitation.getStatus(), "PENDING");
        }
    }

    @Test(priority = 3)
    public void testGetInvitationWithFilter() throws Exception {

        String filter = "status eq PENDING";
        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class);
             MockedStatic<IdentityTenantUtil> identityUtil = Mockito.mockStatic(IdentityTenantUtil.class)) {

            identityUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            List<Invitation> invitationList = invitationCoreService.getInvitations(filter);
            // Checking whether the size of the Invitation list is not empty.
            assertFalse(invitationList.isEmpty());

            Invitation invitation0 = invitationList.get(0);
            assertEquals(invitation0.getStatus(), "PENDING");
        }
    }

    @Test(priority = 4)
    public void testDeleteInvitation() throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {

            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection())
                    .thenReturn(getConnection());
            assertTrue(invitationCoreService.deleteInvitation(INV_02_INVITATION_ID));
        }
    }

    @Test(priority = 5, expectedExceptions = UserInvitationMgtClientException.class,
            dataProvider = "getInvitationFilter")
    public void testGetInvitationWithInvalidFilter(String filter) throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {

            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            invitationCoreService.getInvitations(filter);
            fail("Expected: " + UserInvitationMgtClientException.class.getName());
        }
    }

    @Test(priority = 6, expectedExceptions = UserInvitationMgtException.class)
    public void testIntrospectWithInvalidConfirmationCode() throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {

            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            invitationCoreService.introspectInvitation("1234");
            fail("Expected: " + UserInvitationMgtException.class.getName());
        }
    }

    @Test(priority = 7, expectedExceptions = UserInvitationMgtException.class)
    public void testDeleteInvitationWithInvalidInvitationId() throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {

            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            invitationCoreService.deleteInvitation("23446012-18c7-4cd6-a757-16c39dbf4053");
            fail("Expected: " + UserInvitationMgtException.class.getName());
        }
    }

    @Test(priority = 8, expectedExceptions = UserInvitationMgtException.class)
    public void testDeleteInvitationWithNotOwnedInvitationId() throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {

            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            assertTrue(invitationCoreService.deleteInvitation("1234"));
        }
    }

    @Test(priority = 9)
    public void testCreateInvitationWithNonExistingUserInParent() throws Exception {

        InvitationDO invitation1 = new InvitationDO();
        invitation1.setUsernamesList(Collections.singletonList("samson"));
        invitation1.setUserDomain("DEFAULT");
        invitation1.setRoleAssignments(null);
        invitation1.setUserRedirectUrl("https://localhost:8080/travel-manager-001/invitations/accept");

        OrganizationManager organizationManager = mock(OrganizationManager.class);

        when(userStoreManager.isExistingUser("samson")).thenReturn(false);
        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);

        List<String> ancestors = new ArrayList<>();
        ancestors.add("dc828181-e1a8-4f5e-8936-f154f4ae1234");
        ancestors.add("8d94ff8a-031f-4719-8713-c6a9819b23b2");
        when(organizationManager.getAncestorOrganizationIds(anyString())).thenReturn(ancestors);

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class);
             MockedStatic<IdentityTenantUtil> identityUtil = Mockito.mockStatic(IdentityTenantUtil.class)) {

            identityUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            List<InvitationResult> createdInvitation = invitationCoreService.createInvitations(invitation1);
            assertNotNull(createdInvitation);
            assertEquals(createdInvitation.get(0).getStatus(), "Failed");
        }
    }

    @DataProvider(name = "testConfirmationCodeReturnOnInviteCreationDataProvider")
    public Object[][] inviteNotificationManagingData() {

        return new Object[][]{
                {
                        true, "false", "true", true
                },
                {
                        true, "false", "false", true
                },
                {
                        false, null, "false", true
                },
                {
                        true, "true", "false", false
                },
                {
                        true, "true", "true", false
                },
                {
                        false, null, "true", false
                }
        };
    }

    @Test(priority = 10, dataProvider = "testConfirmationCodeReturnOnInviteCreationDataProvider")
    public void testConfirmationCodeReturnOnInviteCreation(boolean setNotificationManagingProperty,
                                                           String propertyValue,
                                                           String isNotificationManagedInternallyForOrg,
                                                           boolean isConfirmationCodeReturnInResponse)
            throws Exception {

        String username = "alex";
        String userStoreDomain = "DEFAULT";
        String userStoreQualifiedUsername = userStoreDomain + "/" + username;
        String userId = "de828181-e1a8-4f5e-8936-f154f4ae1234";
        String subOrgId = "dc828181-e1a8-4f5e-8936-f154f4aefa75";
        String parentOrgId = "8d94ff8a-031f-4719-8713-c6a9819b23b2";
        String tenantDomainOfSubOrg = "subOrg";
        String tenantDomainOfParentOrg = "parentOrg";

        InvitationDO invitation = new InvitationDO();
        invitation.setUsernamesList(Collections.singletonList(username));
        invitation.setUserDomain(userStoreDomain);
        invitation.setRoleAssignments(null);
        invitation.setUserRedirectUrl("https://localhost:8080/travel-manager-001/invitations/accept");
        if (setNotificationManagingProperty) {
            invitation.setInvitationProperties(
                    Collections.singletonMap("manageNotificationsInternally", propertyValue));
        }

        OrganizationManager organizationManager = mock(OrganizationManager.class);

        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);
        List<String> ancestors = new ArrayList<>();
        ancestors.add(subOrgId);
        ancestors.add(parentOrgId);

        when(organizationManager.getAncestorOrganizationIds(anyString())).thenReturn(ancestors);
        when(organizationManager.getParentOrganizationId(subOrgId)).thenReturn(parentOrgId);

        utilsMockedStatic.when(() -> Utils.getConnectorConfig(EMAIL_VERIFICATION_NOTIFICATION_INTERNALLY_MANAGE,
                "carbon.super")).thenReturn(isNotificationManagedInternallyForOrg);

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class);
             MockedStatic<IdentityTenantUtil> identityUtil = Mockito.mockStatic(IdentityTenantUtil.class)) {

            identityUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true))
                    .thenReturn(getConnection());
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(false))
                    .thenReturn(getConnection()).thenReturn(getConnection());

            UserRealm userRealmParentOrg = mock(UserRealm.class);
            AbstractUserStoreManager userStoreManagerParentOrg = mock(AbstractUserStoreManager.class);
            mockParentOrgDetails(userStoreManagerParentOrg, userStoreQualifiedUsername, userId, userRealmParentOrg,
                    realmService, tenantDomainOfParentOrg, organizationManager, parentOrgId, identityDBUtil);
            when(userStoreManagerParentOrg.getSecondaryUserStoreManager(anyString()))
                    .thenReturn(userStoreManagerParentOrg);

            UserRealm userRealmSubOrg = mock(UserRealm.class);
            AbstractUserStoreManager userStoreManagerSubOrg = mock(AbstractUserStoreManager.class);
            mockSubOrgDetails(userStoreManagerSubOrg, userStoreQualifiedUsername, userRealmSubOrg, realmService,
                    tenantDomainOfSubOrg, organizationManager, subOrgId, identityDBUtil);

            when(userStoreManagerParentOrg.getSecondaryUserStoreManager(anyString()))
                    .thenReturn(userStoreManagerParentOrg);

            organizationSharedUserUtilMockedStatic
                    .when(() -> OrganizationSharedUserUtil
                            .getUserManagedOrganizationClaim(userStoreManagerSubOrg, userId))
                    .thenReturn(parentOrgId);

            List<InvitationResult> createdInvitation = invitationCoreService.createInvitations(invitation);
            assertNotNull(createdInvitation);
            assertEquals(createdInvitation.get(0).getStatus(), "Successful");

            // Clean the stored invitation.
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(false))
                    .thenReturn(getConnection()).thenReturn(getConnection());
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(true))
                    .thenReturn(getConnection());
            Invitation storedInvitation;
            if (isConfirmationCodeReturnInResponse) {
                assertNotNull(createdInvitation.get(0).getConfirmationCode());
                storedInvitation = userInvitationDAO
                        .getInvitationWithAssignmentsByConfirmationCode(createdInvitation.get(0).getConfirmationCode());
            } else {
                String invitedUsername = createdInvitation.get(0).getUsername();
                storedInvitation = userInvitationDAO.getActiveInvitationByUser(invitedUsername, "DEFAULT",
                        parentOrgId, subOrgId);
            }
            userInvitationDAO.deleteInvitation(storedInvitation.getInvitationId());
        }
    }



    @Test(priority = 13, expectedExceptions = UserInvitationMgtClientException.class,
            expectedExceptionsMessageRegExp = ".*Invalid user store domain specified in the invitation.*")
    public void testCreateInvitationWithInvalidUserStoreDomain() throws Exception {

        InvitationDO invitation1 = new InvitationDO();
        invitation1.setUserDomain("INVALID");
        invitation1.setUserRedirectUrl("https://localhost:8080/travel-manager-001/invitations/accept");

        OrganizationManager organizationManager = mock(OrganizationManager.class);
        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);
        when(userStoreManager.getSecondaryUserStoreManager(invitation1.getUserDomain())).thenReturn(null);

        try (MockedStatic<IdentityTenantUtil> identityUtil = Mockito.mockStatic(IdentityTenantUtil.class)) {

            identityUtil.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
            invitationCoreService.createInvitations(invitation1);
        }
    }

    private void mockParentOrgDetails(AbstractUserStoreManager userStoreManagerParentOrg,
                                             String userStoreQualifiedUsername,
                                             String userId, UserRealm userRealmParentOrg, RealmService realmService,
                                             String tenantDomainOfParentOrg, OrganizationManager organizationManager,
                                             String parentOrgId, MockedStatic<IdentityDatabaseUtil> identityDBUtil)
            throws UserStoreException, OrganizationManagementException {

        when(userStoreManagerParentOrg.isExistingUser(userStoreQualifiedUsername)).thenReturn(true);
        when(userStoreManagerParentOrg.getUserIDFromUserName(userStoreQualifiedUsername)).thenReturn(userId);
        when(userStoreManagerParentOrg.getUserClaimValue(userStoreQualifiedUsername, CLAIM_EMAIL_ADDRESS,
                null)).thenReturn(
                "alex@gmail.com");
        when(userRealmParentOrg.getUserStoreManager()).thenReturn(userStoreManagerParentOrg);
        when(realmService.getTenantUserRealm(1)).thenReturn(userRealmParentOrg);
        identityDBUtil.when(() -> IdentityTenantUtil.getTenantId(tenantDomainOfParentOrg)).thenReturn(1);
        identityDBUtil.when(() -> IdentityTenantUtil.getTenantDomain(1)).thenReturn(tenantDomainOfParentOrg);
        when(organizationManager.resolveTenantDomain(parentOrgId)).thenReturn(tenantDomainOfParentOrg);
    }

    private void mockSubOrgDetails(AbstractUserStoreManager userStoreManagerSubOrg,
                                          String userStoreQualifiedUsername,
                                          UserRealm userRealmSubOrg, RealmService realmService,
                                          String tenantDomainOfSubOrg,
                                          OrganizationManager organizationManager, String subOrgId,
                                   MockedStatic<IdentityDatabaseUtil> identityDBUtil)
            throws UserStoreException, OrganizationManagementException {

        when(userStoreManagerSubOrg.isExistingUser(userStoreQualifiedUsername)).thenReturn(false);
        when(userRealmSubOrg.getUserStoreManager()).thenReturn(userStoreManagerSubOrg);
        when(realmService.getTenantUserRealm(2)).thenReturn(userRealmSubOrg);
        identityDBUtil.when(() -> IdentityTenantUtil.getTenantId(tenantDomainOfSubOrg)).thenReturn(2);
        identityDBUtil.when(() -> IdentityTenantUtil.getTenantDomain(2)).thenReturn(tenantDomainOfSubOrg);
        when(organizationManager.resolveTenantDomain(subOrgId)).thenReturn(tenantDomainOfSubOrg);
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());
    }

    private void storeParentUserInvitation(Invitation invitation) throws Exception {

        try (MockedStatic<IdentityDatabaseUtil> identityDBUtil = Mockito.mockStatic(IdentityDatabaseUtil.class)) {
            identityDBUtil.when(() -> IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
            when(IdentityUtil.getProperty(anyString())).thenReturn("1440");
            if (invitation.getRoleAssignments() != null) {
                for (RoleAssignments roleAssignments : invitation.getRoleAssignments()) {
                    for (String role : roleList) {
                        roleAssignments.setRole(role);
                    }
                }
            }
            userInvitationDAO.createInvitation(invitation);
        }
    }

    private void mockCarbonContextForTenant() {

        mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext privilegedCarbonContext = mock(PrivilegedCarbonContext.class);
        when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        when(PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId()).
                thenReturn("dc828181-e1a8-4f5e-8936-f154f4aefa75");
    }

    private Invitation buildInvitation(String invitationId, String confirmationCode, String username,
                                       String userDomain, String email, String userRedirectUrl, String userOrgId,
                                       String invitedOrgId, RoleAssignments[] roleAssignments,
                                       GroupAssignments[] groupAssignments, String status) {

        Invitation invitation = new Invitation();
        invitation.setInvitationId(invitationId);
        invitation.setConfirmationCode(confirmationCode);
        invitation.setUsername(username);
        invitation.setUserDomain(userDomain);
        invitation.setEmail(email);
        invitation.setUserRedirectUrl(userRedirectUrl);
        if (roleAssignments != null) {
            invitation.setRoleAssignments(roleAssignments);
        }
        if (groupAssignments != null) {
            invitation.setGroupAssignments(groupAssignments);
        }
        invitation.setInvitedOrganizationId(invitedOrgId);
        invitation.setUserOrganizationId(userOrgId);
        invitation.setStatus(status);
        return invitation;
    }

    private RoleAssignments buildRoleAssignments(String[] roles) {

        RoleAssignments roleAssignments = new RoleAssignments();
        roleAssignments.setRoles(roles);
        return roleAssignments;
    }

    private GroupAssignments buildGroupAssignments(String[] groups) {

        GroupAssignments groupAssignments = new GroupAssignments();
        for (String group : groups) {
            groupAssignments.setGroupId(group);
        }
        return groupAssignments;
    }
}
