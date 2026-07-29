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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdPGroup;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.JustInTimeProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.RestrictedAttributeModificationException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.ConfigAttribute;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.ConfigAttributes;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.ConnectionSharingDataHolder;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.ErrorMessage.ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE;

/**
 * The shared identity provider resolver. It overlays the parent configuration onto the shadow, preserving any
 * locally-overridden sections.
 */
public class SharedIdpResolver {

    private static final Log LOG = LogFactory.getLog(SharedIdpResolver.class);
    private static final SharedIdpResolver INSTANCE = new SharedIdpResolver();
    /**
     * identity.xml configuration key for the fallback provisioning user store applied when a shared IDP's inherited
     * JIT provisioning user store does not exist in the sub-organization. When unset, the primary user store domain
     * is used.
     */
    private static final String DEFAULT_PROVISIONING_USERSTORE_CONFIG =
            "ConnectionSharing.DefaultProvisioningUserStore";
    /**
     * The <b>base</b> IDP-level attributes shown in the management (base) view: the always-parent-derived display
     * attributes (description, image URL), plus the locally-overridable claim / JIT provisioning configurations so a
     * sub-organization sees (and can override) the inherited claim/JIT config in the management view. These are
     * applied by both {@link #overlayBasicParentAttributes} and (with the rest) {@link #overlayParentConfiguration};
     * kept separate so the base overlay can apply exactly this subset.
     */
    private static final List<ConfigAttribute<IdentityProvider, ?>> BASE_PARENT_CONFIG_ATTRIBUTES = Arrays.asList(
            ConfigAttribute.inherited("description",
                    IdentityProvider::getIdentityProviderDescription,
                    IdentityProvider::setIdentityProviderDescription),
            ConfigAttribute.inherited("image URL", IdentityProvider::getImageUrl, IdentityProvider::setImageUrl),
            ConfigAttribute.inherited("template ID", IdentityProvider::getTemplateId,
                    IdentityProvider::setTemplateId),
            ConfigAttribute.inherited("identity provider groups",
                    IdentityProvider::getIdPGroupConfig, IdentityProvider::setIdPGroupConfig),
            // Locally-overridable sections shown in the management view: the shadow's value wins when configured
            // locally, else the parent's is shown.
            ConfigAttribute.overridable("claim configuration",
                    IdentityProvider::getClaimConfig, IdentityProvider::setClaimConfig,
                    SharedIdpResolver::isClaimConfigLocallyConfigured),
            ConfigAttribute.overridable("JIT provisioning configuration",
                    IdentityProvider::getJustInTimeProvisioningConfig,
                    IdentityProvider::setJustInTimeProvisioningConfig,
                    SharedIdpResolver::isJitProvisioningConfigLocallyConfigured)
    );

    /**
     * The remaining IDP-level config registry — the attributes overlaid only in the full (runtime) view (in
     * addition to {@link #BASE_PARENT_CONFIG_ATTRIBUTES}). Inherited sections are always taken from the parent on
     * read and may NOT be modified on a shadow; {@code local} sections (provisioning role) are never taken from the
     * parent. Anything not listed is locally-owned identity ({@code isEnabled}, internal/resource id, markers). The
     * federated authenticator and outbound provisioning connector sections are NOT here: they are handled
     * per-resource (see the per-resource resolvers).
     */
    private static final List<ConfigAttribute<IdentityProvider, ?>> INHERITED_CONFIG_ATTRIBUTES = Arrays.asList(
            // The connection name is owned by the parent: a sub-organization may not rename a shadow, and a parent
            // rename is reflected on read.
            ConfigAttribute.inherited("name",
                    IdentityProvider::getIdentityProviderName, IdentityProvider::setIdentityProviderName),
            ConfigAttribute.inherited("alias", IdentityProvider::getAlias, IdentityProvider::setAlias),
            ConfigAttribute.inherited("primary flag", IdentityProvider::isPrimary, IdentityProvider::setPrimary),
            ConfigAttribute.inherited("federation hub flag",
                    IdentityProvider::isFederationHub, IdentityProvider::setFederationHub),
            ConfigAttribute.inherited("home realm identifier",
                    IdentityProvider::getHomeRealmId, IdentityProvider::setHomeRealmId),
            ConfigAttribute.inherited("display name",
                    IdentityProvider::getDisplayName, IdentityProvider::setDisplayName),
            ConfigAttribute.inherited("certificate",
                    IdentityProvider::getCertificate, IdentityProvider::setCertificate),
            ConfigAttribute.inherited("roles and permissions",
                    IdentityProvider::getPermissionAndRoleConfig, IdentityProvider::setPermissionAndRoleConfig),
            ConfigAttribute.inherited("federated association configuration",
                    IdentityProvider::getFederatedAssociationConfig,
                    IdentityProvider::setFederatedAssociationConfig),
            ConfigAttribute.inherited("trusted token issuer flag",
                    IdentityProvider::isTrustedTokenIssuer, IdentityProvider::setTrustedTokenIssuer),
            // Local-only sections — never inherited from the parent; always the shadow's own value. The outbound
            // provisioning role is meaningful only within the sub-organization, so it is never taken from parent.
            ConfigAttribute.local("provisioning role", IdentityProvider::getProvisioningRole,
                    IdentityProvider::setProvisioningRole)
    );

    private SharedIdpResolver() {
        // Private constructor to prevent instantiation.
    }

    public static SharedIdpResolver getInstance() {

        return INSTANCE;
    }

    public void overlayParentConfiguration(IdentityProvider parentIdp, IdentityProvider sharedIdp,
                                           String tenantDomain) {

        // 1. Preserve the shadow's locally-owned identity and merged properties. The name and the identity provider
        //    groups are always taken from the shadow (persisted locally with sub-org group ids and kept in sync with
        //    the parent via propagation) rather than overlaid from the parent.
        String localId = sharedIdp.getId();
        String localResourceId = sharedIdp.getResourceId();
        String localName = sharedIdp.getIdentityProviderName();
        boolean localEnabled = sharedIdp.isEnable();
        IdPGroup[] localIdPGroups = sharedIdp.getIdPGroupConfig();
        IdentityProviderProperty[] mergedProperties = mergeProperties(parentIdp.getIdpProperties(),
                sharedIdp.getIdpProperties());

        // 2. Overlay the IDP-level sections (base + the rest) from the parent, preserving the shadow's
        //    locally-overridable ones.
        ConfigAttributes.applyOverlay(parentIdp, sharedIdp, BASE_PARENT_CONFIG_ATTRIBUTES);
        ConfigAttributes.applyOverlay(parentIdp, sharedIdp, INHERITED_CONFIG_ATTRIBUTES);

        // 3. Re-apply the shadow's locally-owned identity. The effective enabled state is the AND of the parent's
        //    and the shadow's local flags: a disabled parent forces the shadow disabled, while an enabled parent
        //    lets the sub-organization keep the shadow disabled locally.
        sharedIdp.setId(localId);
        sharedIdp.setResourceId(localResourceId);
        sharedIdp.setIdentityProviderName(localName);
        sharedIdp.setEnable(parentIdp.isEnable() && localEnabled);
        sharedIdp.setIdPGroupConfig(localIdPGroups);
        sharedIdp.setIdpProperties(mergedProperties);

        // 4. Populate each federated authenticator and outbound provisioning connector from the parent (runtime
        //    depth), honoring any locally-stored overridable values on the shadow.
        resolveAuthenticators(parentIdp, sharedIdp, true);
        resolveConnectors(parentIdp, sharedIdp, true);

        // 5. The inherited JIT provisioning user store may not exist in the sub-organization; fall back to the
        //    configurable default when it does not.
        resolveJitProvisioningUserStore(sharedIdp, tenantDomain);
    }

    public void overlayBasicParentAttributes(IdentityProvider parentIdp, IdentityProvider sharedIdp,
                                             String tenantDomain) {


        boolean localEnabled = sharedIdp.isEnable();
        IdPGroup[] localIdPGroups = sharedIdp.getIdPGroupConfig();
        // Apply only the base parent attributes (description, image URL, claim/JIT); everything else stays as the
        // raw shadow.
        ConfigAttributes.applyOverlay(parentIdp, sharedIdp, BASE_PARENT_CONFIG_ATTRIBUTES);
        // The effective enabled state is the AND of the parent's and the shadow's local flags.
        sharedIdp.setEnable(parentIdp.isEnable() && sharedIdp.isEnable());
        // Populate basic values of each authenticator/connector from the parent.
        resolveAuthenticators(parentIdp, sharedIdp, false);
        resolveConnectors(parentIdp, sharedIdp, false);
        // The inherited JIT provisioning user store may not exist in the sub-organization; fall back to the
        // configurable default when it does not.
        resolveJitProvisioningUserStore(sharedIdp, tenantDomain);
        // Re-apply locally configured values.
        sharedIdp.setEnable(parentIdp.isEnable() && localEnabled);
        sharedIdp.setIdPGroupConfig(localIdPGroups);
    }

    public void doPreUpdateValidations(IdentityProvider updatingShadowIdp, IdentityProvider existingShadowIdp,
                                       IdentityProvider parentIdp) throws IdentityProviderManagementException {

        try {
            boolean isSharedIdpEnabling = !existingShadowIdp.isEnable() && updatingShadowIdp.isEnable();
            if (isSharedIdpEnabling && !parentIdp.isEnable()) {
                throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                        "Cannot enable the shared idp as the parent is disabled.");
            }
            ConfigAttributes.validateRestrictedModifications(updatingShadowIdp, existingShadowIdp,
                    BASE_PARENT_CONFIG_ATTRIBUTES);
            ConfigAttributes.validateRestrictedModifications(updatingShadowIdp, existingShadowIdp,
                    INHERITED_CONFIG_ATTRIBUTES);
            validateIdpPropertiesUpdate(updatingShadowIdp.getIdpProperties(), existingShadowIdp.getIdpProperties());
        } catch (RestrictedAttributeModificationException e) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    e.getMessage(), e);
        }

        doPreUpdateFederatedAuthenticatorValidations(updatingShadowIdp, existingShadowIdp, parentIdp);
        doPreUpdateProvisioningConnectorValidations(updatingShadowIdp, existingShadowIdp, parentIdp);
    }

    private void validateIdpPropertiesUpdate(IdentityProviderProperty[] updatingIdpProperties,
                                             IdentityProviderProperty[] existingIdpProperties)
        throws RestrictedAttributeModificationException {

        Set<String> existingPropertyNames = Arrays.stream(existingIdpProperties)
                .filter(property -> property != null && property.getName() != null)
                .map(IdentityProviderProperty::getName)
                .collect(Collectors.toSet());

        for (IdentityProviderProperty property: updatingIdpProperties) {
            if (property != null && property.getName() != null && !existingPropertyNames.contains(property.getName())) {
                throw new RestrictedAttributeModificationException(property.getName());
            }
        }
    }

    private void doPreUpdateFederatedAuthenticatorValidations(IdentityProvider updatingShadowIdp,
                                                             IdentityProvider existingShadowIdp,
                                                             IdentityProvider parentIdp)
            throws IdentityProviderManagementException {


        doPreUpdateDefaultAuthenticatorValidation(updatingShadowIdp, existingShadowIdp, parentIdp);

        // Validate each federated authenticator in the incoming update against the stored shadow and the parent.
        FederatedAuthenticatorConfig[] updatingAuthenticators = updatingShadowIdp.getFederatedAuthenticatorConfigs();
        if (updatingAuthenticators == null) {
            return;
        }

        Map<String, FederatedAuthenticatorConfig> existingAuthenticators =
                Arrays.stream(existingShadowIdp.getFederatedAuthenticatorConfigs())
                        .collect(Collectors.toMap(FederatedAuthenticatorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));
        Map<String, FederatedAuthenticatorConfig> parentAuthenticators =
                Arrays.stream(parentIdp.getFederatedAuthenticatorConfigs())
                        .collect(Collectors.toMap(FederatedAuthenticatorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));

        for (FederatedAuthenticatorConfig updatingAuthenticator : updatingAuthenticators) {
            if (updatingAuthenticator == null || updatingAuthenticator.getName() == null) {
                continue;
            }
            if (!parentAuthenticators.containsKey(updatingAuthenticator.getName())) {
                throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                        "Cannot add a new authenticator to the shared idp which is not present in the parent idp.");
            }

            getAuthenticatorResolver(updatingAuthenticator).doPreUpdateValidation(updatingAuthenticator,
                    existingAuthenticators.get(updatingAuthenticator.getName()), parentIdp);
        }
    }

    private void doPreUpdateDefaultAuthenticatorValidation(IdentityProvider updatingShadowIdp,
                                                           IdentityProvider existingShadowIdp,
                                                           IdentityProvider parentIdp)
            throws IdentityProviderManagementException {

        FederatedAuthenticatorConfig updatingDefaultAuthenticator = updatingShadowIdp.getDefaultAuthenticatorConfig();
        FederatedAuthenticatorConfig existingDefaultAuthenticator = existingShadowIdp.getDefaultAuthenticatorConfig();
        FederatedAuthenticatorConfig parentDefaultAuthenticator = parentIdp.getDefaultAuthenticatorConfig();

        if (parentDefaultAuthenticator == null && updatingDefaultAuthenticator == null) {
            return;
        }

        if (parentDefaultAuthenticator == null) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    "Cannot update default federated authenticator as the parent does not have a default " +
                            "federated authenticator.");
        }

        if (parentDefaultAuthenticator.getName() != null &&
                updatingDefaultAuthenticator != null && updatingDefaultAuthenticator.getName() != null &&
                !parentDefaultAuthenticator.getName().equals(updatingDefaultAuthenticator.getName())) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    "Updating default federated authenticator and the parent default federated authenticator " +
                            "are not the same.");
        }

        getAuthenticatorResolver(parentDefaultAuthenticator).doPreUpdateValidation(updatingDefaultAuthenticator,
                existingDefaultAuthenticator, parentIdp);

    }

    private void doPreUpdateProvisioningConnectorValidations(IdentityProvider updatingShadowIdp,
                                                            IdentityProvider existingShadowIdp,
                                                            IdentityProvider parentIdp)
            throws IdentityProviderManagementException {


        doPreUpdateDefaultProvisioningConnectorValidation(updatingShadowIdp, existingShadowIdp, parentIdp);

        // Validate each provisioning connector in the incoming update against the stored shadow and the parent.
        ProvisioningConnectorConfig[] updatingConnectors = updatingShadowIdp.getProvisioningConnectorConfigs();
        if (updatingConnectors == null) {
            return;
        }

        Map<String, ProvisioningConnectorConfig> existingConnectors =
                Arrays.stream(existingShadowIdp.getProvisioningConnectorConfigs())
                        .collect(Collectors.toMap(ProvisioningConnectorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));
        Map<String, ProvisioningConnectorConfig> parentConnectors =
                Arrays.stream(parentIdp.getProvisioningConnectorConfigs())
                        .collect(Collectors.toMap(ProvisioningConnectorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));

        for (ProvisioningConnectorConfig updatingConnector : updatingConnectors) {
            if (updatingConnector == null || updatingConnector.getName() == null) {
                continue;
            }
            if (!parentConnectors.containsKey(updatingConnector.getName())) {
                throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                        "Cannot add a new provisioning connector to the shared idp which is not present in " +
                                "the parent idp.");
            }

            getConnectorResolver(updatingConnector).doPreUpdateValidation(updatingConnector,
                    existingConnectors.get(updatingConnector.getName()), parentIdp);
        }
    }

    private void doPreUpdateDefaultProvisioningConnectorValidation(IdentityProvider updatingShadowIdp,
                                                                   IdentityProvider existingShadowIdp,
                                                                   IdentityProvider parentIdp)
            throws IdentityProviderManagementException {

        ProvisioningConnectorConfig updatingConnector = updatingShadowIdp.getDefaultProvisioningConnectorConfig();
        ProvisioningConnectorConfig existingConnector = existingShadowIdp.getDefaultProvisioningConnectorConfig();
        ProvisioningConnectorConfig parentConnector = parentIdp.getDefaultProvisioningConnectorConfig();

        if (parentConnector == null && updatingConnector == null) {
            return;
        }

        if (parentConnector == null) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    "Cannot update default provisioning connector as the parent does not have a default " +
                            "provisioning connector.");
        }

        if (parentConnector.getName() != null && updatingConnector != null && updatingConnector.getName() != null &&
                !parentConnector.getName().equals(updatingConnector.getName())) {
            throw new IdentityProviderManagementClientException(ERROR_CODE_RESTRICTED_SHARED_IDP_UPDATE.getCode(),
                    "Updating default provisioning connector and the parent default provisioning connector " +
                            "are not the same.");
        }

        getConnectorResolver(parentConnector).doPreUpdateValidation(updatingConnector, existingConnector, parentIdp);

    }

    private void resolveAuthenticators(IdentityProvider parentIdp, IdentityProvider shadowIdp,
                                       boolean resolveWithParent) {

        FederatedAuthenticatorConfig[] parentAuthenticators = parentIdp.getFederatedAuthenticatorConfigs();
        FederatedAuthenticatorConfig parentDefault = parentIdp.getDefaultAuthenticatorConfig();
        if (parentAuthenticators == null) {
            shadowIdp.setFederatedAuthenticatorConfigs(null);
            shadowIdp.setDefaultAuthenticatorConfig(null);
            return;
        }
        // Whatever the sub-organization stored locally (nothing by default); matched to its parent by name.
        Map<String, FederatedAuthenticatorConfig> shadowAuthenticators =
                Arrays.stream(shadowIdp.getFederatedAuthenticatorConfigs())
                        .collect(Collectors.toMap(FederatedAuthenticatorConfig::getName, Function.identity(),
                                (existing, replacement) -> existing));

        List<FederatedAuthenticatorConfig> resolved = new ArrayList<>();
        for (FederatedAuthenticatorConfig parentAuthenticator : parentAuthenticators) {
            if (parentAuthenticator == null) {
                continue;
            }
            FederatedAuthenticatorConfig shadowAuthenticator = shadowAuthenticators.get(parentAuthenticator.getName());
            FederatedAuthenticatorConfig resolvedAuthenticator = getAuthenticatorResolver(parentAuthenticator)
                    .resolveAuthenticator(parentAuthenticator, shadowAuthenticator, resolveWithParent);
            resolved.add(resolvedAuthenticator);

            if (parentDefault != null && StringUtils.equals(parentDefault.getName(), parentAuthenticator.getName())) {
                shadowIdp.setDefaultAuthenticatorConfig(resolvedAuthenticator);
            }
        }
        shadowIdp.setFederatedAuthenticatorConfigs(resolved.toArray(new FederatedAuthenticatorConfig[0]));
    }

    private void resolveConnectors(IdentityProvider parentIdp, IdentityProvider shadowIdp,
                                   boolean resolveWithParent) {

        ProvisioningConnectorConfig[] parentConnectors = parentIdp.getProvisioningConnectorConfigs();
        if (parentConnectors == null) {
            shadowIdp.setProvisioningConnectorConfigs(null);
            shadowIdp.setDefaultProvisioningConnectorConfig(null);
            return;
        }
        Map<String, ProvisioningConnectorConfig> storedByName =
                indexByName(shadowIdp.getProvisioningConnectorConfigs(), ProvisioningConnectorConfig::getName);
        List<ProvisioningConnectorConfig> resolved = new ArrayList<>();
        for (ProvisioningConnectorConfig parentConnector : parentConnectors) {
            if (parentConnector == null) {
                continue;
            }
            ProvisioningConnectorConfig storedConnector = storedByName.get(parentConnector.getName());
            resolved.add(getConnectorResolver(parentConnector)
                    .resolveConnector(parentConnector, storedConnector, resolveWithParent));
        }
        shadowIdp.setProvisioningConnectorConfigs(resolved.toArray(new ProvisioningConnectorConfig[0]));

        ProvisioningConnectorConfig parentDefault = parentIdp.getDefaultProvisioningConnectorConfig();
        shadowIdp.setDefaultProvisioningConnectorConfig(parentDefault == null ? null
                : pickByName(resolved, parentDefault.getName(), ProvisioningConnectorConfig::getName));
    }

    private static <T> T pickByName(List<T> items, String name, Function<T, String> nameGetter) {

        for (T item : items) {
            if (StringUtils.equals(nameGetter.apply(item), name)) {
                return item;
            }
        }
        return null;
    }

    private SharedFederatedAuthenticatorResolver getAuthenticatorResolver(FederatedAuthenticatorConfig authenticator) {

        return ConnectionSharingDataHolder.getInstance()
                .getSharedFederatedAuthenticatorResolver(authenticator.getName());
    }

    private SharedProvisioningConnectorResolver getConnectorResolver(ProvisioningConnectorConfig connector) {

        return ConnectionSharingDataHolder.getInstance()
                .getSharedProvisioningConnectorResolver(connector.getName());
    }

    private static <T> Map<String, T> indexByName(T[] items, Function<T, String> nameGetter) {

        Map<String, T> byName = new HashMap<>();
        if (items != null) {
            for (T item : items) {
                if (item != null && nameGetter.apply(item) != null) {
                    byName.put(nameGetter.apply(item), item);
                }
            }
        }
        return byName;
    }

    /**
     * A claim configuration is considered locally configured when it carries any non-default content (claim
     * mappings, IdP claims, a role/user claim URI, a local claim dialect flag or SP claim dialects).
     */
    private static boolean isClaimConfigLocallyConfigured(ClaimConfig claimConfig) {

        if (claimConfig == null) {
            return false;
        }
        return ArrayUtils.isNotEmpty(claimConfig.getClaimMappings())
                || ArrayUtils.isNotEmpty(claimConfig.getIdpClaims())
                || ArrayUtils.isNotEmpty(claimConfig.getSpClaimDialects())
                || StringUtils.isNotBlank(claimConfig.getRoleClaimURI())
                || StringUtils.isNotBlank(claimConfig.getUserClaimURI())
                || claimConfig.isLocalClaimDialect();
    }

    /**
     * A JIT provisioning configuration is considered locally configured when it carries any non-default content.
     */
    private static boolean isJitProvisioningConfigLocallyConfigured(JustInTimeProvisioningConfig jitConfig) {

        if (jitConfig == null) {
            return false;
        }
        return jitConfig.isProvisioningEnabled()
                || jitConfig.isPasswordProvisioningEnabled()
                || jitConfig.isModifyUserNameAllowed()
                || jitConfig.isPromptConsent()
                || jitConfig.isAssociateLocalUserEnabled()
                || jitConfig.isSkipJITOnAttrAccLookUpFailureEnabled()
                || StringUtils.isNotBlank(jitConfig.getUserStoreClaimUri())
                || StringUtils.isNotBlank(jitConfig.getProvisioningUserStore())
                || ArrayUtils.isNotEmpty(jitConfig.getAccountLookupAttributeMappings())
                || !IdPManagementConstants.DEFAULT_SYNC_ATTRIBUTE.equals(jitConfig.getAttributeSyncMethod())
                || !IdPManagementConstants.DEFAULT_SYNC_IDP_GROUP.equals(jitConfig.getIdpGroupSyncMethod());
    }

    /**
     * Verifies that the shadow's (inherited) JIT provisioning user store exists in the sub-organization and, when it
     * does not, falls back to the configurable default provisioning user store (read from identity.xml, defaulting to
     * the primary user store domain). This keeps JIT provisioning targeting a user store that actually exists in the
     * sub-organization, since the parent's provisioning user store need not be present there.
     *
     * @param shadowIdp    The (already overlaid) shadow identity provider whose JIT config is verified in place.
     * @param tenantDomain The tenant domain of the sub-organization the shadow belongs to.
     */
    private void resolveJitProvisioningUserStore(IdentityProvider shadowIdp, String tenantDomain) {

        JustInTimeProvisioningConfig jitConfig = shadowIdp.getJustInTimeProvisioningConfig();
        if (jitConfig == null) {
            return;
        }
        String provisioningUserStore = jitConfig.getProvisioningUserStore();
        if (StringUtils.isBlank(provisioningUserStore)
                || userStoreDomainExists(provisioningUserStore, tenantDomain)) {
            return;
        }

        String defaultProvisioningUserStore = getDefaultProvisioningUserStore();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Provisioning user store: " + provisioningUserStore + " of shared identity provider: "
                    + shadowIdp.getIdentityProviderName() + " does not exist in tenant: " + tenantDomain
                    + ". Falling back to the default provisioning user store: " + defaultProvisioningUserStore + ".");
        }
        jitConfig.setProvisioningUserStore(defaultProvisioningUserStore);
    }

    /**
     * Returns whether the given user store domain exists in the given tenant. The primary user store domain always
     * exists. On any lookup failure the domain is treated as existing (best-effort), so a transient realm error never
     * rewrites a possibly-valid provisioning user store.
     */
    private boolean userStoreDomainExists(String domain, String tenantDomain) {

        if (UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME.equalsIgnoreCase(domain)) {
            return true;
        }
        try {
            RealmService realmService = ConnectionSharingDataHolder.getInstance().getRealmService();
            UserRealm userRealm = realmService.getTenantUserRealm(IdentityTenantUtil.getTenantId(tenantDomain));
            if (userRealm == null) {
                return false;
            }
            org.wso2.carbon.user.api.UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            return userStoreManager instanceof UserStoreManager
                    && ((UserStoreManager) userStoreManager).getSecondaryUserStoreManager(domain) != null;
        } catch (UserStoreException e) {
            LOG.warn("Error while checking whether user store domain: " + domain + " exists in tenant: "
                    + tenantDomain + ". Keeping the configured provisioning user store.", e);
            return true;
        }
    }

    /**
     * Returns the configured default provisioning user store from identity.xml
     * ({@value #DEFAULT_PROVISIONING_USERSTORE_CONFIG}), or the primary user store domain when it is not configured.
     */
    private String getDefaultProvisioningUserStore() {

        String configuredUserStore = IdentityUtil.getProperty(DEFAULT_PROVISIONING_USERSTORE_CONFIG);
        return StringUtils.isNotBlank(configuredUserStore) ? configuredUserStore.trim()
                : UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
    }

    private static IdentityProviderProperty[] mergeProperties(IdentityProviderProperty[] parentProperties,
                                                              IdentityProviderProperty[] shadowProperties) {

        Map<String, IdentityProviderProperty> merged = new LinkedHashMap<>();
        if (parentProperties != null) {
            for (IdentityProviderProperty property : parentProperties) {
                merged.put(property.getName(), property);
            }
        }
        if (shadowProperties != null) {
            for (IdentityProviderProperty property : shadowProperties) {
                merged.put(property.getName(), property);
            }
        }
        return merged.values().toArray(new IdentityProviderProperty[0]);
    }
}
