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

package org.wso2.carbon.identity.organization.management.authz.valve;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.auth.service.AuthenticationContext;
import org.wso2.carbon.identity.auth.service.handler.HandlerManager;
import org.wso2.carbon.identity.auth.service.module.ResourceConfig;
import org.wso2.carbon.identity.authz.service.AuthorizationManager;
import org.wso2.carbon.identity.authz.service.AuthorizationResult;
import org.wso2.carbon.identity.authz.service.AuthorizationStatus;
import org.wso2.carbon.identity.authz.service.exception.AuthzServiceServerException;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationContext;
import org.wso2.carbon.identity.organization.management.authz.valve.internal.OrganizationManagementAuthzValveServiceHolder;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import static org.wso2.carbon.identity.auth.service.util.Constants.OAUTH2_ALLOWED_SCOPES;
import static org.wso2.carbon.identity.auth.service.util.Constants.OAUTH2_VALIDATE_SCOPE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.HTTP_DELETE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.HTTP_GET;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.HTTP_PATCH;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.HTTP_PUT;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.REGEX_FOR_URLS_WITH_ORG_ID;

/**
 * The valve for organization management related authorization.
 */
public class OrganizationManagementAuthzValve extends ValveBase {

    private static final String AUTH_HEADER_NAME = "WWW-Authenticate";
    private static final String AUTH_CONTEXT = "auth-context";

    private static final Log LOG = LogFactory.getLog(OrganizationManagementAuthzValve.class);

    public void invoke(Request request, Response response) throws IOException, ServletException {

        AuthenticationContext authenticationContext = (AuthenticationContext) request.getAttribute(AUTH_CONTEXT);

        if (authenticationContext != null && !isUserEmpty(authenticationContext)) {

            ResourceConfig resourceConfig = authenticationContext.getResourceConfig();
            String context = resourceConfig.getContext();
            String httpMethod = resourceConfig.getHttpMethod();
            String resourceConfigPermissions = resourceConfig.getPermissions();
            List<String> resourceConfigScopes = resourceConfig.getScopes();
            String requestURI = request.getRequestURI();

            // Check whether the request needs to be handled by the organization management authorization valve.
            if (!canHandle(requestURI, httpMethod)) {
                getNext().invoke(request, response);
                return;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("The request will be handled by the organization management authorization valve.");
            }
            OrganizationManagementAuthorizationContext orgMgtAuthorizationContext =
                    new OrganizationManagementAuthorizationContext();
            orgMgtAuthorizationContext.setPermissionString(resourceConfigPermissions);
            orgMgtAuthorizationContext.setRequiredScopes(resourceConfigScopes);
            orgMgtAuthorizationContext.setContext(context);
            orgMgtAuthorizationContext.setRequestUri(requestURI);
            orgMgtAuthorizationContext.setHttpMethods(httpMethod);
            orgMgtAuthorizationContext.setUser(authenticationContext.getUser());
            orgMgtAuthorizationContext.addParameter(OAUTH2_ALLOWED_SCOPES,
                    authenticationContext.getParameter(OAUTH2_ALLOWED_SCOPES));
            orgMgtAuthorizationContext.addParameter(OAUTH2_VALIDATE_SCOPE,
                    authenticationContext.getParameter(OAUTH2_VALIDATE_SCOPE));

            List<AuthorizationManager> authorizationManagerList = OrganizationManagementAuthzValveServiceHolder
                    .getInstance().getAuthorizationManagerList();
            AuthorizationManager authorizationManager = HandlerManager.getInstance()
                    .getFirstPriorityHandler(authorizationManagerList, true);
            try {
                AuthorizationResult authorizationResult = authorizationManager.authorize(orgMgtAuthorizationContext);
                if (authorizationResult.getAuthorizationStatus().equals(AuthorizationStatus.GRANT)) {
                    getNext().invoke(request, response);
                } else {
                    handleErrorResponse(authenticationContext, response, HttpServletResponse.SC_FORBIDDEN, null);
                }
            } catch (AuthzServiceServerException e) {
                handleErrorResponse(authenticationContext, response, HttpServletResponse.SC_BAD_REQUEST, e);
            }
        } else {
            getNext().invoke(request, response);
        }
    }

    private void handleErrorResponse(AuthenticationContext authenticationContext, Response response, int error,
                                     Exception e) throws IOException {

        if (LOG.isDebugEnabled() && e != null) {
            LOG.debug("Authentication error.", e);
        }

        StringBuilder value = new StringBuilder(16);
        value.append("realm user=\"");
        if (authenticationContext != null && authenticationContext.getUser() != null) {
            value.append(authenticationContext.getUser().getUserName());
        }
        value.append('\"');
        response.setHeader(AUTH_HEADER_NAME, value.toString());
        response.sendError(error);
    }

    private boolean isUserEmpty(AuthenticationContext authenticationContext) {

        return (authenticationContext.getUser() == null || StringUtils.isEmpty(authenticationContext.getUser()
                .getUserName()));
    }

    private boolean canHandle(String requestPath, String getHttpMethod) {

        return Pattern.matches(REGEX_FOR_URLS_WITH_ORG_ID, requestPath) && (HTTP_GET.equalsIgnoreCase(getHttpMethod) ||
                HTTP_DELETE.equalsIgnoreCase(getHttpMethod) || HTTP_PUT.equalsIgnoreCase(getHttpMethod) ||
                HTTP_PATCH.equalsIgnoreCase(getHttpMethod));
    }
}
