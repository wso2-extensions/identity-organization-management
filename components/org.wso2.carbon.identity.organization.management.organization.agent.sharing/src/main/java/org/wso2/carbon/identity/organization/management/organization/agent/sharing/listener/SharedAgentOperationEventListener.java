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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.OrganizationAgentSharingService;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.OrganizationAgentSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal.OrganizationAgentSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.user.core.UserStoreClientException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.user.core.UserStoreConfigConstants.DOMAIN_NAME;

/**
 * User operation event listener for shared agent management.
 * Handles cleanup of agent sharing associations when an agent is deleted from the userstore.
 */
public class SharedAgentOperationEventListener extends AbstractIdentityUserOperationEventListener {

    private static final Log LOG = LogFactory.getLog(SharedAgentOperationEventListener.class);
    private final OrganizationAgentSharingService organizationAgentSharingService =
            new OrganizationAgentSharingServiceImpl();

    @Override
    public int getExecutionOrderId() {

        return 130;
    }

    /**
     * Handles agent sharing association cleanup before deleting an agent from the userstore.
     *
     * <p>When a master agent is deleted, all its shared copies in sub-organizations
     * are removed. When a shared agent copy is deleted, the corresponding association record is cleaned up,
     * but unauthorized deletions (i.e., deleting a SHARED-type agent from a non-resident organization) are
     * prevented.</p>
     *
     * @param userID           The ID of the agent being deleted.
     * @param userStoreManager The userstore manager for the current tenant.
     * @return True to allow the deletion to proceed, or false to abort it.
     * @throws UserStoreException If an error occurs during the association cleanup.
     */
    @Override
    public boolean doPreDeleteUserWithID(String userID, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        // Only handle agents; skip regular users.
        AbstractUserStoreManager abstractUserStoreManager = (AbstractUserStoreManager) userStoreManager;
        if (abstractUserStoreManager.getRealmConfiguration() == null ||
                !IdentityUtil.getAgentIdentityUserstoreName().equalsIgnoreCase(
                        userStoreManager.getRealmConfiguration().getUserStoreProperty(DOMAIN_NAME))) {
            return true;
        }

        try {
            // Retrieve the managedOrg claim to determine whether this is a shared agent copy or a master agent.
            String associatedOrgId = getAgentManagedOrganizationClaim(abstractUserStoreManager, userID);
            String orgId = getOrganizationId();

            if (associatedOrgId != null) {
                // This is a shared agent copy – clean up only its association record.
                AgentAssociation agentAssociation = getAgentAssociation(userID, orgId);
                SharedType sharedType = agentAssociation != null ? agentAssociation.getSharedType() : null;

                int loginTenantId = IdentityTenantUtil.getLoginTenantId();
                String tenantDomain = getTenantDomain(loginTenantId);
                String requestInitiatedOrg = OrganizationAgentSharingDataHolder.getInstance()
                        .getOrganizationManager()
                        .resolveOrganizationId(tenantDomain);
                boolean isSharedAgentOrOwner = sharedType == SharedType.SHARED || sharedType == SharedType.OWNER;

                // Prevent deletion if the request originates from a sub-org and the agent has a restricted type.
                if (isSharedAgentOrOwner && !StringUtils.equals(requestInitiatedOrg, associatedOrgId)) {
                    throw new UserStoreClientException(
                            ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_AGENT.getDescription(),
                            ERROR_CODE_UNAUTHORIZED_DELETION_OF_SHARED_AGENT.getCode());
                }
                // Remove only the DB association record; the actual userstore entry is already being deleted.
                return organizationAgentSharingService.deleteAgentAssociation(userID, associatedOrgId);
            }
            // This is a master agent – remove all shared copies and their associations.
            boolean result = organizationAgentSharingService.unshareOrganizationAgents(userID, orgId);

            // Clean up all resource sharing policies associated with this master agent.
            deleteAllAgentResourceSharingPolicies(userID);
            return result;
        } catch (OrganizationManagementException e) {
            throw new UserStoreException(e.getMessage(), e.getErrorCode(), e);
        } catch (ResourceSharingPolicyMgtException e) {
            throw new UserStoreException(e.getMessage(), e.getErrorCode(), e);
        }
    }

    /**
     * Retrieves the managed organization claim value for the given agent ID.
     *
     * @param userStoreManager The userstore manager to query.
     * @param agentId          The ID of the agent.
     * @return The managed organization ID, or {@code null} if the claim is not set.
     */
    private String getAgentManagedOrganizationClaim(AbstractUserStoreManager userStoreManager, String agentId) {

        try {
            java.util.Map<String, String> claimsMap = userStoreManager.getUserClaimValuesWithID(
                    agentId, new String[]{CLAIM_MANAGED_ORGANIZATION}, null);
            return claimsMap.get(CLAIM_MANAGED_ORGANIZATION);
        } catch (UserStoreException e) {
            if (LOG.isDebugEnabled()) {
                String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                LOG.debug("ManagedOrg claim is not available in the tenant domain: " + tenantDomain);
            }
            return null;
        }
    }

    /**
     * Retrieves the agent association for a shared agent entry within the current organization.
     *
     * @param sharedAgentId The shared agent's ID.
     * @param orgId         The current organization ID.
     * @return The {@link AgentAssociation}, or {@code null} if not found.
     * @throws OrganizationManagementException If an error occurs while retrieving the association.
     */
    private AgentAssociation getAgentAssociation(String sharedAgentId, String orgId)
            throws OrganizationManagementException {

        return OrganizationAgentSharingDataHolder.getInstance().getOrganizationAgentSharingService()
                .getAgentAssociation(sharedAgentId, orgId);
    }

    /**
     * Deletes all resource sharing policies associated with the given master agent.
     *
     * @param masterAgentId The ID of the master agent whose policies should be removed.
     * @throws ResourceSharingPolicyMgtException If an error occurs while deleting the policies.
     */
    private void deleteAllAgentResourceSharingPolicies(String masterAgentId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService policyService = getResourceSharingPolicyHandlerService();
        if (policyService == null) {
            return;
        }
        policyService.deleteResourceSharingPolicyByResourceTypeAndId(ResourceType.AGENT, masterAgentId);
    }

    /**
     * Returns the {@link ResourceSharingPolicyHandlerService} from the data holder.
     *
     * @return The resource sharing policy handler service, or {@code null} if not registered.
     */
    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationAgentSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }
}
