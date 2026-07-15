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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.idp.resolver;

import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.ErrorMessage.ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE;

/**
 * Default {@link SharedProvisioningConnectorResolver} — the fallback used for any outbound provisioning connector
 * with no resolver registered under its name, and the base class for connector-specific resolvers.
 *
 * <p>It stores <b>nothing</b> for a connector on the shadow; every attribute is resolved from the parent at fetch
 * time, driven by the {@link #getInheritedConfigAttributes() inherited-config registry}. By default every attribute
 * is inherited; a resolver for a specific connector extends this class, returns the connector name from
 * {@link #getConnectorName()}, and <b>overrides {@link #getInheritedConfigAttributes()}</b> to mark the attributes a
 * sub-organization may edit as {@link ConfigAttribute#overridable overridable}; those locally-stored values are then
 * preserved by {@link #resolveConnector} while the rest are inherited from the parent.</p>
 */
public class DefaultSharedProvisioningConnectorResolver implements SharedProvisioningConnectorResolver {

    /**
     * The default registry: everything beyond the identity fields is inherited from the parent. Override
     * {@link #getInheritedConfigAttributes()} to mark specific attributes overridable.
     */
    private static final List<ConfigAttribute<ProvisioningConnectorConfig, ?>> INHERITED_CONFIG_ATTRIBUTES =
            Arrays.asList(
                    ConfigAttribute.inherited("enabled",
                            ProvisioningConnectorConfig::isEnabled, ProvisioningConnectorConfig::setEnabled),
                    ConfigAttribute.inherited("blocking",
                            ProvisioningConnectorConfig::isBlocking, ProvisioningConnectorConfig::setBlocking),
                    ConfigAttribute.inherited("rules enabled",
                            ProvisioningConnectorConfig::isRulesEnabled, ProvisioningConnectorConfig::setRulesEnabled),
                    ConfigAttribute.inherited("provisioning properties",
                            ProvisioningConnectorConfig::getProvisioningProperties,
                            ProvisioningConnectorConfig::setProvisioningProperties)
            );

    @Override
    public String getConnectorName() {

        // The fallback is held directly by the data holder, never keyed by name.
        return null;
    }

    /**
     * The inherited/overridable attribute registry for this connector. Defaults to everything inherited; override to
     * mark specific attributes {@link ConfigAttribute#overridable overridable}.
     *
     * @return The attribute registry driving the runtime overlay and the deny-guard.
     */
    protected List<ConfigAttribute<ProvisioningConnectorConfig, ?>> getInheritedConfigAttributes() {

        return INHERITED_CONFIG_ATTRIBUTES;
    }

    @Override
    public ProvisioningConnectorConfig resolveConnector(ProvisioningConnectorConfig parentConnector,
                                                        ProvisioningConnectorConfig shadowConnector,
                                                        boolean resolveWithParent) {

        if (parentConnector == null) {
            return null;
        }

        ProvisioningConnectorConfig resolvedConnector = new ProvisioningConnectorConfig();
        resolvedConnector.setName(parentConnector.getName());
        resolvedConnector.setEnabled(parentConnector.isEnabled());
        resolvedConnector.setBlocking(parentConnector.isBlocking());
        resolvedConnector.setRulesEnabled(parentConnector.isRulesEnabled());

        if (resolveWithParent) {
            resolvedConnector.setProvisioningProperties(parentConnector.getProvisioningProperties());
        }

        return resolvedConnector;
    }

    @Override
    public List<String> getRestrictedModifications(ProvisioningConnectorConfig incomingConnector,
                                                   ProvisioningConnectorConfig storedConnector) {

        return ConfigAttributes.restrictedModifications(incomingConnector, storedConnector,
                getInheritedConfigAttributes());
    }

    @Override
    public void doPreUpdateValidation(ProvisioningConnectorConfig updatingConnector,
                                      ProvisioningConnectorConfig storedConnector, IdentityProvider parentIdp)
            throws IdentityProviderManagementException {

        try {
            ConfigAttributes.validateRestrictedModifications(updatingConnector, storedConnector,
                    INHERITED_CONFIG_ATTRIBUTES);
        } catch (IllegalArgumentException e) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    e.getMessage(), e);
        }
    }
}
