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
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import static org.mockito.Mockito.mock;
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

    private ServiceProvider serviceProvider;
    private String tenantDomain;
    private String userName;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        serviceProvider = mock(ServiceProvider.class);
        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
        tenantDomain = "sampleTenantDomain";
        userName = "sampleUser";
    }

    @DataProvider(name = "organizationHierarchyData")
    public Object[][] getOrganizationHierarchyData() {

        return new Object[][]{
                {"orgId1", 0, false, false},
                {"orgId2", 2, false, true},
                {"orgId3", 2, true, false}
        };
    }

    @Test(dataProvider = "organizationHierarchyData")
    public void testDoPreCreateApplication(String organizationId, int hierarchyDepth, boolean subOrgCanCreateApp,
                                           boolean expectException)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationId);
        when(organizationManager.getOrganizationDepthInHierarchy(organizationId)).thenReturn(hierarchyDepth);
        // Set the property to true if the sub organization can create applications.
        if (subOrgCanCreateApp) {
            ServiceProviderProperty[] spProperties = new ServiceProviderProperty[2];
            spProperties[0] = new ServiceProviderProperty();
            spProperties[0].setName(IS_FRAGMENT_APP);
            spProperties[0].setValue(String.valueOf(true));
            when(serviceProvider.getSpProperties()).thenReturn(spProperties);
        }
        try {
            boolean result = fragmentApplicationMgtListener.doPreCreateApplication(serviceProvider, tenantDomain,
                    userName);
            assertEquals(result, !expectException);
        } catch (IdentityApplicationManagementClientException e) {
            assertTrue(expectException);
        }
    }
}

