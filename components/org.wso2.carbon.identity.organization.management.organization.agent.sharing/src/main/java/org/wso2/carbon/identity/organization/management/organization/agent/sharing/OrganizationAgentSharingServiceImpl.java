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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing;

import org.osgi.annotation.bundle.Capability;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.dao.OrganizationAgentSharingDAO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.dao.OrganizationAgentSharingDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal.OrganizationAgentSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.DEFAULT_PROFILE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ID_CLAIM_READ_ONLY;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.PROCESS_ADD_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATE_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Implementation of the organization agent sharing service.
 *
 * <p>When an agent is shared with an organization, a corresponding entry is provisioned in that
 * organization's agent user store and an association record is created in the database.</p>
 */
@Capability(
        namespace = "osgi.service",
        attribute = {
                "objectClass=org.wso2.carbon.identity.organization.management.organization.agent.sharing." +
                        "OrganizationAgentSharingService",
                "service.scope=singleton"
        }
)
public class OrganizationAgentSharingServiceImpl implements OrganizationAgentSharingService {

    private final OrganizationAgentSharingDAO organizationAgentSharingDAO = new OrganizationAgentSharingDAOImpl();

    /**
     * Provisions a shared agent entry in the target organization's agent user store and creates the
     * corresponding agent association record in the database.
     *
     * @param orgId             The ID of the organization where the agent is being shared.
     * @param associatedAgentId The ID of the actual agent in the resident organization.
     * @param associatedOrgId   The ID of the organization where the agent's identity is managed.
     * @param sharedType        The type of sharing for the agent.
     * @throws OrganizationManagementException If an error occurs while provisioning the shared agent entry.
     */
    @Override
    public void shareOrganizationAgent(String orgId, String associatedAgentId, String associatedOrgId,
                                       SharedType sharedType) throws OrganizationManagementException {

        try {
            String sharedOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            startTenantFlow(sharedOrgTenantDomain);
            IdentityUtil.threadLocalProperties.get().put(PROCESS_ADD_SHARED_AGENT, true);

            // Set up the claims for the shared agent entry.
            HashMap<String, String> agentClaims = new HashMap<>();
            agentClaims.put(CLAIM_MANAGED_ORGANIZATION, associatedOrgId);
            agentClaims.put(ID_CLAIM_READ_ONLY, "true");
            UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(true);

            // Add the shared agent entry to the target organization's agent user store.
            int sharedOrgTenantId = IdentityTenantUtil.getTenantId(sharedOrgTenantDomain);
            String domainName = IdentityUtil.getAgentIdentityUserstoreName();
            RealmService realmService = OrganizationAgentSharingDataHolder.getInstance().getRealmService();
            AbstractUserStoreManager sharedOrgAgentStoreManager = (AbstractUserStoreManager)
                    ((UserStoreManager) realmService.getTenantUserRealm(sharedOrgTenantId).getUserStoreManager())
                            .getSecondaryUserStoreManager(domainName);
            sharedOrgAgentStoreManager.addUser(associatedAgentId, generatePassword(), null, agentClaims, DEFAULT_PROFILE);

            // Create the agent association record in the database.
            // Agent ID equals agent username, so associatedAgentId is used directly as the shared agent's ID.
            organizationAgentSharingDAO.createOrganizationAgentAssociation(associatedAgentId, orgId,
                    associatedAgentId, associatedOrgId, sharedType);
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_SHARED_USER, e, orgId);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(PROCESS_ADD_SHARED_AGENT);
            endTenantFlow();
        }
    }

    @Override
    public boolean unshareOrganizationAgents(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.deleteAgentAssociationsOfAssociatedAgent(associatedAgentId,
                associatedOrgId);
    }

    @Override
    public boolean unshareOrganizationAgentInSharedOrganization(String associatedAgentId, String sharedOrgId)
            throws OrganizationManagementException {

        AgentAssociation agentAssociation =
                organizationAgentSharingDAO.getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, sharedOrgId);
        if (agentAssociation == null) {
            return false;
        }
        return organizationAgentSharingDAO.deleteAgentAssociationOfAgentByAssociatedOrg(
                agentAssociation.getAgentId(), sharedOrgId);
    }

    @Override
    public boolean deleteAgentAssociation(String agentId, String associatedOrgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.deleteAgentAssociationOfAgentByAssociatedOrg(agentId, associatedOrgId);
    }

    @Override
    public AgentAssociation getAgentAssociationOfAssociatedAgentByOrgId(String associatedAgentId, String orgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, orgId);
    }

    @Override
    public AgentAssociation getAgentAssociation(String agentId, String orgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociation(agentId, orgId);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgent(String actualAgentId, String residentOrgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociationsOfAssociatedAgent(actualAgentId, residentOrgId);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgent(String actualAgentId, String residentOrgId,
                                                                   List<String> orgIdsScope,
                                                                   List<ExpressionNode> expressionNodes,
                                                                   String sortOrder, int limit)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociationsOfAssociatedAgent(actualAgentId, residentOrgId,
                orgIdsScope, expressionNodes, sortOrder, limit);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgent(String actualAgentId, String residentOrgId,
                                                                   SharedType sharedType)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociationsOfAssociatedAgent(actualAgentId, residentOrgId,
                sharedType);
    }

    @Override
    public boolean hasAgentAssociations(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        return organizationAgentSharingDAO.hasAgentAssociations(associatedAgentId, associatedOrgId);
    }

    @Override
    public boolean hasAgentAssociationsInOrganizations(String associatedAgentId, String associatedOrgId,
                                                       List<String> orgIds)
            throws OrganizationManagementServerException {

        return organizationAgentSharingDAO.hasAgentAssociationsInOrganizations(associatedAgentId, associatedOrgId,
                orgIds);
    }

    @Override
    public List<String> getNonDeletableAgentRoleAssignments(String roleId,
                                                            List<String> deletedDomainQualifiedAgentNamesList,
                                                            String tenantDomain, String requestingOrgId)
            throws IdentityRoleManagementException {

        return organizationAgentSharingDAO.getNonDeletableAgentRoleAssignments(roleId,
                deletedDomainQualifiedAgentNamesList, tenantDomain, requestingOrgId);
    }

    @Override
    public List<String> getSharedAgentRolesFromAgentRoles(List<String> allAgentRolesOfSharedAgent,
                                                          String tenantDomain)
            throws IdentityRoleManagementException {

        return organizationAgentSharingDAO.getSharedAgentRolesFromAgentRoles(allAgentRolesOfSharedAgent,
                tenantDomain);
    }

    @Override
    public void addEditRestrictionsForSharedAgentRole(String roleId, String agentName, String tenantDomain,
                                                      String domainName, EditOperation editOperation,
                                                      String permittedOrgId)
            throws AgentSharingMgtException {

        organizationAgentSharingDAO.addEditRestrictionsForSharedAgentRole(roleId, agentName, tenantDomain, domainName,
                editOperation, permittedOrgId);
    }

    @Override
    public List<String> getRolesSharedWithAgentInOrganization(String agentName, int tenantId, String domainName)
            throws AgentSharingMgtException {

        return organizationAgentSharingDAO.getRolesSharedWithAgentInOrganization(agentName, tenantId, domainName);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgentOnGivenOrgs(String associatedAgentId,
                                                                               List<String> orgIds)
            throws OrganizationManagementServerException {

        return organizationAgentSharingDAO.getAgentAssociationsOfGivenAgentOnGivenOrgs(associatedAgentId, orgIds);
    }

    @Override
    public void updateSharedTypeOfAgentAssociation(int id, SharedType sharedType)
            throws OrganizationManagementServerException {

        organizationAgentSharingDAO.updateSharedTypeOfAgentAssociation(id, sharedType);
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationAgentSharingDataHolder.getInstance().getOrganizationManager();
    }

    private String generatePassword() {

        UUID uuid = UUID.randomUUID();
        return uuid.toString().substring(0, 12);
    }

    private void startTenantFlow(String tenantDomain) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
    }

    private void endTenantFlow() {

        PrivilegedCarbonContext.endTenantFlow();
    }
}
