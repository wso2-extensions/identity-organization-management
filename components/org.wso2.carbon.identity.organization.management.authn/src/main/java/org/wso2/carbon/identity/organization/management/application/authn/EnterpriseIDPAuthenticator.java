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

package org.wso2.carbon.identity.organization.management.application.authn;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.extension.identity.helper.IdentityHelperConstants;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.authn.internal.EnterpriseIDPAuthenticatorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.SESSION_DATA_KEY;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.CLIENT_ID;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.CLIENT_SECRET;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.OAUTH2_AUTHZ_URL;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.OAUTH2_TOKEN_URL;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.OAuth2.CALLBACK_URL;
import static org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantId;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AMPERSAND_SIGN;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_NAME;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.AUTHORIZATION_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.CLIENT_ID_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ENTERPRISE_LOGIN_FAILURE;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.EQUAL_SIGN;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ERROR_MESSAGE;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.IDP_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.INBOUND_AUTH_TYPE_OAUTH;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_ID_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_USER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_ID_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_LIST_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_NAME_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.REDIRECT_URI_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.REQUEST_ORG_PAGE_URL;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.REQUEST_ORG_PAGE_URL_CONFIG;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.REQUEST_ORG_SELECT_PAGE_URL;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.RESPONSE_TYPE_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.SCOPE_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.TOKEN_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.UTF_8;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_REQUEST_ORGANIZATION_REDIRECT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_ENTERPRISE_IDP_LOGIN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_NAME_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORG_PARAMETER_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ROOT_ORG_ID;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

/**
 * Authenticator implementation to redirect the authentication request to shared applications of the requested
 * organization.
 * <p/>
 * Class extends the {@link OpenIDConnectAuthenticator}.
 */
public class EnterpriseIDPAuthenticator extends OpenIDConnectAuthenticator {

    private static final Log log = LogFactory.getLog(EnterpriseIDPAuthenticator.class);

    @Override
    public String getFriendlyName() {

        return AUTHENTICATOR_FRIENDLY_NAME;
    }

    @Override
    public String getName() {

        return AUTHENTICATOR_NAME;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {

        resolvePropertiesForEnterpriseIDP(context);
        super.initiateAuthenticationRequest(request, response, context);
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) throws AuthenticationFailedException {

        resolvePropertiesForEnterpriseIDP(context);
        super.processAuthenticationResponse(request, response, context);

        // Add organization name to the user attributes.
        context.getSubject().getUserAttributes()
                .put(ClaimMapping.build(ORGANIZATION_USER_ATTRIBUTE, ORGANIZATION_USER_ATTRIBUTE, null, false),
                        context.getAuthenticatorProperties().get(ORGANIZATION_ATTRIBUTE));
    }

    /**
     * Process the authenticator properties based on the user information.
     *
     * @param context The authentication context.
     * @throws AuthenticationFailedException The exception thrown when resolving EnterpriseIDP login properties
     */
    private void resolvePropertiesForEnterpriseIDP(AuthenticationContext context) throws AuthenticationFailedException {

        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

        try {
            String mainAppName = context.getServiceProviderName();

            Map<String, String> runtimeParams = getRuntimeParams(context);
            String sharedOrgId = runtimeParams.get(ORG_ID_PARAMETER);
            String sharedOrgName = runtimeParams.get(ORG_NAME_PARAMETER);
            if (StringUtils.isBlank(sharedOrgId)) {
                throw handleAuthFailures(ERROR_CODE_ORG_PARAMETER_NOT_FOUND);
            }

            // Get the shared service provider based on the requested organization.
            ServiceProvider sharedApplication = getSharedApplication(mainAppName, sharedOrgId);

            InboundAuthenticationRequestConfig oidcConfigurations =
                    getAuthenticationConfig(sharedApplication).orElseThrow(
                            () -> handleAuthFailures(ERROR_CODE_INVALID_APPLICATION));

            // Update the authenticator configurations based on the user's organization.
            String clientId = oidcConfigurations.getInboundAuthKey();
            OAuthConsumerAppDTO oauthApp = getOAuthAdminService().getOAuthApplicationData(clientId);

            String sharedOrgTenantDomain = getTenantDomain(sharedOrgId);
            authenticatorProperties.put(CLIENT_ID, clientId);
            authenticatorProperties.put(CLIENT_SECRET, oauthApp.getOauthConsumerSecret());
            authenticatorProperties.put(ORGANIZATION_ATTRIBUTE, sharedOrgName);
            authenticatorProperties.put(OAUTH2_AUTHZ_URL, getAuthorizationEndpoint(sharedOrgTenantDomain));
            authenticatorProperties.put(OAUTH2_TOKEN_URL, getTokenEndpoint(sharedOrgTenantDomain));
            authenticatorProperties.put(CALLBACK_URL, createCallbackUrl());

        } catch (IdentityOAuthAdminException | URLBuilderException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_RESOLVING_ENTERPRISE_IDP_LOGIN, e);
        }
    }

    private String getOrgIdByTenantDomain(String tenantDomain) throws AuthenticationFailedException {

        if (SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain)) {
            return ROOT_ORG_ID;
        }
        int tenantId = getTenantId(tenantDomain);
        try {
            Tenant tenant = getRealmService().getTenantManager().getTenant(tenantId);
            return tenant.getAssociatedOrganizationUUID();
        } catch (UserStoreException e) {
            throw handleAuthFailures(ERROR_CODE_INVALID_ORGANIZATION, e);
        }
    }

    private String getTenantDomain(String organizationID) throws AuthenticationFailedException {

        try {
            return getOrganizationManager().resolveTenantDomain(organizationID);
        } catch (OrganizationManagementException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN);
        }
    }

    @Override
    public AuthenticatorFlowStatus process(HttpServletRequest request, HttpServletResponse response,
                                           AuthenticationContext context) throws AuthenticationFailedException,
            LogoutFailedException {

        if (context.isLogoutRequest()) {
            super.processLogoutResponse(request, response, context);
            return AuthenticatorFlowStatus.SUCCESS_COMPLETED;
        }
        Map<String, String> runtimeParams = getRuntimeParams(context);

        if (StringUtils.isBlank(runtimeParams.get(ORG_ID_PARAMETER))) {
            // todo use resolveOrganizationId(tenant domain) of organization management service

            String orgId = IdentityTenantUtil.getTenantDomainFromContext();
            if (orgId == null || SUPER_TENANT_DOMAIN_NAME.equals(orgId)) {
                orgId = getOrgIdByTenantDomain(SUPER_TENANT_DOMAIN_NAME);
            }

            String tenantDomain = getTenantDomain(orgId);
            if (isSaasAppOwnedByTenant(context.getServiceProviderName(), tenantDomain)) {
                if (StringUtils.isNotBlank(request.getParameter(ORG_PARAMETER))) {
                    String org = request.getParameter(ORG_PARAMETER);
                    if (!resolveOrganizationByName(org, context, response)) {
                        return AuthenticatorFlowStatus.INCOMPLETE;
                    }
                    redirectToSubOrganization(context, response, runtimeParams.get(ORG_ID_PARAMETER));
                    return AuthenticatorFlowStatus.INCOMPLETE;
                }
                redirectToOrgNameCapture(response, context);
                return AuthenticatorFlowStatus.INCOMPLETE;
            }
            String orgName = getOrganizationNameById(orgId);
            runtimeParams.put(ORG_ID_PARAMETER, orgId);
            runtimeParams.put(ORG_NAME_PARAMETER, orgName);
        }
        return super.process(request, response, context);
    }

    private String getOrganizationNameById(String organizationId) throws AuthenticationFailedException {

        try {
            return getOrganizationManager().getOrganizationNameById(organizationId);
        } catch (OrganizationManagementException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_NAME_BY_ID, organizationId, e);
        }
    }

    private boolean resolveOrganizationByName(String org, AuthenticationContext context, HttpServletResponse response)
            throws AuthenticationFailedException {

        Map<String, String> runtimeParams = getRuntimeParams(context);
        try {
            // Also, org name can be provided. ex - From org name capture login page.
            List<Organization> organizations = getOrganizationManager().getOrganizationsByName(org);
            if (CollectionUtils.isNotEmpty(organizations)) {
                if (organizations.size() == 1) {
                    runtimeParams.put(ORG_NAME_PARAMETER, org);
                    runtimeParams.put(ORG_ID_PARAMETER, organizations.get(0).getId());
                    return true;
                }
                List<String> orgDetails = organizations.stream()
                        .map(organization -> organization.getId() + ":" + organization.getName() + ":" +
                                organization.getDescription()).collect(Collectors.toList());
                redirectToSelectOrganization(response, context, String.join(",", orgDetails));
            }
        } catch (OrganizationManagementException ex) {
            context.setProperty(ENTERPRISE_LOGIN_FAILURE, "Invalid Organization Name");
            redirectToOrgNameCapture(response, context);
        }
        return false;
    }

    /**
     * When the organization name is not found or invalid, this method construct the redirect URL to capture the
     * organization name.
     *
     * @param response servlet response.
     * @param context  authentication context.
     * @throws AuthenticationFailedException on errors when setting the redirect URL to capture the organization name.
     */
    @SuppressFBWarnings(value = "UNVALIDATED_REDIRECT", justification = "Redirect params are not based on user inputs.")
    private void redirectToOrgNameCapture(HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        try {
            StringBuilder queryStringBuilder = new StringBuilder();
            queryStringBuilder.append(SESSION_DATA_KEY).append("=").append(urlEncode(context.getContextIdentifier()))
                    .append("&").append("idp").append("=").append(context.getExternalIdP().getName()).append("&")
                    .append("authenticator").append("=").append(getName());

            if (context.getProperties().get(ENTERPRISE_LOGIN_FAILURE) != null) {
                queryStringBuilder.append(ERROR_MESSAGE)
                        .append(urlEncode((String) context.getProperties().get(ENTERPRISE_LOGIN_FAILURE)));
            }

            String url = FrameworkUtils.appendQueryParamsStringToUrl(getOrganizationRequestPageUrl(context),
                    queryStringBuilder.toString());
            response.sendRedirect(url);
        } catch (IOException | URLBuilderException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_REQUEST_ORGANIZATION_REDIRECT, e);
        }
    }

    @SuppressFBWarnings(value = "UNVALIDATED_REDIRECT", justification = "Redirect params are not based on user inputs.")
    private void redirectToSelectOrganization(HttpServletResponse response, AuthenticationContext context,
                                              String orgDetails)
            throws AuthenticationFailedException {

        try {
            StringBuilder queryStringBuilder = new StringBuilder();
            queryStringBuilder.append(SESSION_DATA_KEY).append(EQUAL_SIGN)
                    .append(urlEncode(context.getContextIdentifier()));
            buildQueryParam(queryStringBuilder, IDP_PARAMETER, context.getExternalIdP().getName());
            buildQueryParam(queryStringBuilder, AUTHENTICATOR_PARAMETER, getName());
            buildQueryParam(queryStringBuilder, ORG_LIST_PARAMETER, urlEncode(orgDetails));

            String url = FrameworkUtils.appendQueryParamsStringToUrl(ServiceURLBuilder.create()
                            .addPath(REQUEST_ORG_SELECT_PAGE_URL).build().getAbsolutePublicURL(),
                    queryStringBuilder.toString());
            response.sendRedirect(url);
        } catch (IOException | URLBuilderException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_REQUEST_ORGANIZATION_REDIRECT, e);
        }
    }

    @SuppressFBWarnings(value = "UNVALIDATED_REDIRECT", justification = "Redirect params are not based on user inputs.")
    private void redirectToSubOrganization(AuthenticationContext context, HttpServletResponse response,
                                           String organizationId) throws AuthenticationFailedException {

        try {
            Map<String, String> queryMap = getQueryMap(context.getQueryParams());
            StringBuilder queryStringBuilder = new StringBuilder();
            queryStringBuilder.append(IDP_PARAMETER).append(EQUAL_SIGN).append(context.getExternalIdP().getName());
            buildQueryParam(queryStringBuilder, CLIENT_ID_PARAMETER, queryMap.get(CLIENT_ID_PARAMETER));
            buildQueryParam(queryStringBuilder, REDIRECT_URI_PARAMETER, queryMap.get(REDIRECT_URI_PARAMETER));
            buildQueryParam(queryStringBuilder, RESPONSE_TYPE_PARAMETER, queryMap.get(RESPONSE_TYPE_PARAMETER));
            buildQueryParam(queryStringBuilder, SCOPE_PARAMETER, queryMap.get(SCOPE_PARAMETER));

            String url = FrameworkUtils.appendQueryParamsStringToUrl(getAuthorizationEndpoint(organizationId),
                    queryStringBuilder.toString());
            response.sendRedirect(url);
        } catch (IOException | URLBuilderException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_REQUEST_ORGANIZATION_REDIRECT, e);
        }
    }

    private void buildQueryParam(StringBuilder builder, String query, String param) {

        builder.append(AMPERSAND_SIGN).append(query).append(EQUAL_SIGN).append(param);
    }

    private Map<String, String> getQueryMap(String query) throws UnsupportedEncodingException {

        String[] params = query.split(AMPERSAND_SIGN);
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String[] keyValPairs = param.split(EQUAL_SIGN);
            if (keyValPairs.length == 2) {
                map.put(keyValPairs[0], URLDecoder.decode(keyValPairs[1], UTF_8));
            }
        }
        return map;
    }

    /**
     * Returns the shared application details based on the given organization name, main application and owner
     * organization of the main application.
     *
     * @param application Main application.
     * @param sharedOrgId Identifier of the organization which owns the shared application.
     * @return shared application, instance of {@link ServiceProvider}.
     * @throws AuthenticationFailedException if the application is not found, authentication failed exception will be
     *                                       thrown.
     */
    private ServiceProvider getSharedApplication(String application, String sharedOrgId)
            throws AuthenticationFailedException {

        try {
            return getOrgApplicationManager().resolveSharedApplication(application, sharedOrgId);
        } catch (OrganizationManagementException e) {
            throw handleAuthFailures(ERROR_CODE_ERROR_RETRIEVING_APPLICATION, e);
        }
    }

    private boolean isSaasAppOwnedByTenant(String mainAppName, String tenantDomain) {

        return getOrgApplicationManager().isSaasAppOwnedByTenant(mainAppName, tenantDomain);
    }

    /**
     * Obtain inbound authentication configuration of the application registered for the organization.
     *
     * @param application Enterprise login management application.
     * @return InboundAuthenticationRequestConfig  Inbound authentication request configurations.
     */
    private Optional<InboundAuthenticationRequestConfig> getAuthenticationConfig(ServiceProvider application) {

        InboundAuthenticationConfig inboundAuthConfig = application.getInboundAuthenticationConfig();
        if (inboundAuthConfig == null) {
            return Optional.empty();
        }

        InboundAuthenticationRequestConfig[] inbounds = inboundAuthConfig.getInboundAuthenticationRequestConfigs();
        if (inbounds == null) {
            return Optional.empty();
        }

        return Arrays.stream(inbounds).filter(inbound -> INBOUND_AUTH_TYPE_OAUTH.equals(inbound.getInboundAuthType()))
                .findAny();
    }

    /**
     * Get Configuration Properties.
     */
    @Override
    public List<Property> getConfigurationProperties() {

        return Collections.emptyList();
    }

    /**
     * Returns the authorization endpoint url for a given organization.
     *
     * @param organizationId Organization Id.
     * @return The authorization endpoint URL.
     */
    private String getAuthorizationEndpoint(String organizationId) throws URLBuilderException {

        return ServiceURLBuilder.create().addPath(AUTHORIZATION_ENDPOINT_TENANTED_PATH
                .replace(ORGANIZATION_ID_PLACEHOLDER, organizationId)).build().getAbsolutePublicURL();
    }

    /**
     * Returns the token endpoint url for a given organization.
     *
     * @param organizationId Organization Id.
     * @return The token endpoint URL.
     */
    private String getTokenEndpoint(String organizationId) throws URLBuilderException {

        return ServiceURLBuilder.create().addPath(TOKEN_ENDPOINT_TENANTED_PATH.replace(ORGANIZATION_ID_PLACEHOLDER,
                organizationId)).build().getAbsolutePublicURL();
    }

    /**
     * Returns the token endpoint url for a given organization.
     *
     * @return The callback URL.
     */
    private String createCallbackUrl() throws URLBuilderException {

        return ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build().getAbsolutePublicURL();
    }

    /**
     * Get the request organization page url from the application-authentication.xml file.
     *
     * @param context the AuthenticationContext
     * @return The url path to request organization name.
     */
    private String getOrganizationRequestPageUrl(AuthenticationContext context) throws URLBuilderException {

        String requestOrgPageUrl = getConfiguration(context, REQUEST_ORG_PAGE_URL_CONFIG);
        if (StringUtils.isBlank(requestOrgPageUrl)) {
            requestOrgPageUrl = REQUEST_ORG_PAGE_URL;
        }
        return ServiceURLBuilder.create().addPath(requestOrgPageUrl).build().getAbsolutePublicURL();
    }

    private String urlEncode(String value) throws UnsupportedEncodingException {

        return URLEncoder.encode(value, FrameworkUtils.UTF_8);
    }

    /**
     * Read configurations from application-authentication.xml for given authenticator.
     *
     * @param context    Authentication Context.
     * @param configName Name of the config.
     * @return Config value.
     */
    private String getConfiguration(AuthenticationContext context, String configName) {

        String configValue = null;
        Object propertiesFromLocal = context.getProperty(IdentityHelperConstants.GET_PROPERTY_FROM_REGISTRY);
        String tenantDomain = context.getTenantDomain();
        if ((propertiesFromLocal != null || MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) &&
                super.getAuthenticatorConfig().getParameterMap().containsKey(configName)) {
            configValue = super.getAuthenticatorConfig().getParameterMap().get(configName);
        } else if ((context.getProperty(configName)) != null) {
            configValue = String.valueOf(context.getProperty(configName));
        }
        if (log.isDebugEnabled()) {
            log.debug("Config value for key " + configName + " for tenant " + tenantDomain + " : " + configValue);
        }
        return configValue;
    }

    private AuthenticationFailedException handleAuthFailures(OrganizationManagementConstants.ErrorMessages error) {

        return handleAuthFailures(error, null);
    }

    private AuthenticationFailedException handleAuthFailures(OrganizationManagementConstants.ErrorMessages error,
                                                             Throwable e) {

        if (log.isDebugEnabled()) {
            log.debug(error.getMessage());
        }
        return new AuthenticationFailedException(error.getCode(), error.getMessage(), e);
    }

    private AuthenticationFailedException handleAuthFailures(OrganizationManagementConstants.ErrorMessages error,
                                                             String data, Throwable e) {

        if (log.isDebugEnabled()) {
            log.debug(error.getMessage());
        }
        return new AuthenticationFailedException(error.getCode(), String.format(error.getMessage(), data), e);
    }

    private RealmService getRealmService() {

        return EnterpriseIDPAuthenticatorDataHolder.getInstance().getRealmService();
    }

    private OAuthAdminServiceImpl getOAuthAdminService() {

        return EnterpriseIDPAuthenticatorDataHolder.getInstance().getOAuthAdminService();
    }

    private OrgApplicationManager getOrgApplicationManager() {

        return EnterpriseIDPAuthenticatorDataHolder.getInstance().getOrgApplicationManager();
    }

    private OrganizationManager getOrganizationManager() {

        return EnterpriseIDPAuthenticatorDataHolder.getInstance().getOrganizationManager();
    }
}
