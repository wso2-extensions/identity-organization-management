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

package org.wso2.carbon.identity.organization.management.role.management.service.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.core.persistence.UmPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.role.management.service.constant.RoleManagementConstants;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exception.RoleManagementServerException;

import java.util.UUID;

/**
 * Utility class containing utility functions for role management.
 */
public class Utils {

    /**
     * Get an instance of NamedJdbcTemplate.
     *
     * @return A new instance of NamedJdbcTemplate.
     */
    public static NamedJdbcTemplate getNewNamedJdbcTemplate() {

        return new NamedJdbcTemplate(UmPersistenceManager.getInstance().getDataSource());
    }

    /**
     * Get an instance of NamedJdbcTemplate for Identity Database.
     *
     * @return A new Instance of NamedJdbcTemplate.
     */
    public static NamedJdbcTemplate getNewTemplateForIdentityDatabase() {

        return new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
    }

    /**
     * Creates a new random unique ID.
     *
     * @return A unique ID.
     */
    public static String generateUniqueId() {

        return UUID.randomUUID().toString();
    }

    /**
     * Get the tenant ID.
     *
     * @return The tenant ID.
     */
    public static int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    /**
     * Get the tenant domain.
     *
     * @return The tenant domain.
     */
    public static String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    /**
     * Throw an RoleManagementClientException upon client side error in role management.
     *
     * @param error The error enum.
     * @param data  The error message data.
     * @return RoleManagementClientException.
     */
    public static RoleManagementClientException handleClientException(RoleManagementConstants.ErrorMessages error,
                                                                      String... data) {
        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new RoleManagementClientException(error.getMessage(), description, error.getCode());
    }

    /**
     * Throw an RoleManagementServerException upon client side error in role management.
     *
     * @param error The error enum.
     * @param cause The throwable cause.
     * @param data  The error message data.
     * @return RoleManagementServerException.
     */
    public static RoleManagementServerException handleServerException(RoleManagementConstants.ErrorMessages error,
                                                                      Throwable cause, String... data) {
        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new RoleManagementServerException(error.getMessage(), description, error.getCode(), cause);
    }

    /**
     * Builds the API context whether the tenant qualified URL is enabled or not. In tenant qualified mode the
     * ServiceURLBuilder appends the tenant domain to the URI as a path param automatically. But in non tenant
     * qualified mode we need to append the tenant domain to the path manually.
     * Same goes for the organization qualified URL.
     *
     * @param endpoint Relative endpoint path.
     * @return Context of the API.
     */
    public static String getContext(String endpoint) {

        String context;
        if (IdentityTenantUtil.isTenantQualifiedUrlsEnabled()) {
            context = RoleManagementConstants.ORGANIZATION_MANAGEMENT_API_PATH_COMPONENT + endpoint;
        } else {
            context = String.format(RoleManagementConstants.TENANT_CONTEXT_PATH_COMPONENT,
                    getTenantDomainFromContext()) +
                    RoleManagementConstants.ORGANIZATION_MANAGEMENT_API_PATH_COMPONENT + endpoint;
        }
        return context;
    }

    /**
     * Get tenant domain from context.
     *
     * @return The tenant domain.
     */
    private static String getTenantDomainFromContext() {

        String tenantDomain = IdentityTenantUtil.getTenantDomainFromContext();
        if (StringUtils.isBlank(tenantDomain)) {
            tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        }
        return tenantDomain;
    }
}
