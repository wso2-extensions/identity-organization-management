/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.application.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.AttributeMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_DEFAULT;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_FLOW;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ORGANIZATION_LOGIN_AUTHENTICATOR;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.USER_ORGANIZATION_CLAIM;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.USER_ORGANIZATION_CLAIM_URI;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_ORG_LOGIN_IDP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_IDP_LIST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;
import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.IS_SYSTEM_RESERVED_IDP_DISPLAY_NAME;
import static org.wso2.carbon.idp.mgt.util.IdPManagementConstants.IS_SYSTEM_RESERVED_IDP_FLAG;
import static org.wso2.carbon.user.core.UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;

/**
 * This class provides utility functions for the Organization Application Management.
 */
public class OrgApplicationManagerUtil {

    /**
     * Get a new Jdbc Template.
     *
     * @return a new Jdbc Template.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
    }

    /**
     * Set property value to service provider indicating if it should be shared with all child organizations.
     *
     * @param serviceProvider The main application.
     * @param value           Property value.
     */
    public static void setShareWithAllChildrenProperty(ServiceProvider serviceProvider, boolean value) {

        Optional<ServiceProviderProperty> shareWithAllChildren = stream(serviceProvider.getSpProperties())
                .filter(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()))
                .findFirst();
        if (shareWithAllChildren.isPresent()) {
            shareWithAllChildren.get().setValue(Boolean.toString(value));
            return;
        }
        ServiceProviderProperty[] spProperties = serviceProvider.getSpProperties();
        ServiceProviderProperty[] newSpProperties = new ServiceProviderProperty[spProperties.length + 1];
        System.arraycopy(spProperties, 0, newSpProperties, 0, spProperties.length);

        ServiceProviderProperty shareWithAllChildrenProperty = new ServiceProviderProperty();
        shareWithAllChildrenProperty.setName(SHARE_WITH_ALL_CHILDREN);
        shareWithAllChildrenProperty.setValue(Boolean.TRUE.toString());
        newSpProperties[spProperties.length] = shareWithAllChildrenProperty;
        serviceProvider.setSpProperties(newSpProperties);
    }

    public static void addUserOrganizationClaim(String tenantDomain) throws OrganizationManagementServerException {

        try {
            Optional<LocalClaim> optionalLocalClaim = OrgApplicationMgtDataHolder.getInstance()
                    .getClaimMetadataManagementService().getLocalClaims(tenantDomain)
                    .stream().filter(localClaim -> USER_ORGANIZATION_CLAIM_URI.equals(localClaim.getClaimURI()))
                    .findAny();
            if (optionalLocalClaim.isPresent()) {
                return;
            }
            List<AttributeMapping> attributeMappings = new ArrayList<>();
            attributeMappings.add(new AttributeMapping(PRIMARY_DEFAULT_DOMAIN_NAME, USER_ORGANIZATION_CLAIM));
            Map<String, String> claimProperties = new HashMap<>();
            claimProperties.put("DisplayName", "Organization");
            claimProperties.put("Description", "Local claim for user organization identifier");
            OrgApplicationMgtDataHolder.getInstance().getClaimMetadataManagementService()
                    .addLocalClaim(new LocalClaim(USER_ORGANIZATION_CLAIM_URI, attributeMappings, claimProperties),
                            tenantDomain);
        } catch (ClaimMetadataException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e);
        }
    }

    public static IdentityProvider addOrganizationSSOIdpIfNotExist(String tenantDomain)
            throws OrganizationManagementServerException, OrganizationManagementClientException {

        try {
            IdentityProvider[] idpList = OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService()
                    .getAllIdentityProviders(tenantDomain);
            Optional<IdentityProvider> maybeOrganizationIDP = stream(idpList)
                    .filter(OrgApplicationManagerUtil::isOrganizationLoginIDP).findFirst();
            if (maybeOrganizationIDP.isPresent()) {
                return maybeOrganizationIDP.get();
            }
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_IDP_LIST, e, getOrganizationId());
        }

        // Add organization SSO IDP.
        try {
            return OrgApplicationMgtDataHolder.getInstance().getIdpManager()
                    .addIdPWithResourceId(createOrganizationSSOIDP(), tenantDomain);
        } catch (IdentityProviderManagementClientException e) {
            throw new OrganizationManagementClientException(e.getMessage(), e.getMessage(), e.getErrorCode());
        } catch (IdentityProviderManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATING_ORG_LOGIN_IDP, e, getOrganizationId());
        }
    }

    /**
     * This method will update the root application by adding the organization login authenticator.
     *
     * @param serviceProvider The service provider object of the B2B application.
     * @param organizationSsoIdp The Organization SSO IDP.
     * @throws OrganizationManagementException
     */
    public static void addOrganizationSSOIdpToSP(ServiceProvider serviceProvider, IdentityProvider organizationSsoIdp)
            throws OrganizationManagementException {

        LocalAndOutboundAuthenticationConfig outboundAuthenticationConfig =
                serviceProvider.getLocalAndOutBoundAuthenticationConfig();
        AuthenticationStep[] authSteps = outboundAuthenticationConfig.getAuthenticationSteps();
        if (authSteps.length == 0) {
            authSteps = getDefaultAuthenticationSteps();
        }
        boolean orgSSOIdpConfigured = stream(authSteps[0].getFederatedIdentityProviders())
                .anyMatch(idp -> idp.getDefaultAuthenticatorConfig().getName()
                        .equals(ORGANIZATION_LOGIN_AUTHENTICATOR));
        if (orgSSOIdpConfigured) {
            return;
        }

        if (StringUtils.equalsIgnoreCase(outboundAuthenticationConfig.getAuthenticationType(), AUTH_TYPE_DEFAULT)) {
            // Change the authType to flow, since we are adding organization login authenticator.
            outboundAuthenticationConfig.setAuthenticationType(AUTH_TYPE_FLOW);
        }

        authSteps[0].setFederatedIdentityProviders(
                (IdentityProvider[]) ArrayUtils.addAll(authSteps[0].getFederatedIdentityProviders(),
                        new IdentityProvider[]{organizationSsoIdp}));
        outboundAuthenticationConfig.setAuthenticationSteps(authSteps);
    }

    private static IdentityProvider createOrganizationSSOIDP() {

        FederatedAuthenticatorConfig authConfig = new FederatedAuthenticatorConfig();
        authConfig.setName(ORGANIZATION_LOGIN_AUTHENTICATOR);
        authConfig.setDisplayName(ORGANIZATION_LOGIN_AUTHENTICATOR);
        authConfig.setEnabled(true);

        IdentityProvider idp = new IdentityProvider();
        idp.setIdentityProviderName("SSO");
        idp.setPrimary(false);
        idp.setFederationHub(false);
        idp.setIdentityProviderDescription("Identity provider for Organization SSO.");
        idp.setHomeRealmId("OrganizationSSO");
        idp.setDefaultAuthenticatorConfig(authConfig);
        idp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{authConfig});
        ClaimConfig claimConfig = new ClaimConfig();
        claimConfig.setLocalClaimDialect(true);
        idp.setClaimConfig(claimConfig);

        // Add system reserved properties.
        IdentityProviderProperty[] idpProperties = new IdentityProviderProperty[1];
        IdentityProviderProperty property = new IdentityProviderProperty();
        property.setName(IS_SYSTEM_RESERVED_IDP_FLAG);
        property.setDisplayName(IS_SYSTEM_RESERVED_IDP_DISPLAY_NAME);
        property.setValue("true");
        idpProperties[0] = property;
        idp.setIdpProperties(idpProperties);
        return idp;
    }

    private static boolean isOrganizationLoginIDP(IdentityProvider idp) {

        FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = idp.getFederatedAuthenticatorConfigs();
        return ArrayUtils.isNotEmpty(federatedAuthenticatorConfigs) &&
                ORGANIZATION_LOGIN_AUTHENTICATOR.equals(federatedAuthenticatorConfigs[0].getName());
    }

    private static AuthenticationStep[] getDefaultAuthenticationSteps() throws OrganizationManagementServerException {

        ServiceProvider defaultSP = getDefaultServiceProvider();
        return defaultSP != null
                ? defaultSP.getLocalAndOutBoundAuthenticationConfig().getAuthenticationSteps()
                : new AuthenticationStep[0];
    }

    private static ServiceProvider getDefaultServiceProvider() throws OrganizationManagementServerException {

        try {
            return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService()
                    .getServiceProvider(IdentityApplicationConstants.DEFAULT_SP_CONFIG,
                            MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        } catch (IdentityApplicationManagementException e) {
            throw new OrganizationManagementServerException("Error while retrieving default service provider", null, e);
        }
    }
}

