/*
 * Copyright (c) 2023-2025, WSO2 LLC. (https://www.wso2.com).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.application.listener;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.SUPER_TENANT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ROLE_SHARING_MODE;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;

/**
 * Test class for OrganizationCreationHandler.
 */
@WithCarbonHome
public class OrganizationCreationHandlerTest {

    private static final String organizationID = "org-id";
    private static final String parentOrganizationID = "parent-org-id";
    private static final String parentOrganizationHandle = "parent-org-handle";
    private static final String applicationResourceID = "app-resource-id";
    private static final String sharedApplicationResourceID = "shared-app-resource-id";
    private static final String adminUserId = "admin-user-id";
    private static final String USERNAME = "test-user";

    @InjectMocks
    private OrganizationCreationHandler organizationCreationHandler;

    @Mock
    private Organization organization;

    @Mock
    private ParentOrganizationDO parentOrganizationDO;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;

    @Mock
    private ApplicationManagementService applicationManagementService;

    @Mock
    private OrgApplicationMgtDAO orgApplicationMgtDAO;

    @Mock
    private ApplicationSharingManagerListener applicationSharingManagerListener;

    @Mock
    private RealmService realmService;

    @Mock
    private UserRealm userRealm;

    @Mock
    private RealmConfiguration realmConfiguration;

    @Mock
    private OrganizationUserResidentResolverService userResidentResolverService;

    @BeforeMethod
    public void setUp() throws OrganizationManagementException, UserStoreException {

        MockitoAnnotations.openMocks(this);
        mockPrivilegedCarbonContext();
        mockRealConfig();

        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrgApplicationMgtDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(resourceSharingPolicyHandlerService);
        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
        OrgApplicationMgtDataHolder.getInstance().setOrgApplicationMgtDAO(orgApplicationMgtDAO);
        OrgApplicationMgtDataHolder.getInstance().setRealmService(realmService);
        OrgApplicationMgtDataHolder.getInstance()
                .setApplicationSharingManagerListener(applicationSharingManagerListener);
        OrgApplicationMgtDataHolder.getInstance()
                .setOrganizationUserResidentResolverService(userResidentResolverService);

        when(organizationManager.resolveTenantDomain(SUPER_ORG_ID)).thenReturn(SUPER_TENANT);
    }

    @Test
    public void testPostAddOrganizationListener() throws Exception {

        try (MockedStatic<IdentityTenantUtil> identityTenantUtil = mockStatic(IdentityTenantUtil.class);
             MockedStatic<MultitenantUtils> multitenantUtils = mockStatic(MultitenantUtils.class)) {

            identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(any())).thenReturn(1);
            multitenantUtils.when(() -> MultitenantUtils.getTenantAwareUsername(any())).thenReturn(USERNAME);

            when(organization.getId()).thenReturn(organizationID);
            when(organization.getParent()).thenReturn(parentOrganizationDO);
            when(parentOrganizationDO.getId()).thenReturn(parentOrganizationID);
            when(organizationManager.resolveTenantDomain(parentOrganizationID)).thenReturn(parentOrganizationHandle);
            when(organizationManager.getAncestorOrganizationIds(any()))
                    .thenReturn(Collections.singletonList(SUPER_ORG_ID));
            when(organizationManager.getOrganization(organizationID, false, false))
                    .thenReturn(organization);

            when(resourceSharingPolicyHandlerService.getResourceSharingPoliciesByResourceType(
                    Collections.singletonList(SUPER_ORG_ID), ResourceType.APPLICATION.name()))
                    .thenReturn(getResourceSharingPolicies());
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(applicationResourceID, SUPER_ORG_ID,
                    parentOrganizationID)).thenReturn(Optional.of(sharedApplicationResourceID));
            when(orgApplicationMgtDAO.getSharedApplicationResourceId(applicationResourceID, SUPER_ORG_ID,
                    organizationID)).thenReturn(Optional.of(sharedApplicationResourceID));

            when(applicationManagementService.getApplicationBasicInfoBySPProperty(SUPER_TENANT, USERNAME,
                    SHARE_WITH_ALL_CHILDREN, "true")).thenReturn(getApplicationBasicInfoList());
            when(applicationManagementService.getApplicationByResourceId(any(), any()))
                    .thenReturn(getApplication(ApplicationShareRolePolicy.Mode.ALL.name()));

            Map<String, Object> properties = new HashMap<>();
            properties.put(EVENT_PROP_ORGANIZATION_ID, organizationID);
            properties.put(EVENT_PROP_ORGANIZATION, organization);

            Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, properties);
            organizationCreationHandler.handleEvent(event);

            ArgumentCaptor<String> ownerOrgIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> mainAppIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> sharingOrgIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> sharedAppIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ApplicationShareRolePolicy> policyCaptor
                    = ArgumentCaptor.forClass(ApplicationShareRolePolicy.class);

            verify(applicationSharingManagerListener).postShareApplication(
                    ownerOrgIdCaptor.capture(),
                    mainAppIdCaptor.capture(),
                    sharingOrgIdCaptor.capture(),
                    sharedAppIdCaptor.capture(),
                    policyCaptor.capture());

            assertEquals(ownerOrgIdCaptor.getValue(), SUPER_ORG_ID);
            assertEquals(mainAppIdCaptor.getValue(), applicationResourceID);
            assertEquals(sharingOrgIdCaptor.getValue(), organizationID);
            assertEquals(sharedAppIdCaptor.getValue(), sharedApplicationResourceID);
            assertEquals(ApplicationShareRolePolicy.Mode.ALL, policyCaptor.getValue().getMode());
        }
    }

    @DataProvider(name = "sharedAppIdProvider")
    public Object[][] sharedAppIdProvider() {

        return new Object[][]{
                {sharedApplicationResourceID},
                {null}
        };
    }

    @Test(dataProvider = "sharedAppIdProvider")
    public void testAddSharedAppWhenParentOrgDoesNotContainSharedApp(String sharedApplicationResourceID)
            throws Exception {

        when(organization.getParent()).thenReturn(parentOrganizationDO);
        when(parentOrganizationDO.getId()).thenReturn(parentOrganizationID);
        when(organizationManager.getAncestorOrganizationIds(any()))
                .thenReturn(Collections.singletonList(SUPER_ORG_ID));
        when(resourceSharingPolicyHandlerService.getResourceSharingPoliciesByResourceType(
                Collections.singletonList(SUPER_ORG_ID), ResourceType.APPLICATION.name()))
                .thenReturn(getResourceSharingPolicies());
        when(applicationManagementService.getApplicationBasicInfoBySPProperty(SUPER_TENANT, USERNAME,
                SHARE_WITH_ALL_CHILDREN, "true")).thenReturn(new ApplicationBasicInfo[0]);
        when(orgApplicationMgtDAO.getSharedApplicationResourceId(applicationResourceID, SUPER_ORG_ID,
                parentOrganizationID)).thenReturn(Optional.ofNullable(sharedApplicationResourceID));

        Map<String, Object> properties = new HashMap<>();
        properties.put(EVENT_PROP_ORGANIZATION_ID, organizationID);
        properties.put(EVENT_PROP_ORGANIZATION, organization);

        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, properties);
        organizationCreationHandler.handleEvent(event);

        verify(applicationSharingManagerListener, never()).preShareApplication(
                anyString(), anyString(), anyString(), any(ApplicationShareRolePolicy.class));
    }

    private ServiceProvider getApplication(String mode) {

        ServiceProviderProperty roleSharingModeProp = new ServiceProviderProperty();
        roleSharingModeProp.setName(ROLE_SHARING_MODE);
        roleSharingModeProp.setValue(mode);

        ServiceProvider application = new ServiceProvider();
        application.setApplicationResourceId(applicationResourceID);
        application.setSpProperties(new ServiceProviderProperty[] { roleSharingModeProp });
        application.setOwner(new User());
        application.setApplicationName("Test Application");

        return application;
    }

    private ApplicationBasicInfo[] getApplicationBasicInfoList() {

        ApplicationBasicInfo info = new ApplicationBasicInfo();
        info.setApplicationId(1);
        info.setUuid(applicationResourceID);

        return new ApplicationBasicInfo[]{ info };
    }

    private List<ResourceSharingPolicy> getResourceSharingPolicies() {

        ResourceSharingPolicy policy = new ResourceSharingPolicy();
        policy.setResourceSharingPolicyId(1);
        policy.setResourceType(ResourceType.APPLICATION);
        policy.setResourceId(applicationResourceID);
        policy.setInitiatingOrgId(SUPER_ORG_ID);
        policy.setPolicyHoldingOrgId(SUPER_ORG_ID);
        policy.setSharingPolicy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);

        return Collections.singletonList(policy);
    }

    private void mockRealConfig() throws UserStoreException {

        when(realmService.getTenantUserRealm(1)).thenReturn(userRealm);
        when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
        when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
        when(realmConfiguration.getAdminUserId()).thenReturn(adminUserId);
    }

    private void mockPrivilegedCarbonContext() {

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(USERNAME);
    }
}
