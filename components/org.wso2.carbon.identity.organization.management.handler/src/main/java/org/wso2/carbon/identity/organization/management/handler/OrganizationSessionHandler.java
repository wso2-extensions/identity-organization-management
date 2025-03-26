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

import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.UserSessionManagementService;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCache;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCacheKey;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.UserIdNotFoundException;
import org.wso2.carbon.identity.application.authentication.framework.exception.session.mgt.SessionManagementException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.oidc.model.OIDCStateInfo;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.oauth.common.OAuthConstants.OIDCClaims.IDP_SESSION_KEY;

/**
 * Event handler to handle organization sessions.
 */
public class OrganizationSessionHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationSessionHandler.class);
    private static final String AUTHORIZED_ORGANIZATION_ID_ATTRIBUTE = "org_id";

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        if (IdentityEventConstants.EventName.SESSION_TERMINATE.name().equals(eventName)) {
            handleOrgSessionTerminate(eventProperties);
        } else if (IdentityEventConstants.Event.SESSION_EXTENSION.equals(eventName)) {
            handleOrgSessionExtend(eventProperties);
        } else if (IdentityEventConstants.EventName.SESSION_CREATE.name().equals(eventName)) {
            handleOrgSessionCreation(eventProperties);
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
                        sessionId = getClaimFromJWT(idTokenHint, IDP_SESSION_KEY);
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

    private void handleOrgSessionExtend(Map<String, Object> eventProperties) throws IdentityEventException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Extending associated sub organization session for root organization session: " +
                    eventProperties.get(
                            IdentityEventConstants.EventProperty.SESSION_CONTEXT_ID));
        }
        SessionContext sessionContext = (SessionContext) eventProperties.get(
                IdentityEventConstants.EventProperty.SESSION_CONTEXT);
        Map<String, AuthenticatedIdPData> authenticatedIdPs = sessionContext.getAuthenticatedIdPs();
        if (authenticatedIdPs == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticated IdPs not found in the session context." +
                        " Hence, sub org session extension is not applicable.");
            }
            return;
        }
        String sessionId = null;
        String orgId = null;
        authenticatedIdPsLoop:
        for (Map.Entry<String, AuthenticatedIdPData> entry : authenticatedIdPs.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            List<AuthenticatorConfig> authenticators = entry.getValue().getAuthenticators();
            if (CollectionUtils.isEmpty(authenticators)) {
                continue;
            }
            for (AuthenticatorConfig authenticator : authenticators) {
                if (FrameworkConstants.ORGANIZATION_AUTHENTICATOR.equals(authenticator.getName())) {
                    AuthenticatorStateInfo authenticatorStateInfo = authenticator.getAuthenticatorStateInfo();
                    if (authenticatorStateInfo instanceof OIDCStateInfo) {
                        OIDCStateInfo oidcStateInfo = (OIDCStateInfo) authenticatorStateInfo;
                        String idTokenHint = oidcStateInfo.getIdTokenHint();
                        // Resolve session ID by `isk` claim in ID token hint.
                        sessionId = getClaimFromJWT(idTokenHint, IDP_SESSION_KEY);
                        // Resolve org ID by `org_id` claim in ID token hint.
                        orgId = getClaimFromJWT(idTokenHint, AUTHORIZED_ORGANIZATION_ID_ATTRIBUTE);
                        break authenticatedIdPsLoop;
                    }
                }
            }
        }
        if (StringUtils.isBlank(sessionId) || StringUtils.isBlank(orgId)) {
            LOG.debug("Organization authenticator not found in the authenticators list or " +
                    "Session ID / org ID not found in the ID token.");
            return;
        }
        String tenantDomain;
        try {
            tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            if (StringUtils.isBlank(tenantDomain)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Tenant ID not found for the organization ID: " + orgId);
                }
                return;
            }
        } catch (OrganizationManagementException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while resolving the tenant ID for the organization ID: " + orgId, e);
            }
            throw new IdentityEventException("Error while extending session for session ID: " + sessionId, e);
        }

        SessionContextCacheKey sessionContextCacheKey = new SessionContextCacheKey(sessionId);
        SessionContextCacheEntry sessionContextCacheEntry = SessionContextCache.getInstance()
                .getSessionContextCacheEntry(sessionContextCacheKey, tenantDomain);

        if (sessionContextCacheEntry == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No session available for requested session identifier: " + sessionId);
            }
            return;
        }

        SessionContext orgSessionContext = sessionContextCacheEntry.getContext();
        boolean isSessionExpired = SessionContextCache.getInstance().
                isSessionExpired(sessionContextCacheKey, sessionContextCacheEntry);
        if (isSessionExpired) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Session already expired for provided session cache entry with session ID: " + sessionId);
            }
            return;
        }
        long currentTime = System.currentTimeMillis();
        FrameworkUtils.updateSessionLastAccessTimeMetadata(sessionId, currentTime);
        FrameworkUtils.addSessionContextToCache(sessionId, orgSessionContext, tenantDomain, tenantDomain);
    }

    private void handleOrgSessionCreation(Map<String, Object> eventProperties) {

        SessionContext sessionContext = (SessionContext) eventProperties.get(
                IdentityEventConstants.EventProperty.SESSION_CONTEXT);
        AuthenticationContext context = (AuthenticationContext) eventProperties.get(
                IdentityEventConstants.EventProperty.CONTEXT);
        Map<String, Object> params = (Map<String, Object>) eventProperties.get(
                IdentityEventConstants.EventProperty.PARAMS);

        String rootOrgSessionId;
        if (params.containsKey(FrameworkConstants.AnalyticsAttributes.SESSION_ID)) {
            rootOrgSessionId = (String) params.get(FrameworkConstants.AnalyticsAttributes.SESSION_ID);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Root organization session ID not found in the event properties.");
            }
            return;
        }

        Map<String, AuthenticatedIdPData> authenticatedIdPs = sessionContext.getAuthenticatedIdPs();
        if (authenticatedIdPs == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticated IdPs not found in the session context." +
                        " Hence, handling root org session remember me option is not needed.");
            }
            return;
        }

        String subOrgSessionId = null;
        String subOrgId = null;
        authenticatedIdPsLoop:
        for (Map.Entry<String, AuthenticatedIdPData> entry : authenticatedIdPs.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            List<AuthenticatorConfig> authenticators = entry.getValue().getAuthenticators();
            if (CollectionUtils.isEmpty(authenticators)) {
                continue;
            }
            for (AuthenticatorConfig authenticator : authenticators) {
                if (FrameworkConstants.ORGANIZATION_AUTHENTICATOR.equals(authenticator.getName())) {
                    AuthenticatorStateInfo authenticatorStateInfo = authenticator.getAuthenticatorStateInfo();
                    if (authenticatorStateInfo instanceof OIDCStateInfo) {
                        OIDCStateInfo oidcStateInfo = (OIDCStateInfo) authenticatorStateInfo;
                        String idTokenHint = oidcStateInfo.getIdTokenHint();
                        // Resolve session ID by `isk` claim in ID token hint.
                        subOrgSessionId = getClaimFromJWT(idTokenHint, IDP_SESSION_KEY);
                        // Resolve org ID by `org_id` claim in ID token hint.
                        subOrgId = getClaimFromJWT(idTokenHint, AUTHORIZED_ORGANIZATION_ID_ATTRIBUTE);
                        break authenticatedIdPsLoop;
                    }
                }
            }
        }

        if (StringUtils.isBlank(subOrgSessionId) || StringUtils.isBlank(subOrgId)) {
            LOG.debug("Organization authenticator not found in the authenticators list or " +
                    "Session ID / org ID not found in the ID token.");
            return;
        }
        String subOrgTenantDomain;
        try {
            subOrgTenantDomain = getOrganizationManager().resolveTenantDomain(subOrgId);
            if (StringUtils.isBlank(subOrgTenantDomain)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Tenant domain not found for the organization ID: " + subOrgId);
                }
                return;
            }
        } catch (OrganizationManagementException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while handling root org session remember me option for sub org session: " +
                        subOrgId, e);
            }
            return;
        }

        SessionContextCacheKey sessionContextCacheKey = new SessionContextCacheKey(subOrgSessionId);
        SessionContextCacheEntry sessionContextCacheEntry = SessionContextCache.getInstance()
                .getSessionContextCacheEntry(sessionContextCacheKey, subOrgTenantDomain);
        if (sessionContextCacheEntry == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No session available for requested session identifier: " + subOrgSessionId);
            }
            return;
        }

        // Get the sub organization session context.
        SessionContext orgSessionContext = sessionContextCacheEntry.getContext();

        // Set the remember me option of the sub organization session to the root organization session.
        sessionContext.setRememberMe(orgSessionContext.isRememberMe());

        // Add the root organization session context to the cache.
        FrameworkUtils.addSessionContextToCache(rootOrgSessionId, sessionContext, subOrgTenantDomain,
                context.getLoginTenantDomain());
    }

    private String getClaimFromJWT(String idToken, String claimname) {

        try {
            return String.valueOf(SignedJWT.parse(idToken).getJWTClaimsSet().getClaim(claimname));
        } catch (ParseException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error while parsing the ID token.", e);
            }
        }
        return null;
    }

    private static OrganizationManager getOrganizationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    }
}
