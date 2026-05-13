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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.annotation.bundle.Capability;
import org.wso2.carbon.context.PrivilegedCarbonContext;
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
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.scim2.common.utils.SCIMCommonUtils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.DEFAULT_PROFILE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ID_CLAIM_READ_ONLY;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.PROCESS_ADD_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATE_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_SHARED_USER;
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

    private static final Log LOG = LogFactory.getLog(OrganizationAgentSharingServiceImpl.class);
    private static final int AGENT_STORE_WAIT_TIMEOUT_MS = 15000;
    private static final int AGENT_STORE_POLL_INTERVAL_MS = 50;

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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sharing organization agent with orgId: " + orgId + " and associatedOrgId: "
                        + associatedOrgId);
            }
            String sharedOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            startTenantFlow(sharedOrgTenantDomain);
            IdentityUtil.threadLocalProperties.get().put(PROCESS_ADD_SHARED_AGENT, true);
            SCIMCommonUtils.setThreadLocalIsSCIMAgentFlow(true);

            // Set up the claims for the shared agent entry.
            HashMap<String, String> agentClaims = new HashMap<>();
            agentClaims.put(CLAIM_MANAGED_ORGANIZATION, associatedOrgId);
            agentClaims.put(ID_CLAIM_READ_ONLY, "true");
            UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(true);
            UserCoreUtil.setSkipUsernamePatternValidationThreadLocal(true);

            // Add the shared agent entry to the target organization's agent user store.
            int sharedOrgTenantId = IdentityTenantUtil.getTenantId(sharedOrgTenantDomain);
            String domainName = IdentityUtil.getAgentIdentityUserstoreName();
            AbstractUserStoreManager sharedOrgAgentStoreManager =
                    waitForAgentStoreManager(sharedOrgTenantId, domainName, ERROR_CODE_ERROR_CREATE_SHARED_USER, orgId);
            User sharedUser = sharedOrgAgentStoreManager.addUserWithID(
                    associatedAgentId, generatePassword(), null, agentClaims, DEFAULT_PROFILE);

            // Create the agent association record in the database.
            // Agent ID equals agent username, so associatedAgentId is used directly as the shared agent's ID.
            try {
                organizationAgentSharingDAO.createOrganizationAgentAssociation(sharedUser.getUserID(), orgId,
                        associatedAgentId, associatedOrgId, sharedType);
            } catch (OrganizationManagementException e) {
                try {
                    sharedOrgAgentStoreManager.deleteUserWithID(sharedUser.getUserID());
                } catch (UserStoreException deletionError) {
                    LOG.error("Failed to delete provisioned shared agent " + sharedUser.getUserID()
                            + " after association creation failure in org " + orgId + ".", deletionError);
                }
                throw e;
            }
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_SHARED_USER, e, orgId);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(PROCESS_ADD_SHARED_AGENT);
            SCIMCommonUtils.unsetThreadLocalIsSCIMAgentFlow();
            UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(false);
            UserCoreUtil.setSkipUsernamePatternValidationThreadLocal(false);
            endTenantFlow();
        }
    }

    @Override
    public boolean unshareOrganizationAgents(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsharing organization agents for associatedOrgId: " + associatedOrgId);
        }
        List<AgentAssociation> agentAssociationList =
                organizationAgentSharingDAO.getAgentAssociationsOfAssociatedAgent(associatedAgentId, associatedOrgId);
        for (AgentAssociation agentAssociation : agentAssociationList) {
            removeSharedAgent(agentAssociation);
        }
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
        removeSharedAgent(agentAssociation);
        return organizationAgentSharingDAO.deleteAgentAssociationOfAgentByAssociatedOrg(
                agentAssociation.getAgentId(), agentAssociation.getAgentResidentOrganizationId());
    }

    @Override
    public boolean deleteAgentAssociationsByOrganizationId(String orgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.deleteAgentAssociationsByOrganizationId(orgId);
    }

    @Override
    public AgentAssociation getAgentAssociationOfAssociatedAgentByOrgId(String associatedAgentId, String orgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, orgId);
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgent(String actualAgentId, String residentOrgId)
            throws OrganizationManagementException {

        return organizationAgentSharingDAO.getAgentAssociationsOfAssociatedAgent(actualAgentId, residentOrgId);
    }

    @Override
    public boolean hasAgentAssociationsInOrganizations(String associatedAgentId, String associatedOrgId,
                                                       List<String> orgIds)
            throws OrganizationManagementServerException {

        return organizationAgentSharingDAO.hasAgentAssociationsInOrganizations(associatedAgentId, associatedOrgId,
                orgIds);
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

    private void removeSharedAgent(AgentAssociation agentAssociation) throws OrganizationManagementException {

        if (agentAssociation == null) {
            return;
        }
        String agentId = agentAssociation.getAgentId();
        String organizationId = agentAssociation.getOrganizationId();
        String tenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        try {
            String domainName = IdentityUtil.getAgentIdentityUserstoreName();
            AbstractUserStoreManager sharedOrgAgentStoreManager =
                    waitForAgentStoreManager(tenantId, domainName, ERROR_CODE_ERROR_DELETE_SHARED_USER, agentId,
                            organizationId);
            deleteAgentInTenantFlow(sharedOrgAgentStoreManager, agentId, tenantDomain, organizationId);
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_SHARED_USER, e, agentId, organizationId);
        }
    }

    /**
     * Polls for the agent user store manager until it becomes available or the timeout elapses.
     * Uses a ScheduledExecutorService to schedule repeated checks at AGENT_STORE_POLL_INTERVAL_MS intervals
     * so the calling thread waits on a CompletableFuture instead of sleeping in a loop.
     *
     * @param tenantId    The tenant ID of the target organization.
     * @param domainName  The domain name of the agent user store.
     * @param errorCode   The error code to use if the wait fails.
     * @param contextArgs The context arguments for the error message.
     * @return The resolved AbstractUserStoreManager for the given domain.
     * @throws OrganizationManagementException If the store is not available within the timeout or an error occurs.
     */
    private AbstractUserStoreManager waitForAgentStoreManager(int tenantId, String domainName,
            OrganizationManagementConstants.ErrorMessages errorCode, String... contextArgs)
            throws OrganizationManagementException {

        // Resolve tenant domain on the calling thread (which has a valid CarbonContext) so the background
        // scheduler thread can establish its own tenant flow without depending on thread-local context.
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        RealmService realmService = OrganizationAgentSharingDataHolder.getInstance().getRealmService();
        CompletableFuture<AbstractUserStoreManager> future = new CompletableFuture<>();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            long deadline = System.currentTimeMillis() + AGENT_STORE_WAIT_TIMEOUT_MS;
            scheduler.scheduleAtFixedRate(() -> {
                if (future.isDone()) {
                    return;
                }
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                    AbstractUserStoreManager manager = (AbstractUserStoreManager)
                            ((UserStoreManager) realmService.getTenantUserRealm(tenantId).getUserStoreManager())
                                    .getSecondaryUserStoreManager(domainName);
                    if (manager != null) {
                        future.complete(manager);
                    } else if (System.currentTimeMillis() >= deadline) {
                        future.completeExceptionally(
                                new TimeoutException("Agent user store not available within timeout."));
                    }
                } catch (UserStoreException e) {
                    future.completeExceptionally(e);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }, 0, AGENT_STORE_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            return future.get(AGENT_STORE_WAIT_TIMEOUT_MS + AGENT_STORE_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw handleServerException(errorCode, e, contextArgs);
        } catch (TimeoutException e) {
            throw handleServerException(errorCode,
                    new OrganizationManagementServerException(
                            "Agent user store manager not available for domain: " + domainName), contextArgs);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                throw handleServerException(errorCode,
                        new OrganizationManagementServerException(
                                "Agent user store manager not available for domain: " + domainName), contextArgs);
            }
            throw handleServerException(errorCode, cause != null ? cause : e, contextArgs);
        } finally {
            scheduler.shutdownNow();
        }
    }

    private void deleteAgentInTenantFlow(AbstractUserStoreManager agentStoreManager, String agentId,
                                         String tenantDomain, String organizationId) throws UserStoreException {

        try {
            String requestInitiator = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            SCIMCommonUtils.setThreadLocalIsSCIMAgentFlow(true);
            startTenantFlow(tenantDomain);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(organizationId);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(requestInitiator);
            agentStoreManager.deleteUserWithID(agentId);
        } finally {
            SCIMCommonUtils.unsetThreadLocalIsSCIMAgentFlow();
            endTenantFlow();
        }
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
