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
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

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

    @Test
    public void testDoPreCreateApplicationWithOrganization()
            throws IdentityApplicationManagementException, OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn("orgId1");
        when(organizationManager.getOrganizationDepthInHierarchy("orgId1")).thenReturn(0);

        boolean result = fragmentApplicationMgtListener.doPreCreateApplication(serviceProvider, tenantDomain, userName);

        assertTrue(result);
        verify(organizationManager, times(1)).resolveOrganizationId(tenantDomain);
        verify(organizationManager, times(1)).getOrganizationDepthInHierarchy("orgId1");
    }

    @Test(expectedExceptions = IdentityApplicationManagementClientException.class)
    public void testDoPreCreateApplicationWithSubOrganization() throws IdentityApplicationManagementException,
            OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn("orgId2");
        when(organizationManager.getOrganizationDepthInHierarchy("orgId2")).thenReturn(2);

        try {
            fragmentApplicationMgtListener.doPreCreateApplication(serviceProvider, tenantDomain, userName);
        } finally {
            verify(organizationManager, times(1)).resolveOrganizationId(tenantDomain);
            verify(organizationManager, times(1)).getOrganizationDepthInHierarchy("orgId2");
        }
    }
}

