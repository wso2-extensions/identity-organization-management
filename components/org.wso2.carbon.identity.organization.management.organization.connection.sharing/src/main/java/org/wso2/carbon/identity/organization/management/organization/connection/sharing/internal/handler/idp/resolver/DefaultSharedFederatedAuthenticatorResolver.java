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
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.exception.RestrictedAttributeModificationException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.ConfigAttribute;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.handler.ConfigAttributes;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.ErrorMessage.ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE;

/**
 * Default {@link SharedFederatedAuthenticatorResolver} — the fallback used for any federated authenticator with no
 * resolver registered under its name, and the base class for authenticator-specific resolvers.
 *
 * <p>It stores <b>nothing</b> for an authenticator on the shadow; every attribute is resolved from the parent at
 * fetch time, driven by the {@link #getInheritedConfigAttributes() inherited-config registry}. By default every
 * attribute is inherited, so a sub-organization may not edit any of them. A resolver for a specific authenticator
 * extends this class, returns the authenticator name from {@link #getAuthenticatorName()}, and
 * <b>overrides {@link #getInheritedConfigAttributes()}</b> to mark the attributes a sub-organization may edit as
 * {@link ConfigAttribute#overridable overridable}; those locally-stored values are then preserved by
 * {@link #resolveAuthenticator} while the rest are inherited from the parent.</p>
 */
public class DefaultSharedFederatedAuthenticatorResolver implements SharedFederatedAuthenticatorResolver {

    /**
     * The default registry: everything beyond the identity fields is inherited from the parent. Override
     * {@link #getInheritedConfigAttributes()} to mark specific attributes overridable.
     */
    private static final List<ConfigAttribute<FederatedAuthenticatorConfig, ?>> INHERITED_CONFIG_ATTRIBUTES =
            Arrays.asList(
                    ConfigAttribute.inherited("name", FederatedAuthenticatorConfig::getName,
                            FederatedAuthenticatorConfig::setName),
                    ConfigAttribute.inherited("display name", FederatedAuthenticatorConfig::getDisplayName,
                            FederatedAuthenticatorConfig::setDisplayName),
                    ConfigAttribute.inherited("enabled", FederatedAuthenticatorConfig::isEnabled,
                            FederatedAuthenticatorConfig::setEnabled),
                    ConfigAttribute.inherited("tags", FederatedAuthenticatorConfig::getTags,
                            FederatedAuthenticatorConfig::setTags),
                    ConfigAttribute.inherited("properties", FederatedAuthenticatorConfig::getProperties,
                            FederatedAuthenticatorConfig::setProperties)
            );

    @Override
    public String getAuthenticatorName() {

        // The fallback is held directly by the data holder, never keyed by name.
        return "DEFAULT_AUTHENTICATOR";
    }

    /**
     * The inherited/overridable attribute registry for this authenticator. Defaults to everything inherited;
     * override to mark specific attributes {@link ConfigAttribute#overridable overridable}.
     *
     * @return The attribute registry driving the runtime overlay and the deny-guard.
     */
    protected List<ConfigAttribute<FederatedAuthenticatorConfig, ?>> getInheritedConfigAttributes() {

        return INHERITED_CONFIG_ATTRIBUTES;
    }

    @Override
    public FederatedAuthenticatorConfig resolveAuthenticator(FederatedAuthenticatorConfig parentAuthenticator,
                                                             FederatedAuthenticatorConfig shadowAuthenticator,
                                                             boolean resolveWithParent) {

        if (parentAuthenticator == null) {
            return null;
        }

        FederatedAuthenticatorConfig resolvedAuthenticator = new FederatedAuthenticatorConfig();
        resolvedAuthenticator.setName(parentAuthenticator.getName());
        resolvedAuthenticator.setDisplayName(parentAuthenticator.getDisplayName());
        resolvedAuthenticator.setEnabled(parentAuthenticator.isEnabled());
        resolvedAuthenticator.setTags(parentAuthenticator.getTags());
        resolvedAuthenticator.setDefinedByType(parentAuthenticator.getDefinedByType());

        if (resolveWithParent) {
            resolvedAuthenticator.setProperties(parentAuthenticator.getProperties());
        }

        return resolvedAuthenticator;
    }

    @Override
    public List<String> getRestrictedModifications(FederatedAuthenticatorConfig incomingAuthenticator,
                                                   FederatedAuthenticatorConfig storedAuthenticator) {

        return ConfigAttributes.restrictedModifications(incomingAuthenticator, storedAuthenticator,
                getInheritedConfigAttributes());
    }

    @Override
    public void doPreUpdateValidation(FederatedAuthenticatorConfig updatingAuthenticator,
                                      FederatedAuthenticatorConfig existingAuthenticator, IdentityProvider parentIdp)
            throws IdentityProviderManagementException {

        try {
            ConfigAttributes.validateRestrictedModifications(updatingAuthenticator, existingAuthenticator,
                    INHERITED_CONFIG_ATTRIBUTES);
        } catch (RestrictedAttributeModificationException e) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    e.getMessage(), e);
        }
    }
}
