/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler;

import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.wso2.carbon.identity.event.IdentityEventConstants.Event.POST_DELETE_ROLE_V2_EVENT;
import static org.wso2.carbon.identity.event.IdentityEventConstants.Event.POST_DELETE_USER;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.ROLE_ID;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.USER_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType.ROLE;

/**
 * Unit tests for SharingPolicyCleanUpHandler.
 */
public class SharingPolicyCleanUpHandlerTest {

    @Mock
    ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;

    @BeforeMethod
    public void setUp() {

        openMocks(this);
        OrganizationManagementHandlerDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(resourceSharingPolicyHandlerService);
    }

    @AfterMethod
    public void tearDown() {

        reset(resourceSharingPolicyHandlerService);
    }

    @Test
    public void testPostOrganizationDeleteHandleMethod() throws Exception {

        // Trigger the event.
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, "org-uuid");
        Event event = new Event(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);

        SharingPolicyCleanUpHandler sharingPolicyCleanUpHandler = new SharingPolicyCleanUpHandler();
        sharingPolicyCleanUpHandler.handleEvent(event);
        verify(resourceSharingPolicyHandlerService,
                times(1)).deleteResourceSharingPoliciesAndAttributesByOrganizationId(anyString());
    }

    @Test
    public void testPosUserDeleteHandleMethod() throws Exception {

        // Trigger the event.
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(USER_ID, "user-id");
        Event event = new Event(POST_DELETE_USER, eventProperties);

        SharingPolicyCleanUpHandler sharingPolicyCleanUpHandler = new SharingPolicyCleanUpHandler();
        sharingPolicyCleanUpHandler.handleEvent(event);
        verify(resourceSharingPolicyHandlerService,
                times(1)).deleteResourceSharingPolicyByResourceTypeAndId(
                ResourceType.USER, "user-id");
    }

    @Test
    public void testPostRoleDeletionHandlerMethod() throws Exception {

        // Trigger the event.
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(ROLE_ID, "role-id");
        Event event = new Event(POST_DELETE_ROLE_V2_EVENT, eventProperties);

        SharingPolicyCleanUpHandler sharingPolicyCleanUpHandler = new SharingPolicyCleanUpHandler();
        sharingPolicyCleanUpHandler.handleEvent(event);
        verify(resourceSharingPolicyHandlerService, times(1)).
                deleteSharedResourceAttributeByAttributeTypeAndId(ROLE, "role-id");
    }
}
