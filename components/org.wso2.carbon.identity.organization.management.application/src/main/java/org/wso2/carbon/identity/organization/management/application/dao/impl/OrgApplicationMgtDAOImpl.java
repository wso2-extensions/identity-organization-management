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

package org.wso2.carbon.identity.organization.management.application.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.VIEW_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.INSERT_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_LINK_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * This class implements the {@link OrgApplicationMgtDAO} interface.
 */
public class OrgApplicationMgtDAOImpl implements OrgApplicationMgtDAO {

    private static final Log LOG = LogFactory.getLog(OrgApplicationMgtDAOImpl.class);

    @Override
    public void addSharedApplication(int ownerTenantId, String mainAppId, int sharedTenantId, String sharedAppId)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(INSERT_SHARED_APP, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_APP_ID, mainAppId);
                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_PARENT_TENANT_ID, ownerTenantId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID, sharedAppId);
                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_SHARED_TENANT_ID, sharedTenantId);
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_LINK_APPLICATIONS, e, mainAppId, sharedAppId);
        }
    }

    @Override
    public Optional<String> getSharedApplicationResourceId(int parentTenantId, int sharedTenantId, String parentAppId)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        String sharedAppId;
        try {
            sharedAppId = namedJdbcTemplate.fetchSingleRecord(GET_SHARED_APP_ID,
                    (resultSet, rowNumber) -> resultSet.getString(VIEW_SHARED_APP_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_PARENT_TENANT_ID, parentTenantId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_SHARED_TENANT_ID, sharedTenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_APP_ID, parentAppId);
                    });
            return Optional.ofNullable(sharedAppId);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, parentAppId,
                    Integer.toString(parentTenantId));
        }
    }
}
