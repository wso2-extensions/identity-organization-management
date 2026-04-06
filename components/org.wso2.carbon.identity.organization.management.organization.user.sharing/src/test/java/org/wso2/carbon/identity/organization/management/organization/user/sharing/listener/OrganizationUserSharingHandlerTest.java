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

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrganizationUserSharingHandler.
 */
public class OrganizationUserSharingHandlerTest {

    private OrganizationUserSharingHandler organizationUserSharingHandler;
    private MockedStatic<OrganizationUserSharingDataHolder> dataHolderMockedStatic;
    private MockedStatic<OrganizationManagementUtil> orgManagementUtilMockedStatic;
    private OrganizationUserSharingDataHolder dataHolder;
    private OrganizationUserSharingService organizationUserSharingService;

    private static final String TEST_ORG_ID = "c524c30a-cbd4-4169-ac9d-1ee3edf1bf16";

    @BeforeMethod
    public void setUp() {

        organizationUserSharingHandler = new OrganizationUserSharingHandler();
        dataHolderMockedStatic = mockStatic(OrganizationUserSharingDataHolder.class);
        orgManagementUtilMockedStatic = mockStatic(OrganizationManagementUtil.class);

        dataHolder = mock(OrganizationUserSharingDataHolder.class);
        organizationUserSharingService = mock(OrganizationUserSharingService.class);

        dataHolderMockedStatic.when(OrganizationUserSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getOrganizationUserSharingService()).thenReturn(organizationUserSharingService);
    }

    @AfterMethod
    public void tearDown() {

        dataHolderMockedStatic.close();
        orgManagementUtilMockedStatic.close();
    }

    @DataProvider(name = "deleteOrganizationDataProvider")
    public Object[][] deleteOrganizationDataProvider() {

        return new Object[][]{
                {TEST_ORG_ID, true},
                {null, false},
                {"", false}
        };
    }

    /**
     * Test handleEvent method for organization deletion event with various organization ID values.
     */
    @Test(dataProvider = "deleteOrganizationDataProvider")
    public void testHandleEventForDeleteOrganization(String orgId, boolean shouldServiceBeCalled)
            throws IdentityEventException, OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, orgId);
        Event event = new Event(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);
        when(organizationUserSharingService.deleteUserAssociationsByOrganizationId(anyString())).thenReturn(true);
        organizationUserSharingHandler.handleEvent(event);

        if (shouldServiceBeCalled) {
            verify(organizationUserSharingService, times(1)).deleteUserAssociationsByOrganizationId(orgId);
        } else {
            verify(organizationUserSharingService, never()).deleteUserAssociationsByOrganizationId(anyString());
        }
    }

    /**
     * Test handleEvent method when service throws OrganizationManagementException.
     */
    @Test(expectedExceptions = IdentityEventException.class,
            expectedExceptionsMessageRegExp = ".*Error while cleaning up user associations.*")
    public void testHandleEventForDeleteOrganizationWithException() throws IdentityEventException,
            OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, TEST_ORG_ID);
        Event event = new Event(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);

        // Mock service to throw exception.
        doThrow(new OrganizationManagementException("Test exception"))
                .when(organizationUserSharingService).deleteUserAssociationsByOrganizationId(anyString());
        organizationUserSharingHandler.handleEvent(event);
    }
}
