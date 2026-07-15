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
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dao.ConnectionAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dao.impl.ConnectionAssociationDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.ConnectionAssociation;

import java.util.List;
import java.util.Optional;

/**
 * Default {@link ConnectionAssociationService} implementation, backed by the {@link ConnectionAssociationDAO}.
 */
public class ConnectionAssociationServiceImpl implements ConnectionAssociationService {

    private final ConnectionAssociationDAO connectionAssociationDAO = new ConnectionAssociationDAOImpl();

    @Override
    public Optional<ConnectionAssociation> getConnectionAssociationBySharedConnectionId(
            ConnectionType connectionType, String sharedConnectionId) throws ConnectionSharingMgtException {

        return connectionAssociationDAO.getConnectionAssociationBySharedConnectionId(
                connectionType.getResourceType().name(), sharedConnectionId);
    }

    @Override
    public List<ConnectionAssociation> getConnectionAssociations(ConnectionType connectionType, String connectionId,
                                                                 String residentOrganizationId)
            throws ConnectionSharingMgtException {

        return connectionAssociationDAO.getConnectionAssociations(connectionType.getResourceType().name(), connectionId,
                residentOrganizationId);
    }
}
