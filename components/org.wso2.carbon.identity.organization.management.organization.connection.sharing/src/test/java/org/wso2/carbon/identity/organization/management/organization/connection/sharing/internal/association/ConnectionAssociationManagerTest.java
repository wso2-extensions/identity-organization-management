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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.dao.ConnectionAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectionAssociationManager}. The manager is a thin delegation layer over the
 * {@link ConnectionAssociationDAO}; these tests verify that every call is forwarded to the DAO with the same
 * arguments, that the DAO's result is returned unchanged, and that DAO exceptions are propagated.
 */
public class ConnectionAssociationManagerTest {

    private static final String RESOURCE_TYPE = ResourceType.CONNECTION_IDENTITY_PROVIDER.name();
    private static final String CONNECTION_ID = "conn-1";
    private static final String SHARED_CONNECTION_ID = "shadow-1";
    private static final String ASSOCIATED_ORG_ID = "resident-org-1";
    private static final String SHARED_ORG_ID = "shared-org-1";

    private ConnectionAssociationDAO connectionAssociationDAO;
    private ConnectionAssociationManager connectionAssociationManager;

    @BeforeMethod
    public void setUp() throws Exception {

        connectionAssociationDAO = mock(ConnectionAssociationDAO.class);
        connectionAssociationManager = new ConnectionAssociationManager();
        // The manager instantiates its DAO internally; inject the mock into the private final field.
        Field daoField = ConnectionAssociationManager.class.getDeclaredField("connectionAssociationDAO");
        daoField.setAccessible(true);
        daoField.set(connectionAssociationManager, connectionAssociationDAO);
    }

    private ConnectionAssociation buildAssociation() {

        return new ConnectionAssociation.Builder()
                .id(1)
                .resourceType(ResourceType.CONNECTION_IDENTITY_PROVIDER)
                .sharedConnectionId(SHARED_CONNECTION_ID)
                .organizationId(SHARED_ORG_ID)
                .parentConnectionId(CONNECTION_ID)
                .connectionResidentOrganizationId(ASSOCIATED_ORG_ID)
                .build();
    }

    @Test
    public void testAddConnectionAssociation() throws Exception {

        ConnectionAssociation association = buildAssociation();
        doNothing().when(connectionAssociationDAO).addConnectionAssociation(association);

        connectionAssociationManager.addConnectionAssociation(association);

        verify(connectionAssociationDAO).addConnectionAssociation(association);
    }

    @Test(expectedExceptions = ConnectionSharingMgtServerException.class)
    public void testAddConnectionAssociationPropagatesException() throws Exception {

        doThrow(new ConnectionSharingMgtServerException("code", "error", "error occurred"))
                .when(connectionAssociationDAO).addConnectionAssociation(any());

        connectionAssociationManager.addConnectionAssociation(buildAssociation());
    }

    @Test
    public void testGetSharedConnectionId() throws Exception {

        when(connectionAssociationDAO.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID,
                SHARED_ORG_ID)).thenReturn(Optional.of(SHARED_CONNECTION_ID));

        Optional<String> result = connectionAssociationManager.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID,
                ASSOCIATED_ORG_ID, SHARED_ORG_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(result.get(), SHARED_CONNECTION_ID);
        verify(connectionAssociationDAO).getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID,
                SHARED_ORG_ID);
    }

    @Test
    public void testGetSharedConnectionIdEmpty() throws Exception {

        when(connectionAssociationDAO.getSharedConnectionId(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        Optional<String> result = connectionAssociationManager.getSharedConnectionId(RESOURCE_TYPE, CONNECTION_ID,
                ASSOCIATED_ORG_ID, SHARED_ORG_ID);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testGetConnectionAssociationBySharedConnectionId() throws Exception {

        ConnectionAssociation association = buildAssociation();
        when(connectionAssociationDAO.getConnectionAssociationBySharedConnectionId(RESOURCE_TYPE, SHARED_CONNECTION_ID))
                .thenReturn(Optional.of(association));

        Optional<ConnectionAssociation> result = connectionAssociationManager
                .getConnectionAssociationBySharedConnectionId(RESOURCE_TYPE, SHARED_CONNECTION_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertSame(result.get(), association);
        verify(connectionAssociationDAO).getConnectionAssociationBySharedConnectionId(RESOURCE_TYPE,
                SHARED_CONNECTION_ID);
    }

    @Test
    public void testGetConnectionAssociations() throws Exception {

        List<ConnectionAssociation> associations = Collections.singletonList(buildAssociation());
        when(connectionAssociationDAO.getConnectionAssociations(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID))
                .thenReturn(associations);

        List<ConnectionAssociation> result = connectionAssociationManager.getConnectionAssociations(RESOURCE_TYPE,
                CONNECTION_ID, ASSOCIATED_ORG_ID);

        Assert.assertSame(result, associations);
        verify(connectionAssociationDAO).getConnectionAssociations(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID);
    }

    @Test
    public void testGetConnectionAssociationsWithFilteringAndPagination() throws Exception {

        List<ConnectionAssociation> associations = Collections.singletonList(buildAssociation());
        List<String> sharedOrgIds = Collections.singletonList(SHARED_ORG_ID);
        List<ExpressionNode> expressionNodes = Collections.emptyList();
        when(connectionAssociationDAO.getConnectionAssociations(eq(RESOURCE_TYPE), eq(CONNECTION_ID),
                eq(ASSOCIATED_ORG_ID), eq(sharedOrgIds), eq(expressionNodes), eq("ASC"), anyInt()))
                .thenReturn(associations);

        List<ConnectionAssociation> result = connectionAssociationManager.getConnectionAssociations(RESOURCE_TYPE,
                CONNECTION_ID, ASSOCIATED_ORG_ID, sharedOrgIds, expressionNodes, "ASC", 10);

        Assert.assertSame(result, associations);
        verify(connectionAssociationDAO).getConnectionAssociations(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID,
                sharedOrgIds, expressionNodes, "ASC", 10);
    }

    @Test
    public void testGetConnectionAssociationsByResidentOrg() throws Exception {

        List<ConnectionAssociation> associations = Collections.singletonList(buildAssociation());
        when(connectionAssociationDAO.getConnectionAssociationsByResidentOrg(ASSOCIATED_ORG_ID))
                .thenReturn(associations);

        List<ConnectionAssociation> result =
                connectionAssociationManager.getConnectionAssociationsByResidentOrg(ASSOCIATED_ORG_ID);

        Assert.assertSame(result, associations);
        verify(connectionAssociationDAO).getConnectionAssociationsByResidentOrg(ASSOCIATED_ORG_ID);
    }

    @Test
    public void testGetConnectionAssociationsBySharedOrg() throws Exception {

        List<ConnectionAssociation> associations = Collections.singletonList(buildAssociation());
        when(connectionAssociationDAO.getConnectionAssociationsBySharedOrg(SHARED_ORG_ID))
                .thenReturn(associations);

        List<ConnectionAssociation> result =
                connectionAssociationManager.getConnectionAssociationsBySharedOrg(SHARED_ORG_ID);

        Assert.assertSame(result, associations);
        verify(connectionAssociationDAO).getConnectionAssociationsBySharedOrg(SHARED_ORG_ID);
    }

    @Test
    public void testDeleteConnectionAssociation() throws Exception {

        doNothing().when(connectionAssociationDAO).deleteConnectionAssociation(RESOURCE_TYPE, CONNECTION_ID,
                ASSOCIATED_ORG_ID, SHARED_ORG_ID);

        connectionAssociationManager.deleteConnectionAssociation(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID,
                SHARED_ORG_ID);

        verify(connectionAssociationDAO).deleteConnectionAssociation(RESOURCE_TYPE, CONNECTION_ID, ASSOCIATED_ORG_ID,
                SHARED_ORG_ID);
    }

    @Test
    public void testDeleteConnectionAssociationsByOrganizationId() throws Exception {

        doNothing().when(connectionAssociationDAO).deleteConnectionAssociationsByOrganizationId(SHARED_ORG_ID);

        connectionAssociationManager.deleteConnectionAssociationsByOrganizationId(SHARED_ORG_ID);

        verify(connectionAssociationDAO).deleteConnectionAssociationsByOrganizationId(SHARED_ORG_ID);
    }

    @Test(expectedExceptions = ConnectionSharingMgtServerException.class)
    public void testDeleteConnectionAssociationsByOrganizationIdPropagatesException() throws Exception {

        doThrow(new ConnectionSharingMgtServerException("code", "error", "error occurred"))
                .when(connectionAssociationDAO).deleteConnectionAssociationsByOrganizationId(anyString());

        connectionAssociationManager.deleteConnectionAssociationsByOrganizationId(SHARED_ORG_ID);
    }
}
