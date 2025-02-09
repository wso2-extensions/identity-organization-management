/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler;

import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.UserSessionManagementService;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.UserIdNotFoundException;
import org.wso2.carbon.identity.application.authentication.framework.exception.session.mgt.SessionManagementException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.model.OIDCStateInfo;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.oauth.common.OAuthConstants.OIDCClaims.IDP_SESSION_KEY;

/**
 * Event handler to handle organization sessions.
 */
public class OrganizationSessionHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationSessionHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        if (IdentityEventConstants.EventName.SESSION_TERMINATE.name().equals(eventName)) {
            handleOrgSessionTerminate(eventProperties);
        }
    }

    private void handleOrgSessionTerminate(Map<String, Object> eventProperties) {

        try {
            UserSessionManagementService userSessionManagementService = OrganizationManagementHandlerDataHolder
                    .getInstance().getUserSessionManagementService();

            SessionContext sessionContext = (SessionContext) eventProperties.get(
                    IdentityEventConstants.EventProperty.SESSION_CONTEXT);
            Map<String, AuthenticatedIdPData> authenticatedIdPs = sessionContext.getAuthenticatedIdPs();
            if (authenticatedIdPs == null ||
                    !authenticatedIdPs.containsKey(FrameworkConstants.ORGANIZATION_LOGIN_IDP_NAME)) {
                return;
            }
            AuthenticatedIdPData authenticatedIdPData =
                    authenticatedIdPs.get(FrameworkConstants.ORGANIZATION_LOGIN_IDP_NAME);
            List<AuthenticatorConfig> authenticators = authenticatedIdPData.getAuthenticators();
            if (CollectionUtils.isEmpty(authenticators)) {
                return;
            }
            String sessionId = null;
            for (AuthenticatorConfig authenticator : authenticators) {
                if (FrameworkConstants.ORGANIZATION_AUTHENTICATOR.equals(authenticator.getName())) {
                    AuthenticatorStateInfo authenticatorStateInfo = authenticator.getAuthenticatorStateInfo();
                    if (authenticatorStateInfo instanceof OIDCStateInfo) {
                        OIDCStateInfo oidcStateInfo = (OIDCStateInfo) authenticatorStateInfo;
                        String idTokenHint = oidcStateInfo.getIdTokenHint();
                        // Resolve session ID by `isk` claim in ID token hint.
                        sessionId = getSessionId(idTokenHint);
                        break;
                    }
                }
            }

            Map<String, Object> params = (Map<String, Object>) eventProperties.get(
                    IdentityEventConstants.EventProperty.PARAMS);
            if (params == null || !params.containsKey(FrameworkConstants.AnalyticsAttributes.USER)) {
                return;
            }
            AuthenticatedUser authenticatedUser = (AuthenticatedUser) params.get(
                    FrameworkConstants.AnalyticsAttributes.USER);
            String userId = authenticatedUser.getUserId();
            if (StringUtils.isNotBlank(sessionId)) {
                userSessionManagementService.terminateSessionBySessionId(userId, sessionId);
            }
        } catch (SessionManagementException | UserIdNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while terminating the org session.", e);
            }
        }
    }

    private String getSessionId(String idToken) {

        String base64Body = idToken.split("\\.")[1];
        byte[] decoded = Base64.getDecoder().decode(base64Body);
        try {
            Set<Map.Entry<String, Object>> jwtAttributeSet =
                    JSONObjectUtils.parse(new String(decoded, StandardCharsets.UTF_8)).entrySet();
            for (Map.Entry<String, Object> entry : jwtAttributeSet) {
                if (StringUtils.equals(IDP_SESSION_KEY, entry.getKey())) {
                    return String.valueOf(entry.getValue());
                }
            }
        } catch (ParseException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while parsing the ID token.", e);
            }
        }
        return null;
    }
}
