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

import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

import java.util.List;

/**
 * Strategy for the shared-connection handling of a <b>single</b> federated authenticator.
 *
 * <p>A shared identity provider is resolved per resource, not per IDP template: an IDP may carry several federated
 * authenticators (of different types) alongside several outbound provisioning connectors, and may have no
 * {@code templateId} at all. So each authenticator is resolved independently by the resolver registered under its
 * {@link #getAuthenticatorName() name}, and the whole shadow is assembled from those per-authenticator (and
 * per-connector) results.</p>
 *
 * <p>By default nothing is stored for an authenticator on the shadow — it is resolved entirely from the parent at
 * fetch time. A resolver may, in future, choose to store a few (overridable) attributes of an authenticator on the
 * shadow (mostly properties); {@link #resolveAuthenticator} then merges those stored values with the remaining
 * values resolved from the parent. Implementations are contributed via the OSGi whiteboard and keyed by the
 * authenticator name they handle.</p>
 */
public interface SharedFederatedAuthenticatorResolver {

    /**
     * The federated authenticator name this resolver handles. Used to register the resolver in the per-authenticator
     * registry; a resolver is selected when this equals the (parent) authenticator's name.
     *
     * @return The federated authenticator name; must be non-{@code null} for a contributed resolver.
     */
    String getAuthenticatorName();

    /**
     * Resolves a federated authenticator for a fetch of the shadow identity provider. The result is assembled from
     * the parent's authenticator and whatever (if anything) the sub-organization stored locally on the shadow:
     * <ul>
     *   <li>{@code resolveWithParent = false} (management view) — the basic/identity attributes (name, enabled,
     *       tags, defined-by type), plus any locally-stored overridable values.</li>
     *   <li>{@code resolveWithParent = true} (runtime view) — the parent's full configuration, with the remaining
     *       (non-stored) values inherited from the parent and any locally-stored overridable values preserved.</li>
     * </ul>
     *
     * @param parentAuthenticator The parent's federated authenticator (the source of inherited values).
     * @param shadowAuthenticator The authenticator stored locally on the shadow; {@code null} when nothing is
     *                            stored (the default).
     * @param resolveWithParent   {@code true} for the runtime view, {@code false} for the management view.
     * @return The resolved federated authenticator.
     */
    FederatedAuthenticatorConfig resolveAuthenticator(FederatedAuthenticatorConfig parentAuthenticator,
                                                      FederatedAuthenticatorConfig shadowAuthenticator,
                                                      boolean resolveWithParent);

    /**
     * Returns the inherited (non-overridable) attributes of this authenticator that an incoming update would change
     * relative to the stored shadow — i.e. the modifications a sub-organization is not allowed to make. An empty
     * list means the update touches only locally-overridable attributes and is allowed.
     *
     * @param incomingAuthenticator The incoming (to-be-persisted) authenticator.
     * @param storedAuthenticator   The shadow's current stored authenticator (the baseline).
     * @return The display names of the restricted attributes that were modified; empty if none.
     */
    List<String> getRestrictedModifications(FederatedAuthenticatorConfig incomingAuthenticator,
                                            FederatedAuthenticatorConfig storedAuthenticator);

    /**
     * Performs pre-update validation for the incoming authenticator against the stored authenticator. If any
     * validation fails, an {@link IdentityProviderManagementException} is thrown.
     *
     * @param updatingAuthenticator The incoming (to-be-persisted) authenticator.
     * @param existingAuthenticator The shadow's current stored authenticator (the baseline).
     * @param parentIdp The parent identity provider from which the shadow inherits values.
     * @throws IdentityProviderManagementException If any validation fails.
     */
    void doPreUpdateValidation(FederatedAuthenticatorConfig updatingAuthenticator,
                               FederatedAuthenticatorConfig existingAuthenticator, IdentityProvider parentIdp)
            throws IdentityProviderManagementException;
}
