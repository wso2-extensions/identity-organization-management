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
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Contains unit tests for SharedRoleMgtListener.
 */
@WithCarbonHome
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
    private static final String ORGANIZATION_CUSTOM_ROLES_DISABLED = "organizationCustomRolesDisabled";

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

        PrivilegedCarbonContext.startTenantFlow();
        MockitoAnnotations.openMocks(this);
        OrganizationManagementHandlerDataHolder.getInstance().
                setApplicationManagementService(mockedApplicationManagementService);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        OrganizationManagementHandlerDataHolder.getInstance().setOrgApplicationManager(mockedOrgApplicationManager);
        OrganizationManagementHandlerDataHolder.getInstance().setRoleManagementServiceV2(mockedRoleManagementService);
        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);
        identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
    }

    @AfterClass
    public void tearDown() {

        organizationManagementUtilMockedStatic.close();
        identityTenantUtilMockedStatic.close();
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

    @DataProvider(name = "organizationCustomRolesUpdateDataProvider")
    public Object[][] organizationCustomRolesUpdateDataProvider() {

        return new Object[][]{
                // isEnabledForStoredApp, isEnhancedOrgAuthEnabled, isOrganizationCustomRolesEnabled, allowedAudience,
                // isDisabled.
                {true, true, false, APPLICATION_AUD, true},
                {true, false, false, APPLICATION_AUD, true},
                {true, true, true, ORGANIZATION_AUD, true},
                {true, true, true, APPLICATION_AUD, false},
                {false, true, false, APPLICATION_AUD, false}
        };
    }

    @Test(dataProvider = "organizationCustomRolesUpdateDataProvider")
    public void testDoPreUpdateApplicationMarksOrganizationCustomRolesDisabled(boolean isEnabledForStoredApp,
                                                                              boolean isEnhancedOrgAuthEnabled,
                                                                              boolean isOrganizationCustomRolesEnabled,
                                                                              String allowedAudience,
                                                                              boolean isDisabled) throws Exception {

        IdentityUtil.threadLocalProperties.set(new HashMap<>());
        ServiceProvider storedApplication = buildMainApplication(isEnabledForStoredApp);
        when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(storedApplication);

        ServiceProvider updatedApplication = buildMainApplication(true);
        updatedApplication.setApplicationResourceId(SAMPLE_MAIN_APP_ID);
        updatedApplication.setEnhancedOrganizationAuthenticationEnabled(isEnhancedOrgAuthEnabled);
        updatedApplication.setOrganizationCustomRolesEnabled(isOrganizationCustomRolesEnabled);
        updatedApplication.getAssociatedRolesConfig().setAllowedAudience(allowedAudience);
        try {
            assertTrue(new SharedRoleMgtListener().doPreUpdateApplication(updatedApplication, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME));

            // The update is marked only if organizations can no longer create custom application roles.
            assertEquals(Boolean.TRUE.equals(IdentityUtil.threadLocalProperties.get()
                    .get(ORGANIZATION_CUSTOM_ROLES_DISABLED)), isDisabled);
        } finally {
            IdentityUtil.threadLocalProperties.set(new HashMap<>());
            // The application management service mock is shared by the other tests.
            when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID,
                    SAMPLE_TENANT_DOMAIN)).thenReturn(null);
        }
    }

    @Test
    public void testDoPreUpdateApplicationDoesNotMarkOrganizationAudienceRoleManagementDisabled() throws Exception {

        IdentityUtil.threadLocalProperties.set(new HashMap<>());
        // The stored main application uses organization audience roles and enables organization custom roles.
        ServiceProvider storedApplication = buildMainApplication(true);
        storedApplication.getAssociatedRolesConfig().setAllowedAudience(RoleConstants.ORGANIZATION);
        when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(storedApplication);

        ServiceProvider updatedApplication = buildMainApplication(true);
        updatedApplication.setApplicationResourceId(SAMPLE_MAIN_APP_ID);
        updatedApplication.getAssociatedRolesConfig().setAllowedAudience(RoleConstants.ORGANIZATION);
        updatedApplication.setOrganizationCustomRolesEnabled(false);
        try {
            assertTrue(new SharedRoleMgtListener().doPreUpdateApplication(updatedApplication, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME));

            // Organization roles created by organizations are not deleted. They are only no longer honoured.
            assertFalse(IdentityUtil.threadLocalProperties.get().containsKey(ORGANIZATION_CUSTOM_ROLES_DISABLED));
        } finally {
            IdentityUtil.threadLocalProperties.set(new HashMap<>());
            // The application management service mock is shared by the other tests.
            when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID,
                    SAMPLE_TENANT_DOMAIN)).thenReturn(null);
        }
    }

    @Test
    public void testDoPostUpdateApplicationDeletesOrganizationCustomRolesWhenDisabled() throws Exception {

        clearInvocations(mockedRoleManagementService);
        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ORGANIZATION_CUSTOM_ROLES_DISABLED, true);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        SharedApplication sharedApplication = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_ORG_ID);
        when(mockedOrganizationManager.resolveOrganizationId(SAMPLE_TENANT_DOMAIN)).thenReturn(SAMPLE_ORG_ID);
        when(mockedOrgApplicationManager.getSharedApplications(SAMPLE_ORG_ID, SAMPLE_MAIN_APP_ID))
                .thenReturn(Collections.singletonList(sharedApplication));
        when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID))
                .thenReturn(SAMPLE_SHARED_APP_TENANT_DOMAIN);
        // Roles of the shared application: an inherited role and a role created by the organization.
        RoleBasicInfo inheritedRole = new RoleBasicInfo(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_NAME);
        RoleBasicInfo localRole = new RoleBasicInfo("local-role-id", "local-role");
        when(mockedRoleManagementService.getRolesByApplication(SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(Arrays.asList(inheritedRole, localRole));
        when(mockedRoleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(
                Arrays.asList(SAMPLE_SHARED_ROLE_ID, "local-role-id"), SAMPLE_SHARED_APP_TENANT_DOMAIN))
                .thenReturn(Collections.singletonMap(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_ID));

        ServiceProvider mainApplication = buildMainApplication(false);
        mainApplication.setApplicationResourceId(SAMPLE_MAIN_APP_ID);
        try (MockedStatic<IdentityUtil> identityUtil = mockStatic(IdentityUtil.class, CALLS_REAL_METHODS)) {
            identityUtil.when(() -> IdentityUtil.getInitiatorId(any(), any())).thenReturn(SAMPLE_USERNAME);
            assertTrue(new SharedRoleMgtListener().doPostUpdateApplication(mainApplication, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME));
        }

        // Only the role created by the organization is deleted. The roles are deleted asynchronously.
        verify(mockedRoleManagementService, timeout(5000)).deleteRole("local-role-id",
                SAMPLE_SHARED_APP_TENANT_DOMAIN);
        verify(mockedRoleManagementService, never()).deleteRole(SAMPLE_SHARED_ROLE_ID,
                SAMPLE_SHARED_APP_TENANT_DOMAIN);
        assertFalse(IdentityUtil.threadLocalProperties.get().containsKey(ORGANIZATION_CUSTOM_ROLES_DISABLED));
    }

    @Test
    public void testDoPostUpdateApplicationContinuesWhenOrganizationCustomRoleDeletionFails() throws Exception {

        clearInvocations(mockedRoleManagementService);
        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ORGANIZATION_CUSTOM_ROLES_DISABLED, true);
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        SharedApplication sharedApplication = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_ORG_ID);
        when(mockedOrganizationManager.resolveOrganizationId(SAMPLE_TENANT_DOMAIN)).thenReturn(SAMPLE_ORG_ID);
        when(mockedOrgApplicationManager.getSharedApplications(SAMPLE_ORG_ID, SAMPLE_MAIN_APP_ID))
                .thenReturn(Collections.singletonList(sharedApplication));
        when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID))
                .thenReturn(SAMPLE_SHARED_APP_TENANT_DOMAIN);
        // Two roles created by the organization, where the deletion of the first one fails.
        RoleBasicInfo failingRole = new RoleBasicInfo("failing-local-role-id", "failing-local-role");
        RoleBasicInfo localRole = new RoleBasicInfo("next-local-role-id", "next-local-role");
        when(mockedRoleManagementService.getRolesByApplication(SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(Arrays.asList(failingRole, localRole));
        when(mockedRoleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(
                Arrays.asList("failing-local-role-id", "next-local-role-id"), SAMPLE_SHARED_APP_TENANT_DOMAIN))
                .thenReturn(Collections.emptyMap());
        doThrow(new IdentityRoleManagementException("Role not found.")).when(mockedRoleManagementService)
                .deleteRole("failing-local-role-id", SAMPLE_SHARED_APP_TENANT_DOMAIN);

        ServiceProvider mainApplication = buildMainApplication(false);
        mainApplication.setApplicationResourceId(SAMPLE_MAIN_APP_ID);
        try (MockedStatic<IdentityUtil> identityUtil = mockStatic(IdentityUtil.class, CALLS_REAL_METHODS)) {
            identityUtil.when(() -> IdentityUtil.getInitiatorId(any(), any())).thenReturn(SAMPLE_USERNAME);
            assertTrue(new SharedRoleMgtListener().doPostUpdateApplication(mainApplication, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME));
        }

        // The failed role does not block the deletion of the next role.
        verify(mockedRoleManagementService, timeout(5000)).deleteRole("next-local-role-id",
                SAMPLE_SHARED_APP_TENANT_DOMAIN);
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

    @Test
    public void testDoPostGetAssociatedRolesOfApplicationForApplicationAudience() throws Exception {

        clearInvocations(mockedApplicationManagementService, mockedRoleManagementService);
        when(mockedApplicationManagementService.getMainAppId(SAMPLE_SHARED_APP_ID)).thenReturn(SAMPLE_MAIN_APP_ID);
        when(mockedApplicationManagementService.getTenantIdByApp(SAMPLE_MAIN_APP_ID)).thenReturn(SAMPLE_TENANT_ID);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID)).
                thenReturn(SAMPLE_TENANT_DOMAIN);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);
        when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(buildMainApplication(true));

        // Application roles of the shared application: an inherited role and a role created by the organization.
        when(mockedRoleManagementService.getRolesByApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN))
                .thenReturn(Arrays.asList(new RoleBasicInfo(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_NAME),
                        new RoleBasicInfo("local-role-id", "local-role")));

        // Roles resolved by the application management component, which are replaced by the listener.
        List<RoleV2> associatedRoles = new ArrayList<>(Collections.singletonList(
                new RoleV2(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_NAME)));
        SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
        assertTrue(sharedRoleMgtListener.doPostGetAssociatedRolesOfApplication(associatedRoles, SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_TENANT_DOMAIN));

        // The roles are resolved by the role audience, without resolving through the main application roles.
        assertEquals(associatedRoles.stream().map(RoleV2::getId).toArray(String[]::new),
                new String[]{SAMPLE_SHARED_ROLE_ID, "local-role-id"});
        assertEquals(associatedRoles.get(1).getName(), "local-role");
        verify(mockedApplicationManagementService, never()).getAssociatedRolesOfApplication(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN);
        verify(mockedRoleManagementService, never()).getMainRoleToSharedRoleMappingsBySubOrg(anyList(),
                anyString());
    }

    @DataProvider(name = "organizationAudienceRoleManagementDataProvider")
    public Object[][] organizationAudienceRoleManagementDataProvider() {

        return new Object[][]{
                // isOrganizationCustomRolesEnabled, expected role IDs.
                {true, new String[]{"org-role-id"}},
                {false, new String[]{SAMPLE_SHARED_ROLE_ID}}
        };
    }

    @Test(dataProvider = "organizationAudienceRoleManagementDataProvider")
    public void testDoPostGetAssociatedRolesOfApplicationForOrganizationAudience(
            boolean isOrganizationCustomRolesEnabled, String[] expectedRoleIds) throws Exception {

        when(mockedApplicationManagementService.getMainAppId(SAMPLE_SHARED_APP_ID)).thenReturn(SAMPLE_MAIN_APP_ID);
        when(mockedApplicationManagementService.getTenantIdByApp(SAMPLE_MAIN_APP_ID)).thenReturn(SAMPLE_TENANT_ID);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID)).
                thenReturn(SAMPLE_TENANT_DOMAIN);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN)).thenReturn(ORGANIZATION_AUD);
        ServiceProvider organizationAudienceMainApplication = buildMainApplication(isOrganizationCustomRolesEnabled);
        organizationAudienceMainApplication.getAssociatedRolesConfig().setAllowedAudience(RoleConstants.ORGANIZATION);
        when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(organizationAudienceMainApplication);
        when(mockedApplicationManagementService.getAssociatedRolesOfApplication(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN)).thenReturn(Collections.singletonList(
                        new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME)));
        Map<String, String> mainRoleToSharedRoleMappings = new HashMap<>();
        mainRoleToSharedRoleMappings.put(SAMPLE_ROLE_ID, SAMPLE_SHARED_ROLE_ID);
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(SAMPLE_ROLE_ID), SAMPLE_SHARED_APP_TENANT_DOMAIN))
                .thenReturn(mainRoleToSharedRoleMappings);

        // Organization roles of the organization, including a role created by the organization.
        when(mockedRoleManagementService.getRoles(anyString(), eq(100), eq(1), eq(null), eq(null),
                eq(SAMPLE_SHARED_APP_TENANT_DOMAIN))).thenReturn(Collections.singletonList(
                        new RoleBasicInfo("org-role-id", "org-role")));

        List<RoleV2> associatedRoles = new ArrayList<>(Collections.singletonList(
                new RoleV2("stale-role-id", "stale-role")));
        try (MockedStatic<IdentityUtil> identityUtil = mockStatic(IdentityUtil.class, CALLS_REAL_METHODS)) {
            identityUtil.when(IdentityUtil::getMaximumItemPerPage).thenReturn(100);
            SharedRoleMgtListener sharedRoleMgtListener = new SharedRoleMgtListener();
            assertTrue(sharedRoleMgtListener.doPostGetAssociatedRolesOfApplication(associatedRoles,
                    SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN));
        }

        /*
         When the main application enables organization custom roles, the roles are all the organization roles of
         the organization, including the ones created by the organization. Otherwise, they are resolved through the
         main application roles, as before.
         */
        assertEquals(associatedRoles.stream().map(RoleV2::getId).toArray(String[]::new), expectedRoleIds);
    }

    @Test
    public void testDoPostGetAssociatedRolesOfApplicationWhenRoleCreationNotAllowed() throws Exception {

        when(mockedApplicationManagementService.getMainAppId(SAMPLE_SHARED_APP_ID)).thenReturn(SAMPLE_MAIN_APP_ID);
        when(mockedApplicationManagementService.getTenantIdByApp(SAMPLE_MAIN_APP_ID)).thenReturn(SAMPLE_TENANT_ID);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID)).
                thenReturn(SAMPLE_TENANT_DOMAIN);
        when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(buildMainApplication(false));
        when(mockedApplicationManagementService.getAssociatedRolesOfApplication(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN)).thenReturn(Collections.singletonList(
                        new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME)));
        when(mockedRoleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(SAMPLE_ROLE_ID), SAMPLE_SHARED_APP_TENANT_DOMAIN))
                .thenReturn(Collections.singletonMap(SAMPLE_ROLE_ID, SAMPLE_SHARED_ROLE_ID));

        List<RoleV2> associatedRoles = new ArrayList<>(Arrays.asList(
                new RoleV2(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_NAME), new RoleV2("local-role-id", "local-role")));
        assertTrue(new SharedRoleMgtListener().doPostGetAssociatedRolesOfApplication(associatedRoles,
                SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN));

        // When the main application does not allow role creation, the roles are resolved as before.
        assertEquals(associatedRoles.size(), 1);
        assertEquals(associatedRoles.get(0).getId(), SAMPLE_SHARED_ROLE_ID);
    }

    @Test
    public void testDoPostUpdateApplicationWithAddedAppRoleMarksSharedRoleProvisioning() throws Exception {

        clearInvocations(mockedRoleManagementService);
        RoleV2 mainRole = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        mockAddedAppRoleSharing(mainRole);
        when(mockedRoleManagementService.isExistingRoleName(SAMPLE_ROLE_NAME, RoleConstants.APPLICATION,
                SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(false);
        RoleBasicInfo sharedRoleBasicInfo = new RoleBasicInfo(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_NAME);
        List<Object> provisioningFlowMarkers = new ArrayList<>();
        when(mockedRoleManagementService.addRole(SAMPLE_ROLE_NAME, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), RoleConstants.APPLICATION, SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenAnswer(invocation -> {
                    provisioningFlowMarkers.add(IdentityUtil.threadLocalProperties.get()
                            .get(RoleConstants.IS_SHARED_ROLE_PROVISIONING_FLOW));
                    return sharedRoleBasicInfo;
                });

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationResourceId(SAMPLE_MAIN_APP_ID);
        new SharedRoleMgtListener().doPostUpdateApplication(serviceProvider, SAMPLE_TENANT_DOMAIN, SAMPLE_USERNAME);

        // The inherited role is created within the shared role provisioning flow.
        assertEquals(provisioningFlowMarkers, Collections.singletonList(true));
        assertFalse(IdentityUtil.threadLocalProperties.get()
                .containsKey(RoleConstants.IS_SHARED_ROLE_PROVISIONING_FLOW));
        verify(mockedRoleManagementService, times(1)).addMainRoleToSharedRoleRelationship(SAMPLE_ROLE_ID,
                SAMPLE_SHARED_ROLE_ID, SAMPLE_TENANT_DOMAIN, SAMPLE_SHARED_APP_TENANT_DOMAIN);
    }

    @Test
    public void testDoPostUpdateApplicationWithAddedAppRoleConflictingWithOrgRole() throws Exception {

        clearInvocations(mockedRoleManagementService);
        RoleV2 mainRole = new RoleV2(SAMPLE_ROLE_ID, SAMPLE_ROLE_NAME);
        mockAddedAppRoleSharing(mainRole);
        // The organization has authored a role with the same name for the shared application.
        when(mockedRoleManagementService.isExistingRoleName(SAMPLE_ROLE_NAME, RoleConstants.APPLICATION,
                SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(true);

        ServiceProvider serviceProvider = buildMainApplication(true);
        serviceProvider.setApplicationResourceId(SAMPLE_MAIN_APP_ID);
        try (MockedStatic<LoggerUtils> loggerUtils = mockStatic(LoggerUtils.class);
             MockedStatic<IdentityUtil> identityUtil = mockStatic(IdentityUtil.class, CALLS_REAL_METHODS)) {
            identityUtil.when(() -> IdentityUtil.getInitiatorId(any(), any())).thenReturn(SAMPLE_USERNAME);
            assertTrue(new SharedRoleMgtListener().doPostUpdateApplication(serviceProvider, SAMPLE_TENANT_DOMAIN,
                    SAMPLE_USERNAME));
            loggerUtils.verify(() -> LoggerUtils.triggerAuditLogEvent(any(), anyBoolean()), times(1));
        }

        // The main application update does not fail and the conflicting role is skipped.
        verify(mockedRoleManagementService, never()).addRole(anyString(), anyList(), anyList(), anyList(),
                eq(RoleConstants.APPLICATION), eq(SAMPLE_SHARED_APP_ID), anyString());
        verify(mockedRoleManagementService, never()).addMainRoleToSharedRoleRelationship(eq(SAMPLE_ROLE_ID),
                anyString(), anyString(), anyString());
    }

    @Test
    public void testDoPreDeleteApplicationAuditsOrganizationCustomRoles() throws Exception {

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(SAMPLE_APPLICATION_NAME);
        serviceProvider.setApplicationResourceId(SAMPLE_SHARED_APP_ID);
        ServiceProviderProperty serviceProviderProperty = new ServiceProviderProperty();
        serviceProviderProperty.setName(IS_FRAGMENT_APP);
        serviceProviderProperty.setValue(Boolean.TRUE.toString());
        serviceProvider.setSpProperties(new ServiceProviderProperty[]{serviceProviderProperty});

        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(true);
        when(mockedApplicationManagementService.getServiceProvider(SAMPLE_APPLICATION_NAME,
                SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(serviceProvider);
        when(mockedOrganizationManager.resolveOrganizationId(SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(
                SAMPLE_SHARED_APP_ORG_ID);
        when(mockedOrgApplicationManager.getMainApplicationIdForGivenSharedApp(SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_ORG_ID)).thenReturn(SAMPLE_MAIN_APP_ID);
        when(mockedApplicationManagementService.getTenantIdByApp(SAMPLE_MAIN_APP_ID)).thenReturn(SAMPLE_TENANT_ID);
        identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(SAMPLE_TENANT_ID)).
                thenReturn(SAMPLE_TENANT_DOMAIN);
        when(mockedApplicationManagementService.getAllowedAudienceForRoleAssociation(SAMPLE_MAIN_APP_ID,
                SAMPLE_TENANT_DOMAIN)).thenReturn(APPLICATION_AUD);
        when(mockedApplicationManagementService.getApplicationByResourceId(SAMPLE_MAIN_APP_ID, SAMPLE_TENANT_DOMAIN))
                .thenReturn(buildMainApplication(true));

        RoleBasicInfo inheritedRole = new RoleBasicInfo(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_NAME);
        RoleBasicInfo localRole = new RoleBasicInfo("local-role-id", "local-role");
        when(mockedRoleManagementService.getRolesByApplication(SAMPLE_SHARED_APP_ID,
                SAMPLE_SHARED_APP_TENANT_DOMAIN)).thenReturn(Arrays.asList(inheritedRole, localRole));
        Map<String, String> sharedRoleToMainRoleMappings = new HashMap<>();
        sharedRoleToMainRoleMappings.put(SAMPLE_SHARED_ROLE_ID, SAMPLE_ROLE_ID);
        when(mockedRoleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(
                Arrays.asList(SAMPLE_SHARED_ROLE_ID, "local-role-id"), SAMPLE_SHARED_APP_TENANT_DOMAIN))
                .thenReturn(sharedRoleToMainRoleMappings);

        try (MockedStatic<LoggerUtils> loggerUtils = mockStatic(LoggerUtils.class);
             MockedStatic<IdentityUtil> identityUtil = mockStatic(IdentityUtil.class, CALLS_REAL_METHODS)) {
            identityUtil.when(() -> IdentityUtil.getInitiatorId(any(), any())).thenReturn(SAMPLE_USERNAME);
            assertTrue(new SharedRoleMgtListener().doPreDeleteApplication(SAMPLE_APPLICATION_NAME,
                    SAMPLE_SHARED_APP_TENANT_DOMAIN, SAMPLE_USERNAME));
            // Only the organization authored role is audited.
            loggerUtils.verify(() -> LoggerUtils.triggerAuditLogEvent(any(), anyBoolean()), times(1));
        }
    }

    private void mockAddedAppRoleSharing(RoleV2 mainRole) throws Exception {

        Map<String, Object> threadLocalProperties = new HashMap<>();
        threadLocalProperties.put(ADDED_APPLICATION_AUDIENCE_ROLES, Collections.singletonList(mainRole));
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        SharedApplication sharedApplication = new SharedApplication(SAMPLE_SHARED_APP_ID, SAMPLE_SHARED_APP_ORG_ID);
        when(mockedOrganizationManager.resolveOrganizationId(SAMPLE_TENANT_DOMAIN)).thenReturn(SAMPLE_ORG_ID);
        when(mockedOrgApplicationManager.getSharedApplications(SAMPLE_ORG_ID, SAMPLE_MAIN_APP_ID)).
                thenReturn(Collections.singletonList(sharedApplication));
        when(mockedOrganizationManager.resolveTenantDomain(SAMPLE_SHARED_APP_ORG_ID)).
                thenReturn(SAMPLE_SHARED_APP_TENANT_DOMAIN);
    }

    /**
     * Build a main application which allows organizations to create application roles for its shared applications.
     *
     * @param isOrganizationCustomRolesEnabled Whether application role creation is enabled.
     * @return Main application.
     */
    private static ServiceProvider buildMainApplication(boolean isOrganizationCustomRolesEnabled) {

        ServiceProvider mainApplication = new ServiceProvider();
        mainApplication.setEnhancedOrganizationAuthenticationEnabled(isOrganizationCustomRolesEnabled);
        mainApplication.setOrganizationCustomRolesEnabled(isOrganizationCustomRolesEnabled);
        AssociatedRolesConfig associatedRolesConfig = new AssociatedRolesConfig();
        associatedRolesConfig.setAllowedAudience(RoleConstants.APPLICATION);
        mainApplication.setAssociatedRolesConfig(associatedRolesConfig);
        return mainApplication;
    }
}
