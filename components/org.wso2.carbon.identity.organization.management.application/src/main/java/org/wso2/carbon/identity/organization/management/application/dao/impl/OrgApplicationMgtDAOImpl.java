/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.HAS_FRAGMENT_APPS;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.INSERT_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.IS_FRAGMENT_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_NAME;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_VALUE;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_ID;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_APPLICATION_HAS_FRAGMENTS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_APPLICATION_IS_A_FRAGMENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_LINK_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * This class implements the {@link OrgApplicationMgtDAO} interface.
 */
public class OrgApplicationMgtDAOImpl implements OrgApplicationMgtDAO {

    @Override
    public void addSharedApplication(String mainAppId, String ownerOrgId, String sharedAppId, String sharedOrgId,
            boolean shareWithAllChildren) throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(INSERT_SHARED_APP, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID, mainAppId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID, ownerOrgId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID, sharedAppId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN,
                            String.valueOf(shareWithAllChildren));
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_LINK_APPLICATIONS, e, mainAppId, sharedAppId);
        }
    }

    @Override
    public List<SharedApplicationDO> getSharedApplications(String organizationId, String applicationId)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_SHARED_APPLICATIONS,
                    (resultSet, rowNumber) -> new SharedApplicationDO(
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID, applicationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID, organizationId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, applicationId,
                    organizationId);
        }
    }

    @Override
    public Optional<SharedApplicationDO> getSharedApplication(int sharedAppId, String sharedOrgId)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            SharedApplicationDO sharedApplicationDO = namedJdbcTemplate.fetchSingleRecord(GET_SHARED_APPLICATION,
                    (resultSet, rowNumber) -> new SharedApplicationDO(
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID),
                            resultSet.getBoolean(DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SP_APP_ID,
                                Integer.toString(sharedAppId));
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);
                    });
            return Optional.ofNullable(sharedApplicationDO);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, String.valueOf(sharedAppId),
                    sharedOrgId);
        }
    }

    @Override
    public Optional<MainApplicationDO> getMainApplication(String sharedAppId, String sharedOrgId)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            MainApplicationDO mainApplicationDO = namedJdbcTemplate.fetchSingleRecord(GET_MAIN_APPLICATION,
                    (resultSet, rowNumber) -> new MainApplicationDO(
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID, sharedAppId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);
                    });
            return Optional.ofNullable(mainApplicationDO);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_MAIN_APPLICATION, e, sharedAppId,
                    sharedOrgId);
        }
    }

    @Override
    public Optional<String> getSharedApplicationResourceId(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        String sharedAppId;
        try {
            sharedAppId = namedJdbcTemplate.fetchSingleRecord(GET_SHARED_APP_ID,
                    (resultSet, rowNumber) -> resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID, mainAppId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID, ownerOrgId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);

                    });
            return Optional.ofNullable(sharedAppId);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppId, ownerOrgId);
        }
    }

    @Override
    public boolean hasFragments(String applicationId) throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        String sharedAppId;
        try {
            int hasFragment = namedJdbcTemplate.fetchSingleRecord(HAS_FRAGMENT_APPS,
                    (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID, applicationId));
            return hasFragment > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_APPLICATION_HAS_FRAGMENTS, e, applicationId);
        }
    }

    @Override
    public boolean isFragmentApplication(int applicationId) throws OrganizationManagementException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            int isFragment = namedJdbcTemplate.fetchSingleRecord(IS_FRAGMENT_APPLICATION,
                    (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SP_ID, Integer.toString(applicationId));
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_METADATA_NAME, IS_FRAGMENT_APP);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_METADATA_VALUE, "true");
                    });
            return isFragment > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_APPLICATION_IS_A_FRAGMENT, e,
                    String.valueOf(applicationId));
        }
    }
}
