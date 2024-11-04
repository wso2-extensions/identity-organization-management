/*
 * Copyright (c) 2023-2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.IS_API_BASED_AUTHENTICATION_ENABLED_PROPERTY_NAME;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

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

    @InjectMocks
    private FragmentApplicationMgtListener fragmentApplicationMgtListener;

    private MockedStatic<IdentityTenantUtil> mockedUtilities;

    private ServiceProvider serviceProvider;
    private static final String primaryTenantDomain = "primaryTenantDomain";
    private static final String tenantDomain = "sampleTenantDomain";
    private static final String userName = "sampleUser";
    private static final String applicationName = "sampleApp";
    private static final String applicationResourceID = "fcb0c1d7-28c0-46f7-bc2d-345678a1b23c";
    private static final String organizationID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String sampleClaimURI = "http://wso2.org/claims/sampleClaim1";

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        serviceProvider = mock(ServiceProvider.class);
        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrgApplicationMgtDataHolder.getInstance().setOrgApplicationMgtDAO(orgApplicationMgtDAO);
        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
    }

    @DataProvider(name = "subOrganizationMetaData")
    public Object[][] getSubOrganizationMetaData() {

        return new Object[][]{
                // Create application in sub-organization.
                {"orgId2", 2, tenantDomain, false, true},
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
                    mockServiceProviderProperty(IS_FRAGMENT_APP, "true"),
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
                mockServiceProviderProperty(IS_FRAGMENT_APP, "true"),
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

    private void mockUtils(String requestInitiatedDomain) {

        mockedUtilities = Mockito.mockStatic(IdentityTenantUtil.class, Mockito.withSettings()
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
        mockedUtilities.when(() -> IdentityTenantUtil.getTenantDomainFromContext()).thenReturn(requestInitiatedDomain);
    }

    @AfterMethod
    public void tearDown() {

        mockedUtilities.close();
    }
}

