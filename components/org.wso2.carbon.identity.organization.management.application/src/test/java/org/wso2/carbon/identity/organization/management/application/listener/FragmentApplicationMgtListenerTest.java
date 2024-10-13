/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com).
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

/**
 * Test class for FragmentApplicationMgtListener.
 */
public class FragmentApplicationMgtListenerTest {

    @Mock
    private OrganizationManager organizationManager;

    @InjectMocks
    private FragmentApplicationMgtListener fragmentApplicationMgtListener;

    private MockedStatic<IdentityTenantUtil> mockedUtilities;

    private ServiceProvider serviceProvider;
    private static String primaryTenantDomain = "primaryTenantDomain";
    private static String tenantDomain = "sampleTenantDomain";
    private static String userName = "sampleUser";

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        serviceProvider = mock(ServiceProvider.class);
        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);

        mockedUtilities = Mockito.mockStatic(IdentityTenantUtil.class, Mockito.withSettings()
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
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
            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[1];
            spProperties[0] = new ServiceProviderProperty();
            spProperties[0].setName(IS_FRAGMENT_APP);
            spProperties[0].setValue(String.valueOf(true));
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

    @AfterMethod
    public void tearDown() {

        mockedUtilities.close();
    }
}

