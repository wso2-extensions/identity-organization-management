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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAO;
import org.wso2.carbon.identity.organization.user.invitation.management.dao.UserInvitationDAOImpl;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtClientException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.internal.UserInvitationMgtDataHolder;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;
import org.wso2.carbon.identity.organization.user.invitation.management.models.RoleAssignments;
import org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
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
import static org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils.closeH2Base;
import static org.wso2.carbon.identity.organization.user.invitation.management.util.TestUtils.getConnection;

@PrepareForTest({PrivilegedCarbonContext.class,
        IdentityDatabaseUtil.class,
        UserInvitationMgtDataHolder.class,
        IdentityTenantUtil.class,
        UserInvitationMgtDataHolder.class,
        IdentityUtil.class})
public class InvitationCoreServiceImplTest extends PowerMockTestCase {

    private final UserInvitationDAO userInvitationDAO = new UserInvitationDAOImpl();
    private InvitationCoreServiceImpl invitationCoreService;

    @BeforeClass
    public void setUp() throws Exception {

        invitationCoreService = new InvitationCoreServiceImpl();
        TestUtils.initiateH2Base();
        setUpCarbonHome();
        mockCarbonContextForTenant();
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityDatabaseUtil.class);
        mockStatic(IdentityUtil.class);

        Connection connection1 = getConnection();
        Connection connection2 = getConnection();
        Connection connection3 = getConnection();

        Invitation invitation1 = buildInvitation(INV_01_INVITATION_ID, INV_01_CONF_CODE, INV_01_UN, "DEFAULT",
                INV_01_EMAIL, "https://localhost:8080/travel-manager-001/invitations/accept", INV_01_USER_ORG_ID,
                INV_01_INV_ORG_ID, null, "PENDING");
        Invitation invitation2 = buildInvitation(INV_02_INVITATION_ID, INV_02_CONF_CODE, INV_02_UN, "DEFAULT",
                INV_02_EMAIL, "https://localhost:8080/travel-manager-001/invitations/accept",
                INV_02_USER_ORG_ID, INV_02_INV_ORG_ID, null, "PENDING");

        RoleAssignments roleAssignments2 = buildRoleAssignments("1e174bbd-19fa-4449-b8e7-5fabe6f3dab7",
                new String[]{"1224", "12345"});
        Invitation invitation3 = buildInvitation(INV_03_INVITATION_ID, INV_03_CONF_CODE, INV_03_UN,
                "DEFAULT", INV_03_EMAIL, "https://localhost:8080/travel-manager-001/invitations/accept",
                INV_03_USER_ORG_ID, INV_03_INV_ORG_ID, new RoleAssignments[]{roleAssignments2}, "PENDING");

        populateH2Base(connection1, invitation1);
        populateH2Base(connection2, invitation2);
        populateH2Base(connection3, invitation3);
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

    @Test(priority = 9, expectedExceptions = UserInvitationMgtClientException.class)
    public void testCreateInvitationWithNonExistingUserInParent() throws Exception {

        Invitation invitation1 = buildInvitation(null,
                null, "samson", "DEFAULT",
                null, "https://localhost:8080/travel-manager-001/invitations/accept",
                null, null,
                null, null);

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
        invitationCoreService.createInvitation(invitation1);
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
                                       String invitedOrgId, RoleAssignments[] roleAssignments, String status) {

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
        invitation.setInvitedOrganizationId(invitedOrgId);
        invitation.setUserOrganizationId(userOrgId);
        invitation.setStatus(status);
        return invitation;
    }

    private RoleAssignments buildRoleAssignments(String applicationId, String[] roles) {

        RoleAssignments roleAssignments = new RoleAssignments();
        roleAssignments.setApplicationId(applicationId);
        roleAssignments.setRoles(roles);
        return roleAssignments;
    }
}
