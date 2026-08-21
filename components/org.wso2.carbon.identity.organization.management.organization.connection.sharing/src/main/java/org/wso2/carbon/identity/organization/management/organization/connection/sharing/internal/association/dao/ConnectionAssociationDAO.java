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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.dao;

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;

import java.util.List;
import java.util.Optional;

/**
 * DAO for persisting shadow connection associations (table {@code IDN_ORG_CONNECTION_ASSOCIATION}) that link a
 * connection in its associated (parent) organization to the shadow connections created in shared (sub)
 * organizations.
 */
public interface ConnectionAssociationDAO {

    /**
     * Persists a shadow connection association.
     *
     * @param association The association to persist (including its resource type).
     * @throws ConnectionSharingMgtServerException If an error occurs while persisting the association.
     */
    void addConnectionAssociation(ConnectionAssociation association) throws ConnectionSharingMgtServerException;

    /**
     * Resolves the shadow connection ID for a connection in a specific shared organization.
     *
     * @param resourceType        The type of the connection.
     * @param connectionId        The ID of the associated (parent) connection.
     * @param associatedOrgId     The ID of the organization the connection resides in.
     * @param sharedOrgId         The ID of the organization the connection is shared with.
     * @return The shadow connection ID, if present.
     * @throws ConnectionSharingMgtServerException If an error occurs while resolving the shadow connection ID.
     */
    Optional<String> getSharedConnectionId(String resourceType, String connectionId, String associatedOrgId,
                                           String sharedOrgId) throws ConnectionSharingMgtServerException;

    /**
     * Resolves the shadow connection association for a given shadow (shared) resource UUID. Used to resolve a
     * shadow connection back to its parent connection at runtime.
     *
     * @param resourceType       The type of the connection.
     * @param sharedConnectionId The shadow (shared) resource UUID.
     * @return The association, if present.
     * @throws ConnectionSharingMgtServerException If an error occurs while resolving the association.
     */
    Optional<ConnectionAssociation> getConnectionAssociationBySharedConnectionId(String resourceType,
                                                                                 String sharedConnectionId)
            throws ConnectionSharingMgtServerException;

    /**
     * Retrieves all shadow connection associations of a connection.
     *
     * @param resourceType    The type of the connection.
     * @param connectionId    The ID of the associated (parent) connection.
     * @param associatedOrgId The ID of the organization the connection resides in.
     * @return The list of associations.
     * @throws ConnectionSharingMgtServerException If an error occurs while retrieving the associations.
     */
    List<ConnectionAssociation> getConnectionAssociations(String resourceType, String connectionId,
                                                          String associatedOrgId)
            throws ConnectionSharingMgtServerException;

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
    List<ConnectionAssociation> getConnectionAssociations(String resourceType, String connectionId,
                                                          String associatedOrgId, List<String> sharedOrgIds,
                                                          List<ExpressionNode> expressionNodes, String sortOrder,
                                                          int limit) throws ConnectionSharingMgtServerException;

    /**
     * Removes the shadow connection association of a connection in a specific shared organization.
     *
     * @param resourceType    The type of the connection.
     * @param connectionId    The ID of the associated (parent) connection.
     * @param associatedOrgId The ID of the organization the connection resides in.
     * @param sharedOrgId     The ID of the organization the connection was shared with.
     * @throws ConnectionSharingMgtServerException If an error occurs while removing the association.
     */
    void deleteConnectionAssociation(String resourceType, String connectionId, String associatedOrgId,
                                     String sharedOrgId) throws ConnectionSharingMgtServerException;

    /**
     * Retrieves all shadow connection associations whose original (parent) connection resides in the given
     * organization — i.e. the connections that organization owns and has shared with other organizations. Used to
     * unshare an organization's owned connections before it is deleted.
     *
     * @param residentOrgId The ID of the organization that owns the original connections.
     * @return The list of associations (across all connection types and shared organizations).
     * @throws ConnectionSharingMgtServerException If an error occurs while retrieving the associations.
     */
    List<ConnectionAssociation> getConnectionAssociationsByResidentOrg(String residentOrgId)
            throws ConnectionSharingMgtServerException;

    /**
     * Retrieves all shadow connection associations whose shadow connection resides in the given organization —
     * i.e. the connections that organization holds as shadows shared to it from its ancestors. Used to remove the
     * shadow connections held by an organization before it is deleted.
     *
     * @param sharedOrgId The ID of the organization that holds the shadow connections.
     * @return The list of associations (across all connection types and resident organizations).
     * @throws ConnectionSharingMgtServerException If an error occurs while retrieving the associations.
     */
    List<ConnectionAssociation> getConnectionAssociationsBySharedOrg(String sharedOrgId)
            throws ConnectionSharingMgtServerException;

    /**
     * Removes all shadow connection associations referencing the given organization, whether it held the shadow
     * connection or owned the original (parent) connection. Used to clean up associations when an organization is
     * deleted.
     *
     * @param organizationId The ID of the (deleted) organization.
     * @throws ConnectionSharingMgtServerException If an error occurs while removing the associations.
     */
    void deleteConnectionAssociationsByOrganizationId(String organizationId)
            throws ConnectionSharingMgtServerException;
}
