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
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants;
import org.wso2.carbon.identity.organization.management.application.authn.internal.EnterpriseIDPAuthenticatorDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.CLIENT_ID;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.CLIENT_SECRET;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.OAUTH2_AUTHZ_URL;
import static org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants.OAUTH2_TOKEN_URL;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.OAuth2.CALLBACK_URL;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPAuthenticatorConstants.ORG_PARAMETER;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.ENTERPRISE_LOGIN_FAILURE;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.ERROR_MESSAGE;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.ErrorMessages.ENTERPRISE_IDP_LOGIN_FAILED;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.ErrorMessages.ORG_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.ErrorMessages.ORG_PARAMETER_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.REQUEST_ORG_PAGE_URL;
import static org.wso2.carbon.identity.organization.management.application.authn.constant.EnterpriseIDPErrorConstants.REQUEST_ORG_PAGE_URL_CONFIG;

/**
 * Authenticator implementation to redirect the authentication request to shared applications of the requested
 * organization.
 * <p/>
 * Class extends the {@link OpenIDConnectAuthenticator}.
 */
@SuppressFBWarnings
public class EnterpriseIDPAuthenticator extends OpenIDConnectAuthenticator {

    private static final Log log = LogFactory.getLog(EnterpriseIDPAuthenticator.class);

    @Override
    public String getFriendlyName() {

        return EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
    }

    @Override
    public String getName() {

        return EnterpriseIDPAuthenticatorConstants.AUTHENTICATOR_NAME;
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
                .put(ClaimMapping.build(EnterpriseIDPAuthenticatorConstants.ORGANIZATION_USER_ATTRIBUTE,
                                EnterpriseIDPAuthenticatorConstants.ORGANIZATION_USER_ATTRIBUTE,
                                null, false),
                        context.getAuthenticatorProperties()
                                .get(EnterpriseIDPAuthenticatorConstants.ORGANIZATION_ATTRIBUTE));
    }

    /**
     * Process the authenticator properties based on the user information.
     *
     * @param context The authentication context.
     */
    private void resolvePropertiesForEnterpriseIDP(AuthenticationContext context) throws AuthenticationFailedException {

        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

        try {
            String application = context.getServiceProviderName();
            String ownerTenant = context.getTenantDomain();

            Map<String, String> runtimeParams = getRuntimeParams(context);
            if (StringUtils.isBlank(runtimeParams.get(ORG_PARAMETER))) {
                throw new AuthenticationFailedException(ORG_NOT_FOUND.getCode(),
                        ORG_NOT_FOUND.getMessage());
            }
            String organizationName = runtimeParams.get(ORG_PARAMETER);

            // Get the shared service provider for the user's organization.
            ServiceProvider sharedApplication = getSharedApplication(organizationName, application, ownerTenant);

            InboundAuthenticationRequestConfig oidcConfigurations = getAuthenticationConfig(
                    sharedApplication).orElseThrow(() -> handleAuthFailures(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                    ENTERPRISE_IDP_LOGIN_FAILED.getMessage()));

            // Update the authenticator configurations based on the user's organization.
            String clientId = oidcConfigurations.getInboundAuthKey();
            OAuthAdminServiceImpl oAuthAdminService = EnterpriseIDPAuthenticatorDataHolder.getInstance()
                    .getOAuthAdminService();
            OAuthConsumerAppDTO oauthApp = oAuthAdminService.getOAuthApplicationData(clientId);

            authenticatorProperties.put(CLIENT_ID, clientId);
            authenticatorProperties.put(CLIENT_SECRET, oauthApp.getOauthConsumerSecret());

            authenticatorProperties.put(EnterpriseIDPAuthenticatorConstants.ORGANIZATION_ATTRIBUTE, organizationName);
            authenticatorProperties.put(OAUTH2_AUTHZ_URL, getAuthorizationEndpoint(organizationName));
            authenticatorProperties.put(OAUTH2_TOKEN_URL, getTokenEndpoint(organizationName));
            authenticatorProperties.put(CALLBACK_URL, getCallbackUrl());

        } catch (IdentityOAuthAdminException | URLBuilderException e) {
            throw new AuthenticationFailedException(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                    ENTERPRISE_IDP_LOGIN_FAILED.getMessage(), e);
        }
    }

    @Override
    public AuthenticatorFlowStatus process(HttpServletRequest request, HttpServletResponse response,
                                           AuthenticationContext context) throws AuthenticationFailedException,
            LogoutFailedException {

        if (context.isLogoutRequest()) {
            return AuthenticatorFlowStatus.SUCCESS_COMPLETED;
        }

        Map<String, String> runtimeParams = getRuntimeParams(context);
        if (request.getParameterMap().containsKey(ORG_PARAMETER)) {
            String organizationName = request.getParameter(ORG_PARAMETER);
            runtimeParams.put(ORG_PARAMETER, organizationName);
        }

        if (StringUtils.isBlank(runtimeParams.get(ORG_PARAMETER)) ||
                !validateOrganization(runtimeParams.get(ORG_PARAMETER), context)) {
            if (log.isDebugEnabled()) {
                log.debug(ORG_PARAMETER_NOT_FOUND.getMessage());
            }
            redirectToOrgNameCapture(response, context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        }
        return super.process(request, response, context);

    }

    private boolean validateOrganization(String organizationName, AuthenticationContext context)
            throws AuthenticationFailedException {

        try {
            boolean exist = EnterpriseIDPAuthenticatorDataHolder.getInstance().
                    getOrganizationManager().isOrganizationExistById(organizationName);
            if (!exist) {
                context.setProperty(ENTERPRISE_LOGIN_FAILURE, "Invalid Organization Name");
            }
            return exist;
        } catch (OrganizationManagementException e) {
            throw new AuthenticationFailedException("Error retrieving the organization: " + organizationName, e);
        }
    }

    /**
     * When the organization name is not found or invalid, this method construct the redirect URL to capture the
     * organization name.
     *
     * @param response servlet response.
     * @param context  authentication context.
     * @throws AuthenticationFailedException
     */
    private void redirectToOrgNameCapture(HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        try {
            StringBuilder queryStringBuilder = new StringBuilder();

            queryStringBuilder.append("sessionDataKey=").append(context.getContextIdentifier()).append("&idp=")
                    .append(context.getExternalIdP().getIdPName()).append("&authenticator=").append(getName());

            if (context.getProperties().get(ENTERPRISE_LOGIN_FAILURE) != null) {
                queryStringBuilder.append(ERROR_MESSAGE).append(URLEncoder
                        .encode((String) context.getProperties().get(ENTERPRISE_LOGIN_FAILURE), "utf-8"));
            }
            response.sendRedirect(FrameworkUtils.appendQueryParamsStringToUrl(getOrganizationRequestPageUrl(context),
                    queryStringBuilder.toString()));
        } catch (IOException | URLBuilderException e) {
            throw new AuthenticationFailedException(
                    "Error while redirecting to request organization page. ", e);
        }
    }

    /**
     * Returns the shared application details based on the given organization name, main application and owner
     * organization of the main application.
     *
     * @param organizationName  organization which owns the shared application.
     * @param application       main application.
     * @param ownerOrganization organization which owns the main application.
     * @return shared application, instance of {@link ServiceProvider}.
     * @throws AuthenticationFailedException if the application is not found, authentication failed exception will be
     *                                       thrown.
     */
    private ServiceProvider getSharedApplication(String organizationName, String application,
                                                 String ownerOrganization) throws AuthenticationFailedException {

        ApplicationManagementService applicationManagementService =
                EnterpriseIDPAuthenticatorDataHolder.getInstance()
                        .getApplicationManagementService();
        try {
            String sharedApplicationId = EnterpriseIDPAuthenticatorDataHolder.getInstance()
                    .getOrgApplicationManager().resolveSharedAppResourceId(organizationName, application,
                            // Use proper error code/message.
                            ownerOrganization).orElseThrow(() -> handleAuthFailures(
                            ENTERPRISE_IDP_LOGIN_FAILED.getCode(), ENTERPRISE_IDP_LOGIN_FAILED.getMessage()));
            return Optional.ofNullable(applicationManagementService.getApplicationByResourceId(sharedApplicationId,
                    // Use proper error code/message.
                    organizationName)).orElseThrow(() -> handleAuthFailures(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                    ENTERPRISE_IDP_LOGIN_FAILED.getMessage()));

        } catch (IdentityApplicationManagementException | OrganizationManagementException e) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Error on getting outbound service provider of the organization. "
                        + "Organization: %s, Inbound service provider: %s", organizationName, application));
            }
            throw new AuthenticationFailedException(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                    ENTERPRISE_IDP_LOGIN_FAILED.getMessage(), e);
        }
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

        return Arrays.stream(inbounds).filter(inbound -> "oauth2".equals(inbound.getInboundAuthType())).findAny();
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
     * @param organizationName Name of the organization.
     * @return The authorization endpoint URL.
     */
    private String getAuthorizationEndpoint(String organizationName) throws URLBuilderException {

        return ServiceURLBuilder.create()
                .addPath(EnterpriseIDPAuthenticatorConstants.AUTHORIZATION_ENDPOINT_TENANTED_PATH
                        .replace(EnterpriseIDPAuthenticatorConstants.TENANT_PLACEHOLDER, organizationName)).build()
                .getAbsolutePublicURL();
    }

    /**
     * Returns the token endpoint url for a given organization.
     *
     * @param organizationName Name of the organization.
     * @return The token endpoint URL.
     */
    private String getTokenEndpoint(String organizationName) throws URLBuilderException {

        return ServiceURLBuilder.create()
                .addPath(EnterpriseIDPAuthenticatorConstants.TOKEN_ENDPOINT_TENANTED_PATH
                        .replace(EnterpriseIDPAuthenticatorConstants.TENANT_PLACEHOLDER, organizationName)).build()
                .getAbsolutePublicURL();
    }

    /**
     * Returns the token endpoint url for a given organization.
     *
     * @return The callback URL.
     */
    private String getCallbackUrl() throws URLBuilderException {

        return ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build().getAbsolutePublicURL();
    }

    /**
     * Get the request organization page url from the application-authentication.xml file.
     *
     * @param context the AuthenticationContext
     * @return loginPage
     */
    private String getOrganizationRequestPageUrl(AuthenticationContext context) throws URLBuilderException {

        String requestOrgPageUrl = getConfiguration(context, REQUEST_ORG_PAGE_URL_CONFIG);
        return StringUtils.isNotBlank(requestOrgPageUrl) ? requestOrgPageUrl :
                ServiceURLBuilder.create().addPath(REQUEST_ORG_PAGE_URL).build().getAbsolutePublicURL();
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

    private AuthenticationFailedException handleAuthFailures(String errorCode, String errorMessage) {

        if (log.isDebugEnabled()) {
            log.debug(errorMessage);
        }
        return new AuthenticationFailedException(errorCode, errorMessage);
    }
}
