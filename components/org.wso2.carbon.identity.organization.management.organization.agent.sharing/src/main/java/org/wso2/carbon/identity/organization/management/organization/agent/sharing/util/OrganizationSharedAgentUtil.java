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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.util;

import org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal.OrganizationAgentSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Optional;

/**
 * Utility class for organization shared agent management.
 */
public class OrganizationSharedAgentUtil {

    /**
     * Get the agent ID of the associated agent by the organization ID.
     *
     * @param associatedAgentId The ID of the associated (root) agent.
     * @param orgId             The organization ID where the agent is shared.
     * @return An Optional containing the shared agent ID, or empty if no association exists.
     * @throws OrganizationManagementException If an error occurs while retrieving the agent association.
     */
    public static Optional<String> getAgentIdOfAssociatedAgentByOrgId(String associatedAgentId, String orgId)
            throws OrganizationManagementException {

        AgentAssociation agentAssociation = OrganizationAgentSharingDataHolder.getInstance()
                .getOrganizationAgentSharingService()
                .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, orgId);
        if (agentAssociation == null) {
            return Optional.empty();
        }
        return Optional.of(agentAssociation.getAgentId());
    }
}
