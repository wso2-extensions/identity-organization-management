/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.service.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.persistence.UmPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_URL_FOR_RESPONSE_BODY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_PATH;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATH_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SERVER_API_PATH_COMPONENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.TENANT_CONTEXT_PATH_COMPONENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.V1_API_PATH_COMPONENT;

/**
 * This class provides utility functions for the Organization Management.
 */
public class Utils {

    /**
     * Throw an OrganizationManagementClientException upon client side error in organization management.
     *
     * @param error The error enum.
     * @param data  The error message data.
     * @return OrganizationManagementClientException
     */
    public static OrganizationManagementClientException handleClientException(
            OrganizationManagementConstants.ErrorMessages error, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrganizationManagementClientException(error.getMessage(), description, error.getCode());
    }

    /**
     * Throw an OrganizationManagementServerException upon server side error in organization management.
     *
     * @param error The error enum.
     * @param e     The error.
     * @param data  The error message data.
     * @return OrganizationManagementServerException
     */
    public static OrganizationManagementServerException handleServerException(
            OrganizationManagementConstants.ErrorMessages error, Throwable e, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new OrganizationManagementServerException(error.getMessage(), description, error.getCode(), e);
    }

    /**
     * Get a new Jdbc Template.
     *
     * @return a new Jdbc Template.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(UmPersistenceManager.getInstance().getDataSource());
    }

    /**
     * Get the tenant ID.
     *
     * @return the tenant ID.
     */
    public static int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    /**
     * Get the tenant domain.
     *
     * @return the tenant domain.
     */
    public static String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    /**
     * Get the username of the authenticated user.
     *
     * @return the username of the authenticated user.
     */
    public static String getAuthenticatedUsername() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
    }

    /**
     * Get the user ID.
     *
     * @return the user ID.
     */
    public static String getUserId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
    }

    /**
     * Build URI prepending the server API context with the proxy context path to the endpoint.
     *
     * @param organizationId The organization ID.
     * @return Relative URI.
     */
    public static String buildURIForBody(String organizationId) throws OrganizationManagementServerException {

        String context = getContext(V1_API_PATH_COMPONENT + PATH_SEPARATOR + ORGANIZATION_PATH
                + PATH_SEPARATOR + organizationId);

        try {
            return ServiceURLBuilder.create().addPath(context).build().getRelativePublicURL();
        } catch (URLBuilderException e) {
            throw handleServerException(ERROR_CODE_ERROR_BUILDING_URL_FOR_RESPONSE_BODY, e);
        }
    }

    /**
     * Builds the API context on whether the tenant qualified url is enabled or not. In tenant qualified mode the
     * ServiceURLBuilder appends the tenant domain to the URI as a path param automatically. But
     * in non tenant qualified mode we need to append the tenant domain to the path manually.
     *
     * @param endpoint Relative endpoint path.
     * @return Context of the API.
     */
    public static String getContext(String endpoint) {

        String context;
        if (IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
            context = SERVER_API_PATH_COMPONENT + endpoint;
        } else {
            context = String.format(TENANT_CONTEXT_PATH_COMPONENT, getTenantDomainFromContext()) +
                    SERVER_API_PATH_COMPONENT + endpoint;
        }
        return context;
    }

    private static String getTenantDomainFromContext() {

        String tenantDomain = IdentityTenantUtil.getTenantDomainFromContext();
        if (StringUtils.isBlank(tenantDomain)) {
            tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        }
        return tenantDomain;
    }

    /**
     * Generate unique identifier for the organization.
     *
     * @return organization id.
     */
    public static String generateUniqueID() {

        return UUID.randomUUID().toString();
    }
}
