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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.association;

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.dao.ConnectionAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.dao.impl.ConnectionAssociationDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtServerException;

import java.util.List;
import java.util.Optional;

/**
 * Single access point to the shadow connection association store within the connection sharing component. It fronts
 * the {@link ConnectionAssociationDAO} so that all consumers reach the {@code IDN_ORG_CONNECTION_ASSOCIATION} table
 * through this manager rather than the persistence layer directly. The DAO is retained as a separate layer so a
 * caching layer can be introduced here later without touching callers.
 */
public class ConnectionAssociationManager {

    private final ConnectionAssociationDAO connectionAssociationDAO = new ConnectionAssociationDAOImpl();

    /**
     * Persists a shadow connection association.
     *
     * @param association The association to persist (including its resource type).
     * @throws ConnectionSharingMgtServerException If an error occurs while persisting the association.
     */
    public void addConnectionAssociation(ConnectionAssociation association)
            throws ConnectionSharingMgtServerException {

        connectionAssociationDAO.addConnectionAssociation(association);
    }

    /**
     * Resolves the shadow connection ID for a connection in a specific shared organization.
     *
     * @param resourceType    The type of the connection.
     * @param connectionId    The ID of the associated (parent) connection.
     * @param associatedOrgId The ID of the organization the connection resides in.
     * @param sharedOrgId     The ID of the organization the connection is shared with.
     * @return The shadow connection ID, if present.
     * @throws ConnectionSharingMgtServerException If an error occurs while resolving the shadow connection ID.
     */
    public Optional<String> getSharedConnectionId(String resourceType, String connectionId, String associatedOrgId,
                                                  String sharedOrgId) throws ConnectionSharingMgtServerException {

        return connectionAssociationDAO.getSharedConnectionId(resourceType, connectionId, associatedOrgId,
                sharedOrgId);
    }

    /**
     * Resolves the shadow connection association for a given shadow (shared) resource UUID. Used to resolve a shadow
     * connection back to its parent connection at runtime.
     *
     * @param resourceType       The type of the connection.
     * @param sharedConnectionId The shadow (shared) resource UUID.
     * @return The association, if present.
     * @throws ConnectionSharingMgtServerException If an error occurs while resolving the association.
     */
    public Optional<ConnectionAssociation> getConnectionAssociationBySharedConnectionId(String resourceType,
                                                                                        String sharedConnectionId)
            throws ConnectionSharingMgtServerException {

        return connectionAssociationDAO.getConnectionAssociationBySharedConnectionId(resourceType, sharedConnectionId);
    }

    /**
     * Retrieves all shadow connection associations of a connection.
     *
     * @param resourceType    The type of the connection.
     * @param connectionId    The ID of the associated (parent) connection.
     * @param associatedOrgId The ID of the organization the connection resides in.
     * @return The list of associations.
     * @throws ConnectionSharingMgtServerException If an error occurs while retrieving the associations.
     */
    public List<ConnectionAssociation> getConnectionAssociations(String resourceType, String connectionId,
                                                                 String associatedOrgId)
            throws ConnectionSharingMgtServerException {

        return connectionAssociationDAO.getConnectionAssociations(resourceType, connectionId, associatedOrgId);
    }

    /**
     * Retrieves shadow connection associations of a connection, scoped to the given shared organizations, with
     * in-DAO filtering and keyset pagination.
     *
     * @param resourceType    The type of the connection.
     * @param connectionId    The ID of the associated (parent) connection.
     * @param associatedOrgId The ID of the organization the connection resides in.
     * @param sharedOrgIds    The shared organization IDs to scope the search to.
     * @param expressionNodes Expression nodes for in-DAO filtering (id, cursor conditions).
     * @param sortOrder       Sort order for keyset pagination (ASC or DESC).
     * @param limit           Maximum number of records to return (0 = no limit).
     * @return The list of associations.
     * @throws ConnectionSharingMgtServerException If an error occurs while retrieving the associations.
     */
    public List<ConnectionAssociation> getConnectionAssociations(String resourceType, String connectionId,
                                                                 String associatedOrgId, List<String> sharedOrgIds,
                                                                 List<ExpressionNode> expressionNodes,
                                                                 String sortOrder, int limit)
            throws ConnectionSharingMgtServerException {

        return connectionAssociationDAO.getConnectionAssociations(resourceType, connectionId, associatedOrgId,
                sharedOrgIds, expressionNodes, sortOrder, limit);
    }

    /**
     * Retrieves all shadow connection associations whose original (parent) connection resides in the given
     * organization — i.e. the connections that organization owns and has shared with other organizations.
     *
     * @param residentOrgId The ID of the organization that owns the original connections.
     * @return The list of associations (across all connection types and shared organizations).
     * @throws ConnectionSharingMgtServerException If an error occurs while retrieving the associations.
     */
    public List<ConnectionAssociation> getConnectionAssociationsByResidentOrg(String residentOrgId)
            throws ConnectionSharingMgtServerException {

        return connectionAssociationDAO.getConnectionAssociationsByResidentOrg(residentOrgId);
    }

    /**
     * Removes the shadow connection association of a connection in a specific shared organization.
     *
     * @param resourceType    The type of the connection.
     * @param connectionId    The ID of the associated (parent) connection.
     * @param associatedOrgId The ID of the organization the connection resides in.
     * @param sharedOrgId     The ID of the organization the connection was shared with.
     * @throws ConnectionSharingMgtServerException If an error occurs while removing the association.
     */
    public void deleteConnectionAssociation(String resourceType, String connectionId, String associatedOrgId,
                                            String sharedOrgId) throws ConnectionSharingMgtServerException {

        connectionAssociationDAO.deleteConnectionAssociation(resourceType, connectionId, associatedOrgId, sharedOrgId);
    }

    /**
     * Removes all shadow connection associations referencing the given organization, whether it held the shadow
     * connection or owned the original (parent) connection.
     *
     * @param organizationId The ID of the (deleted) organization.
     * @throws ConnectionSharingMgtServerException If an error occurs while removing the associations.
     */
    public void deleteConnectionAssociationsByOrganizationId(String organizationId)
            throws ConnectionSharingMgtServerException {

        connectionAssociationDAO.deleteConnectionAssociationsByOrganizationId(organizationId);
    }
}
