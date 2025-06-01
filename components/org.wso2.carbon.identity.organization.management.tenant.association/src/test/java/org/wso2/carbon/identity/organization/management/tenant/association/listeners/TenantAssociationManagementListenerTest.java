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

package org.wso2.carbon.identity.organization.management.tenant.association.listeners;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.tenant.association.internal.TenantAssociationDataHolder;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class TenantAssociationManagementListenerTest {

    private static final String tenantName = "tenantName";
    private static final String tenantDomain = "tenantDomain";
    private static final String tenantAdmin = "tenantAdmin";
    private static final String organizationID = "10084a8d-113f-4211-a0d5-efe36b082211";

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private RealmService realmService;

    @Mock
    private TenantManager tenantManager;

    @Mock
    private Tenant tenant;

    @InjectMocks
    private TenantAssociationManagementListener tenantAssociationManagementListener;

    @Captor
    private ArgumentCaptor<Organization> organizationArgumentCaptor;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        TenantAssociationDataHolder.setRealmService(realmService);
        TenantAssociationDataHolder.setOrganizationManager(organizationManager);
    }

    @DataProvider(name = "tenantNameDataProvider")
    public Object[][] tenantNameDataProvider() {

        return new Object[][]{
                {tenantName, true},
                {null, false}
        };
    }

    @Test(dataProvider = "tenantNameDataProvider")
    public void testOnTenantCreateWithTenantName(String tenantName, boolean isTenantNameProvided) throws Exception {

        when(realmService.getTenantManager()).thenReturn(tenantManager);
        when(tenantManager.getTenant(-1)).thenReturn(tenant);
        when(tenant.getAssociatedOrganizationUUID()).thenReturn(organizationID);
        when(organizationManager.getOrganizationDepthInHierarchy(organizationID)).thenReturn(-1);

        TenantInfoBean tenantInfoBean = createTenantInfoBean();
        if (isTenantNameProvided) {
            tenantInfoBean.setName(tenantName);
        }
        tenantAssociationManagementListener.onTenantCreate(tenantInfoBean);
        verify(organizationManager).addRootOrganization(anyInt(), organizationArgumentCaptor.capture());
        Organization organization = organizationArgumentCaptor.getValue();

        if (isTenantNameProvided) {
            assertEquals(tenantName, organization.getName());
        } else {
            assertEquals(tenantDomain, organization.getName());
        }
    }

    private TenantInfoBean createTenantInfoBean() {

        TenantInfoBean tenantInfoBean = new TenantInfoBean();
        tenantInfoBean.setTenantId(-1);
        tenantInfoBean.setTenantDomain(tenantDomain);
        tenantInfoBean.setAdmin(tenantAdmin);
        return tenantInfoBean;
    }
}
