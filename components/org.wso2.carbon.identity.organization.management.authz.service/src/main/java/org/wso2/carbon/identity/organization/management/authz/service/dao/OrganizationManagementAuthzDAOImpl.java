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

package org.wso2.carbon.identity.organization.management.authz.service.dao;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.authz.service.exception.OrganizationManagementAuthzServiceServerException;

import java.util.Collections;
import java.util.List;

import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.SCIM_ROLE_ID_ATTR_NAME;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.GET_PERMISSIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.GET_ROLE_NAMES_FOR_TENANT;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.GET_SCIM_ROLE_IDS_FOR_ORGANIZATION_USER;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.ROLE_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SCIM_GROUP_ATTR_VALUE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_NAME;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_VALUE;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_USER_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.VIEW_COLUMN_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getAllowedPermissions;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getNewTemplateForIdentityDatabase;

/**
 * Implementation of {@link OrganizationManagementAuthzDAO}.
 */
public class OrganizationManagementAuthzDAOImpl implements OrganizationManagementAuthzDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementAuthzDAOImpl.class);

    @Override
    public boolean isUserAuthorized(String userId, String resourceId, String orgId, int tenantId)
            throws OrganizationManagementAuthzServiceServerException {

        List<String> permissions = getAllowedPermissions(resourceId);
        List<String> userPermissions = getUserAssignedPermissions(tenantId, userId, orgId);
        if (CollectionUtils.isEmpty(userPermissions)) {
            return false;
        }
        for (String userPermission : userPermissions) {
            if (permissions.contains(userPermission)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getUserAssignedPermissions(int tenantId, String userId, String orgId)
            throws OrganizationManagementAuthzServiceServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        List<String> permissions = Collections.emptyList();
        try {
            List<String> roleNamesList = getRoleNames(userId, orgId, tenantId);
            if (CollectionUtils.isEmpty(roleNamesList)) {
                return permissions;
            }
            permissions = namedJdbcTemplate.withTransaction(template ->
                    template.executeQuery(buildQueryForGettingPermissions(roleNamesList),
                            (resultSet, rowNumber) -> resultSet.getString(1)));
        } catch (TransactionException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
        return permissions;
    }

    private List<String> getUserAssignedRolesInOrganization(String userId, String organizationId, int tenantId)
            throws OrganizationManagementAuthzServiceServerException {

        List<String> roleList;
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            roleList = namedJdbcTemplate.executeQuery(GET_SCIM_ROLE_IDS_FOR_ORGANIZATION_USER,
                            (resultSet, rowNumber) ->
                                    resultSet.getString(ROLE_ID),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_USER_ID, userId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_ORGANIZATION_ID, organizationId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                            });
        } catch (DataAccessException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
        return roleList;
    }

    private List<String> getRoleNames(String userId, String organizationId, int tenantId)
            throws OrganizationManagementAuthzServiceServerException {

        List<String> roleIdList = getUserAssignedRolesInOrganization(userId, organizationId, tenantId);
        List<String> roleNamesList = Collections.emptyList();
        if (CollectionUtils.isEmpty(roleIdList)) {
            return roleNamesList;
        }
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplateForIdentityDatabase();
        try {
            roleNamesList = namedJdbcTemplate.withTransaction(template ->
                    template.executeQuery(buildQueryForGettingRoleNames(roleIdList.size()),
                            (resultSet, rowNumber) -> resultSet.getString(VIEW_COLUMN_ROLE_NAME)
                                    .split("/")[1].trim(),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ATTR_NAME,
                                        SCIM_ROLE_ID_ATTR_NAME);
                                int index = 0;
                                for (String roleId : roleIdList) {
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ATTR_VALUE + (index++),
                                            roleId);
                                }
                            }));
        } catch (TransactionException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
        return roleNamesList;
    }

    private String buildQueryForGettingRoleNames(int numberOfRoles) {

        StringBuilder sb = new StringBuilder();
        sb.append(GET_ROLE_NAMES_FOR_TENANT).append("(");
        for (int i = 0; i < numberOfRoles; i++) {
            sb.append(String.format(SCIM_GROUP_ATTR_VALUE, i));
            if (i != numberOfRoles - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildQueryForGettingPermissions(List<String> roleNamesList) {

        StringBuilder sb = new StringBuilder();
        int numberOfRoles = roleNamesList.size();
        sb.append("(");
        for (int i = 0; i < numberOfRoles; i++) {
            sb.append(ROLE_NAME).append("='").append(roleNamesList.get(i)).append("'");
            if (i != numberOfRoles - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return String.format(GET_PERMISSIONS_FOR_USER, sb);
    }
}
