/*
 * Copyright (c) 2022-2024, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtUtil;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.user.core.UserStoreClientException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.Error.INVALID_OFFSET;
import static org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants.Error.SORTING_NOT_IMPLEMENTED;
import static org.wso2.carbon.identity.application.mgt.ApplicationMgtUtil.getConsoleAccessUrlFromServerConfig;
import static org.wso2.carbon.identity.application.mgt.ApplicationMgtUtil.getMyAccountAccessUrlFromServerConfig;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.DELETE_SHARED_APP_LINKS_OF_ORG;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_FILTERED_SHARED_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.GET_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.HAS_FRAGMENT_APPS;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.INSERT_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.IS_FRAGMENT_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.IS_FRAGMENT_APPLICATION_H2;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_INFORMIX;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_MSSQL;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_MYSQL;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_ORACLE;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_POSTGRESL;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_INFORMIX;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_MSSQL;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_MYSQL;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_ORACLE;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_POSTGRES;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APP_COUNT_BY_APP_NAME_AND_TENANT;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.LOAD_DISCOVERABLE_SHARED_APP_COUNT_BY_TENANT;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_NAME;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_METADATA_VALUE;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_APP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SP_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.SHARED_ORG_ID_LIST_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.SQLPlaceholders.SHARED_ORG_ID_PLACEHOLDER_PREFIX;
import static org.wso2.carbon.identity.organization.management.application.constant.SQLConstants.UPDATE_SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_APPLICATION_HAS_FRAGMENTS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_APPLICATION_IS_A_FRAGMENT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_SHARED_APPLICATION_LINK;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_LINK_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isMySqlDB;

/**
 * This class implements the {@link OrgApplicationMgtDAO} interface.
 */
public class OrgApplicationMgtDAOImpl implements OrgApplicationMgtDAO {

    private static final Log log = LogFactory.getLog(OrgApplicationMgtDAOImpl.class);
    private static final String ASTERISK = "*";

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
                    namedPreparedStatement.setBoolean(DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN,
                            shareWithAllChildren);
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
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_SP_APP_ID, sharedAppId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);
                    });
            return Optional.ofNullable(sharedApplicationDO);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, Integer.toString(sharedAppId),
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
            String prepStmt = JdbcUtils.isH2DB() ? IS_FRAGMENT_APPLICATION_H2 : IS_FRAGMENT_APPLICATION;
            int isFragment = namedJdbcTemplate.fetchSingleRecord(prepStmt,
                    (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_SP_ID, applicationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_METADATA_NAME, IS_FRAGMENT_APP);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_METADATA_VALUE, Boolean.TRUE.toString());
                    });
            return isFragment > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_APPLICATION_IS_A_FRAGMENT, e,
                    String.valueOf(applicationId));
        }
    }

    @Override
    public void updateShareWithAllChildren(String mainApplicationId, String ownerOrganizationId,
                                           boolean shareWithAllChildren) throws OrganizationManagementException {
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(UPDATE_SHARE_WITH_ALL_CHILDREN, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID, mainApplicationId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID, ownerOrganizationId);
                    namedPreparedStatement.setBoolean(DB_SCHEMA_COLUMN_NAME_SHARE_WITH_ALL_CHILDREN,
                            shareWithAllChildren);
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, mainApplicationId);
        }
    }

    @Override
    public List<SharedApplicationDO> getSharedApplications(String mainAppId, String ownerOrgId,
                                                           List<String> sharedOrgIds)
            throws OrganizationManagementException {

        if (CollectionUtils.isEmpty(sharedOrgIds)) {
            return Collections.emptyList();
        }

        String placeholders = IntStream.range(0, sharedOrgIds.size())
                .mapToObj(i -> ":" + SHARED_ORG_ID_PLACEHOLDER_PREFIX + i + ";")
                .collect(Collectors.joining(", "));
        String sqlStmt = GET_FILTERED_SHARED_APPLICATIONS.replace(SHARED_ORG_ID_LIST_PLACEHOLDER, placeholders);

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(sqlStmt,
                    (resultSet, rowNumber) -> new SharedApplicationDO(
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID),
                            resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_APP_ID)),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_MAIN_APP_ID, mainAppId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_OWNER_ORG_ID, ownerOrgId);
                        for (int i = 0; i < sharedOrgIds.size(); i++) {
                            namedPreparedStatement.setString(SHARED_ORG_ID_PLACEHOLDER_PREFIX + i, sharedOrgIds.get(i));
                        }
                    });
        } catch (DataAccessException e) {
            throw handleServerException(
                    ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppId, ownerOrgId);
        }
    }

    @Override
    public List<ApplicationBasicInfo> getDiscoverableSharedApplicationBasicInfo(int limit, int offset, String filter,
                                                                          String sortOrder, String sortBy,
                                                                          String tenantDomain, String rootOrgId)
            throws OrganizationManagementException {

        validateForUnImplementedSortingAttributes(sortOrder, sortBy);
        validateAttributesForPagination(offset, limit);

        // TODO: 17/9/24 : Enforce a max limit
        if (StringUtils.isBlank(filter) || ASTERISK.equals(filter)) {
            return getDiscoverableSharedApplicationBasicInfo(limit, offset, tenantDomain, rootOrgId);
        }

        String filterResolvedForSQL = resolveSQLFilter(filter);

        HashMap<Integer, ApplicationBasicInfo> applicationBasicInfos = new HashMap<>();

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            String databaseVendorType = connection.getMetaData().getDatabaseProductName();

            try (NamedPreparedStatement statement =
                         new NamedPreparedStatement(connection,
                                 getDBVendorSpecificDiscoverableSharedAppRetrievalQueryByAppName(databaseVendorType))) {
                statement.setString(1, tenantDomain);
                statement.setString(2, filterResolvedForSQL);
                statement.setString(3, rootOrgId);
                statement.setInt(4, offset);
                statement.setInt(5, limit);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        buildDiscoverableAppBasicInfo(applicationBasicInfos, resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            throw new OrganizationManagementException("Error while getting application basic information" +
                    " for discoverable applications in tenantDomain: " + tenantDomain, e);
        }
        return Collections.unmodifiableList(new ArrayList<>(applicationBasicInfos.values()));
    }

    @Override
    public int getCountOfDiscoverableSharedApplications(String filter, String tenantDomain, String rootOrgId)
            throws OrganizationManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Getting count of discoverable shared applications matching filter: " + filter + " in " +
                    "tenantDomain: " + tenantDomain);
        }

        if (StringUtils.isBlank(filter) || ASTERISK.equals(filter)) {
            return getCountOfDiscoverableSharedApplications(tenantDomain, rootOrgId);
        }

        int count = 0;
        String filterResolvedForSQL = resolveSQLFilter(filter);
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {

            try (NamedPreparedStatement statement =
                         new NamedPreparedStatement(connection,
                                 LOAD_DISCOVERABLE_SHARED_APP_COUNT_BY_APP_NAME_AND_TENANT)) {
                statement.setString(1, tenantDomain);
                statement.setString(2, filterResolvedForSQL);
                statement.setString(3, rootOrgId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String groupId = resultSet.getString(ApplicationConstants.ApplicationTableColumns.GROUP_ID);
                        if (groupId == null || checkLoggedInUserIsInGroup(groupId)) {
                            count++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new OrganizationManagementServerException("Error while getting count of discoverable " +
                    "applications matching filter:" + filter + " in tenantDomain: " + tenantDomain);
        }
        return count;
    }

    @Override
    public void deleteSharedAppLinks(String organizationId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_SHARED_APP_LINKS_OF_ORG, preparedStatement -> {
                preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ORG_ID, organizationId);
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_SHARED_APPLICATION_LINK, e, organizationId);
        }
    }

    private int getCountOfDiscoverableSharedApplications(String tenantDomain, String rootOrgId)
            throws OrganizationManagementException {

        int count = 0;
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {

            try (NamedPreparedStatement statement =
                         new NamedPreparedStatement(connection,
                                 LOAD_DISCOVERABLE_SHARED_APP_COUNT_BY_TENANT)) {
                statement.setString(1, tenantDomain);
                statement.setString(2, rootOrgId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String groupId = resultSet.getString(ApplicationConstants.ApplicationTableColumns.GROUP_ID);
                        if (groupId == null || checkLoggedInUserIsInGroup(groupId)) {
                            count++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new OrganizationManagementServerException("Error while getting count of discoverable " +
                    "shared applications in tenantDomain: " + tenantDomain);
        }
        return count;
    }

    private List<ApplicationBasicInfo> getDiscoverableSharedApplicationBasicInfo(int limit, int offset,
                                                                           String tenantDomain, String rootOrgId)
            throws OrganizationManagementException {

        HashMap<Integer, ApplicationBasicInfo> applicationBasicInfos = new HashMap<>();

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            String databaseVendorType = connection.getMetaData().getDatabaseProductName();

            try (NamedPreparedStatement statement =
                         new NamedPreparedStatement(connection,
                                 getDBVendorSpecificDiscoverableSharedAppRetrievalQuery(databaseVendorType))) {
                statement.setString(1, tenantDomain);
                statement.setString(2, rootOrgId);
                statement.setInt(3, offset);
                statement.setInt(4, limit);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        buildDiscoverableAppBasicInfo(applicationBasicInfos, resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            throw new OrganizationManagementException("Error while getting application basic information" +
                    " for discoverable applications in tenantDomain: " + tenantDomain, e);
        }
        return Collections.unmodifiableList(new ArrayList<>(applicationBasicInfos.values()));
    }

    /**
     * Build the discoverable application basic information from the result set.
     *
     * @param applicationBasicInfos HashMap to store the application basic information.
     * @param resultSet             Current result set.
     * @throws SQLException                           Error while reading the result set.
     * @throws OrganizationManagementException Error while building the application basic information.
     */
    private void buildDiscoverableAppBasicInfo(HashMap<Integer, ApplicationBasicInfo> applicationBasicInfos,
                                               ResultSet resultSet)
            throws SQLException, OrganizationManagementException {

        int applicationId = resultSet.getInt(ApplicationConstants.ApplicationTableColumns.ID);
        if (!applicationBasicInfos.containsKey(applicationId)) {
            String groupId = resultSet.getString(ApplicationConstants.ApplicationTableColumns.GROUP_ID);
            if (groupId == null) {
                applicationBasicInfos.put(applicationId, buildApplicationBasicInfo(resultSet));
                return;
            }
            if (checkLoggedInUserIsInGroup(groupId)) {
                applicationBasicInfos.put(applicationId, buildApplicationBasicInfo(resultSet));
            }
        }
    }

    /**
     * Check whether the logged-in user is in the provided group.
     *
     * @param groupId Group id to check whether the user is in.
     * @return True if the user is in the group.
     * @throws OrganizationManagementException Error while checking the user is in the group.
     */
    private boolean checkLoggedInUserIsInGroup(String groupId) throws OrganizationManagementException {

        String loggedInUserId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        try {
            AbstractUserStoreManager userStoreManager = ApplicationMgtUtil.getUserStoreManager(tenantDomain);
            if (userStoreManager.isUserInGroup(loggedInUserId, groupId)) {
                return true;
            }
        } catch (IdentityApplicationManagementException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode(), e);
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            String msg = "Error while checking the user: " + loggedInUserId + " is in the group: " + groupId +
                    " in tenant: " + tenantDomain;
            if (e instanceof UserStoreClientException) {
                if (log.isDebugEnabled()) {
                    log.debug(msg, e);
                }
            } else {
                throw new OrganizationManagementException(msg, e);
            }
        }
        return false;
    }

    /**
     * Get the value for the shareWithAllChildren column according to the db type.
     *
     * @param shareWithAllChildren The value of the shareWithAllChildren column.
     * @return The value of the shareWithAllChildren column according to the db type.
     * @throws OrganizationManagementServerException If an error occurs while getting the value.
     */
    private String getShareWithAllChildrenValue(boolean shareWithAllChildren)
            throws OrganizationManagementServerException {

        if (isMySqlDB()) {
            return shareWithAllChildren ? "1" : "0";
        }
        return String.valueOf(shareWithAllChildren);
    }

    private void validateForUnImplementedSortingAttributes(String sortOrder, String sortBy)
            throws OrganizationManagementServerException {

        if (StringUtils.isNotBlank(sortBy) || StringUtils.isNotBlank(sortOrder)) {
            throw new OrganizationManagementServerException(SORTING_NOT_IMPLEMENTED.getCode(),
                    "Sorting not supported.");
        }
    }

    /**
     * Validates the offset and limit values for pagination.
     *
     * @param offset Starting index.
     * @param limit  Count value.
     * @throws OrganizationManagementClientException
     */
    private void validateAttributesForPagination(int offset, int limit)
            throws OrganizationManagementClientException {

        if (offset < 0) {
            throw new OrganizationManagementClientException("Invalid offset requested.",
                    "Invalid offset requested. Offset value should be zero or greater than zero.",
                    INVALID_OFFSET.getCode());
        }

        if (limit <= 0) {
            throw new OrganizationManagementClientException("Invalid limit requested.",
                    "Invalid limit requested. Limit value should be greater than zero.",
                    INVALID_OFFSET.getCode());
        }
    }

    private ApplicationBasicInfo buildApplicationBasicInfo(ResultSet appNameResultSet)
            throws SQLException, OrganizationManagementException {

        /*
         * If you add a new value to basicInfo here, please consider to add it in the
         * buildApplicationBasicInfoWithInboundConfig() function also.
         */
        ApplicationBasicInfo basicInfo = new ApplicationBasicInfo();
        basicInfo.setApplicationId(appNameResultSet.getInt(ApplicationConstants.ApplicationTableColumns.ID));
        basicInfo.setApplicationName(appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.APP_NAME));
        basicInfo.setDescription(appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.DESCRIPTION));

        basicInfo.setApplicationResourceId(appNameResultSet.getString(ApplicationConstants
                .ApplicationTableColumns.UUID));
        basicInfo.setImageUrl(appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.IMAGE_URL));

        try {
            basicInfo.setAccessUrl(appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.ACCESS_URL));
            if (ApplicationMgtUtil.isConsoleOrMyAccount(basicInfo.getApplicationName())) {
                basicInfo.setAccessUrl(ApplicationMgtUtil.resolveOriginUrlFromPlaceholders(
                        appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.ACCESS_URL),
                        basicInfo.getApplicationName()));
            }
        } catch (URLBuilderException e) {
            throw new OrganizationManagementException(
                    "Error occurred when resolving origin of the access URL with placeholders", e);
        }
        String tenantDomain = IdentityTenantUtil.getTenantDomain(appNameResultSet.getInt(
                ApplicationConstants.ApplicationTableColumns.TENANT_ID));
        if (ApplicationMgtUtil.isConsole(basicInfo.getApplicationName())) {
            String consoleAccessUrl = getConsoleAccessUrlFromServerConfig(tenantDomain);
            if (StringUtils.isNotBlank(consoleAccessUrl)) {
                basicInfo.setAccessUrl(consoleAccessUrl);
            }
        }
        if (ApplicationMgtUtil.isMyAccount(basicInfo.getApplicationName())) {
            String myAccountAccessUrl = getMyAccountAccessUrlFromServerConfig(tenantDomain);
            if (StringUtils.isNotBlank(myAccountAccessUrl)) {
                basicInfo.setAccessUrl(myAccountAccessUrl);
            }
        }

        String username = appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.USERNAME);
        String userStoreDomain = appNameResultSet.getString(ApplicationConstants.ApplicationTableColumns.USER_STORE);
        int tenantId = appNameResultSet.getInt(ApplicationConstants.ApplicationTableColumns.TENANT_ID);

        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(userStoreDomain)
                && !(tenantId == MultitenantConstants.INVALID_TENANT_ID)) {
            User appOwner = new User();
            appOwner.setUserStoreDomain(userStoreDomain);
            appOwner.setUserName(username);
            appOwner.setTenantDomain(IdentityTenantUtil.getTenantDomain(tenantId));

            basicInfo.setAppOwner(appOwner);
        }

        return basicInfo;
    }

    private String resolveSQLFilter(String filter) {

        //To avoid any issues when the filter string is blank or null, assigning "%" to SQLFilter.
        String sqlfilter = "SP_APP.APP_NAME LIKE '%'";
        if (StringUtils.isNotBlank(filter)) {
            sqlfilter = filter.trim()
                    .replace(ASTERISK, "%")
                    .replace("?", "_");
        }

        if (log.isDebugEnabled()) {
            log.debug("Input filter: " + filter + " resolved for SQL filter: " + sqlfilter);
        }
        return sqlfilter;
    }

    private String getDBVendorSpecificDiscoverableSharedAppRetrievalQuery(String dbVendorType)
            throws OrganizationManagementServerException {

        if ("MySQL".equals(dbVendorType) || "MariaDB".equals(dbVendorType)
                || "H2".equals(dbVendorType)
                || "DB2".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_MYSQL;
        } else if ("Oracle".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_ORACLE;
        } else if ("PostgreSQL".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_POSTGRES;
        } else if ("Microsoft SQL Server".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_MSSQL;
        } else if ("INFORMIX".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_INFORMIX;
        }

        throw new OrganizationManagementServerException("Error while loading discoverable applications from " +
                "DB. Database driver for " + dbVendorType + "could not be identified or not supported.");
    }

    private String getDBVendorSpecificDiscoverableSharedAppRetrievalQueryByAppName(String dbVendorType)
            throws OrganizationManagementServerException {

        if ("MySQL".equals(dbVendorType) || "MariaDB".equals(dbVendorType)
                || "H2".equals(dbVendorType)
                || "DB2".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_MYSQL;
        } else if ("Oracle".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_ORACLE;
        } else if ("PostgreSQL".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_POSTGRESL;
        } else if ("Microsoft SQL Server".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_MSSQL;
        } else if ("INFORMIX".equals(dbVendorType)) {
            return LOAD_DISCOVERABLE_SHARED_APPS_BY_TENANT_AND_APP_NAME_INFORMIX;
        }

        throw new OrganizationManagementServerException("Error while loading discoverable applications from " +
                "DB. Database driver for " + dbVendorType + "could not be identified or not supported.");
    }
}
