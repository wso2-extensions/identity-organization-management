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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.ConnectionAssociationManager;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.component.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.ConnectionTypeHandler;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util.ConnectionSharingAuditLogger;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util.ConnectionSharingInitiatorContext;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event handler for sharing connections to newly created organizations and unsharing connections owned by
 * organizations being deleted.
 **/
public class OrganizationSharedConnectionHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationSharedConnectionHandler.class);
    private static final ExecutorService CONNECTION_SHARING_EXECUTOR = Executors.newFixedThreadPool(5);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Organization createdOrganization =
                    (Organization) event.getEventProperties().get(Constants.EVENT_PROP_ORGANIZATION);
            if (createdOrganization == null) {
                return;
            }
            shareConnectionsToOrganizationAsync(createdOrganization.getId());
        } else if (Constants.EVENT_POST_DELETE_ORGANIZATION.equals(eventName)) {
            String deletingOrgId = (String) event.getEventProperties().get(Constants.EVENT_PROP_ORGANIZATION_ID);
            if (StringUtils.isBlank(deletingOrgId)) {
                return;
            }
            cleanupSharedConnectionsForOrganizationAsync(deletingOrgId);
        }
    }

    /**
     * Unshares every connection owned by the given organization from all organizations it was shared with. Each
     * distinct owned connection is unshared once via its {@link ConnectionTypeHandler}, which removes the shadow
     * resources in the shared organizations and the corresponding associations.
     */
    private void unshareConnectionsOwnedByOrganization(String residentOrgId) throws ConnectionSharingMgtException {

        List<ConnectionAssociation> associations =
                getConnectionAssociationManager().getConnectionAssociationsByResidentOrg(residentOrgId);
        Set<String> processedConnections = new HashSet<>();
        for (ConnectionAssociation association : associations) {
            ResourceType resourceType = association.getResourceType();
            String connectionId = association.getParentConnectionId();
            // The same connection appears once per shared organization; unshare it from all organizations only once.
            if (!processedConnections.add(resourceType.name() + ":" + connectionId)) {
                continue;
            }
            ConnectionTypeHandler handler = resolveConnectionTypeHandler(resourceType);
            if (handler == null) {
                continue;
            }
            try {
                handler.unshareConnectionFromAllOrgs(connectionId, residentOrgId);
            } catch (ConnectionSharingMgtException e) {
                LOG.error("Error while unsharing connection: " + connectionId + " of type: " + resourceType +
                        " owned by the organization being deleted: " + residentOrgId + ".", e);
            }
        }
    }
    
    private void deleteConnectionsSharedToOrganization(String deletedOrgId) throws ConnectionSharingMgtException {

        List<ConnectionAssociation> associations =
                getConnectionAssociationManager().getConnectionAssociationsBySharedOrg(deletedOrgId);
        for (ConnectionAssociation association : associations) {
            ResourceType resourceType = association.getResourceType();
            ConnectionTypeHandler handler = resolveConnectionTypeHandler(resourceType);
            if (handler == null) {
                continue;
            }
            try {
                handler.unshareConnectionFromOrg(association.getParentConnectionId(), deletedOrgId,
                        association.getConnectionResidentOrganizationId());
            } catch (ConnectionSharingMgtException e) {
                LOG.error("Error while removing connection: " + association.getParentConnectionId() + " of type: " +
                        resourceType + " shared to the organization being deleted: " + deletedOrgId + ".", e);
            }
        }
    }

    private void cleanupSharedConnectionsForOrganizationAsync(String deletingOrgId) {

        ConnectionSharingInitiatorContext initiatorContext = ConnectionSharingInitiatorContext.capture();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
            try {
                initiateThreadLocalContext(initiatorContext, threadLocalProperties);
                unshareConnectionsOwnedByOrganization(deletingOrgId);
                deleteConnectionsSharedToOrganization(deletingOrgId);
                getConnectionAssociationManager().deleteConnectionAssociationsByOrganizationId(deletingOrgId);
            } catch (ConnectionSharingMgtException e) {
                LOG.error("Error while cleaning up connection sharing for the organization being deleted: " +
                        deletingOrgId + ".", e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, CONNECTION_SHARING_EXECUTOR).exceptionally(ex -> {
            LOG.error("Error occurred during async connection sharing cleanup for the deleted organization: " +
                    deletingOrgId + ".", ex);
            return null;
        });
    }

    private void shareConnectionsToOrganizationAsync(String createdOrgId) {

        ConnectionSharingInitiatorContext initiatorContext = ConnectionSharingInitiatorContext.capture();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
            try {
                initiateThreadLocalContext(initiatorContext, threadLocalProperties);
                shareConnectionsToOrganization(createdOrgId);
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                LOG.error("Error while sharing connections to the created organization: " + createdOrgId + ".", e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, CONNECTION_SHARING_EXECUTOR).exceptionally(ex -> {
            LOG.error("Error occurred during async connection sharing to the created organization: " + createdOrgId +
                    ".", ex);
            return null;
        });
    }

    /**
     * Restores the captured initiator Carbon context onto the current (worker) thread within a new tenant flow. The
     * caller is responsible for ending the tenant flow.
     */
    private void initiateThreadLocalContext(ConnectionSharingInitiatorContext initiatorContext,
                                            Map<String, Object> threadLocalProperties) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(initiatorContext.getSharingInitiatedTenantDomain(), true);
        carbonContext.setTenantId(initiatorContext.getSharingInitiatedTenantId());
        carbonContext.setUsername(initiatorContext.getSharingInitiatedUsername());
        IdentityUtil.threadLocalProperties.get().putAll(threadLocalProperties);
    }

    /**
     * Materializes shadow connections in the created organization for every future-applicable connection sharing
     * policy held by an ancestor organization.
     */
    private void shareConnectionsToOrganization(String createdOrgId)
            throws OrganizationManagementException, ResourceSharingPolicyMgtException {

        List<String> ancestorOrganizationIds = getOrganizationManager().getAncestorOrganizationIds(createdOrgId);
        // The first entry is the created organization itself; the rest are its ancestors. A primary organization
        // (or one with no ancestors) has nothing to inherit.
        if (ancestorOrganizationIds == null || ancestorOrganizationIds.size() <= 1) {
            return;
        }
        List<String> ancestorOrgs = ancestorOrganizationIds.subList(1, ancestorOrganizationIds.size());

        Map<String, List<ResourceSharingPolicy>> policiesByHoldingOrg = getResourceSharingPolicyHandlerService()
                .getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(ancestorOrgs);

        for (List<ResourceSharingPolicy> policies : policiesByHoldingOrg.values()) {
            for (ResourceSharingPolicy policy : policies) {
                ResourceType resourceType = policy.getResourceType();
                if (!isFutureApplicablePolicy(policy.getSharingPolicy()) || resourceType == null) {
                    continue;
                }
                ConnectionTypeHandler handler = resolveConnectionTypeHandler(resourceType);
                if (handler == null) {
                    // Not a connection resource type (e.g. USER / APPLICATION) or no handler registered for it.
                    continue;
                }
                try {
                    String parentOrgId = ancestorOrgs.getFirst();
                    if (isConnectionSharedWithParent(resourceType, policy, parentOrgId)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Skipping sharing connection: " + policy.getResourceId() + " of type: " +
                                    policy.getResourceType() + " from the holding organization: " +
                                    policy.getInitiatingOrgId() + " to the created organization: " + createdOrgId +
                                    " because it is not shared with the parent organization: " + parentOrgId +
                                    " (the first ancestor).");
                        }
                        ConnectionSharingAuditLogger.logConnectionShareFailure(resourceType.name(),
                                policy.getResourceId(), policy.getInitiatingOrgId(), createdOrgId,
                                "Connection is not shared with the immediate parent organization: " + parentOrgId);
                        continue;
                    }
                    handler.shareConnectionToOrg(policy.getResourceId(), createdOrgId, policy.getSharingPolicy(),
                            policy.getInitiatingOrgId());
                } catch (ConnectionSharingMgtException e) {
                    LOG.error("Error while sharing connection: " + policy.getResourceId() + " of type: " +
                            policy.getResourceType() + " to the created organization: " + createdOrgId + ".", e);
                }
            }
        }
    }

    private boolean isConnectionSharedWithParent(ResourceType resourceType, ResourceSharingPolicy policy,
                                                 String parentOrgId) throws ConnectionSharingMgtException {

        // If the immediate parent is the resource holding organization, the parent connection is the original
        // connection itself.
        if (StringUtils.equals(parentOrgId, policy.getInitiatingOrgId())) {
            return true;
        }

        return getConnectionAssociationManager().getSharedConnectionId(resourceType.name(),
                        policy.getResourceId(), policy.getInitiatingOrgId(), parentOrgId).isPresent();

    }

    private ConnectionTypeHandler resolveConnectionTypeHandler(ResourceType resourceType) {

        return ConnectionSharingDataHolder.getInstance().getConnectionTypeHandler(resourceType);
    }

    private boolean isFutureApplicablePolicy(PolicyEnum policy) {

        return PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.equals(policy) ||
                PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.equals(policy);
    }

    private OrganizationManager getOrganizationManager() {

        return ConnectionSharingDataHolder.getInstance().getOrganizationManager();
    }

    private ConnectionAssociationManager getConnectionAssociationManager() {

        return ConnectionSharingDataHolder.getInstance().getConnectionAssociationManager();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return ConnectionSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }
}
