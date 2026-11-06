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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_POST_SHARE_USER;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_POST_UNSHARE_USER;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ASSOCIATED_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ASSOCIATED_USER_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ROLES;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_ROLE_IS_SHARED;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_ROLE_IS_SHARED;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.management.ext.Constants.EVENT_PROP_SHARED_USER_ID;

/**
 * Unit tests for UserSharingEventPublisher.
 */
public class UserSharingEventPublisherTest {

    private static final String RESIDENT_ORG_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final String RESIDENT_TENANT_DOMAIN = "carbon.super";
    private static final String SHARED_ORG_ID = "e4644686-2918-4ea4-a594-ce88cfca3511";
    private static final String SHARED_TENANT_DOMAIN = "sub001";
    private static final String ASSOCIATED_USER_ID = "cab985b0-f5c2-49e8-a85d-038d3d008efc";
    private static final String SHARED_USER_ID = "cc678471-37e0-4a2f-92e8-aeba169ddfa7";
    private static final String MAIN_ROLE_ID = "4404e2a5-2c03-4c10-9629-0402583c4fda";
    private static final String SHARED_ROLE_ID = "7d1c3b52-90a4-4f3e-b6f1-2a8e5c4d9e07";
    private static final String RESIDENT_ROLE_ID = "2f6a9c1e-58b3-4d07-a1e4-93c6b0d8f215";
    private static final String OTHER_ROLE_ID = "b3e8d2a4-7c61-4f95-8e0a-1d4c6f2b9a73";

    private MockedStatic<OrganizationUserSharingDataHolder> dataHolderMockedStatic;
    private MockedStatic<OrganizationManagementUtil> organizationManagementUtilMockedStatic;
    private IdentityEventService identityEventService;
    private RoleManagementService roleManagementService;
    private OrganizationUserSharingService organizationUserSharingService;

    @BeforeMethod
    public void setUp() throws Exception {

        dataHolderMockedStatic = mockStatic(OrganizationUserSharingDataHolder.class);
        organizationManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);

        OrganizationUserSharingDataHolder dataHolder = mock(OrganizationUserSharingDataHolder.class);
        OrganizationManager organizationManager = mock(OrganizationManager.class);
        identityEventService = mock(IdentityEventService.class);
        roleManagementService = mock(RoleManagementService.class);
        organizationUserSharingService = mock(OrganizationUserSharingService.class);

        dataHolderMockedStatic.when(OrganizationUserSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);
        when(dataHolder.getIdentityEventService()).thenReturn(identityEventService);
        when(dataHolder.getRoleManagementService()).thenReturn(roleManagementService);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(organizationUserSharingService);
        when(organizationManager.resolveTenantDomain(RESIDENT_ORG_ID)).thenReturn(RESIDENT_TENANT_DOMAIN);
        when(organizationManager.resolveTenantDomain(SHARED_ORG_ID)).thenReturn(SHARED_TENANT_DOMAIN);
    }

    @AfterMethod
    public void tearDown() {

        dataHolderMockedStatic.close();
        organizationManagementUtilMockedStatic.close();
    }

    @Test
    public void testPublishUnshareEvent() throws Exception {

        UserSharingEventPublisher.publish(EVENT_POST_UNSHARE_USER, buildUserAssociation());

        Event event = captureEvent();
        assertEquals(event.getEventName(), EVENT_POST_UNSHARE_USER);
        assertUserProperties(event.getEventProperties());
        assertFalse(event.getEventProperties().containsKey(EVENT_PROP_ROLES));
    }

    @Test
    public void testPublishUserSharedEventOfRootOrganizationUser() throws Exception {

        mockSharedRoles();
        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(
                RESIDENT_TENANT_DOMAIN)).thenReturn(false);

        UserSharingEventPublisher.publishUserSharedEvent(buildUserAssociation());

        Event event = captureEvent();
        assertEquals(event.getEventName(), EVENT_POST_SHARE_USER);
        assertUserProperties(event.getEventProperties());
        // The role of the root organization is the main role the shared role was shared from.
        assertRole(event.getEventProperties(), MAIN_ROLE_ID, false);
    }

    @Test
    public void testPublishUserSharedEventOfSubOrganizationUser() throws Exception {

        mockSharedRoles();
        organizationManagementUtilMockedStatic.when(() -> OrganizationManagementUtil.isOrganization(
                RESIDENT_TENANT_DOMAIN)).thenReturn(true);
        when(roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                Collections.singletonList(MAIN_ROLE_ID), RESIDENT_TENANT_DOMAIN))
                .thenReturn(Collections.singletonMap(MAIN_ROLE_ID, RESIDENT_ROLE_ID));

        UserSharingEventPublisher.publishUserSharedEvent(buildUserAssociation());

        Event event = captureEvent();
        assertEquals(event.getEventName(), EVENT_POST_SHARE_USER);
        assertUserProperties(event.getEventProperties());
        // The role of a sub organization is its shared copy of the main role.
        assertRole(event.getEventProperties(), RESIDENT_ROLE_ID, true);
    }

    @Test
    public void testPublishUserSharedEventWithoutRoles() throws Exception {

        when(roleManagementService.getRoleIdListOfUser(SHARED_USER_ID, SHARED_TENANT_DOMAIN))
                .thenReturn(Collections.emptyList());
        when(organizationUserSharingService.getSharedUserRolesFromUserRoles(anyList(), anyString()))
                .thenReturn(Collections.emptyList());

        UserSharingEventPublisher.publishUserSharedEvent(buildUserAssociation());

        Event event = captureEvent();
        assertEquals(event.getEventName(), EVENT_POST_SHARE_USER);
        assertUserProperties(event.getEventProperties());
        assertTrue(((List<?>) event.getEventProperties().get(EVENT_PROP_ROLES)).isEmpty());
    }

    private UserAssociation buildUserAssociation() {

        UserAssociation userAssociation = new UserAssociation();
        userAssociation.setUserId(SHARED_USER_ID);
        userAssociation.setOrganizationId(SHARED_ORG_ID);
        userAssociation.setAssociatedUserId(ASSOCIATED_USER_ID);
        userAssociation.setUserResidentOrganizationId(RESIDENT_ORG_ID);
        userAssociation.setSharedType(SharedType.SHARED);
        return userAssociation;
    }

    // The shared user holds one shared role and one role not assigned by the sharing.
    private void mockSharedRoles() throws Exception {

        when(roleManagementService.getRoleIdListOfUser(SHARED_USER_ID, SHARED_TENANT_DOMAIN))
                .thenReturn(Arrays.asList(SHARED_ROLE_ID, OTHER_ROLE_ID));
        when(organizationUserSharingService.getSharedUserRolesFromUserRoles(
                Arrays.asList(SHARED_ROLE_ID, OTHER_ROLE_ID), SHARED_TENANT_DOMAIN))
                .thenReturn(Collections.singletonList(SHARED_ROLE_ID));
        when(roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(
                Collections.singletonList(SHARED_ROLE_ID), SHARED_TENANT_DOMAIN))
                .thenReturn(Collections.singletonMap(SHARED_ROLE_ID, MAIN_ROLE_ID));
    }

    private Event captureEvent() throws Exception {

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(identityEventService).handleEvent(eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private void assertUserProperties(Map<String, Object> eventProperties) {

        assertEquals(eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN), RESIDENT_TENANT_DOMAIN);
        assertEquals(eventProperties.get(EVENT_PROP_SHARED_TENANT_DOMAIN), SHARED_TENANT_DOMAIN);
        assertEquals(eventProperties.get(EVENT_PROP_SHARED_USER_ID), SHARED_USER_ID);
        assertEquals(eventProperties.get(EVENT_PROP_SHARED_ORGANIZATION_ID), SHARED_ORG_ID);
        assertEquals(eventProperties.get(EVENT_PROP_ASSOCIATED_USER_ID), ASSOCIATED_USER_ID);
        assertEquals(eventProperties.get(EVENT_PROP_ASSOCIATED_ORGANIZATION_ID), RESIDENT_ORG_ID);
    }

    @SuppressWarnings("unchecked")
    private void assertRole(Map<String, Object> eventProperties, String roleId, boolean isRoleShared) {

        List<Map<String, String>> roles = (List<Map<String, String>>) eventProperties.get(EVENT_PROP_ROLES);
        assertEquals(roles.size(), 1);
        Map<String, String> role = roles.get(0);
        assertEquals(role.get(EVENT_PROP_SHARED_ROLE_ID), SHARED_ROLE_ID);
        assertEquals(role.get(EVENT_PROP_SHARED_ROLE_IS_SHARED), "true");
        assertEquals(role.get(EVENT_PROP_ROLE_ID), roleId);
        assertEquals(role.get(EVENT_PROP_ROLE_IS_SHARED), String.valueOf(isRoleShared));
    }
}
