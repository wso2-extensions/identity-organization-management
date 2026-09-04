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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.listener;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.ConnectionAssociationManager;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.ConnectionTypeHandler;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util.ConnectionSharingInitiatorContext;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrganizationSharedConnectionHandler}. The asynchronous processing is run synchronously via a
 * direct executor injected through the package-private constructor, so both the {@code POST_DELETE} cleanup (owned
 * connections, shadows shared to the org, and the association rows) and the {@code POST_ADD} share path can be
 * verified deterministically on the test thread.
 */
public class OrganizationSharedConnectionHandlerTest {

    private static final ResourceType RESOURCE_TYPE = ResourceType.CONNECTION_IDENTITY_PROVIDER;
    private static final String DELETING_ORG_ID = "deleting-org-id";
    private static final String CREATED_ORG_ID = "created-org-id";
    private static final String PARENT_ORG_ID = "parent-org-id";
    private static final String OWNER_ORG_ID = "owner-org-id";
    private static final String CONNECTION_ID = "connection-id";

    private MockedStatic<ConnectionSharingDataHolder> mockedDataHolder;
    private MockedStatic<ConnectionSharingInitiatorContext> mockedInitiatorContext;
    private MockedStatic<PrivilegedCarbonContext> mockedCarbonContext;
    private ConnectionSharingDataHolder dataHolder;
    private ConnectionAssociationManager associationManager;
    private OrganizationManager organizationManager;
    private ResourceSharingPolicyHandlerService policyService;
    private ConnectionTypeHandler handler;
    private OrganizationSharedConnectionHandler eventHandler;

    @BeforeClass
    public void setUpClass() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());
    }

    @BeforeMethod
    public void setUp() {

        mockedDataHolder = mockStatic(ConnectionSharingDataHolder.class);
        mockedInitiatorContext = mockStatic(ConnectionSharingInitiatorContext.class);
        mockedCarbonContext = mockStatic(PrivilegedCarbonContext.class);

        associationManager = mock(ConnectionAssociationManager.class);
        organizationManager = mock(OrganizationManager.class);
        policyService = mock(ResourceSharingPolicyHandlerService.class);
        handler = mock(ConnectionTypeHandler.class);
        dataHolder = mock(ConnectionSharingDataHolder.class);

        mockedDataHolder.when(ConnectionSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getConnectionAssociationManager()).thenReturn(associationManager);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);
        when(dataHolder.getResourceSharingPolicyHandlerService()).thenReturn(policyService);
        when(dataHolder.getConnectionTypeHandler(RESOURCE_TYPE)).thenReturn(handler);
        when(handler.getResourceType()).thenReturn(RESOURCE_TYPE);
        when(handler.isSharingEnabled()).thenReturn(true);

        ConnectionSharingInitiatorContext context = mock(ConnectionSharingInitiatorContext.class);
        when(context.getSharingInitiatedTenantDomain()).thenReturn("carbon.super");
        mockedInitiatorContext.when(ConnectionSharingInitiatorContext::capture).thenReturn(context);

        mockedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext)
                .thenReturn(mock(PrivilegedCarbonContext.class));

        Executor directExecutor = Runnable::run;
        eventHandler = new OrganizationSharedConnectionHandler(directExecutor);
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        mockedInitiatorContext.close();
        mockedCarbonContext.close();
    }

    // ----- POST_DELETE -----

    @Test
    public void testPostDeleteOrganizationTriggersAssociationCleanup() throws Exception {

        eventHandler.handleEvent(deleteEvent(DELETING_ORG_ID));

        verify(associationManager).deleteConnectionAssociationsByOrganizationId(DELETING_ORG_ID);
    }

    @Test
    public void testPostDeleteWithBlankOrganizationIdIsNoOp() throws Exception {

        eventHandler.handleEvent(deleteEvent("  "));

        verify(associationManager, never()).deleteConnectionAssociationsByOrganizationId(anyString());
    }

    @Test
    public void testPostDeleteUnsharesOwnedConnectionsOncePerConnection() throws Exception {

        // The same owned connection appears once per shared organization; it must be unshared only once.
        when(associationManager.getConnectionAssociationsByResidentOrg(DELETING_ORG_ID)).thenReturn(Arrays.asList(
                association(CONNECTION_ID, DELETING_ORG_ID, "shadow-a", "shared-org-a"),
                association(CONNECTION_ID, DELETING_ORG_ID, "shadow-b", "shared-org-b")));

        eventHandler.handleEvent(deleteEvent(DELETING_ORG_ID));

        verify(handler, times(1)).unshareConnectionFromAllOrgs(CONNECTION_ID, DELETING_ORG_ID);
        verify(associationManager).deleteConnectionAssociationsByOrganizationId(DELETING_ORG_ID);
    }

    @Test
    public void testPostDeleteRemovesConnectionsSharedToOrganization() throws Exception {

        // The organization holds a shadow of a connection owned by another organization.
        when(associationManager.getConnectionAssociationsBySharedOrg(DELETING_ORG_ID)).thenReturn(
                Collections.singletonList(association(CONNECTION_ID, OWNER_ORG_ID, "shadow-c", DELETING_ORG_ID)));

        eventHandler.handleEvent(deleteEvent(DELETING_ORG_ID));

        verify(handler).unshareConnectionFromOrg(CONNECTION_ID, DELETING_ORG_ID, OWNER_ORG_ID);
        verify(associationManager).deleteConnectionAssociationsByOrganizationId(DELETING_ORG_ID);
    }

    // ----- POST_ADD -----

    @Test
    public void testPostAddWithNullOrganizationIsNoOp() throws Exception {

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.EVENT_PROP_ORGANIZATION, null);

        eventHandler.handleEvent(new Event(Constants.EVENT_POST_ADD_ORGANIZATION, properties));

        verify(organizationManager, never()).getAncestorOrganizationIds(anyString());
    }

    @Test
    public void testPostAddWithNoAncestorsDoesNotShare() throws Exception {

        // A primary organization (only itself in the ancestor chain) has nothing to inherit.
        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Collections.singletonList(CREATED_ORG_ID));

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(policyService, never()).getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any());
        verify(handler, never()).shareConnectionToOrg(anyString(), anyString(), any(), anyString());
    }

    @Test
    public void testPostAddSkipsNonFutureApplicablePolicy() throws Exception {

        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Arrays.asList(CREATED_ORG_ID, PARENT_ORG_ID));
        // SELECTED_ORG_ONLY is not a future-applicable policy, so nothing is inherited by the new organization.
        ResourceSharingPolicy nonFuturePolicy = policy(RESOURCE_TYPE, PolicyEnum.SELECTED_ORG_ONLY);
        when(policyService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any()))
                .thenReturn(policyMap(PARENT_ORG_ID, nonFuturePolicy));

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(handler, never()).shareConnectionToOrg(anyString(), anyString(), any(), anyString());
    }

    @Test
    public void testPostAddSkipsWhenNoHandlerForResourceType() throws Exception {

        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Arrays.asList(CREATED_ORG_ID, PARENT_ORG_ID));
        // A non-connection resource type (no handler registered) is skipped.
        ResourceSharingPolicy userPolicy = policy(ResourceType.USER, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        when(policyService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any()))
                .thenReturn(policyMap(PARENT_ORG_ID, userPolicy));

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(handler, never()).shareConnectionToOrg(anyString(), anyString(), any(), anyString());
    }

    @Test
    public void testPostAddSharesInheritedConnectionToCreatedOrganization() throws Exception {

        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Arrays.asList(CREATED_ORG_ID, PARENT_ORG_ID));
        // A future-applicable policy whose connection resides in an ancestor above the immediate parent.
        ResourceSharingPolicy inheritedPolicy = mock(ResourceSharingPolicy.class);
        when(inheritedPolicy.getResourceType()).thenReturn(RESOURCE_TYPE);
        when(inheritedPolicy.getSharingPolicy()).thenReturn(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        when(inheritedPolicy.getResourceId()).thenReturn(CONNECTION_ID);
        when(inheritedPolicy.getInitiatingOrgId()).thenReturn(OWNER_ORG_ID);
        when(policyService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any()))
                .thenReturn(policyMap(PARENT_ORG_ID, inheritedPolicy));
        // The connection is shared with the immediate parent, so the created organization inherits it.
        when(associationManager.getSharedConnectionId(RESOURCE_TYPE.name(), CONNECTION_ID, OWNER_ORG_ID,
                PARENT_ORG_ID)).thenReturn(java.util.Optional.of("shadow-in-parent"));

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(handler).shareConnectionToOrg(CONNECTION_ID, CREATED_ORG_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS,
                OWNER_ORG_ID);
    }

    @Test
    public void testPostAddSkipsWhenConnectionNotSharedWithImmediateParent() throws Exception {

        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Arrays.asList(CREATED_ORG_ID, PARENT_ORG_ID));
        ResourceSharingPolicy inheritedPolicy = mock(ResourceSharingPolicy.class);
        when(inheritedPolicy.getResourceType()).thenReturn(RESOURCE_TYPE);
        when(inheritedPolicy.getSharingPolicy()).thenReturn(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        when(inheritedPolicy.getResourceId()).thenReturn(CONNECTION_ID);
        when(inheritedPolicy.getInitiatingOrgId()).thenReturn(OWNER_ORG_ID);
        when(policyService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any()))
                .thenReturn(policyMap(PARENT_ORG_ID, inheritedPolicy));
        // No shadow of the connection exists in the immediate parent, so the created organization must not get one
        // either; a shadow may only be created below an organization that already holds the connection.
        when(associationManager.getSharedConnectionId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(handler, never()).shareConnectionToOrg(anyString(), anyString(), any(), anyString());
    }

    @Test
    public void testPostAddSharesWhenImmediateParentOwnsTheConnection() throws Exception {

        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Arrays.asList(CREATED_ORG_ID, PARENT_ORG_ID));
        // The immediate parent owns the connection, hence the parent connection is the original connection itself
        // and no association lookup is required.
        ResourceSharingPolicy ownedPolicy = policy(RESOURCE_TYPE, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        when(policyService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any()))
                .thenReturn(policyMap(PARENT_ORG_ID, ownedPolicy));

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(handler).shareConnectionToOrg(CONNECTION_ID, CREATED_ORG_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS,
                PARENT_ORG_ID);
        verify(associationManager, never()).getSharedConnectionId(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testPostAddSkipsWhenSharingDisabledForConnectionType() throws Exception {

        // Sharing is switched off for the connection type in this server, so nothing is propagated to the created
        // organization.
        when(handler.isSharingEnabled()).thenReturn(false);
        when(organizationManager.getAncestorOrganizationIds(CREATED_ORG_ID))
                .thenReturn(Arrays.asList(CREATED_ORG_ID, PARENT_ORG_ID));
        ResourceSharingPolicy futurePolicy = policy(RESOURCE_TYPE, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS);
        when(policyService.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(any()))
                .thenReturn(policyMap(PARENT_ORG_ID, futurePolicy));

        eventHandler.handleEvent(addEvent(CREATED_ORG_ID));

        verify(handler, never()).shareConnectionToOrg(anyString(), anyString(), any(), anyString());
    }

    @Test
    public void testPostDeleteCleansUpEvenWhenSharingDisabledForConnectionType() throws Exception {

        // Cleanup of already shared connections is never gated on the connection type being shareable, so no
        // shadow connections are orphaned when the connection type is disabled.
        when(handler.isSharingEnabled()).thenReturn(false);
        when(associationManager.getConnectionAssociationsByResidentOrg(DELETING_ORG_ID)).thenReturn(
                Collections.singletonList(association(CONNECTION_ID, DELETING_ORG_ID, "shadow-a", "shared-org-a")));
        when(associationManager.getConnectionAssociationsBySharedOrg(DELETING_ORG_ID)).thenReturn(
                Collections.singletonList(association(CONNECTION_ID, OWNER_ORG_ID, "shadow-c", DELETING_ORG_ID)));

        eventHandler.handleEvent(deleteEvent(DELETING_ORG_ID));

        verify(handler).unshareConnectionFromAllOrgs(CONNECTION_ID, DELETING_ORG_ID);
        verify(handler).unshareConnectionFromOrg(CONNECTION_ID, DELETING_ORG_ID, OWNER_ORG_ID);
        verify(associationManager).deleteConnectionAssociationsByOrganizationId(DELETING_ORG_ID);
    }

    // ----- Routing -----

    @Test
    public void testUnknownEventIsNoOp() throws Exception {

        eventHandler.handleEvent(new Event("UNRELATED_EVENT", new HashMap<>()));

        verify(associationManager, never()).deleteConnectionAssociationsByOrganizationId(anyString());
        verify(organizationManager, never()).getAncestorOrganizationIds(anyString());
    }

    private Event deleteEvent(String organizationId) {

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        return new Event(Constants.EVENT_POST_DELETE_ORGANIZATION, properties);
    }

    private Event addEvent(String organizationId) {

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(organizationId);
        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        return new Event(Constants.EVENT_POST_ADD_ORGANIZATION, properties);
    }

    private ConnectionAssociation association(String parentConnectionId, String residentOrgId,
                                             String sharedConnectionId, String sharedOrgId) {

        return new ConnectionAssociation.Builder()
                .resourceType(RESOURCE_TYPE)
                .parentConnectionId(parentConnectionId)
                .connectionResidentOrganizationId(residentOrgId)
                .sharedConnectionId(sharedConnectionId)
                .organizationId(sharedOrgId)
                .build();
    }

    private ResourceSharingPolicy policy(ResourceType resourceType, PolicyEnum sharingPolicy) {

        ResourceSharingPolicy policy = mock(ResourceSharingPolicy.class);
        when(policy.getResourceType()).thenReturn(resourceType);
        when(policy.getSharingPolicy()).thenReturn(sharingPolicy);
        when(policy.getResourceId()).thenReturn(CONNECTION_ID);
        when(policy.getInitiatingOrgId()).thenReturn(PARENT_ORG_ID);
        return policy;
    }

    private Map<String, List<ResourceSharingPolicy>> policyMap(String holdingOrgId, ResourceSharingPolicy policy) {

        Map<String, List<ResourceSharingPolicy>> map = new HashMap<>();
        map.put(holdingOrgId, Collections.singletonList(policy));
        return map;
    }
}
