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

package org.wso2.carbon.identity.organization.management.application;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

/**
 * Unit tests for OrgApplicationManagerImpl.
 */
public class OrgApplicationManagerImplTest {

    @Mock
    private OrgApplicationMgtDAO orgApplicationMgtDAO;
    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private ApplicationManagementService applicationManagementService;

    private OrgApplicationManager orgApplicationManager;

    private static final Map<String, String> childAppIdMap = new HashMap<String, String>() {{
        put("99b701c6-e309-4241-b047-0c299c45d1a0", "56ef1d92-add6-449b-8a3c-fc308d2a4eac");
        put("5b65d2f9-1b0c-4e66-9f2c-3aa57dcd1ef7", "ede1f19f-c8b4-4f3c-a19d-18aab939ad3f");
        put("8o65d2l6-1u0c-4e76-9f2c-4aa67trt1ef8", "udt1f16f-c8y4-4r3c-a20d-58hhg969ad6p");
    }};
    private static final String CHILD_APP_ID = "42ef1d92-add6-449b-8a3c-fc308d2a4eac";
    private static final String CHILD_ORG_ID = "30b701c6-e309-4241-b047-0c299c45d1a0";
    private static final String PARENT_APP_ID = "1e2ef3df-e670-4339-9833-9df41dda7c96";
    private static final String PARENT_ORG_ID = "93d996f9-a5ba-4275-a52b-adaad9eba869";
    private static final String PARENT_TENANT_DOMAIN = "test-organization";
    private static final String ROOT_APP_ID = "fa9b9ac5-a429-49e2-9c51-4259c7ebe45e";
    private static final String ROOT_ORG_ID = "72b81cba-51c7-4dc1-91be-b267e177c17a";
    private static final String ROOT_TENANT_DOMAIN = "root-organization";
    private static final String INVALID_CHILD_APP_ID = "6t9ef3df-f966-5839-9123-8ki61dda7c88";
    private static final String INVALID_PARENT_APP_ID = "5y9ef3df-f966-5839-9693-8ki61dda7c77";

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        OrgApplicationMgtDataHolder.getInstance().setOrgApplicationMgtDAO(orgApplicationMgtDAO);
        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(applicationManagementService);

        orgApplicationManager = new OrgApplicationManagerImpl();
    }

    @DataProvider(name = "parentAppIdRetrievalTestData")
    public Object[][] getParentAppIdRetrievalTestData() {

        return new Object[][]{
                // Generic case.
                {CHILD_APP_ID, CHILD_ORG_ID, PARENT_APP_ID, PARENT_ORG_ID, ROOT_APP_ID, ROOT_ORG_ID, true,
                        PARENT_APP_ID},
                // Parent app is the root app.
                {CHILD_APP_ID, CHILD_ORG_ID, ROOT_APP_ID, ROOT_ORG_ID, ROOT_APP_ID, ROOT_ORG_ID, true, ROOT_APP_ID},
                // App is not shared with the parent app.
                {CHILD_APP_ID, CHILD_ORG_ID, null, PARENT_ORG_ID, ROOT_APP_ID, ROOT_ORG_ID, false, null},
                // Root app is passed as the child app.
                {ROOT_APP_ID, ROOT_ORG_ID, null, ROOT_ORG_ID, ROOT_APP_ID, ROOT_ORG_ID, true, null}
        };
    }

    @Test
    public void testGetAncestorAppIdsOfChildApp() throws Exception {

        List<String> ancestorOrganizationIds = new ArrayList<>();
        ancestorOrganizationIds.add(CHILD_ORG_ID);
        ancestorOrganizationIds.add(PARENT_ORG_ID);
        ancestorOrganizationIds.add(ROOT_ORG_ID);
        when(organizationManager.getAncestorOrganizationIds(CHILD_ORG_ID)).thenReturn(ancestorOrganizationIds);

        List<SharedApplicationDO> ancestorApplications = new ArrayList<>();
        SharedApplicationDO childApplicationDO = new SharedApplicationDO(CHILD_ORG_ID, CHILD_APP_ID);
        SharedApplicationDO parentApplicationDO = new SharedApplicationDO(PARENT_ORG_ID, PARENT_APP_ID);
        ancestorApplications.add(childApplicationDO);
        ancestorApplications.add(parentApplicationDO);

        MainApplicationDO mainApp = new MainApplicationDO(ROOT_ORG_ID, ROOT_APP_ID);
        when(orgApplicationMgtDAO.getMainApplication(CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(Optional.of(mainApp));
        when(orgApplicationMgtDAO.getSharedApplications(eq(ROOT_APP_ID), eq(ROOT_ORG_ID), anyList()))
                .thenReturn(ancestorApplications);

        Map<String, String> resolvedAncestorAppIds =
                orgApplicationManager.getAncestorAppIds(CHILD_APP_ID, CHILD_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 3);
        Assert.assertEquals(resolvedAncestorAppIds.get(CHILD_ORG_ID), CHILD_APP_ID);
        Assert.assertEquals(resolvedAncestorAppIds.get(PARENT_ORG_ID), PARENT_APP_ID);
        Assert.assertEquals(resolvedAncestorAppIds.get(ROOT_ORG_ID), ROOT_APP_ID);
    }

    @Test
    public void testGetAncestorAppIdsOfParentApp() throws Exception {

        List<String> ancestorOrganizationIds = new ArrayList<>();
        ancestorOrganizationIds.add(PARENT_ORG_ID);
        ancestorOrganizationIds.add(ROOT_ORG_ID);
        when(organizationManager.getAncestorOrganizationIds(PARENT_ORG_ID)).thenReturn(ancestorOrganizationIds);

        List<SharedApplicationDO> ancestorApplications = new ArrayList<>();
        SharedApplicationDO parentApplicationDO = new SharedApplicationDO(PARENT_ORG_ID, PARENT_APP_ID);
        ancestorApplications.add(parentApplicationDO);

        MainApplicationDO mainApp = new MainApplicationDO(ROOT_ORG_ID, ROOT_APP_ID);
        when(orgApplicationMgtDAO.getMainApplication(PARENT_APP_ID, PARENT_ORG_ID)).thenReturn(Optional.of(mainApp));
        when(orgApplicationMgtDAO.getSharedApplications(eq(ROOT_APP_ID), eq(ROOT_ORG_ID), anyList()))
                .thenReturn(ancestorApplications);

        Map<String, String> resolvedAncestorAppIds =
                orgApplicationManager.getAncestorAppIds(PARENT_APP_ID, PARENT_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 2);
        Assert.assertEquals(resolvedAncestorAppIds.get(PARENT_ORG_ID), PARENT_APP_ID);
        Assert.assertEquals(resolvedAncestorAppIds.get(ROOT_ORG_ID), ROOT_APP_ID);
    }

    @Test
    public void testGetAncestorAppIdsOfRootApp() throws Exception {

        when(orgApplicationMgtDAO.getMainApplication(ROOT_APP_ID, ROOT_ORG_ID)).thenReturn(Optional.empty());

        when(organizationManager.resolveTenantDomain(ROOT_ORG_ID)).thenReturn(ROOT_TENANT_DOMAIN);

        ServiceProvider rootApp = mock(ServiceProvider.class);
        ServiceProviderProperty isFragmentAppProperty = new ServiceProviderProperty();
        isFragmentAppProperty.setName(IS_FRAGMENT_APP);
        isFragmentAppProperty.setValue("false");
        when(rootApp.getSpProperties()).thenReturn(new ServiceProviderProperty[]{isFragmentAppProperty});
        when(applicationManagementService.getApplicationByResourceId(ROOT_APP_ID, ROOT_TENANT_DOMAIN)).thenReturn(
                rootApp);

        Map<String, String> resolvedAncestorAppIds =
                orgApplicationManager.getAncestorAppIds(ROOT_APP_ID, ROOT_ORG_ID);

        Assert.assertNotNull(resolvedAncestorAppIds);
        Assert.assertEquals(resolvedAncestorAppIds.size(), 1);
    }

    @Test
    public void testGetAncestorAppIdsWithInvalidChildAppId() throws Exception {

        when(orgApplicationMgtDAO.getMainApplication(INVALID_CHILD_APP_ID, CHILD_ORG_ID)).thenReturn(Optional.empty());

        Map<String, String> resolvedAncestorAppIds =
                orgApplicationManager.getAncestorAppIds(INVALID_CHILD_APP_ID, CHILD_ORG_ID);

        Assert.assertEquals(resolvedAncestorAppIds, Collections.emptyMap());
    }

    @DataProvider(name = "childAppIdsRetrievalTestData")
    public Object[][] getChildAppIdsRetrievalTestData() {

        List<String> childOrgIds = new ArrayList<>(childAppIdMap.keySet());

        return new Object[][]{
                // Generic case.
                {PARENT_APP_ID, PARENT_ORG_ID, PARENT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, true, childOrgIds,
                        childAppIdMap, childAppIdMap},
                // App is not shared with child organizations.
                {PARENT_APP_ID, PARENT_ORG_ID, PARENT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, true, childOrgIds,
                        Collections.emptyMap(), Collections.emptyMap()},
                // Passing an empty list of child organizations.
                {PARENT_APP_ID, PARENT_ORG_ID, PARENT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, true,
                        Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap()},
                // Passing a null list of child organizations.
                {PARENT_APP_ID, PARENT_ORG_ID, PARENT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, true, null,
                        Collections.emptyMap(), Collections.emptyMap()},
                // Parent app is the root app.
                {ROOT_APP_ID, ROOT_ORG_ID, ROOT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, false, childOrgIds,
                        childAppIdMap, childAppIdMap},
                // Parent app is the root app but app is not shared with child organizations.
                {ROOT_APP_ID, ROOT_ORG_ID, ROOT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, false, childOrgIds,
                        Collections.emptyMap(), Collections.emptyMap()},
                // Parent app is the root app and passing an empty list of child organizations.
                {ROOT_APP_ID, ROOT_ORG_ID, ROOT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, false, Collections.emptyList(),
                        Collections.emptyMap(), Collections.emptyMap()},
                // Parent app is the root app and passing a null list of child organizations.
                {ROOT_APP_ID, ROOT_ORG_ID, ROOT_TENANT_DOMAIN, ROOT_APP_ID, ROOT_ORG_ID, false, null,
                        Collections.emptyMap(), Collections.emptyMap()},
        };
    }

    @Test(dataProvider = "childAppIdsRetrievalTestData")
    public void testGetChildAppIds(String parentAppId, String parentOrgId, String parentTenantDomain, String rootAppId,
                                   String rootOrgId, boolean isFragmentApp, List<String> childOrgIds,
                                   Map<String, String> childAppIdMap, Map<String, String> expectedChildAppIdMap)
            throws Exception {

        when(organizationManager.resolveTenantDomain(parentOrgId)).thenReturn(parentTenantDomain);

        ServiceProvider parentApp = mock(ServiceProvider.class);
        ServiceProviderProperty isFragmentAppProperty = new ServiceProviderProperty();
        isFragmentAppProperty.setName(IS_FRAGMENT_APP);
        isFragmentAppProperty.setValue(String.valueOf(isFragmentApp));
        when(parentApp.getSpProperties()).thenReturn(new ServiceProviderProperty[]{isFragmentAppProperty});
        when(applicationManagementService.getApplicationByResourceId(parentAppId, parentTenantDomain)).thenReturn(
                parentApp);

        List<SharedApplicationDO> sharedApplications = childAppIdMap.entrySet().stream()
                .map(app -> new SharedApplicationDO(app.getKey(), app.getValue()))
                .collect(Collectors.toList());

        MainApplicationDO mainApp = new MainApplicationDO(rootOrgId, rootAppId);
        when(orgApplicationMgtDAO.getMainApplication(parentAppId, parentOrgId)).thenReturn(Optional.of(mainApp));
        when(orgApplicationMgtDAO.getSharedApplications(rootAppId, rootOrgId, childOrgIds))
                .thenReturn(sharedApplications);

        Map<String, String> resolvedChildAppIdMap =
                orgApplicationManager.getChildAppIds(parentAppId, parentOrgId, childOrgIds);

        Assert.assertEquals(resolvedChildAppIdMap, expectedChildAppIdMap);
    }

    @Test
    public void testGetChildAppIdsWithInvalidParentAppId() throws Exception {

        when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_TENANT_DOMAIN);
        when(applicationManagementService.getApplicationByResourceId(INVALID_PARENT_APP_ID,
                PARENT_TENANT_DOMAIN)).thenReturn(null);
        when(orgApplicationMgtDAO.getMainApplication(INVALID_PARENT_APP_ID, PARENT_ORG_ID)).thenReturn(
                Optional.empty());

        Map<String, String> resolvedChildAppIdMap =
                orgApplicationManager.getChildAppIds(INVALID_PARENT_APP_ID, PARENT_ORG_ID,
                        new ArrayList<>(childAppIdMap.keySet()));

        Assert.assertEquals(resolvedChildAppIdMap, Collections.emptyMap());
    }

    @Test
    public void testGetDiscoverableSharedApplicationBasicInfo() throws OrganizationManagementException {

        int limit = 10;
        int offset = 0;
        String filter = "*";
        String sortOrder = "ASC";
        String sortBy = "name";
        String tenantDomain = "abc.com";
        String rootOrgId = "root-org-id";

        ApplicationBasicInfo app1 = new ApplicationBasicInfo();
        app1.setApplicationName("App1");
        app1.setDescription("Description1");
        ApplicationBasicInfo app2 = new ApplicationBasicInfo();
        app2.setApplicationName("App2");
        app2.setDescription("Description2");
        List<ApplicationBasicInfo> expectedApps = Arrays.asList(app1, app2);
        when(organizationManager.getPrimaryOrganizationId(tenantDomain)).thenReturn(rootOrgId);
        when(orgApplicationMgtDAO.getDiscoverableSharedApplicationBasicInfo(limit, offset, filter,
                sortOrder, sortBy, tenantDomain, rootOrgId)).thenReturn(expectedApps);
        List<ApplicationBasicInfo> actualApps = orgApplicationManager.getDiscoverableSharedApplicationBasicInfo(
                limit, offset, filter, sortOrder, sortBy, tenantDomain);

        assertEquals(expectedApps, actualApps);
    }

    @Test
    public void testGetCountOfDiscoverableSharedApplications() throws OrganizationManagementException {

        String filter = "*";
        String tenantDomain = "dummyTenant";
        String rootOrgId = "rootOrg123";
        int expectedCount = 5;

        when(organizationManager.getPrimaryOrganizationId(tenantDomain)).thenReturn(rootOrgId);
        when(orgApplicationMgtDAO.getCountOfDiscoverableSharedApplications(filter, tenantDomain, rootOrgId))
                .thenReturn(expectedCount);

        int actualCount = orgApplicationManager.getCountOfDiscoverableSharedApplications(filter, tenantDomain);

        assertEquals(expectedCount, actualCount);
    }
}
