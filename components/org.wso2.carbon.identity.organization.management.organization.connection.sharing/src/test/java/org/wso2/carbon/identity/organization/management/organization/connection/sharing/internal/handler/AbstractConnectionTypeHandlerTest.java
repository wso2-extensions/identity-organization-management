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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.ConnectionAssociationManager;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util.ConnectionSharingAuditLogger;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the share / unshare mechanics in {@link AbstractConnectionTypeHandler} (the resource-type-agnostic
 * base). A minimal concrete subclass supplies the abstract create/delete-shared-resource operations so the base's
 * orchestration of the association manager, the organization manager and the descendant cascade can be verified.
 */
public class AbstractConnectionTypeHandlerTest {

    private static final String RESOURCE_TYPE = ResourceType.CONNECTION_IDENTITY_PROVIDER.name();
    private static final String CONNECTION_ID = "connection-id";
    private static final String INITIATING_ORG_ID = "initiating-org-id";
    private static final String TARGET_ORG_ID = "target-org-id";
    private static final String CHILD_ORG_ID = "child-org-id";
    private static final String CREATED_SHADOW_ID = "created-shadow-id";

    private MockedStatic<ConnectionSharingDataHolder> mockedDataHolder;
    private MockedStatic<ConnectionSharingAuditLogger> mockedAuditLogger;
    private ConnectionAssociationManager associationManager;
    private OrganizationManager organizationManager;
    private TestConnectionTypeHandler handler;

    @BeforeMethod
    public void setUp() {

        mockedDataHolder = mockStatic(ConnectionSharingDataHolder.class);
        mockedAuditLogger = mockStatic(ConnectionSharingAuditLogger.class);

        associationManager = mock(ConnectionAssociationManager.class);
        organizationManager = mock(OrganizationManager.class);
        ConnectionSharingDataHolder dataHolder = mock(ConnectionSharingDataHolder.class);
        mockedDataHolder.when(ConnectionSharingDataHolder::getInstance).thenReturn(dataHolder);
        when(dataHolder.getConnectionAssociationManager()).thenReturn(associationManager);
        when(dataHolder.getOrganizationManager()).thenReturn(organizationManager);

        handler = new TestConnectionTypeHandler();
    }

    @AfterMethod
    public void tearDown() {

        mockedDataHolder.close();
        mockedAuditLogger.close();
    }

    @Test
    public void testShareConnectionToOrgCreatesShadowAndAssociation() throws Exception {

        when(associationManager.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID, TARGET_ORG_ID))
                .thenReturn(Optional.empty());
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("tenant-domain");

        handler.shareConnectionToOrg(CONNECTION_ID, TARGET_ORG_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS,
                INITIATING_ORG_ID);

        ArgumentCaptor<ConnectionAssociation> captor = ArgumentCaptor.forClass(ConnectionAssociation.class);
        verify(associationManager).addConnectionAssociation(captor.capture());
        ConnectionAssociation association = captor.getValue();
        Assert.assertEquals(association.getSharedConnectionId(), CREATED_SHADOW_ID);
        Assert.assertEquals(association.getParentConnectionId(), CONNECTION_ID);
        Assert.assertEquals(association.getConnectionResidentOrganizationId(), INITIATING_ORG_ID);
        Assert.assertEquals(association.getOrganizationId(), TARGET_ORG_ID);
    }

    @Test
    public void testShareConnectionToOrgSkipsWhenAlreadyShared() throws Exception {

        when(associationManager.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID, TARGET_ORG_ID))
                .thenReturn(Optional.of("existing-shadow-id"));

        handler.shareConnectionToOrg(CONNECTION_ID, TARGET_ORG_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS,
                INITIATING_ORG_ID);

        Assert.assertTrue(handler.createdConnections.isEmpty());
        verify(associationManager, never()).addConnectionAssociation(any());
    }

    @Test
    public void testUnshareConnectionFromOrgDeletesShadowAndAssociation() throws Exception {

        when(associationManager.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID, TARGET_ORG_ID))
                .thenReturn(Optional.of("shadow-id"));
        when(organizationManager.getChildOrganizationsIds(TARGET_ORG_ID, true)).thenReturn(Collections.emptyList());
        when(organizationManager.resolveTenantDomain(TARGET_ORG_ID)).thenReturn("target-tenant");

        handler.unshareConnectionFromOrg(CONNECTION_ID, TARGET_ORG_ID, INITIATING_ORG_ID);

        Assert.assertTrue(handler.deletedShadowIds.contains("shadow-id"));
        verify(associationManager).deleteConnectionAssociation(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID,
                TARGET_ORG_ID);
    }

    @Test
    public void testUnshareConnectionFromOrgSkipsWhenNotShared() throws Exception {

        when(associationManager.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID, TARGET_ORG_ID))
                .thenReturn(Optional.empty());
        when(organizationManager.getChildOrganizationsIds(TARGET_ORG_ID, true)).thenReturn(Collections.emptyList());

        handler.unshareConnectionFromOrg(CONNECTION_ID, TARGET_ORG_ID, INITIATING_ORG_ID);

        Assert.assertTrue(handler.deletedShadowIds.isEmpty());
        verify(associationManager, never()).deleteConnectionAssociation(anyString(), anyString(), anyString(),
                anyString());
    }

    @Test
    public void testUnshareConnectionFromOrgCascadesToChildOrganizations() throws Exception {

        when(associationManager.getSharedConnectionId(eq(RESOURCE_TYPE), eq(CONNECTION_ID), eq(INITIATING_ORG_ID),
                eq(TARGET_ORG_ID))).thenReturn(Optional.of("shadow-parent"));
        when(associationManager.getSharedConnectionId(eq(RESOURCE_TYPE), eq(CONNECTION_ID), eq(INITIATING_ORG_ID),
                eq(CHILD_ORG_ID))).thenReturn(Optional.of("shadow-child"));
        when(organizationManager.getChildOrganizationsIds(TARGET_ORG_ID, true))
                .thenReturn(Collections.singletonList(CHILD_ORG_ID));
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("tenant-domain");

        handler.unshareConnectionFromOrg(CONNECTION_ID, TARGET_ORG_ID, INITIATING_ORG_ID);

        // The shadow is removed from the target organization and cascaded to its descendant.
        Assert.assertTrue(handler.deletedShadowIds.contains("shadow-parent"));
        Assert.assertTrue(handler.deletedShadowIds.contains("shadow-child"));
        verify(associationManager).deleteConnectionAssociation(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID,
                TARGET_ORG_ID);
        verify(associationManager).deleteConnectionAssociation(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID,
                CHILD_ORG_ID);
    }

    @Test
    public void testShareConnectionToAllOrgsSharesToAllDescendants() throws Exception {

        when(organizationManager.getChildOrganizationsIds(INITIATING_ORG_ID, true))
                .thenReturn(Arrays.asList("child-1", "child-2"));
        when(associationManager.getSharedConnectionId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("tenant-domain");

        handler.shareConnectionToAllOrgs(CONNECTION_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS, INITIATING_ORG_ID);

        verify(associationManager, times(2)).addConnectionAssociation(any());
    }

    @Test
    public void testShareConnectionToExistingChildOrgs() throws Exception {

        when(organizationManager.getChildOrganizationsIds(TARGET_ORG_ID, true))
                .thenReturn(Collections.singletonList("grandchild-1"));
        when(associationManager.getSharedConnectionId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("tenant-domain");

        handler.shareConnectionToExistingChildOrgs(CONNECTION_ID, TARGET_ORG_ID,
                PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN, INITIATING_ORG_ID);

        verify(associationManager, times(1)).addConnectionAssociation(any());
    }

    @Test
    public void testValidateConnectionShareEligibilityDefaultIsNoOp() throws Exception {

        // The base handler inherits the interface's no-op eligibility check.
        handler.validateConnectionShareEligibility(CONNECTION_ID, INITIATING_ORG_ID);
    }

    @Test
    public void testUnshareConnectionFromAllOrgsRemovesEveryShadow() throws Exception {

        ConnectionAssociation first = association("shadow-1", "shared-org-1");
        ConnectionAssociation second = association("shadow-2", "shared-org-2");
        when(associationManager.getConnectionAssociations(RESOURCE_TYPE, CONNECTION_ID, INITIATING_ORG_ID))
                .thenReturn(Arrays.asList(first, second));
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("tenant-domain");

        handler.unshareConnectionFromAllOrgs(CONNECTION_ID, INITIATING_ORG_ID);

        Assert.assertTrue(handler.deletedShadowIds.contains("shadow-1"));
        Assert.assertTrue(handler.deletedShadowIds.contains("shadow-2"));
        verify(associationManager, times(2)).deleteConnectionAssociation(anyString(), anyString(), anyString(),
                anyString());
    }

    @Test
    public void testShareSkipsChildOrgsWhenTargetNotEligible() throws Exception {

        // A client error while sharing to an organization skips it (and its descendants) without failing the whole
        // operation.
        handler.failCreateWithClientError = true;
        when(organizationManager.getChildOrganizationsIds(anyString(), eq(true)))
                .thenReturn(Collections.singletonList("child-1"));
        when(associationManager.getSharedConnectionId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("tenant-domain");

        handler.shareConnectionToAllOrgs(CONNECTION_ID, PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS, INITIATING_ORG_ID);

        verify(associationManager, never()).addConnectionAssociation(any());
    }

    private ConnectionAssociation association(String sharedConnectionId, String sharedOrgId) {

        return new ConnectionAssociation.Builder()
                .resourceType(ResourceType.CONNECTION_IDENTITY_PROVIDER)
                .parentConnectionId(CONNECTION_ID)
                .connectionResidentOrganizationId(INITIATING_ORG_ID)
                .sharedConnectionId(sharedConnectionId)
                .organizationId(sharedOrgId)
                .build();
    }

    /**
     * Concrete handler that records the abstract create/delete-shared-resource calls so the base orchestration can
     * be asserted without a real identity provider persistence layer.
     */
    private static class TestConnectionTypeHandler extends AbstractConnectionTypeHandler {

        private final List<String> createdConnections = new ArrayList<>();
        private final List<String> deletedShadowIds = new ArrayList<>();
        private boolean failCreateWithClientError;

        @Override
        public ResourceType getResourceType() {

            return ResourceType.CONNECTION_IDENTITY_PROVIDER;
        }

        @Override
        protected String createSharedResource(String connectionId, String residentOrgId, String residentTenantDomain,
                                              String targetTenantDomain)
                throws org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception
                        .ConnectionSharingMgtClientException {

            if (failCreateWithClientError) {
                throw new org.wso2.carbon.identity.organization.management.organization.connection.sharing.api
                        .exception.ConnectionSharingMgtClientException("code", "message", "not eligible");
            }
            createdConnections.add(connectionId);
            return CREATED_SHADOW_ID;
        }

        @Override
        protected void deleteSharedResource(String sharedResourceId, String targetTenantDomain) {

            deletedShadowIds.add(sharedResourceId);
        }
    }
}
