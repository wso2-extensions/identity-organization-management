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

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.exception.OrganizationManagementTenantResolverServerException;

import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.constant.SQLConstants.GET_ORGANIZATION_TENANT_DOMAIN;
import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_ORG_ID;
import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.util.Util.getNewTemplate;

/**
 * Implementation of {@link OrganizationManagementTenantResolverDAO}.
 */
public class OrganizationManagementTenantResolverDAOImpl implements OrganizationManagementTenantResolverDAO {

    @Override
    public String resolveTenantDomain(String organizationDomain) throws
            OrganizationManagementTenantResolverServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_TENANT_DOMAIN,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    namedPreparedStatement -> namedPreparedStatement.setString(DB_SCHEMA_COLUMN_ORG_ID,
                            organizationDomain));
        } catch (DataAccessException e) {
            throw new OrganizationManagementTenantResolverServerException(e);
        }
    }
}
