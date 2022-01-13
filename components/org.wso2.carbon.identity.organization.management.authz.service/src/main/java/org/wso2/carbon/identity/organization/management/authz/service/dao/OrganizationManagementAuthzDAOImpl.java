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

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.organization.management.authz.service.exception.OrganizationManagementAuthzServiceServerException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wso2.carbon.identity.organization.management.authz.service.constant.AuthorizationConstants.PERMISSION_SPLITTER;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.GET_AUTHORIZED_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.GET_PERMISSIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.PERMISSION_LIST_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_USER_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getNewTemplate;

/**
 * Implementation of {@link OrganizationManagementAuthzDAO}.
 */
public class OrganizationManagementAuthzDAOImpl implements OrganizationManagementAuthzDAO {

    @Override
    public boolean isUserAuthorized(String userId, String resourceId, String orgId, int tenantId)
            throws OrganizationManagementAuthzServiceServerException {

        List<String> permissions = getAllowedPermissions(resourceId);
        for (String userPermission : getUserAssignedPermissions(tenantId, userId, orgId)) {
            if (permissions.contains(userPermission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getUserAuthorizedOrganizations(String userId, String resourceId, int tenantId) throws
            OrganizationManagementAuthzServiceServerException {

        String permissionPlaceholder = "PERMISSION_";
        List<String> permissions = getAllowedPermissions(resourceId);
        List<String> permissionPlaceholders = new ArrayList<>();
        // Constructing the placeholders required to hold the permission strings in the named prepared statement.
        for (int i = 1; i <= permissions.size(); i++) {
            permissionPlaceholders.add(":" + permissionPlaceholder + i + ";");
        }
        String placeholder = String.join(", ", permissionPlaceholders);
        String sql = GET_AUTHORIZED_ORGANIZATIONS.replace(PERMISSION_LIST_PLACEHOLDER, placeholder);

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        List<String> organizations = new ArrayList<>();
        try {
            namedJdbcTemplate.executeQuery(sql,
                    (resultSet, rowNumber) -> organizations.add(resultSet.getString(1)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_USER_ID, userId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        int index = 1;
                        for (String permission : permissions) {
                            namedPreparedStatement.setString(permissionPlaceholder + index, permission);
                            index++;
                        }
                    }
            );
        } catch (DataAccessException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
        return organizations;
    }

    private List<String> getUserAssignedPermissions(int tenantId, String userId, String orgId)
            throws OrganizationManagementAuthzServiceServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        List<String> permissions = new ArrayList<>();
        try {
            namedJdbcTemplate.executeQuery(GET_PERMISSIONS_FOR_USER,
                    (resultSet, rowNumber) -> permissions.add(resultSet.getString(1)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_USER_ID, userId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_ORGANIZATION_ID, orgId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    }
            );
        } catch (DataAccessException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
        return permissions;
    }

    private List<String> getAllowedPermissions(String resourceId) {

        String[] permissionParts = resourceId.split(PERMISSION_SPLITTER);
        List<String> allowedPermissions = new ArrayList<>();
        for (int i = 0; i < permissionParts.length - 1; i++) {
            allowedPermissions.add(String.join(PERMISSION_SPLITTER,
                    subArray(permissionParts, permissionParts.length - i)));
        }
        return allowedPermissions;
    }

    private static <T> T[] subArray(T[] array, int end) {

        return Arrays.copyOfRange(array, 0, end);
    }
}
