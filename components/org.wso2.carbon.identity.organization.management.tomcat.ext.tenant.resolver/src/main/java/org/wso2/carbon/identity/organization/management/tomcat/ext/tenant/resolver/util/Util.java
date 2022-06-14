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

package org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.util;

import org.apache.catalina.connector.Request;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.core.persistence.UmPersistenceManager;
import org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.dao.OrganizationManagementTenantResolverDAO;
import org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.dao.OrganizationManagementTenantResolverDAOImpl;
import org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.exception.OrganizationManagementTenantResolverServerException;
import org.wso2.carbon.tomcat.ext.utils.URLMappingHolder;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;

/**
 * This class provides utility functions for the organization management tenant derivation.
 */
public class Util {

    private static final String ORGANIZATION_PATH_PARAM = "/o/";

    /**
     * Retrieve the tenant domain from request.
     *
     * @param request The HTTP servlet request.
     * @return tenant domain.
     * @throws OrganizationManagementTenantResolverServerException The server exception thrown when retrieving the
     *                                                             tenant domain of an organization.
     */
    public static String getTenantDomain(HttpServletRequest request)
            throws OrganizationManagementTenantResolverServerException {

        String requestURI = request.getRequestURI();
        String domain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        if (StringUtils.isBlank(requestURI)) {
            return domain;
        }
        domain = getTenantDomainFromOrgDomain(requestURI, domain);
        return domain;
    }

    /**
     * Retrieve the tenant domain from request.
     *
     * @param request The HTTP servlet request.
     * @return tenant domain.
     * @throws OrganizationManagementTenantResolverServerException The server exception thrown when retrieving the
     *                                                             tenant domain of an organization.
     */
    public static String getTenantDomainFromURLMapping(Request request)
            throws OrganizationManagementTenantResolverServerException {

        String requestURI = request.getRequestURI();
        String domain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        String serverName = request.getHost().getName();
        String appContext = URLMappingHolder.getInstance().getApplicationFromUrlMapping(serverName); //
        if (appContext != null) {
            requestURI = appContext;
        }
        domain = getTenantDomainFromOrgDomain(requestURI, domain);
        return domain;
    }

    private static String getTenantDomainFromOrgDomain(String requestURI, String domain) throws
            OrganizationManagementTenantResolverServerException {

        String domainInRequestPath = requestURI.substring(requestURI.indexOf(ORGANIZATION_PATH_PARAM) + 3);
        if (domainInRequestPath.indexOf('/') != -1) {
            domainInRequestPath = domainInRequestPath.substring(0, domainInRequestPath.indexOf('/'));
            // todo: check if it is ok to directly have the root org id or need to fetch it from db.
            if (StringUtils.equals("root4a8d-113f-4211-a0d5-efe36b082211", domainInRequestPath)) {
                // super tenant domain will be returned.
                return domain;
            }
            OrganizationManagementTenantResolverDAO tenantResolverDAO =
                    new OrganizationManagementTenantResolverDAOImpl();
            domain = tenantResolverDAO.resolveTenantDomain(domainInRequestPath);
        }
        return domain;
    }

    /**
     * Get a new Jdbc template.
     *
     * @return a new Jdbc template.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(UmPersistenceManager.getInstance().getDataSource());
    }
}
