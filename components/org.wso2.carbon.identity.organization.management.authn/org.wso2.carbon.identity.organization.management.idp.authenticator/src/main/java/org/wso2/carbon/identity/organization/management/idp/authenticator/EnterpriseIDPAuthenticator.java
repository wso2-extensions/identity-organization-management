/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.idp.authenticator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
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
import org.wso2.carbon.identity.organization.management.idp.authenticator.internal.EnterpriseIDPAuthenticatorDataHolder;
import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementException;

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
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.AUTHORIZATION_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_USER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ORG_PARAMETER;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.TENANT_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.TOKEN_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.ENTERPRISE_IDP_LOGIN_FAILED;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.ORG_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.ORG_PARAMETER_NOT_FOUND;


/**
 * Enterprise IDP Login Authenticator.
 */
public class EnterpriseIDPAuthenticator extends OpenIDConnectAuthenticator {

    private static final long serialVersionUID = -1855408304527154250L;

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
                .put(ClaimMapping.build(ORGANIZATION_USER_ATTRIBUTE, ORGANIZATION_USER_ATTRIBUTE,
                        null, false), context.getAuthenticatorProperties().get(ORGANIZATION_ATTRIBUTE));
    }

    /**
     * Process the authenticator properties based on the user information.
     *
     * @param context The authentication context.
     */
    private void resolvePropertiesForEnterpriseIDP(AuthenticationContext context) throws AuthenticationFailedException {

        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

        try {
            String organizationName;
            String inboundSp = context.getServiceProviderName();
            String inboundSpTenant = context.getTenantDomain();

            Map<String, String> runtimeParams = getRuntimeParams(context);
            if (!StringUtils.isNotBlank(runtimeParams.get(ORG_PARAMETER))) {
                throw new AuthenticationFailedException(ORG_NOT_FOUND.getCode(),
                        ORG_NOT_FOUND.getMessage());
            }
            organizationName = runtimeParams.get(ORG_PARAMETER);

            // Get the outbound service provider of the organization.
            String outboundSpUUID;
            ServiceProvider outboundServiceProvider;
            ApplicationManagementService applicationManagementService =
                    EnterpriseIDPAuthenticatorDataHolder.getInstance()
                            .getApplicationManagementService();
            try {
                outboundSpUUID = EnterpriseIDPAuthenticatorDataHolder.getInstance()
                        .getEnterpriseLoginManagementService()
                        .resolveOrganizationSpResourceId(organizationName, inboundSp, inboundSpTenant);
                outboundServiceProvider = applicationManagementService.getApplicationByResourceId(outboundSpUUID,
                        organizationName);
            } catch (EnterpriseLoginManagementException | IdentityApplicationManagementException e) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Error on getting outbound service provider of the organization. "
                            + "Organization: %s, Inbound service provider: %s", organizationName, inboundSp));
                }
                throw new AuthenticationFailedException(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                        ENTERPRISE_IDP_LOGIN_FAILED.getMessage(), e);
            }

            Optional<InboundAuthenticationRequestConfig> oidcConfigurations = getAuthenticationConfig(
                    outboundServiceProvider);
            if (!oidcConfigurations.isPresent()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Error in getting inbound authentication configurations of sp: %s of the "
                            + "organization: %s", outboundServiceProvider, organizationName));
                }
                throw new AuthenticationFailedException(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                        ENTERPRISE_IDP_LOGIN_FAILED.getMessage());
            }

            // Update the authenticator configurations based on the user's organization.
            String clientId = oidcConfigurations.get().getInboundAuthKey();
            OAuthAdminServiceImpl oAuthAdminService = EnterpriseIDPAuthenticatorDataHolder.getInstance()
                    .getOAuthAdminService();
            OAuthConsumerAppDTO oauthApp = oAuthAdminService.getOAuthApplicationData(clientId);
            authenticatorProperties.put(CLIENT_ID, clientId);
            authenticatorProperties.put(CLIENT_SECRET, oauthApp.getOauthConsumerSecret());

            authenticatorProperties.put(ORGANIZATION_ATTRIBUTE, organizationName);
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
                                           AuthenticationContext context) throws AuthenticationFailedException {

        try {
            Map<String, String> runtimeParams = getRuntimeParams(context);
            if (request.getParameterMap().containsKey(ORG_PARAMETER)) {
                String organizationName = request.getParameter(ORG_PARAMETER);
                runtimeParams.put(ORG_PARAMETER, organizationName);
            }

            if (StringUtils.isBlank(runtimeParams.get(ORG_PARAMETER))) {
                if (log.isDebugEnabled()) {
                    log.debug(ORG_PARAMETER_NOT_FOUND.getMessage());
                }
                throw new AuthenticationFailedException(ORG_PARAMETER_NOT_FOUND.getCode(),
                        ORG_PARAMETER_NOT_FOUND.getMessage());
            }

            return super.process(request, response, context);
        } catch (LogoutFailedException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
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
                .addPath(AUTHORIZATION_ENDPOINT_TENANTED_PATH.replace(TENANT_PLACEHOLDER, organizationName)).build()
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
                .addPath(TOKEN_ENDPOINT_TENANTED_PATH.replace(TENANT_PLACEHOLDER, organizationName)).build()
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
}
