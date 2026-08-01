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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.idp.resolver;

import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

import java.util.List;

/**
 * Strategy for the shared-connection handling of a <b>single</b> outbound provisioning connector.
 *
 * <p>Mirrors {@link SharedFederatedAuthenticatorResolver} for outbound provisioning connectors: an IDP may carry
 * several connectors (alongside several federated authenticators), so each is resolved independently by the
 * resolver registered under its {@link #getConnectorName() name}, and the whole shadow is assembled from those
 * per-connector (and per-authenticator) results. By default nothing is stored for a connector on the shadow — it is
 * resolved entirely from the parent at fetch time; a resolver may in future store a few overridable attributes and
 * have {@link #resolveConnector} merge them with the remaining values from the parent. Implementations are
 * contributed via the OSGi whiteboard and keyed by the connector name they handle.</p>
 */
public interface SharedProvisioningConnectorResolver {

    /**
     * The outbound provisioning connector name this resolver handles. Used to register the resolver in the
     * per-connector registry; a resolver is selected when this equals the (parent) connector's name.
     *
     * @return The provisioning connector name; must be non-{@code null} for a contributed resolver.
     */
    String getConnectorName();

    /**
     * Resolves an outbound provisioning connector for a fetch of the shadow identity provider. The result is
     * assembled from the parent's connector and whatever (if anything) the sub-organization stored locally on the
     * shadow:
     * <ul>
     *   <li>{@code resolveWithParent = false} (management view) — the basic/identity attributes (name, enabled),
     *       plus any locally-stored overridable values.</li>
     *   <li>{@code resolveWithParent = true} (runtime view) — the parent's full configuration, with the remaining
     *       (non-stored) values inherited from the parent and any locally-stored overridable values preserved.</li>
     * </ul>
     *
     * @param parentConnector The parent's provisioning connector (the source of inherited values).
     * @param shadowConnector The connector stored locally on the shadow; {@code null} when nothing is stored (the
     *                        default).
     * @param resolveWithParent {@code true} for the runtime view, {@code false} for the management view.
     * @return The resolved provisioning connector.
     */
    ProvisioningConnectorConfig resolveConnector(ProvisioningConnectorConfig parentConnector,
                                                 ProvisioningConnectorConfig shadowConnector,
                                                 boolean resolveWithParent);

    /**
     * Returns the inherited (non-overridable) attributes of this connector that an incoming update would change
     * relative to the stored shadow — i.e. the modifications a sub-organization is not allowed to make. An empty
     * list means the update touches only locally-overridable attributes and is allowed.
     *
     * @param incomingConnector The incoming (to-be-persisted) connector.
     * @param storedConnector   The shadow's current stored connector (the baseline).
     * @return The display names of the restricted attributes that were modified; empty if none.
     */
    List<String> getRestrictedModifications(ProvisioningConnectorConfig incomingConnector,
                                            ProvisioningConnectorConfig storedConnector);

    /**
     * Performs pre-update validation for the provisioning connector update. Throws an exception if the update is not
     * allowed.
     *
     * @param updatingConnector The incoming (to-be-persisted) connector.
     * @param storedConnector   The shadow's current stored connector (the baseline).
     * @param parentIdp         The parent identity provider.
     * @throws IdentityProviderManagementException If the update is not allowed.
     */
    void doPreUpdateValidation(ProvisioningConnectorConfig updatingConnector,
                               ProvisioningConnectorConfig storedConnector, IdentityProvider parentIdp)
            throws IdentityProviderManagementException;
}
