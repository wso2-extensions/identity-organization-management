/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.dao;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.FilterQueryBuilder;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementServerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_DB2;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_DB2;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CHECK_AGENT_ORG_ASSOCIATION_EXISTS_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CREATE_ORGANIZATION_AGENT_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.CREATE_ORGANIZATION_AGENT_ASSOCIATION_WITH_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DELETE_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_ROOT_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DELETE_ORGANIZATION_AGENT_ASSOCIATION_FOR_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_HEAD;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT_DB2;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ASSOCIATIONS_OF_AGENT_IN_GIVEN_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_AGENT_ROLE_IN_TENANT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_AGENT_BY_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_ORGANIZATION_AGENT_ASSOCIATION_FOR_ROOT_AGENT_IN_ORG;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_RESTRICTED_AGENT_NAMES_BY_ROLE_AND_ORG;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_SHARED_AGENT_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.GET_SHARED_ROLES_OF_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.INSERT_RESTRICTED_EDIT_PERMISSION_FOR_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.UPDATE_AGENT_ASSOCIATION_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.AND;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.ASC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_AGENT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ASSOCIATED_AGENT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ASSOCIATED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_AGENT_NAME;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_DOMAIN_NAME;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_EDIT_OPERATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_PERMITTED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_SHARED_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.COLUMN_NAME_UM_UUID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.DESC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.HAS_AGENT_ASSOCIATIONS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.ORG_ID_SCOPE_PLACEHOLDER_PREFIX;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_NAME_AGENT_NAMES;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_ORG_IDS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.PLACEHOLDER_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.SQLPlaceholders.WHITE_SPACE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DBTypes.DB_TYPE_DB2;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DBTypes.DB_TYPE_DEFAULT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DBTypes.DB_TYPE_MSSQL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DBTypes.DB_TYPE_MYSQL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DBTypes.DB_TYPE_ORACLE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SQLConstants.DBTypes.DB_TYPE_POSTGRESQL;
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
 * DAO implementation for managing organization agent associations.
 */
public class OrganizationAgentSharingDAOImpl implements OrganizationAgentSharingDAO {

    @Override
    public void createOrganizationAgentAssociation(String agentId, String orgId, String associatedAgentId,
                                                   String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(CREATE_ORGANIZATION_AGENT_ASSOCIATION_WITH_TYPE, namedPreparedStatement -> {
                    namedPreparedStatement.setString(COLUMN_NAME_AGENT_ID, agentId);
                    namedPreparedStatement.setString(COLUMN_NAME_ORG_ID, orgId);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_AGENT_ID, associatedAgentId);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                    namedPreparedStatement.setString(COLUMN_NAME_UM_SHARED_TYPE, sharedType.name());
                }, null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_ORGANIZATION_USER_ASSOCIATION, e, associatedAgentId);
        }
    }

    @Override
    public boolean deleteAgentAssociationOfAgentByAssociatedOrg(String agentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_AGENT_ASSOCIATION_FOR_SHARED_AGENT,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, agentId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATION_FOR_SHARED_USER, e,
                    agentId);
        }
    }

    @Override
    public boolean deleteAgentAssociationsOfAssociatedAgent(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_ROOT_AGENT,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedAgentId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETE_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_AGENT,
                    (resultSet, rowNumber) -> {
                        AgentAssociation agentAssociation = new AgentAssociation();
                        agentAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        agentAssociation.setAgentId(resultSet.getString(COLUMN_NAME_AGENT_ID));
                        agentAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        agentAssociation.setAssociatedAgentId(resultSet.getString(COLUMN_NAME_ASSOCIATED_AGENT_ID));
                        agentAssociation.setAgentResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        agentAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return agentAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedAgentId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId,
                                                                         List<String> orgIdsScope,
                                                                         List<ExpressionNode> expressionNodes,
                                                                         String sortOrder, int limit)
            throws OrganizationManagementException {

        if (CollectionUtils.isEmpty(orgIdsScope)) {
            return Collections.emptyList();
        }

        // Hard-guard sort order because it gets injected into SQL.
        String resolvedSortOrder = ASC_SORT_ORDER.equalsIgnoreCase(sortOrder) ? ASC_SORT_ORDER : DESC_SORT_ORDER;

        FilterQueryBuilder filterQueryBuilder =
                org.wso2.carbon.identity.organization.management.organization.agent.sharing.util
                        .FilterQueriesUtil.getSharedAgentOrgsFilterQueryBuilder(expressionNodes);
        String filterQuery = filterQueryBuilder.getFilterQuery();
        Map<String, String> filterAttributeValue = filterQueryBuilder.getFilterAttributeValue();

        String orgIdPlaceholders = IntStream.range(0, orgIdsScope.size())
                .mapToObj(i -> ":" + ORG_ID_SCOPE_PLACEHOLDER_PREFIX + i + ";")
                .collect(Collectors.joining(", "));

        String sql = buildGetAgentAssociationsSql(filterQuery, orgIdPlaceholders, resolvedSortOrder, limit);

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(sql,
                    (resultSet, rowNumber) -> {
                        AgentAssociation agentAssociation = new AgentAssociation();
                        agentAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        agentAssociation.setAgentId(resultSet.getString(COLUMN_NAME_AGENT_ID));
                        agentAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        agentAssociation.setAssociatedAgentId(resultSet.getString(COLUMN_NAME_ASSOCIATED_AGENT_ID));
                        agentAssociation.setAgentResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        agentAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return agentAssociation;
                    },
                    ps -> {
                        // Base params.
                        ps.setString(COLUMN_NAME_ASSOCIATED_AGENT_ID, associatedAgentId);
                        ps.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);

                        // Filter params.
                        for (Map.Entry<String, String> entry : filterAttributeValue.entrySet()) {
                            ps.setString(entry.getKey(), entry.getValue());
                        }

                        // Scope params.
                        for (int i = 0; i < orgIdsScope.size(); i++) {
                            ps.setString(ORG_ID_SCOPE_PLACEHOLDER_PREFIX + i, orgIdsScope.get(i));
                        }
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfAssociatedAgent(String associatedAgentId,
                                                                         String associatedOrgId,
                                                                         SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_AGENT_BY_SHARED_TYPE,
                    (resultSet, rowNumber) -> {
                        AgentAssociation agentAssociation = new AgentAssociation();
                        agentAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        agentAssociation.setAgentId(resultSet.getString(COLUMN_NAME_AGENT_ID));
                        agentAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        agentAssociation.setAssociatedAgentId(resultSet.getString(COLUMN_NAME_ASSOCIATED_AGENT_ID));
                        agentAssociation.setAgentResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        agentAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return agentAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedAgentId);
                        namedPreparedStatement.setString(2, associatedOrgId);
                        namedPreparedStatement.setString(3, sharedType.name());
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public boolean hasAgentAssociations(String associatedAgentId, String associatedOrgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        Map<String, String> dbQueryMap = getDBQueryMapOfHasAgentAssociations();
        String query = getDBSpecificQuery(dbQueryMap);
        try {
            Boolean result = namedJdbcTemplate.fetchSingleRecord(
                    query,
                    (resultSet, rowNumber) -> resultSet.getBoolean(HAS_AGENT_ASSOCIATIONS),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_AGENT_ID, associatedAgentId);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                    });
            return Boolean.TRUE.equals(result);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECK_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public boolean hasAgentAssociationsInOrganizations(String associatedAgentId, String associatedOrgId,
                                                       List<String> orgIds)
            throws OrganizationManagementServerException {

        if (CollectionUtils.isEmpty(orgIds)) {
            return false;
        }

        List<String> orgIdPlaceholders = new ArrayList<>();
        for (int i = 1; i <= orgIds.size(); i++) {
            orgIdPlaceholders.add(":" + PLACEHOLDER_ORG_ID + i + ";");
        }

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        Map<String, String> dbQueryMap = getDBQueryMapOfHasAgentAssociationsInOrgScope();
        String query = getDBSpecificQuery(dbQueryMap).replace(
                PLACEHOLDER_ORG_IDS, String.join(", ", orgIdPlaceholders));

        try {
            Boolean result = namedJdbcTemplate.fetchSingleRecord(
                    query,
                    (resultSet, rowNumber) -> resultSet.getBoolean(HAS_AGENT_ASSOCIATIONS),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_AGENT_ID, associatedAgentId);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                        int index = 1;
                        for (String orgId : orgIds) {
                            namedPreparedStatement.setString(PLACEHOLDER_ORG_ID + index, orgId);
                            index++;
                        }
                    });
            return Boolean.TRUE.equals(result);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECK_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    @Override
    public AgentAssociation getAgentAssociationOfAssociatedAgentByOrgId(String associatedAgentId, String orgId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_AGENT_ASSOCIATION_FOR_ROOT_AGENT_IN_ORG,
                    (resultSet, rowNumber) -> {
                        AgentAssociation agentAssociation = new AgentAssociation();
                        agentAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        agentAssociation.setAgentId(resultSet.getString(COLUMN_NAME_AGENT_ID));
                        agentAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        agentAssociation.setAssociatedAgentId(resultSet.getString(COLUMN_NAME_ASSOCIATED_AGENT_ID));
                        agentAssociation.setAgentResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        agentAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return agentAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, associatedAgentId);
                        namedPreparedStatement.setString(2, orgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_FOR_USER_AT_SHARED_ORG, e,
                    orgId);
        }
    }

    @Override
    public AgentAssociation getAgentAssociation(String agentId, String organizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_AGENT_ASSOCIATIONS_FOR_SHARED_AGENT,
                    (resultSet, rowNumber) -> {
                        AgentAssociation agentAssociation = new AgentAssociation();
                        agentAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        agentAssociation.setAgentId(resultSet.getString(COLUMN_NAME_AGENT_ID));
                        agentAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        agentAssociation.setAssociatedAgentId(resultSet.getString(COLUMN_NAME_ASSOCIATED_AGENT_ID));
                        agentAssociation.setAgentResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        agentAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return agentAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(1, agentId);
                        namedPreparedStatement.setString(2, organizationId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GET_ORGANIZATION_USER_ASSOCIATION_OF_SHARED_USER, e,
                    agentId, organizationId);
        }
    }

    @Override
    public List<String> getNonDeletableAgentRoleAssignments(String roleId,
                                                            List<String> deletedDomainQualifiedAgentNames,
                                                            String tenantDomain, String permittedOrgId)
            throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(deletedDomainQualifiedAgentNames)) {
            return Collections.emptyList();
        }

        Map<String, List<String>> domainToAgentNamesMap = groupAgentNamesByDomain(deletedDomainQualifiedAgentNames);
        List<String> nonDeletableAgentNames = new ArrayList<>();
        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        for (Map.Entry<String, List<String>> entry : domainToAgentNamesMap.entrySet()) {
            String domainName = entry.getKey();
            List<String> agentNamesInDomain = entry.getValue();

            String agentNamePlaceholder = "AGENT_NAME_";
            List<String> agentNamePlaceholders = new ArrayList<>();
            for (int i = 1; i <= agentNamesInDomain.size(); i++) {
                agentNamePlaceholders.add(":" + agentNamePlaceholder + i + ";");
            }
            String sqlStatement = GET_RESTRICTED_AGENT_NAMES_BY_ROLE_AND_ORG.replace(
                    PLACEHOLDER_NAME_AGENT_NAMES, String.join(", ", agentNamePlaceholders));

            try {
                nonDeletableAgentNames.addAll(namedJdbcTemplate.executeQuery(sqlStatement,
                        (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_AGENT_NAME),
                        namedPreparedStatement -> {
                            namedPreparedStatement.setString(COLUMN_NAME_UM_UUID, roleId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_EDIT_OPERATION,
                                    EditOperation.DELETE.name());
                            for (int index = 1; index <= agentNamesInDomain.size(); index++) {
                                namedPreparedStatement.setString(agentNamePlaceholder + index,
                                        agentNamesInDomain.get(index - 1));
                            }
                        }));
            } catch (DataAccessException e) {
                String errorMessage = String.format(
                        "Error while retrieving permitted agent names for role ID: %s in tenant domain: %s.", roleId,
                        tenantDomain);
                throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
            }
        }
        return nonDeletableAgentNames;
    }

    @Override
    public List<String> getSharedAgentRolesFromAgentRoles(List<String> roleIds, String tenantDomain)
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
                GET_SHARED_AGENT_ROLES.replace(PLACEHOLDER_ROLE_IDS, String.join(", ", roleIdPlaceholders));

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
                    String.format("Error while retrieving shared agent roles for tenant domain: %s.", tenantDomain);
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }

    @Override
    public void addEditRestrictionsForSharedAgentRole(String roleId, String agentName, String tenantDomain,
                                                      String domainName, EditOperation editOperation,
                                                      String permittedOrgId)
            throws AgentSharingMgtServerException {

        int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            // Query to retrieve UM_ID of the agent role assignment.
            Integer agentRoleId = namedJdbcTemplate.fetchSingleRecord(GET_AGENT_ROLE_IN_TENANT,
                    (resultSet, rowNumber) -> resultSet.getInt(COLUMN_NAME_UM_ID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_AGENT_NAME, agentName);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_UUID, roleId);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                    });

            if (agentRoleId != null) {
                // Insert the record into UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS.
                namedJdbcTemplate.executeUpdate(INSERT_RESTRICTED_EDIT_PERMISSION_FOR_AGENT,
                        namedPreparedStatement -> {
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_HYBRID_USER_ROLE_ID, agentRoleId);
                            namedPreparedStatement.setInt(COLUMN_NAME_UM_HYBRID_USER_ROLE_TENANT_ID, tenantId);
                            namedPreparedStatement.setString(COLUMN_NAME_UM_EDIT_OPERATION, editOperation.name());
                            namedPreparedStatement.setString(COLUMN_NAME_UM_PERMITTED_ORG_ID, permittedOrgId);
                        });
            } else {
                throw new AgentSharingMgtServerException(ERROR_CODE_ERROR_RETRIEVING_AGENT_ROLE_ID);
            }
        } catch (DataAccessException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_ERROR_INSERTING_RESTRICTED_PERMISSION);
        }
    }

    @Override
    public List<String> getRolesSharedWithAgentInOrganization(String agentName, int tenantId, String domainName)
            throws AgentSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_SHARED_ROLES_OF_SHARED_AGENT,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_UM_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_UM_AGENT_NAME, agentName);
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_DOMAIN_NAME, domainName);
                    });
        } catch (DataAccessException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT);
        }
    }

    @Override
    public List<AgentAssociation> getAgentAssociationsOfGivenAgentOnGivenOrgs(String associatedAgentId,
                                                                               List<String> orgIds)
            throws OrganizationManagementServerException {

        if (CollectionUtils.isEmpty(orgIds)) {
            return Collections.emptyList();
        }

        String orgIdPlaceholder = "ORG_ID_";
        List<String> orgIdPlaceholders = new ArrayList<>();
        for (int i = 1; i <= orgIds.size(); i++) {
            orgIdPlaceholders.add(":" + orgIdPlaceholder + i + ";");
        }

        String fetchAgentAssociationsQuery =
                GET_AGENT_ASSOCIATIONS_OF_AGENT_IN_GIVEN_ORGS.replace(PLACEHOLDER_ORG_IDS,
                        String.join(", ", orgIdPlaceholders));

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(
                    fetchAgentAssociationsQuery,
                    (resultSet, rowNumber) -> {
                        AgentAssociation agentAssociation = new AgentAssociation();
                        agentAssociation.setId(resultSet.getInt(COLUMN_NAME_UM_ID));
                        agentAssociation.setAgentId(resultSet.getString(COLUMN_NAME_AGENT_ID));
                        agentAssociation.setOrganizationId(resultSet.getString(COLUMN_NAME_ORG_ID));
                        agentAssociation.setAssociatedAgentId(resultSet.getString(COLUMN_NAME_ASSOCIATED_AGENT_ID));
                        agentAssociation.setAgentResidentOrganizationId(
                                resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID));
                        agentAssociation.setSharedType(
                                SharedType.fromString(resultSet.getString(COLUMN_NAME_UM_SHARED_TYPE)));
                        return agentAssociation;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_AGENT_ID, associatedAgentId);
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
    public void updateSharedTypeOfAgentAssociation(int id, SharedType sharedType)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(
                    UPDATE_AGENT_ASSOCIATION_SHARED_TYPE,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(COLUMN_NAME_UM_ID, id);
                        namedPreparedStatement.setString(COLUMN_NAME_UM_SHARED_TYPE, sharedType.name());
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATE_ORGANIZATION_USER_ASSOCIATIONS, e);
        }
    }

    private String buildGetAgentAssociationsSql(String filterQuery, String orgIdPlaceholders,
                                                String sortOrder, int limit)
            throws OrganizationManagementServerException {

        String head = GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_HEAD + WHITE_SPACE + AND + WHITE_SPACE;

        if (!filterQuery.isEmpty()) {
            head += filterQuery;
        }

        String tail;
        if (limit <= 0) {
            tail = String.format(GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL, sortOrder);
        } else {
            tail = getAgentAssociationsByFilteringTailWithLimit(sortOrder, limit);
        }

        return (head + tail).replace(PLACEHOLDER_ORG_IDS, orgIdPlaceholders);
    }

    private String getAgentAssociationsByFilteringTailWithLimit(String sortOrder, int limit)
            throws OrganizationManagementServerException {

        if (isOracleDB()) {
            return String.format(
                    GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT_ORACLE,
                    sortOrder, limit);
        } else if (isMSSqlDB()) {
            return String.format(
                    GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT_MSSQL,
                    sortOrder, limit);
        } else if (isDB2DB()) {
            return String.format(
                    GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT_DB2,
                    sortOrder, limit);
        }
        return String.format(GET_AGENT_ASSOCIATIONS_FOR_ASSOCIATED_AGENT_BY_FILTERING_TAIL_WITH_LIMIT,
                sortOrder, limit);
    }

    private Map<String, String> getDBQueryMapOfHasAgentAssociations() {

        Map<String, String> dbQueryMap = new HashMap<>();
        dbQueryMap.put(DB_TYPE_DB2, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_DB2);
        dbQueryMap.put(DB_TYPE_MSSQL, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_MSSQL);
        dbQueryMap.put(DB_TYPE_MYSQL, CHECK_AGENT_ORG_ASSOCIATION_EXISTS);
        dbQueryMap.put(DB_TYPE_ORACLE, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_ORACLE);
        dbQueryMap.put(DB_TYPE_POSTGRESQL, CHECK_AGENT_ORG_ASSOCIATION_EXISTS);
        dbQueryMap.put(DB_TYPE_DEFAULT, CHECK_AGENT_ORG_ASSOCIATION_EXISTS);
        return dbQueryMap;
    }

    private Map<String, String> getDBQueryMapOfHasAgentAssociationsInOrgScope() {

        Map<String, String> dbQueryMap = new HashMap<>();
        dbQueryMap.put(DB_TYPE_DB2, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_DB2);
        dbQueryMap.put(DB_TYPE_MSSQL, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_MSSQL);
        dbQueryMap.put(DB_TYPE_MYSQL, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE);
        dbQueryMap.put(DB_TYPE_ORACLE, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE_ORACLE);
        dbQueryMap.put(DB_TYPE_POSTGRESQL, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE);
        dbQueryMap.put(DB_TYPE_DEFAULT, CHECK_AGENT_ORG_ASSOCIATION_EXISTS_IN_ORG_SCOPE);
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

    private Map<String, List<String>> groupAgentNamesByDomain(List<String> deletedDomainQualifiedAgentNames) {

        Map<String, List<String>> domainToAgentNamesMap = new HashMap<>();
        for (String deletedDomainQualifiedAgentName : deletedDomainQualifiedAgentNames) {
            String domainName =
                    org.wso2.carbon.user.core.util.UserCoreUtil.extractDomainFromName(deletedDomainQualifiedAgentName);
            String agentName =
                    org.wso2.carbon.user.core.util.UserCoreUtil.removeDomainFromName(deletedDomainQualifiedAgentName);
            domainToAgentNamesMap.computeIfAbsent(domainName, k -> new ArrayList<>()).add(agentName);
        }
        return domainToAgentNamesMap;
    }
}
