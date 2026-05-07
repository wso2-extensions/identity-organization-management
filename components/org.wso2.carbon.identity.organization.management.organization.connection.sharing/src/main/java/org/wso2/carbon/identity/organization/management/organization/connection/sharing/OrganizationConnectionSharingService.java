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

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Collections;
import java.util.List;

/**
 * Service interface for managing shadow connection associations across shared organizations.
 */
public interface OrganizationConnectionSharingService {

    /**
     * Retrieves shadow connection associations for a given parent connection, scoped to the provided organization list.
     *
     * @param connectionId    The ID of the parent connection being queried.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @param orgIdsScope     The list of child organization IDs to scope the search to.
     * @param expressionNodes Expression nodes for in-DAO filtering (id, name, cursor conditions).
     * @param sortOrder       Sort order for keyset pagination (ASC or DESC).
     * @param limit           Maximum number of records to return (0 = no limit).
     * @return A list of {@link ConnectionAssociation}s representing shadow connections.
     * @throws OrganizationManagementException If an error occurs while fetching associations.
     */
    default List<ConnectionAssociation> getConnectionAssociationsOfGivenConnection(
            String connectionId, String initiatingOrgId, List<String> orgIdsScope,
            List<ExpressionNode> expressionNodes, String sortOrder, int limit)
            throws OrganizationManagementException {

        return Collections.emptyList();
    }
}
