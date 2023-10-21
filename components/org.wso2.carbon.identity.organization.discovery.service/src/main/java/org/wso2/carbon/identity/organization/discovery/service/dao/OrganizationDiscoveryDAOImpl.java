/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service.dao;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.discovery.service.model.OrganizationDiscovery;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.CHECK_DISCOVERY_ATTRIBUTE_ADDED_IN_ORGANIZATION;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.CHECK_DISCOVERY_ATTRIBUTE_EXIST_IN_HIERARCHY;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.DELETE_ORGANIZATION_DISCOVERY_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.DISCOVERY_ATTRIBUTE_VALUE_LIST_PLACEHOLDER;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.EXCLUDE_CURRENT_ORGANIZATION_FROM_CHECK_DISCOVERY_ATTRIBUTE_EXIST;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.GET_ORGANIZATIONS_DISCOVERY_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.GET_ORGANIZATION_DISCOVERY_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.GET_ORGANIZATION_ID_BY_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.INSERT_ORGANIZATION_DISCOVERY_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ID;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_ROOT_ID;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_TYPE;
import static org.wso2.carbon.identity.organization.discovery.service.constant.SQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_VALUE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_ORGANIZATION_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_DISCOVERY_ATTRIBUTE_ADDED_IN_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CHECKING_ORGANIZATION_DISCOVERY_ATTRIBUTE_EXIST_IN_HIERARCHY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_ORGANIZATION_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_ORGANIZATION_ID_BY_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_LISTING_ORGANIZATIONS_DISCOVERY_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_ORGANIZATION_DISCOVERY_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * DAO implementation for organization discovery.
 */
public class OrganizationDiscoveryDAOImpl implements OrganizationDiscoveryDAO {

    @Override
    public void addOrganizationDiscoveryAttributes(String organizationId,
                                                   List<OrgDiscoveryAttribute> discoveryAttributes)
            throws OrganizationManagementServerException {

        String rootOrganizationId = getOrganizationId();
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeBatchInsert(INSERT_ORGANIZATION_DISCOVERY_ATTRIBUTES, (namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROOT_ID, rootOrganizationId);
                    for (OrgDiscoveryAttribute attribute : discoveryAttributes) {
                        for (String value : attribute.getValues()) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_TYPE, attribute.getType());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, value);
                            namedPreparedStatement.addBatch();
                        }
                    }
                }), null);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_ORGANIZATION_DISCOVERY_ATTRIBUTE, e, organizationId);
        }
    }

    @Override
    public boolean isDiscoveryAttributeExistInHierarchy(boolean excludeCurrentOrganization, String rootOrganizationId,
                                                        String currentOrganizationId, String type, List<String> values)
            throws OrganizationManagementServerException {

        String valuePlaceholder = "VALUE_";
        List<String> valuePlaceholders = new ArrayList<>();

        for (int i = 1; i <= values.size(); i++) {
            valuePlaceholders.add(":" + valuePlaceholder + i + ";");
        }
        String placeholder = String.join(", ", valuePlaceholders);
        String sqlStmt = CHECK_DISCOVERY_ATTRIBUTE_EXIST_IN_HIERARCHY.replace
                (DISCOVERY_ATTRIBUTE_VALUE_LIST_PLACEHOLDER, placeholder);
        if (excludeCurrentOrganization) {
            sqlStmt = sqlStmt + EXCLUDE_CURRENT_ORGANIZATION_FROM_CHECK_DISCOVERY_ATTRIBUTE_EXIST;
        }

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            int attrCount = namedJdbcTemplate.fetchSingleRecord(sqlStmt,
                    (resultSet, rowNumber) -> resultSet.getInt(1),
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROOT_ID, rootOrganizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_TYPE, type);
                        if (excludeCurrentOrganization) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, currentOrganizationId);
                        }
                        int index = 1;
                        for (String value : values) {
                            namedPreparedStatement.setString(valuePlaceholder + index, value);
                            index++;
                        }
                    });
            return attrCount > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_ORGANIZATION_DISCOVERY_ATTRIBUTE_EXIST_IN_HIERARCHY,
                    e);
        }
    }

    @Override
    public boolean isDiscoveryAttributeAddedToOrganization(String organizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            List<Integer> discoveryAttributes = namedJdbcTemplate.executeQuery
                    (CHECK_DISCOVERY_ATTRIBUTE_ADDED_IN_ORGANIZATION,
                            (resultSet, rowNumber) -> resultSet.getInt(1), namedPreparedStatement ->
                                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId));
            return discoveryAttributes.get(0) > 0;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_CHECKING_DISCOVERY_ATTRIBUTE_ADDED_IN_ORGANIZATION, e,
                    organizationId);
        }
    }

    @Override
    public List<OrgDiscoveryAttribute> getOrganizationDiscoveryAttributes(String organizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        Map<String, List<String>> rowDataCollector = new HashMap<>();
        try {
            namedJdbcTemplate.executeQuery(GET_ORGANIZATION_DISCOVERY_ATTRIBUTES,
                    (resultSet, rowNumber) -> {
                        String key = resultSet.getString(1);
                        String value = resultSet.getString(2);
                        if (rowDataCollector.containsKey(key)) {
                            rowDataCollector.get(key).add(value);
                        } else {
                            List<String> values = new ArrayList<>();
                            values.add(value);
                            rowDataCollector.put(key, values);
                        }
                        return null;
                    },
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_DISCOVERY_ATTRIBUTE, e,
                    organizationId);
        }

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        rowDataCollector.forEach((key, value) -> {
            OrgDiscoveryAttribute organization = new OrgDiscoveryAttribute();
            organization.setType(key);
            organization.setValues(value);
            discoveryAttributes.add(organization);
        });
        return discoveryAttributes;
    }

    @Override
    public void deleteOrganizationDiscoveryAttributes(String organizationId) throws
            OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_ORGANIZATION_DISCOVERY_ATTRIBUTES, namedPreparedStatement ->
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_ORGANIZATION_DISCOVERY_ATTRIBUTE, e, organizationId);
        }
    }

    @Override
    public void updateOrganizationDiscoveryAttributes(String organizationId,
                                                      List<OrgDiscoveryAttribute> discoveryAttributes)
            throws OrganizationManagementServerException {

        String rootOrganizationId = getOrganizationId();
        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeUpdate(DELETE_ORGANIZATION_DISCOVERY_ATTRIBUTES, namedPreparedStatement ->
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId));
                template.executeBatchInsert(INSERT_ORGANIZATION_DISCOVERY_ATTRIBUTES, (namedPreparedStatement -> {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ID, organizationId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROOT_ID, rootOrganizationId);
                    for (OrgDiscoveryAttribute attribute : discoveryAttributes) {
                        for (String value : attribute.getValues()) {
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_TYPE, attribute.getType());
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, value);
                            namedPreparedStatement.addBatch();
                        }
                    }
                }), null);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_ORGANIZATION_DISCOVERY_ATTRIBUTE, e, organizationId);
        }
    }

    @Override
    public List<OrganizationDiscovery> getOrganizationsDiscoveryAttributes(String rootOrganizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        List<OrganizationDiscoveryRowDataCollector> rowDataCollectors;
        try {
            rowDataCollectors = namedJdbcTemplate.executeQuery(GET_ORGANIZATIONS_DISCOVERY_ATTRIBUTES,
                    (resultSet, rowNumber) -> {
                        OrganizationDiscoveryRowDataCollector collector = new OrganizationDiscoveryRowDataCollector();
                        collector.setId(resultSet.getString(1));
                        collector.setAttributeType(resultSet.getString(2));
                        collector.setAttributeValue(resultSet.getString(3));
                        collector.setOrganizationName(resultSet.getString(4));
                        return collector;
                    },
                    namedPreparedStatement ->
                            namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROOT_ID, rootOrganizationId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_LISTING_ORGANIZATIONS_DISCOVERY_ATTRIBUTES, e,
                    rootOrganizationId);
        }
        return buildOrganizationsDiscoveryFromRawData(rowDataCollectors);
    }

    @Override
    public String getOrganizationIdByDiscoveryAttribute(String attributeType, String attributeValue,
                                                        String rootOrganizationId)
            throws OrganizationManagementServerException {

        NamedJdbcTemplate namedJdbcTemplate = Utils.getNewTemplate();
        try {
            return namedJdbcTemplate.fetchSingleRecord(GET_ORGANIZATION_ID_BY_DISCOVERY_ATTRIBUTE,
                    (resultSet, rowNumber) -> resultSet.getString(1), namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_TYPE, attributeType);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_VALUE, attributeValue);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_ROOT_ID, rootOrganizationId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_ERROR_GETTING_ORGANIZATION_ID_BY_DISCOVERY_ATTRIBUTE, e,
                    attributeType, attributeValue, rootOrganizationId);
        }
    }

    private List<OrganizationDiscovery> buildOrganizationsDiscoveryFromRawData(
            List<OrganizationDiscoveryRowDataCollector> organizationRowDataCollectors) {

        List<OrganizationDiscovery> discoveryList = new ArrayList<>();

        for (OrganizationDiscoveryRowDataCollector collector : organizationRowDataCollectors) {
            String organizationId = collector.getId();
            String organizationName = collector.getOrganizationName();
            String attributeType = collector.getAttributeType();
            String attributeValue = collector.getAttributeValue();

            OrganizationDiscovery existingDiscovery = null;
            for (OrganizationDiscovery discovery : discoveryList) {
                if (StringUtils.equals(discovery.getOrganizationId(), organizationId)) {
                    existingDiscovery = discovery;
                    break;
                }
            }

            if (existingDiscovery == null) {
                OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
                orgDiscoveryAttribute.setType(attributeType);
                orgDiscoveryAttribute.setValues(Collections.singletonList(attributeValue));
                List<OrgDiscoveryAttribute> orgDiscoveryAttributeList = new ArrayList<>();
                orgDiscoveryAttributeList.add(orgDiscoveryAttribute);

                OrganizationDiscovery organizationDiscovery = new OrganizationDiscovery();
                organizationDiscovery.setOrganizationId(organizationId);
                organizationDiscovery.setOrganizationName(organizationName);
                organizationDiscovery.setDiscoveryAttributes(orgDiscoveryAttributeList);

                discoveryList.add(organizationDiscovery);
            } else {
                List<OrgDiscoveryAttribute> discoveryAttributes = existingDiscovery.getDiscoveryAttributes();
                boolean attributeExists = false;
                String newAttributeValue = null;
                List<String> existingAttributeValues = null;
                for (OrgDiscoveryAttribute attribute : discoveryAttributes) {
                    if (StringUtils.equals(attribute.getType(), attributeType)) {
                        existingAttributeValues = attribute.getValues();
                        newAttributeValue = attributeValue;
                        attributeExists = true;
                        break;
                    }
                }

                if (attributeExists) {
                    for (OrgDiscoveryAttribute attribute : discoveryAttributes) {
                        if (StringUtils.equals(attribute.getType(), attributeType)) {
                            if (existingAttributeValues == null) {
                                attribute.setValues(Collections.singletonList(newAttributeValue));
                                break;
                            }
                            List<String> attributeValues = new ArrayList<>(existingAttributeValues);
                            attributeValues.add(newAttributeValue);
                            attribute.setValues(attributeValues);
                            break;
                        }
                    }
                } else {
                    OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
                    orgDiscoveryAttribute.setType(attributeType);
                    orgDiscoveryAttribute.setValues(Collections.singletonList(attributeValue));
                    discoveryAttributes.add(orgDiscoveryAttribute);
                }
            }
        }
        return discoveryList;
    }
}
