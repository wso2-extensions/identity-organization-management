/*
 * Copyright (c) 2023-2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.listener;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.model.script.AuthenticationScriptConfig;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.IS_API_BASED_AUTHENTICATION_ENABLED_PROPERTY_NAME;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_FRAGMENT_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_SHARE_FOR_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.IS_APP_SHARED;

/**
 * Test class for FragmentApplicationMgtListener.
 */
public class FragmentApplicationMgtListenerTest {

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private OrgApplicationMgtDAO orgApplicationMgtDAO;

    @Mock
    private ApplicationManagementService applicationManagementService;

    @Mock
    private SharedApplicationDO sharedApplicationDO;

    @InjectMocks
    private FragmentApplicationMgtListener fragmentApplicationMgtListener;

    private MockedStatic<IdentityTenantUtil> mockedUtilities;

    private ServiceProvider serviceProvider;
    private static final String primaryTenantDomain = "primaryTenantDomain";
    private static final String tenantDomain = "sampleTenantDomain";
    private static final String userName = "sampleUser";
    private static final String applicationName = "sampleApp";
    private static final String updatedApplicationName = "updatedSampleApp";
    private static final String applicationResourceID = "fcb0c1d7-28c0-46f7-bc2d-345678a1b23c";
    private static final String organizationID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String sampleClaimURI = "http://wso2.org/claims/sampleClaim1";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        serviceProvider = mock(ServiceProvider.class);
        sharedApplicationDO = mock(SharedApplicationDO.class);
        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrgApplicationMgtDataHolder.getInstance().setOrgApplicationMgtDAO(orgApplicationMgtDAO);
        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
        mockedUtilities = Mockito.mockStatic(IdentityTenantUtil.class, Mockito.withSettings()
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @DataProvider(name = "subOrganizationMetaData")
    public Object[][] getSubOrganizationMetaData() {

        return new Object[][]{
                // Create application in sub-organization.
                {"orgId2", 2, tenantDomain, false, false},
                // Create an application in a sub-organization, and it's marked as a fragment app.
                {"orgId3", 2, tenantDomain, true, true},
                // Create an application marked as a fragmented app by an internal process of primaryTenantDomain.
                {"orgId4", 2, primaryTenantDomain, true, false}
        };
    }

    @Test(dataProvider = "subOrganizationMetaData")
    public void testCreateApplicationInSubOrg(String organizationId, int hierarchyDepth, String requestInitiatedDomain,
                                              boolean isSharedApp, boolean expectException)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationId);
        when(organizationManager.getOrganizationDepthInHierarchy(organizationId)).thenReturn(hierarchyDepth);
        if (isSharedApp) {
            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                    mockServiceProviderProperty(IS_FRAGMENT_APP, TRUE),
            };
            when(serviceProvider.getSpProperties()).thenReturn(spProperties);
        }
        mockUtils(requestInitiatedDomain);
        try {
            boolean result =
                    fragmentApplicationMgtListener.doPreCreateApplication(serviceProvider, tenantDomain, userName);
            assertEquals(result, !expectException);
        } catch (IdentityApplicationManagementClientException e) {
            assertTrue(expectException);
        }
    }

    @Test
    public void testCreateApplicationInPrimaryOrg()
            throws IdentityApplicationManagementException, OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(primaryTenantDomain)).thenReturn("orgId1");
        when(organizationManager.getOrganizationDepthInHierarchy("orgId1")).thenReturn(0);
        mockUtils(primaryTenantDomain);
        boolean result =
                fragmentApplicationMgtListener.doPreCreateApplication(serviceProvider, primaryTenantDomain, userName);
        assertTrue(result);
    }

    @DataProvider(name = "testAPIBasedAuthPropertyInheritanceDataProvider")
    public Object[][] testAPIBasedAuthPropertyInheritanceDataProvider() {

        return new Object[][]{
                // Both shared app and main app API Based Authentication property is enabled.
                {true, true, true},
                // Only main app API Based Authentication property is enabled.
                {true, false, true},
                // Only shared app API Based Authentication property is enabled.
                {false, true, false},
                // Both shared app and main app API Based Authentication property is disabled.
                {false, false, false}
        };
    }

    @Test(dataProvider = "testAPIBasedAuthPropertyInheritanceDataProvider")
    public void testAPIBasedAuthPropertyInheritance(boolean isMainAppAPIAuthEnabled, boolean isSharedAppAPIAuthEnabled,
                                                    boolean expectedResult)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        mockClaimConfig(serviceProvider);
        mockUtils(primaryTenantDomain);

        MainApplicationDO mainApplicationDO = new MainApplicationDO(organizationID, applicationResourceID);
        ServiceProvider sharedApplication = new ServiceProvider();
        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                mockServiceProviderProperty(IS_FRAGMENT_APP, TRUE),
                mockServiceProviderProperty(IS_API_BASED_AUTHENTICATION_ENABLED_PROPERTY_NAME,
                        String.valueOf(isSharedAppAPIAuthEnabled))
        };
        ServiceProviderProperty[] mainSPProperties = new ServiceProviderProperty[]{
                mockServiceProviderProperty(IS_API_BASED_AUTHENTICATION_ENABLED_PROPERTY_NAME,
                        String.valueOf(isMainAppAPIAuthEnabled))
        };
        sharedApplication.setSpProperties(spProperties);
        sharedApplication.setLocalAndOutBoundAuthenticationConfig(new LocalAndOutboundAuthenticationConfig());
        sharedApplication.setApplicationResourceId(applicationResourceID);

        when(serviceProvider.getSpProperties()).thenReturn(mainSPProperties);
        when(serviceProvider.isAPIBasedAuthenticationEnabled()).thenReturn(isMainAppAPIAuthEnabled);
        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationID);
        when(organizationManager.resolveTenantDomain(organizationID)).thenReturn(tenantDomain);
        when(orgApplicationMgtDAO.getMainApplication(applicationResourceID, organizationID))
                .thenReturn(Optional.of(mainApplicationDO));
        when(applicationManagementService.getApplicationByResourceId(anyString(), anyString()))
                .thenReturn(serviceProvider);

        boolean result = fragmentApplicationMgtListener
                .doPostGetServiceProvider(sharedApplication, applicationName, tenantDomain);
        AssertJUnit.assertTrue(result);
        AssertJUnit.assertEquals(expectedResult, sharedApplication.isAPIBasedAuthenticationEnabled());
    }

    @DataProvider(name = "testSharedAppNameModificationDataProvider")
    public Object[][] testSharedAppNameModificationDataProvider() {

        return new Object[][]{
                {TRUE, true}, {TRUE, false}, {FALSE, true}, {FALSE, false},
        };
    }

    @Test(dataProvider = "testSharedAppNameModificationDataProvider")
    public void testSharedAppNameModificationNotAllowed(String isSharedApp, boolean isAppNameUpdated)
            throws IdentityApplicationManagementException {

        try (MockedStatic<OrganizationManagementUtil> organizationManagementUtil =
                     mockStatic(OrganizationManagementUtil.class)) {

            mockUtils(tenantDomain);
            organizationManagementUtil.when(() -> OrganizationManagementUtil.isOrganization(tenantDomain))
                    .thenReturn(true);

            ServiceProvider sharedApplication = new ServiceProvider();
            sharedApplication.setApplicationName(isAppNameUpdated ? updatedApplicationName : applicationName);

            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                    mockServiceProviderProperty(IS_FRAGMENT_APP, isSharedApp)
            };
            when(serviceProvider.getSpProperties()).thenReturn(spProperties);
            when(serviceProvider.getApplicationName()).thenReturn(applicationName);
            when(applicationManagementService.getApplicationByResourceId(any(), anyString()))
                    .thenReturn(serviceProvider);

            if (Boolean.parseBoolean(isSharedApp) && isAppNameUpdated) {
                IdentityApplicationManagementClientException thrownException =
                        Assert.expectThrows(IdentityApplicationManagementClientException.class,
                                () -> fragmentApplicationMgtListener
                                        .doPreUpdateApplication(sharedApplication, tenantDomain, userName));

                assertEquals("Application name modification is not allowed for shared applications.",
                        thrownException.getMessage());
            } else {
                sharedApplication.setLocalAndOutBoundAuthenticationConfig(new LocalAndOutboundAuthenticationConfig());

                boolean result = fragmentApplicationMgtListener
                        .doPreUpdateApplication(sharedApplication, tenantDomain, userName);
                assertTrue(result);
            }
        }
    }

    @DataProvider(name = "testFragmentApplicationPreDeletionDataProvider")
    public Object[][] testFragmentApplicationPreDeletionDataProvider() {

        return new Object[][]{
                {new String[]{DELETE_MAIN_APPLICATION}, false, true, false},
                {new String[]{DELETE_SHARE_FOR_MAIN_APPLICATION}, false, true, false},
                {new String[]{DELETE_FRAGMENT_APPLICATION}, false, true, false},
                {new String[]{DELETE_FRAGMENT_APPLICATION}, true, true, true},
                {new String[]{}, true, true, true},
                {new String[]{}, false, true, true},
                {new String[]{}, false, false, false},
        };
    }

    @Test(dataProvider = "testFragmentApplicationPreDeletionDataProvider")
    public void testFragmentApplicationPreDeletion(String[] threadLocalPropertiesSet, boolean isShareWithAllChildren,
                                                   boolean isSharedApplicationPresent, boolean isExceptionExpected)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        Map<String, Object> threadLocalProperties = new HashMap<>();
        for (String property : threadLocalPropertiesSet) {
            threadLocalProperties.put(property, true);
        }
        IdentityUtil.threadLocalProperties.set(threadLocalProperties);

        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                mockServiceProviderProperty(IS_FRAGMENT_APP, TRUE)
        };
        when(serviceProvider.getSpProperties()).thenReturn(spProperties);
        when(applicationManagementService.getServiceProvider(applicationName, tenantDomain))
                .thenReturn(serviceProvider);
        when(serviceProvider.getApplicationID()).thenReturn(1);
        when(serviceProvider.getApplicationResourceId()).thenReturn(applicationResourceID);
        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationID);

        if (isSharedApplicationPresent) {
            when(orgApplicationMgtDAO.getSharedApplication(1, organizationID))
                    .thenReturn(Optional.ofNullable(sharedApplicationDO));
            when(sharedApplicationDO.shareWithAllChildren()).thenReturn(isShareWithAllChildren);
        }
        if (isExceptionExpected) {
            IdentityApplicationManagementClientException thrownException =
                    Assert.expectThrows(IdentityApplicationManagementClientException.class,
                            () -> fragmentApplicationMgtListener
                                    .doPreDeleteApplication(applicationName, tenantDomain, userName));

            assertEquals(String.format("Cannot delete shared application with resource id: %s. " +
                            "Delete is allowed only when the main application is deleted, or its sharing is revoked.",
                    applicationResourceID), thrownException.getMessage());
        } else {
            boolean result = fragmentApplicationMgtListener
                    .doPreDeleteApplication(applicationName, tenantDomain, userName);
            assertTrue(result);
        }
    }

    private void mockClaimConfig(ServiceProvider mainApplication) {

        ClaimConfig claimConfig = mock(ClaimConfig.class);
        ClaimMapping claimMapping1 = mock(ClaimMapping.class);
        Claim claim1 = new Claim();
        claim1.setClaimUri(sampleClaimURI);

        when(mainApplication.getClaimConfig()).thenReturn(claimConfig);
        when(claimConfig.getClaimMappings()).thenReturn(new ClaimMapping[]{claimMapping1});
        when(claimMapping1.getLocalClaim()).thenReturn(claim1);
        when(claimConfig.isAlwaysSendMappedLocalSubjectId()).thenReturn(true);
    }

    private ServiceProviderProperty mockServiceProviderProperty(String name, String value) {

        ServiceProviderProperty property = new ServiceProviderProperty();
        property.setName(name);
        property.setValue(String.valueOf(value));
        return property;
    }

    @Test
    public void testInheritDiscoverabilityProperty() throws Exception {

        ServiceProvider mainApplication = mock(ServiceProvider.class);
        when(mainApplication.isDiscoverable()).thenReturn(true);
        when(serviceProvider.isDiscoverable()).thenReturn(false);
        Method method = fragmentApplicationMgtListener.getClass()
                .getDeclaredMethod("inheritDiscoverabilityProperty", ServiceProvider.class, ServiceProvider.class);
        method.setAccessible(true);
        method.invoke(fragmentApplicationMgtListener, mainApplication, serviceProvider);
        verify(serviceProvider).setDiscoverable(true);
    }

    private void mockUtils(String requestInitiatedDomain) {

        mockedUtilities.when(() -> IdentityTenantUtil.getTenantDomainFromContext()).thenReturn(requestInitiatedDomain);
    }

    @DataProvider(name = "testClaimConfigInheritanceDataProvider")
    public Object[][] testClaimConfigInheritanceDataProvider() {

        return new Object[][] {
                // supportApplicationRoles, alwaysSendMappedLocalSubjectId, mappedLocalSubjectMandatory
                {true, true, false},
                {false, false, true}
        };
    }

    @Test(dataProvider = "testClaimConfigInheritanceDataProvider")
    public void testClaimConfigInheritance(boolean supportAppRoles, boolean alwaysSendMappedLocalSubjectId,
                                           boolean mappedLocalSubjectMandatory) throws Exception {

        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {

            utilsMockedStatic.when(Utils::isB2BApplicationRoleSupportEnabled).thenReturn(supportAppRoles);
            ServiceProvider sharedSP = new ServiceProvider();
            sharedSP.setApplicationResourceId(applicationResourceID);
            sharedSP.setLocalAndOutBoundAuthenticationConfig(new LocalAndOutboundAuthenticationConfig());
            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[] {
                    mockServiceProviderProperty(IS_FRAGMENT_APP, TRUE)
            };
            sharedSP.setSpProperties(spProperties);

            ClaimMapping[] claimMappings = {
                    ClaimMapping.build("http://wso2.org/claims/email", null, null, false),
                    ClaimMapping.build("http://wso2.org/claims/runtime/temp", null, null, false)
            };
            ClaimConfig mainClaimConfig = new ClaimConfig();
            mainClaimConfig.setClaimMappings(claimMappings);
            mainClaimConfig.setAlwaysSendMappedLocalSubjectId(alwaysSendMappedLocalSubjectId);
            mainClaimConfig.setMappedLocalSubjectMandatory(mappedLocalSubjectMandatory);

            MainApplicationDO mainApplicationDO = new MainApplicationDO(organizationID, applicationResourceID);

            ServiceProvider mainSp = new ServiceProvider();
            mainSp.setClaimConfig(mainClaimConfig);
            mainSp.setSpProperties(new ServiceProviderProperty[0]);

            when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationID);
            when(orgApplicationMgtDAO.getMainApplication(applicationResourceID, organizationID)).thenReturn(
                    Optional.of(mainApplicationDO));
            when(organizationManager.resolveTenantDomain(organizationID)).thenReturn(tenantDomain);
            when(applicationManagementService.getApplicationByResourceId(any(), anyString())).thenReturn(mainSp);

            fragmentApplicationMgtListener.doPostGetServiceProvider(sharedSP, applicationName, tenantDomain);

            ClaimConfig resultClaimConfig = sharedSP.getClaimConfig();
            Assert.assertNotNull(resultClaimConfig);

            // Filter runtime claim.
            Assert.assertEquals(resultClaimConfig.getClaimMappings()[0].getLocalClaim().getClaimUri(),
                                "http://wso2.org/claims/email");

            if (supportAppRoles) {
                Assert.assertEquals(resultClaimConfig.getClaimMappings().length, 3);
                // Verify adding the application roles claim.
                Assert.assertEquals(resultClaimConfig.getClaimMappings()[1].getLocalClaim().getClaimUri(),
                                    "http://wso2.org/claims/applicationRoles");
                // Verify adding the roles claim.
                Assert.assertEquals(resultClaimConfig.getClaimMappings()[2].getLocalClaim().getClaimUri(),
                                    "http://wso2.org/claims/roles");
            } else {
                Assert.assertEquals(resultClaimConfig.getClaimMappings().length, 2);
                // Verify adding the roles claim.
                Assert.assertEquals(resultClaimConfig.getClaimMappings()[1].getLocalClaim().getClaimUri(),
                                    "http://wso2.org/claims/roles");
            }

            Assert.assertEquals(resultClaimConfig.isAlwaysSendMappedLocalSubjectId(), alwaysSendMappedLocalSubjectId);
            Assert.assertEquals(resultClaimConfig.isMappedLocalSubjectMandatory(), mappedLocalSubjectMandatory);
        }
    }

    @DataProvider(name = "adaptiveAuthForSharedAppsDataProvider")
    public Object[][] adaptiveAuthForSharedAppsDataProvider() {

        return new Object[][]{
                {false, true}, // Adaptive auth not enabled for shared apps, expect exception.
                {true, false}  // Adaptive auth enabled for shared apps, expect no exception.
        };
    }

    @Test(dataProvider = "adaptiveAuthForSharedAppsDataProvider")
    public void testAdaptiveAuthScriptUpdateForSharedApps(
            boolean isAdaptiveAuthEnabledForSharedApps, boolean expectException) throws Exception {

        try (MockedStatic<OrganizationManagementUtil> organizationManagementUtil = mockStatic(
                OrganizationManagementUtil.class);
             MockedStatic<Utils> utils = mockStatic(Utils.class)) {

            mockUtils(tenantDomain);
            organizationManagementUtil.when(() -> OrganizationManagementUtil.isOrganization(tenantDomain))
                    .thenReturn(true);
            utils.when(Utils::isAdaptiveAuthEnabledForSharedApps).thenReturn(isAdaptiveAuthEnabledForSharedApps);

            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                    mockServiceProviderProperty(IS_FRAGMENT_APP, TRUE)
            };
            when(serviceProvider.getSpProperties()).thenReturn(spProperties);
            when(serviceProvider.getApplicationName()).thenReturn(applicationName);
            when(applicationManagementService.getApplicationByResourceId(any(), anyString()))
                    .thenReturn(serviceProvider);

            ServiceProvider updatedSharedApp = new ServiceProvider();
            updatedSharedApp.setApplicationName(applicationName);
            LocalAndOutboundAuthenticationConfig authConfig = new LocalAndOutboundAuthenticationConfig();
            AuthenticationScriptConfig scriptConfig = new AuthenticationScriptConfig();
            scriptConfig.setEnabled(true);
            scriptConfig.setContent("some script");
            authConfig.setAuthenticationScriptConfig(scriptConfig);
            updatedSharedApp.setLocalAndOutBoundAuthenticationConfig(authConfig);

            if (expectException) {
                IdentityApplicationManagementClientException thrownException =
                        Assert.expectThrows(IdentityApplicationManagementClientException.class,
                                () -> fragmentApplicationMgtListener.doPreUpdateApplication(
                                        updatedSharedApp, tenantDomain, userName));
                Assert.assertEquals(thrownException.getMessage(),
                        "Authentication script configuration not allowed for shared applications.");
            } else {
                boolean result =
                        fragmentApplicationMgtListener.doPreUpdateApplication(updatedSharedApp, tenantDomain, userName);
                Assert.assertTrue(result);
            }
        }
    }

    @Test
    public void testRemovesOrgSSOAndAddsFallback() 
            throws IdentityApplicationManagementException, OrganizationManagementException {
        
        try (MockedStatic<OrganizationManagementUtil> organizationManagementUtil = 
                mockStatic(OrganizationManagementUtil.class);
             MockedStatic<OrgApplicationManagerUtil> orgApplicationManagerUtil = 
                mockStatic(OrgApplicationManagerUtil.class)) {
            
            organizationManagementUtil.when(() -> OrganizationManagementUtil.isOrganization(tenantDomain))
                    .thenReturn(false);
            
            // Setup existing app 
            ServiceProvider existingApp = new ServiceProvider();
            existingApp.setApplicationName(applicationName);
            existingApp.setApplicationResourceId(applicationResourceID);
            
            // Create auth config with only organization SSO 
            LocalAndOutboundAuthenticationConfig existingAuthConfig = new LocalAndOutboundAuthenticationConfig();
            existingAuthConfig.setAuthenticationType("flow");
            AuthenticationStep existingAuthStep = new AuthenticationStep();
            existingAuthStep.setStepOrder(1);
            existingAuthStep.setSubjectStep(true);
            existingAuthStep.setAttributeStep(true);
            
            IdentityProvider existingOrgSSOIdp = new IdentityProvider();
            existingOrgSSOIdp.setIdentityProviderName("OrganizationSSO");
            FederatedAuthenticatorConfig existingOrgSSOAuthConfig = new FederatedAuthenticatorConfig();
            existingOrgSSOAuthConfig.setName("OrganizationAuthenticator");
            existingOrgSSOIdp.setDefaultAuthenticatorConfig(existingOrgSSOAuthConfig);
            existingOrgSSOIdp.setFederatedAuthenticatorConfigs(
                    new FederatedAuthenticatorConfig[]{existingOrgSSOAuthConfig});
            
            existingAuthStep.setFederatedIdentityProviders(new IdentityProvider[]{existingOrgSSOIdp});
            existingAuthStep.setLocalAuthenticatorConfigs(new LocalAuthenticatorConfig[0]);
            existingAuthConfig.setAuthenticationSteps(new AuthenticationStep[]{existingAuthStep});
            existingApp.setLocalAndOutBoundAuthenticationConfig(existingAuthConfig);
            
            // Setup service provider 
            LocalAndOutboundAuthenticationConfig serviceProviderAuthConfig = new LocalAndOutboundAuthenticationConfig();
            serviceProviderAuthConfig.setAuthenticationType("flow");
            AuthenticationStep serviceProviderAuthStep = new AuthenticationStep();
            serviceProviderAuthStep.setStepOrder(1);
            serviceProviderAuthStep.setSubjectStep(true);
            serviceProviderAuthStep.setAttributeStep(true);
            
            IdentityProvider serviceProviderOrgSSOIdp = new IdentityProvider();
            serviceProviderOrgSSOIdp.setIdentityProviderName("OrganizationSSO");
            FederatedAuthenticatorConfig serviceProviderOrgSSOAuthConfig = new FederatedAuthenticatorConfig();
            serviceProviderOrgSSOAuthConfig.setName("OrganizationAuthenticator");
            serviceProviderOrgSSOIdp.setDefaultAuthenticatorConfig(serviceProviderOrgSSOAuthConfig);
            serviceProviderOrgSSOIdp.setFederatedAuthenticatorConfigs(
                    new FederatedAuthenticatorConfig[]{serviceProviderOrgSSOAuthConfig});
            
            serviceProviderAuthStep.setFederatedIdentityProviders(new IdentityProvider[]{serviceProviderOrgSSOIdp});
            serviceProviderAuthStep.setLocalAuthenticatorConfigs(new LocalAuthenticatorConfig[0]);
            serviceProviderAuthConfig.setAuthenticationSteps(new AuthenticationStep[]{serviceProviderAuthStep});
            
            // Setup service provider properties and mocking
            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                    mockServiceProviderProperty(IS_APP_SHARED, "false"), 
                    mockServiceProviderProperty(IS_FRAGMENT_APP, "false")
            };
            when(serviceProvider.getSpProperties()).thenReturn(spProperties);
            when(serviceProvider.getApplicationResourceId()).thenReturn(applicationResourceID);
            when(serviceProvider.getLocalAndOutBoundAuthenticationConfig()).thenReturn(serviceProviderAuthConfig);
            when(applicationManagementService.getApplicationByResourceId(applicationResourceID, tenantDomain))
                    .thenReturn(existingApp);
            
            // Mock default authentication config for fallback
            LocalAndOutboundAuthenticationConfig defaultAuthConfig = new LocalAndOutboundAuthenticationConfig();
            defaultAuthConfig.setAuthenticationType("default");
            AuthenticationStep defaultAuthStep = new AuthenticationStep();
            defaultAuthStep.setStepOrder(1);
            LocalAuthenticatorConfig defaultLocalAuth = new LocalAuthenticatorConfig();
            defaultLocalAuth.setName("BasicAuthenticator");
            defaultLocalAuth.setEnabled(true);
            defaultAuthStep.setLocalAuthenticatorConfigs(new LocalAuthenticatorConfig[]{defaultLocalAuth});
            defaultAuthConfig.setAuthenticationSteps(new AuthenticationStep[]{defaultAuthStep});
            orgApplicationManagerUtil.when(OrgApplicationManagerUtil::getDefaultAuthenticationConfig)
                    .thenReturn(defaultAuthConfig);
            
            boolean result = fragmentApplicationMgtListener
                    .doPreUpdateApplication(serviceProvider, tenantDomain, userName);
            
            assertTrue(result);
            
            LocalAndOutboundAuthenticationConfig updatedAuthConfig = 
                    serviceProvider.getLocalAndOutBoundAuthenticationConfig();
            AuthenticationStep firstStep = updatedAuthConfig.getAuthenticationSteps()[0];
            
            IdentityProvider[] federatedProviders = firstStep.getFederatedIdentityProviders();
            Assert.assertEquals(federatedProviders.length, 0, 
                    "Should have no federated providers after removing organization SSO");
            
            LocalAuthenticatorConfig[] localAuths = firstStep.getLocalAuthenticatorConfigs();
            Assert.assertEquals(localAuths.length, 1, 
                    "Should have exactly one local authenticator as fallback");
            Assert.assertEquals("BasicAuthenticator", localAuths[0].getName(), 
                    "Default BasicAuthenticator should be present as fallback");
        }
    }
    
    @Test
    public void testRemovesOnlyOrgSSOAndKeepsOthers() 
            throws IdentityApplicationManagementException, OrganizationManagementException {
        
        try (MockedStatic<OrganizationManagementUtil> organizationManagementUtil = 
                mockStatic(OrganizationManagementUtil.class);
             MockedStatic<OrgApplicationManagerUtil> orgApplicationManagerUtil = 
                mockStatic(OrgApplicationManagerUtil.class)) {
            
            organizationManagementUtil.when(() -> OrganizationManagementUtil.isOrganization(tenantDomain))
                    .thenReturn(false);
            
            // Setup existing app 
            ServiceProvider existingApp = new ServiceProvider();
            existingApp.setApplicationName(applicationName);
            existingApp.setApplicationResourceId(applicationResourceID);
            
            // Create auth config with organization SSO + Google + Basic for existing app
            LocalAndOutboundAuthenticationConfig existingAuthConfig = new LocalAndOutboundAuthenticationConfig();
            existingAuthConfig.setAuthenticationType("flow");
            AuthenticationStep existingAuthStep = new AuthenticationStep();
            existingAuthStep.setStepOrder(1);
            existingAuthStep.setSubjectStep(true);
            existingAuthStep.setAttributeStep(true);
            
            IdentityProvider existingOrgSSOIdp = new IdentityProvider();
            existingOrgSSOIdp.setIdentityProviderName("OrganizationSSO");
            FederatedAuthenticatorConfig existingOrgSSOAuthConfig = new FederatedAuthenticatorConfig();
            existingOrgSSOAuthConfig.setName("OrganizationAuthenticator");
            existingOrgSSOIdp.setDefaultAuthenticatorConfig(existingOrgSSOAuthConfig);
            existingOrgSSOIdp.setFederatedAuthenticatorConfigs(
                    new FederatedAuthenticatorConfig[]{existingOrgSSOAuthConfig});
            
            IdentityProvider existingGoogleIdp = new IdentityProvider();
            existingGoogleIdp.setIdentityProviderName("Google");
            FederatedAuthenticatorConfig existingGoogleAuthConfig = new FederatedAuthenticatorConfig();
            existingGoogleAuthConfig.setName("GoogleAuthenticator");
            existingGoogleIdp.setDefaultAuthenticatorConfig(existingGoogleAuthConfig);
            existingGoogleIdp.setFederatedAuthenticatorConfigs(
                    new FederatedAuthenticatorConfig[]{existingGoogleAuthConfig});
            
            LocalAuthenticatorConfig existingLocalAuth = new LocalAuthenticatorConfig();
            existingLocalAuth.setName("BasicAuthenticator");
            existingLocalAuth.setEnabled(true);
            
            existingAuthStep.setFederatedIdentityProviders(
                    new IdentityProvider[]{existingOrgSSOIdp, existingGoogleIdp});
            existingAuthStep.setLocalAuthenticatorConfigs(new LocalAuthenticatorConfig[]{existingLocalAuth});
            existingAuthConfig.setAuthenticationSteps(new AuthenticationStep[]{existingAuthStep});
            existingApp.setLocalAndOutBoundAuthenticationConfig(existingAuthConfig);
            
            // Setup service provider 
            LocalAndOutboundAuthenticationConfig serviceProviderAuthConfig = new LocalAndOutboundAuthenticationConfig();
            serviceProviderAuthConfig.setAuthenticationType("flow");
            AuthenticationStep serviceProviderAuthStep = new AuthenticationStep();
            serviceProviderAuthStep.setStepOrder(1);
            serviceProviderAuthStep.setSubjectStep(true);
            serviceProviderAuthStep.setAttributeStep(true);
            
            IdentityProvider serviceProviderOrgSSOIdp = new IdentityProvider();
            serviceProviderOrgSSOIdp.setIdentityProviderName("OrganizationSSO");
            FederatedAuthenticatorConfig serviceProviderOrgSSOAuthConfig = new FederatedAuthenticatorConfig();
            serviceProviderOrgSSOAuthConfig.setName("OrganizationAuthenticator");
            serviceProviderOrgSSOIdp.setDefaultAuthenticatorConfig(serviceProviderOrgSSOAuthConfig);
            serviceProviderOrgSSOIdp.setFederatedAuthenticatorConfigs(
                    new FederatedAuthenticatorConfig[]{serviceProviderOrgSSOAuthConfig});
            
            IdentityProvider serviceProviderGoogleIdp = new IdentityProvider();
            serviceProviderGoogleIdp.setIdentityProviderName("Google");
            FederatedAuthenticatorConfig serviceProviderGoogleAuthConfig = new FederatedAuthenticatorConfig();
            serviceProviderGoogleAuthConfig.setName("GoogleAuthenticator");
            serviceProviderGoogleIdp.setDefaultAuthenticatorConfig(serviceProviderGoogleAuthConfig);
            serviceProviderGoogleIdp.setFederatedAuthenticatorConfigs(
                    new FederatedAuthenticatorConfig[]{serviceProviderGoogleAuthConfig});
            
            LocalAuthenticatorConfig serviceProviderLocalAuth = new LocalAuthenticatorConfig();
            serviceProviderLocalAuth.setName("BasicAuthenticator");
            serviceProviderLocalAuth.setEnabled(true);
            
            serviceProviderAuthStep.setFederatedIdentityProviders(
                    new IdentityProvider[]{serviceProviderOrgSSOIdp, serviceProviderGoogleIdp});
            serviceProviderAuthStep.setLocalAuthenticatorConfigs(
                    new LocalAuthenticatorConfig[]{serviceProviderLocalAuth});
            serviceProviderAuthConfig.setAuthenticationSteps(new AuthenticationStep[]{serviceProviderAuthStep});
            
            // Setup service provider properties and mocking
            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{
                    mockServiceProviderProperty(IS_APP_SHARED, "false"), 
                    mockServiceProviderProperty(IS_FRAGMENT_APP, "false")
            };
            when(serviceProvider.getSpProperties()).thenReturn(spProperties);
            when(serviceProvider.getApplicationResourceId()).thenReturn(applicationResourceID);
            when(serviceProvider.getLocalAndOutBoundAuthenticationConfig()).thenReturn(serviceProviderAuthConfig);
            when(applicationManagementService.getApplicationByResourceId(applicationResourceID, tenantDomain))
                    .thenReturn(existingApp);
            
            // Mock default authentication config for fallback
            LocalAndOutboundAuthenticationConfig defaultAuthConfig = new LocalAndOutboundAuthenticationConfig();
            defaultAuthConfig.setAuthenticationType("default");
            AuthenticationStep defaultAuthStep = new AuthenticationStep();
            defaultAuthStep.setStepOrder(1);
            LocalAuthenticatorConfig defaultLocalAuth = new LocalAuthenticatorConfig();
            defaultLocalAuth.setName("BasicAuthenticator");
            defaultLocalAuth.setEnabled(true);
            defaultAuthStep.setLocalAuthenticatorConfigs(new LocalAuthenticatorConfig[]{defaultLocalAuth});
            defaultAuthConfig.setAuthenticationSteps(new AuthenticationStep[]{defaultAuthStep});
            orgApplicationManagerUtil.when(OrgApplicationManagerUtil::getDefaultAuthenticationConfig)
                    .thenReturn(defaultAuthConfig);
            
            boolean result = fragmentApplicationMgtListener
                    .doPreUpdateApplication(serviceProvider, tenantDomain, userName);
            
            assertTrue(result);
            
            LocalAndOutboundAuthenticationConfig modifiedAuthConfig = 
                    serviceProvider.getLocalAndOutBoundAuthenticationConfig();
            Assert.assertNotNull(modifiedAuthConfig, "Authentication config should not be null");
            
            AuthenticationStep[] modifiedAuthSteps = modifiedAuthConfig.getAuthenticationSteps();
            Assert.assertNotNull(modifiedAuthSteps, "Authentication steps should not be null");
            Assert.assertTrue(modifiedAuthSteps.length > 0, "Should have at least one authentication step");
            
            AuthenticationStep firstStep = modifiedAuthSteps[0];
            Assert.assertNotNull(firstStep, "First authentication step should not be null");
            
            IdentityProvider[] federatedProviders = firstStep.getFederatedIdentityProviders();
            Assert.assertNotNull(federatedProviders, "Federated providers should not be null");
            Assert.assertTrue(federatedProviders.length > 0, "Should have at least one federated provider (Google)");
            
            boolean hasGoogleIdp = false;
            boolean hasOrgSSO = false;
            for (IdentityProvider idp : federatedProviders) {
                if (idp.getDefaultAuthenticatorConfig() != null) {
                    String authName = idp.getDefaultAuthenticatorConfig().getName();
                    if ("GoogleAuthenticator".equals(authName)) {
                        hasGoogleIdp = true;
                    } else if ("OrganizationAuthenticator".equals(authName)) {
                        hasOrgSSO = true;
                    }
                }
            }
            
            Assert.assertTrue(hasGoogleIdp, "Google IDP should remain after removing organization SSO");
            Assert.assertFalse(hasOrgSSO, "Organization SSO should be removed");
            
            LocalAuthenticatorConfig[] localAuths = firstStep.getLocalAuthenticatorConfigs();
            Assert.assertNotNull(localAuths, "Local authenticators should not be null");
            Assert.assertTrue(localAuths.length > 0, "Should have at least one local authenticator");
            
            boolean hasBasicAuth = false;
            for (LocalAuthenticatorConfig localAuthConfig : localAuths) {
                if ("BasicAuthenticator".equals(localAuthConfig.getName())) {
                    hasBasicAuth = true;
                    break;
                }
            }
            Assert.assertTrue(hasBasicAuth, "BasicAuthenticator should remain");
        }
    }

    @AfterMethod
    public void tearDown() {

        mockedUtilities.close();
    }
}

