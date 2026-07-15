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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal;

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.ConnectionAssociationService;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.ConnectionTypeHandler;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionType;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver.DefaultSharedFederatedAuthenticatorResolver;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver.DefaultSharedProvisioningConnectorResolver;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver.SharedFederatedAuthenticatorResolver;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver.SharedProvisioningConnectorResolver;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data holder for connection sharing management.
 */
public class ConnectionSharingDataHolder {

    private static final ConnectionSharingDataHolder instance = new ConnectionSharingDataHolder();

    private OrganizationManager organizationManager;
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;
    private IdpManager idpManager;
    private ConnectionAssociationService connectionAssociationService;
    private RealmService realmService;
    private final Map<ConnectionType, ConnectionTypeHandler> connectionTypeHandlers =
            new EnumMap<>(ConnectionType.class);
    // Per-resource resolvers contributed via the whiteboard pattern, keyed by the authenticator/connector name they
    // handle. A shadow IDP can carry several authenticators and connectors at once (and may have no templateId), so
    // each resource is resolved independently by name. The defaults are the fallbacks held directly by the data
    // holder, used when no resolver is registered under a resource's name.
    private final Map<String, SharedFederatedAuthenticatorResolver> sharedFederatedAuthenticatorResolvers =
            new ConcurrentHashMap<>();
    private final SharedFederatedAuthenticatorResolver defaultSharedFederatedAuthenticatorResolver =
            new DefaultSharedFederatedAuthenticatorResolver();
    private final Map<String, SharedProvisioningConnectorResolver> sharedProvisioningConnectorResolvers =
            new ConcurrentHashMap<>();
    private final SharedProvisioningConnectorResolver defaultSharedProvisioningConnectorResolver =
            new DefaultSharedProvisioningConnectorResolver();

    private ConnectionSharingDataHolder() {

    }

    public static ConnectionSharingDataHolder getInstance() {

        return instance;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    public ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return resourceSharingPolicyHandlerService;
    }

    public void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        this.resourceSharingPolicyHandlerService = resourceSharingPolicyHandlerService;
    }

    public IdpManager getIdpManager() {

        return idpManager;
    }

    public void setIdpManager(IdpManager idpManager) {

        this.idpManager = idpManager;
    }

    public ConnectionAssociationService getConnectionAssociationService() {

        return connectionAssociationService;
    }

    public void setConnectionAssociationService(ConnectionAssociationService connectionAssociationService) {

        this.connectionAssociationService = connectionAssociationService;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Registers a {@link ConnectionTypeHandler} for the connection type it handles.
     *
     * @param connectionTypeHandler The handler to register.
     */
    public void addConnectionTypeHandler(ConnectionTypeHandler connectionTypeHandler) {

        if (connectionTypeHandler != null && connectionTypeHandler.getConnectionType() != null) {
            connectionTypeHandlers.put(connectionTypeHandler.getConnectionType(), connectionTypeHandler);
        }
    }

    /**
     * Removes a previously registered {@link ConnectionTypeHandler}.
     *
     * @param connectionTypeHandler The handler to remove.
     */
    public void removeConnectionTypeHandler(ConnectionTypeHandler connectionTypeHandler) {

        if (connectionTypeHandler != null && connectionTypeHandler.getConnectionType() != null) {
            connectionTypeHandlers.remove(connectionTypeHandler.getConnectionType(), connectionTypeHandler);
        }
    }

    /**
     * Returns the {@link ConnectionTypeHandler} registered for the given {@link ConnectionType}.
     *
     * @param connectionType The connection type.
     * @return The registered handler, or {@code null} if none is registered.
     */
    public ConnectionTypeHandler getConnectionTypeHandler(ConnectionType connectionType) {

        return connectionType == null ? null : connectionTypeHandlers.get(connectionType);
    }

    public void addSharedFederatedAuthenticatorResolver(SharedFederatedAuthenticatorResolver resolver) {

        if (resolver != null && resolver.getAuthenticatorName() != null) {
            sharedFederatedAuthenticatorResolvers.put(resolver.getAuthenticatorName(), resolver);
        }
    }

    public void removeSharedFederatedAuthenticatorResolver(SharedFederatedAuthenticatorResolver resolver) {

        if (resolver != null && resolver.getAuthenticatorName() != null) {
            sharedFederatedAuthenticatorResolvers.remove(resolver.getAuthenticatorName(), resolver);
        }
    }

    /**
     * Resolves the {@link SharedFederatedAuthenticatorResolver} registered under the given federated authenticator
     * name, or the default resolver when none is registered under that name.
     *
     * @param authenticatorName The (parent) federated authenticator name; may be {@code null}.
     * @return The resolver to use; never {@code null}.
     */
    public SharedFederatedAuthenticatorResolver getSharedFederatedAuthenticatorResolver(String authenticatorName) {

        SharedFederatedAuthenticatorResolver resolver = authenticatorName == null ? null
                : sharedFederatedAuthenticatorResolvers.get(authenticatorName);
        return resolver != null ? resolver : defaultSharedFederatedAuthenticatorResolver;
    }

    public void addSharedProvisioningConnectorResolver(SharedProvisioningConnectorResolver resolver) {

        if (resolver != null && resolver.getConnectorName() != null) {
            sharedProvisioningConnectorResolvers.put(resolver.getConnectorName(), resolver);
        }
    }

    public void removeSharedProvisioningConnectorResolver(SharedProvisioningConnectorResolver resolver) {

        if (resolver != null && resolver.getConnectorName() != null) {
            sharedProvisioningConnectorResolvers.remove(resolver.getConnectorName(), resolver);
        }
    }

    /**
     * Resolves the {@link SharedProvisioningConnectorResolver} registered under the given outbound provisioning
     * connector name, or the default resolver when none is registered under that name.
     *
     * @param connectorName The (parent) provisioning connector name; may be {@code null}.
     * @return The resolver to use; never {@code null}.
     */
    public SharedProvisioningConnectorResolver getSharedProvisioningConnectorResolver(String connectorName) {

        SharedProvisioningConnectorResolver resolver = connectorName == null ? null
                : sharedProvisioningConnectorResolvers.get(connectorName);
        return resolver != null ? resolver : defaultSharedProvisioningConnectorResolver;
    }
}
