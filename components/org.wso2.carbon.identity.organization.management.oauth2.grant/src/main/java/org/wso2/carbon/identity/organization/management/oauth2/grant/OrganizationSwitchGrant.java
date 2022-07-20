/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.oauth2.grant;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.application.authentication.framework.exception.UserIdNotFoundException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.RequestParameter;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationManager;
import org.wso2.carbon.identity.organization.management.authz.service.exception.OrganizationManagementAuthzServiceServerException;
import org.wso2.carbon.identity.organization.management.oauth2.grant.exception.OrganizationSwitchGrantException;
import org.wso2.carbon.identity.organization.management.oauth2.grant.internal.OrganizationSwitchGrantDataHolder;
import org.wso2.carbon.identity.organization.management.oauth2.grant.util.OrganizationSwitchGrantConstants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagerImpl;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Arrays;

import static org.wso2.carbon.identity.organization.management.oauth2.grant.util.OrganizationSwitchGrantConstants.ORG_SWITCH_PERMISSION;
import static org.wso2.carbon.identity.organization.management.oauth2.grant.util.OrganizationSwitchGrantConstants.ORG_SWITCH_PERMISSION_FOR_ROOT;
import static org.wso2.carbon.identity.organization.management.oauth2.grant.util.OrganizationSwitchGrantUtil.handleServerException;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_AUTHENTICATED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_VALIDATING_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_VALIDATING_USER_ROOT_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ROOT_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;

/**
 * Implements the AuthorizationGrantHandler for the OrganizationSwitch grant type.
 */
public class OrganizationSwitchGrant extends AbstractAuthorizationGrantHandler  {

    public OrganizationManager organizationManager = new OrganizationManagerImpl();

    private static final Log LOG = LogFactory.getLog(OrganizationSwitchGrant.class);

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx)  throws IdentityOAuth2Exception {

        super.validateGrant(tokReqMsgCtx);

        String token = extractParameter(OrganizationSwitchGrantConstants.Params.TOKEN_PARAM, tokReqMsgCtx);
        String organizationId = extractParameter(OrganizationSwitchGrantConstants.Params.ORG_PARAM, tokReqMsgCtx);

        OAuth2TokenValidationResponseDTO validationResponseDTO = validateToken(token);

        if (!validationResponseDTO.isValid()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Access token validation failed.");
            }

            throw new IdentityOAuth2Exception("Invalid token received.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Access token validation success.");
        }

        User authorizedUser = User.getUserFromUserName(validationResponseDTO.getAuthorizedUser());
        String userId = getUserIdFromAuthorizedUser(authorizedUser);

        boolean isValidCollaborator;
        if (StringUtils.equals(ROOT_ORG_ID, organizationId)) {
            isValidCollaborator = validateCollaboratorAssociationForRoot(authorizedUser.getUserName());
        } else {
            isValidCollaborator = validateCollaboratorAssociation(userId, organizationId);
        }

        if (!isValidCollaborator) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authorized user: " + authorizedUser.toFullQualifiedUsername() + " does not have " +
                        "permission for the current organization");
            }

            return false;
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        authenticatedUser.setUserName(authorizedUser.getUserName());
        authenticatedUser.setUserStoreDomain(authorizedUser.getUserStoreDomain());
        authenticatedUser.setTenantDomain(getTenantDomainFromOrganizationId(organizationId));
        authenticatedUser.setUserId(userId);

        tokReqMsgCtx.setAuthorizedUser(authenticatedUser);

        String[] allowedScopes = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getScope();
        tokReqMsgCtx.setScope(allowedScopes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Issuing an access token for user: " + authenticatedUser + " with scopes: " +
                    Arrays.toString(tokReqMsgCtx.getScope()));
        }

        return true;
    }

    private boolean validateCollaboratorAssociation(String userId, String orgId)
            throws OrganizationSwitchGrantException {

        try {
            return OrganizationManagementAuthorizationManager.getInstance()
                    .isUserAuthorized(userId, ORG_SWITCH_PERMISSION, orgId);
        } catch (OrganizationManagementAuthzServiceServerException e) {
           throw handleServerException(ERROR_CODE_ERROR_VALIDATING_USER_ASSOCIATION, e);
        }
    }

    private String extractParameter(String param, OAuthTokenReqMessageContext tokReqMsgCtx) {

        RequestParameter[] parameters = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getRequestParameters();

        if (parameters != null) {
            for (RequestParameter parameter : parameters) {
                if (param.equals(parameter.getKey())) {
                    if (ArrayUtils.isNotEmpty(parameter.getValue())) {
                        return parameter.getValue()[0];
                    }
                }
            }
        }

        return null;
    }

    /**
     * Validate access token.
     *
     * @param accessToken
     * @return OAuth2TokenValidationResponseDTO of the validated token
     */
    private OAuth2TokenValidationResponseDTO validateToken(String accessToken) {

        OAuth2TokenValidationService oAuth2TokenValidationService = new OAuth2TokenValidationService();
        OAuth2TokenValidationRequestDTO requestDTO = new OAuth2TokenValidationRequestDTO();
        OAuth2TokenValidationRequestDTO.OAuth2AccessToken token = requestDTO.new OAuth2AccessToken();

        token.setIdentifier(accessToken);
        token.setTokenType("bearer");
        requestDTO.setAccessToken(token);

        OAuth2TokenValidationRequestDTO.TokenValidationContextParam contextParam = requestDTO.new
                TokenValidationContextParam();

        OAuth2TokenValidationRequestDTO.TokenValidationContextParam[] contextParams = {contextParam};
        requestDTO.setContext(contextParams);

        OAuth2ClientApplicationDTO clientApplicationDTO = oAuth2TokenValidationService
                .findOAuthConsumerIfTokenIsValid
                        (requestDTO);
        return clientApplicationDTO.getAccessTokenValidationResponse();
    }

    private String getUserIdFromAuthorizedUser(User authorizedUser) throws OrganizationSwitchGrantException {

        try {
            return new AuthenticatedUser(authorizedUser).getUserId();
        } catch (UserIdNotFoundException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_AUTHENTICATED_USER, e);
        }
    }

    private String getTenantDomainFromOrganizationId(String organizationId) throws OrganizationSwitchGrantException {

        try {
            return organizationManager.resolveTenantDomain(organizationId);
        } catch (OrganizationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN, e);
        }
    }

    private boolean validateCollaboratorAssociationForRoot(String username) throws OrganizationSwitchGrantException {

        try {
            RealmService realmService = OrganizationSwitchGrantDataHolder.getInstance().getRealmService();
            UserRealm tenantUserRealm = realmService.getTenantUserRealm(getTenantId());
            AuthorizationManager authorizationManager = tenantUserRealm.getAuthorizationManager();
            return authorizationManager.isUserAuthorized(username, ORG_SWITCH_PERMISSION_FOR_ROOT,
                    CarbonConstants.UI_PERMISSION_ACTION);
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_VALIDATING_USER_ROOT_ASSOCIATION, e);
        }
    }
}
