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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing;

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionType;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.ConnectionAssociation;

import java.util.List;
import java.util.Optional;

/**
 * Service-level interface exposing read access to shadow connection associations (the {@code
 * IDN_ORG_CONNECTION_ASSOCIATION} table) for consumers outside this component — notably the connection type
 * handler subsystem, which needs to resolve a shadow connection back to its parent without reaching into the
 * persistence (DAO) layer directly.
 */
public interface ConnectionAssociationService {

    /**
     * Resolves the shadow connection association for a given shadow (shared) resource UUID.
     *
     * @param connectionType     The connection type.
     * @param sharedConnectionId The shadow (shared) resource UUID.
     * @return The association, if present.
     * @throws ConnectionSharingMgtException If an error occurs while resolving the association.
     */
    Optional<ConnectionAssociation> getConnectionAssociationBySharedConnectionId(ConnectionType connectionType,
                                                                                 String sharedConnectionId)
            throws ConnectionSharingMgtException;

    /**
     * Retrieves the shadow connection associations of a connection that resides in the given organization — i.e.
     * the shadow copies of the connection across the organizations it is shared with.
     *
     * @param connectionType         The connection type.
     * @param connectionId           The ID of the original (parent) connection.
     * @param residentOrganizationId The ID of the organization that owns the original connection.
     * @return The list of associations.
     * @throws ConnectionSharingMgtException If an error occurs while retrieving the associations.
     */
    List<ConnectionAssociation> getConnectionAssociations(ConnectionType connectionType, String connectionId,
                                                          String residentOrganizationId)
            throws ConnectionSharingMgtException;
}
