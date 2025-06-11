/*
 * Copyright (c) 2024-2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareUpdateOperation;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_RESOURCE_SHARING_POLICY_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_ROLE_AUDIENCES;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_ROLE_SHARING_CONFIG;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.EVENT_PROP_UPDATE_OPERATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ROLE_SHARING_MODE;

/**
 * Unit tests for SharedRoleMgtHandler.
 */
@WithCarbonHome
public class SharedRoleMgtHandlerTest {

    private static final String PARENT_ORG_TENANT_DOMAIN = "parent-org-tenant-domain";
    private static final String PARENT_ORG_ID = "parent-org-id";
    private static final String PARENT_ORG_USER_NAME = "parent-org-user";
    private static final String PARENT_ORG_USER_ID = "parent-org-user-id";
    private static final String PARENT_ORG_APP_ID = "parent-application-id";
    private static final String SHARED_ORG_TENANT_DOMAIN = "shared-org-tenant-domain";
    private static final String SHARED_ORG_ID = "shared-org-id";
    private static final String SHARED_ORG_APP_ID = "shared-app-id";
    private static final String ORGANIZATION_AUD = "organization";
    private static final String APPLICATION_AUD = "application";
    private static final String MAIN_ORG_ID = "main-org-id";
    private static final String MAIN_APP_ID = "main-app-id";
    private static final String MAIN_ORG_TENANT_DOMAIN = "main-org-tenant-domain";
    private static final int RESOURCE_SHARING_POLICY_ID = 123;

    private static MockedStatic<LoggerUtils> loggerUtils = null;
    private static MockedStatic<IdentityUtil> identityUtil = null;

    @BeforeClass
    public void setUp() {

        loggerUtils = mockStatic(LoggerUtils.class);
        identityUtil = mockStatic(IdentityUtil.class);
    }

    @AfterClass
    public void tearDown() {

        loggerUtils.close();
        identityUtil.close();
    }

    @DataProvider(name = "v2AuditLogsEnabled")
    public Object[][] v2AuditLogsEnabled() {

        return new Object[][]{
                {false},
                {true}
        };
    }

    @Test(dataProvider = "v2AuditLogsEnabled", expectedExceptions = IdentityEventException.class,
            expectedExceptionsMessageRegExp = ".*has a non shared role with.*")
    public void testHandleEventForPreShareApplicationEventWithConflictingRoles(boolean isV2AuditLogsEnabled)
            throws Exception {

        Event event = createPreShareApplicationEvent();

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).
                thenReturn(SHARED_ORG_TENANT_DOMAIN);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).
                thenReturn(PARENT_ORG_TENANT_DOMAIN);

        List<RoleV2> roles = new ArrayList<>();
        RoleV2 role = new RoleV2();
        role.setId("role-id");
        role.setName("role-name");
        roles.add(role);

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        lenient().when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);
        lenient().when(mockedApplicationManagementService.getAssociatedRolesOfApplication(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(roles);

        RoleManagementService roleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(roleManagementService);
        lenient().when(roleManagementService.isExistingRoleName(roles.get(0).getName(), ORGANIZATION_AUD,
                        SHARED_ORG_ID, SHARED_ORG_TENANT_DOMAIN)).thenReturn(true);
        lenient().when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(role.getId()), SHARED_ORG_TENANT_DOMAIN)).thenReturn(new HashMap<>());

        loggerUtils.when(LoggerUtils::isEnableV2AuditLogs).thenReturn(isV2AuditLogsEnabled);
        identityUtil.when(() -> IdentityUtil.getInitiatorId(PARENT_ORG_USER_NAME, PARENT_ORG_TENANT_DOMAIN)).
                thenReturn(PARENT_ORG_USER_ID);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    @Test
    public void testHandleEventForPreShareApplicationEventWithoutConflictingRoles() throws Exception {

        Event event = createPreShareApplicationEvent();

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).
                thenReturn(SHARED_ORG_TENANT_DOMAIN);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).
                thenReturn(PARENT_ORG_TENANT_DOMAIN);

        List<RoleV2> roles = new ArrayList<>();
        RoleV2 role = new RoleV2();
        role.setId("role-id");
        role.setName("role-name");
        roles.add(role);

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        lenient().when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);
        lenient().when(mockedApplicationManagementService.getAssociatedRolesOfApplication(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(roles);

        RoleManagementService roleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(roleManagementService);
        lenient().when(roleManagementService.isExistingRoleName(roles.get(0).getName(), ORGANIZATION_AUD,
                SHARED_ORG_ID, SHARED_ORG_TENANT_DOMAIN)).thenReturn(true);
        Map<String, String> mainRoleToSharedRoleMapping = new HashMap<>();
        mainRoleToSharedRoleMapping.put(roles.get(0).getId(), "mapped-shared-role-id");
        lenient().when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(role.getId()), SHARED_ORG_TENANT_DOMAIN)).
                thenReturn(mainRoleToSharedRoleMapping);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    @Test
    public void testHandleEventForPreShareApplicationEventWithApplicationAud() throws Exception {

        Event event = createPreShareApplicationEvent();

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).
                thenReturn(SHARED_ORG_TENANT_DOMAIN);
        lenient().when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).
                thenReturn(PARENT_ORG_TENANT_DOMAIN);

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        lenient().when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(PARENT_ORG_APP_ID,
                PARENT_ORG_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    @Test
    public void testHandleEventForPostShareApplicationEventAppAudienceWithPolicyId() throws Exception {

        Event event = createPostShareApplicationEvent(true, ApplicationShareRolePolicy.Mode.ALL, null);

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.getOrganization(SHARED_ORG_ID, false, false))
                .thenReturn(createMockOrganization(SHARED_ORG_ID, PARENT_ORG_ID));

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);

        RoleManagementService mockedRoleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);

        List<RoleBasicInfo> mainAppRoles = Collections.singletonList(createMockRoleBasicInfo("role1",
                "app-role1-id", MAIN_APP_ID));
        when(mockedRoleManagementService.getRoles(anyString(), eq(null), eq(0), eq(null), eq(null),
                eq(MAIN_ORG_TENANT_DOMAIN))).thenReturn(mainAppRoles);
        when(mockedRoleManagementService.getRoles(anyString(), eq(null), eq(0), eq(null), eq(null),
                eq(SHARED_ORG_TENANT_DOMAIN))).thenReturn(Collections.emptyList()); // No existing shared roles

        Map<String, String> roleMappings = new HashMap<>();
        roleMappings.put("app-role1-id", "parent-shared-role-id");
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList("app-role1-id"), PARENT_ORG_TENANT_DOMAIN))
                .thenReturn(roleMappings);

        when(mockedRoleManagementService.isExistingRoleName(anyString(), eq(RoleConstants.APPLICATION),
                eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN))).thenReturn(false); // Role does not exist yet
        RoleBasicInfo createdRole = createMockRoleBasicInfo("role1", "shared-role1-id", SHARED_ORG_APP_ID);
        when(mockedRoleManagementService.addRole(eq("role1"), anyList(), anyList(), anyList(),
                eq(RoleConstants.APPLICATION), eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN)))
                .thenReturn(createdRole);

        ResourceSharingPolicyHandlerService mockedResourceSharingPolicyHandlerService =
                mock(ResourceSharingPolicyHandlerService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(mockedResourceSharingPolicyHandlerService);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);

        verify(mockedRoleManagementService, times(1)).addRole(eq("role1"), anyList(), anyList(),
                anyList(), eq(RoleConstants.APPLICATION), eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN));
        verify(mockedRoleManagementService, times(1)).addMainRoleToSharedRoleRelationship(
                eq("app-role1-id"), eq("shared-role1-id"), eq(MAIN_ORG_TENANT_DOMAIN), eq(SHARED_ORG_TENANT_DOMAIN));
        verify(mockedResourceSharingPolicyHandlerService, times(1)).addSharedResourceAttributes(
                anyList());
    }

    @Test
    public void testHandleEventForPostShareApplicationEventAppAudienceWithoutPolicyId() throws Exception {

        Event event = createPostShareApplicationEvent(false, ApplicationShareRolePolicy.Mode.ALL, null); // No policy ID

        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.getOrganization(SHARED_ORG_ID, false, false))
                .thenReturn(createMockOrganization(SHARED_ORG_ID, PARENT_ORG_ID));

        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);

        RoleManagementService mockedRoleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);
        List<RoleBasicInfo> mainAppRoles = Collections.singletonList(createMockRoleBasicInfo("role1",
                "app-role1-id", MAIN_APP_ID));
        when(mockedRoleManagementService.getRoles(anyString(), eq(null), eq(0), eq(null), eq(null),
                eq(MAIN_ORG_TENANT_DOMAIN))).thenReturn(mainAppRoles);
        when(mockedRoleManagementService.getRoles(anyString(), eq(null), eq(0), eq(null), eq(null),
                eq(SHARED_ORG_TENANT_DOMAIN))).thenReturn(Collections.emptyList());

        Map<String, String> roleMappings = new HashMap<>();
        roleMappings.put("app-role1-id", "parent-shared-role-id");
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList("app-role1-id"), PARENT_ORG_TENANT_DOMAIN))
                .thenReturn(roleMappings);

        when(mockedRoleManagementService.isExistingRoleName(anyString(), eq(RoleConstants.APPLICATION),
                eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN))).thenReturn(false);
        RoleBasicInfo createdRole = createMockRoleBasicInfo("role1", "shared-role1-id", SHARED_ORG_APP_ID);
        when(mockedRoleManagementService.addRole(eq("role1"), anyList(), anyList(), anyList(),
                eq(RoleConstants.APPLICATION), eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN)))
                .thenReturn(createdRole);

        ResourceSharingPolicyHandlerService mockedResourceSharingPolicyHandlerService =
                mock(ResourceSharingPolicyHandlerService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(mockedResourceSharingPolicyHandlerService);

        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);

        verify(mockedRoleManagementService, times(1)).addRole(eq("role1"), anyList(), anyList(),
                anyList(), eq(RoleConstants.APPLICATION), eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN));
        verify(mockedRoleManagementService, times(1)).addMainRoleToSharedRoleRelationship(
                eq("app-role1-id"), eq("shared-role1-id"), eq(MAIN_ORG_TENANT_DOMAIN), eq(SHARED_ORG_TENANT_DOMAIN));
        verify(mockedResourceSharingPolicyHandlerService, never()).addSharedResourceAttributes(anyList());
    }

    @Test
    public void testAddAppRolesToSharedApplication() throws Exception {

        // Create an event for adding app roles to a shared application.
        Event event = createUpdateRolesOfSharedApplicationEvent(
                ApplicationShareUpdateOperation.Operation.ADD,
                Collections.singletonList(createRoleWithAudience("newRole", APPLICATION_AUD, MAIN_APP_ID)));

        // Mock the organization manager.
        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_TENANT_DOMAIN);

        // Return parent org as PARENT_ORG_ID consistently.
        when(mockedOrganizationManager.getOrganization(SHARED_ORG_ID, false, false))
                .thenReturn(createMockOrganization(SHARED_ORG_ID, PARENT_ORG_ID));

        // Mock the application management service.
        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);

        // Setup to return APPLICATION audience type.
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);

        // Create a service provider with SELECTED role sharing mode.
        ServiceProvider mockSharedSP = mock(ServiceProvider.class);
        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[1];
        ServiceProviderProperty spProperty = new ServiceProviderProperty();
        spProperty.setName(ROLE_SHARING_MODE);
        spProperty.setValue(ApplicationShareRolePolicy.Mode.SELECTED.name());
        spProperties[0] = spProperty;
        when(mockSharedSP.getSpProperties()).thenReturn(spProperties);
        when(mockedApplicationManagementService.getApplicationByResourceId(SHARED_ORG_APP_ID,
                SHARED_ORG_TENANT_DOMAIN)).thenReturn(mockSharedSP);

        // Mock the role management service.
        RoleManagementService mockedRoleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);

        // Setup main app roles.
        List<RoleBasicInfo> mainAppRoles = Collections.singletonList(
                createMockRoleBasicInfo("newRole", "main-role-id", MAIN_APP_ID));
        when(mockedRoleManagementService.getRoles(anyString(), eq(null), eq(0), eq(null), eq(null),
                eq(MAIN_ORG_TENANT_DOMAIN))).thenReturn(mainAppRoles);

        // Setup parent organization role mappings - ensure PARENT_ORG_TENANT_DOMAIN is used.
        Map<String, String> roleMappings = new HashMap<>();
        roleMappings.put("main-role-id", "parent-shared-role-id");

        // Mock with specific parameter values to ensure proper matching.
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                eq(Collections.singletonList("main-role-id")), eq(PARENT_ORG_TENANT_DOMAIN)))
                .thenReturn(roleMappings);

        // Setup that the role doesn't exist yet in the shared org.
        when(mockedRoleManagementService.isExistingRoleName(eq("newRole"), eq(RoleConstants.APPLICATION),
                eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN))).thenReturn(false);

        // Setup result for role creation.
        RoleBasicInfo createdRole = createMockRoleBasicInfo("newRole", "shared-role-id", SHARED_ORG_APP_ID);
        when(mockedRoleManagementService.addRole(eq("newRole"), anyList(), anyList(), anyList(),
                eq(RoleConstants.APPLICATION), eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN)))
                .thenReturn(createdRole);

        // Execute the handler.
        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);

        // Verify the role was created and relationship was established.
        verify(mockedRoleManagementService, times(1)).addRole(eq("newRole"), anyList(), anyList(),
                anyList(), eq(RoleConstants.APPLICATION), eq(SHARED_ORG_APP_ID), eq(SHARED_ORG_TENANT_DOMAIN));
        verify(mockedRoleManagementService, times(1)).addMainRoleToSharedRoleRelationship(
                eq("main-role-id"), eq("shared-role-id"), eq(MAIN_ORG_TENANT_DOMAIN), eq(SHARED_ORG_TENANT_DOMAIN));
    }

    /**
     * Test for removing application-scoped roles from a shared application.
     */
    @Test
    public void testRemoveAppRolesFromSharedApplication() throws Exception {

        // Create an event for removing app roles from a shared application.
        Event event = createUpdateRolesOfSharedApplicationEvent(
                ApplicationShareUpdateOperation.Operation.REMOVE,
                Collections.singletonList(createRoleWithAudience("existingRole", APPLICATION_AUD, SHARED_ORG_APP_ID)));

        // Mock the organization manager.
        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.getOrganization(SHARED_ORG_ID, true, false))
                .thenReturn(createMockOrganization(SHARED_ORG_ID, PARENT_ORG_ID));

        // Mock the application management service.
        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);

        // Setup to return APPLICATION audience type.
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);

        // Create a service provider with SELECTED role sharing mode.
        ServiceProvider mockSharedSP = mock(ServiceProvider.class);
        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[1];
        ServiceProviderProperty spProperty = new ServiceProviderProperty();
        spProperty.setName(ROLE_SHARING_MODE);
        spProperty.setValue(ApplicationShareRolePolicy.Mode.SELECTED.name());
        spProperties[0] = spProperty;
        when(mockSharedSP.getSpProperties()).thenReturn(spProperties);
        when(mockedApplicationManagementService.getApplicationByResourceId(SHARED_ORG_APP_ID,
                SHARED_ORG_TENANT_DOMAIN)).thenReturn(mockSharedSP);

        // Mock the role management service.
        RoleManagementService mockedRoleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);

        // Setup existing roles in shared app.
        List<RoleBasicInfo> sharedAppRoles = Collections.singletonList(
                createMockRoleBasicInfo("existingRole", "shared-role-id", SHARED_ORG_APP_ID));
        when(mockedRoleManagementService.getRoles(anyString(), eq(null), eq(0), eq(null), eq(null),
                eq(SHARED_ORG_TENANT_DOMAIN))).thenReturn(sharedAppRoles);

        // Execute the handler.
        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);

        // Verify the role was deleted.
        verify(mockedRoleManagementService, times(1)).deleteRole("shared-role-id", SHARED_ORG_TENANT_DOMAIN);
    }

    /**
     * Test for adding organization-scoped roles to a shared application.
     */
    @Test
    public void testAddOrgRolesToSharedApplication() throws Exception {

        // Create an event for adding org roles to a shared application.
        Event event = createUpdateRolesOfSharedApplicationEvent(
                ApplicationShareUpdateOperation.Operation.ADD,
                Collections.singletonList(createRoleWithAudience("newOrgRole", ORGANIZATION_AUD, SHARED_ORG_ID)));

        // Mock the organization manager.
        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_ORG_TENANT_DOMAIN);

        // Properly create the organization with parent relation.
        org.wso2.carbon.identity.organization.management.service.model.Organization sharedOrg =
                createMockOrganization(SHARED_ORG_ID, MAIN_ORG_ID); // Setting MAIN_ORG as direct parent.
        when(mockedOrganizationManager.getOrganization(SHARED_ORG_ID, false, false))
                .thenReturn(sharedOrg);

        // Mock the application management service.
        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);

        // Setup to return ORGANIZATION audience type.
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);

        // Create a service provider with SELECTED role sharing mode.
        ServiceProvider mockSharedSP = mock(ServiceProvider.class);
        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[1];
        ServiceProviderProperty spProperty = new ServiceProviderProperty();
        spProperty.setName(ROLE_SHARING_MODE);
        spProperty.setValue(ApplicationShareRolePolicy.Mode.SELECTED.name());
        spProperties[0] = spProperty;
        when(mockSharedSP.getSpProperties()).thenReturn(spProperties);
        when(mockedApplicationManagementService.getApplicationByResourceId(SHARED_ORG_APP_ID,
                SHARED_ORG_TENANT_DOMAIN)).thenReturn(mockSharedSP);

        // Mock the role management service.
        RoleManagementService mockedRoleManagementService = mock(RoleManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);

        // Setup associated org roles in the main app.
        List<RoleV2> mainAppOrgRoles = new ArrayList<>();
        RoleV2 orgRole = new RoleV2();
        orgRole.setId("org-role-id");
        orgRole.setName("newOrgRole");
        mainAppOrgRoles.add(orgRole);
        when(mockedApplicationManagementService.getAssociatedRolesOfApplication(
                MAIN_APP_ID, MAIN_ORG_TENANT_DOMAIN)).thenReturn(mainAppOrgRoles);

        // Setup that the role doesn't exist yet in the shared org.
        when(mockedRoleManagementService.isExistingRoleName("newOrgRole", RoleConstants.ORGANIZATION,
                SHARED_ORG_ID, SHARED_ORG_TENANT_DOMAIN)).thenReturn(false);

        // Setup result for role creation.
        RoleBasicInfo createdRole = createMockRoleBasicInfo("newOrgRole", "shared-org-role-id", SHARED_ORG_ID);
        when(mockedRoleManagementService.addRole(eq("newOrgRole"), anyList(), anyList(), anyList(),
                eq(RoleConstants.ORGANIZATION), eq(SHARED_ORG_ID), eq(SHARED_ORG_TENANT_DOMAIN)))
                .thenReturn(createdRole);

        // Execute the handler.
        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);

        // Verify the org role was created and relationship was established.
        verify(mockedRoleManagementService, times(1)).addRole(eq("newOrgRole"), anyList(), anyList(),
                anyList(), eq(RoleConstants.ORGANIZATION), eq(SHARED_ORG_ID), eq(SHARED_ORG_TENANT_DOMAIN));
        verify(mockedRoleManagementService, times(1)).addMainRoleToSharedRoleRelationship(
                eq("org-role-id"), eq("shared-org-role-id"), eq(MAIN_ORG_TENANT_DOMAIN), eq(SHARED_ORG_TENANT_DOMAIN));
    }

    /**
     * Test for attempting to remove organization-scoped roles from a shared application, which is not supported.
     */
    @Test(expectedExceptions = IdentityEventException.class,
          expectedExceptionsMessageRegExp = ".*Removing roles from shared application.*is not supported.*")
    public void testRemoveOrgRolesFromSharedApplication() throws Exception {

        // Create an event for removing org roles from a shared application - this should fail.
        Event event = createUpdateRolesOfSharedApplicationEvent(
                ApplicationShareUpdateOperation.Operation.REMOVE,
                Collections.singletonList(createRoleWithAudience("existingOrgRole", ORGANIZATION_AUD, SHARED_ORG_ID)));

        // Mock the organization manager.
        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);

        // Mock the application management service.
        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);

        // Setup to return ORGANIZATION audience type.
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);

        // Execute the handler - should throw exception.
        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    /**
     * Test for invalid operation type when updating roles of a shared application.
     */
    @Test(expectedExceptions = IdentityEventException.class,
          expectedExceptionsMessageRegExp = ".*Invalid operation type.*")
    public void testInvalidOperationTypeForUpdateRoles() throws Exception {

        // Create an event with an invalid operation
        Event event = createUpdateRolesOfSharedApplicationEvent(
                null,  // null operation should cause error.
                Collections.singletonList(createRoleWithAudience("someRole", APPLICATION_AUD, MAIN_APP_ID)));

        // Mock the organization manager.
        OrganizationManager mockedOrganizationManager = mock(OrganizationManager.class);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        when(mockedOrganizationManager.resolveTenantDomain(MAIN_ORG_ID)).thenReturn(MAIN_ORG_TENANT_DOMAIN);
        when(mockedOrganizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_ORG_TENANT_DOMAIN);

        // Mock the application management service.
        ApplicationManagementService mockedApplicationManagementService = mock(ApplicationManagementService.class);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setApplicationManagementService(mockedApplicationManagementService);

        // Setup to return APPLICATION audience type.
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(MAIN_APP_ID,
                MAIN_ORG_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);

        // Execute the handler - should throw exception.
        SharedRoleMgtHandler sharedRoleMgtHandler = new SharedRoleMgtHandler();
        sharedRoleMgtHandler.handleEvent(event);
    }

    /**
     * Helper method to create an event for testing the updateSharedApplicationRoles handler.
     */
    private Event createUpdateRolesOfSharedApplicationEvent(
            ApplicationShareUpdateOperation.Operation operation, List<RoleWithAudienceDO> roles) {

        Event event = new Event(OrgApplicationMgtConstants.EVENT_POST_UPDATE_ROLES_OF_SHARED_APPLICATION);
        event.addEventProperty(EVENT_PROP_MAIN_ORGANIZATION_ID, MAIN_ORG_ID);
        event.addEventProperty(EVENT_PROP_MAIN_APPLICATION_ID, MAIN_APP_ID);
        event.addEventProperty(EVENT_PROP_SHARED_ORGANIZATION_ID, SHARED_ORG_ID);
        event.addEventProperty(EVENT_PROP_SHARED_APPLICATION_ID, SHARED_ORG_APP_ID);
        event.addEventProperty(EVENT_PROP_UPDATE_OPERATION, operation);
        event.addEventProperty(EVENT_PROP_ROLE_AUDIENCES, roles);
        return event;
    }

    /**
     * Helper method to create a RoleWithAudienceDO object.
     */
    private RoleWithAudienceDO createRoleWithAudience(String roleName, String audienceType, String audienceName) {

        RoleWithAudienceDO.AudienceType audienceTypeEnum =
                RoleWithAudienceDO.AudienceType.fromValue(audienceType);
        return new RoleWithAudienceDO(roleName, audienceName, audienceTypeEnum);
    }

    private static Event createPreShareApplicationEvent() {

        Event event = new Event(OrgApplicationMgtConstants.EVENT_PRE_SHARE_APPLICATION);
        event.addEventProperty(EVENT_PROP_PARENT_ORGANIZATION_ID, PARENT_ORG_ID);
        event.addEventProperty(EVENT_PROP_PARENT_APPLICATION_ID, PARENT_ORG_APP_ID);
        event.addEventProperty(EVENT_PROP_SHARED_ORGANIZATION_ID, SHARED_ORG_ID);
        event.addEventProperty(EVENT_PROP_SHARED_APPLICATION_ID, SHARED_ORG_APP_ID);
        return event;
    }

    private Event createPostShareApplicationEvent(boolean withPolicyId, ApplicationShareRolePolicy.Mode mode,
                                                  List<RoleWithAudienceDO> roles) {

        Event event = new Event(OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION);
        event.addEventProperty(EVENT_PROP_MAIN_ORGANIZATION_ID, MAIN_ORG_ID);
        event.addEventProperty(EVENT_PROP_MAIN_APPLICATION_ID, MAIN_APP_ID);
        event.addEventProperty(EVENT_PROP_SHARED_ORGANIZATION_ID, SHARED_ORG_ID);
        event.addEventProperty(EVENT_PROP_SHARED_APPLICATION_ID, SHARED_ORG_APP_ID);
        ApplicationShareRolePolicy.Builder policyBuilder = new ApplicationShareRolePolicy.Builder().mode(mode);
        if (roles != null) {
            policyBuilder.roleWithAudienceDOList(roles);
        }
        event.addEventProperty(EVENT_PROP_ROLE_SHARING_CONFIG, policyBuilder.build());
        if (withPolicyId) {
            event.addEventProperty(EVENT_PROP_RESOURCE_SHARING_POLICY_ID, RESOURCE_SHARING_POLICY_ID);
        }
        return event;
    }

    private RoleBasicInfo createMockRoleBasicInfo(String name, String id, String audienceId) {

        RoleBasicInfo role = mock(RoleBasicInfo.class);
        when(role.getName()).thenReturn(name);
        when(role.getId()).thenReturn(id);
        when(role.getAudienceId()).thenReturn(audienceId);
        // Assuming audienceName is same as audienceId for app roles.
        when(role.getAudienceName()).thenReturn(audienceId);
        return role;
    }

    private org.wso2.carbon.identity.organization.management.service.model.Organization createMockOrganization(
            String orgId, String parentId) {

        org.wso2.carbon.identity.organization.management.service.model.Organization org =
                new org.wso2.carbon.identity.organization.management.service.model.Organization();
        org.setId(orgId);
        ParentOrganizationDO parent = new ParentOrganizationDO();
        parent.setId(parentId);
        org.setParent(parent);
        return org;
    }
}
