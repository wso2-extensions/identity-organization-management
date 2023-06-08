/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler.tests;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.GovernanceConfigUpdateHandler;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GovernanceConfigUpdateHandlerTest {

    @Mock
    private IdentityGovernanceService identityGovernanceService;

    @Mock
    private OrganizationManager organizationManager;

    @InjectMocks
    private GovernanceConfigUpdateHandler configUpdateHandler;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        OrganizationManagementHandlerDataHolder.getInstance().setIdentityGovernanceService(identityGovernanceService);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    @Test
    public void testHandleEventWithPostAddOrganizationEvent()
            throws IdentityEventException, OrganizationManagementException, IdentityGovernanceException {

        // Create a sample organization.
        Organization organization = new Organization();
        organization.setId("orgId");

        // Mock the necessary methods.
        when(organizationManager.resolveTenantDomain(organization.getId())).thenReturn("sampleTenantDomain");
        when(organizationManager.getOrganizationDepthInHierarchy(organization.getId())).thenReturn(2);

        // Trigger the event.
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        configUpdateHandler.handleEvent(event);

        // Verify that the necessary methods are called.
        verify(organizationManager).resolveTenantDomain(organization.getId());
        verify(organizationManager).getOrganizationDepthInHierarchy(organization.getId());
        verify(identityGovernanceService).updateConfiguration(Mockito.anyString(), Mockito.anyMap());
    }
}

