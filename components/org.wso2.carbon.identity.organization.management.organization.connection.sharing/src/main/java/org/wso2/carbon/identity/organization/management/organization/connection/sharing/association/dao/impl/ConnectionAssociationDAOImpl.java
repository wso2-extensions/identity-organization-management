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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.dao.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.dao.ConnectionAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.util.ConnectionSharingUtil;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_INTERNAL_ERROR;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.COLUMN_NAME_ASSOCIATED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.COLUMN_NAME_ASSOCIATED_RESOURCE_UUID;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.COLUMN_NAME_ID;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.COLUMN_NAME_RESOURCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.COLUMN_NAME_SHARED_ORG_ID;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.COLUMN_NAME_SHARED_RESOURCE_UUID;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.DELETE_CONNECTION_ASSOCIATIONS_BY_ORG;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.DELETE_CONNECTION_ASSOCIATION_IN_ORG;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_CONNECTION_ASSOCIATIONS_BY_FILTERING_HEAD;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_CONNECTION_ASSOCIATIONS_BY_PARENT;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_CONNECTION_ASSOCIATIONS_BY_RESIDENT_ORG;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_CONNECTION_ASSOCIATIONS_LIMIT;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_CONNECTION_ASSOCIATIONS_ORDER_BY;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_CONNECTION_ASSOCIATION_BY_SHARED_RESOURCE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.GET_SHARED_CONNECTION_ID;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.INSERT_CONNECTION_ASSOCIATION;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.SHARED_ORG_ID_LIST_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingSQLConstants.SHARED_ORG_ID_PLACEHOLDER_PREFIX;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_AFTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_BEFORE;

/**
 * Default {@link ConnectionAssociationDAO} implementation backed by the identity datasource.
 */
public class ConnectionAssociationDAOImpl implements ConnectionAssociationDAO {

    private static final String CURSOR_AFTER_PARAM = "CURSOR_AFTER";
    private static final String CURSOR_BEFORE_PARAM = "CURSOR_BEFORE";
    private static final String FILTER_SHARED_ORG_ID_PARAM = "FILTER_SHARED_ORG_ID";

    @Override
    public void addConnectionAssociation(ConnectionAssociation association)
            throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(INSERT_CONNECTION_ASSOCIATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(COLUMN_NAME_RESOURCE_TYPE,
                            association.getResourceType().name());
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_RESOURCE_UUID,
                            association.getParentConnectionId());
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID,
                            association.getConnectionResidentOrganizationId());
                    namedPreparedStatement.setString(COLUMN_NAME_SHARED_RESOURCE_UUID,
                            association.getSharedConnectionId());
                    namedPreparedStatement.setString(COLUMN_NAME_SHARED_ORG_ID, association.getOrganizationId());
                }, association, false);
                return null;
            });
        } catch (TransactionException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public Optional<String> getSharedConnectionId(String resourceType, String connectionId, String associatedOrgId,
                                                  String sharedOrgId) throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            String sharedConnectionId = namedJdbcTemplate.fetchSingleRecord(GET_SHARED_CONNECTION_ID,
                    (resultSet, rowNumber) -> resultSet.getString(COLUMN_NAME_SHARED_RESOURCE_UUID),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_RESOURCE_TYPE, resourceType);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_RESOURCE_UUID, connectionId);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                        namedPreparedStatement.setString(COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);
                    });
            return Optional.ofNullable(sharedConnectionId);
        } catch (DataAccessException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<ConnectionAssociation> getConnectionAssociationsByResidentOrg(String residentOrgId)
            throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_CONNECTION_ASSOCIATIONS_BY_RESIDENT_ORG,
                    (resultSet, rowNumber) -> mapConnectionAssociation(resultSet),
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, residentOrgId));
        } catch (DataAccessException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public Optional<ConnectionAssociation> getConnectionAssociationBySharedConnectionId(String resourceType,
                                                                                        String sharedConnectionId)
            throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            ConnectionAssociation association = namedJdbcTemplate.fetchSingleRecord(
                    GET_CONNECTION_ASSOCIATION_BY_SHARED_RESOURCE,
                    (resultSet, rowNumber) -> mapConnectionAssociation(resultSet),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_RESOURCE_TYPE, resourceType);
                        namedPreparedStatement.setString(COLUMN_NAME_SHARED_RESOURCE_UUID, sharedConnectionId);
                    });
            return Optional.ofNullable(association);
        } catch (DataAccessException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<ConnectionAssociation> getConnectionAssociations(String resourceType, String connectionId,
                                                                 String associatedOrgId)
            throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeQuery(GET_CONNECTION_ASSOCIATIONS_BY_PARENT,
                    (resultSet, rowNumber) -> mapConnectionAssociation(resultSet),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(COLUMN_NAME_RESOURCE_TYPE, resourceType);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_RESOURCE_UUID, connectionId);
                        namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public List<ConnectionAssociation> getConnectionAssociations(String resourceType, String connectionId,
                                                                 String associatedOrgId, List<String> sharedOrgIds,
                                                                 List<ExpressionNode> expressionNodes,
                                                                 String sortOrder, int limit)
            throws ConnectionSharingMgtServerException {

        if (CollectionUtils.isEmpty(sharedOrgIds)) {
            return Collections.emptyList();
        }

        Map<String, String> filterParams = new LinkedHashMap<>();
        String filterConditions = buildFilterConditions(expressionNodes, filterParams);
        String placeholders = IntStream.range(0, sharedOrgIds.size())
                .mapToObj(i -> ":" + SHARED_ORG_ID_PLACEHOLDER_PREFIX + i + ";")
                .collect(Collectors.joining(", "));

        StringBuilder sqlBuilder = new StringBuilder(
                GET_CONNECTION_ASSOCIATIONS_BY_FILTERING_HEAD.replace(SHARED_ORG_ID_LIST_PLACEHOLDER, placeholders));
        sqlBuilder.append(filterConditions);
        sqlBuilder.append(String.format(GET_CONNECTION_ASSOCIATIONS_ORDER_BY, sortOrder));
        if (limit > 0) {
            sqlBuilder.append(String.format(GET_CONNECTION_ASSOCIATIONS_LIMIT, limit));
        }

        List<ConnectionAssociation> associations = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false);
             NamedPreparedStatement namedPreparedStatement =
                     new NamedPreparedStatement(connection, sqlBuilder.toString())) {

            namedPreparedStatement.setString(COLUMN_NAME_RESOURCE_TYPE, resourceType);
            namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_RESOURCE_UUID, connectionId);
            namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
            for (int i = 0; i < sharedOrgIds.size(); i++) {
                namedPreparedStatement.setString(SHARED_ORG_ID_PLACEHOLDER_PREFIX + i, sharedOrgIds.get(i));
            }
            for (Map.Entry<String, String> entry : filterParams.entrySet()) {
                namedPreparedStatement.setString(entry.getKey(), entry.getValue());
            }

            try (ResultSet resultSet = namedPreparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    associations.add(mapConnectionAssociation(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
        return associations;
    }

    @Override
    public void deleteConnectionAssociation(String resourceType, String connectionId, String associatedOrgId,
                                            String sharedOrgId) throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_CONNECTION_ASSOCIATION_IN_ORG, namedPreparedStatement -> {
                    namedPreparedStatement.setString(COLUMN_NAME_RESOURCE_TYPE, resourceType);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_RESOURCE_UUID, connectionId);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, associatedOrgId);
                    namedPreparedStatement.setString(COLUMN_NAME_SHARED_ORG_ID, sharedOrgId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    @Override
    public void deleteConnectionAssociationsByOrganizationId(String organizationId)
            throws ConnectionSharingMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_CONNECTION_ASSOCIATIONS_BY_ORG, namedPreparedStatement -> {
                    namedPreparedStatement.setString(COLUMN_NAME_SHARED_ORG_ID, organizationId);
                    namedPreparedStatement.setString(COLUMN_NAME_ASSOCIATED_ORG_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_INTERNAL_ERROR, e);
        }
    }

    private String buildFilterConditions(List<ExpressionNode> expressionNodes, Map<String, String> filterParams) {

        if (CollectionUtils.isEmpty(expressionNodes)) {
            return StringUtils.EMPTY;
        }
        StringBuilder conditions = new StringBuilder();
        for (ExpressionNode expressionNode : expressionNodes) {
            String attribute = expressionNode.getAttributeValue();
            String value = expressionNode.getValue();
            if (StringUtils.isBlank(attribute) || StringUtils.isBlank(value)) {
                continue;
            }
            if (PAGINATION_AFTER.equalsIgnoreCase(attribute)) {
                conditions.append(" AND ").append(COLUMN_NAME_ID).append(" < :").append(CURSOR_AFTER_PARAM)
                        .append(";");
                filterParams.put(CURSOR_AFTER_PARAM, value);
            } else if (PAGINATION_BEFORE.equalsIgnoreCase(attribute)) {
                conditions.append(" AND ").append(COLUMN_NAME_ID).append(" > :").append(CURSOR_BEFORE_PARAM)
                        .append(";");
                filterParams.put(CURSOR_BEFORE_PARAM, value);
            } else if (ORGANIZATION_ID_FIELD.equalsIgnoreCase(attribute)) {
                conditions.append(" AND ").append(COLUMN_NAME_SHARED_ORG_ID).append(" = :")
                        .append(FILTER_SHARED_ORG_ID_PARAM).append(";");
                filterParams.put(FILTER_SHARED_ORG_ID_PARAM, value);
            }
        }
        return conditions.toString();
    }

    private ConnectionAssociation mapConnectionAssociation(ResultSet resultSet) throws SQLException {

        return new ConnectionAssociation.Builder()
                .id(resultSet.getInt(COLUMN_NAME_ID))
                .resourceType(ResourceType.valueOf(resultSet.getString(COLUMN_NAME_RESOURCE_TYPE)))
                .parentConnectionId(resultSet.getString(COLUMN_NAME_ASSOCIATED_RESOURCE_UUID))
                .connectionResidentOrganizationId(resultSet.getString(COLUMN_NAME_ASSOCIATED_ORG_ID))
                .sharedConnectionId(resultSet.getString(COLUMN_NAME_SHARED_RESOURCE_UUID))
                .organizationId(resultSet.getString(COLUMN_NAME_SHARED_ORG_ID))
                .build();
    }

    private NamedJdbcTemplate getNewTemplate() {

        return ConnectionSharingUtil.getNewTemplate();
    }
}
