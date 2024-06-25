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
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

public class OrgApplicationManagerImplTest {

    @Mock
    private OrgApplicationMgtDAO orgApplicationMgtDAO;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private ApplicationManagementService applicationManagementService;

    private static final Map<String, String> childAppIdMap = new HashMap<String, String>() {{
        put("30b701c6-e309-4241-b047-0c299c45d1a0", "42ef1d92-add6-449b-8a3c-fc308d2a4eac");
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

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        OrgApplicationMgtDataHolder.getInstance().setOrgApplicationMgtDAO(orgApplicationMgtDAO);
        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
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

    @Test(dataProvider = "parentAppIdRetrievalTestData")
    public void testGetParentAppId(String childAppId, String childOrgId, String parentAppId,
                                   String parentOrgId, String rootAppId, String rootOrgId,
                                   boolean isAppSharedWithParent, String expectedParentAppId)
            throws Exception {

        MainApplicationDO mainApp = new MainApplicationDO(rootOrgId, rootAppId);
        if (parentAppId != null) {
            when(orgApplicationMgtDAO.getMainApplication(childAppId, childOrgId)).thenReturn(Optional.of(mainApp));
            when(orgApplicationMgtDAO.getParentAppId(rootAppId, rootOrgId, parentOrgId)).thenReturn(parentAppId);
        } else if (isAppSharedWithParent) {
            when(orgApplicationMgtDAO.getMainApplication(childAppId, childOrgId)).thenReturn(Optional.empty());
        } else {
            when(orgApplicationMgtDAO.getMainApplication(childAppId, childOrgId)).thenReturn(Optional.of(mainApp));
            when(orgApplicationMgtDAO.getParentAppId(rootAppId, rootOrgId, parentOrgId)).thenReturn(null);
        }

        OrgApplicationManager orgApplicationManager = new OrgApplicationManagerImpl();
        String resolvedParentAppId = orgApplicationManager.getParentAppId(childAppId, childOrgId, parentOrgId);

        Assert.assertEquals(resolvedParentAppId, expectedParentAppId);
    }

    @Test
    public void testGetParentAppIdWithInvalidChildAppIds() throws Exception {

        String invalidChildAppId = "invalid-child-app-id";
        when(orgApplicationMgtDAO.getMainApplication(invalidChildAppId, CHILD_ORG_ID)).thenReturn(Optional.empty());

        OrgApplicationManager orgApplicationManager = new OrgApplicationManagerImpl();
        String resolvedParentAppId = orgApplicationManager.getParentAppId(invalidChildAppId, CHILD_ORG_ID,
                PARENT_ORG_ID);

        Assert.assertNull(resolvedParentAppId);
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
                .map(app -> {
                    return new SharedApplicationDO(app.getKey(), app.getValue());
                })
                .collect(Collectors.toList());

        if (isFragmentApp) {
            MainApplicationDO mainApp = new MainApplicationDO(rootOrgId, rootAppId);
            when(orgApplicationMgtDAO.getMainApplication(parentAppId, parentOrgId)).thenReturn(Optional.of(mainApp));
            when(orgApplicationMgtDAO.getSharedApplications(rootOrgId, rootAppId)).thenReturn(sharedApplications);
        } else {
            when(orgApplicationMgtDAO.getSharedApplications(parentOrgId, parentAppId)).thenReturn(sharedApplications);
        }

        OrgApplicationManager orgApplicationManager = new OrgApplicationManagerImpl();
        Map<String, String> resolvedChildAppIdMap =
                orgApplicationManager.getChildAppIds(parentAppId, parentOrgId, childOrgIds);

        Assert.assertEquals(resolvedChildAppIdMap, expectedChildAppIdMap);
    }

    @Test
    public void testGetChildAppIdsWithInvalidChildAppIds() throws Exception {

        String invalidParentAppId = "invalid-parent-app-id";
        when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_TENANT_DOMAIN);
        when(applicationManagementService.getApplicationByResourceId(invalidParentAppId,
                PARENT_TENANT_DOMAIN)).thenReturn(null);
        when(orgApplicationMgtDAO.getMainApplication(invalidParentAppId, PARENT_ORG_ID)).thenReturn(Optional.empty());

        OrgApplicationManager orgApplicationManager = new OrgApplicationManagerImpl();
        Map<String, String> resolvedChildAppIdMap =
                orgApplicationManager.getChildAppIds(invalidParentAppId, PARENT_ORG_ID,
                        new ArrayList<>(childAppIdMap.keySet()));

        Assert.assertEquals(resolvedChildAppIdMap, Collections.emptyMap());
    }
}
