/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.handler;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

/**
 * Test class for UserStoreInitializationHandler.
 */
public class UserStoreInitializationHandlerTest {

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private RealmService realmService;

    @Mock
    private UserRealm userRealm;

    @Mock
    private AbstractUserStoreManager userStoreManager;

    @Mock
    private UserStoreManager defaultUserStore;

    @Mock
    private UserStoreManager agentUserStore;

    private UserStoreInitializationHandler handler;
    private MockedStatic<IdentityUtil> identityUtil;
    private MockedStatic<IdentityTenantUtil> identityTenantUtil;

    @BeforeMethod
    public void setUp() throws UserStoreException {

        MockitoAnnotations.openMocks(this);
        handler = new UserStoreInitializationHandler();
        
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationManagementHandlerDataHolder.getInstance().setRealmService(realmService);

        identityUtil = Mockito.mockStatic(IdentityUtil.class);
        identityTenantUtil = Mockito.mockStatic(IdentityTenantUtil.class);

        // Default configuration - handler enabled.
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.Enable"))
                .thenReturn("true");
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.UserStores"))
                .thenReturn("DEFAULT,AGENT");
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.WaitTime"))
                .thenReturn("5000"); // 5 seconds for tests.
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.WaitInterval"))
                .thenReturn("100"); // 100ms for tests.

        when(realmService.getTenantUserRealm(anyInt())).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
    }

    @AfterMethod
    public void tearDown() {

        identityUtil.close();
        identityTenantUtil.close();
    }

    @DataProvider(name = "organizationDataProvider")
    public Object[][] organizationDataProvider() {

        Organization subOrganization = new Organization();
        subOrganization.setId("sub-org-123");
        subOrganization.setName("Sub Organization");

        Organization rootOrganization = new Organization();
        rootOrganization.setId("root-org-456");
        rootOrganization.setName("Root Organization");

        return new Object[][]{
                {subOrganization, 1}, // Sub-organization.
                {rootOrganization, 0}  // Root organization.
        };
    }

    @Test(dataProvider = "organizationDataProvider")
    public void testHandleEventWithPostAddOrganization(Organization organization, int depth)
            throws IdentityEventException, OrganizationManagementException, UserStoreException {

        // Mock organization manager.
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(depth);
        when(organizationManager.resolveTenantDomain(organization.getId())).thenReturn("example.com");
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId("example.com")).thenReturn(1);

        // Mock user stores - return immediately.
        when(userStoreManager.getSecondaryUserStoreManager("DEFAULT")).thenReturn(defaultUserStore);
        when(userStoreManager.getSecondaryUserStoreManager("AGENT")).thenReturn(agentUserStore);

        // Trigger the event.
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        
        handler.handleEvent(event);

        // Verify behavior.
        verify(organizationManager).getOrganizationDepthInHierarchy(organization.getId());
        
        if (depth > 0) {
            // For sub-organization, should wait for user stores.
            verify(organizationManager).resolveTenantDomain(organization.getId());
            verify(userStoreManager).getSecondaryUserStoreManager("DEFAULT");
            verify(userStoreManager).getSecondaryUserStoreManager("AGENT");
        } else {
            // For root organization, should not wait for user stores.
            verify(organizationManager, never()).resolveTenantDomain(organization.getId());
            verify(userStoreManager, never()).getSecondaryUserStoreManager(anyString());
        }
    }

    @Test
    public void testHandlerDisabled() throws IdentityEventException, OrganizationManagementException {

        // Disable the handler.
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.Enable"))
                .thenReturn("false");

        Organization organization = new Organization();
        organization.setId("sub-org-123");
        
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(1);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        
        handler.handleEvent(event);

        // Should not attempt to wait for user stores.
        verify(organizationManager, never()).resolveTenantDomain(anyString());
        verify(userStoreManager, never()).getSecondaryUserStoreManager(anyString());
    }

    @Test
    public void testUserStoreInitializedAfterDelay()
            throws IdentityEventException, OrganizationManagementException, UserStoreException {

        Organization organization = new Organization();
        organization.setId("sub-org-123");
        
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(1);
        when(organizationManager.resolveTenantDomain(organization.getId())).thenReturn("example.com");
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId("example.com")).thenReturn(1);

        // Simulate user stores not available initially, then available.
        when(userStoreManager.getSecondaryUserStoreManager("DEFAULT"))
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(defaultUserStore);
        when(userStoreManager.getSecondaryUserStoreManager("AGENT"))
                .thenReturn(agentUserStore);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        
        handler.handleEvent(event);

        // Should have retried and eventually succeeded.
        verify(userStoreManager, times(3)).getSecondaryUserStoreManager("DEFAULT");
        verify(userStoreManager).getSecondaryUserStoreManager("AGENT");
    }

    @Test
    public void testUserStoreNotInitializedWithinTimeout()
            throws OrganizationManagementException, UserStoreException {

        Organization organization = new Organization();
        organization.setId("sub-org-123");
        
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(1);
        when(organizationManager.resolveTenantDomain(organization.getId())).thenReturn("example.com");
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId("example.com")).thenReturn(1);

        // Simulate user store never becoming available.
        when(userStoreManager.getSecondaryUserStoreManager("DEFAULT")).thenReturn(null);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);

        // Should throw exception.
        assertThrows(IdentityEventException.class, () -> handler.handleEvent(event));
    }

    @Test
    public void testCustomUserStoreConfiguration()
            throws IdentityEventException, OrganizationManagementException, UserStoreException {

        // Configure only one user store.
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.UserStores"))
                .thenReturn("CUSTOM");

        Organization organization = new Organization();
        organization.setId("sub-org-123");
        
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(1);
        when(organizationManager.resolveTenantDomain(organization.getId())).thenReturn("example.com");
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId("example.com")).thenReturn(1);

        UserStoreManager customUserStore = Mockito.mock(UserStoreManager.class);
        when(userStoreManager.getSecondaryUserStoreManager("CUSTOM")).thenReturn(customUserStore);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        
        handler.handleEvent(event);

        // Should wait for custom user store only.
        verify(userStoreManager).getSecondaryUserStoreManager("CUSTOM");
        verify(userStoreManager, never()).getSecondaryUserStoreManager("DEFAULT");
        verify(userStoreManager, never()).getSecondaryUserStoreManager("AGENT");
    }

    @Test
    public void testEmptyUserStoreConfiguration()
            throws IdentityEventException, OrganizationManagementException, UserStoreException {

        // No user stores configured - should use default.
        identityUtil.when(() -> IdentityUtil.getProperty("OrganizationUserStoreInitialization.UserStores"))
                .thenReturn(null);

        Organization organization = new Organization();
        organization.setId("sub-org-123");
        
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(1);
        when(organizationManager.resolveTenantDomain(organization.getId())).thenReturn("example.com");
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId("example.com")).thenReturn(1);

        when(userStoreManager.getSecondaryUserStoreManager("DEFAULT")).thenReturn(defaultUserStore);
        when(userStoreManager.getSecondaryUserStoreManager("AGENT")).thenReturn(agentUserStore);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        
        handler.handleEvent(event);

        // Should use default user stores.
        verify(userStoreManager).getSecondaryUserStoreManager("DEFAULT");
        verify(userStoreManager).getSecondaryUserStoreManager("AGENT");
    }
}
