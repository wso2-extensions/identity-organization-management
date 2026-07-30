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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.ConnectionAssociationManager;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.util.ConnectionSharingAuditLogger;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.util.ConnectionSharingUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_INTERNAL_ERROR;

/**
 * Base {@link ConnectionTypeHandler} that owns all the connection-type-agnostic orchestration: the shared
 * connection association persistence (via {@link ConnectionAssociationManager}), organization scoping and tenant-flow
 * handling. Concrete handlers only implement the type-specific resource operations
 * ({@link #createSharedResource} / {@link #deleteSharedResource}) and declare their {@link #getResourceType()}.
 */
public abstract class AbstractConnectionTypeHandler implements ConnectionTypeHandler {

    private static final Log LOG = LogFactory.getLog(AbstractConnectionTypeHandler.class);

    @Override
    public void shareConnectionToOrg(String connectionId, String targetOrgId, PolicyEnum policy,
                                     String initiatingOrgId) throws ConnectionSharingMgtException {

        String resourceType = getResourceType().name();
        try {
            shareConnection(connectionId, targetOrgId, policy, initiatingOrgId);
        } catch (ConnectionSharingMgtClientException e) {
            ConnectionSharingAuditLogger.logConnectionShareFailure(resourceType, connectionId, initiatingOrgId,
                    targetOrgId, e.getDescription());
        } catch (ConnectionSharingMgtException | OrganizationManagementException e) {
            ConnectionSharingAuditLogger.logConnectionShareFailure(resourceType, connectionId, initiatingOrgId,
                    targetOrgId, ERROR_CODE_INTERNAL_ERROR.getMessage());
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public void shareConnectionToAllOrgs(String connectionId, PolicyEnum policy, String initiatingOrgId)
            throws ConnectionSharingMgtException {

        // All descendant organizations of the connection-owning (initiating) organization.
        shareConnectionToChildOrgs(connectionId, initiatingOrgId, policy, initiatingOrgId);
    }

    @Override
    public void shareConnectionToExistingChildOrgs(String connectionId, String parentOrgId, PolicyEnum policy,
                                                   String initiatingOrgId) throws ConnectionSharingMgtException {

        // All existing descendant organizations of the given parent organization.
        shareConnectionToChildOrgs(connectionId, parentOrgId, policy, initiatingOrgId);
    }

    /**
     * Shares the connection with every existing descendant organization of {@code parentOrgId}, creating shadow
     * resources as required. If sharing to an organization fails, its own descendants are skipped (the failed
     * organization's sub-tree is pruned) while sibling branches continue.
     *
     * @param connectionId    The ID of the parent connection being shared.
     * @param parentOrgId     The organization whose descendants receive the connection.
     * @param policy          The sharing policy applied.
     * @param initiatingOrgId The ID of the organization that owns the parent connection.
     * @throws ConnectionSharingMgtException If an error occurs while propagating the connection.
     */
    private void shareConnectionToChildOrgs(String connectionId, String parentOrgId, PolicyEnum policy,
                                            String initiatingOrgId) throws ConnectionSharingMgtException {

        try {
            Deque<String> pendingOrgIds = new ArrayDeque<>(getAllChildOrgIds(parentOrgId));
            while (!pendingOrgIds.isEmpty()) {
                String targetOrgId = pendingOrgIds.poll();
                try {
                    shareConnection(connectionId, targetOrgId, policy, initiatingOrgId);
                } catch (ConnectionSharingMgtClientException e) {
                    logAndSkipChildOrgIds(pendingOrgIds, connectionId, initiatingOrgId, targetOrgId,
                            e.getDescription() + ". Skipping sharing for its child organizations.");
                } catch (ConnectionSharingMgtException | OrganizationManagementException e) {
                    logAndSkipChildOrgIds(pendingOrgIds, connectionId, initiatingOrgId, targetOrgId,
                            ERROR_CODE_INTERNAL_ERROR.getMessage() + ". Skipping sharing for its child organizations.");
                    throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
                }
            }
        } catch (OrganizationManagementException e) {
            ConnectionSharingAuditLogger.logConnectionShareFailure(getResourceType().name(), connectionId,
                    initiatingOrgId, null,
                    ERROR_CODE_INTERNAL_ERROR.getMessage() + ". Skipping sharing for its child organizations.");
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    private void shareConnection(String connectionId, String targetOrgId, PolicyEnum policy,
                                 String initiatingOrgId) throws ConnectionSharingMgtException,
            OrganizationManagementException {

        String resourceType = getResourceType().name();
        try {
            ConnectionSharingUtil.startConnectionShareFlow();
            // Skip if the connection is already shared with the target organization.
            if (getConnectionAssociationManager().getSharedConnectionId(resourceType, connectionId, initiatingOrgId,
                    targetOrgId).isPresent()) {
                LOG.debug("Connection: " + connectionId + " is already shared with organization: " + targetOrgId +
                        ". Skip sharing.");
                return;
            }
            String residentTenantDomain = getOrganizationManager().resolveTenantDomain(initiatingOrgId);
            String targetTenantDomain = getOrganizationManager().resolveTenantDomain(targetOrgId);

            String sharedResourceId = createSharedResource(connectionId, initiatingOrgId, residentTenantDomain,
                    targetTenantDomain);
            addConnectionAssociation(connectionId, targetOrgId, initiatingOrgId, getResourceType(), sharedResourceId);

            ConnectionSharingAuditLogger.logConnectionShared(resourceType, connectionId, initiatingOrgId,
                    targetOrgId, sharedResourceId);
        } finally {
            ConnectionSharingUtil.endConnectionShareFlow();
        }
    }

    private void addConnectionAssociation(String connectionId, String targetOrgId, String initiatingOrgId,
                                          ResourceType resourceType, String sharedResourceId)
            throws ConnectionSharingMgtServerException {

        ConnectionAssociation association = new ConnectionAssociation.Builder()
                .resourceType(resourceType)
                .parentConnectionId(connectionId)
                .connectionResidentOrganizationId(initiatingOrgId)
                .sharedConnectionId(sharedResourceId)
                .organizationId(targetOrgId)
                .build();
        getConnectionAssociationManager().addConnectionAssociation(association);
    }

    private void logAndSkipChildOrgIds(Deque<String> pendingOrgIds, String connectionId, String initiatingOrgId,
                                       String targetOrgId, String reason) throws OrganizationManagementException {

        ConnectionSharingAuditLogger.logConnectionShareFailure(getResourceType().name(), connectionId, initiatingOrgId,
                targetOrgId, reason);
        List<String> skippingChildOrgIds = getAllChildOrgIds(targetOrgId);
        pendingOrgIds.removeAll(skippingChildOrgIds);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Skipping sharing for child organizations of organization: " + targetOrgId +
                    ". Skipping child organizations: " + skippingChildOrgIds);
        }
    }

    private List<String> getAllChildOrgIds(String organizationId) throws OrganizationManagementException {

        return getOrganizationManager().getChildOrganizationsIds(organizationId, true);

    }

    @Override
    public void unshareConnectionFromOrg(String connectionId, String targetOrgId, String initiatingOrgId)
            throws ConnectionSharingMgtException {

        String resourceType = getResourceType().name();
        Optional<String> sharedConnectionId = getConnectionAssociationManager().getSharedConnectionId(resourceType,
                connectionId, initiatingOrgId, targetOrgId);
        if (sharedConnectionId.isPresent()) {
            unshareSharedConnection(resourceType, connectionId, initiatingOrgId, targetOrgId,
                    sharedConnectionId.get());
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Connection: " + connectionId + " is not shared with organization: " + targetOrgId +
                    ". Skipping unshare.");
        }
        // A connection removed from an organization must also be removed from that organization's descendants.
        unshareConnectionFromChildOrgs(resourceType, connectionId, targetOrgId, initiatingOrgId);
    }

    private void unshareConnectionFromChildOrgs(String resourceType, String connectionId, String targetOrgId,
                                                String initiatingOrgId) throws ConnectionSharingMgtException {

        List<String> childOrgIds;
        try {
            childOrgIds = getOrganizationManager().getChildOrganizationsIds(targetOrgId, true);
        } catch (OrganizationManagementException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
        for (String childOrgId : childOrgIds) {
            Optional<String> childSharedConnectionId = getConnectionAssociationManager().getSharedConnectionId(
                    resourceType, connectionId, initiatingOrgId, childOrgId);
            if (childSharedConnectionId.isEmpty()) {
                LOG.debug("Connection: " + connectionId + " is not shared with organization: " + childOrgId +
                            ". Skipping unshare.");
                continue;
            }
            unshareSharedConnection(resourceType, connectionId, initiatingOrgId, childOrgId,
                    childSharedConnectionId.get());
        }
    }

    @Override
    public void unshareConnectionFromAllOrgs(String connectionId, String initiatingOrgId)
            throws ConnectionSharingMgtException {

        String resourceType = getResourceType().name();
        List<ConnectionAssociation> associations = getConnectionAssociationManager()
                .getConnectionAssociations(resourceType, connectionId, initiatingOrgId);
        for (ConnectionAssociation association : associations) {
            unshareSharedConnection(resourceType, connectionId, initiatingOrgId, association.getOrganizationId(),
                    association.getSharedConnectionId());
        }
    }

    private void unshareSharedConnection(String resourceType, String connectionId, String initiatingOrgId,
                                         String targetOrgId, String sharedConnectionId) {

        try {
            String targetTenantDomain = getOrganizationManager().resolveTenantDomain(targetOrgId);
            ConnectionSharingUtil.startConnectionUnshareFlow();
            try {
                deleteSharedResource(sharedConnectionId, targetTenantDomain);
            } finally {
                ConnectionSharingUtil.endConnectionUnshareFlow();
            }
            getConnectionAssociationManager().deleteConnectionAssociation(resourceType, connectionId, initiatingOrgId,
                    targetOrgId);
            ConnectionSharingAuditLogger.logConnectionUnshared(resourceType, connectionId, initiatingOrgId,
                    targetOrgId, sharedConnectionId);
        } catch (ConnectionSharingMgtClientException e) {
            ConnectionSharingAuditLogger.logConnectionUnshareBlocked(resourceType, connectionId, initiatingOrgId,
                    targetOrgId, sharedConnectionId, e.getMessage());
        } catch (ConnectionSharingMgtException | OrganizationManagementException e) {
            LOG.error("Error occurred while removing the shadow connection: " + sharedConnectionId +
                    " from organization: " + targetOrgId + ".", e);
            ConnectionSharingAuditLogger.logConnectionUnshareFailure(resourceType, connectionId, initiatingOrgId,
                    targetOrgId, sharedConnectionId);
        }
    }

    @Override
    public List<ConnectionAssociation> getConnectionAssociations(String connectionId, String initiatingOrgId,
                                                                 List<String> orgIdsScope,
                                                                 List<ExpressionNode> expressionNodes,
                                                                 String sortOrder, int limit)
            throws OrganizationManagementException {

        if (CollectionUtils.isEmpty(orgIdsScope)) {
            return Collections.emptyList();
        }
        try {
            return getConnectionAssociationManager().getConnectionAssociations(getResourceType().name(), connectionId,
                    initiatingOrgId, orgIdsScope, expressionNodes, sortOrder, limit);
        } catch (ConnectionSharingMgtServerException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        }
    }

    // =========================================================
    // Type-specific operations (implemented per connection type)
    // =========================================================

    /**
     * Creates the shadow resource in the target organization's tenant for the given parent connection.
     *
     * @param connectionId         The ID of the parent connection being shared.
     * @param residentOrgId        The organization ID of the organization that owns the parent connection.
     * @param residentTenantDomain The tenant domain of the organization that owns the parent connection.
     * @param targetTenantDomain   The tenant domain of the organization the connection is being shared with.
     * @return The shadow resource ID (never {@code null}).
     * @throws ConnectionSharingMgtException If the shadow resource could not be created — including when the parent
     *                                       connection cannot be resolved (a server error); a failure to produce a
     *                                       resource ID is always an error, never a silent skip.
     */
    protected abstract String createSharedResource(String connectionId, String residentOrgId,
                                                   String residentTenantDomain, String targetTenantDomain)
            throws ConnectionSharingMgtException;

    /**
     * Deletes the shadow resource from the target organization's tenant.
     *
     * @param sharedResourceId   The shadow resource ID.
     * @param targetTenantDomain The tenant domain of the organization holding the shadow resource.
     * @throws ConnectionSharingMgtException If an error occurs while deleting the shadow resource.
     */
    protected abstract void deleteSharedResource(String sharedResourceId, String targetTenantDomain)
            throws ConnectionSharingMgtException;

    // =========================================================
    // Shared helpers for concrete handlers
    // =========================================================

    /**
     * Executes the given operation within a tenant flow for the supplied tenant domain, restoring the previous
     * flow afterwards. The current authenticated user (if any) is carried into the started flow.
     *
     * @param tenantDomain The tenant domain to switch into.
     * @param operation    The operation to execute.
     * @param <T>          The operation's return type.
     * @return The operation result.
     * @throws ConnectionSharingMgtException If the operation fails.
     */
    protected <T> T runInTenant(String tenantDomain, TenantBoundOperation<T> operation)
            throws ConnectionSharingMgtException {

        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(tenantDomain, true);
            carbonContext.setTenantId(IdentityTenantUtil.getTenantId(tenantDomain));
            if (StringUtils.isNotBlank(username)) {
                carbonContext.setUsername(username);
            }
            return operation.execute();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    protected OrganizationManager getOrganizationManager() {

        return ConnectionSharingDataHolder.getInstance().getOrganizationManager();
    }

    protected ConnectionAssociationManager getConnectionAssociationManager() {

        return ConnectionSharingDataHolder.getInstance().getConnectionAssociationManager();
    }

    /**
     * A tenant-bound operation executed via {@link #runInTenant(String, TenantBoundOperation)}.
     *
     * @param <T> The operation's return type.
     */
    @FunctionalInterface
    protected interface TenantBoundOperation<T> {

        T execute() throws ConnectionSharingMgtException;
    }
}
