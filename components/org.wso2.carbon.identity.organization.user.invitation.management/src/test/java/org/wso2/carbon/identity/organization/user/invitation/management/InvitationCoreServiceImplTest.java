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

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
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
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.Group;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.CLAIM_EMAIL_ADDRESS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.APPLICATION_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.CONSOLE_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.INTERNAL_DOMAIN;
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
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.PRIMARY_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.TENANT_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.constants.InvitationTestConstants.USER_ID;
import static org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils.closeH2Base;
import static org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils.getConnection;

@PrepareForTest({PrivilegedCarbonContext.class,
        RoleManagementService.class,
        IdentityDatabaseUtil.class,
        UserInvitationMgtDataHolder.class,
        IdentityTenantUtil.class,
        UserInvitationMgtDataHolder.class,
        IdentityUtil.class,
        OrganizationSharedUserUtil.class,
        UserCoreUtil.class,
        InvitationCoreServiceImpl.class})
public class InvitationCoreServiceImplTest extends PowerMockTestCase {

    private final UserInvitationDAO userInvitationDAO = new UserInvitationDAOImpl();
    private InvitationCoreServiceImpl invitationCoreService;
    private final String [] roleList = {"1224", "12345"};

    private final String [] groupList = {"4321", "54321"};
    @BeforeClass
    public void setUp() throws Exception {

        invitationCoreService = new InvitationCoreServiceImpl();
        TestUtils.initiateH2Base();
        setUpCarbonHome();
        mockCarbonContextForTenant();
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityDatabaseUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(OrganizationSharedUserUtil.class);

        Connection connection1 = getConnection();
        Connection connection2 = getConnection();
        Connection connection3 = getConnection();
        Connection connection4 = getConnection();

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

        populateH2Base(connection1, invitation1);
        populateH2Base(connection2, invitation2);
        populateH2Base(connection3, invitation3);
        populateH2Base(connection4, invitation4);
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

        when(IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
        RoleManagementService roleManagementService = mock(RoleManagementService.class);
        UserInvitationMgtDataHolder.getInstance().setRoleManagementService(roleManagementService);
        when(roleManagementService.getRoleWithoutUsers(anyString(), anyString())).thenReturn(buildRoleInfo());
        OrganizationManager organizationManager = mock(OrganizationManager.class);
        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("carbon.super");

        RealmService realmService = mock(RealmService.class);
        UserInvitationMgtDataHolder.getInstance().setRealmService(realmService);
        UserRealm userRealm = mock(UserRealm.class);
        AbstractUserStoreManager userStoreManager = mock(AbstractUserStoreManager.class);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);

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

    @Test(priority = 2)
    public void testIntrospectInvitation() throws Exception {

        when(IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
        Invitation invitation = invitationCoreService.introspectInvitation(INV_01_CONF_CODE);
        assertEquals(invitation.getInvitationId(), INV_01_INVITATION_ID);
        assertEquals(invitation.getUsername(), INV_01_UN);
        assertEquals(invitation.getEmail(), INV_01_EMAIL);
        assertEquals(invitation.getStatus(), "PENDING");
    }

    @Test(priority = 3)
    public void testGetInvitationWithFilter() throws Exception {

        String filter = "status eq PENDING";
        when(IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
        List<Invitation> invitationList = invitationCoreService.getInvitations(filter);
        // Checking whether the size of the Invitation list is not empty.
        assertFalse(invitationList.isEmpty());

        Invitation invitation0 = invitationList.get(0);
        assertEquals(invitation0.getStatus(), "PENDING");
    }

    @Test(priority = 4)
    public void testDeleteInvitation() throws Exception {

        Connection connection = getConnection();
        Connection connection1 = getConnection();
        when(IdentityDatabaseUtil.getDBConnection(false)).thenReturn(connection);
        when(IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection1);
        assertTrue(invitationCoreService.deleteInvitation(INV_02_INVITATION_ID));
    }

    @Test(priority = 5, expectedExceptions = UserInvitationMgtClientException.class,
            dataProvider = "getInvitationFilter")
    public void testGetInvitationWithInvalidFilter(String filter) throws Exception {

        when(IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
        invitationCoreService.getInvitations(filter);
        fail("Expected: " + UserInvitationMgtClientException.class.getName());
    }

    @Test(priority = 6, expectedExceptions = UserInvitationMgtException.class)
    public void testIntrospectWithInvalidConfirmationCode() throws Exception {

        when(IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(getConnection());
        invitationCoreService.introspectInvitation("1234");
        fail("Expected: " + UserInvitationMgtException.class.getName());
    }

    @Test(priority = 7, expectedExceptions = UserInvitationMgtException.class)
    public void testDeleteInvitationWithInvalidInvitationId() throws Exception {

        Connection connection = getConnection();
        Connection connection1 = getConnection();
        when(IdentityDatabaseUtil.getDBConnection(false)).thenReturn(connection);
        when(IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection1);
        invitationCoreService.deleteInvitation("23446012-18c7-4cd6-a757-16c39dbf4053");
        fail("Expected: " + UserInvitationMgtException.class.getName());
    }

    @Test(priority = 8, expectedExceptions = UserInvitationMgtException.class)
    public void testDeleteInvitationWithNotOwnedInvitationId() throws Exception {

        Connection connection = getConnection();
        Connection connection1 = getConnection();
        when(IdentityDatabaseUtil.getDBConnection(false)).thenReturn(connection);
        when(IdentityDatabaseUtil.getDBConnection(true)).thenReturn(connection1);
        assertTrue(invitationCoreService.deleteInvitation("1234"));
    }
    @Test(priority = 9)
    public void testCreateInvitationWithNonExistingUserInParent() throws Exception {

        InvitationDO invitation1 = new InvitationDO();
        invitation1.setUsernamesList(Collections.singletonList("samson"));
        invitation1.setUserDomain("DEFAULT");
        invitation1.setRoleAssignments(null);
        invitation1.setUserRedirectUrl("https://localhost:8080/travel-manager-001/invitations/accept");

        OrganizationManager organizationManager = mock(OrganizationManager.class);
        RealmService realmService = mock(RealmService.class);
        UserRealm userRealm = mock(UserRealm.class);
        AbstractUserStoreManager userStoreManager = mock(AbstractUserStoreManager.class);

        when(userStoreManager.isExistingUser("samson")).thenReturn(false);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);
        UserInvitationMgtDataHolder.getInstance().setRealmService(realmService);
        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);

        List<String> ancestors = new ArrayList<>();
        ancestors.add("dc828181-e1a8-4f5e-8936-f154f4ae1234");
        ancestors.add("8d94ff8a-031f-4719-8713-c6a9819b23b2");
        when(organizationManager.getAncestorOrganizationIds(anyString())).thenReturn(ancestors);

        when(IdentityDatabaseUtil.getDBConnection(false)).thenReturn(getConnection());
        when(IdentityDatabaseUtil.getDBConnection(true)).thenReturn(getConnection());
        when(IdentityTenantUtil.getTenantId(anyString())).thenReturn(-1234);
        mockIdentityTenantUtil();
        List<InvitationResult> createdInvitation = invitationCoreService.createInvitations(invitation1);
        assertNotNull(createdInvitation);
        assertEquals(createdInvitation.get(0).getStatus(), "Failed");
    }

    @DataProvider(name = "testConfirmationCodeReturnOnInviteCreationDataProvider")
    public Object[][] inviteNotificationManagingData() {

        return new Object[][]{
                {
                        true, "false", true, true
                },
                {
                        true, "false", false, true
                },
                {
                        true, "true", true, false
                },
                {
                        true, "true", false, false
                },
                {
                        false, null, false, true
                },
                {
                        false, null, true, false
                }
        };
    }

    @Test(priority = 10, dataProvider = "testConfirmationCodeReturnOnInviteCreationDataProvider")
    public void testConfirmationCodeReturnOnInviteCreation(boolean setNotificationManagingProperty,
                                                           String propertyValue,
                                                           boolean isNotificationManagedInternallyForOrg,
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
        RealmService realmService = mock(RealmService.class);

        UserRealm userRealmSubOrg = mock(UserRealm.class);
        AbstractUserStoreManager userStoreManagerSubOrg = mock(AbstractUserStoreManager.class);
        mockSubOrgDetails(userStoreManagerSubOrg, userStoreQualifiedUsername, userRealmSubOrg, realmService,
                tenantDomainOfSubOrg, organizationManager, subOrgId);

        UserRealm userRealmParentOrg = mock(UserRealm.class);
        AbstractUserStoreManager userStoreManagerParentOrg = mock(AbstractUserStoreManager.class);
        mockParentOrgDetails(userStoreManagerParentOrg, userStoreQualifiedUsername, userId, userRealmParentOrg,
                realmService, tenantDomainOfParentOrg, organizationManager, parentOrgId);

        OrganizationSharedUserUtil organizationSharedUserUtil = mock(OrganizationSharedUserUtil.class);
        when(organizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManagerSubOrg, userId)).thenReturn(
                parentOrgId);

        UserInvitationMgtDataHolder.getInstance().setRealmService(realmService);
        UserInvitationMgtDataHolder.getInstance().setOrganizationManagerService(organizationManager);
        List<String> ancestors = new ArrayList<>();
        ancestors.add(subOrgId);
        ancestors.add(parentOrgId);

        when(organizationManager.getAncestorOrganizationIds(anyString())).thenReturn(ancestors);
        when(organizationManager.getParentOrganizationId(subOrgId)).thenReturn(parentOrgId);

        stub(method(InvitationCoreServiceImpl.class, "isActiveInvitationAvailable",
                String.class, String.class, String.class, String.class)).toReturn(false);
        stub(method(InvitationCoreServiceImpl.class, "isUserExistAtInvitedOrganization",
                String.class)).toReturn(false);
        stub(method(InvitationCoreServiceImpl.class, "triggerInvitationAddNotification", Invitation.class))
                .toReturn(true);
        stub(method(InvitationCoreServiceImpl.class, "isNotificationsInternallyManagedForOrganization",
                String.class)).toReturn(isNotificationManagedInternallyForOrg);

        when(IdentityDatabaseUtil.getDBConnection(true)).thenReturn(getConnection());
        when(IdentityDatabaseUtil.getDBConnection(false)).thenReturn(getConnection());

        List<InvitationResult> createdInvitation = invitationCoreService.createInvitations(invitation);
        assertNotNull(createdInvitation);
        assertEquals(createdInvitation.get(0).getStatus(), "Successful");
        if (isConfirmationCodeReturnInResponse) {
            assertNotNull(createdInvitation.get(0).getConfirmationCode());
        }
    }

    private static void mockParentOrgDetails(AbstractUserStoreManager userStoreManagerParentOrg,
                                             String userStoreQualifiedUsername,
                                             String userId, UserRealm userRealmParentOrg, RealmService realmService,
                                             String tenantDomainOfParentOrg, OrganizationManager organizationManager,
                                             String parentOrgId)
            throws UserStoreException, OrganizationManagementException {

        when(userStoreManagerParentOrg.isExistingUser(userStoreQualifiedUsername)).thenReturn(true);
        when(userStoreManagerParentOrg.getUserIDFromUserName(userStoreQualifiedUsername)).thenReturn(userId);
        when(userStoreManagerParentOrg.getUserClaimValue(userStoreQualifiedUsername, CLAIM_EMAIL_ADDRESS,
                null)).thenReturn(
                "alex@gmail.com");
        when(userRealmParentOrg.getUserStoreManager()).thenReturn(userStoreManagerParentOrg);
        when(realmService.getTenantUserRealm(1)).thenReturn(userRealmParentOrg);
        when(IdentityTenantUtil.getTenantId(tenantDomainOfParentOrg)).thenReturn(1);
        when(IdentityTenantUtil.getTenantDomain(1)).thenReturn(tenantDomainOfParentOrg);
        when(organizationManager.resolveTenantDomain(parentOrgId)).thenReturn(tenantDomainOfParentOrg);
    }

    private static void mockSubOrgDetails(AbstractUserStoreManager userStoreManagerSubOrg,
                                          String userStoreQualifiedUsername,
                                          UserRealm userRealmSubOrg, RealmService realmService,
                                          String tenantDomainOfSubOrg,
                                          OrganizationManager organizationManager, String subOrgId)
            throws UserStoreException, OrganizationManagementException {

        when(userStoreManagerSubOrg.isExistingUser(userStoreQualifiedUsername)).thenReturn(false);
        when(userRealmSubOrg.getUserStoreManager()).thenReturn(userStoreManagerSubOrg);
        when(realmService.getTenantUserRealm(2)).thenReturn(userRealmSubOrg);
        when(IdentityTenantUtil.getTenantId(tenantDomainOfSubOrg)).thenReturn(2);
        when(IdentityTenantUtil.getTenantDomain(2)).thenReturn(tenantDomainOfSubOrg);
        when(organizationManager.resolveTenantDomain(subOrgId)).thenReturn(tenantDomainOfSubOrg);
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome,
                "repository/conf").toString());
    }

    private static void mockIdentityTenantUtil() {

        when(IdentityTenantUtil.getTenantDomain(anyInt())).thenReturn("carbon.super");
    }

    private void populateH2Base(Connection connection, Invitation invitation) throws Exception {

        when(IdentityDatabaseUtil.getDBConnection(anyBoolean())).thenReturn(connection);
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

    @Test(priority = 11)
    public void testGetUserGroups() throws Exception {

        // Mocking IdentityTenantUtil.getTenantId static method
        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);

        // Mocking the getAbstractUserStoreManager method
        AbstractUserStoreManager userStoreManager = mock(AbstractUserStoreManager.class);
        stub(method(InvitationCoreServiceImpl.class, "getAbstractUserStoreManager", int.class))
                .toReturn(userStoreManager);

        // Mocking the userStoreManager.getGroupListOfUser method
        Group group1 = mock(Group.class);
        Group group2 = mock(Group.class);
        Group group3 = mock(Group.class);
        when(group1.getGroupName()).thenReturn("Application/group1");
        when(group2.getGroupName()).thenReturn("Internal/group2");
        when(group3.getGroupName()).thenReturn("Primary/group3");
        when(group1.getGroupID()).thenReturn("1");
        when(group2.getGroupID()).thenReturn("2");
        when(group3.getGroupID()).thenReturn("3");
        List<Group> groups = Arrays.asList(group1, group2, group3);
        when(userStoreManager.getGroupListOfUser(USER_ID, null, null)).thenReturn(groups);

        // Mocking UserCoreUtil.extractDomainFromName
        mockStatic(UserCoreUtil.class);
        when(UserCoreUtil.extractDomainFromName("Application/group1")).thenReturn(APPLICATION_DOMAIN);
        when(UserCoreUtil.extractDomainFromName("Internal/group2")).thenReturn(INTERNAL_DOMAIN);
        when(UserCoreUtil.extractDomainFromName("Primary/group3")).thenReturn(PRIMARY_DOMAIN);

        // Invoking the method
        List<String> resultUserGroups = Whitebox.invokeMethod(
                invitationCoreService, "getUserGroups", USER_ID, TENANT_DOMAIN);

        // Assertion
        List<String> expectedUserGroups = Collections.singletonList("3");
        assertEquals(resultUserGroups, expectedUserGroups);
    }

    @Test(priority = 12)
    public void testIsInvitedUserHasConsoleAccess() throws Exception {

        // Mocking Role List
        RoleBasicInfo role1 = new RoleBasicInfo("1", "Role1");
        RoleBasicInfo role2 = new RoleBasicInfo("2", "Role2");
        RoleBasicInfo role3 = new RoleBasicInfo("3", "Role3");
        RoleBasicInfo role4 = new RoleBasicInfo("4", "Role4");
        role2.setAudienceName(CONSOLE_DOMAIN);
        List<RoleBasicInfo> roleList = new ArrayList<>(Arrays.asList(role1, role2));

        // Mocking Group List
        List<String> groupList = Arrays.asList("groupID1", "groupID2");

        // Mocking RoleManagementService getRoleListOfUser
        RoleManagementService roleManagementService = mock(RoleManagementService.class);
        stub(method(InvitationCoreServiceImpl.class, "getRoleManagementService"))
                .toReturn(roleManagementService);
        when(roleManagementService.getRoleListOfUser(USER_ID, TENANT_DOMAIN)).thenReturn(roleList);

        // Stubbing getUserGroups Method
        stub(method(InvitationCoreServiceImpl.class, "getUserGroups")).toReturn(groupList);

        // Mocking RoleManagementService getRoleListOfGroups
        List<RoleBasicInfo> roleListFromUserGroups = Arrays.asList(role3, role4);
        when(roleManagementService.getRoleListOfGroups(groupList, TENANT_DOMAIN)).thenReturn(roleListFromUserGroups);

        // Invoke the method under test
        boolean resultWithDirectConsoleAccess = Whitebox.invokeMethod(invitationCoreService,
                        "isInvitedUserHasConsoleAccess", USER_ID, TENANT_DOMAIN);

        // Assertion
        assertTrue(resultWithDirectConsoleAccess);

        // Test case where user does inherit have console access via a group role
        role2.setAudienceName(APPLICATION_DOMAIN);
        role4.setAudienceName(CONSOLE_DOMAIN);

        // Re-invoke the method under test
        Boolean resultWithConsoleAccessViaGroup = Whitebox.invokeMethod(invitationCoreService,
                "isInvitedUserHasConsoleAccess", USER_ID, TENANT_DOMAIN);

        // Assertion
        assertTrue(resultWithConsoleAccessViaGroup);

        // Test case where user does not have console access
        role4.setAudienceName(APPLICATION_DOMAIN);

        // Re-invoke the method under test
        Boolean resultWithoutConsoleAccess = Whitebox.invokeMethod(invitationCoreService,
                "isInvitedUserHasConsoleAccess", USER_ID, TENANT_DOMAIN);

        // Assertion
        assertFalse(resultWithoutConsoleAccess);
    }
}
