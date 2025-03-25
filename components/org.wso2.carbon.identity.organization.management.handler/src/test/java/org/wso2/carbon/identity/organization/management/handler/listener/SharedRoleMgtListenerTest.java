/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler.listener;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.AssociatedRolesConfig;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.handler.util.TestUtils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Contains unit tests for SharedRoleMgtListener.
 */
public class SharedRoleMgtListenerTest {

    private static final String SAMPLE_APPLICATION_NAME = "sampleApplicationName";
    private static final String SAMPLE_TENANT_DOMAIN = "sampleTenantDomain";
    private static final int SAMPLE_TENANT_ID = 12345;
    private static final String SAMPLE_USERNAME = "sampleUsername";
    private static final String SAMPLE_ROLE_NAME = "sampleRoleName";
    private static final String SAMPLE_MAIN_APP_ID = "main-app-id";
    private static final String SAMPLE_SHARED_APP_ID = "shared-app-id";
    private static final String SAMPLE_SHARED_APP_ORG_ID = "shared-app-org-id";
    private static final String SAMPLE_SHARED_APP_TENANT_DOMAIN = "shared-app-tenant-domain";
    private static final String SAMPLE_ORG_ID = "org-id";
    private static final String SAMPLE_ROLE_ID = "role-id";
    private static final String SAMPLE_SHARED_ROLE_ID = "shared-role-id";
    private static final String IS_FRAGMENT_APP = "isFragmentApp";
    private static final String ORGANIZATION_AUD = "organization";
    private static final String APPLICATION_AUD = "application";
    private static final String REMOVED_ORGANIZATION_AUDIENCE_ROLES = "removedOrganizationAudienceRoles";
    private static final String ADDED_APPLICATION_AUDIENCE_ROLES = "addedApplicationAudienceRoles";
    private static final String ADDED_ORGANIZATION_AUDIENCE_ROLES = "addedOrganizationAudienceRoles";
    private static final String PARENT_ORG_USER_NAME = "parent-org-user";
    private static MockedStatic<LoggerUtils> loggerUtils = null;
    private static MockedStatic<IdentityUtil> identityUtil = null;

    @Mock
    private ApplicationManagementService mockedApplicationManagementService;

    @Mock
    private OrganizationManager mockedOrganizationManager;

    @Mock
    private OrgApplicationManager mockedOrgApplicationManager;

    @Mock
    private RoleManagementService mockedRoleManagementService;

    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilMockedStatic;
    private MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic;

    @BeforeClass
    public void setUpClass() {

        TestUtils.initPrivilegedCarbonContext();
        MockitoAnnotations.openMocks(this);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SAMPLE_TENANT_DOMAIN);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(PARENT_ORG_USER_NAME);
        OrganizationManagementHandlerDataHolder.getInstance().
                setApplicationManagementService(mockedApplicationManagementService);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        OrganizationManagementHandlerDataHolder.getInstance().setOrgApplicationManager(mockedOrgApplicationManager);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);
        loggerUtils = mockStatic(LoggerUtils.class);
        identityUtil = mockStatic(IdentityUtil.class);
        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);
        identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
    }

    @AfterClass
    public void tearDown() {

        organizationManagementUtilMockedStatic.close();
        identityTenantUtilMockedStatic.close();
        loggerUtils.close();
        identityUtil.close();
    }

    @DataProvider(name = "organizationTypeDataProvider")
    public Object[][] organizationTypeDataProvider() {

        // Create a ServiceProvider object for a main application.
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(SAMPLE_APPLICATION_NAME);

        return new Object[][]{
                {false, null, true},
                {true, serviceProvider, true},
                {true, null, false},
        };
    }

    @DataProvider(name = "roleAudienceDataProvider")
    public Object[][] roleAudienceDataProvider() {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(SAMPLE_APPLICATION_NAME);
        serviceProvider.setApplicationResourceId(SAMPLE_SHARED_APP_ID);

        ServiceProviderProperty serviceProviderProperty = new ServiceProviderProperty();
        serviceProviderProperty.setName(IS_FRAGMENT_APP);
        serviceProviderProperty.setValue(Boolean.TRUE.toString());
        serviceProvider.setSpProperties(new ServiceProviderProperty[]{serviceProviderProperty});

        return new Object[][] {
                {true, serviceProvider, APPLICATION_AUD, true}
        };
    }

    @DataProvider(name = "applicationDataProvider")
    public Object[][] applicationDataProvider() {

        return new Object[][] {
                {null},
                {SAMPLE_MAIN_APP_ID}
        };
    }

    @DataProvider(name = "updateApplicationDataProvider")
    public Object[][] updateApplicationDataProvider() {

        ServiceProvider fragmentServiceProvider = new ServiceProvider();
        ServiceProviderProperty serviceProviderProperty = new ServiceProviderProperty();
        serviceProviderProperty.setName(IS_FRAGMENT_APP);
        serviceProviderProperty.setValue(Boolean.TRUE.toString());
        fragmentServiceProvider.setSpProperties(new ServiceProviderProperty[]{serviceProviderProperty});

        ServiceProvider mainServiceProvider = new ServiceProvider();
        ServiceProviderProperty mainServiceProviderProperty = new ServiceProviderProperty();
        mainServiceProviderProperty.setName("SAMPLE_PROPERTY");
        mainServiceProviderProperty.setValue("SAMPLE_VALUE");
        mainServiceProvider.setSpProperties(new ServiceProviderProperty[]{mainServiceProviderProperty});

        return new Object[][] {
                {fragmentServiceProvider},
                {mainServiceProvider}
        };
    }

    @DataProvider(name = "mainRoleProvider")
    public Object[][] mainRoleProvider() {

        RoleV2 roleV2 = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        List<RoleV2> addedAppRoles = Collections.singletonList(roleV2);

        List<RoleV2> addedEmptyAppRoles = Collections.emptyList();

        return new Object[][] {
                {addedAppRoles},
                {addedEmptyAppRoles}
        };
    }

    @DataProvider(name = "createSharedRolesWithOrgAudienceDataProvider")
    public Object[][] createSharedRolesWithOrgAudienceDataProvider() {

        RoleV2 roleV2 = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        List<RoleV2> addedOrgRoleList = Collections.singletonList(roleV2);

        return new Object[][] {
                {null, false, 0},
                {addedOrgRoleList, false, 1},
                {addedOrgRoleList, true, 2}
        };
    }

    @Test(dataProvider = "organizationTypeDataProvider")
    public void testDoPreDeleteApplicationInOrgTypes(boolean isOrganization, ServiceProvider serviceProvider,
                                                     boolean expected) throws Exception {

        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(isOrganization);
        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        if (!isOrganization) {
            assertEquals(sharedRoleMgtListener.doPreDeleteApplication(SAMPLE_APPLICATION_NAME, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME), expected);
        } else {
            when(mockedApplicationManagementService.getServiceProvider(SAMPLE_APPLICATION_NAME, SAMPLE_TENANT_DOMAIN))
                    .thenReturn(serviceProvider);
            assertEquals(sharedRoleMgtListener.doPreDeleteApplication(SAMPLE_APPLICATION_NAME, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME), expected);
        }
    }

    @Test(dataProvider = "roleAudienceDataProvider")
    public void testDoPreDeleteApplicationBasedOnRoleAudience(boolean isOrganization, ServiceProvider serviceProvider,
                                                              String audience, boolean expected) throws Exception {

        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(isOrganization);
        when(mockedApplicationManagementService.getServiceProvider(SAMPLE_APPLICATION_NAME, SAMPLE_TENANT_DOMAIN))
                .thenReturn(serviceProvider);
        when(mockedOrganizationManager.resolveOrganizationId(SAMPLE_TENANT_DOMAIN)).thenReturn(
                SAMPLE_SHARED_APP_ORG_ID);
        when(mockedOrgApplicationManager.getMainApplicationIdForGivenSharedApp(SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_ORG_ID)).thenReturn(SAMPLE_MAIN_APP_ID);
        when(mockedApplicationManagementService.getTenantIdByApp(SAMPLE_MAIN_APP_ID)).thenReturn(SAMPLE_TENANT_ID);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID)).
                thenReturn(SAMPLE_TENANT_DOMAIN);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN)).thenReturn(audience);
        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        assertEquals(sharedRoleMgtListener.doPreDeleteApplication(SAMPLE_APPLICATION_NAME, SAMPLE_TENANT_DOMAIN,
                SAMPLE_USERNAME), expected);
    }

    @Test(dataProvider = "applicationDataProvider")
    public void testDoPostGetAllowedAudienceForRoleAssociation(String mainAppId) throws Exception {

        AssociatedRolesConfig associatedRolesConfig = new AssociatedRolesConfig();
        RoleV2 roleV2 = new RoleV2();
        roleV2.setName(SAMPLE_ROLE_NAME);
        associatedRolesConfig.setRoles(new RoleV2[]{roleV2});
        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        when(mockedApplicationManagementService.getMainAppId(SAMPLE_SHARED_APP_ID)).thenReturn(mainAppId);
        when(mockedApplicationManagementService.getTenantIdByApp(mainAppId)).thenReturn(SAMPLE_TENANT_ID);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID)).
                thenReturn(SAMPLE_TENANT_DOMAIN);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(mainAppId, SAMPLE_TENANT_DOMAIN)).
                thenReturn(ORGANIZATION_AUD);
        assertTrue(sharedRoleMgtListener.doPostGetAllowedAudienceForRoleAssociation(associatedRolesConfig,
                SAMPLE_SHARED_APP_ID, SAMPLE_USERNAME));
        if (StringUtils.isNotEmpty(mainAppId)) {
            assertEquals(associatedRolesConfig.getAllowedAudience(), ORGANIZATION_AUD);
        }
    }

    @Test
    public void testHandleRemovedOrganizationAudienceRolesOnAppUpdate() throws Exception {

        RoleV2 roleV2 = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        List<RoleV2> removedOrgRolesList = Collections.singletonList(roleV2);

        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(REMOVED_ORGANIZATION_AUDIENCE_ROLES, removedOrgRolesList);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        SharedApplication sharedApplication = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_ORG_ID);
        List<SharedApplication> sharedApplications = Collections.singletonList(sharedApplication);

        Map<String, String> mainRoleToSharedRoleMappingsInSubOrg = new HashMap<>();
        mainRoleToSharedRoleMappingsInSubOrg.put(SAMPLE_ROLE_ID, SAMPLE_SHARED_ROLE_ID);

        when(mockedOrgApplicationManager.getSharedApplications(null, SAMPLE_SHARED_APP_ID))
                .thenReturn(sharedApplications);
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(SAMPLE_ROLE_ID), null))
                .thenReturn(mainRoleToSharedRoleMappingsInSubOrg);
        when(mockedRoleManagementService.getAssociatedApplicationByRoleId(SAMPLE_ROLE_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(Collections.singletonList(SAMPLE_SHARED_APP_ID));

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationResourceId(SAMPLE_SHARED_APP_ID);
        assertEquals(sharedRoleMgtListener.doPostUpdateApplication(serviceProvider, SAMPLE_TENANT_DOMAIN,
                SAMPLE_USERNAME), true);
    }

    @Test(dataProvider = "updateApplicationDataProvider")
    public void testDoPreUpdateApplication(ServiceProvider serviceProvider) throws Exception {

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        sharedRoleMgtListener.doPreUpdateApplication(serviceProvider, SAMPLE_TENANT_DOMAIN, SAMPLE_USERNAME);
    }

    @Test(dataProvider = "updateApplicationDataProvider")
    public void testDoPostUpdateApplication(ServiceProvider serviceProvider) throws Exception {

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        sharedRoleMgtListener.doPostUpdateApplication(serviceProvider, SAMPLE_TENANT_DOMAIN, SAMPLE_USERNAME);
    }

    @Test(dataProvider = "mainRoleProvider")
    public void testDoPostUpdateApplicationWithAddedAppRoles(List<RoleV2> addedAppRoles) throws Exception {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationResourceId(SAMPLE_MAIN_APP_ID);

        SharedApplication sharedApp1 = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_ORG_ID);

        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ADDED_APPLICATION_AUDIENCE_ROLES, addedAppRoles);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        if (!addedAppRoles.isEmpty()) {
            when(mockedOrganizationManager.resolveOrganizationId(SAMPLE_TENANT_DOMAIN)).thenReturn(SAMPLE_ORG_ID);
            when(mockedOrgApplicationManager.getSharedApplications(SAMPLE_ORG_ID, SAMPLE_MAIN_APP_ID)).
                    thenReturn(Collections.singletonList(sharedApp1));
            when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID)).
                    thenReturn(SAMPLE_SHARED_APP_TENANT_DOMAIN);

            RoleBasicInfo sharedRoleBasicInfo = new RoleBasicInfo();
            sharedRoleBasicInfo.setId(SAMPLE_SHARED_ROLE_ID);
            sharedRoleBasicInfo.setAudience(RoleConstants.ORGANIZATION);
            sharedRoleBasicInfo.setAudienceId(SAMPLE_SHARED_APP_ORG_ID);
            when(mockedRoleManagementService.addRole(addedAppRoles.get(0).getName(), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList(), RoleConstants.APPLICATION,
                    sharedApp1.getSharedApplicationId(), SAMPLE_SHARED_APP_TENANT_DOMAIN)).
                    thenReturn(sharedRoleBasicInfo);
            doNothing().when(mockedRoleManagementService).addMainRoleToSharedRoleRelationship(anyString(), anyString(),
                    anyString(), anyString());
        }

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        sharedRoleMgtListener.doPostUpdateApplication(serviceProvider, SAMPLE_TENANT_DOMAIN, SAMPLE_USERNAME);
    }

    @Test(description = "Test the scenario where an organization role is trying to be created in the sub " +
            "organization but the sub organization have an organization role with the same name.",
            dataProvider = "createSharedRolesWithOrgAudienceDataProvider")
    public void testCreateSharedRolesWithOrgAudienceWithOrgAudienceRoleConflict(List<RoleV2> addedOrgRoleList,
                                                                       boolean isV2AuditLogsEnabled,
                                                                       int expectedInvocations) throws Exception {

        when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID)).thenReturn(
                SAMPLE_SHARED_APP_TENANT_DOMAIN);
        when(mockedRoleManagementService.isExistingRoleName(SAMPLE_ROLE_NAME, ORGANIZATION_AUD,
                SAMPLE_SHARED_APP_ORG_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(Boolean.TRUE);
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(SAMPLE_ROLE_ID), SAMPLE_SHARED_APP_TENANT_DOMAIN)).
                thenReturn(new HashMap<>());

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        Method createSharedRolesWithOrgAudiencePrivateMethod = SharedRoleMgtListener.class.
                getDeclaredMethod("createSharedRolesWithOrgAudience", List.class, String.class, String.class);
        createSharedRolesWithOrgAudiencePrivateMethod.setAccessible(true);

        loggerUtils.when(LoggerUtils::isEnableV2AuditLogs).thenReturn(isV2AuditLogsEnabled);
        createSharedRolesWithOrgAudiencePrivateMethod.invoke(sharedRoleMgtListener, addedOrgRoleList,
                SAMPLE_TENANT_DOMAIN, SAMPLE_SHARED_APP_ORG_ID);

        loggerUtils.verify(LoggerUtils::isEnableV2AuditLogs, times(expectedInvocations));
    }

    @Test(description = "Test the scenario where the organization roles are trying to be created in the sub " +
            "organization but the sub organization have an organization role with the same name.")
    public void testDoPostUpdateApplicationWithOrgAudienceRoleConflict() throws Exception {

        ServiceProvider mainServiceProvider = new ServiceProvider();
        mainServiceProvider.setApplicationResourceId(SAMPLE_MAIN_APP_ID);

        RoleV2 roleV2 = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        List<RoleV2> addedOrgRoleList = Collections.singletonList(roleV2);
        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ADDED_ORGANIZATION_AUDIENCE_ROLES, addedOrgRoleList);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        when(mockedRoleManagementService.getRoleNameByRoleId(SAMPLE_ROLE_ID, SAMPLE_TENANT_DOMAIN)).
                thenReturn(SAMPLE_ROLE_NAME);

        SharedApplication sharedApplication = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_ORG_ID);
        List<SharedApplication> sharedApplications = Collections.singletonList(sharedApplication);
        when(mockedOrgApplicationManager.getSharedApplications(null, SAMPLE_MAIN_APP_ID))
                .thenReturn(sharedApplications);

        when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID)).thenReturn(
                SAMPLE_SHARED_APP_TENANT_DOMAIN);

        when(mockedRoleManagementService.isExistingRoleName(SAMPLE_ROLE_NAME, ORGANIZATION_AUD,
                SAMPLE_SHARED_APP_ORG_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(Boolean.TRUE);

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        sharedRoleMgtListener.doPostUpdateApplication(mainServiceProvider, SAMPLE_TENANT_DOMAIN, SAMPLE_USERNAME);
    }

    @Test
    public void testDoPostUpdateApplicationWithoutOrgAudienceRoleConflict() throws Exception {

        ServiceProvider mainServiceProvider = new ServiceProvider();
        mainServiceProvider.setApplicationResourceId(SAMPLE_MAIN_APP_ID);

        RoleV2 roleV2 = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        List<RoleV2> addedOrgRoleList = Collections.singletonList(roleV2);
        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ADDED_ORGANIZATION_AUDIENCE_ROLES, addedOrgRoleList);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        when(mockedRoleManagementService.getRoleNameByRoleId(SAMPLE_ROLE_ID, SAMPLE_TENANT_DOMAIN)).
                thenReturn(SAMPLE_ROLE_NAME);

        SharedApplication sharedApplication = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_ORG_ID);
        List<SharedApplication> sharedApplications = Collections.singletonList(sharedApplication);
        when(mockedOrgApplicationManager.getSharedApplications(null, SAMPLE_MAIN_APP_ID))
                .thenReturn(sharedApplications);

        when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID)).thenReturn(
                SAMPLE_SHARED_APP_TENANT_DOMAIN);

        when(mockedRoleManagementService.isExistingRoleName(SAMPLE_ROLE_NAME, ORGANIZATION_AUD,
                SAMPLE_SHARED_APP_ORG_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(Boolean.FALSE);

        RoleBasicInfo sharedRoleBasicInfo = new RoleBasicInfo();
        sharedRoleBasicInfo.setAudience(RoleConstants.ORGANIZATION);
        sharedRoleBasicInfo.setAudienceId(SAMPLE_SHARED_APP_ORG_ID);
        when(mockedRoleManagementService.addRole(roleV2.getName(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), RoleConstants.ORGANIZATION,
                SAMPLE_SHARED_APP_ORG_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(sharedRoleBasicInfo);
        doNothing().when(mockedRoleManagementService).addMainRoleToSharedRoleRelationship(anyString(), anyString(),
                anyString(), anyString());

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        sharedRoleMgtListener.doPostUpdateApplication(mainServiceProvider, SAMPLE_TENANT_DOMAIN, SAMPLE_USERNAME);
    }

    @Test(priority = 100)
    public void testDoPostUpdateApplicationWithRoleMgtExceptions() throws Exception {

        ServiceProvider mainServiceProvider = new ServiceProvider();
        mainServiceProvider.setApplicationResourceId(SAMPLE_MAIN_APP_ID);

        RoleV2 roleV2 = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        List<RoleV2> addedOrgRoleList = Collections.singletonList(roleV2);
        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ADDED_ORGANIZATION_AUDIENCE_ROLES, addedOrgRoleList);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        when(mockedRoleManagementService.getRoleNameByRoleId(roleV2.getId(), SAMPLE_TENANT_DOMAIN)).
                thenThrow(IdentityRoleManagementException.class);

        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        assertTrue(sharedRoleMgtListener.doPostUpdateApplication(mainServiceProvider, SAMPLE_TENANT_DOMAIN,
                SAMPLE_USERNAME));
    }
}
