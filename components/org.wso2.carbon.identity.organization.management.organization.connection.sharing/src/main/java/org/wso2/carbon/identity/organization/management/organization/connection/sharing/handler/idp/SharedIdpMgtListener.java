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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp;

import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdPGroup;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.UserDefinedFederatedAuthenticatorConfig;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.ConnectionAssociationManager;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver.SharedIdpResolver;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.util.ConnectionSharingUtil;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.idp.mgt.listener.AbstractIdentityProviderMgtListener;
import org.wso2.carbon.idp.mgt.model.SharedIdPResolveType;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.ErrorMessage.ERROR_CODE_SHARED_IDP_DIRECT_CREATION;
import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.ErrorMessage.ERROR_CODE_SHARED_IDP_DIRECT_DELETION;
import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.ErrorMessage.ERROR_CODE_SHARED_PARENT_IDP_DELETION;

/**
 * {@link org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtListener} implementation that resolves shared (shadow)
 * identity providers at read time. A shadow identity provider stores only minimal local state (name, enabled flag
 * and the {@code isShared} marker); its full configuration lives on the parent identity provider in the resident
 * organization.
 *
 * <p>On {@code getIdPByResourceId}, this listener detects a shadow identity provider, resolves it back to its parent
 * via the {@code IDN_ORG_CONNECTION_ASSOCIATION} table, and overlays the parent's configuration onto the returned
 * object while preserving the shadow's locally-owned state (resource ID, internal ID, name, enabled flag and the
 * sharing markers). The result is the parent's configuration with the sub-organization's local edits applied.</p>
 *
 * <p>On {@code updateIdPByResourceId}, this listener enforces a <b>deny-by-default</b> policy: only the
 * locally-owned identity and the locally-overridable sections may be changed on a shadow; any modification to a
 * parent-inherited section is rejected with a client error. The read-time overlays and the write-time restriction
 * rules are owned by the {@link SharedIdpResolver}, which resolves each authenticator/connector through its
 * per-resource resolver.</p>
 *
 * <p>On {@code addIdP} and {@code deleteIdPByResourceId}, this listener is a guard rail that prevents creating or
 * deleting a shared (shadow) identity provider directly (e.g. via the REST API). A shadow may only be created by
 * the connection sharing process and deleted by the connection unsharing process, both of which mark their flow
 * via {@link ConnectionSharingUtil}; any other attempt (an add carrying the {@code isSharedConnection} marker, or
 * a delete of a resource that has a connection association) is rejected.</p>
 */
public class SharedIdpMgtListener extends AbstractIdentityProviderMgtListener {

    private static final Log LOG = LogFactory.getLog(SharedIdpMgtListener.class);
    private static final int DEFAULT_LISTENER_ORDER = 301;

    // Propagates parent connection changes (name / idp groups) to shadow connections off the parent-update
    // request thread.
    private static final ExecutorService SHADOW_SYNC_EXECUTOR = Executors.newFixedThreadPool(5);

    private static final Gson GSON = new Gson();

    @Override
    public int getDefaultOrderId() {

        return DEFAULT_LISTENER_ORDER;
    }

    @Override
    public boolean doPreAddIdP(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityProviderManagementException {

        // Guard rail: a shared idp may only be created by the connection sharing process
        if (identityProvider != null && isSharedConnection(identityProvider)
                && !ConnectionSharingUtil.isConnectionShareFlow()) {
            throw new IdentityProviderManagementClientException(
                    ERROR_CODE_SHARED_IDP_DIRECT_CREATION.getCode(),
                    ERROR_CODE_SHARED_IDP_DIRECT_CREATION.getMessage());
        }
        return true;
    }

    @Override
    public boolean doPreUpdateIdP(String oldIdPName, IdentityProvider updatingIdp, String tenantDomain)
            throws IdentityProviderManagementException {

        IdentityProvider existingIdp = getIdpManager().getIdPByName(oldIdPName, tenantDomain, true,
                SharedIdPResolveType.RAW);
        return doPreUpdateIdPByResourceId(existingIdp.getResourceId(), updatingIdp, tenantDomain);
    }

    @Override
    public boolean doPreUpdateIdPByResourceId(String resourceId, IdentityProvider updatingIdp, String tenantDomain)
            throws IdentityProviderManagementException {

        if (updatingIdp == null) {
            return true;
        }
        // An internal sync propagation updates a shadow's name / idp groups on behalf of the parent; allow it (the
        // shadow is fetched raw, so no parent-derived values are round-tripped into its row).
        if (ConnectionSharingUtil.isSharedConnectionSyncFlow()) {
            return true;
        }

        IdentityProvider existingIdp = getIdpManager().getIdPByResourceId(resourceId, tenantDomain, true,
                SharedIdPResolveType.RAW);
        if (existingIdp == null) {
            return true;
        }
        if (!isSharedConnection(existingIdp)) {
            // A (potential) parent connection update: record whether its name, idp groups, federated authenticators
            // or provisioning connectors are changing so the post-update hook can propagate them to its shared
            // connections.
            initializeIdpNameUpdatingThreadLocal(updatingIdp.getIdentityProviderName(),
                    existingIdp.getIdentityProviderName());
            initializeIdpGroupUpdatingThreadLocal(updatingIdp.getIdPGroupConfig(), existingIdp.getIdPGroupConfig());
            initializeIdpAuthenticatorsUpdatingThreadLocal(updatingIdp, existingIdp);
            initializeIdpProvisioningConnectorsUpdatingThreadLocal(updatingIdp, existingIdp);
            return true;
        }

        IdentityProvider parentIdp;
        try {
            Optional<ConnectionAssociation> association = getConnectionAssociationManager()
                    .getConnectionAssociationBySharedConnectionId(ResourceType.CONNECTION_IDENTITY_PROVIDER.name(),
                            resourceId);
            if (association.isEmpty()) {
                throw new IdentityProviderManagementException(
                        "Shared identity provider: " + resourceId + " has no connection association.");
            }
            String parentTenantDomain = getOrganizationManager()
                    .resolveTenantDomain(association.get().getConnectionResidentOrganizationId());

            parentIdp = getIdpManager().getIdPByResourceId(association.get().getParentConnectionId(),
                    parentTenantDomain, true, SharedIdPResolveType.FULL_RESOLVED);
        } catch (ConnectionSharingMgtException | OrganizationManagementException e) {
            throw new IdentityProviderManagementException("Error while resolving parent idp of the shared idp: "
                    + resourceId, e);
        }

        SharedIdpResolver.getInstance().doPreUpdateValidations(updatingIdp, existingIdp, parentIdp);
        return true;
    }

    @Override
    public boolean doPreDeleteIdPByResourceId(String resourceId, String tenantDomain)
            throws IdentityProviderManagementException {

        // Guard rail: a shared idp may only be deleted by the connection unsharing process
        if (ConnectionSharingUtil.isConnectionUnshareFlow()) {
            return true;
        }
        try {
            boolean isShadow = getConnectionAssociationManager()
                    .getConnectionAssociationBySharedConnectionId(ResourceType.CONNECTION_IDENTITY_PROVIDER.name(),
                            resourceId).isPresent();
            if (isShadow) {
                throw new IdentityProviderManagementClientException(
                        ERROR_CODE_SHARED_IDP_DIRECT_DELETION.getCode(),
                        ERROR_CODE_SHARED_IDP_DIRECT_DELETION.getMessage());
            }

            // A parent connection that has been shared with organizations cannot be deleted while its shared
            // connections still exist; it must be unshared from all organizations first.
            String residentOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<ConnectionAssociation> sharedConnections = getConnectionAssociationManager().getConnectionAssociations(
                    ResourceType.CONNECTION_IDENTITY_PROVIDER.name(), resourceId, residentOrgId);
            if (CollectionUtils.isNotEmpty(sharedConnections)) {
                throw new IdentityProviderManagementClientException(
                        ERROR_CODE_SHARED_PARENT_IDP_DELETION.getCode(),
                        ERROR_CODE_SHARED_PARENT_IDP_DELETION.getMessage());
            }
        } catch (ConnectionSharingMgtException | OrganizationManagementException e) {
            throw new IdentityProviderManagementException("Error while checking whether identity provider: "
                    + resourceId + " has shared connections.", e);
        }
        return true;
    }

    @Override
    public IdentityProvider doPostGetIdPByResourceId(String resourceId, IdentityProvider identityProvider,
                                                     String tenantDomain, SharedIdPResolveType resolveType)
            throws IdentityProviderManagementException {

        return resolveSharedIdp(identityProvider, tenantDomain, resolveType);
    }

    @Override
    public IdentityProvider doPostGetIdPByName(String idPName, IdentityProvider identityProvider, String tenantDomain,
                                               SharedIdPResolveType resolveType)
            throws IdentityProviderManagementException {

        return resolveSharedIdp(identityProvider, tenantDomain, resolveType);
    }

    @Override
    public IdentityProvider doPostGetIdPById(String id, IdentityProvider identityProvider, String tenantDomain,
                                             SharedIdPResolveType resolveType)
            throws IdentityProviderManagementException {

        return resolveSharedIdp(identityProvider, tenantDomain, resolveType);
    }

    @Override
    public List<IdentityProvider> doPostGetIdPs(List<IdentityProvider> identityProviders, String tenantDomain,
                                                List<String> requiredAttributes, SharedIdPResolveType resolveType)
            throws IdentityProviderManagementException {

        if (identityProviders == null) {
            return null;
        }
        // Each shadow in the list is resolved against its parent and replaced by the resolved copy; non-shadow
        // entries are returned as-is by resolveSharedIdp.
        for (int i = 0; i < identityProviders.size(); i++) {
            identityProviders.set(i, resolveSharedIdp(identityProviders.get(i), tenantDomain, resolveType));
        }
        return identityProviders;
    }

    /**
     * Resolves a shadow identity provider against its parent and returns the resolved copy; a non-shadow identity
     * provider is returned unchanged. The overlay (when any) is applied to a <b>clone</b> — never the supplied
     * (cached) instance — so the shared IdP caches are never mutated. The depth is selected by {@code resolveType}:
     * <ul>
     *   <li>{@link SharedIdPResolveType#RAW} — the stored shadow is returned as-is (no overlay). This is the true
     *       persisted state, used by the update flow and the name-sync propagation, so parent-derived values are
     *       never round-tripped into the shadow's row.</li>
     *   <li>{@link SharedIdPResolveType#BASE_RESOLVED} (management view) — only the always-parent-derived attributes
     *       are overlaid (image URL, description, effective enabled state and the per-resource basic identities via
     *       {@code SharedIdpResolver.overlayBasicParentAttributes}); everything else stays as the raw stored
     *       shadow.</li>
     *   <li>{@link SharedIdPResolveType#FULL_RESOLVED} (runtime engagement view) — the parent's full configuration is
     *       overlaid, preserving the locally-owned identity and the locally-overridable sections.</li>
     * </ul>
     * The overlay rules are owned by the {@link SharedIdpResolver}.
     *
     * @param identityProvider The identity provider to resolve (maybe a shadow or a non-shadow).
     * @param tenantDomain     The tenant domain of the identity provider.
     * @param resolveType      The resolution depth to apply.
     * @return The resolved identity provider.
     * @throws IdentityProviderManagementException If an error occurs while resolving the parent identity provider
     */
    private IdentityProvider resolveSharedIdp(IdentityProvider identityProvider, String tenantDomain,
                                              SharedIdPResolveType resolveType)
            throws IdentityProviderManagementException {

        if (identityProvider == null || !isSharedConnection(identityProvider)
                || resolveType == SharedIdPResolveType.RAW) {
            // RAW: return the stored shadow untouched (no parent overlay).
            return identityProvider;
        }

        String sharedResourceId = identityProvider.getResourceId();
        try {
            IdentityProvider parentIdp = getParentIdp(identityProvider, tenantDomain).orElse(null);
            if (parentIdp == null) {
                return identityProvider;
            }
            // Overlay onto a clone, never the supplied (cached) instance — otherwise the resolution would corrupt
            // the cached shadow and that parent configuration would be round-tripped into its DB row on update.
            // The resolver owns the overlay rules and resolves each authenticator/connector per-resource.
            IdentityProvider resolvedIdp = cloneIdentityProvider(identityProvider);
            switch (resolveType) {
                case BASE_RESOLVED:
                    SharedIdpResolver.getInstance()
                            .overlayBasicParentAttributes(parentIdp, resolvedIdp, tenantDomain);
                    break;
                case FULL_RESOLVED:
                    SharedIdpResolver.getInstance()
                            .overlayParentConfiguration(parentIdp, resolvedIdp, tenantDomain);
                    break;
                default:
                    throw new IdentityProviderManagementException(
                            "Unsupported shared identity provider resolve type: " + resolveType);
            }
            return resolvedIdp;
        } catch (ConnectionSharingMgtException | OrganizationManagementException e) {
            throw new IdentityProviderManagementException(
                    "Error while resolving shared identity provider: " + sharedResourceId, e);
        }
    }

    @Override
    public boolean doPostUpdateIdPByResourceId(String resourceId, IdentityProvider oldIdentityProvider,
                                               IdentityProvider newIdentityProvider, String tenantDomain)
            throws IdentityProviderManagementException {

        // The name / idp-group / authenticator / connector changes were already detected in the pre-update listener;
        // act only if any was. All markers are consumed unconditionally.
        boolean nameUpdated = ConnectionSharingUtil.consumeConnectionNameUpdated();
        boolean groupsUpdated = ConnectionSharingUtil.consumeConnectionGroupsUpdated();
        boolean authenticatorsUpdated = ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated();
        boolean connectorsUpdated = ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated();
        if ((nameUpdated || groupsUpdated || authenticatorsUpdated || connectorsUpdated)
                && newIdentityProvider != null) {
            syncSharedIdps(resourceId, newIdentityProvider, tenantDomain, authenticatorsUpdated, connectorsUpdated);
        }
        return true;
    }

    @Override
    public boolean doPostUpdateIdP(String oldIdPName, IdentityProvider updatingIdp, String tenantDomain)
            throws IdentityProviderManagementException {

        boolean nameUpdated = ConnectionSharingUtil.consumeConnectionNameUpdated();
        boolean groupsUpdated = ConnectionSharingUtil.consumeConnectionGroupsUpdated();
        boolean authenticatorsUpdated = ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated();
        boolean connectorsUpdated = ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated();
        if ((nameUpdated || groupsUpdated || authenticatorsUpdated || connectorsUpdated) && updatingIdp != null) {
            IdentityProvider existingIdp = getIdpManager().getIdPByName(updatingIdp.getIdentityProviderName(),
                    tenantDomain, true);
            syncSharedIdps(existingIdp.getResourceId(), updatingIdp, tenantDomain, authenticatorsUpdated,
                    connectorsUpdated);
        }
        return true;
    }

    private void initializeIdpNameUpdatingThreadLocal(String updatingIdpName, String existingIdpName) {

        boolean isIdpNameUpdating = updatingIdpName != null && !updatingIdpName.equals(existingIdpName);
        ConnectionSharingUtil.setIsConnectionNameUpdating(isIdpNameUpdating);
    }

    private boolean isIDPGroupsUpdating(IdPGroup[] updatingGroups, IdPGroup[] existingGroups) {

        return !groupNames(updatingGroups).equals(groupNames(existingGroups));
    }

    private void initializeIdpGroupUpdatingThreadLocal(IdPGroup[] updatingGroups, IdPGroup[] existingGroups) {

        boolean isIdpGroupsUpdating = !groupNames(updatingGroups).equals(groupNames(existingGroups));
        ConnectionSharingUtil.setIsConnectionGroupsUpdating(isIdpGroupsUpdating);
    }

    private Set<String> groupNames(IdPGroup[] groups) {

        if (groups == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (IdPGroup group : groups) {
            if (group != null && StringUtils.isNotBlank(group.getIdpGroupName())) {
                names.add(group.getIdpGroupName());
            }
        }
        return names;
    }

    private void initializeIdpAuthenticatorsUpdatingThreadLocal(IdentityProvider updatingIdp,
                                                                IdentityProvider existingIdp) {

        boolean isAuthenticatorsUpdating = !authenticatorNames(updatingIdp.getFederatedAuthenticatorConfigs())
                .equals(authenticatorNames(existingIdp.getFederatedAuthenticatorConfigs()))
                || !StringUtils.equals(defaultAuthenticatorName(updatingIdp), defaultAuthenticatorName(existingIdp));
        ConnectionSharingUtil.setIsConnectionAuthenticatorsUpdating(isAuthenticatorsUpdating);
    }

    private void initializeIdpProvisioningConnectorsUpdatingThreadLocal(IdentityProvider updatingIdp,
                                                                        IdentityProvider existingIdp) {

        boolean isConnectorsUpdating = !connectorNames(updatingIdp.getProvisioningConnectorConfigs())
                .equals(connectorNames(existingIdp.getProvisioningConnectorConfigs()))
                || !StringUtils.equals(defaultConnectorName(updatingIdp), defaultConnectorName(existingIdp));
        ConnectionSharingUtil.setIsConnectionProvisioningConnectorsUpdating(isConnectorsUpdating);
    }

    private Set<String> authenticatorNames(FederatedAuthenticatorConfig[] authenticators) {

        if (authenticators == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (FederatedAuthenticatorConfig authenticator : authenticators) {
            if (authenticator != null && StringUtils.isNotBlank(authenticator.getName())) {
                names.add(authenticator.getName());
            }
        }
        return names;
    }

    private String defaultAuthenticatorName(IdentityProvider identityProvider) {

        FederatedAuthenticatorConfig defaultAuthenticator = identityProvider.getDefaultAuthenticatorConfig();
        return defaultAuthenticator != null ? defaultAuthenticator.getName() : null;
    }

    private Set<String> connectorNames(ProvisioningConnectorConfig[] connectors) {

        if (connectors == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (ProvisioningConnectorConfig connector : connectors) {
            if (connector != null && StringUtils.isNotBlank(connector.getName())) {
                names.add(connector.getName());
            }
        }
        return names;
    }

    private String defaultConnectorName(IdentityProvider identityProvider) {

        ProvisioningConnectorConfig defaultConnector = identityProvider.getDefaultProvisioningConnectorConfig();
        return defaultConnector != null ? defaultConnector.getName() : null;
    }
    
    private void syncSharedIdps(String connectionId, IdentityProvider parentIdp, String tenantDomain,
                                boolean syncAuthenticators, boolean syncConnectors) {

        if (StringUtils.isBlank(connectionId)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String residentOrgId = getOrganizationManager().resolveOrganizationId(tenantDomain);
                List<ConnectionAssociation> associations = getConnectionAssociationManager().getConnectionAssociations(
                        ResourceType.CONNECTION_IDENTITY_PROVIDER.name(), connectionId, residentOrgId);
                for (ConnectionAssociation association : associations) {
                    syncSharedIdp(association, parentIdp, syncAuthenticators, syncConnectors);
                }
            } catch (OrganizationManagementException | ConnectionSharingMgtException e) {
                LOG.error("Error while propagating the update of connection: " + connectionId +
                        " to its shared connections.", e);
            }
        }, SHADOW_SYNC_EXECUTOR);
    }

    private void syncSharedIdp(ConnectionAssociation association, IdentityProvider parentIdp,
                               boolean syncAuthenticators, boolean syncConnectors) {

        String shadowResourceId = association.getSharedConnectionId();
        String sharedOrgId = association.getOrganizationId();
        try {
            String sharedTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrgId);
            PrivilegedCarbonContext.startTenantFlow();
            try {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedTenantDomain, true);
                ConnectionSharingUtil.startSharedConnectionSyncFlow();
                try {
                    // Fetch the raw stub (no parent overlay) so only the synced attributes change and no
                    // parent-derived values are persisted back to the association's row.
                    IdentityProvider sharedIdp = getIdpManager().getIdPByResourceId(shadowResourceId,
                            sharedTenantDomain, true, SharedIdPResolveType.RAW);
                    if (sharedIdp == null) {
                        return;
                    }
                    IdentityProvider shadowIdpUpdate = cloneIdentityProvider(sharedIdp);
                    shadowIdpUpdate.setIdentityProviderName(parentIdp.getIdentityProviderName());
                    shadowIdpUpdate.setIdPGroupConfig(
                            mergeGroups(parentIdp.getIdPGroupConfig(), sharedIdp.getIdPGroupConfig()));
                    if (syncAuthenticators) {
                        syncShadowAuthenticators(shadowIdpUpdate, parentIdp);
                    }
                    if (syncConnectors) {
                        syncShadowConnectors(shadowIdpUpdate, parentIdp);
                    }
                    getIdpManager().updateIdPByResourceId(shadowResourceId, shadowIdpUpdate, sharedTenantDomain);
                } finally {
                    ConnectionSharingUtil.endSharedConnectionSyncFlow();
                }
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (OrganizationManagementException | IdentityProviderManagementException e) {
            LOG.error("Error while propagating the update to shared connection: " + shadowResourceId +
                    " in organization: " + sharedOrgId + ".", e);
        }
    }

    private void syncShadowAuthenticators(IdentityProvider sharedIdp, IdentityProvider parentIdp) {

        FederatedAuthenticatorConfig parentDefaultAuthenticator = parentIdp.getDefaultAuthenticatorConfig();
        FederatedAuthenticatorConfig[] parentAuthenticators = parentIdp.getFederatedAuthenticatorConfigs();
        if (parentAuthenticators == null) {
            sharedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[0]);
            sharedIdp.setDefaultAuthenticatorConfig(null);
            return;
        }

        Map<String, FederatedAuthenticatorConfig> existingSharedIdpAuthenticators =
                Arrays.stream(sharedIdp.getFederatedAuthenticatorConfigs())
                        .collect(Collectors.toMap(FederatedAuthenticatorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));
        List<FederatedAuthenticatorConfig> resolved = new ArrayList<>();
        for (FederatedAuthenticatorConfig parentAuthenticator : parentAuthenticators) {
            if (parentAuthenticator == null || StringUtils.isBlank(parentAuthenticator.getName())) {
                continue;
            }
            // Preserve the existing shared config (with its local overrides); add a name-only config when the parent
            // authenticator is newly added.
            FederatedAuthenticatorConfig config = existingSharedIdpAuthenticators.get(parentAuthenticator.getName());
            if (config == null) {
                config = new FederatedAuthenticatorConfig();
                config.setName(parentAuthenticator.getName());
            }
            resolved.add(config);

            if (StringUtils.equals(parentDefaultAuthenticator.getName(), config.getName())) {
                sharedIdp.setDefaultAuthenticatorConfig(config);
            }
        }
        sharedIdp.setFederatedAuthenticatorConfigs(resolved.toArray(new FederatedAuthenticatorConfig[0]));
    }

    private void syncShadowConnectors(IdentityProvider sharedIdp, IdentityProvider parentIdp) {

        ProvisioningConnectorConfig[] parentConnectors = parentIdp.getProvisioningConnectorConfigs();
        ProvisioningConnectorConfig parentDefaultConnector = parentIdp.getDefaultProvisioningConnectorConfig();
        if (parentConnectors == null) {
            sharedIdp.setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[0]);
            sharedIdp.setDefaultProvisioningConnectorConfig(null);
            return;
        }

        Map<String, ProvisioningConnectorConfig> sharedIdpConnectors =
                Arrays.stream(sharedIdp.getProvisioningConnectorConfigs())
                        .collect(Collectors.toMap(ProvisioningConnectorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));
        List<ProvisioningConnectorConfig> resolved = new ArrayList<>();
        for (ProvisioningConnectorConfig parentConnector : parentConnectors) {
            if (parentConnector == null || StringUtils.isBlank(parentConnector.getName())) {
                continue;
            }
            ProvisioningConnectorConfig connectorConfig = sharedIdpConnectors.get(parentConnector.getName());
            if (connectorConfig == null) {
                connectorConfig = new ProvisioningConnectorConfig();
                connectorConfig.setName(parentConnector.getName());
            }
            resolved.add(connectorConfig);

            if (StringUtils.equals(parentDefaultConnector.getName(), connectorConfig.getName())) {
                sharedIdp.setDefaultProvisioningConnectorConfig(connectorConfig);
            }
        }
        sharedIdp.setProvisioningConnectorConfigs(resolved.toArray(new ProvisioningConnectorConfig[0]));
    }

    private IdPGroup[] mergeGroups(IdPGroup[] parentGroups, IdPGroup[] sharedGroups) {

        if (parentGroups == null) {
            return new IdPGroup[0];
        }
        Map<String, String> shadowGroupIdByName = new HashMap<>();
        if (sharedGroups != null) {
            for (IdPGroup shadowGroup : sharedGroups) {
                if (shadowGroup != null && StringUtils.isNotBlank(shadowGroup.getIdpGroupName())) {
                    shadowGroupIdByName.put(shadowGroup.getIdpGroupName(), shadowGroup.getIdpGroupId());
                }
            }
        }
        List<IdPGroup> mergedGroups = new ArrayList<>();
        for (IdPGroup parentGroup : parentGroups) {
            if (parentGroup == null || StringUtils.isBlank(parentGroup.getIdpGroupName())) {
                continue;
            }
            IdPGroup mergedGroup = new IdPGroup();
            mergedGroup.setIdpGroupName(parentGroup.getIdpGroupName());
            mergedGroup.setIdpGroupId(shadowGroupIdByName.get(parentGroup.getIdpGroupName()));
            mergedGroups.add(mergedGroup);
        }
        return mergedGroups.toArray(new IdPGroup[0]);
    }

    private IdentityProvider cloneIdentityProvider(IdentityProvider identityProvider) {

        IdentityProvider clone = GSON.fromJson(GSON.toJson(identityProvider), IdentityProvider.class);
        FederatedAuthenticatorConfig[] authenticators = identityProvider.getFederatedAuthenticatorConfigs();
        if (authenticators != null && authenticators.length == 1
                && authenticators[0] instanceof UserDefinedFederatedAuthenticatorConfig) {
            UserDefinedFederatedAuthenticatorConfig clonedAuthenticator = GSON.fromJson(
                    GSON.toJson(authenticators[0]), UserDefinedFederatedAuthenticatorConfig.class);
            clone.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{clonedAuthenticator});
        }
        return clone;
    }

    private Optional<IdentityProvider> getParentIdp(IdentityProvider shadowIdp, String tenantDomain)
            throws ConnectionSharingMgtException, OrganizationManagementException, IdentityProviderManagementException {

        String sharedResourceId = shadowIdp.getResourceId();
        Optional<ConnectionAssociation> association = getConnectionAssociationManager()
                .getConnectionAssociationBySharedConnectionId(ResourceType.CONNECTION_IDENTITY_PROVIDER.name(),
                        sharedResourceId);
        if (association.isEmpty()) {
            LOG.warn("No connection association found for shared identity provider: " + sharedResourceId +
                    " in tenant: " + tenantDomain + ".");
            return Optional.empty();
        }

        String residentTenantDomain = getOrganizationManager()
                .resolveTenantDomain(association.get().getConnectionResidentOrganizationId());
        // The parent is a regular identity provider (no sharing marker), so this does not recurse into this
        // listener's shadow-resolution branch.
        IdentityProvider parentIdp = getIdpManager().getIdPByResourceId(association.get().getParentConnectionId(),
                residentTenantDomain, true, SharedIdPResolveType.FULL_RESOLVED);
        if (parentIdp == null) {
            LOG.warn("Parent identity provider: " + association.get().getParentConnectionId() +
                    " could not be resolved for shared identity provider: " + sharedResourceId + ".");
        }
        return Optional.ofNullable(parentIdp);
    }

    /**
     * Returns whether the given identity provider is a shadow (shared) identity provider, based on the presence of
     * the {@code isSharedConnection} marker property.
     */
    private boolean isSharedConnection(IdentityProvider identityProvider) {

        IdentityProviderProperty[] properties = identityProvider.getIdpProperties();
        if (properties == null) {
            return false;
        }
        for (IdentityProviderProperty property : properties) {
            if (IdPManagementConstants.IS_SHARED_IDP_PROPERTY.equals(property.getName())) {
                return Boolean.parseBoolean(property.getValue());
            }
        }
        return false;
    }

    private OrganizationManager getOrganizationManager() {

        return ConnectionSharingDataHolder.getInstance().getOrganizationManager();
    }

    private IdpManager getIdpManager() {

        return ConnectionSharingDataHolder.getInstance().getIdpManager();
    }

    private ConnectionAssociationManager getConnectionAssociationManager() {

        return ConnectionSharingDataHolder.getInstance().getConnectionAssociationManager();
    }

}
