/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.executor;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.model.ExecutorDTO;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
import org.wso2.carbon.identity.organization.management.executor.internal.OrganizationManagementExecutorDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;

public class OrganizationRoleAssignmentExecutorTest {

    private static final String PARENT_TENANT = "parent-org";
    private static final String CHILD_TENANT = "child-org";
    private static final String CHILD_ORG_ID = "child-org-id";
    private static final String USER_ID = "registered-user-id";
    private static final String SHARED_USER_ID = "shared-user-id";

    private OrganizationRoleAssignmentExecutor executor;
    private RoleManagementService roleManagementService;
    private OrganizationManager organizationManager;
    private OrganizationUserSharingService userSharingService;
    private MockedStatic<PrivilegedCarbonContext> carbonContext;

    @BeforeMethod
    public void setUp() throws Exception {

        System.setProperty(CarbonBaseConstants.CARBON_HOME,
                Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString());
        executor = new OrganizationRoleAssignmentExecutor();
        roleManagementService = mock(RoleManagementService.class);
        organizationManager = mock(OrganizationManager.class);
        userSharingService = mock(OrganizationUserSharingService.class);
        OrganizationManagementExecutorDataHolder dataHolder = OrganizationManagementExecutorDataHolder.getInstance();
        dataHolder.setRoleManagementService(roleManagementService);
        dataHolder.setOrganizationManager(organizationManager);
        dataHolder.setOrganizationUserSharingService(userSharingService);
        carbonContext = mockStatic(PrivilegedCarbonContext.class);
        carbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                .thenReturn(mock(PrivilegedCarbonContext.class));
        when(roleManagementService.getRoleBasicInfoById(anyString(), anyString()))
                .thenReturn(mock(RoleBasicInfo.class));
        when(roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(anyList(), anyString()))
                .thenReturn(Collections.emptyMap());
        when(roleManagementService.getRoleIdListOfUser(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(organizationManager.resolveOrganizationId(CHILD_TENANT)).thenReturn(CHILD_ORG_ID);
    }

    @AfterMethod
    public void tearDown() {

        carbonContext.close();
        OrganizationManagementExecutorDataHolder dataHolder = OrganizationManagementExecutorDataHolder.getInstance();
        dataHolder.setRoleManagementService(null);
        dataHolder.setOrganizationManager(null);
        dataHolder.setOrganizationUserSharingService(null);
    }

    @Test
    public void testExecutorContract() {

        Assert.assertEquals(executor.getName(), "OrganizationRoleAssignmentExecutor");
        Assert.assertTrue(executor.getInitiationData().isEmpty());
        Assert.assertNull(executor.rollback(new FlowExecutionContext()));
    }

    @Test
    public void testMultipleRolesAssignedToChildResidentUser() throws Exception {

        FlowExecutionContext context = context("[\"role-1\",\"role-2\"]", true);
        shareRole("role-1", "child-role-1");
        shareRole("role-2", "child-role-2");

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        verifyAssignment("child-role-1", USER_ID);
        verifyAssignment("child-role-2", USER_ID);
        verifyNoInteractions(userSharingService);
        Assert.assertEquals(context.getTenantDomain(), PARENT_TENANT);
        carbonContext.verify(PrivilegedCarbonContext::endTenantFlow);
    }

    @Test
    public void testParentResidentUserUsesSharedChildUserId() throws Exception {

        FlowExecutionContext context = context("[\"role-1\"]", false);
        UserAssociation association = new UserAssociation();
        association.setUserId(SHARED_USER_ID);
        when(userSharingService.getUserAssociationOfAssociatedUserByOrgId(USER_ID, CHILD_ORG_ID))
                .thenReturn(association);
        shareRole("role-1", "child-role-1");

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        verifyAssignment("child-role-1", SHARED_USER_ID);
        verify(roleManagementService, never()).updateUserListOfRole("child-role-1",
                Collections.singletonList(USER_ID), Collections.emptyList(), CHILD_TENANT);
    }

    @Test
    public void testParentInheritedRoleResolvesThroughMainRole() throws Exception {

        FlowExecutionContext context = context("[\"parent-shared-role\"]", true);
        when(roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(
                Collections.singletonList("parent-shared-role"), PARENT_TENANT))
                .thenReturn(Collections.singletonMap("parent-shared-role", "main-role"));
        shareRole("main-role", "child-role");

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        verifyAssignment("child-role", USER_ID);
    }

    @Test
    public void testUnavailableSharedRoleDoesNotPreventOtherAssignments() throws Exception {

        FlowExecutionContext context = context("[\"unshared-role\",\"role-2\"]", true);
        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList("unshared-role"), CHILD_TENANT)).thenReturn(Collections.emptyMap());
        shareRole("role-2", "child-role-2");

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        verifyAssignment("child-role-2", USER_ID);
        verify(roleManagementService, never()).addRole(anyString(), anyList(), anyList(), anyList(), anyString(),
                anyString(), anyString());
    }

    @Test
    public void testMissingParentRoleIsSkipped() throws Exception {

        FlowExecutionContext context = context("[\"deleted-role\",\"role-2\"]", true);
        when(roleManagementService.getRoleBasicInfoById("deleted-role", PARENT_TENANT))
                .thenThrow(mock(IdentityRoleManagementException.class));
        shareRole("role-2", "child-role-2");

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        verifyAssignment("child-role-2", USER_ID);
        verify(roleManagementService, never()).getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList("deleted-role"), CHILD_TENANT);
    }

    @Test
    public void testAssignmentFailureDoesNotPreventRemainingRoles() throws Exception {

        FlowExecutionContext context = context("[\"role-1\",\"role-2\"]", true);
        shareRole("role-1", "child-role-1");
        shareRole("role-2", "child-role-2");
        when(roleManagementService.updateUserListOfRole("child-role-1", Collections.singletonList(USER_ID),
                Collections.emptyList(), CHILD_TENANT)).thenThrow(mock(IdentityRoleManagementException.class));

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        verifyAssignment("child-role-2", USER_ID);
        carbonContext.verify(PrivilegedCarbonContext::endTenantFlow);
    }

    @Test
    public void testRuntimeFailureDoesNotPreventRemainingRoles() throws Exception {

        FlowExecutionContext context = context("[\"role-1\",\"role-2\"]", true);
        shareRole("role-1", "child-role-1");
        shareRole("role-2", "child-role-2");
        when(roleManagementService.updateUserListOfRole("child-role-1", Collections.singletonList(USER_ID),
                Collections.emptyList(), CHILD_TENANT)).thenThrow(new IllegalStateException("Unavailable"));

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);
        verifyAssignment("child-role-2", USER_ID);
    }

    @Test
    public void testDuplicateRolesAndExistingMembershipsAreNotAddedAgain() throws Exception {

        FlowExecutionContext context = context("[\"role-1\",\"role-1\",\"role-2\"]", true);
        shareRole("role-1", "child-role-1");
        shareRole("role-2", "child-role-2");
        when(roleManagementService.getRoleIdListOfUser(USER_ID, CHILD_TENANT))
                .thenReturn(Arrays.asList("Internal/selfsignup", "child-role-1"));

        executor.execute(context);

        verify(roleManagementService, never()).updateUserListOfRole("child-role-1", Collections.singletonList(USER_ID),
                Collections.emptyList(), CHILD_TENANT);
        verify(roleManagementService, times(1)).updateUserListOfRole("child-role-2", Collections.singletonList(USER_ID),
                Collections.emptyList(), CHILD_TENANT);
    }

    @Test
    public void testRetrySkipsMembershipAlreadyAssigned() throws Exception {

        FlowExecutionContext context = context("[\"role-1\"]", true);
        shareRole("role-1", "child-role-1");
        when(roleManagementService.getRoleIdListOfUser(USER_ID, CHILD_TENANT))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList("child-role-1"));

        executor.execute(context);
        executor.execute(context);

        verify(roleManagementService, times(1)).updateUserListOfRole("child-role-1", Collections.singletonList(USER_ID),
                Collections.emptyList(), CHILD_TENANT);
    }

    @Test(dataProvider = "invalidMetadata")
    public void testInvalidOrAbsentMetadataDoesNotFailProvisioning(String metadata) {

        Assert.assertEquals(executor.execute(context(metadata, true)).getResult(), STATUS_COMPLETE);
        verifyNoInteractions(roleManagementService, userSharingService, organizationManager);
    }

    @DataProvider(name = "invalidMetadata")
    public Object[][] invalidMetadata() {

        return new Object[][]{{null}, {""}, {"[]"}, {"["}, {"{}"}, {"\"role-1\""}, {"[null,1,\"\"]"}};
    }

    @Test
    public void testInvalidEntriesDoNotDiscardValidRoleIds() throws Exception {

        FlowExecutionContext context = context("[null,1,\"role-1\"]", true);
        shareRole("role-1", "child-role-1");

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);
        verifyAssignment("child-role-1", USER_ID);
    }

    @Test
    public void testMissingSharedUserDoesNotFailProvisioning() throws Exception {

        Assert.assertEquals(executor.execute(context("[\"role-1\"]", false)).getResult(), STATUS_COMPLETE);
        verify(roleManagementService, never()).updateUserListOfRole(anyString(), anyList(), anyList(), anyString());
    }

    @Test
    public void testUnavailableRoleServiceDoesNotFailProvisioning() {

        OrganizationManagementExecutorDataHolder.getInstance().setRoleManagementService(null);

        Assert.assertEquals(executor.execute(context("[\"role-1\"]", true)).getResult(), STATUS_COMPLETE);
        verifyNoInteractions(roleManagementService);
    }

    @Test
    public void testUnavailableSharingServiceDoesNotFailProvisioning() {

        OrganizationManagementExecutorDataHolder.getInstance().setOrganizationUserSharingService(null);

        Assert.assertEquals(executor.execute(context("[\"role-1\"]", false)).getResult(), STATUS_COMPLETE);
        verifyNoInteractions(roleManagementService);
    }

    @Test
    public void testUserResolutionFailureDoesNotFailProvisioning() throws Exception {

        when(userSharingService.getUserAssociationOfAssociatedUserByOrgId(USER_ID, CHILD_ORG_ID))
                .thenThrow(mock(OrganizationManagementException.class));

        Assert.assertEquals(executor.execute(context("[\"role-1\"]", false)).getResult(), STATUS_COMPLETE);
    }

    @Test
    public void testRoleLookupFailureRestoresTenant() throws Exception {

        when(roleManagementService.getRoleIdListOfUser(USER_ID, CHILD_TENANT))
                .thenThrow(mock(IdentityRoleManagementException.class));
        FlowExecutionContext context = context("[\"role-1\"]", true);

        Assert.assertEquals(executor.execute(context).getResult(), STATUS_COMPLETE);

        Assert.assertEquals(context.getTenantDomain(), PARENT_TENANT);
        carbonContext.verify(PrivilegedCarbonContext::endTenantFlow);
        verify(roleManagementService, never()).updateUserListOfRole(anyString(), anyList(), anyList(), anyString());
    }

    private FlowExecutionContext context(String roleIds, boolean newOrganization) {

        FlowExecutionContext context = new FlowExecutionContext();
        context.setTenantDomain(PARENT_TENANT);
        context.setContextIdentifier("registration-flow");
        context.getFlowOrganization().setOrganizationHandle(CHILD_TENANT);
        context.getFlowUser().setUserId(USER_ID);
        ExecutorDTO config = new ExecutorDTO("ProvisioningDispatchExecutor");
        if (roleIds != null) {
            config.addMetadata("roleIds", roleIds);
        }
        if (newOrganization) {
            config.addMetadata("provisionTarget", "NEW_ORGANIZATION");
        }
        NodeConfig node = new NodeConfig.Builder().id("END").build();
        node.setExecutorConfig(config);
        context.setCurrentNode(node);
        return context;
    }

    private void shareRole(String sourceRoleId, String targetRoleId) throws Exception {

        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(sourceRoleId), CHILD_TENANT))
                .thenReturn(Collections.singletonMap(sourceRoleId, targetRoleId));
    }

    private void verifyAssignment(String roleId, String userId) throws Exception {

        verify(roleManagementService).updateUserListOfRole(roleId, Collections.singletonList(userId),
                Collections.emptyList(), CHILD_TENANT);
    }
}
