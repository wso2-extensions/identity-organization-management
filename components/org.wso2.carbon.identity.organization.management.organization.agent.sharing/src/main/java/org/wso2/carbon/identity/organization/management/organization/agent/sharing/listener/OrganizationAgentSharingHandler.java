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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.OrganizationAgentSharingService;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal.OrganizationAgentSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The event handler for sharing agents to newly created organizations and cleaning up agent sharing associations
 * when an organization is deleted.
 */
public class OrganizationAgentSharingHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationAgentSharingHandler.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * Handles agent sharing for newly created organizations and cleanup for deleted organizations.
     *
     * @param event The event to be handled.
     * @throws IdentityEventException If an error occurs while handling the event.
     */
    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Organization createdOrganization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            if (createdOrganization == null) {
                return;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Handling agent sharing for created organization: " + createdOrganization.getId());
            }
            try {
                if (!OrganizationManagementUtil.isOrganization(createdOrganization.getOrganizationHandle())) {
                    return;
                }
            } catch (OrganizationManagementException e) {
                throw new IdentityEventException("Error while determining organization type for created " +
                        "organization: " + createdOrganization.getId() + ".", e);
            }
            // Capture calling tenant domain before dispatching to the background thread since
            // CarbonContext is thread-local and won't be available on the executor thread.
            String callerTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String createdOrgId = createdOrganization.getId();
            executorService.submit(() -> {
                PrivilegedCarbonContext.startTenantFlow();
                try {
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(callerTenantDomain, true);
                    shareAgents(createdOrgId);
                } catch (OrganizationManagementException e) {
                    LOG.error("Error while sharing agents to created organization: " + createdOrgId + ".", e);
                } catch (ResourceSharingPolicyMgtException e) {
                    LOG.error("Error while retrieving resource sharing policies for created organization: "
                            + createdOrgId + ".", e);
                } catch (IdentityRoleManagementException e) {
                    LOG.error("Error while assigning roles to shared agents for created organization: "
                            + createdOrgId + ".", e);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            });
        }

        if (Constants.EVENT_POST_DELETE_ORGANIZATION.equals(eventName)) {
            String deletedOrgId = (String) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID);
            try {
                if (StringUtils.isNotBlank(deletedOrgId)) {
                    cleanupAgentAssociations(deletedOrgId);
                }
            } catch (OrganizationManagementException e) {
                throw new IdentityEventException("Error while cleaning up agent associations for deleted " +
                        "organization: " + deletedOrgId, e);
            }
        }
    }

    /**
     * Returns the priority of this handler, defaulting to 15 when the super implementation returns -1.
     *
     * @param messageContext The message context.
     * @return The handler priority.
     */
    @Override
    public int getPriority(MessageContext messageContext) {

        int priority = super.getPriority(messageContext);
        if (priority == -1) {
            priority = 16;
        }
        return priority;
    }

    /**
     * Shares agents that have a future-applicable sharing policy to the newly created organization.
     *
     * @param createdOrgId The ID of the newly created organization.
     * @throws OrganizationManagementException    If an error occurs while managing organizations.
     * @throws ResourceSharingPolicyMgtException  If an error occurs while retrieving sharing policies.
         * @throws IdentityRoleManagementException    If an error occurs while managing role assignments.
     */
    private void shareAgents(String createdOrgId)
             throws OrganizationManagementException, ResourceSharingPolicyMgtException,
             IdentityRoleManagementException {

        List<String> allAncestorOrgs = getOrganizationManager().getAncestorOrganizationIds(createdOrgId);
        int topHierarchyLevel = Utils.getSubOrgStartLevel() >= 1 ? Utils.getSubOrgStartLevel() - 1 : 0;
        int startIndex = 1;
        int safeEndIndex = allAncestorOrgs.size() - topHierarchyLevel;
        List<String> relevantAncestorOrgs;
        if (safeEndIndex > startIndex && safeEndIndex <= allAncestorOrgs.size()) {
            relevantAncestorOrgs = new ArrayList<>(allAncestorOrgs.subList(startIndex, safeEndIndex));
        } else {
            relevantAncestorOrgs = new ArrayList<>();
        }

        // Get agent resources of each ancestor organization.
        List<ResourceSharingPolicy> agentResources =
                getResourceSharingPolicyHandlerService().getResourceSharingPoliciesByResourceType(
                        relevantAncestorOrgs, ResourceType.AGENT.name());

        for (ResourceSharingPolicy resource : agentResources) {
            if (shouldShareAgent(resource.getSharingPolicy())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sharing agent: " + resource.getResourceId() + " from organization: " +
                            resource.getInitiatingOrgId() + " to organization: " + createdOrgId);
                }
                shareAgent(resource, resource.getResourceId(), resource.getInitiatingOrgId(), createdOrgId);
            }
        }
    }

    /**
     * Determines whether an agent should be shared to the newly created organization based on the sharing policy.
     * Only future-applicable general policies are supported for agents: {@code ALL_EXISTING_AND_FUTURE_ORGS} and
     * {@code SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN}.
     *
     * @param policy        The sharing policy defined for the agent.
     * @return True if the agent should be shared; false otherwise.
     */
    private boolean shouldShareAgent(PolicyEnum policy) {

        return PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.equals(policy) ||
                PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.equals(policy);
    }

    /**
     * Shares a single agent to the newly created organization if no existing association is found.
     *
     * @param associatedAgentId The ID of the agent to be shared.
     * @param residentOrgId     The ID of the organization where the agent is managed.
     * @param createdOrgId      The ID of the newly created organization.
     * @throws OrganizationManagementException If an error occurs while sharing the agent.
     */
        private void shareAgent(ResourceSharingPolicy resource, String associatedAgentId, String residentOrgId,
                    String createdOrgId)
            throws OrganizationManagementException, ResourceSharingPolicyMgtException,
            IdentityRoleManagementException {

        List<SharedResourceAttribute> agentAttributes =
            getResourceSharingPolicyHandlerService()
                .getSharedResourceAttributesBySharingPolicyId(resource.getResourceSharingPolicyId());

        List<String> roleIds = agentAttributes.stream()
            .filter(attribute -> SharedAttributeType.ROLE.equals(attribute.getSharedAttributeType()))
            .map(SharedResourceAttribute::getSharedAttributeId)
            .collect(Collectors.toList());

        AgentAssociation existingAssociation =
                getOrganizationAgentSharingService()
                        .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, createdOrgId);
        AgentAssociation sharedAssociation = existingAssociation;
        if (existingAssociation == null) {
            getOrganizationAgentSharingService().shareOrganizationAgent(
                    createdOrgId, associatedAgentId, residentOrgId, SharedType.SHARED);
            sharedAssociation = getOrganizationAgentSharingService()
                .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, createdOrgId);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Agent: " + associatedAgentId + " is already shared with the organization: " + createdOrgId);
        }

        if (sharedAssociation == null || roleIds.isEmpty()) {
            return;
        }

        String createdOrgTenantDomain = getOrganizationManager().resolveTenantDomain(createdOrgId);
        Map<String, String> mainRoleToSharedRoleMappings = getRoleManagementService()
            .getMainRoleToSharedRoleMappingsBySubOrg(roleIds, createdOrgTenantDomain);
        List<String> sharedRoleIds = new ArrayList<>(mainRoleToSharedRoleMappings.values());
        List<String> sharedAgentExistingRoles =
            getRoleManagementService().getRoleIdListOfUser(sharedAssociation.getAgentId(), createdOrgTenantDomain);
        sharedRoleIds.removeIf(sharedAgentExistingRoles::contains);

        for (String sharedRoleId : sharedRoleIds) {
            getRoleManagementService().updateUserListOfRole(sharedRoleId,
                Collections.singletonList(sharedAssociation.getAgentId()),
                Collections.emptyList(), createdOrgTenantDomain);
            restrictAgentRoleDeletion(sharedRoleId, sharedAssociation, createdOrgTenantDomain,
                resource.getInitiatingOrgId());
        }
    }

        private void restrictAgentRoleDeletion(String roleId, AgentAssociation sharedAssociation, String tenantDomain,
                           String permittedOrgId) {

        try {
            String domainName = IdentityUtil.getAgentIdentityUserstoreName();
            getOrganizationAgentSharingService().addEditRestrictionsForSharedAgentRole(roleId,
                sharedAssociation.getAgentId(), tenantDomain, domainName, EditOperation.DELETE, permittedOrgId);
        } catch (AgentSharingMgtException e) {
            LOG.error("Error while adding edit restrictions for shared agent role deletion.", e);
        }
        }

    private void cleanupAgentAssociations(String deletedOrgId) throws OrganizationManagementException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Cleaning up agent associations for deleted organization: " + deletedOrgId);
        }
        getOrganizationAgentSharingService().deleteAgentAssociationsByOrganizationId(deletedOrgId);
    }

    private OrganizationAgentSharingService getOrganizationAgentSharingService() {

        return OrganizationAgentSharingDataHolder.getInstance().getOrganizationAgentSharingService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationAgentSharingDataHolder.getInstance().getOrganizationManager();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationAgentSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationAgentSharingDataHolder.getInstance().getRoleManagementService();
    }
}
