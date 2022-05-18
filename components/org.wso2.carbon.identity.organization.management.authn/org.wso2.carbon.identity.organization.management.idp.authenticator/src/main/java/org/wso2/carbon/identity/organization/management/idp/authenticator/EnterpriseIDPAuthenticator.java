/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.idp.authenticator;

import org.apache.commons.collections.CollectionUtils;
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
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.idp.authenticator.internal.EnterpriseIDPAuthenticatorDataHolder;
import org.wso2.carbon.identity.organization.management.login.mgt.core.exceptions.EnterpriseLoginManagementException;

import java.util.ArrayList;
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
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ACCOUNT_CHOOSE_PROMPT_PAGE;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.AUTHORIZATION_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.EMAIL_DOMAIN_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ORGANIZATION_USER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.ORG_PARAMETER;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.TENANT_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.TOKEN_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.USERINFO_ENDPOINT_TENANTED_PATH;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.EnterpriseIDPAuthenticatorConstants.USERNAME_PARAMETER;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.EMAIL_DOMAIN_NOT_ASSOCIATED;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.ENTERPRISE_IDP_LOGIN_FAILED;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.ORG_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.USERNAME_IS_NOT_AN_EMAIL_ERROR;
import static org.wso2.carbon.identity.organization.management.idp.authenticator.util.EnterpriseIDPErrorConstants.ErrorMessages.USERNAME_PARAMETER_NOT_FOUND;

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
            String username;
            List<String> userOrganizationList;
            String inboundSpTenant = context.getTenantDomain();
            Map<String, String> runtimeParams = getRuntimeParams(context);
            if (request.getParameterMap().containsKey(ORG_PARAMETER)) {
                String organizationName = request.getParameter(ORG_PARAMETER);
                validateUserAssociation(inboundSpTenant, runtimeParams, organizationName);
                runtimeParams.put(ORG_PARAMETER, organizationName);
            } else if (StringUtils.isNotBlank(runtimeParams.get(ORG_PARAMETER))) {
                String organizationName = runtimeParams.get(ORG_PARAMETER);
                validateUserAssociation(inboundSpTenant, runtimeParams, organizationName);
            } else {
                if (StringUtils.isBlank(runtimeParams.get(USERNAME_PARAMETER))) {
                    if (log.isDebugEnabled()) {
                        log.debug(USERNAME_PARAMETER_NOT_FOUND.getMessage());
                    }
                    throw new AuthenticationFailedException(USERNAME_PARAMETER_NOT_FOUND.getCode(),
                            USERNAME_PARAMETER_NOT_FOUND.getMessage());
                }
                username = runtimeParams.get(USERNAME_PARAMETER);
                validateEmailDomain(username);
                String emailDomain = username.split(EMAIL_DOMAIN_SEPARATOR)[1];
                userOrganizationList = getOrganizationList(emailDomain, inboundSpTenant);
                if (userOrganizationList.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug(EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN.getMessage() +
                                " Email domain: " + emailDomain);
                    }
                    throw new AuthenticationFailedException(
                            EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN.getCode(),
                            EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN.getMessage());
                }
                /* Even if there are multiple organizations registered for the same email domain,
                choose the first one as the default.*/
                String organizationName = userOrganizationList.get(0);
                runtimeParams.put(ORG_PARAMETER, organizationName);
            }
            return super.process(request, response, context);
        } catch (LogoutFailedException e) {
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }

    private void validateUserAssociation(String inboundSpTenant, Map<String, String> runtimeParams,
                                         String organizationName)
            throws AuthenticationFailedException {

        String username;
        List<String> userOrganizationList;
        if (StringUtils.isNotBlank(runtimeParams.get(USERNAME_PARAMETER))) {
            username = runtimeParams.get(USERNAME_PARAMETER);
            validateEmailDomain(username);
            String emailDomain = username.split(EMAIL_DOMAIN_SEPARATOR)[1];
            userOrganizationList = getOrganizationList(emailDomain, inboundSpTenant);
            if (!userOrganizationList.contains(organizationName)) {
                throw new AuthenticationFailedException(EMAIL_DOMAIN_NOT_ASSOCIATED.getCode(),
                        EMAIL_DOMAIN_NOT_ASSOCIATED.getMessage());
            }
        }
    }

    private void validateEmailDomain(String username) throws AuthenticationFailedException {

        if (!username.contains(EMAIL_DOMAIN_SEPARATOR)) {
            if (log.isDebugEnabled()) {
                log.debug(USERNAME_IS_NOT_AN_EMAIL_ERROR.getMessage());
            }
            throw new AuthenticationFailedException(USERNAME_IS_NOT_AN_EMAIL_ERROR.getCode(),
                    USERNAME_IS_NOT_AN_EMAIL_ERROR.getMessage());
        }
    }

    private List<String> getOrganizationList(String emailDomain, String inboundSpTenant)
            throws AuthenticationFailedException {

        List<String> userOrganizationList = new ArrayList<>();
        List<Integer> userOrganizationListIds;
        try {
            userOrganizationListIds = EnterpriseIDPAuthenticatorDataHolder.getInstance()
                    .getEnterpriseLoginManagementService()
                    .getOrganizationsForEmailDomain(emailDomain, inboundSpTenant);

            if (CollectionUtils.isEmpty(userOrganizationListIds)) {
                if (log.isDebugEnabled()) {
                    log.debug(EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN.getMessage() + " Email " +
                            "domain: " + emailDomain);
                }
                throw new AuthenticationFailedException(
                        EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN.getCode(),
                        EMAIL_DOMAIN_IS_NOT_REGISTERED_FOR_ENTERPRISE_IDP_LOGIN.getMessage());
            }

            for (Integer tenantId : userOrganizationListIds) {
                userOrganizationList.add(IdentityTenantUtil.getTenantDomain(tenantId));
            }
            return userOrganizationList;
        } catch (EnterpriseLoginManagementException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error on getting organization list for email domain: " + emailDomain);
            }
            throw new AuthenticationFailedException(ENTERPRISE_IDP_LOGIN_FAILED.getCode(),
                    ENTERPRISE_IDP_LOGIN_FAILED.getMessage(), e);
        }
    }

    private String getAccountChooserURL() throws AuthenticationFailedException {

        return this.buildUrl(ACCOUNT_CHOOSE_PROMPT_PAGE);
    }

    private String buildUrl(String defaultContext) throws AuthenticationFailedException {

        if (!IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
            throw new AuthenticationFailedException("Tenant qualified URL is disabled.");
        }
        try {
            return ServiceURLBuilder.create().addPath(new String[]{defaultContext}).build().getAbsolutePublicURL();
        } catch (URLBuilderException e) {
            throw new IdentityRuntimeException("Error while building tenant qualified url for context: " +
                    defaultContext, e);
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
     * Returns the user info endpoint url for a given organization.
     *
     * @param organizationName Name of the organization.
     * @return The user info endpoint URL.
     */
    private String getUserInfoEndpoint(String organizationName) throws URLBuilderException {

        return ServiceURLBuilder.create()
                .addPath(USERINFO_ENDPOINT_TENANTED_PATH.replace(TENANT_PLACEHOLDER, organizationName)).build()
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
