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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdPGroup;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionType;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.AbstractConnectionTypeHandler;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.ConnectionSharingDataHolder;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdpManager;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_CONNECTION_SHARE_CLIENT_ERROR;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_INTERNAL_ERROR;

/**
 * {@link org.wso2.carbon.identity.organization.management.organization.connection.sharing.ConnectionTypeHandler}
 * implementation for identity provider connections. Only the identity-provider-specific shadow-resource
 * operations live here; the association persistence and orchestration are inherited from
 * {@link AbstractConnectionTypeHandler}.
 *
 * <p>The shadow is a stub: it stores only the minimal local state (name, enabled flag and the {@code isShared}
 * marker). The full configuration is resolved from the parent identity provider at runtime via the connection
 * association.</p>
 */
public class IdpConnectionTypeHandler extends AbstractConnectionTypeHandler {

    private static final Log LOG = LogFactory.getLog(IdpConnectionTypeHandler.class);

    @Override
    public ConnectionType getConnectionType() {

        return ConnectionType.IDP;
    }

    @Override
    protected String createSharedResource(String connectionId, String residentOrgId, String residentTenantDomain,
                                          String targetTenantDomain) throws ConnectionSharingMgtException {

        IdentityProvider parentIdp;
        try {
            parentIdp = getIdpManager().getIdPByResourceId(connectionId, residentTenantDomain, false);
        } catch (IdentityProviderManagementException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
        if (parentIdp == null) {
            String errorMessage = "Identity provider connection: " + connectionId + " not found in tenant: " +
                    residentTenantDomain + ". Cannot share to tenant: " + targetTenantDomain + ".";
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR.getCode(), errorMessage,
                    ERROR_CODE_INTERNAL_ERROR.getDescription());
        }

        IdentityProvider shadowIdp = buildShadowIdp(parentIdp);
        return runInTenant(targetTenantDomain, () -> {
            try {
                return getIdpManager().addIdPWithResourceId(shadowIdp, targetTenantDomain).getResourceId();
            } catch (IdentityProviderManagementClientException e) {
                throw new ConnectionSharingMgtClientException(ERROR_CODE_CONNECTION_SHARE_CLIENT_ERROR.getCode(),
                        ERROR_CODE_CONNECTION_SHARE_CLIENT_ERROR.getMessage(),
                        e.getMessage());
            } catch (IdentityProviderManagementException e) {
                throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
            }
        });
    }

    @Override
    protected void deleteSharedResource(String sharedResourceId, String targetTenantDomain)
            throws ConnectionSharingMgtException {

        runInTenant(targetTenantDomain, () -> {
            try {
                getIdpManager().forceDeleteIdpByResourceId(sharedResourceId, targetTenantDomain);
            } catch (IdentityProviderManagementException e) {
                throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
            }
            return null;
        });
    }

    /**
     * Builds the shadow (stub) identity provider that is persisted in the sub-org: the name, enabled flag, the
     * {@code isShared} marker, the federated authenticator / outbound provisioning connector stubs and the identity
     * provider groups. The idp groups are persisted (rather than resolved from the parent at fetch time) so they
     * have their own stable group ids in the sub-org for group-based role assignment; they are kept in sync with
     * the parent by the {@code SharedIdpMgtListener} propagation. The rest of the configuration is resolved from
     * the parent identity provider at fetch time by the {@code SharedIdpResolver}.
     */
    private IdentityProvider buildShadowIdp(IdentityProvider parentIdp) {

        IdentityProvider sharedIdp = new IdentityProvider();
        sharedIdp.setIdentityProviderName(parentIdp.getIdentityProviderName());
        sharedIdp.setEnable(parentIdp.isEnable());

        setFederatedAuthenticators(sharedIdp, parentIdp);
        setProvisioningConnectors(sharedIdp, parentIdp);
        setIdpGroups(sharedIdp, parentIdp);

        IdentityProviderProperty sharedConnectionProperty = new IdentityProviderProperty();
        sharedConnectionProperty.setName(IdentityProvider.IS_SHARED_IDP_PROPERTY);
        sharedConnectionProperty.setValue(Boolean.TRUE.toString());
        sharedIdp.setIdpProperties(new IdentityProviderProperty[]{sharedConnectionProperty});

        return sharedIdp;
    }

    private void setFederatedAuthenticators(IdentityProvider sharedIdp, IdentityProvider parentIdp) {

        if (parentIdp.getFederatedAuthenticatorConfigs() != null) {
            List<FederatedAuthenticatorConfig> sharedFedAuthenticators = new ArrayList<>();
            for (FederatedAuthenticatorConfig config : parentIdp.getFederatedAuthenticatorConfigs()) {
                FederatedAuthenticatorConfig sharedFedAuthenticator = new FederatedAuthenticatorConfig();
                sharedFedAuthenticator.setName(config.getName());
                sharedFedAuthenticators.add(sharedFedAuthenticator);
            }
            sharedIdp.setFederatedAuthenticatorConfigs(
                    sharedFedAuthenticators.toArray(new FederatedAuthenticatorConfig[0]));
        }

        if (parentIdp.getDefaultAuthenticatorConfig() != null) {
            FederatedAuthenticatorConfig defaultAuthenticator = new FederatedAuthenticatorConfig();
            defaultAuthenticator.setName(parentIdp.getDefaultAuthenticatorConfig().getName());
            sharedIdp.setDefaultAuthenticatorConfig(defaultAuthenticator);
        }
    }

    private void setProvisioningConnectors(IdentityProvider sharedIdp, IdentityProvider parentIdp) {

        if (parentIdp.getProvisioningConnectorConfigs() != null) {
            List<ProvisioningConnectorConfig> sharedProvisioningConnectors = new ArrayList<>();
            for (ProvisioningConnectorConfig config : parentIdp.getProvisioningConnectorConfigs()) {
                ProvisioningConnectorConfig sharedProvisioningConnector = new ProvisioningConnectorConfig();
                sharedProvisioningConnector.setName(config.getName());
                sharedProvisioningConnectors.add(sharedProvisioningConnector);
            }
            sharedIdp.setProvisioningConnectorConfigs(
                    sharedProvisioningConnectors.toArray(new ProvisioningConnectorConfig[0]));
        }

        if (parentIdp.getDefaultProvisioningConnectorConfig() != null) {
            ProvisioningConnectorConfig defaultProvisioningConnector = new ProvisioningConnectorConfig();
            defaultProvisioningConnector.setName(parentIdp.getDefaultProvisioningConnectorConfig().getName());
            sharedIdp.setDefaultProvisioningConnectorConfig(defaultProvisioningConnector);
        }
    }

    /**
     * Persists the parent's identity provider groups on the shadow by name only, with a {@code null} id so the
     * sub-org generates its own stable group ids (the add path rejects pre-set group ids). These sub-org group ids
     * remain stable across parent group changes for groups matched by name, so sub-org group-based role mappings
     * survive parent updates.
     */
    private void setIdpGroups(IdentityProvider sharedIdp, IdentityProvider parentIdp) {

        IdPGroup[] parentGroups = parentIdp.getIdPGroupConfig();
        if (parentGroups == null) {
            return;
        }
        List<IdPGroup> sharedGroups = new ArrayList<>();
        for (IdPGroup parentGroup : parentGroups) {
            if (parentGroup == null || StringUtils.isBlank(parentGroup.getIdpGroupName())) {
                continue;
            }
            IdPGroup sharedGroup = new IdPGroup();
            sharedGroup.setIdpGroupName(parentGroup.getIdpGroupName());
            sharedGroups.add(sharedGroup);
        }
        sharedIdp.setIdPGroupConfig(sharedGroups.toArray(new IdPGroup[0]));
    }

    private IdpManager getIdpManager() {

        return ConnectionSharingDataHolder.getInstance().getIdpManager();
    }
}
