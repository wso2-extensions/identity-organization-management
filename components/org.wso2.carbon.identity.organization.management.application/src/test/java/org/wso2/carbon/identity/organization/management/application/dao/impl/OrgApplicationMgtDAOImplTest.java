/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.DiscoverableGroup;
import org.wso2.carbon.identity.application.common.model.GroupBasicInfo;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.application.mgt.dao.impl.ApplicationDAOImpl;
import org.wso2.carbon.identity.application.mgt.internal.ApplicationManagementServiceComponentHolder;
import org.wso2.carbon.identity.application.mgt.provider.ApplicationPermissionProvider;
import org.wso2.carbon.identity.common.testng.WithH2Database;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.Group;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_ID;

/**
 * Unit tests for OrgApplicationMgtDAOImpl.
 */
@WithH2Database(jndiName = "jdbc/WSO2IdentityDB", files = {"dbscripts/identity.sql"})
public class OrgApplicationMgtDAOImplTest {

    private static final String DEFAULT_USER_STORE_DOMAIN = "PRIMARY";
    private static final String USERNAME = "test-user";
    private static final String USER_ID = "42ef1d92-add6-449b-8a3c-fc308d2a4eac";
    private static final String ROOT_ORG_ID = "72b81cba-51c7-4dc1-91be-b267e177c17a";
    private static final String SHARED_ORG_ID_1 = "30b701c6-e309-4241-b047-0c299c45d1a0";
    private static final int SHARED_TENANT_ID_1 = 1;
    private static final String SHARED_ORG_ID_2 = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final int SHARED_TENANT_ID_2 = 2;
    private static final String UN_SHARED_ORG_ID = "89d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final int UN_SHARED_TENANT_ID = 3;
    private static final String SAMPLE_APP_1 = "test-app";
    private static final String SAMPLE_APP_2 = "scl-app";
    private static final String SAMPLE_APP_3 = "medical-app";

    private MockedStatic<IdentityTenantUtil> mockIdentityTenantUtil;
    private MockedStatic<IdentityUtil> mockIdentityUtil;
    private MockedStatic<ApplicationManagementServiceComponentHolder> mockedApplicationManagementServiceComponentHolder;
    private ApplicationManagementServiceComponentHolder mockComponentHolder;
    private UserRealm mockUserRealm;
    private RealmService mockRealmService;
    private AbstractUserStoreManager mockAbstractUserStoreManager;
    private ApplicationPermissionProvider mockApplicationPermissionProvider;
    private TenantManager mockTenantManager;
    private OrganizationUserResidentResolverService mockOrganizationUserResidentResolverService;

    private OrgApplicationMgtDAOImpl orgApplicationMgtDAO;
    private ApplicationDAO applicationDAO;

    /**
     * Setup the test environment for OrgApplicationMgtDAOImpl.
     */
    @BeforeClass
    public void setup() throws org.wso2.carbon.user.api.UserStoreException, IdentityApplicationManagementException,
            OrganizationManagementException {

        mockIdentityTenantUtil = mockStatic(IdentityTenantUtil.class);
        mockIdentityUtil = mockStatic(IdentityUtil.class);
        mockedApplicationManagementServiceComponentHolder =
                mockStatic(ApplicationManagementServiceComponentHolder.class);
        mockComponentHolder = mock(ApplicationManagementServiceComponentHolder.class);
        mockUserRealm = mock(UserRealm.class);
        mockRealmService = mock(RealmService.class);
        mockAbstractUserStoreManager = mock(AbstractUserStoreManager.class);
        mockApplicationPermissionProvider = mock(ApplicationPermissionProvider.class);
        mockTenantManager = mock(TenantManager.class);
        mockOrganizationUserResidentResolverService = mock(OrganizationUserResidentResolverService.class);
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

        createAndShareApplication(SAMPLE_APP_1, new String[] {SHARED_ORG_ID_1, SHARED_ORG_ID_2});

        return new Object[][] {
                // Passing org ids of both shared apps only.
                {Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2), 2},
                // Passing org ids of both shared apps and an unshared org id.
                {Arrays.asList(SHARED_ORG_ID_1, SHARED_ORG_ID_2, UN_SHARED_ORG_ID), 2},
                // Passing org id of one shared app only.
                {Collections.singletonList(SHARED_ORG_ID_1), 1},
                // Passing org id of shared app and an unshared org id.
                {Arrays.asList(SHARED_ORG_ID_1, UN_SHARED_ORG_ID), 1},
                // Passing an unshared org id only.
                {Collections.singletonList(UN_SHARED_ORG_ID), 0},
                // Passing an empty list
                {Collections.emptyList(), 0},
        };
    }

    @Test(dataProvider = "filteredSharedApplicationsTestData")
    public void testGetFilteredSharedApplications(List<String> sharedOrgIds, int expectedNumOfApps) throws Exception {

        String rootAppUUID =
                applicationDAO.getApplication(SAMPLE_APP_1, SUPER_TENANT_DOMAIN_NAME).getApplicationResourceId();
        List<SharedApplicationDO> sharedApplications =
                orgApplicationMgtDAO.getSharedApplications(rootAppUUID, ROOT_ORG_ID, sharedOrgIds);

        Assert.assertNotNull(sharedApplications);
        Assert.assertEquals(sharedApplications.size(), expectedNumOfApps);
    }

    @Test(description = "Test the correct discoverable apps list for logged in user",
            dependsOnMethods = {"testGetFilteredSharedApplications"})
    public void testDiscoverableAppsList()
            throws IdentityApplicationManagementException, OrganizationManagementException, UserStoreException {

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(SHARED_ORG_ID_1);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(SHARED_TENANT_ID_1);
        createAndShareApplication(SAMPLE_APP_2, new String[] {SHARED_ORG_ID_1});
        ServiceProvider serviceProvider1 = applicationDAO.getApplication(SAMPLE_APP_2, SHARED_ORG_ID_1);
        serviceProvider1.setDiscoverable(true);
        serviceProvider1.setAccessUrl("https://localhost:5000/scl-app");
        serviceProvider1.setDiscoverableGroups(
                new DiscoverableGroup[] {getNewDiscoverableGroup(DEFAULT_USER_STORE_DOMAIN, 1, 0)});
        applicationDAO.updateApplication(serviceProvider1, SHARED_ORG_ID_1);
        createAndShareApplication(SAMPLE_APP_3, new String[] {SHARED_ORG_ID_1});
        ServiceProvider serviceProvider2 = applicationDAO.getApplication(SAMPLE_APP_3, SHARED_ORG_ID_1);
        serviceProvider2.setDiscoverableGroups(
                new DiscoverableGroup[] {getNewDiscoverableGroup(DEFAULT_USER_STORE_DOMAIN, 1, 1)});
        serviceProvider2.setDiscoverable(true);
        serviceProvider2.setAccessUrl("https://localhost:5000/medical-app");
        applicationDAO.updateApplication(serviceProvider2, SHARED_ORG_ID_1);
        ServiceProvider serviceProvider3 = applicationDAO.getApplication(SAMPLE_APP_1, SHARED_ORG_ID_1);
        serviceProvider3.setDiscoverable(true);
        serviceProvider3.setAccessUrl("https://localhost:5000/test-app");
        applicationDAO.updateApplication(serviceProvider3, SHARED_ORG_ID_1);
        when(mockAbstractUserStoreManager.getGroupListOfUser(eq(USER_ID), nullable(String.class),
                nullable(String.class))).thenReturn(Collections.singletonList(new Group("test-group-id-0")));
        List<ApplicationBasicInfo> applicationBasicInfos =
                orgApplicationMgtDAO.getDiscoverableSharedApplicationBasicInfo(10, 0, null, null, null, SHARED_ORG_ID_1,
                        ROOT_ORG_ID);
        assertEquals(applicationBasicInfos.size(), 2);
        assertEquals(applicationBasicInfos.get(0).getApplicationName(), SAMPLE_APP_2);
        assertEquals(applicationBasicInfos.get(1).getApplicationName(), SAMPLE_APP_1);
    }

    @Test(description = "Test retrieving discoverable apps when getUserGroupList throws an exception",
            dependsOnMethods = {"testDiscoverableAppsList"})
    public void testDiscoverableAppsListWhenGetUserGroupListThrowsException() throws UserStoreException {

        when(mockAbstractUserStoreManager.getGroupListOfUser(eq(USER_ID), nullable(String.class),
                nullable(String.class))).thenThrow(new UserStoreException());
        assertThrows(OrganizationManagementException.class,
                () -> orgApplicationMgtDAO.getDiscoverableSharedApplicationBasicInfo(10, 0, null, null, null,
                        SHARED_ORG_ID_1, ROOT_ORG_ID));
    }

    @Test(description = "Test retrieving discoverable apps list with a filter",
            dependsOnMethods = {"testDiscoverableAppsListWhenGetUserGroupListThrowsException"})
    public void testDiscoverableAppsListWithFilter()
            throws UserStoreException, OrganizationManagementException {

        when(mockAbstractUserStoreManager.getGroupListOfUser(eq(USER_ID), nullable(String.class),
                nullable(String.class))).thenReturn(
                Arrays.asList(new Group("test-group-id-0"), new Group("test-group-id-1")));
        List<ApplicationBasicInfo> applicationBasicInfos =
                orgApplicationMgtDAO.getDiscoverableSharedApplicationBasicInfo(10, 0, "medical*", null, null,
                        SHARED_ORG_ID_1, ROOT_ORG_ID);
        assertEquals(applicationBasicInfos.size(), 1);
        assertEquals(applicationBasicInfos.get(0).getApplicationName(), SAMPLE_APP_3);
    }

    @Test(description = "Test getting count of discoverable applications", dependsOnMethods = {
            "testDiscoverableAppsListWithFilter"})
    public void testGetDiscoverableAppCount()
            throws UserStoreException, OrganizationManagementException {

        when(mockAbstractUserStoreManager.getGroupListOfUser(eq(USER_ID), nullable(String.class),
                nullable(String.class))).thenReturn(Collections.singletonList(new Group("test-group-id-0")));
        assertEquals(orgApplicationMgtDAO.getCountOfDiscoverableSharedApplications(null, SHARED_ORG_ID_1, ROOT_ORG_ID),
                2);
        assertEquals(
                orgApplicationMgtDAO.getCountOfDiscoverableSharedApplications("scl*", SHARED_ORG_ID_1, ROOT_ORG_ID),
                1);
    }

    /**
     * Get a new DiscoverableGroup object.
     *
     * @param userStore      User store domain.
     * @param numberOfGroups Number of groups to be added.
     * @param startIndex     Suffix start index of the group.
     * @return New DiscoverableGroup object.
     */
    private DiscoverableGroup getNewDiscoverableGroup(String userStore, int numberOfGroups, int startIndex) {

        DiscoverableGroup discoverableGroup = new DiscoverableGroup();
        discoverableGroup.setUserStore(userStore);
        List<GroupBasicInfo> groupBasicInfos = new ArrayList<>();
        for (int i = startIndex; i < numberOfGroups + startIndex; i++) {
            GroupBasicInfo groupBasicInfo = new GroupBasicInfo();
            groupBasicInfo.setId("test-group-id-" + i);
            groupBasicInfo.setName("test-group-name-" + i);
            groupBasicInfos.add(groupBasicInfo);
        }
        discoverableGroup.setGroups(groupBasicInfos.toArray(new GroupBasicInfo[0]));
        return discoverableGroup;
    }

    /**
     * Create and share an application with the given shared organization ids.
     *
     * @param appName      Application name.
     * @param sharedOrgIds Shared organization ids.
     */
    private void createAndShareApplication(String appName, String[] sharedOrgIds)
            throws IdentityApplicationManagementException, OrganizationManagementException {

        ServiceProvider application = new ServiceProvider();
        application.setApplicationName(appName);
        application.setApplicationVersion("v1.0.0");
        applicationDAO.createApplication(application, SUPER_TENANT_DOMAIN_NAME);
        for (String sharedOrgId : sharedOrgIds) {
            ServiceProvider sharedApp = new ServiceProvider();
            sharedApp.setApplicationName(appName);
            sharedApp.setApplicationVersion("v1.0.0");
            applicationDAO.createApplication(sharedApp, sharedOrgId);
            orgApplicationMgtDAO.addSharedApplication(application.getApplicationResourceId(), ROOT_ORG_ID,
                    sharedApp.getApplicationResourceId(), sharedOrgId, false);
        }
    }

    /**
     * Setup the configurations for the test.
     */
    private void setupInitConfigurations()
            throws org.wso2.carbon.user.api.UserStoreException, IdentityApplicationManagementException,
            OrganizationManagementException {

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
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(ROOT_ORG_ID))
                .thenReturn(SUPER_TENANT_ID);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(eq(SUPER_TENANT_ID)))
                .thenReturn(SUPER_TENANT_DOMAIN_NAME);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(eq(SHARED_ORG_ID_1)))
                .thenReturn(SHARED_TENANT_ID_1);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(eq(SHARED_TENANT_ID_1)))
                .thenReturn(SHARED_ORG_ID_1);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(eq(SHARED_ORG_ID_2)))
                .thenReturn(SHARED_TENANT_ID_2);
        mockIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(eq(SHARED_TENANT_ID_2)))
                .thenReturn(SHARED_ORG_ID_2);

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
        when(mockRealmService.getTenantUserRealm(SHARED_TENANT_ID_1)).thenReturn(mockUserRealm);
        when(mockRealmService.getTenantUserRealm(SHARED_TENANT_ID_2)).thenReturn(mockUserRealm);
        when(mockRealmService.getTenantUserRealm(UN_SHARED_TENANT_ID)).thenReturn(mockUserRealm);
        when(mockRealmService.getTenantManager()).thenReturn(mockTenantManager);
        when(mockTenantManager.getTenant(eq(SHARED_TENANT_ID_1))).thenReturn(
                getTenant(SHARED_TENANT_ID_1, SHARED_ORG_ID_1));
        when(mockTenantManager.getTenant(eq(SHARED_TENANT_ID_2))).thenReturn(
                getTenant(SHARED_TENANT_ID_2, SHARED_ORG_ID_2));
        when(mockTenantManager.getTenant(eq(UN_SHARED_TENANT_ID))).thenReturn(
                getTenant(UN_SHARED_TENANT_ID, UN_SHARED_ORG_ID));
        when(mockUserRealm.getUserStoreManager()).thenReturn(mockAbstractUserStoreManager);
        when(mockComponentHolder.getApplicationPermissionProvider()).thenReturn(mockApplicationPermissionProvider);
        when(mockApplicationPermissionProvider.loadPermissions(anyString())).thenReturn(new ArrayList<>());
        when(mockComponentHolder.getOrganizationUserResidentResolverService())
                .thenReturn(mockOrganizationUserResidentResolverService);
        when(mockOrganizationUserResidentResolverService.resolveUserFromResidentOrganization(eq(USERNAME), eq(USER_ID),
                anyString())).thenReturn(
                Optional.of(new User(USER_ID, USERNAME, null)));
    }

    /**
     * Get a new Tenant object.
     *
     * @param tenantId     Tenant id.
     * @param tenantDomain Tenant domain.
     * @return New Tenant object.
     */
    private Tenant getTenant(int tenantId, String tenantDomain) {

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setDomain(tenantDomain);
        tenant.setAssociatedOrganizationUUID(tenantDomain);
        return tenant;
    }
}
