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

package org.wso2.carbon.identity.organization.management.claim.provider;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.openidconnect.ClaimProvider;
import org.wso2.carbon.identity.organization.management.claim.provider.internal.OrganizationClaimProviderServiceComponentHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT;

/**
 * Claim provider for organization specific details.
 */
public class OrganizationClaimProvider implements ClaimProvider {

    private static final String ORGANIZATION_ID_ATTRIBUTE = "org_id";
    private static final String ORGANIZATION_NAME_ATTRIBUTE = "org_name";

    @Override
    public Map<String, Object> getAdditionalClaims(OAuthAuthzReqMessageContext oAuthAuthzReqMessageContext,
                                                   OAuth2AuthorizeRespDTO oAuth2AuthorizeRespDTO) {

        return null;
    }

    @Override
    public Map<String, Object> getAdditionalClaims(OAuthTokenReqMessageContext oAuthTokenReqMessageContext,
                                                   OAuth2AccessTokenRespDTO oAuth2AccessTokenRespDTO)
            throws IdentityOAuth2Exception {

        Map<String, Object> additionalClaims = new HashMap<>();
        try {
            String loginTenantDomain = oAuthTokenReqMessageContext.getAuthorizedUser().getTenantDomain();
            String organizationId = getOrganizationManager().resolveOrganizationId(loginTenantDomain);
            if (StringUtils.isNotBlank(organizationId)) {
                String organizationName = getOrganizationManager().getOrganizationNameById(organizationId);
                additionalClaims.put(ORGANIZATION_ID_ATTRIBUTE, organizationId);
                additionalClaims.put(ORGANIZATION_NAME_ATTRIBUTE, organizationName);
            }
        } catch (OrganizationManagementClientException e) {
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                return additionalClaims;
            } else {
                throw new IdentityOAuth2Exception("Error while resolving organization id.", e);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityOAuth2Exception("Error while resolving organization id.", e);
        }
        return additionalClaims;
    }

    private static OrganizationManager getOrganizationManager() {

        return OrganizationClaimProviderServiceComponentHolder.getInstance().getOrganizationManager();
    }
}
