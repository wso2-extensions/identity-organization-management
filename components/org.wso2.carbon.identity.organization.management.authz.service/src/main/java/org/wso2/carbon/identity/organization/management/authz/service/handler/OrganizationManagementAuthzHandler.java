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

package org.wso2.carbon.identity.organization.management.authz.service.handler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.authz.service.AuthorizationContext;
import org.wso2.carbon.identity.authz.service.AuthorizationResult;
import org.wso2.carbon.identity.authz.service.AuthorizationStatus;
import org.wso2.carbon.identity.authz.service.exception.AuthzServiceServerException;
import org.wso2.carbon.identity.authz.service.handler.AuthorizationHandler;
import org.wso2.carbon.identity.core.handler.InitConfig;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationContext;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationManager;
import org.wso2.carbon.identity.organization.management.authz.service.exception.OrganizationManagementAuthzServiceServerException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.wso2.carbon.identity.auth.service.util.Constants.OAUTH2_ALLOWED_SCOPES;
import static org.wso2.carbon.identity.auth.service.util.Constants.OAUTH2_VALIDATE_SCOPE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.ORGANIZATION_RESOURCE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.REGEX_FOR_URLS_WITH_ORG_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.RESOURCE_PERMISSION_NONE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.SCOPE_PERMISSION_MAP;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.URI_SPLITTER;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getUserStoreManager;

/**
 * Authorization handler to handle organization management related authorization.
 */
public class OrganizationManagementAuthzHandler extends AuthorizationHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementAuthzHandler.class);

    @Override
    public AuthorizationResult handleAuthorization(AuthorizationContext authorizationContext)
            throws AuthzServiceServerException {

        if (!(authorizationContext instanceof OrganizationManagementAuthorizationContext)) {
            return super.handleAuthorization(authorizationContext);
        }
        AuthorizationResult authorizationResult = new AuthorizationResult(AuthorizationStatus.DENY);

        String requestUri = ((OrganizationManagementAuthorizationContext) authorizationContext).getRequestUri();
        User user = authorizationContext.getUser();
        String userDomain = user.getTenantDomain();
        int tenantId = IdentityTenantUtil.getTenantId(userDomain);
        String permissionString = authorizationContext.getPermissionString();
        String[] allowedScopes = authorizationContext.getParameter(OAUTH2_ALLOWED_SCOPES) == null ? null :
                (String[]) authorizationContext.getParameter(OAUTH2_ALLOWED_SCOPES);
        boolean validateScope = authorizationContext.getParameter(OAUTH2_VALIDATE_SCOPE) == null ? false :
                (Boolean) authorizationContext.getParameter(OAUTH2_VALIDATE_SCOPE);

        String organizationId = extractOrganizationId(requestUri);
        if (StringUtils.isNotBlank(organizationId)) {
            try {
                // If the scopes are configured for the API, it gets the first priority.
                if (isScopeValidationRequired(validateScope, authorizationContext)) {
                    validateScopes(tenantId, organizationId, allowedScopes, user, authorizationContext,
                            authorizationResult);
                } else if (StringUtils.isNotBlank(permissionString)) {
                    validatePermissions(tenantId, organizationId, permissionString, user, authorizationResult);
                }
            } catch (OrganizationManagementAuthzServiceServerException e) {
                String errorMessage = "Error occurred while evaluating authorization of user for organization " +
                        "management." + e.getMessage();
                LOG.error(errorMessage);
                throw new AuthzServiceServerException(errorMessage, e);
            }
        }
        return authorizationResult;
    }

    @Override
    public void init(InitConfig initConfig) {

    }

    @Override
    public String getName() {

        return "OrganizationManagementAuthorizationHandler";
    }

    @Override
    public int getPriority() {

        return 50;
    }

    private void validatePermissions(int tenantId, String orgId, String permissionString, User user,
                                     AuthorizationResult authorizationResult)
            throws OrganizationManagementAuthzServiceServerException {

        if (RESOURCE_PERMISSION_NONE.equalsIgnoreCase(permissionString)) {
            authorizationResult.setAuthorizationStatus(AuthorizationStatus.GRANT);
            return;
        }

        boolean isUserAuthorized = OrganizationManagementAuthorizationManager.getInstance().isUserAuthorized
                (getUserId(user), permissionString, orgId);
        if (isUserAuthorized) {
            authorizationResult.setAuthorizationStatus(AuthorizationStatus.GRANT);
        }
    }

    private String getUserId(User user) throws OrganizationManagementAuthzServiceServerException {

        try {
            AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) getUserStoreManager(user);
            return userStoreManager.getUser(null, user.getUserName()).getUserID();
        } catch (UserStoreException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
    }

    private String extractOrganizationId(String requestPath) {

        if (Pattern.matches(REGEX_FOR_URLS_WITH_ORG_ID, requestPath)) {
            String[] requestUriParts = requestPath.split(URI_SPLITTER);
            return Arrays.asList(requestUriParts).get((Arrays.asList(requestUriParts).indexOf(ORGANIZATION_RESOURCE))
                    + 1);
        }
        return null;
    }

    private boolean isScopeValidationRequired(boolean validateScope, AuthorizationContext authorizationContext) {

        return validateScope && CollectionUtils.isNotEmpty(authorizationContext.getRequiredScopes());
    }

    private void validateScopes(int tenantId, String orgId, String[] tokenScopes, User user,
                                AuthorizationContext authorizationContext, AuthorizationResult authorizationResult)
            throws OrganizationManagementAuthzServiceServerException {

        boolean granted = true;
        if (tokenScopes != null) {
            // todo: check on scope validation during token issuance.
            for (String scope : authorizationContext.getRequiredScopes()) {
                if (!ArrayUtils.contains(tokenScopes, scope)) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                List<String> permissions = new ArrayList<>();
                for (Map.Entry<String, String> entry : SCOPE_PERMISSION_MAP.entrySet()) {
                    String scope = entry.getKey();
                    for (String tokenScope : tokenScopes) {
                        if (StringUtils.equals(tokenScope, scope)) {
                            permissions.add(entry.getValue());
                        }
                    }
                }
                for (String permissionString : permissions) {
                    boolean isUserAuthorized = OrganizationManagementAuthorizationManager.getInstance()
                            .isUserAuthorized(getUserId(user), permissionString, orgId);
                    if (isUserAuthorized) {
                        authorizationResult.setAuthorizationStatus(AuthorizationStatus.GRANT);
                        break;
                    }
                }
            }
        }
    }
}
