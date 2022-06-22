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
import java.util.List;

import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.IS_USER_AUTHORIZED;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.PERMISSION_LIST_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_COUNT_UM_RESOURCE_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_USER_ID;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getAllowedPermissions;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getNewTemplate;

/**
 * Implementation of {@link OrganizationManagementAuthzDAO}.
 */
public class OrganizationManagementAuthzDAOImpl implements OrganizationManagementAuthzDAO {

    @Override
    public boolean isUserAuthorized(String userId, String resourceId, String orgId)
            throws OrganizationManagementAuthzServiceServerException {

        String permissionPlaceholder = "PERMISSION_";
        List<String> permissions = getAllowedPermissions(resourceId);
        List<String> permissionPlaceholders = new ArrayList<>();
        // Constructing the placeholders required to hold the permission strings in the named prepared statement.
        for (int i = 1; i <= permissions.size(); i++) {
            permissionPlaceholders.add(":" + permissionPlaceholder + i + ";");
        }
        String placeholder = String.join(", ", permissionPlaceholders);
        String sqlStmt = IS_USER_AUTHORIZED.replace(PERMISSION_LIST_PLACEHOLDER, placeholder);

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(sqlStmt,
                    (resultSet, rowNumber) -> resultSet.getInt(DB_SCHEMA_COLUMN_NAME_COUNT_UM_RESOURCE_ID) > 0,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_USER_ID, userId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_ORGANIZATION_ID, orgId);
                        int index = 1;
                        for (String permission : permissions) {
                            namedPreparedStatement.setString(permissionPlaceholder + index, permission);
                            index++;
                        }
                    });
        } catch (DataAccessException e) {
            throw new OrganizationManagementAuthzServiceServerException(e);
        }
    }
}
