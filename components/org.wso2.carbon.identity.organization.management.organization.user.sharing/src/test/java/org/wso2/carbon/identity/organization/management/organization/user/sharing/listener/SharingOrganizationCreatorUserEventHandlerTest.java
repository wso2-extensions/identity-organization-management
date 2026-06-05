/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.listener;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ORGANIZATION_ID;

/**
 * Tests for {@link SharingOrganizationCreatorUserEventHandler} (issue #6955): {@code associatedOrgId} must
 * resolve from the calling tenant when the creator is set in the org attributes, not from the principal's
 * resident organization.
 */
public class SharingOrganizationCreatorUserEventHandlerTest {

    private static final String POST_SHARED_CONSOLE_APP = "POST_SHARED_CONSOLE_APP";

    private static final String CHILD_ORG_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CHILD_TENANT_DOMAIN = "11111111-1111-1111-1111-111111111111";
    private static final String PARENT_ORG_ID = "22222222-2222-2222-2222-222222222222";
    private static final String PARENT_TENANT_DOMAIN = "22222222-2222-2222-2222-222222222222";
    private static final String RESIDENT_ORG_ID = "super-tenant-resident-org-id";
    private static final String RESIDENT_TENANT_DOMAIN = "carbon.super";
    private static final String CREATOR_USER_ID = "33333333-3333-3333-3333-333333333333";
    private static final String CREATOR_USERNAME = "creatoruser6955";
    private static final String REALM_ADMIN_USERNAME = "admin";
    private static final String REALM_ADMIN_USER_ID = "realm-admin-id";

    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private OrganizationUserSharingService userSharingService;
    @Mock
    private RealmService realmService;
    @Mock
    private UserRealm userRealm;
    @Mock
    private RealmConfiguration realmConfiguration;
    @Mock
    private OrganizationUserSharingDataHolder dataHolder;

    private PrivilegedCarbonContext privilegedCarbonContext;
    private MockedStatic<OrganizationUserSharingDataHolder> dataHolderStatic;
    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilStatic;
    private MockedStatic<Utils> utilsStatic;
    private MockedStatic<PrivilegedCarbonContext> privilegedCarbonContextStatic;
    private MockedStatic<IdentityTenantUtil> identityTenantUtilStatic;

    private SharingOrganizationCreatorUserEventHandler handler;

    @BeforeMethod
    public void setUp() throws Exception {

        openMocks(this);
        setUpCarbonHome();

        dataHolderStatic = mockStatic(OrganizationUserSharingDataHolder.class);
        dataHolderStatic.when(OrganizationUserSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);
        when(dataHolder.getRealmService()).thenReturn(realmService);

        organizationManagementUtilStatic = mockStatic(OrganizationManagementUtil.class);
        // Treat the new org as a sub-organization by default; tests may override.
        organizationManagementUtilStatic.when(() -> OrganizationManagementUtil.isOrganization(anyString()))
                .thenReturn(true);

        utilsStatic = mockStatic(Utils.class);
        // Calling (parent) org context.
        utilsStatic.when(Utils::getTenantDomain).thenReturn(PARENT_TENANT_DOMAIN);
        utilsStatic.when(Utils::getOrganizationId).thenReturn(PARENT_ORG_ID);

        privilegedCarbonContextStatic = mockStatic(PrivilegedCarbonContext.class);
        // Mock the instance only after the static mock is in place (bytecode instrumentation).
        privilegedCarbonContext = mock(PrivilegedCarbonContext.class);
        privilegedCarbonContextStatic.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                .thenReturn(privilegedCarbonContext);
        when(privilegedCarbonContext.getUserResidentOrganizationId()).thenReturn(RESIDENT_ORG_ID);

        identityTenantUtilStatic = mockStatic(IdentityTenantUtil.class);
        identityTenantUtilStatic.when(() -> IdentityTenantUtil.getTenantId(anyString())).thenReturn(1);

        when(realmService.getTenantUserRealm(1)).thenReturn(userRealm);
        when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
        when(realmConfiguration.getAdminUserName()).thenReturn(REALM_ADMIN_USERNAME);
        when(realmConfiguration.getAdminUserId()).thenReturn(REALM_ADMIN_USER_ID);

        when(organizationManager.resolveTenantDomain(CHILD_ORG_ID)).thenReturn(CHILD_TENANT_DOMAIN);
        when(organizationManager.resolveTenantDomain(PARENT_ORG_ID)).thenReturn(PARENT_TENANT_DOMAIN);
        when(organizationManager.resolveTenantDomain(RESIDENT_ORG_ID)).thenReturn(RESIDENT_TENANT_DOMAIN);
        when(organizationManager.resolveOrganizationId(PARENT_TENANT_DOMAIN)).thenReturn(PARENT_ORG_ID);

        handler = new SharingOrganizationCreatorUserEventHandler();
        // Replace the private final userSharingService with the mock to capture its calls.
        injectUserSharingService(handler, userSharingService);
    }

    @AfterMethod
    public void tearDown() {

        if (dataHolderStatic != null) {
            dataHolderStatic.close();
        }
        if (organizationManagementUtilStatic != null) {
            organizationManagementUtilStatic.close();
        }
        if (utilsStatic != null) {
            utilsStatic.close();
        }
        if (privilegedCarbonContextStatic != null) {
            privilegedCarbonContextStatic.close();
        }
        if (identityTenantUtilStatic != null) {
            identityTenantUtilStatic.close();
        }
    }

    /** Regression for #6955: associatedOrgId must come from the caller tenant, not the resident org. */
    @Test
    public void testAssociatedOrgResolvedFromCallerTenantWhenCreatorSetInAttributes() throws Exception {

        Organization childOrg = newOrganizationWithCreatorAttributes(CHILD_ORG_ID, CREATOR_USER_ID, CREATOR_USERNAME);
        when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(childOrg);

        // Short-circuit after shareOrganizationUser is captured; the handler wraps this in IdentityEventException.
        when(userSharingService.getUserAssociationOfAssociatedUserByOrgId(anyString(), anyString()))
                .thenThrow(new OrganizationManagementException("short-circuit after capturing args"));

        try {
            handler.handleEvent(buildEvent(POST_SHARED_CONSOLE_APP, CHILD_ORG_ID));
        } catch (IdentityEventException ignored) {
            // Expected.
        }

        ArgumentCaptor<String> orgIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> associatedUserIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> associatedOrgIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(userSharingService).shareOrganizationUser(
                orgIdCaptor.capture(),
                associatedUserIdCaptor.capture(),
                associatedOrgIdCaptor.capture(),
                eq(SharedType.OWNER));

        Assert.assertEquals(orgIdCaptor.getValue(), CHILD_ORG_ID,
                "Sub-org id passed to shareOrganizationUser must be the new (child) org id.");
        Assert.assertEquals(associatedOrgIdCaptor.getValue(), PARENT_ORG_ID,
                "associatedOrgId must be resolved from Utils.getTenantDomain(), not from "
                        + "PrivilegedCarbonContext.getUserResidentOrganizationId().");
        Assert.assertNotEquals(associatedOrgIdCaptor.getValue(), RESIDENT_ORG_ID,
                "associatedOrgId must NOT come from getUserResidentOrganizationId() in the /o/ flow.");
        Assert.assertEquals(associatedUserIdCaptor.getValue(), REALM_ADMIN_USER_ID,
                "associatedUserId continues to be sourced from the sub-org realm admin.");
    }

    /** Default flow (no creator attributes) still resolves associatedOrgId from the resident org. */
    @Test
    public void testAssociatedOrgResolvedFromResidentOrgWhenCreatorNotSetInAttributes() throws Exception {

        Organization rootOrg = new Organization();
        rootOrg.setId(CHILD_ORG_ID);
        when(organizationManager.getOrganization(CHILD_ORG_ID, false, false)).thenReturn(rootOrg);

        when(userSharingService.getUserAssociationOfAssociatedUserByOrgId(anyString(), anyString()))
                .thenThrow(new OrganizationManagementException("short-circuit after capturing args"));

        try {
            handler.handleEvent(buildEvent(POST_SHARED_CONSOLE_APP, CHILD_ORG_ID));
        } catch (IdentityEventException ignored) {
            // Expected.
        }

        ArgumentCaptor<String> associatedOrgIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(userSharingService).shareOrganizationUser(
                anyString(),
                anyString(),
                associatedOrgIdCaptor.capture(),
                eq(SharedType.OWNER));

        Assert.assertEquals(associatedOrgIdCaptor.getValue(), RESIDENT_ORG_ID,
                "Default flow must keep resolving associatedOrgId from the principal's resident organization.");
    }

    @Test
    public void testHandlerIgnoresNonConsoleAppEvents() throws Exception {

        handler.handleEvent(buildEvent("PRE_ADD_USER", CHILD_ORG_ID));

        verifyNoInteractions(organizationManager);
        verifyNoInteractions(userSharingService);
        dataHolderStatic.verify(OrganizationUserSharingDataHolder::getInstance, never());
    }

    private Organization newOrganizationWithCreatorAttributes(String orgId, String creatorId, String creatorUsername) {

        Organization organization = new Organization();
        organization.setId(orgId);
        organization.setAttributes(Arrays.asList(
                new OrganizationAttribute(OrganizationManagementConstants.CREATOR_ID, creatorId),
                new OrganizationAttribute(OrganizationManagementConstants.CREATOR_USERNAME, creatorUsername)));
        return organization;
    }

    private Event buildEvent(String eventName, String orgId) {

        Map<String, Object> properties = new HashMap<>();
        properties.put(EVENT_PROP_ORGANIZATION_ID, orgId);
        return new Event(eventName, properties);
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
    }

    private static void injectUserSharingService(SharingOrganizationCreatorUserEventHandler handler,
                                                 OrganizationUserSharingService mockService) throws Exception {

        Field field = SharingOrganizationCreatorUserEventHandler.class.getDeclaredField("userSharingService");
        field.setAccessible(true);
        field.set(handler, mockService);
    }
}
