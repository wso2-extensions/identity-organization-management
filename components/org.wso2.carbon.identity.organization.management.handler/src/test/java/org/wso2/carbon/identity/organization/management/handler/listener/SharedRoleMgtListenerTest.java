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

package org.wso2.carbon.identity.organization.management.handler.listener;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.AssociatedRolesConfig;
import org.wso2.carbon.identity.application.common.model.RoleV2;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
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
    private static final String IS_FRAGMENT_APP = "isFragmentApp";
    private static final String ORGANIZATION_AUD = "organization";
    private static final String APPLICATION_AUD = "application";

    @Mock
    private ApplicationManagementService mockedApplicationManagementService;

    @Mock
    private OrganizationManager mockedOrganizationManager;

    @Mock
    private OrgApplicationManager mockedOrgApplicationManager;

    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilMockedStatic;
    private MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic;

    @BeforeClass
    public void setUpClass() {

        MockitoAnnotations.openMocks(this);
        OrganizationManagementHandlerDataHolder.getInstance().
                setApplicationManagementService(mockedApplicationManagementService);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(mockedOrganizationManager);
        OrganizationManagementHandlerDataHolder.getInstance().setOrgApplicationManager(mockedOrgApplicationManager);
        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);
        identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
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
}
