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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.dao;

import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.List;

/**
 * DAO interface for organization agent sharing.
 */
public interface OrganizationAgentSharingDAO {

    /**
     * Creates the association between the shared agent and the actual agent in the shared organization.
     *
     * @param agentId           ID of the agent who gets created in the organization.
     * @param orgId             Organization ID of the agent's shared organization.
     * @param associatedAgentId Actual agent ID of the associated agent.
     * @param associatedOrgId   The organization ID where the associated agent is managed.
     * @param sharedType        The type of sharing for the agent in the organization.
     * @throws OrganizationManagementServerException If an error occurs while creating the organization agent
     *                                               association.
     */
    void createOrganizationAgentAssociation(String agentId, String orgId, String associatedAgentId,
                                            String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException;

    /**
     * Delete the organization agent association for a shared agent in a shared organization.
     *
     * @param agentId         The ID of the agent.
     * @param associatedOrgId The organization ID where the associated agent's identity is managed.
     * @return True if the agent association is deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the agent association.
     */
    boolean deleteAgentAssociationOfAgentByAssociatedOrg(String agentId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Delete all the organization agent associations for a given agent.
     *
     * @param associatedAgentId Actual agent ID of the agent.
     * @param associatedOrgId   The organization ID where the agent's identity is managed.
     * @return True if all the agent associations are deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the agent associations.
     */
    boolean deleteAgentAssociationsOfAssociatedAgent(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Get all the agent associations for a given agent.
     *
     * @param associatedAgentId Actual agent ID of the agent.
     * @param associatedOrgId   The organization ID where the agent is managed.
     * @return The list of {@link AgentAssociation}s.
     * @throws OrganizationManagementServerException If an error occurs while fetching agent associations.
     */
    List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Delete all agent associations for a deleted organization.
     *
     * @param orgId The organization ID that is being deleted.
     * @return True if all the agent associations are deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the agent associations.
     */
    boolean deleteAgentAssociationsByOrganizationId(String orgId)
            throws OrganizationManagementServerException;

    /**
     * Checks if the given agent has at least one association within the specified organization scope.
     *
     * @param associatedAgentId The ID of the associated agent.
     * @param associatedOrgId   The organization ID where the agent's identity is managed.
     * @param orgIds            The list of organization IDs defining the scope to check for associations.
     * @return True if the agent has at least one association within the specified organization scope.
     * @throws OrganizationManagementServerException If an error occurs while checking agent associations.
     */
    boolean hasAgentAssociationsInOrganizations(String associatedAgentId, String associatedOrgId,
                                                List<String> orgIds)
            throws OrganizationManagementServerException;

    /**
     * Get the organization agent association of a given agent in a given organization.
     *
     * @param associatedAgentId ID of the associated agent.
     * @param orgId             Organization ID where the agent is shared.
     * @return The organization agent association details.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the agent association.
     */
    AgentAssociation getAgentAssociationOfAssociatedAgentByOrgId(String associatedAgentId, String orgId)
            throws OrganizationManagementServerException;

    /**
     * Adds edit restrictions for shared agent roles.
     *
     * @param roleId         The roleId of the role to be restricted.
     * @param agentName      The name of the shared agent.
     * @param tenantDomain   The tenant domain of the shared agent.
     * @param domainName     The domain name associated with the agent.
     * @param editOperation  The type of edit operation being performed.
     * @param permittedOrgId The organization ID with permitted access.
     * @throws AgentSharingMgtServerException If an error occurs while adding edit restrictions.
     */
    void addEditRestrictionsForSharedAgentRole(String roleId, String agentName, String tenantDomain,
                                               String domainName, EditOperation editOperation,
                                               String permittedOrgId)
            throws AgentSharingMgtServerException;

    /**
     * Retrieves the IDs of roles shared with an agent in a specific organization.
     *
     * @param agentName  The name of the agent whose shared roles are to be retrieved.
     * @param tenantId   The ID of the tenant to which the agent belongs.
     * @param domainName The name of the domain in which the agent resides.
     * @return A {@link List} containing the IDs of the shared roles.
     * @throws AgentSharingMgtServerException If an error occurs while retrieving the roles shared with the agent
     *                                        in the organization.
     */
    List<String> getRolesSharedWithAgentInOrganization(String agentName, int tenantId, String domainName)
            throws AgentSharingMgtServerException;

}
