package org.wso2.carbon.identity.organization.management.claim.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.openidconnect.ClaimProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Claim provider for add custom org claim for tokens
 */
public class CustomClaimProvider implements ClaimProvider {

    private static final Log log = LogFactory.getLog(CustomClaimProvider.class);
    private static final String ORGANIZATION_USER_ATTRIBUTE = "org";

    @Override
    public Map<String, Object> getAdditionalClaims(OAuthAuthzReqMessageContext oAuthAuthzReqMessageContext,
                                                   OAuth2AuthorizeRespDTO oAuth2AuthorizeRespDTO)
            throws IdentityOAuth2Exception {

        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getAdditionalClaims(OAuthTokenReqMessageContext oAuthTokenReqMessageContext,
                                                   OAuth2AccessTokenRespDTO oAuth2AccessTokenRespDTO)
            throws IdentityOAuth2Exception {

        Map<String, Object> additionalClaims = new HashMap<>();

        String organizationClaim = oAuthTokenReqMessageContext.getAuthorizedUser().getUserAttributes()
                .get(ClaimMapping.build(ORGANIZATION_USER_ATTRIBUTE, ORGANIZATION_USER_ATTRIBUTE, null, false));

        if (organizationClaim != null) {
            additionalClaims.put("org", organizationClaim);
        } else {
            additionalClaims.put("org", oAuthTokenReqMessageContext.getAuthorizedUser().getTenantDomain());
        }
        return additionalClaims;
    }
}
