/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.dao;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.FilterQueriesUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.FilterQueryBuilder;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementServerException;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_DB2;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_DB2;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CHECK_USER_ORG_ASSOCIATION_EXISTS_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CREATE_ORGANIZATION_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.CREATE_ORGANIZATION_USER_ASSOCIATION_WITH_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_DB2;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_DEFAULT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_MYSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DBTypes.DB_TYPE_POSTGRESQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_ROOT_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER_BY_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_ORGANIZATION_USER_ASSOCIATION_FOR_ROOT_USER_IN_ORG;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_SHARED_ROLES_OF_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_SHARED_USER_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_HEAD;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ASSOCIATIONS_OF_USER_IN_GIVEN_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.GET_USER_ROLE_IN_TENANT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.INSERT_RESTRICTED_EDIT_PERMISSION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.AND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.ASC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ASSOCIATED_USER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_DOMAIN_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_USER_NAME;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_UUID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_USER_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.DESC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.HAS_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.ORG_ID_SCOPE_PLACEHOLDER_PREFIX;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_NAME_USER_NAMES;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_ORG_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.SQLPlaceholders.WHITE_SPACE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SQLConstants.UPDATE_USER_ASSOCIATION_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ERROR_RETRIEVING_USER_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECK_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_OF_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATE_ORGANIZATION_USER_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isDB2DB;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isMSSqlDB;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isMySqlDB;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isOracleDB;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isPostgreSqlDB;
import static org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants.Error.UNEXPECTED_SERVER_ERROR;

/**
 * DAO implementation for managing organization user associations.
 */
public class OrganizationUserSharingDAOImpl implements OrganizationUserSharingDAO {

    @Override
    public void createOrganizationUserAssociation(String userId, String orgId, String associatedUserId,
                                                  String associatedOrgId) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(CREATE_ORGANIZATION_USER_ASSOCIATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(1, userId);
                    namedPreparedStatement.setString(2, orgId);
                    namedPreparedStatement.setString(3, associatedUserId);
                    namedPreparedStatement.setString(4, associatedOrgId);
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION, e, associatedUserId);
        }
    }

    @Override
    public void createOrganizationUserAssociation(String userId, String orgId, String associatedUserId,
                                                  String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(CREATE_ORGANIZATION_USER_ASSOCIATION_WITH_TYPE, namedPreparedStatement -> {
                    namedPreparedStatement.setString(COLUMN_NAME_USER_ID, userId);
                    namedPreparedStatement.setString(COLUMN_NAME_ORG_ID, orgId);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_USER_ID, associatedUserId);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                    namedPreparedStatement.setString(COLUMN_NAME_UM_SHARED_TYPE, sharedType.name());
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION, e, associatedUserId);
        }
    }

    public boolean deleteUserAssociationOfUserByAssociatedOrg(String userId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, userId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER, e,
                    userId);
        }
    }

    @Override
    public boolean deleteUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_USER_ASSOCIATIONS_FOR_ROOT_USER,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId,
                                                                     List<String> orgIdsScope,
                                                                     List<ExpressionNode> expressionNodes,
                                                                     String sortOrder, int limit)
            throws OrganizationManagementException {

        if (CollectionUtils.isEmpty(orgIdsScope)) {
            return Collections.emptyList();
        }

        // (Optional but recommended) Hard-guard sort order because it gets injected into SQL.
        String resolvedSortOrder = ASC_SORT_ORDER.equalsIgnoreCase(sortOrder) ? ASC_SORT_ORDER : DESC_SORT_ORDER;

        FilterQueryBuilder filterQueryBuilder = FilterQueriesUtil.getSharedUserOrgsFilterQueryBuilder(expressionNodes);
        String filterQuery = filterQueryBuilder.getFilterQuery();
        Map<String, String> filterAttributeValue = filterQueryBuilder.getFilterAttributeValue();

        String orgIdPlaceholders = IntStream.range(0, orgIdsScope.size())
                .mapToObj(i -> ":" + ORG_ID_SCOPE_PLACEHOLDER_PREFIX + i + ";")
                .collect(Collectors.joining(", "));

        String sql = buildGetUserAssociationsSql(filterQuery, orgIdPlaceholders, resolvedSortOrder, limit);

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(sql,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return userAssociation;
                    },
                    ps -> {
                        // base params
                        ps.setString(COLUMN_NAME_ASSOCIATED_USER_ID, associatedUserId);
                        ps.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);

                        // filter params.
                        for (Map.Entry<String, String> entry : filterAttributeValue.entrySet()) {
                            ps.setString(entry.getKey(), entry.getValue());
                        }

                        // scope params.
                        for (int i = 0; i < orgIdsScope.size(); i++) {
                            ps.setString(ORG_ID_SCOPE_PLACEHOLDER_PREFIX + i, orgIdsScope.get(i));
                        }
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    private String buildGetUserAssociationsSql(String filterQuery, String orgIdPlaceholders,
                                               String sortOrder, int limit)
            throws OrganizationManagementServerException {

        String head = GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_HEAD + WHITE_SPACE + AND + WHITE_SPACE;

        if (!filterQuery.isEmpty()) {
            head += filterQuery;
        }

        String tail;
        if (limit == 0) {
            tail = String.format(GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL, sortOrder);
        } else {
            tail = getUserAssociationsByFilteringTailWithLimit(sortOrder, limit);
        }

        return (head + tail).replace(PLACEHOLDER_ORG_IDS, orgIdPlaceholders);
    }

    private String getUserAssociationsByFilteringTailWithLimit(String sortOrder, int limit)
            throws OrganizationManagementServerException {

        if (isOracleDB()) {
            return String.format(GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT_ORACLE,
                    sortOrder, limit);
        } else if (isMSSqlDB()) {
            return String.format(GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT_MSSQL,
                    sortOrder, limit);
        }
        return String.format(GET_USER_ASSOCIATIONS_FOR_ASSOCIATED_USER_BY_FILTERING_TAIL_WITH_LIMIT,
                sortOrder, limit);
    }

    @Override
    public List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId,
                                                                     SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_USER_BY_SHARED_TYPE,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                        namedPreparedStatement.setString(3, sharedType.name());
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public boolean hasUserAssociations(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        Map<String, String> dbQueryMap = getDBQueryMapOfHasUserAssociations();
        String query = getDBSpecificQuery(dbQueryMap);
        try {
            Boolean result = namedJdbcTemplate.fetchSingleRecord(
                    query,
                    (resultSet, rowNumber) -> resultSet.getBoolean(HAS_USER_ASSOCIATIONS),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_USER_ID, associatedUserId);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                    }
                                                                );
            return Boolean.TRUE.equals(result);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECK_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public boolean hasUserAssociationsInOrgScope(String associatedUserId, String associatedOrgId,
                                                 List<String> orgIdsScope)
            throws OrganizationManagementServerException {

        if (orgIdsScope == null || orgIdsScope.isEmpty()) {
            return false;
        }

        String orgIdPlaceholder = "ORG_ID_";
        List<String> orgIdPlaceholders = new ArrayList<>();
        for (int i = 1; i <= orgIdsScope.size(); i++) {
            orgIdPlaceholders.add(":" + orgIdPlaceholder + i + ";");
        }

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        Map<String, String> dbQueryMap = getDBQueryMapOfHasUserAssociationsInOrgScope();
        String query = getDBSpecificQuery(dbQueryMap).replace(
                SQLConstants.SQLPlaceholders.PLACEHOLDER_ORG_IDS, String.join(", ", orgIdPlaceholders));

        try {
            Boolean result = namedJdbcTemplate.fetchSingleRecord(
                    query,
                    (resultSet, rowNumber) -> resultSet.getBoolean(HAS_USER_ASSOCIATIONS),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_USER_ID, associatedUserId);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                        int index = 1;
                        for (String orgId : orgIdsScope) {
                            namedPreparedStatement.setString(orgIdPlaceholder + index, orgId);
                            index++;
                        }
                    });
            return Boolean.TRUE.equals(result);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECK_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public UserAssociation getUserAssociationOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_USER_ASSOCIATION_FOR_ROOT_USER_IN_ORG,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedUserId);
                        namedPreparedStatement.setString(2, orgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG, e,
                    orgId);
        }
    }

    @Override
    public UserAssociation getUserAssociation(String userId, String organizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_USER_ASSOCIATIONS_FOR_SHARED_USER,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, userId);
                        namedPreparedStatement.setString(2, organizationId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_OF_SHARED_USER, e,
                    userId, organizationId);
        }
    }

    @Override
    public List<String> getNonDeletableUserRoleAssignments(String roleId, List<String> deletedDomainQualifiedUserNames,
                                                           String tenantDomain, String permittedOrgId)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(deletedDomainQualifiedUserNames)) {
            return Collections.emptyList();
        }

        Map<String, List<String>> domainToUserNamesMap = groupUsernamesByDomain(deletedDomainQualifiedUserNames);
        List<String> nonDeletableUserNames = new ArrayList<>();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        for (Map.Entry<String, List<String>> entry : domainToUserNamesMap.entrySet()) {
            String domainName = entry.getKey();
            List<String> userNamesInDomain = entry.getValue();

            String usernamePlaceholder = "USERNAME_";
            List<String> usernamePlaceholders = new ArrayList<>();
            for (int i = 1; i <= userNamesInDomain.size(); i++) {
                usernamePlaceholders.add(":" + usernamePlaceholder + i + ";");
            }
            String sqlStatement = GET_RESTRICTED_USERNAMES_BY_ROLE_AND_ORG.replace(
                    PLACEHOLDER_NAME_USER_NAMES, String.join(", ", usernamePlaceholders));

            try {
                nonDeletableUserNames.addAll(namedJdbcTemplate.executeQuery(sqlStatement,
                        (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_USER_NAME),
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(COLUMN_NAME_UM_UUID, roleId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_EDIT_OPERATION,
                                    EditOperation.DELETE.name());
                            for (int index = 1; index <= userNamesInDomain.size(); index++) {
                                namedPreparedStatement.setString(usernamePlaceholder + index,
                                        userNamesInDomain.get(index - 1));
                            }
                        }));
            } catch (DataAccessException e) {
                String errorMessage = String.format(
                        "Error while retrieving permitted usernames for role ID: %s in tenant domain: %s.", roleId,
                        tenantDomain);
                throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
            }
        }
        return nonDeletableUserNames;
    }

    @Override
    public List<String> getSharedUserRolesFromUserRoles(List<String> roleIds, String tenantDomain)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }

        String roleIdPlaceholder = "ROLE_ID_";
        List<String> roleIdPlaceholders = new ArrayList<>();
        for (int i = 1; i <= roleIds.size(); i++) {
            roleIdPlaceholders.add(":" + roleIdPlaceholder + i + ";");
        }

        String fetchSharedRolesQuery =
                GET_SHARED_USER_ROLES.replace(PLACEHOLDER_ROLE_IDS, String.join(", ", roleIdPlaceholders));

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            return namedJdbcTemplate.executeQuery(
                    fetchSharedRolesQuery,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        int index = 1;
                        for (String roleId : roleIds) {
                            namedPreparedStatement.setString(roleIdPlaceholder + index, roleId);
                            index++;
                        }
                    });
        } catch (DataAccessException e) {
            String errorMessage =
                    String.format("Error while retrieving shared user roles for tenant domain: %s.", tenantDomain);
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }

    @Override
    public void addEditRestrictionsForSharedUserRole(String roleId, String username, String tenantDomain,
                                                     String domainName, EditOperation editOperation,
                                                     String permittedOrgId)
            throws UserSharingMgtServerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            // Query to retrieve UM_ID
            Integer userRoleId = namedJdbcTemplate.fetchSingleRecord(GET_USER_ROLE_IN_TENANT,
                    (resultSet, rowNumber) -> resultSet.getInt(COLUMN_NAME_UM_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_USER_NAME, username);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_UUID, roleId);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                    });

            if (userRoleId != null) {
                // Insert the record into UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS
                namedJdbcTemplate.executeUpdate(INSERT_RESTRICTED_EDIT_PERMISSION,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_HYBRID_USER_ROLE_ID, userRoleId);
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID, tenantId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_EDIT_OPERATION, editOperation.name());
                            namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);
                        });
            } else {
                throw new UserSharingMgtServerException(ERROR_CODE_ERROR_RETRIEVING_USER_ROLE_ID);
            }
        } catch (DataAccessException e) {
            throw new UserSharingMgtServerException(ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION);
        }
    }

    @Override
    public List<String> getRolesSharedWithUserInOrganization(String username, int tenantId, String domainName)
            throws UserSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_SHARED_ROLES_OF_SHARED_USER,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_USER_NAME, username);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                    });
        } catch (DataAccessException e) {
            throw new UserSharingMgtServerException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER);
        }
    }

    @Override
    public List<UserAssociation> getUserAssociationsOfGivenUserOnGivenOrgs(String associatedUserId, List<String> orgIds)
            throws OrganizationManagementServerException {

        if (CollectionUtils.isEmpty(orgIds)) {
            return Collections.emptyList();
        }

        String orgIdPlaceholder = "ORG_ID_";
        List<String> orgIdPlaceholders = new ArrayList<>();
        for (int i = 1; i <= orgIds.size(); i++) {
            orgIdPlaceholders.add(":" + orgIdPlaceholder + i + ";");
        }

        String fetchUserAssociationsQuery =
                GET_USER_ASSOCIATIONS_OF_USER_IN_GIVEN_ORGS.replace(PLACEHOLDER_ORG_IDS,
                        String.join(", ", orgIdPlaceholders));

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    fetchUserAssociationsQuery,
                    (resultSet, rowNumber) -> {
                        UserAssociation userAssociation = new UserAssociation();
                        userAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        userAssociation.setUserId(resultSet.getString(COLUMN_NAME_USER_ID));
                        userAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        userAssociation.setAssociatedUserId(resultSet.getString(COLUMN_NAME_ASSOCIATED_USER_ID));
                        userAssociation.setUserResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        userAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return userAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_USER_ID, associatedUserId);
                        int index = 1;
                        for (String orgId : orgIds) {
                            namedPreparedStatement.setString(orgIdPlaceholder + index, orgId);
                            index++;
                        }
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public void updateSharedTypeOfUserAssociation(int id, SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(
                    UPDATE_USER_ASSOCIATION_SHARED_TYPE,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_ID, id);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_SHARED_TYPE, sharedType.name());
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATE_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    private Map<String, List<String>> groupUsernamesByDomain(List<String> deletedDomainQualifiedUserNames) {

        Map<String, List<String>> domainToUserNamesMap = new HashMap<>();
        for (String deletedDomainQualifiedUserName : deletedDomainQualifiedUserNames) {
            String domainName = UserCoreUtil.extractDomainFromName(deletedDomainQualifiedUserName);
            String username = UserCoreUtil.removeDomainFromName(deletedDomainQualifiedUserName);
            domainToUserNamesMap.computeIfAbsent(domainName, k -> new ArrayList<>()).add(username);
        }
        return domainToUserNamesMap;
    }

    private Map<String, String> getDBQueryMapOfHasUserAssociations() {

        Map<String, String> dbQueryMap = new HashMap<>();
        dbQueryMap.put(DB_TYPE_DB2, CHECK_USER_ORG_ASSOCIATION_EXISTS_DB2);
        dbQueryMap.put(DB_TYPE_MSSQL, CHECK_USER_ORG_ASSOCIATION_EXISTS_MSSQL);
        dbQueryMap.put(DB_TYPE_MYSQL, CHECK_USER_ORG_ASSOCIATION_EXISTS);
        dbQueryMap.put(DB_TYPE_ORACLE, CHECK_USER_ORG_ASSOCIATION_EXISTS_ORACLE);
        dbQueryMap.put(DB_TYPE_POSTGRESQL, CHECK_USER_ORG_ASSOCIATION_EXISTS);
        dbQueryMap.put(DB_TYPE_DEFAULT, CHECK_USER_ORG_ASSOCIATION_EXISTS);
        return dbQueryMap;
    }

    private Map<String, String> getDBQueryMapOfHasUserAssociationsInOrgScope() {

        Map<String, String> dbQueryMap = new HashMap<>();
        dbQueryMap.put(DB_TYPE_DB2, CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_DB2);
        dbQueryMap.put(DB_TYPE_MSSQL, CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_MSSQL);
        dbQueryMap.put(DB_TYPE_MYSQL, CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE);
        dbQueryMap.put(DB_TYPE_ORACLE, CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_ORACLE);
        dbQueryMap.put(DB_TYPE_POSTGRESQL, CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE);
        dbQueryMap.put(DB_TYPE_DEFAULT, CHECK_USER_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE);
        return dbQueryMap;
    }

    private String getDBSpecificQuery(Map<String, String> dbQueryMap) throws OrganizationManagementServerException {

        if (isDB2DB()) {
            return dbQueryMap.get(DB_TYPE_DB2);
        } else if (isMSSqlDB()) {
            return dbQueryMap.get(DB_TYPE_MSSQL);
        } else if (isMySqlDB()) {
            return dbQueryMap.get(DB_TYPE_MYSQL);
        } else if (isOracleDB()) {
            return dbQueryMap.get(DB_TYPE_ORACLE);
        } else if (isPostgreSqlDB()) {
            return dbQueryMap.get(DB_TYPE_POSTGRESQL);
        }
        return dbQueryMap.get(DB_TYPE_DEFAULT);
    }
}
