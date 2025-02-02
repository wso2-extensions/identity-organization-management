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

package org.wso2.carbon.identity.organization.management.application.dao.impl;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.dao.impl.ApplicationDAOImpl;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;
import org.wso2.carbon.identity.application.mgt.provider.ApplicationPermissionProvider;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_ID;

/**
 * Unit tests for OrgApplicationMgtDAOImpl.
 */
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/identity.sql"})
public class OrgApplicationMgtDAOImplTest {

    private static final String DEFAULT_USER_STORE_DOMAIN = "PRIMARY";
    private static final String USERNAME = "test-user";
    private static final String USER_ID = "test-user-id";
    private static final String SUPER_ORG_ID = "test-super-org-id";

    MockedStatic<IdentityTenantUtil> mockIdentityTenantUtil;
    MockedStatic<IdentityUtil> mockIdentityUtil;
    MockedStatic<ApplicationManagementServiceComponentHolder> mockedApplicationManagementServiceComponentHolder;
    ApplicationManagementServiceComponentHolder mockComponentHolder;
    UserRealm mockUserRealm;
    RealmService mockRealmService;
    AbstractUserStoreManager mockAbstractUserStoreManager;
    ApplicationPermissionProvider mockApplicationPermissionProvider;

    private OrgApplicationMgtDAOImpl orgApplicationMgtDAO;
    private ApplicationDAOImpl applicationDAO;

    /**
     * Setup the test environment for OrgApplicationMgtDAOImpl.
     */
    @BeforeClass
    public void setup() throws org.wso2.carbon.user.api.UserStoreException, IdentityApplicationManagementException {

        mockIdentityTenantUtil = mockStatic(IdentityTenantUtil.class);
        mockIdentityUtil = mockStatic(IdentityUtil.class);
        mockedApplicationManagementServiceComponentHolder =
                mockStatic(ApplicationManagementServiceComponentHolder.class);
        mockComponentHolder = mock(ApplicationManagementServiceComponentHolder.class);
        mockUserRealm = mock(UserRealm.class);
        mockRealmService = mock(RealmService.class);
        mockAbstractUserStoreManager = mock(AbstractUserStoreManager.class);
        mockApplicationPermissionProvider = mock(ApplicationPermissionProvider.class);
        setupInitConfigurations();

        orgApplicationMgtDAO = new OrgApplicationMgtDAOImpl();
        applicationDAO = new ApplicationDAOImpl();
    }

    /**
     * Clean up the test environment of OrgApplicationMgtDAOImpl.
     */
    @AfterClass
    public void tearDown() {

        mockIdentityTenantUtil.close();
        mockIdentityUtil.close();
        mockedApplicationManagementServiceComponentHolder.close();
    }

    @DataProvider(name = "filteredSharedApplicationsTestData")
    public Object[][] getFilteredSharedApplicationsTestData()
            throws IdentityApplicationManagementException, OrganizationManagementException {

        String sharedOrgID1 = "30b701c6-e309-4241-b047-0c299c45d1a0";
        String sharedOrgID2 = "93d996f9-a5ba-4275-a52b-adaad9eba869";
        String unsharedOrgID = "89d996f9-a5ba-4275-a52b-adaad9eba869";

        Integer[] appIDs = createAndShareApplication("test-app", new String[] {sharedOrgID1, sharedOrgID2});

        return new Object[][] {
                // Passing org ids of both shared apps only.
                {Arrays.asList(sharedOrgID1, sharedOrgID2), 2, appIDs[0]},
                // Passing org ids of both shared apps and an unshared org id.
                {Arrays.asList(sharedOrgID1, sharedOrgID2, unsharedOrgID), 2, appIDs[0]},
                // Passing org id of one shared app only.
                {Collections.singletonList(sharedOrgID1), 1, appIDs[0]},
                // Passing org id of shared app and an unshared org id.
                {Arrays.asList(sharedOrgID1, unsharedOrgID), 1, appIDs[0]},
                // Passing an unshared org id only.
                {Collections.singletonList(unsharedOrgID), 0, appIDs[0]},
                // Passing an empty list
                {Collections.emptyList(), 0, appIDs[0]},
        };
    }

    @Test(dataProvider = "filteredSharedApplicationsTestData")
    public void testGetFilteredSharedApplications(List<String> sharedOrgIds, int expectedNumOfApps, int rootAppId)
            throws Exception {

        String rootAppUUID = applicationDAO.getApplication(rootAppId).getApplicationResourceId();
        List<SharedApplicationDO> sharedApplications =
                orgApplicationMgtDAO.getSharedApplications(rootAppUUID, SUPER_ORG_ID, sharedOrgIds);

        Assert.assertNotNull(sharedApplications);
        Assert.assertEquals(sharedApplications.size(), expectedNumOfApps);
    }

    /**
     * Create and share an application with the given shared organization ids.
     *
     * @param appName      Application name.
     * @param sharedOrgIds Shared organization ids.
     * @return Application IDs of the created applications.
     */
    private Integer[] createAndShareApplication(String appName, String[] sharedOrgIds)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        ServiceProvider application = new ServiceProvider();
        application.setApplicationName(appName);
        application.setApplicationVersion("v1.0.0");
        Integer[] createAppIds = new Integer[sharedOrgIds.length + 1];
        int appId = applicationDAO.createApplication(application, SUPER_TENANT_DOMAIN_NAME);
        createAppIds[0] = appId;
        for (int i = 0; i < sharedOrgIds.length; i++) {
            String sharedOrgId = sharedOrgIds[i];
            mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(eq(sharedOrgId)))
                    .thenReturn(i + 1);
            ServiceProvider sharedApp = new ServiceProvider();
            sharedApp.setApplicationName(appName);
            sharedApp.setApplicationVersion("v1.0.0");
            int sharedAppId = applicationDAO.createApplication(sharedApp, sharedOrgId);
            orgApplicationMgtDAO.addSharedApplication(application.getApplicationResourceId(), SUPER_ORG_ID,
                    sharedApp.getApplicationResourceId(), sharedOrgId, false);
            createAppIds[i + 1] = sharedAppId;
        }
        return createAppIds;
    }

    private void deleteAllApplications(String[] sharedOrgIds)
            throws IdentityApplicationManagementException, OrganizationManagementServerException {

        applicationDAO.deleteApplications(SUPER_TENANT_ID);
        for (String sharedOrgId : sharedOrgIds) {
            applicationDAO.deleteApplications(IdentityTenantUtil.getTenantId(sharedOrgId));
            orgApplicationMgtDAO.deleteSharedAppLinks(sharedOrgId);
        }
    }

    /**
     * Setup the configurations for the test.
     */
    private void setupInitConfigurations()
            throws org.wso2.carbon.user.api.UserStoreException, IdentityApplicationManagementException {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes", "repository").
                toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SUPER_TENANT_DOMAIN_NAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SUPER_TENANT_ID);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(USERNAME);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserId(USER_ID);

        CarbonConstants.ENABLE_LEGACY_AUTHZ_RUNTIME = false;

        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(eq(SUPER_TENANT_DOMAIN_NAME)))
                .thenReturn(SUPER_TENANT_ID);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(SUPER_ORG_ID))
                .thenReturn(SUPER_TENANT_ID);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(eq(SUPER_TENANT_ID)))
                .thenReturn(SUPER_TENANT_DOMAIN_NAME);

        mockIdentityUtil.when(IdentityUtil::getIdentityConfigDirPath)
                .thenReturn(Paths.get(carbonHome, "conf", "identity").toString());
        mockIdentityUtil.when(() -> IdentityUtil.extractDomainFromName(anyString()))
                .thenReturn(DEFAULT_USER_STORE_DOMAIN);
        mockIdentityUtil.when(IdentityUtil::getPrimaryDomainName).thenReturn(SUPER_TENANT_DOMAIN_NAME);

        mockedApplicationManagementServiceComponentHolder.when(
                        ApplicationManagementServiceComponentHolder::getInstance)
                .thenReturn(mockComponentHolder);
        when(mockComponentHolder.getRealmService()).thenReturn(mockRealmService);
        when(mockRealmService.getTenantUserRealm(SUPER_TENANT_ID)).thenReturn(mockUserRealm);
        when(mockUserRealm.getUserStoreManager()).thenReturn(mockAbstractUserStoreManager);
        when(mockComponentHolder.getApplicationPermissionProvider()).thenReturn(mockApplicationPermissionProvider);
        when(mockApplicationPermissionProvider.loadPermissions(anyString())).thenReturn(new ArrayList<>());
    }
}
