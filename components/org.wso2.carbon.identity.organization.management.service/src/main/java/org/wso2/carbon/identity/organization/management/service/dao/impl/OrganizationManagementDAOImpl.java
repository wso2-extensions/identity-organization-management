/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.service.dao.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.FilterQueryBuilder;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationUserRoleMapping;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static java.time.ZoneOffset.UTC;
import static org.wso2.carbon.identity.organization.management.authz.service.util.OrganizationManagementAuthzUtil.getAllowedPermissions;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ATTRIBUTE_COLUMN_MAP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.CO;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.EQ;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.EW;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_ORGANIZATION_ROLE_MAPPING;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_ATTRIBUTE_KEY_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_ORGANIZATION_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION_ADD_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION_DELETE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_PATCHING_ORGANIZATION_UPDATE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_CHILD_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_DATA_FROM_IDENTITY_DB;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_ID_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ROLE_NAMES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.GE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.GT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.INTERNAL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.LE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.LT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PERMISSION_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SCIM_ROLE_ID_ATTR_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SW;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.VIEW_ORGANIZATION_PERMISSION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.ADD_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.ADD_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS_MAPPING;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.AND;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_CHILD_ORGANIZATIONS_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_ORGANIZATION_ATTRIBUTE_KEY_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_ORGANIZATION_EXIST_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.CHECK_ORGANIZATION_EXIST_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.DELETE_ORGANIZATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.DELETE_ORGANIZATION_ATTRIBUTES_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.DELETE_ORGANIZATION_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ALL_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_CHILD_ORGANIZATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATIONS_BY_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATIONS_BY_TENANT_ID_TAIL;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATION_BY_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ORGANIZATION_ID_BY_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ROLE_IDS_FOR_TENANT;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.GET_ROLE_NAMES;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.INSERT_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.INSERT_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.OR;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.PATCH_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.PATCH_ORGANIZATION_CONCLUDE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.PERMISSION_LIST_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SCIM_GROUP_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ATTR_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_CREATED_TIME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_FORCED;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_KEY;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_PARENT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROLE_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_USER_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_LIMIT;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.UPDATE_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.UPDATE_ORGANIZATION_ATTRIBUTE_VALUE;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.UPDATE_ORGANIZATION_LAST_MODIFIED;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ASSIGNED_AT_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ATTR_KEY_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ATTR_VALUE_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_CREATED_TIME_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_DESCRIPTION_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_FORCED_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_LAST_MODIFIED_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_PARENT_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ROLE_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_ROLE_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_SCIM_ATTR_VALUE_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.constant.SQLConstants.VIEW_USER_ID_COLUMN;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getUserId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Organization management dao implementation.
 */
public class OrganizationManagementDAOImpl implements OrganizationManagementDAO {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementDAOImpl.class);
    private static final Calendar CALENDAR = Calendar.getInstance(TimeZone.getTimeZone(UTC));

    @Override
    public void addOrganization(int tenantId, String tenantDomain, Organization organization) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(INSERT_ORGANIZATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organization.getId());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_NAME, organization.getName());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_DESCRIPTION, organization.getDescription());
                    namedPreparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_CREATED_TIME,
                            Timestamp.from(organization.getCreated()), CALENDAR);
                    namedPreparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED,
                            Timestamp.from(organization.getLastModified()), CALENDAR);
                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, organization.getParent().getId());
                }, organization, false);
                if (CollectionUtils.isNotEmpty(organization.getAttributes())) {
                    insertOrganizationAttributes(organization);
                }
                return null;
            });
            //after adding organization we can add the forced roles coming from parent.
            insertOrganizationUserRoleMappingsForForcedRoles(organization.getId(),
                    organization.getParent().getId(), tenantId);
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_ORGANIZATION, e, tenantDomain);
        }
    }

    private void insertOrganizationAttributes(Organization organization) throws TransactionException {

        String organizationId = organization.getId();
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        namedJdbcTemplate.withTransaction(template -> {
            template.executeBatchInsert(INSERT_ATTRIBUTE, (namedPreparedStatement -> {
                for (OrganizationAttribute attribute : organization.getAttributes()) {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, attribute.getKey());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, attribute.getValue());
                    namedPreparedStatement.addBatch();
                }
            }), organizationId);
            return null;
        });
    }

    @Override
    public boolean isOrganizationExistByName(int tenantId, String organizationName, String tenantDomain) throws
            OrganizationManagementServerException {

        return isOrganizationExist(tenantId, organizationName, tenantDomain, CHECK_ORGANIZATION_EXIST_BY_NAME,
                DB_SCHEMA_COLUMN_NAME_NAME, ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_NAME);
    }

    @Override
    public boolean isOrganizationExistById(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        return isOrganizationExist(tenantId, organizationId, tenantDomain, CHECK_ORGANIZATION_EXIST_BY_ID,
                DB_SCHEMA_COLUMN_NAME_ID, ERROR_CODE_ERROR_CHECKING_ORGANIZATION_EXIST_BY_ID);
    }

    private boolean isOrganizationExist(int tenantId, String organization, String tenantDomain,
                                        String checkOrganizationExistQuery, String dbSchemaColumnNameId,
                                        OrganizationManagementConstants.ErrorMessages errorMessage)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            int orgCount = namedJdbcTemplate.fetchSingleRecord(checkOrganizationExistQuery,
                    (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(dbSchemaColumnNameId, organization);
                    });
            return orgCount > 0;
        } catch (DataAccessException e) {
            throw handleServerException(errorMessage, e, organization, tenantDomain);
        }
    }

    @Override
    public String getOrganizationIdByName(int tenantId, String organizationName, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_ID_BY_NAME,
                    (resultSet, rowNumber) -> resultSet.getString(VIEW_ID_COLUMN), namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_NAME, organizationName);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_ID_BY_NAME, e, organizationName,
                    tenantDomain);
        }
    }

    @Override
    public Organization getOrganization(int tenantId, String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<OrganizationRowDataCollector> organizationRowDataCollectors;
        try {
            organizationRowDataCollectors = namedJdbcTemplate.executeQuery(GET_ORGANIZATION_BY_ID,
                    (resultSet, rowNumber) -> {
                        OrganizationRowDataCollector collector = new OrganizationRowDataCollector();
                        collector.setId(organizationId);
                        collector.setName(resultSet.getString(VIEW_NAME_COLUMN));
                        collector.setDescription(resultSet.getString(VIEW_DESCRIPTION_COLUMN));
                        collector.setParentId(resultSet.getString(VIEW_PARENT_ID_COLUMN));
                        collector.setLastModified(resultSet.getTimestamp(VIEW_LAST_MODIFIED_COLUMN, CALENDAR)
                                .toInstant());
                        collector.setCreated(resultSet.getTimestamp(VIEW_CREATED_TIME_COLUMN, CALENDAR)
                                .toInstant());
                        collector.setAttributeKey(resultSet.getString(VIEW_ATTR_KEY_COLUMN));
                        collector.setAttributeValue(resultSet.getString(VIEW_ATTR_VALUE_COLUMN));
                        return collector;
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    });
            return (organizationRowDataCollectors == null || organizationRowDataCollectors.size() == 0) ?
                    null : buildOrganizationFromRawData(organizationRowDataCollectors);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_BY_ID, e, organizationId,
                    tenantDomain);
        }
    }

    @Override
    public List<BasicOrganization> getOrganizations(int tenantId, Integer limit, String tenantDomain, String sortOrder,
                                                    List<ExpressionNode> expressionNodes)
            throws OrganizationManagementServerException {

        FilterQueryBuilder filterQueryBuilder = new FilterQueryBuilder();
        appendFilterQuery(expressionNodes, filterQueryBuilder);
        Map<String, String> filterAttributeValue = filterQueryBuilder.getFilterAttributeValue();

        List<String> roleIdList = getSCIMRoleIdList(tenantId);
        String sqlStmt = GET_ORGANIZATIONS_BY_TENANT_ID + filterQueryBuilder.getFilterQuery() +
                String.format(GET_ORGANIZATIONS_BY_TENANT_ID_TAIL,
                        buildQueryForGettingOrganizationList(roleIdList), sortOrder);

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<BasicOrganization> organizations;
        try {
            organizations = namedJdbcTemplate.withTransaction(template ->
                    template.executeQuery(sqlStmt,
                            (resultSet, rowNumber) -> {
                                BasicOrganization organization = new BasicOrganization();
                                organization.setId(resultSet.getString(1));
                                organization.setName(resultSet.getString(2));
                                organization.setCreated(resultSet.getTimestamp(3).toString());
                                return organization;
                            },
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_USER_ID, getUserId());
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                for (Map.Entry<String, String> entry : filterAttributeValue.entrySet()) {
                                    namedPreparedStatement.setString(entry.getKey(), entry.getValue());
                                }
                                namedPreparedStatement.setInt(DB_SCHEMA_LIMIT, limit);
                            }));
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS, e, tenantDomain);
        }
        return organizations;
    }

    @Override
    public void deleteOrganization(int tenantId, String organizationId, String tenantDomain, boolean force) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            // Delete organization from UM_ORG table and cascade the deletion to the other table.
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_BY_ID, namedPreparedStatement -> {
                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    @Override
    public boolean hasChildOrganizations(String organizationId, String tenantDomain) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            List<Integer> childOrganizationIds = namedJdbcTemplate.executeQuery(CHECK_CHILD_ORGANIZATIONS_EXIST,
                    (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, organizationId);
                    });
            return childOrganizationIds.get(0) > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_CHILD_ORGANIZATIONS, e, organizationId,
                    tenantDomain);
        }
    }

    @Override
    public void patchOrganization(String organizationId, String tenantDomain, Instant lastModifiedInstant,
                                  List<PatchOperation> patchOperations) throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                for (PatchOperation patchOperation : patchOperations) {
                    if (patchOperation.getPath().startsWith(PATCH_PATH_ORG_ATTRIBUTES)) {
                        patchOrganizationAttribute(organizationId, patchOperation, tenantDomain);
                    } else {
                        patchOrganizationField(organizationId, patchOperation, tenantDomain);
                    }
                }
                updateLastModifiedTime(organizationId, tenantDomain, lastModifiedInstant);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    @Override
    public void updateOrganization(String organizationId, String tenantDomain, Organization organization) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ORGANIZATION, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_NAME, organization.getName());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_DESCRIPTION, organization.getDescription());
                    namedPreparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED,
                            Timestamp.from(organization.getLastModified()), CALENDAR);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                deleteOrganizationAttributes(organizationId, tenantDomain);
                if (CollectionUtils.isNotEmpty(organization.getAttributes())) {
                    insertOrganizationAttributes(organization);
                }
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    @Override
    public boolean isAttributeExistByKey(String tenantDomain, String organizationId, String attributeKey)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            int attrCount = namedJdbcTemplate.fetchSingleRecord(CHECK_ORGANIZATION_ATTRIBUTE_KEY_EXIST,
                    (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, attributeKey);
                    });
            return attrCount > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_ORGANIZATION_ATTRIBUTE_KEY_EXIST, e,
                    attributeKey, organizationId, tenantDomain);
        }
    }

    @Override
    public List<String> getChildOrganizationIds(int tenantId, String organizationId, String tenantDomain,
                                                Organization organization)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<String> childOrganizationIds;
        try {
            childOrganizationIds = namedJdbcTemplate.executeQuery(GET_CHILD_ORGANIZATIONS,
                    (resultSet, rowNumber) -> resultSet.getString(1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, organizationId);
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATIONS, e, tenantDomain);
        }
        return childOrganizationIds;
    }

    private void deleteOrganizationAttributes(String organizationId, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_ORGANIZATION_ATTRIBUTES_BY_ID, namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_ORGANIZATION_ATTRIBUTES, e, organizationId,
                    tenantDomain);
        }
    }

    private String buildQueryOrganization(String path) {

        // Updating a primary field
        StringBuilder sb = new StringBuilder();
        sb.append(PATCH_ORGANIZATION);
        if (path.equals(PATCH_PATH_ORG_NAME)) {
            sb.append(VIEW_NAME_COLUMN);
        } else if (path.equals(PATCH_PATH_ORG_DESCRIPTION)) {
            sb.append(VIEW_DESCRIPTION_COLUMN);
        }
        sb.append(PATCH_ORGANIZATION_CONCLUDE);
        String query = sb.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Organization patch query : " + query);
        }
        return query;
    }

    private void patchOrganizationField(String organizationId, PatchOperation patchOperation, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(buildQueryOrganization(patchOperation.getPath()), namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE,
                            patchOperation.getOp().equals(PATCH_OP_REMOVE) ? null : patchOperation.getValue());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    private void patchOrganizationAttribute(String organizationId, PatchOperation patchOperation, String tenantDomain)
            throws OrganizationManagementServerException {

        String attributeKey = patchOperation.getPath().replace(PATCH_PATH_ORG_ATTRIBUTES, "").trim();
        patchOperation.setPath(attributeKey);
        if (patchOperation.getOp().equals(PATCH_OP_ADD)) {
            insertOrganizationAttribute(organizationId, patchOperation, tenantDomain);
        } else if (patchOperation.getOp().equals(PATCH_OP_REPLACE)) {
            updateOrganizationAttribute(organizationId, patchOperation, tenantDomain);
        } else {
            deleteOrganizationAttribute(organizationId, patchOperation, tenantDomain);
        }
    }

    private void insertOrganizationAttribute(String organizationId, PatchOperation patchOperation, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeInsert(INSERT_ATTRIBUTE, (namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, patchOperation.getPath());
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, patchOperation.getValue());
                }), null, false);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION_ADD_ATTRIBUTE, e, tenantDomain);
        }
    }

    private void updateOrganizationAttribute(String organizationId, PatchOperation patchOperation, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ORGANIZATION_ATTRIBUTE_VALUE, preparedStatement -> {
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, patchOperation.getValue());
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, patchOperation.getPath());
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION_UPDATE_ATTRIBUTE, e, organizationId,
                    tenantDomain);
        }
    }

    private void deleteOrganizationAttribute(String organizationId, PatchOperation patchOperation, String tenantDomain)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_ATTRIBUTE, namedPreparedStatement -> {
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_KEY, patchOperation.getPath());
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION_DELETE_ATTRIBUTE, e,
                    patchOperation.getPath(), organizationId, tenantDomain);
        }
    }

    private void updateLastModifiedTime(String organizationId, String tenantDomain, Instant lastModifiedInstant) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(UPDATE_ORGANIZATION_LAST_MODIFIED, preparedStatement -> {
                    preparedStatement.setTimeStamp(DB_SCHEMA_COLUMN_NAME_LAST_MODIFIED,
                            Timestamp.from(lastModifiedInstant), CALENDAR);
                    preparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                });
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_PATCHING_ORGANIZATION, e, organizationId, tenantDomain);
        }
    }

    private Organization buildOrganizationFromRawData(List<OrganizationRowDataCollector>
                                                              organizationRowDataCollectors) {

        Organization organization = new Organization();
        organizationRowDataCollectors.forEach(collector -> {
            if (organization.getId() == null) {
                organization.setId(collector.getId());
                organization.setName(collector.getName());
                organization.setDescription(collector.getDescription());
                organization.getParent().setId(collector.getParentId());
                organization.setCreated(collector.getCreated());
                organization.setLastModified(collector.getLastModified());
            }
            List<OrganizationAttribute> attributes = organization.getAttributes();
            List<String> attributeKeys = new ArrayList<>();
            for (OrganizationAttribute attribute : attributes) {
                attributeKeys.add(attribute.getKey());
            }
            if (collector.getAttributeKey() != null && !attributeKeys.contains(collector.getAttributeKey())) {
                organization.getAttributes().add(new OrganizationAttribute(collector.getAttributeKey(),
                        collector.getAttributeValue()));
            }
        });
        return organization;
    }

    private void appendFilterQuery(List<ExpressionNode> expressionNodes, FilterQueryBuilder filterQueryBuilder) {

        int count = 1;
        StringBuilder filter = new StringBuilder();
        if (CollectionUtils.isEmpty(expressionNodes)) {
            filterQueryBuilder.setFilterQuery(StringUtils.EMPTY);
        } else {
            for (ExpressionNode expressionNode : expressionNodes) {
                String operation = expressionNode.getOperation();
                String value = expressionNode.getValue();
                String attributeName = ATTRIBUTE_COLUMN_MAP.get(expressionNode.getAttributeValue());
                if (StringUtils.isNotBlank(attributeName) && StringUtils.isNotBlank(value) && StringUtils
                        .isNotBlank(operation)) {
                    switch (operation) {
                        case EQ: {
                            equalFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case SW: {
                            startWithFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case EW: {
                            endWithFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case CO: {
                            containsFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case GE: {
                            greaterThanOrEqualFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case LE: {
                            lessThanOrEqualFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case GT: {
                            greaterThanFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        case LT: {
                            lessThanFilterBuilder(count, value, attributeName, filter, filterQueryBuilder);
                            count++;
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            }
            if (StringUtils.isBlank(filter.toString())) {
                filterQueryBuilder.setFilterQuery(StringUtils.EMPTY);
            } else {
                filterQueryBuilder.setFilterQuery(filter.toString());
            }
        }
    }

    private void equalFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                    FilterQueryBuilder filterQueryBuilder) {

        String filterString = " = :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(value);
    }

    private void startWithFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                        FilterQueryBuilder filterQueryBuilder) {

        String filterString = " like :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(value + "%");
    }

    private void endWithFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                      FilterQueryBuilder filterQueryBuilder) {

        String filterString = " like :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue("%" + value);
    }

    private void containsFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                       FilterQueryBuilder filterQueryBuilder) {

        String filterString = " like :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue("%" + value + "%");
    }

    private void greaterThanOrEqualFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                                 FilterQueryBuilder filterQueryBuilder) {

        String filterString = " >= :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(value);
    }

    private void lessThanOrEqualFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                              FilterQueryBuilder filterQueryBuilder) {

        String filterString = " <= :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(value);
    }

    private void greaterThanFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                          FilterQueryBuilder filterQueryBuilder) {

        String filterString = " > :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(value);
    }

    private void lessThanFilterBuilder(int count, String value, String attributeName, StringBuilder filter,
                                       FilterQueryBuilder filterQueryBuilder) {

        String filterString = " < :FILTER_ID_" + count + "; AND ";
        filter.append(attributeName).append(filterString);
        filterQueryBuilder.setFilterAttributeValue(value);
    }

    private void insertOrganizationUserRoleMappingsForForcedRoles(String organizationId, String parentId,
                                                                  int tenantId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            List<OrganizationUserRoleMapping> organizationUserRoleMappingList;
            organizationUserRoleMappingList = namedJdbcTemplate.withTransaction(template ->
                    template.executeQuery(GET_ALL_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS,
                            (resultSet, resultRow) ->
                                    new OrganizationUserRoleMapping(parentId,
                                            resultSet.getString(VIEW_USER_ID_COLUMN),
                                            resultSet.getString(VIEW_ROLE_ID_COLUMN),
                                            resultSet.getString(VIEW_ASSIGNED_AT_COLUMN),
                                            resultSet.getInt(VIEW_FORCED_COLUMN) == 1),
                            namedPreparedStatement -> {
                                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_PARENT_ID, parentId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_FORCED, 1);
                            }));
            if (CollectionUtils.isNotEmpty(organizationUserRoleMappingList)) {
                int n = organizationUserRoleMappingList.size();
                namedJdbcTemplate.withTransaction(template ->
                        template.executeInsert(buildQueryForOrganizationUserRoleMapping(n),
                        namedPreparedStatement -> {
                            for (int i = 0; i < n; i++) {
                                OrganizationUserRoleMapping organizationUserRoleMapping =
                                        organizationUserRoleMappingList.get(i);
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ID + "%d", i),
                                        Utils.generateUniqueID());
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_USER_ID + "%d", i),
                                        organizationUserRoleMapping.getUserId());
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ROLE_ID + "%d", i),
                                        organizationUserRoleMapping.getRoleId());
                                namedPreparedStatement.setInt(String.format(DB_SCHEMA_COLUMN_NAME_TENANT_ID + "%d", i),
                                        tenantId);
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ORG_ID + "%d", i),
                                        organizationId);
                                namedPreparedStatement.setString(String.format(DB_SCHEMA_COLUMN_NAME_ASSIGNED_AT + "%d",
                                        i), parentId);
                                namedPreparedStatement.setInt(String.format(DB_SCHEMA_COLUMN_NAME_FORCED + "%d", i),
                                        1); //since mandatory
                            }
                        }, organizationUserRoleMappingList, false));
            }
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_ORGANIZATION_ROLE_MAPPING, e);
        }
    }

    private List<String> getSCIMRoleIdList(int tenantId) throws OrganizationManagementServerException {

        List<String> roleNamesList = getRoleNames();
        List<String> roleIdList = new ArrayList<>();
        if (CollectionUtils.isEmpty(roleNamesList)) {
            return roleIdList;
        }
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplateForIdentityDatabase();
        try {
            roleIdList = namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    buildQueryForGettingRoleIds(roleNamesList.size()),
                    (resultSet, rowNumber) -> resultSet.getString(VIEW_SCIM_ATTR_VALUE_COLUMN),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_TENANT_ID, tenantId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ATTR_NAME, SCIM_ROLE_ID_ATTR_NAME);
                        int index = 0;
                        for (String roleName : roleNamesList) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROLE_NAME + (index++),
                                    INTERNAL + roleName);
                        }
                    })
            );
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_DATA_FROM_IDENTITY_DB, e);
        }
        return roleIdList;
    }

    private List<String> getRoleNames() throws OrganizationManagementServerException {

        List<String> roleNamesList;
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<String> permissions = getAllowedPermissions(VIEW_ORGANIZATION_PERMISSION);
        List<String> permissionPlaceholders = new ArrayList<>();
        // Constructing the placeholders required to hold the permission strings in the named prepared statement.
        for (int i = 1; i <= permissions.size(); i++) {
            permissionPlaceholders.add(":" + PERMISSION_PLACEHOLDER + i + ";");
        }
        String placeholder = String.join(", ", permissionPlaceholders);

        StringBuilder sb = new StringBuilder(GET_ROLE_NAMES.replace(PERMISSION_LIST_PLACEHOLDER, placeholder));
        try {
            roleNamesList = namedJdbcTemplate.withTransaction(template -> template.executeQuery(
                    sb.toString(),
                    (resultSet, rowNumber) -> resultSet.getString(VIEW_ROLE_NAME_COLUMN),
                    namedPreparedStatement -> {
                        int index = 0;
                        for (String permission : permissions) {
                            namedPreparedStatement.setString(PERMISSION_PLACEHOLDER + (++index), permission);
                        }
                    }));
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ROLE_NAMES, e);
        }
        return roleNamesList;
    }

    private String buildQueryForGettingRoleIds(int numberOfRoles) {

        StringBuilder sb = new StringBuilder();
        sb.append(GET_ROLE_IDS_FOR_TENANT).append("(");
        for (int i = 0; i < numberOfRoles; i++) {
            sb.append(String.format(SCIM_GROUP_ROLE_NAME, i));
            if (i != numberOfRoles - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildQueryForGettingOrganizationList(List<String> roleIdList) {

        if (CollectionUtils.isEmpty(roleIdList)) {
            return "";
        }
        int numberOfRoles = roleIdList.size();
        StringBuilder sb = new StringBuilder();
        sb.append(AND).append("(");
        for (int i = 0; i < numberOfRoles; i++) {
            sb.append(VIEW_ROLE_ID_COLUMN).append("='").append(roleIdList.get(i)).append("'");
            if (i != numberOfRoles - 1) {
                sb.append(OR);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String buildQueryForOrganizationUserRoleMapping(int numberOfForcedRoles) {

        StringBuilder sb = new StringBuilder();
        sb.append(ADD_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS);
        for (int i = 0; i < numberOfForcedRoles; i++) {
            sb.append(String.format(ADD_FORCED_ORGANIZATION_USER_ROLE_MAPPINGS_MAPPING, i));
            if (i != numberOfForcedRoles - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
