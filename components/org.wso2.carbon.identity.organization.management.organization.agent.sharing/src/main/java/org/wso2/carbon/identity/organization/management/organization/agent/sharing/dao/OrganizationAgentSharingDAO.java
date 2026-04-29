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

import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

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
    default void createOrganizationAgentAssociation(String agentId, String orgId, String associatedAgentId,
                                                    String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("createOrganizationAgentAssociation method is not implemented.");
    }

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
     * Get all the agent associations for a given agent filtered by expression nodes.
     *
     * @param associatedAgentId Actual agent ID of the agent.
     * @param associatedOrgId   The organization ID where the agent is managed.
     * @param orgIdsScope       The list of organization IDs to limit the search scope.
     * @param expressionNodes   The list of expression nodes to filter the agent associations.
     * @param sortOrder         The order to sort the results.
     * @param limit             The maximum number of results to return.
     * @return The list of {@link AgentAssociation}s.
     * @throws OrganizationManagementException If an error occurs while fetching agent associations.
     */
    default List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId,
                                                                         List<String> orgIdsScope,
                                                                         List<ExpressionNode> expressionNodes,
                                                                         String sortOrder, int limit)
            throws OrganizationManagementException {

        throw new NotImplementedException(
                "getAgentAssociationsOfAssociatedAgent method with filters is not implemented.");
    }

    /**
     * Get all the agent associations for a given agent filtered by shared type.
     *
     * @param associatedAgentId Actual agent ID of the agent.
     * @param associatedOrgId   The organization ID where the agent is managed.
     * @param sharedType        The type of sharing relationship to filter the associations.
     * @return The list of {@link AgentAssociation}s.
     * @throws OrganizationManagementServerException If an error occurs while fetching agent associations.
     */
    default List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId,
                                                                         SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("getAgentAssociationsOfAssociatedAgent method is not implemented.");
    }

    /**
     * Checks if the given agent has at least one association with any child organization.
     *
     * @param associatedAgentId The ID of the associated agent.
     * @param associatedOrgId   The organization ID where the agent's identity is managed.
     * @return True if the agent has at least one association with any organization.
     * @throws OrganizationManagementServerException If an error occurs while checking agent associations.
     */
    default boolean hasAgentAssociations(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("hasAgentAssociations method is not implemented.");
    }

    /**
     * Checks if the given agent has at least one association within the specified organization scope.
     *
     * @param associatedAgentId The ID of the associated agent.
     * @param associatedOrgId   The organization ID where the agent's identity is managed.
     * @param orgIds            The list of organization IDs defining the scope to check for associations.
     * @return True if the agent has at least one association within the specified organization scope.
     * @throws OrganizationManagementServerException If an error occurs while checking agent associations.
     */
    default boolean hasAgentAssociationsInOrganizations(String associatedAgentId, String associatedOrgId,
                                                        List<String> orgIds)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("hasAgentAssociationsInOrganizations DAO method is not implemented.");
    }

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
     * Get the shared agent association of a shared agent.
     *
     * @param agentId        The agent ID of the shared agent.
     * @param organizationId The organization ID of the agent.
     * @return The agent association of the agent.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the agent association.
     */
    AgentAssociation getAgentAssociation(String agentId, String organizationId)
            throws OrganizationManagementServerException;

    /**
     * Retrieve the list of agent names that are not eligible to be removed from the specified role within the given
     * tenant domain, based on the permissions of the requesting organization.
     *
     * @param roleId                               The role ID from which the agents are to be removed.
     * @param deletedDomainQualifiedAgentNamesList The list of agent names with domain intended for removal.
     * @param tenantDomain                         The tenant domain where the operation is being performed.
     * @param requestingOrgId                      The ID of the requesting organization performing the operation.
     * @return A list of agent names that the requesting organization is not permitted to remove from the given role.
     * @throws IdentityRoleManagementException If an error occurs while validating the permissions or retrieving
     *                                         eligible agent names.
     */
    default List<String> getNonDeletableAgentRoleAssignments(String roleId,
                                                             List<String> deletedDomainQualifiedAgentNamesList,
                                                             String tenantDomain, String requestingOrgId)
            throws IdentityRoleManagementException {

        throw new NotImplementedException("getNonDeletableAgentRoleAssignments method is not implemented.");
    }

    /**
     * Retrieves the shared agent roles among the given agent roles.
     *
     * @param allAgentRolesOfSharedAgent List of all roles associated with the shared agent.
     * @param tenantDomain               The tenant domain of the shared agent.
     * @return List of shared agent roles.
     * @throws IdentityRoleManagementException If an error occurs while retrieving shared agent roles.
     */
    default List<String> getSharedAgentRolesFromAgentRoles(List<String> allAgentRolesOfSharedAgent,
                                                           String tenantDomain)
            throws IdentityRoleManagementException {

        throw new NotImplementedException("getSharedAgentRolesFromAgentRoles method is not implemented.");
    }

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
    default void addEditRestrictionsForSharedAgentRole(String roleId, String agentName, String tenantDomain,
                                                       String domainName, EditOperation editOperation,
                                                       String permittedOrgId)
            throws AgentSharingMgtServerException {

        throw new NotImplementedException("addEditRestrictionsForSharedAgentRole method is not implemented.");
    }

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
    default List<String> getRolesSharedWithAgentInOrganization(String agentName, int tenantId, String domainName)
            throws AgentSharingMgtServerException {

        throw new NotImplementedException("getRolesSharedWithAgentInOrganization method is not implemented.");
    }

    /**
     * Get the agent associations of the associated agent in the given organizations.
     *
     * @param associatedAgentId The ID of the associated agent.
     * @param orgIds            The list of organization IDs.
     * @return The list of {@link AgentAssociation}s for the given agent in the specified organizations.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the agent associations.
     */
    default List<AgentAssociation> getAgentAssociationsOfGivenAgentOnGivenOrgs(String associatedAgentId,
                                                                                List<String> orgIds)
            throws OrganizationManagementServerException {

        throw new NotImplementedException(
                "getAgentAssociationsOfGivenAgentOnGivenOrgs method is not implemented.");
    }

    /**
     * Updates the shared type of agent association.
     *
     * @param id         The ID of the agent association.
     * @param sharedType The new shared type to be set for the agent association.
     * @throws OrganizationManagementServerException If an error occurs while updating the shared type.
     */
    default void updateSharedTypeOfAgentAssociation(int id, SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("updateSharedTypeOfAgentAssociation method is not implemented.");
    }
}
