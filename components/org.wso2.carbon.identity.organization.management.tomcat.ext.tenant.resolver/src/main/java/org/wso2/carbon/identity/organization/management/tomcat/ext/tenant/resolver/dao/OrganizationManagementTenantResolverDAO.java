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

package org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.dao;

import org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.exception.OrganizationManagementTenantResolverServerException;

/**
 * DAO to resolve the tenant domain of a tenant type organization.
 */
public interface OrganizationManagementTenantResolverDAO {

    /**
     * Derive the tenant domain of an organization based on the given organization domain.
     *
     * @param organizationDomain The organization domain available in the request. This domain will be the same unique
     *                           identifier generated for an organization.
     * @return tenant domain.
     * @throws OrganizationManagementTenantResolverServerException The server exception thrown when retrieving the
     *                                                             tenant domain of an organization.
     */
    String resolveTenantDomain(String organizationDomain) throws OrganizationManagementTenantResolverServerException;
}
