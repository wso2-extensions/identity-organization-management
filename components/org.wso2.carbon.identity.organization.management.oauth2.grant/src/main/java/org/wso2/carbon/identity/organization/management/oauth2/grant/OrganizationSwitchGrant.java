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

package org.wso2.carbon.identity.organization.management.oauth2.grant;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.RequestParameter;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.identity.organization.management.oauth2.grant.util.Constants;

import java.util.Arrays;

/**
 * Implements the AuthorizationGrantHandler for the OrganizationSwitch grant type.
 */
public class OrganizationSwitchGrant extends AbstractAuthorizationGrantHandler  {

    private static final Log LOG = LogFactory.getLog(OrganizationSwitchGrant.class);


    public static final String MOBILE_GRANT_PARAM = "mobileNumber";

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx)  throws IdentityOAuth2Exception {

        super.validateGrant(tokReqMsgCtx);

        String token = extractParameter(Constants.Params.TOKEN_PARAM, tokReqMsgCtx);
        String organization = extractParameter(Constants.Params.ORG_PARAM, tokReqMsgCtx);

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

        String currentOrganizationId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();

        boolean isValidCollaborator = validateCollaboratorAssociation(authorizedUser);

        if (!isValidCollaborator) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authorized user: " + authorizedUser.toFullQualifiedUsername() + " does not have " +
                        "permission for the current organization");
            }
//            ResponseHeader responseHeader = new ResponseHeader();
//            responseHeader.setKey("error-description");
//            responseHeader.setValue("Associated user is invalid.");
//            tokReqMsgCtx.addProperty("RESPONSE_HEADERS", new ResponseHeader[]{responseHeader});

            return false;
        }

        User currentUser = new User();
        currentUser.setUserName(authorizedUser.getUserName());
        currentUser.setUserStoreDomain(authorizedUser.getUserStoreDomain());
        currentUser.setTenantDomain(convertOrgToTenant(currentOrganizationId));

        tokReqMsgCtx.setAuthorizedUser(
                OAuth2Util.getUserFromUserName(currentUser.toFullQualifiedUsername()));

        //This is commented to support account switching capability.
        //https://github.com/wso2/product-is/issues/7385
        //String[] allowedScopes =  getAllowedScopes(validationResponseDTO.getScope(),
        //tokReqMsgCtx.getOauth2AccessTokenReqDTO().getScope());
        String[] allowedScopes = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getScope();
        tokReqMsgCtx.setScope(allowedScopes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Issuing an access token for user: " + currentUser + " with scopes: " +
                    Arrays.toString(tokReqMsgCtx.getScope()));
        }

        return true;
    }

    private boolean validateCollaboratorAssociation(User authorizedUser) {

        //TODO implement
        return true;
    }

    private String convertOrgToTenant(String currentOrganizationId) {

        //TODO implement
        return currentOrganizationId;
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

        //TODO: If these values are not set, validation will fail giving an NPE. Need to see why that happens
        OAuth2TokenValidationRequestDTO.TokenValidationContextParam contextParam = requestDTO.new
                TokenValidationContextParam();
        contextParam.setKey("dummy");
        contextParam.setValue("dummy");

        OAuth2TokenValidationRequestDTO.TokenValidationContextParam[] contextParams = {contextParam};
        requestDTO.setContext(contextParams);

        //TODO check if we can validate a token from different tenant
        OAuth2ClientApplicationDTO clientApplicationDTO = oAuth2TokenValidationService
                .findOAuthConsumerIfTokenIsValid
                        (requestDTO);
        return clientApplicationDTO.getAccessTokenValidationResponse();
    }

}
