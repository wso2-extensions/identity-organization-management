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

package org.wso2.carbon.identity.organization.management.application;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.service.AsyncOperationStatusMgtService;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.listener.ApplicationSharingManagerListener;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.GeneralApplicationShareOperation;
import org.wso2.carbon.identity.organization.management.application.model.operation.SelectiveShareApplicationOperation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationNode;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_DEFAULT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SKIP_ORGANIZATION_HIERARCHY_VALIDATION;

/**
 * Unit tests for OrgApplicationManagerImpl.
 */
@WithCarbonHome
public class OrgApplicationManagerImplTest {

    @Mock
    private OrgApplicationMgtDAO orgApplicationMgtDAO;
    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private ApplicationManagementService applicationManagementService;
    @Mock
    private AsyncOperationStatusMgtService asyncOperationStatusMgtService;
    @Mock
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;
    @Mock
    private ApplicationSharingManagerListener listener;
    @Mock
    private OrgApplicationMgtDataHolder orgApplicationMgtDataHolder;
    @Mock
    private RealmService realmService;
    @Mock
    private UserRealm userRealm;
    @Mock
    private RealmConfiguration realmConfiguration;
    @Mock
    private OrganizationUserResidentResolverService organizationUserResidentResolverService;
    @Mock
    private User mockUser;
    @Mock
    private org.wso2.carbon.identity.application.common.model.User mockAppOwner;
    @Mock
    private OAuthAdminServiceImpl oAuthAdminService;
    @Mock
    private IdpManager idpManager;
    @Mock
    private OrgApplicationMgtDataHolder mockOrgApplicationMgtDataHolder;
    @Mock
    private RoleManagementService roleManagementService;

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
        OrgApplicationMgtDataHolder.getInstance().setIdpManager(idpManager);

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

        String tenantDomain = "abc.com";
        String organizationId = "1111111-222222";
        String rootOrgId = "root-org-id";

        ApplicationBasicInfo app1 = new ApplicationBasicInfo();
        app1.setApplicationName("App1");
        app1.setDescription("Description1");
        ApplicationBasicInfo app2 = new ApplicationBasicInfo();
        app2.setApplicationName("App2");
        app2.setDescription("Description2");
        List<ApplicationBasicInfo> expectedApps = Arrays.asList(app1, app2);
        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationId);
        when(organizationManager.getPrimaryOrganizationId(organizationId)).thenReturn(rootOrgId);
        when(orgApplicationMgtDAO.getDiscoverableSharedApplicationBasicInfo(10, 0, "*",
                "ASC", "dummyName", tenantDomain, rootOrgId)).thenReturn(expectedApps);
        List<ApplicationBasicInfo> actualApps = orgApplicationManager.getDiscoverableSharedApplicationBasicInfo(
                10, 0, "*", "ASC", "dummyName", tenantDomain);
        assertEquals(actualApps, expectedApps);
    }

    @Test
    public void testGetCountOfDiscoverableSharedApplications() throws OrganizationManagementException {

        int expectedCount = 5;
        String tenantDomain = "dummyTenant";
        String organizationId = "1111111-222222";
        String rootOrgId = "rootOrg123";
        when(organizationManager.resolveOrganizationId(tenantDomain)).thenReturn(organizationId);
        when(organizationManager.getPrimaryOrganizationId(organizationId)).thenReturn(rootOrgId);
        when(orgApplicationMgtDAO.getCountOfDiscoverableSharedApplications("*", tenantDomain,
                rootOrgId)).thenReturn(5);

        int actualCount = orgApplicationManager.getCountOfDiscoverableSharedApplications("*", tenantDomain);
        assertEquals(actualCount, expectedCount);
    }

    @Test
    public void testShareApplication() throws OrganizationManagementException, UserStoreException {

        String ownerOrgId = "ownerOrgId";
        String sharedOrgId = "sharedOrgId";
        ServiceProvider mainApplication = mock(ServiceProvider.class);
        boolean shareWithAllChildren = true;
        String sharedTenantDomain = "sharedTenantDomain";
        int tenantId = 1;
        String adminUserId = "adminUserId";
        String domainQualifiedUserName = "domainQualifiedUserName";
        String sharedApplicationId = "sharedApplicationId";
        String mainApplicationId = "mainApplicationId";
        OAuthConsumerAppDTO createdOAuthApp = mock(OAuthConsumerAppDTO.class);

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
             MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class);
             MockedStatic<MultitenantUtils> multitenantUtilsMockedStatic = mockStatic(MultitenantUtils.class)) {

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(orgApplicationMgtDataHolder);
            when(orgApplicationMgtDataHolder.getApplicationSharingManagerListener()).thenReturn(listener);
            when(orgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveTenantDomain(sharedOrgId)).thenReturn(sharedTenantDomain);
            identityTenantUtilMockedStatic.when(
                    () -> IdentityTenantUtil.getTenantId(sharedTenantDomain)).thenReturn(tenantId);
            when(orgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            when(realmService.getTenantUserRealm(tenantId)).thenReturn(userRealm);
            when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
            when(realmConfiguration.getAdminUserId()).thenReturn(adminUserId);
            when(orgApplicationMgtDataHolder.getRoleManagementServiceV2())
                    .thenReturn(roleManagementService);

            // Get Domain Qualified Username.
            when(orgApplicationMgtDataHolder.getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            when(orgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(anyString(), anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(organizationUserResidentResolverService
                    .resolveUserFromResidentOrganization(null, adminUserId, sharedOrgId))
                    .thenReturn(Optional.of(mockUser));
            when(mockUser.getDomainQualifiedUsername()).thenReturn(domainQualifiedUserName);
            when(mainApplication.getOwner()).thenReturn(mockAppOwner);
            when(mainApplication.getApplicationResourceId()).thenReturn(mainApplicationId);
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername(null))
                    .thenReturn(domainQualifiedUserName);

            // Resolve Shared App.
            when(orgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);

            // Resolve Urls.
            when(orgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(organizationManager.resolveTenantDomain(ownerOrgId)).thenReturn("ownerTenantDomain");

            Organization mockSubOrg = new Organization();
            mockSubOrg.setId(sharedOrgId);
            ParentOrganizationDO parentOrganizationDO = new ParentOrganizationDO();
            parentOrganizationDO.setId(ownerOrgId);
            mockSubOrg.setParent(parentOrganizationDO);
            when(organizationManager.getOrganization(sharedOrgId, false, false))
                    .thenReturn(mockSubOrg);

            mockServiceURLBuilder("https://localhost:8080", serviceURLBuilder);

            // Create Oauth Application.
            when(orgApplicationMgtDataHolder.getOAuthAdminService()).thenReturn(oAuthAdminService);
            when(oAuthAdminService.registerAndRetrieveOAuthApplicationData(any())).thenReturn(createdOAuthApp);

            // Prepare shared application.
            when(orgApplicationMgtDataHolder
                    .getApplicationManagementService()).thenReturn(applicationManagementService);
            when(applicationManagementService.createApplication(any(ServiceProvider.class), anyString(), anyString()))
                    .thenReturn(sharedApplicationId);

            when(orgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService()).thenReturn(
                    resourceSharingPolicyHandlerService);
            lenient().doNothing().when(resourceSharingPolicyHandlerService)
                    .deleteResourceSharingPolicyInOrgByResourceTypeAndId(anyString(), any(), anyString(), anyString());

            orgApplicationManager.shareApplication(
                    ownerOrgId, sharedOrgId, mainApplication, shareWithAllChildren, null);
        } catch (URLBuilderException | IdentityOAuthAdminException | IdentityApplicationManagementException |
                 ResourceSharingPolicyMgtException e) {
            throw new RuntimeException(e);
        }
    }

    private void mockServiceURLBuilder(String url, MockedStatic<ServiceURLBuilder> serviceURLBuilder)
            throws URLBuilderException {

        ServiceURLBuilder mockServiceURLBuilder = mock(ServiceURLBuilder.class);
        serviceURLBuilder.when(ServiceURLBuilder::create).thenReturn(mockServiceURLBuilder);
        lenient().when(mockServiceURLBuilder.addPath(any())).thenReturn(mockServiceURLBuilder);
        lenient().when(mockServiceURLBuilder.setTenant(any())).thenReturn(mockServiceURLBuilder);

        ServiceURL serviceURL = mock(ServiceURL.class);
        lenient().when(serviceURL.getAbsolutePublicURL()).thenReturn(url);
        lenient().when(mockServiceURLBuilder.build()).thenReturn(serviceURL);
    }

    @Test
    public void testShareApplicationWithSelectedOrganizations_SingleOrganization_AllRolesPolicy() throws Exception {

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = 
                     mockStatic(IdentityTenantUtil.class)) {

            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn("test-org-id");
            
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("main-tenant-domain"))
                    .thenReturn(-1234);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("test-tenant"))
                    .thenReturn(-1234);
            
            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);
            when(mockOrgApplicationMgtDataHolder.getIdpManager()).thenReturn(idpManager);
            when(mockOrgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            lenient().when(mockOrgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            lenient().when(mockOrgApplicationMgtDataHolder.getApplicationSharingManagerListener())
                    .thenReturn(listener);
            lenient().when(mockOrgApplicationMgtDataHolder.getIdentityEventService())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getRoleManagementServiceV2())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            lenient().when(mockOrgApplicationMgtDataHolder.getAsyncOperationStatusMgtService())
                    .thenReturn(asyncOperationStatusMgtService);

            when(orgApplicationMgtDAO.getSharedApplications(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            OrgApplicationManager testOrgApplicationManager = new OrgApplicationManagerImpl();

            String mainOrgId = "main-org-id";
            String mainAppId = "main-app-id";
            String childOrgId = "child-org-id";

            ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.ALL)
                    .build();
            SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                    childOrgId,
                    PolicyEnum.SELECTED_ORG_ONLY,
                    allRolesPolicy
            );
            List<SelectiveShareApplicationOperation> shareOperations = Collections.singletonList(shareOperation);

            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);
            when(applicationManagementService.getAllIdentityProviders("main-tenant-domain"))
                    .thenReturn(new IdentityProvider[0]);

            IdentityProvider mockCreatedIdp = mock(IdentityProvider.class);
            lenient().when(idpManager.addIdPWithResourceId(any(IdentityProvider.class), eq("main-tenant-domain")))
                    .thenReturn(mockCreatedIdp);

            List<OrganizationNode> childGraph = createMockOrganizationGraph(childOrgId);
            when(organizationManager.getChildOrganizationGraph(mainOrgId, true)).thenReturn(childGraph);
            Organization childOrg = createMockOrganization(childOrgId);
            when(organizationManager.getOrganization(childOrgId, false, false)).thenReturn(childOrg);
            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");

            testOrgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId, shareOperations);

            verify(organizationManager).resolveTenantDomain(mainOrgId);
            verify(organizationManager, times(1)).getChildOrganizationGraph(mainOrgId, true);
            verify(organizationManager).getOrganization(childOrgId, false, false);
            verify(applicationManagementService).getApplicationByResourceId(mainAppId, "main-tenant-domain");
            verify(applicationManagementService).getAllIdentityProviders("main-tenant-domain");
        }
    }

    @Test
    public void testShareApplicationWithSelectedOrganizations_MultipleOrganizations_HierarchyRespected()
            throws Exception {

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = 
                     mockStatic(IdentityTenantUtil.class)) {

            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn("test-org-id");

            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("main-tenant-domain"))
                    .thenReturn(-1234);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("test-tenant"))
                    .thenReturn(-1234);

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);
            when(mockOrgApplicationMgtDataHolder.getIdpManager()).thenReturn(idpManager);
            when(mockOrgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            lenient().when(mockOrgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            lenient().when(mockOrgApplicationMgtDataHolder.getApplicationSharingManagerListener()).thenReturn(listener);
            lenient().when(mockOrgApplicationMgtDataHolder.getIdentityEventService()).thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getRoleManagementServiceV2()).thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            lenient().when(mockOrgApplicationMgtDataHolder.getAsyncOperationStatusMgtService())
                    .thenReturn(asyncOperationStatusMgtService);
            when(orgApplicationMgtDAO.getSharedApplications(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            OrgApplicationManager testOrgApplicationManager = new OrgApplicationManagerImpl();

            // Setup hierarchy: Main -> Child1 -> GrandChild1, Main -> Child2.
            String mainOrgId = "main-org-id";
            String mainAppId = "main-app-id";
            String child1OrgId = "child1-org-id";
            String grandChild1OrgId = "grandchild1-org-id";
            String child2OrgId = "child2-org-id";

            ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.ALL)
                    .build();
            List<SelectiveShareApplicationOperation> shareOperations = Arrays.asList(
                    new SelectiveShareApplicationOperation(child1OrgId, PolicyEnum.SELECTED_ORG_ONLY, allRolesPolicy),
                    new SelectiveShareApplicationOperation(grandChild1OrgId, PolicyEnum.SELECTED_ORG_ONLY,
                            allRolesPolicy),
                    new SelectiveShareApplicationOperation(child2OrgId, PolicyEnum.SELECTED_ORG_ONLY, allRolesPolicy)
            );

            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);
            when(applicationManagementService.getAllIdentityProviders("main-tenant-domain"))
                    .thenReturn(new IdentityProvider[0]);

            IdentityProvider mockCreatedIdp = mock(IdentityProvider.class);
            lenient().when(idpManager.addIdPWithResourceId(any(IdentityProvider.class), eq("main-tenant-domain")))
                    .thenReturn(mockCreatedIdp);

            List<OrganizationNode> childGraph = createMockComplexOrganizationGraph(child1OrgId, grandChild1OrgId,
                    child2OrgId);
            when(organizationManager.getChildOrganizationGraph(mainOrgId, true)).thenReturn(childGraph);
            when(organizationManager.getOrganization(child1OrgId, false, false))
                    .thenReturn(createMockOrganization(child1OrgId));
            when(organizationManager.getOrganization(grandChild1OrgId, false, false))
                    .thenReturn(createMockOrganization(grandChild1OrgId));
            when(organizationManager.getOrganization(child2OrgId, false, false))
                    .thenReturn(createMockOrganization(child2OrgId));

            testOrgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId, shareOperations);

            verify(organizationManager).getOrganization(child1OrgId, false, false);
            verify(organizationManager).getOrganization(grandChild1OrgId, false, false);
            verify(organizationManager).getOrganization(child2OrgId, false, false);
        }
    }

    @Test
    public void testShareApplicationWithSelectedOrganizations_SelectedRolesPolicy() throws Exception {

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = 
                     mockStatic(IdentityTenantUtil.class)) {

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);
            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn("test-org-id");
            
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("main-tenant-domain"))
                    .thenReturn(-1234);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("test-tenant"))
                    .thenReturn(-1234);
            
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);
            when(mockOrgApplicationMgtDataHolder.getIdpManager()).thenReturn(idpManager);
            when(mockOrgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            lenient().when(mockOrgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            lenient().when(mockOrgApplicationMgtDataHolder.getApplicationSharingManagerListener())
                    .thenReturn(listener);
            lenient().when(mockOrgApplicationMgtDataHolder.getIdentityEventService())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getRoleManagementServiceV2())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            lenient().when(mockOrgApplicationMgtDataHolder.getAsyncOperationStatusMgtService())
                    .thenReturn(asyncOperationStatusMgtService);

            when(orgApplicationMgtDAO.getSharedApplications(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            OrgApplicationManager testOrgApplicationManager = new OrgApplicationManagerImpl();

            String mainOrgId = "main-org-id";
            String mainAppId = "main-app-id";
            String childOrgId = "child-org-id";

            List<RoleWithAudienceDO> specificRoles = Arrays.asList(
                    createMockRoleWithAudience("admin-role", "APPLICATION", mainAppId),
                    createMockRoleWithAudience("user-role", "APPLICATION", mainAppId)
            );
            ApplicationShareRolePolicy selectedRolesPolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.SELECTED)
                    .roleWithAudienceDOList(specificRoles)
                    .build();
            SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                    childOrgId,
                    PolicyEnum.SELECTED_ORG_ONLY,
                    selectedRolesPolicy
            );

            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");

            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);

            when(applicationManagementService.getAllIdentityProviders("main-tenant-domain"))
                    .thenReturn(new IdentityProvider[0]);

            IdentityProvider mockCreatedIdp = mock(IdentityProvider.class);
            lenient().when(idpManager.addIdPWithResourceId(any(IdentityProvider.class), eq("main-tenant-domain")))
                    .thenReturn(mockCreatedIdp);

            List<OrganizationNode> childGraph = createMockOrganizationGraph(childOrgId);
            when(organizationManager.getChildOrganizationGraph(mainOrgId, true)).thenReturn(childGraph);
            Organization childOrg = createMockOrganization(childOrgId);
            when(organizationManager.getOrganization(childOrgId, false, false)).thenReturn(childOrg);

            testOrgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId,
                    Arrays.asList(shareOperation));

            verify(organizationManager).resolveTenantDomain(mainOrgId);
            verify(organizationManager).getOrganization(childOrgId, false, false);
            verify(applicationManagementService).getApplicationByResourceId(mainAppId, "main-tenant-domain");
            verify(applicationManagementService).getAllIdentityProviders("main-tenant-domain");
        }
    }

    @Test
    public void testShareApplicationWithSelectedOrganizations_NoRolesPolicy() throws Exception {

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = 
                     mockStatic(IdentityTenantUtil.class)) {

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);
            
            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn("test-org-id");
            
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("main-tenant-domain"))
                    .thenReturn(-1234);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("test-tenant"))
                    .thenReturn(-1234);
            
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);
            when(mockOrgApplicationMgtDataHolder.getIdpManager()).thenReturn(idpManager);
            when(mockOrgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            lenient().when(mockOrgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            lenient().when(mockOrgApplicationMgtDataHolder.getApplicationSharingManagerListener())
                    .thenReturn(listener);
            lenient().when(mockOrgApplicationMgtDataHolder.getIdentityEventService())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getRoleManagementServiceV2())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            lenient().when(mockOrgApplicationMgtDataHolder.getAsyncOperationStatusMgtService())
                    .thenReturn(asyncOperationStatusMgtService);

            when(orgApplicationMgtDAO.getSharedApplications(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            OrgApplicationManager testOrgApplicationManager = new OrgApplicationManagerImpl();

            String mainOrgId = "main-org-id";
            String mainAppId = "main-app-id";
            String childOrgId = "child-org-id";

            ApplicationShareRolePolicy noRolesPolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.NONE)
                    .build();
            SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                    childOrgId,
                    PolicyEnum.SELECTED_ORG_ONLY,
                    noRolesPolicy
            );
            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);

            when(applicationManagementService.getAllIdentityProviders("main-tenant-domain"))
                    .thenReturn(new IdentityProvider[0]);

            IdentityProvider mockCreatedIdp = mock(IdentityProvider.class);
            lenient().when(idpManager.addIdPWithResourceId(any(IdentityProvider.class), eq("main-tenant-domain")))
                    .thenReturn(mockCreatedIdp);

            List<OrganizationNode> childGraph = createMockOrganizationGraph(childOrgId);
            when(organizationManager.getChildOrganizationGraph(mainOrgId, true)).thenReturn(childGraph);

            Organization childOrg = createMockOrganization(childOrgId);
            when(organizationManager.getOrganization(childOrgId, false, false)).thenReturn(childOrg);

            testOrgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId,
                    Collections.singletonList(shareOperation));

            verify(organizationManager).getOrganization(childOrgId, false, false);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
          expectedExceptionsMessageRegExp = ".*Invalid organization share configuration.*")
    public void testShareApplicationWithSelectedOrganizations_NullConfigList() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";

        orgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId, null);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
          expectedExceptionsMessageRegExp = ".*Invalid sharing organization ID.*")
    public void testShareApplicationWithSelectedOrganizations_BlankOrganizationId() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";

        ApplicationShareRolePolicy rolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();
        SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                "",
                PolicyEnum.SELECTED_ORG_ONLY,
                rolePolicy
        );
        orgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId,
                Collections.singletonList(shareOperation));
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
          expectedExceptionsMessageRegExp = ".*Invalid or empty sharing policy.*")
    public void testShareApplicationWithSelectedOrganizations_NullPolicy() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";
        String childOrgId = "child-org-id";

        ApplicationShareRolePolicy rolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();
        SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                childOrgId,
                null,
                rolePolicy
        );
        orgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId,
                Collections.singletonList(shareOperation));
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
          expectedExceptionsMessageRegExp = ".*Invalid role sharing operation.*")
    public void testShareApplicationWithSelectedOrganizations_NullRoleSharing() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";
        String childOrgId = "child-org-id";

        SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                childOrgId,
                PolicyEnum.SELECTED_ORG_ONLY,
                null
        );
        orgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId,
                Collections.singletonList(shareOperation));
    }

    @Test(expectedExceptions = IllegalStateException.class,
          expectedExceptionsMessageRegExp = ".*Role sharing mode must be set.*")
    public void testShareApplicationWithSelectedOrganizations_NullRoleSharingMode() {

        // Create role policy with null mode (this would fail during builder validation).
        ApplicationShareRolePolicy invalidRolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(null)
                .build();
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testShareApplicationWithSelectedOrganizations_AlreadySharedApplication() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";
        String childOrgId = "child-org-id";

        ApplicationShareRolePolicy rolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();
        SelectiveShareApplicationOperation shareOperation = new SelectiveShareApplicationOperation(
                childOrgId,
                PolicyEnum.SELECTED_ORG_ONLY,
                rolePolicy
        );
        when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");

        // Create already shared application (fragment app).
        ServiceProvider sharedApplication = createMockServiceProvider("shared-app", true);
        when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                .thenReturn(sharedApplication);
        orgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId,
                Collections.singletonList(shareOperation));
    }

    @Test
    public void testShareApplicationWithSelectedOrganizations_EmptyConfigList() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";

        when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
        ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
        when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                .thenReturn(mainApplication);

        // Execute with empty list - should return early without error.
        orgApplicationManager.shareApplicationWithSelectedOrganizations(mainOrgId, mainAppId, Collections.emptyList());

        verify(organizationManager).resolveTenantDomain(mainOrgId);
        verify(applicationManagementService).getApplicationByResourceId(mainAppId, "main-tenant-domain");
    }

    // ========================================
    // Test Methods for shareApplicationWithAllOrganizations
    // ========================================

    @Test
    public void testShareApplicationWithAllOrganizations_AllExistingAndFutureOrgs() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";

        ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();
        GeneralApplicationShareOperation generalOperation = new GeneralApplicationShareOperation(
                PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS,
                allRolesPolicy
        );

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic = 
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = 
                     mockStatic(IdentityTenantUtil.class)) {
            
            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn("test-org-id");
            
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("main-tenant-domain"))
                    .thenReturn(-1234);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("test-tenant"))
                    .thenReturn(-1234);

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);
            when(mockOrgApplicationMgtDataHolder.getIdpManager()).thenReturn(idpManager);
            when(mockOrgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            lenient().when(mockOrgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            lenient().when(mockOrgApplicationMgtDataHolder
                    .getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            lenient().when(mockOrgApplicationMgtDataHolder.getApplicationSharingManagerListener())
                    .thenReturn(listener);
            lenient().when(mockOrgApplicationMgtDataHolder.getIdentityEventService())
                    .thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getRoleManagementServiceV2())
                    .thenReturn(roleManagementService);
            lenient().when(mockOrgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            lenient().when(mockOrgApplicationMgtDataHolder.getAsyncOperationStatusMgtService())
                    .thenReturn(asyncOperationStatusMgtService);

            when(orgApplicationMgtDAO.getSharedApplications(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            OrgApplicationManager testOrgApplicationManager = new OrgApplicationManagerImpl();
            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);
            when(applicationManagementService.getAllIdentityProviders("main-tenant-domain"))
                    .thenReturn(new IdentityProvider[0]);

            List<BasicOrganization> childOrganizations = Collections.singletonList(
                    createMockBasicOrganization("child1-org-id", "child1-name")
                                                                                  );
            when(organizationManager.getChildOrganizations(mainOrgId, true)).thenReturn(childOrganizations);

            List<OrganizationNode> childGraph = createMockOrganizationGraph("child1-org-id");
            when(organizationManager.getChildOrganizationGraph(mainOrgId, true)).thenReturn(childGraph);
            Organization childOrg = createMockOrganization("child1-org-id");
            when(organizationManager.getOrganization("child1-org-id", false, false)).thenReturn(childOrg);

            testOrgApplicationManager.shareApplicationWithAllOrganizations(mainOrgId, mainAppId, generalOperation);

            verify(organizationManager).getChildOrganizations(mainOrgId, true);
            verify(organizationManager).getOrganization("child1-org-id", false, false);
        }
    }

    @Test
    public void testShareApplicationWithAllOrganizations_AllExistingOrgsOnly() throws Exception {


        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {

            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);

            String mainOrgId = "main-org-id";
            String mainAppId = "main-app-id";

            ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.ALL)
                    .build();
            GeneralApplicationShareOperation generalOperation = new GeneralApplicationShareOperation(
                    PolicyEnum.ALL_EXISTING_ORGS_ONLY,
                    allRolesPolicy
            );
            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);
            when(organizationManager.getChildOrganizations(mainOrgId, true)).thenReturn(Collections.emptyList());

            when(mockOrgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);

            // Execute - should return early without error.
            orgApplicationManager.shareApplicationWithAllOrganizations(mainOrgId, mainAppId, generalOperation);

            verify(organizationManager).resolveTenantDomain(mainOrgId);
            verify(organizationManager).getChildOrganizations(mainOrgId, true);
            verify(applicationManagementService).getApplicationByResourceId(mainAppId, "main-tenant-domain");
        }
    }

    @DataProvider(name = "ConsoleOrMyAccountTestData")
    public Object[][] getConsoleOrMyAccountTestData() {

        return new Object[][]{
                {"Console"},
                {"My Account"}
        };
    }

    @Test(dataProvider = "ConsoleOrMyAccountTestData")
    public void testShareApplicationWithAllOrganizations_AllExistingOrgsOnly_ConsoleOrMyAccount(String appName)
            throws Exception {


        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {

            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);

            String mainOrgId = "main-org-id";
            String mainAppId = "main-app-id";

            ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.ALL)
                    .build();
            GeneralApplicationShareOperation generalOperation = new GeneralApplicationShareOperation(
                    PolicyEnum.ALL_EXISTING_ORGS_ONLY,
                    allRolesPolicy
            );
            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
            ServiceProvider mainApplication = createMockServiceProvider(appName, false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);
            when(organizationManager.getChildOrganizations(mainOrgId, true)).thenReturn(Collections.emptyList());

            when(mockOrgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);

            // Execute - should return early without error.
            orgApplicationManager.shareApplicationWithAllOrganizations(mainOrgId, mainAppId, generalOperation);

            verify(organizationManager).resolveTenantDomain(mainOrgId);
            verify(organizationManager).getChildOrganizations(mainOrgId, true);
            verify(applicationManagementService).getApplicationByResourceId(mainAppId, "main-tenant-domain");
            verify(applicationManagementService, times(2)).updateApplication(
                    any(ServiceProvider.class), eq("main-tenant-domain"), eq("test-user"));
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testShareApplicationWithAllOrganizations_AlreadySharedApplication() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";

        ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();
        GeneralApplicationShareOperation generalOperation = new GeneralApplicationShareOperation(
                PolicyEnum.ALL_EXISTING_ORGS_ONLY,
                allRolesPolicy
        );

        when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");

        // Create already shared application (fragment app).
        ServiceProvider sharedApplication = createMockServiceProvider("shared-app", true);
        when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                .thenReturn(sharedApplication);

        // Execute - should throw exception.
        orgApplicationManager.shareApplicationWithAllOrganizations(mainOrgId, mainAppId, generalOperation);
    }

    @Test
    public void testShareApplicationWithAllOrganizations_RolePolicyVariations() throws Exception {

        String mainOrgId = "main-org-id";
        String mainAppId = "main-app-id";

        // Test NONE role policy.
        ApplicationShareRolePolicy noneRolesPolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.NONE)
                .build();
        GeneralApplicationShareOperation generalOperation = new GeneralApplicationShareOperation(
                PolicyEnum.ALL_EXISTING_ORGS_ONLY,
                noneRolesPolicy
        );

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic = 
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = 
                     mockStatic(IdentityTenantUtil.class)) {

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(mockOrgApplicationMgtDataHolder);

            utilsMockedStatic.when(Utils::getAuthenticatedUsername).thenReturn("test-user");
            utilsMockedStatic.when(Utils::getOrganizationId).thenReturn("test-org-id");
            
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("main-tenant-domain"))
                    .thenReturn(-1234);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId("test-tenant"))
                    .thenReturn(-1234);
            
            when(mockOrgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(mockOrgApplicationMgtDataHolder.getApplicationManagementService())
                    .thenReturn(applicationManagementService);
            when(mockOrgApplicationMgtDataHolder.getIdpManager()).thenReturn(idpManager);
            when(mockOrgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            lenient().when(mockOrgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            lenient().when(mockOrgApplicationMgtDataHolder.getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            lenient().when(mockOrgApplicationMgtDataHolder.getApplicationSharingManagerListener()).thenReturn(listener);
            lenient().when(mockOrgApplicationMgtDataHolder.getIdentityEventService()).thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getRoleManagementServiceV2()).thenReturn(null);
            lenient().when(mockOrgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);
            lenient().when(mockOrgApplicationMgtDataHolder.getAsyncOperationStatusMgtService())
                    .thenReturn(asyncOperationStatusMgtService);

            when(orgApplicationMgtDAO.getSharedApplications(anyString(), anyString()))
                    .thenReturn(Collections.emptyList());

            OrgApplicationManager testOrgApplicationManager = new OrgApplicationManagerImpl();

            when(organizationManager.resolveTenantDomain(mainOrgId)).thenReturn("main-tenant-domain");
            ServiceProvider mainApplication = createMockServiceProvider("main-app", false);
            when(applicationManagementService.getApplicationByResourceId(mainAppId, "main-tenant-domain"))
                    .thenReturn(mainApplication);

            when(applicationManagementService.getAllIdentityProviders("main-tenant-domain"))
                    .thenReturn(new IdentityProvider[0]);

            // Mock child organizations list (required for the implementation flow).
            List<BasicOrganization> childOrganizations = Collections.singletonList(
                    createMockBasicOrganization("child1-org-id", "child1-name")
                                                                                  );
            when(organizationManager.getChildOrganizations(mainOrgId, true)).thenReturn(childOrganizations);
            List<OrganizationNode> childGraph = createMockOrganizationGraph("child1-org-id");
            when(organizationManager.getChildOrganizationGraph(mainOrgId, true)).thenReturn(childGraph);
            Organization childOrg = createMockOrganization("child1-org-id");
            when(organizationManager.getOrganization("child1-org-id", false, false)).thenReturn(childOrg);

            testOrgApplicationManager.shareApplicationWithAllOrganizations(mainOrgId, mainAppId, generalOperation);

            // Verify sharing occurred with NONE role policy.
            verify(organizationManager).getOrganization("child1-org-id", false, false);
        }
    }

    // ========================================
    // Tests for skipOrganizationHierarchyValidation in shareApplicationWithPolicy.
    // ========================================

        /**
         * Ensures sharing succeeds when skipOrganizationHierarchyValidation is enabled with
         * policy SELECTED_ORG_ONLY and role mode ALL; verifies SP creation and DB link.
         */
        @Test
        public void testSkipAllRolesSuccess() throws Exception {

        String ownerOrgId = "owner-org-id";
        String sharingOrgId = "sharing-org-id";
        String sharedTenantDomain = "shared-tenant";
        String ownerTenantDomain = "owner-tenant";
        int tenantId = 42;
        String adminUserId = "admin-user-id";
        String domainQualifiedUserName = "admin@sharing.org";

        // Enable skip hierarchy validation.
        IdentityUtil.threadLocalProperties.get().put(SKIP_ORGANIZATION_HIERARCHY_VALIDATION, true);

        ServiceProvider mainApplication = createMockServiceProvider("regular-app", false);
        when(mainApplication.getOwner()).thenReturn(mockAppOwner);

        ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();

        OAuthConsumerAppDTO createdOAuthApp = mock(OAuthConsumerAppDTO.class);
        String createdSharedAppId = "shared-app-id";

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
             MockedStatic<ServiceURLBuilder> serviceURLBuilder = mockStatic(ServiceURLBuilder.class);
             MockedStatic<MultitenantUtils> multitenantUtilsMockedStatic = mockStatic(MultitenantUtils.class)) {

            // Wire DataHolder and dependencies.
            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(orgApplicationMgtDataHolder);
            when(orgApplicationMgtDataHolder.getApplicationSharingManagerListener()).thenReturn(listener);
            when(orgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(orgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            when(orgApplicationMgtDataHolder.getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            when(orgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(anyString(), anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(orgApplicationMgtDataHolder.getOAuthAdminService()).thenReturn(oAuthAdminService);
            when(orgApplicationMgtDataHolder.getApplicationManagementService()).thenReturn(
                    applicationManagementService);
            when(orgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            when(orgApplicationMgtDataHolder.getResourceSharingPolicyHandlerService())
                    .thenReturn(resourceSharingPolicyHandlerService);

            when(organizationManager.resolveTenantDomain(sharingOrgId)).thenReturn(sharedTenantDomain);
            when(organizationManager.resolveTenantDomain(ownerOrgId)).thenReturn(ownerTenantDomain);

            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId(sharedTenantDomain))
                    .thenReturn(tenantId);
            when(realmService.getTenantUserRealm(tenantId)).thenReturn(userRealm);
            when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
            when(realmConfiguration.getAdminUserId()).thenReturn(adminUserId);
            when(organizationUserResidentResolverService
                    .resolveUserFromResidentOrganization(null, adminUserId, sharingOrgId))
                    .thenReturn(Optional.of(mockUser));
            when(mockUser.getDomainQualifiedUsername()).thenReturn(domainQualifiedUserName);
            // Fallback path safety: if resident resolver returns empty, avoid NPE in MultitenantUtils.
            when(mainApplication.getOwner()).thenReturn(mockAppOwner);
            when(mockAppOwner.toFullQualifiedUsername()).thenReturn("owner@owner-tenant");
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername("owner@owner-tenant"))
                    .thenReturn("owner");
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername(null))
                    .thenReturn("owner");

            // No existing shared app for target org.
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(
                    anyString(), anyString(), anyString())).thenReturn(Optional.empty());

            // URLs and OAuth app creation flow.
            mockServiceURLBuilder("https://localhost:9443", serviceURLBuilder);
            when(oAuthAdminService.registerAndRetrieveOAuthApplicationData(any())).thenReturn(createdOAuthApp);

            // SP creation and DB update.
            when(applicationManagementService.createApplication(any(ServiceProvider.class), eq(sharedTenantDomain),
                    anyString())).thenReturn(createdSharedAppId);

            // Execute: should bypass parent sharing check and proceed.
            orgApplicationManager.shareApplicationWithPolicy(ownerOrgId, mainApplication, sharingOrgId,
                    PolicyEnum.SELECTED_ORG_ONLY, allRolesPolicy, null);

            verify(applicationManagementService, times(1)).createApplication(any(ServiceProvider.class),
                    eq(sharedTenantDomain), anyString());
            verify(orgApplicationMgtDAO, times(1)).addSharedApplication(anyString(), eq(ownerOrgId),
                    eq(createdSharedAppId), eq(sharingOrgId));
        }
    }

        /**
         * Ensures a client exception is thrown when skipOrganizationHierarchyValidation is enabled but
         * the sharing policy is not SELECTED_ORG_ONLY.
         */
        @Test(expectedExceptions = OrganizationManagementClientException.class)
        public void testSkipWrongPolicyFail() throws Exception {

        String ownerOrgId = "owner-org-id";
        String sharingOrgId = "sharing-org-id";
        String sharedTenantDomain = "shared-tenant";
        int tenantId = 7;
        String adminUserId = "admin";

        // Enable skip hierarchy validation.
        IdentityUtil.threadLocalProperties.get().put(SKIP_ORGANIZATION_HIERARCHY_VALIDATION, true);

        ServiceProvider mainApplication = createMockServiceProvider("regular-app", false);
        when(mainApplication.getOwner()).thenReturn(mockAppOwner);

        ApplicationShareRolePolicy allRolesPolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
             MockedStatic<MultitenantUtils> multitenantUtilsMockedStatic = mockStatic(MultitenantUtils.class)) {

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(orgApplicationMgtDataHolder);
            when(orgApplicationMgtDataHolder.getApplicationSharingManagerListener()).thenReturn(listener);
            when(orgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(orgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            when(orgApplicationMgtDataHolder.getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            when(orgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(anyString(), anyString(), anyString()))
                    .thenReturn(Optional.empty());

            when(organizationManager.resolveTenantDomain(sharingOrgId)).thenReturn(sharedTenantDomain);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId(sharedTenantDomain))
                    .thenReturn(tenantId);
            when(realmService.getTenantUserRealm(tenantId)).thenReturn(userRealm);
            when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
            when(realmConfiguration.getAdminUserId()).thenReturn(adminUserId);
            when(organizationUserResidentResolverService
                    .resolveUserFromResidentOrganization(null, adminUserId, sharingOrgId))
                    .thenReturn(Optional.of(mockUser));
            when(mockUser.getDomainQualifiedUsername()).thenReturn("admin@sharing.org");
            // Fallback path safety to avoid NPE.
            when(mainApplication.getOwner()).thenReturn(mockAppOwner);
            when(mockAppOwner.toFullQualifiedUsername()).thenReturn("owner@owner-tenant");
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername("owner@owner-tenant"))
                    .thenReturn("owner");
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername(null))
                    .thenReturn("owner");

            // Expect: client exception due to wrong policy when skip flag is set.
            orgApplicationManager.shareApplicationWithPolicy(ownerOrgId, mainApplication, sharingOrgId,
                    PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN, allRolesPolicy, null);
        }
    }

        /**
         * Ensures a client exception is thrown when skipOrganizationHierarchyValidation is enabled with
         * SELECTED_ORG_ONLY policy but role mode is not ALL.
         */
        @Test(expectedExceptions = OrganizationManagementClientException.class)
        public void testSkipNonAllRoleFail() throws Exception {

        String ownerOrgId = "owner-org-id";
        String sharingOrgId = "sharing-org-id";
        String sharedTenantDomain = "shared-tenant";
        int tenantId = 3;
        String adminUserId = "admin";

        // Enable skip hierarchy validation.
        IdentityUtil.threadLocalProperties.get().put(SKIP_ORGANIZATION_HIERARCHY_VALIDATION, true);

        ServiceProvider mainApplication = createMockServiceProvider("regular-app", false);
        when(mainApplication.getOwner()).thenReturn(mockAppOwner);

        ApplicationShareRolePolicy noneRolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.NONE)
                .build();

        try (MockedStatic<OrgApplicationMgtDataHolder> orgApplicationMgtDataHolderMockedStatic =
                     mockStatic(OrgApplicationMgtDataHolder.class);
             MockedStatic<IdentityTenantUtil> identityTenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class);
             MockedStatic<MultitenantUtils> multitenantUtilsMockedStatic = mockStatic(MultitenantUtils.class)) {

            orgApplicationMgtDataHolderMockedStatic.when(OrgApplicationMgtDataHolder::getInstance)
                    .thenReturn(orgApplicationMgtDataHolder);
            when(orgApplicationMgtDataHolder.getApplicationSharingManagerListener()).thenReturn(listener);
            when(orgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
            when(orgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);
            when(orgApplicationMgtDataHolder.getOrganizationUserResidentResolverService())
                    .thenReturn(organizationUserResidentResolverService);
            when(orgApplicationMgtDataHolder.getOrgApplicationMgtDAO()).thenReturn(orgApplicationMgtDAO);
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(anyString(), anyString(), anyString()))
                    .thenReturn(Optional.empty());

            when(organizationManager.resolveTenantDomain(sharingOrgId)).thenReturn(sharedTenantDomain);
            identityTenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantId(sharedTenantDomain))
                    .thenReturn(tenantId);
            when(realmService.getTenantUserRealm(tenantId)).thenReturn(userRealm);
            when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
            when(realmConfiguration.getAdminUserId()).thenReturn(adminUserId);
            when(organizationUserResidentResolverService
                    .resolveUserFromResidentOrganization(null, adminUserId, sharingOrgId))
                    .thenReturn(Optional.of(mockUser));
            when(mockUser.getDomainQualifiedUsername()).thenReturn("admin@sharing.org");
            // Fallback path safety to avoid NPE.
            when(mainApplication.getOwner()).thenReturn(mockAppOwner);
            when(mockAppOwner.toFullQualifiedUsername()).thenReturn("owner@owner-tenant");
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername("owner@owner-tenant"))
                    .thenReturn("owner");
            multitenantUtilsMockedStatic.when(() -> MultitenantUtils.getTenantAwareUsername(null))
                    .thenReturn("owner");

            // Expect: client exception due to non-ALL role policy when skip flag is set.
            orgApplicationManager.shareApplicationWithPolicy(ownerOrgId, mainApplication, sharingOrgId,
                    PolicyEnum.SELECTED_ORG_ONLY, noneRolePolicy, null);
        }
    }

    // ========================================
    // Helper Methods for Test Setup
    // ========================================

    private ServiceProvider createMockServiceProvider(String appName, boolean isFragmentApp) {

        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getApplicationName()).thenReturn(appName);
        when(serviceProvider.getApplicationResourceId()).thenReturn(appName + "-resource-id");

        // Set up properties to indicate if it's a fragment app.
        ServiceProviderProperty isFragmentProperty = new ServiceProviderProperty();
        isFragmentProperty.setName(IS_FRAGMENT_APP);
        isFragmentProperty.setValue(String.valueOf(isFragmentApp));

        when(serviceProvider.getSpProperties()).thenReturn(new ServiceProviderProperty[]{isFragmentProperty});

        LocalAndOutboundAuthenticationConfig authConfig = mock(LocalAndOutboundAuthenticationConfig.class);
        when(authConfig.getAuthenticationType()).thenReturn(AUTH_TYPE_DEFAULT);
        when(authConfig.getAuthenticationSteps()).thenReturn(new AuthenticationStep[0]);
        when(serviceProvider.getLocalAndOutBoundAuthenticationConfig()).thenReturn(authConfig);

        ClaimConfig claimConfig = mock(ClaimConfig.class);
        when(serviceProvider.getClaimConfig()).thenReturn(claimConfig);
        return serviceProvider;
    }

    private List<OrganizationNode> createMockOrganizationGraph(String childOrgId) {

        OrganizationNode childNode = mock(OrganizationNode.class);
        when(childNode.getId()).thenReturn(childOrgId);
        when(childNode.getName()).thenReturn("Child Organization");
        when(childNode.getChildren()).thenReturn(Collections.emptyList());

        return Collections.singletonList(childNode);
    }

    private List<OrganizationNode> createMockComplexOrganizationGraph(String child1Id, String grandChild1Id,
                                                                      String child2Id) {

        OrganizationNode grandChildNode = mock(OrganizationNode.class);
        when(grandChildNode.getId()).thenReturn(grandChild1Id);
        when(grandChildNode.getName()).thenReturn("GrandChild Organization");
        when(grandChildNode.getChildren()).thenReturn(Collections.emptyList());

        OrganizationNode child1Node = mock(OrganizationNode.class);
        when(child1Node.getId()).thenReturn(child1Id);
        when(child1Node.getName()).thenReturn("Child1 Organization");
        when(child1Node.getChildren()).thenReturn(Arrays.asList(grandChildNode));

        OrganizationNode child2Node = mock(OrganizationNode.class);
        when(child2Node.getId()).thenReturn(child2Id);
        when(child2Node.getName()).thenReturn("Child2 Organization");
        when(child2Node.getChildren()).thenReturn(Collections.emptyList());

        return Arrays.asList(child1Node, child2Node);
    }

    private Organization createMockOrganization(String orgId) {

        Organization organization = new Organization();
        organization.setId(orgId);
        organization.setName("Organization " + orgId);
        organization.setType("TENANT");
        return organization;
    }

    private BasicOrganization createMockBasicOrganization(String orgId, String name) {

        BasicOrganization basicOrg = new BasicOrganization();
        basicOrg.setId(orgId);
        basicOrg.setName(name);
        return basicOrg;
    }

    private RoleWithAudienceDO createMockRoleWithAudience(String roleName, String audienceType, String audienceName) {

        RoleWithAudienceDO role = mock(RoleWithAudienceDO.class);
        when(role.getRoleName()).thenReturn(roleName);
        when(role.getAudienceType()).thenReturn(RoleWithAudienceDO.AudienceType.fromValue(audienceType));
        when(role.getAudienceName()).thenReturn(audienceName);
        return role;
    }
}
