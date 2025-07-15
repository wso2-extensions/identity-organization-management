/*
 * Copyright (c) 2024-2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao;

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicyWithAttributes;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_DELETION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARING_POLICY_AND_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARING_POLICY_CREATION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_BY_RESOURCE_TYPE_AND_ID_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RETRIEVING_RESOURCE_SHARING_POLICY_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_BY_RESOURCE_ID_AND_TYPE_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_SHARED_RESOURCE_ATTRIBUTE_DELETION_BY_ATTRIBUTE_TYPE_AND_ID_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.CREATE_RESOURCE_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.CREATE_SHARED_RESOURCE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_RESOURCE_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_RESOURCE_SHARING_POLICY_BY_ORG_ID_AT_ATTRIBUTE_DELETION;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_RESOURCE_SHARING_POLICY_BY_RESOURCE_TYPE_AND_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_RESOURCE_SHARING_POLICY_BY_RESOURCE_TYPE_AND_ID_AT_RESOURCE_DELETION;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_RESOURCE_SHARING_POLICY_IN_ORG_BY_RESOURCE_TYPE_AND_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_SHARED_RESOURCE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_SHARED_RESOURCE_ATTRIBUTE_BY_ATTRIBUTE_TYPE_AND_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_SHARED_RESOURCE_ATTRIBUTE_BY_ATTRIBUTE_TYPE_AND_ID_AT_ATTRIBUTE_DELETION;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_RESOURCE_SHARING_POLICIES_BY_ORG_IDS_HEAD;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_RESOURCE_SHARING_POLICIES_WITH_INITIATING_ORG_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_RESOURCE_SHARING_POLICIES_WITH_SHARED_ATTRIBUTES_BY_POLICY_HOLDING_ORGS_HEAD;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_RESOURCE_SHARING_POLICY_BY_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_SHARED_RESOURCE_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE_AND_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.RESOURCE_TYPE_FILTER;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.JOIN_COLUMN_UM_ID_OF_UM_SHARED_RESOURCE_ATTRIBUTES_TABLE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.util.ResourceSharingUtils.handleServerException;

/**
 * DAO implementation for handling resource sharing policies.
 */
public class ResourceSharingPolicyHandlerDAOImpl implements ResourceSharingPolicyHandlerDAO {

    @Override
    public int addResourceSharingPolicy(ResourceSharingPolicy resourceSharingPolicy)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeInsert(CREATE_RESOURCE_SHARING_POLICY, namedPreparedStatement -> {
                setResourceSharingPolicyParameters(namedPreparedStatement, resourceSharingPolicy);
            }, null, true, DB_SCHEMA_COLUMN_NAME_UM_ID);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_CREATION_FAILED, e,
                    resourceSharingPolicy.getResourceType(), resourceSharingPolicy.getResourceId());
        }
    }

    @Override
    public Optional<ResourceSharingPolicy> getResourceSharingPolicyById(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            return Optional.ofNullable(namedJdbcTemplate.fetchSingleRecord(
                    GET_RESOURCE_SHARING_POLICY_BY_ID,
                    (resultSet, rowNum) -> retrieveResourceSharingPolicyRecordFromDB(resultSet),
                    namedPreparedStatement ->
                            namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_ID, resourceSharingPolicyId)));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RETRIEVING_RESOURCE_SHARING_POLICY_FAILED);
        }
    }

    @Override
    public List<ResourceSharingPolicy> getResourceSharingPolicies(List<String> policyHoldingOrganizationIds)
            throws ResourceSharingPolicyMgtServerException {

        return getResourceSharingPoliciesByResourceType(policyHoldingOrganizationIds, null);
    }

    @Override
    public List<ResourceSharingPolicy> getResourceSharingPoliciesByResourceType(
            List<String> policyHoldingOrganizationIds, String resourceType)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        // Dynamically build placeholders for the query.
        String placeholders = policyHoldingOrganizationIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        String query = GET_RESOURCE_SHARING_POLICIES_BY_ORG_IDS_HEAD + "(" + placeholders + ")";

        if (resourceType != null) {
            query = query + RESOURCE_TYPE_FILTER;
        }
        try {
            return namedJdbcTemplate.executeQuery(query,
                    (resultSet, rowNumber) -> retrieveResourceSharingPolicyRecordFromDB(resultSet),
                    preparedStatement -> {
                        int index = 1;
                        for (String orgId : policyHoldingOrganizationIds) {
                            preparedStatement.setString(index++, orgId);
                        }
                        if (resourceType != null) {
                            preparedStatement.setString(index, resourceType);
                        }
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RETRIEVING_RESOURCE_SHARING_POLICY_FAILED);
        }
    }

    @Override
    public Map<ResourceType, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByResourceType(
            List<String> policyHoldingOrganizationIds) throws ResourceSharingPolicyMgtServerException {

        List<ResourceSharingPolicy> resourceSharingPolicies = getResourceSharingPolicies(policyHoldingOrganizationIds);
        return resourceSharingPolicies.stream().collect(Collectors.groupingBy(ResourceSharingPolicy::getResourceType));
    }

    @Override
    public Map<String, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
            List<String> policyHoldingOrganizationIds) throws ResourceSharingPolicyMgtServerException {

        List<ResourceSharingPolicy> resourceSharingPolicies = getResourceSharingPolicies(policyHoldingOrganizationIds);
        return resourceSharingPolicies.stream()
                .collect(Collectors.groupingBy(ResourceSharingPolicy::getPolicyHoldingOrgId));
    }

    @Override
    public void deleteResourceSharingPolicyRecordById(int resourceSharingPolicyId,
                                                         String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_RESOURCE_SHARING_POLICY,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_UM_ID, resourceSharingPolicyId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                                sharingPolicyInitiatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED);
        }
    }

    @Override
    public void deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId,
                                                                  String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_RESOURCE_SHARING_POLICY_BY_RESOURCE_TYPE_AND_ID,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE,
                                resourceType.name());
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID,
                                resourceId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                                sharingPolicyInitiatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_BY_RESOURCE_TYPE_AND_ID_FAILED);
        }
    }

    @Override
    public void deleteResourceSharingPolicyInOrgByResourceTypeAndId(String policyHoldingOrgId,
                                                                    ResourceType resourceType, String resourceId,
                                                                    String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_RESOURCE_SHARING_POLICY_IN_ORG_BY_RESOURCE_TYPE_AND_ID,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID,
                                policyHoldingOrgId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE,
                                resourceType.name());
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID,
                                resourceId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                                sharingPolicyInitiatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_BY_RESOURCE_TYPE_AND_ID_FAILED);
        }
    }

    @Override
    public boolean addSharedResourceAttributes(List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeBatchInsert(CREATE_SHARED_RESOURCE_ATTRIBUTE, (namedPreparedStatement -> {
                    for (SharedResourceAttribute sharedResourceAttribute : sharedResourceAttributes) {
                        setSharedResourceAttributeParameters(namedPreparedStatement, sharedResourceAttribute);
                        namedPreparedStatement.addBatch();
                    }
                }), null);
                return true;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED);
        }
        return true;
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesBySharingPolicyId(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();

        try {
            namedJdbcTemplate.executeQuery(GET_SHARED_RESOURCE_ATTRIBUTES, (resultSet, rowNumber) -> {
                sharedResourceAttributes.add(retrieveSharedResourceAttributeRecordFromDB(resultSet));
                return null;
            }, namedPreparedStatement -> namedPreparedStatement.setInt(
                    DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                    resourceSharingPolicyId));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_FAILED);
        }
        return sharedResourceAttributes;
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesByType(SharedAttributeType attributeType)
            throws ResourceSharingPolicyMgtServerException {

        return getSharedResourceAttributes(GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE, attributeType, null);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesById(String attributeId)
            throws ResourceSharingPolicyMgtServerException {

        return getSharedResourceAttributes(GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_ID, null, attributeId);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesByTypeAndId(SharedAttributeType attributeType,
                                                                                String attributeId)
            throws ResourceSharingPolicyMgtServerException {

        return getSharedResourceAttributes(GET_SHARED_RESOURCE_ATTRIBUTES_BY_ATTRIBUTE_TYPE_AND_ID, attributeType,
                attributeId);
    }

    @Override
    public void deleteSharedResourceAttributesByResourceSharingPolicyId(int resourceSharingPolicyId,
                                                                           SharedAttributeType sharedAttributeType,
                                                                           String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_SHARED_RESOURCE_ATTRIBUTE, namedPreparedStatement -> {
                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                        resourceSharingPolicyId);
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE,
                        sharedAttributeType.name());
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                        sharingPolicyInitiatedOrgId);
            });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_DELETION_FAILED);
        }
    }

    @Override
    public void deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType,
                                                                     String attributeId,
                                                                     String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_SHARED_RESOURCE_ATTRIBUTE_BY_ATTRIBUTE_TYPE_AND_ID,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE,
                                attributeType.name());
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID,
                                attributeId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                                sharingPolicyInitiatedOrgId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_SHARED_RESOURCE_ATTRIBUTE_DELETION_BY_ATTRIBUTE_TYPE_AND_ID_FAILED);
        }
    }

    @Override
    public boolean addResourceSharingPolicyWithAttributes(ResourceSharingPolicy resourceSharingPolicy,
                                                          List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException {

        int resourceSharingPolicyId = addResourceSharingPolicy(resourceSharingPolicy);

        sharedResourceAttributes.forEach(attribute -> attribute.setResourceSharingPolicyId(resourceSharingPolicyId));

        try {
            addSharedResourceAttributes(sharedResourceAttributes);
        } catch (ResourceSharingPolicyMgtServerException e) {
            deleteResourceSharingPolicyRecordById(resourceSharingPolicyId, resourceSharingPolicy.getInitiatingOrgId());
            throw handleServerException(
                    ERROR_CODE_RESOURCE_SHARING_POLICY_AND_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED);
        }

        return true;
    }

    @Override
    public Map<String, Map<ResourceSharingPolicy, List<SharedResourceAttribute>>>
    getResourceSharingPoliciesWithSharedAttributes(List<String> policyHoldingOrganizationIds)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        String placeholders = policyHoldingOrganizationIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String query = String.format(GET_RESOURCE_SHARING_POLICIES_WITH_SHARED_ATTRIBUTES_BY_POLICY_HOLDING_ORGS_HEAD,
                placeholders);

        try {
            List<ResourceSharingPolicyWithAttributes> result = namedJdbcTemplate.executeQuery(query,
                    (resultSet, rowNumber) -> {
                        ResourceSharingPolicy policy = retrieveResourceSharingPolicyRecordFromDB(resultSet);
                        SharedResourceAttribute attribute = null;
                        if (resultSet.getString(JOIN_COLUMN_UM_ID_OF_UM_SHARED_RESOURCE_ATTRIBUTES_TABLE) != null) {
                            attribute = retrieveSharedResourceAttributeRecordFromDB(resultSet);
                        }
                        return new ResourceSharingPolicyWithAttributes(
                                resultSet.getString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID), policy, attribute);
                    },
                    namedPreparedStatement -> {
                        for (int i = 0; i < policyHoldingOrganizationIds.size(); i++) {
                            namedPreparedStatement.setString(i + 1, policyHoldingOrganizationIds.get(i));
                        }
                    });

           return result.stream()
                            .collect(Collectors.groupingBy(
                                    ResourceSharingPolicyWithAttributes::getPolicyHoldingOrgId,
                                    Collectors.groupingBy(
                                            ResourceSharingPolicyWithAttributes::getPolicy,
                                            Collectors.mapping(ResourceSharingPolicyWithAttributes::getAttribute,
                                                    Collectors.toList())
                                                         )
                                                          ));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RETRIEVING_RESOURCE_SHARING_POLICY_FAILED);
        }
    }

    @Override
    public void deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_RESOURCE_SHARING_POLICY_BY_RESOURCE_TYPE_AND_ID_AT_RESOURCE_DELETION,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE,
                                resourceType.name());
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID,
                                resourceId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_BY_RESOURCE_TYPE_AND_ID_FAILED);
        }
    }

    @Override
    public void deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType, String attributeId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(
                    DELETE_SHARED_RESOURCE_ATTRIBUTE_BY_ATTRIBUTE_TYPE_AND_ID_AT_ATTRIBUTE_DELETION,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE,
                                attributeType.name());
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID,
                                attributeId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_SHARED_RESOURCE_ATTRIBUTE_DELETION_BY_ATTRIBUTE_TYPE_AND_ID_FAILED);
        }
    }

    @Override
    public void deleteResourceSharingPoliciesAndAttributesByOrganizationId(String organizationId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(
                    DELETE_RESOURCE_SHARING_POLICY_BY_ORG_ID_AT_ATTRIBUTE_DELETION,
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID, organizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID, organizationId);
                    });
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED);
        }
    }

    @Override
    public Map<ResourceSharingPolicy, List<SharedResourceAttribute>>
    getResourceSharingPolicyByInitiatingOrgId(String initiatingOrganizationId, String resourceType, String resourceId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();

        try {
            List<ResourceSharingPolicyWithAttributes> result = namedJdbcTemplate.executeQuery(
                    GET_RESOURCE_SHARING_POLICIES_WITH_INITIATING_ORG_ID,
                    (resultSet, rowNumber) -> {
                        ResourceSharingPolicy policy = retrieveResourceSharingPolicyRecordFromDB(resultSet);
                        SharedResourceAttribute attribute = null;
                        if (resultSet.getString(JOIN_COLUMN_UM_ID_OF_UM_SHARED_RESOURCE_ATTRIBUTES_TABLE) != null) {
                            attribute = retrieveSharedResourceAttributeRecordFromDB(resultSet);
                        }
                        return new ResourceSharingPolicyWithAttributes(
                                resultSet.getString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID),
                                policy,
                                attribute
                        );
                    },
                    namedPreparedStatement -> {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                                initiatingOrganizationId);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE, resourceType);
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID, resourceId);
                    });
            return result.stream().collect(Collectors.groupingBy(
                    ResourceSharingPolicyWithAttributes::getPolicy,
                    Collectors.mapping(ResourceSharingPolicyWithAttributes::getAttribute, Collectors.toList())));
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RETRIEVING_RESOURCE_SHARING_POLICY_FAILED);
        }
    }

    private List<SharedResourceAttribute> getSharedResourceAttributes(String query, SharedAttributeType attributeType,
                                                                      String attributeId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();

        try {
            namedJdbcTemplate.executeQuery(query, (resultSet, rowNumber) -> {
                sharedResourceAttributes.add(retrieveSharedResourceAttributeRecordFromDB(resultSet));
                return null;
            }, namedPreparedStatement -> {
                if (attributeType != null) {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE, attributeType.name());
                }
                if (attributeId != null) {
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID, attributeId);
                }
            });
        } catch (DataAccessException e) {
            throw handleServerException(
                    ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_BY_RESOURCE_ID_AND_TYPE_FAILED);
        }
        return sharedResourceAttributes;
    }

    private ResourceSharingPolicy retrieveResourceSharingPolicyRecordFromDB(ResultSet resultSet) throws SQLException {

        ResourceSharingPolicy policy = new ResourceSharingPolicy();
        policy.setResourceSharingPolicyId(
                resultSet.getInt(DB_SCHEMA_COLUMN_NAME_UM_ID));
        policy.setResourceType(ResourceType.valueOf(
                resultSet.getString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE)));
        policy.setResourceId(
                resultSet.getString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID));
        policy.setInitiatingOrgId(
                resultSet.getString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID));
        policy.setPolicyHoldingOrgId(
                resultSet.getString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID));
        policy.setSharingPolicy(
                PolicyEnum.getPolicyByPolicyCode(resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARING_POLICY)));
        return policy;
    }

    private SharedResourceAttribute retrieveSharedResourceAttributeRecordFromDB(ResultSet resultSet)
            throws SQLException {

        SharedResourceAttribute attributes = new SharedResourceAttribute();
        attributes.setResourceSharingPolicyId(
                resultSet.getInt(DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID));
        attributes.setSharedAttributeType(SharedAttributeType.valueOf(
                resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE)));
        attributes.setSharedAttributeId(
                resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID));
        attributes.setSharedResourceAttributeId(
                resultSet.getInt(DB_SCHEMA_COLUMN_NAME_UM_ID));
        return attributes;
    }

    private void setResourceSharingPolicyParameters(NamedPreparedStatement namedPreparedStatement,
                                                    ResourceSharingPolicy policy) throws SQLException {

        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE, policy.getResourceType().name());
        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID, policy.getResourceId());
        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID, policy.getInitiatingOrgId());
        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID, policy.getPolicyHoldingOrgId());
        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARING_POLICY,
                policy.getSharingPolicy().getPolicyCode());
    }

    private void setSharedResourceAttributeParameters(NamedPreparedStatement namedPreparedStatement,
                                                      SharedResourceAttribute attribute) throws SQLException {

        namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                attribute.getResourceSharingPolicyId());
        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE,
                attribute.getSharedAttributeType().name());
        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID, attribute.getSharedAttributeId());
    }
}
