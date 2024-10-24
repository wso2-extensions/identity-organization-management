/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service.listener;

import org.apache.commons.lang3.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.discovery.service.util.TestUtils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.internal.OrganizationManagementDataHolder;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.user.core.UserCoreConstants.DEFAULT_PROFILE;

/**
 * This class contains unit tests to verify the behavior of the OrganizationDiscoveryUserOperationListener class.
 */
public class OrganizationDiscoveryUserOperationListenerTest {

    private static final String TEST_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String TEST_TENANT_DOMAIN = "example.com";
    private static final String TEST_USER = "testUser";
    private static final Object TEST_CREDENTIALS = "dummyPassword";
    private static final int TEST_TENANT_ID = 1234;

    @InjectMocks
    private OrganizationDiscoveryUserOperationListener organizationDiscoveryUserOperationListener;

    @Mock
    private UserStoreManager userStoreManager;

    @Mock
    private RealmService realmService;

    @Mock
    private TenantManager tenantManager;

    @Mock
    private Tenant tenant;

    @Mock
    private OrganizationManager organizationManager;

    @BeforeMethod
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);

        TestUtils.mockDataSource();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(TEST_TENANT_DOMAIN);
        OrganizationManagementDataHolder.getInstance().setRealmService(realmService);
        OrganizationManagementDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationManager(organizationManager);

        when(realmService.getTenantManager()).thenReturn(tenantManager);
        when(tenantManager.getTenantId(TEST_TENANT_DOMAIN)).thenReturn(TEST_TENANT_ID);
        when(tenantManager.getTenant(TEST_TENANT_ID)).thenReturn(tenant);
        when(tenant.getAssociatedOrganizationUUID()).thenReturn(TEST_ORG_ID);
        when(organizationManager.getOrganizationDepthInHierarchy(TEST_ORG_ID)).thenReturn(1);
    }

    @DataProvider(name = "skipEmailDomainValidationTestDataProvider")
    public Object[][] claimsProvider() {

        return new Object[][]{
                {null, false},
                {new HashMap<String, String>(), false},
                {new HashMap<String, String>() {{ put(CLAIM_MANAGED_ORGANIZATION, StringUtils.EMPTY); }}, false},
                {new HashMap<String, String>() {{ put(CLAIM_MANAGED_ORGANIZATION, TEST_ORG_ID); }}, true}
        };
    }

    @Test(dataProvider = "skipEmailDomainValidationTestDataProvider")
    public void testSkipEmailDomainValidationForSharedUserCreation(Map<String, String> claims, boolean expectedResult)
            throws UserStoreException, OrganizationManagementException {

        when(organizationManager.resolveOrganizationId(TEST_TENANT_DOMAIN))
                .thenThrow(new OrganizationManagementException("Validation not skipped."));

        boolean result = organizationDiscoveryUserOperationListener.doPreAddUserWithID(TEST_USER, TEST_CREDENTIALS,
                new String[]{}, claims, DEFAULT_PROFILE, userStoreManager);

        if (expectedResult) {
            assertTrue(result, "Email domain validation should be skipped for shared user creation.");
        } else {
            assertFalse(result, "Expected false when CLAIM_MANAGED_ORGANIZATION claim is missing.");
        }
    }
}
