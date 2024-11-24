/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.SharedResourceAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.service.util.Utils.getNewTemplate;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_CREATION_OF_SHARED_RESOURCE_ATTRIBUTE_BUILDER_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_DELETION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARING_POLICY_CREATION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_FAILED;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.CREATE_RESOURCE_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_RESOURCE_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.DELETE_SHARED_RESOURCE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.GET_SHARED_RESOURCE_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.INSERT_SHARED_RESOURCE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingSQLConstants.SQLPlaceholders.DB_SCHEMA_COLUMN_NAME_UM_ID;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.util.ResourceSharingUtils.handleServerException;

/**
 * DAO implementation for handling user sharing policies.
 */
public class ResourceSharingPolicyHandlerDAOImpl implements ResourceSharingPolicyHandlerDAO {

    private static final Log LOG = LogFactory.getLog(ResourceSharingPolicyHandlerDAOImpl.class);

    @Override
    public int addResourceSharingPolicyRecord(ResourceSharingPolicy resourceSharingPolicy)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            return namedJdbcTemplate.executeInsert(CREATE_RESOURCE_SHARING_POLICY, namedPreparedStatement -> {
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_ID,
                        resourceSharingPolicy.getResourceId());
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_RESOURCE_TYPE,
                        resourceSharingPolicy.getResourceType().name());
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_INITIATING_ORG_ID,
                        resourceSharingPolicy.getInitiatingOrgId());
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_POLICY_HOLDING_ORG_ID,
                        resourceSharingPolicy.getPolicyHoldingOrgId());
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARING_POLICY,
                        resourceSharingPolicy.getSharingPolicy().getPolicyCode());
            }, null, true, DB_SCHEMA_COLUMN_NAME_UM_ID);
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_CREATION_FAILED, e,
                    resourceSharingPolicy.getResourceType(), resourceSharingPolicy.getResourceId());
        }
    }

    @Override
    public void addSharedResourceAttributes(SharedResourceAttributes sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        int resourceSharingPolicyId = sharedResourceAttributes.getResourceSharingPolicyId();
        SharedAttributeType sharedAttributeType = sharedResourceAttributes.getSharedAttributeType();
        List<String> sharedAttributes = sharedResourceAttributes.getSharedAttributes();

        try {
            namedJdbcTemplate.withTransaction(template -> {
                template.executeBatchInsert(INSERT_SHARED_RESOURCE_ATTRIBUTE, (namedPreparedStatement -> {
                    namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                            resourceSharingPolicyId);
                    namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE,
                            sharedAttributeType.name());
                    for (String attribute : sharedAttributes) {
                        namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID, attribute);
                        namedPreparedStatement.addBatch();
                    }
                }), null);
                return null;
            });
        } catch (TransactionException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_CREATION_FAILED);
        }
    }

    @Override
    public boolean deleteResourceSharingPolicyRecordById(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_RESOURCE_SHARING_POLICY,
                    namedPreparedStatement -> namedPreparedStatement.setInt(
                            DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                            resourceSharingPolicyId));
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARING_POLICY_DELETION_FAILED);
        }
    }

    @Override
    public boolean deleteSharedResourceAttributesByResourceSharingPolicyId(int resourceSharingPolicyId,
                                                                           SharedAttributeType sharedAttributeType)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        try {
            namedJdbcTemplate.executeUpdate(DELETE_SHARED_RESOURCE_ATTRIBUTE, namedPreparedStatement -> {
                namedPreparedStatement.setInt(DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                        resourceSharingPolicyId);
                namedPreparedStatement.setString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE,
                        sharedAttributeType.name());
            });
            return true;
        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RESOURCE_SHARED_RESOURCE_ATTRIBUTE_DELETION_FAILED);
        }
    }

    @Override
    public List<SharedResourceAttributes> getSharedResourceAttributes(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException {

        NamedJdbcTemplate namedJdbcTemplate = getNewTemplate();
        Map<String, SharedResourceAttributes> attributesMap = new HashMap<>();

        try {
            namedJdbcTemplate.executeQuery(GET_SHARED_RESOURCE_ATTRIBUTES, (resultSet, rowNumber) -> {
                String sharedAttributeType = resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_TYPE);
                String sharedAttributeId = resultSet.getString(DB_SCHEMA_COLUMN_NAME_SHARED_ATTRIBUTE_ID);

                // If an object for the current type already exists, add the attribute to its list.
                if (attributesMap.containsKey(sharedAttributeType)) {
                    attributesMap.get(sharedAttributeType).getSharedAttributes().add(sharedAttributeId);
                } else {
                    // Otherwise, create a new SharedResourceAttributes.Builder object.
                    SharedResourceAttributes.Builder attributesBuilder = SharedResourceAttributes.builder()
                            .withResourceSharingPolicyId(
                                    resultSet.getInt(DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID))
                            .withSharedAttributeType(SharedAttributeType.valueOf(sharedAttributeType));
                    List<String> sharedAttributes = new ArrayList<>();
                    sharedAttributes.add(sharedAttributeId);
                    attributesBuilder.withSharedAttributes(sharedAttributes);
                    try {
                        attributesMap.put(sharedAttributeType, attributesBuilder.build());
                    } catch (ResourceSharingPolicyMgtException e) {
                        LOG.debug(ERROR_CODE_CREATION_OF_SHARED_RESOURCE_ATTRIBUTE_BUILDER_FAILED);
                    }

                }
                return null;
            }, namedPreparedStatement -> namedPreparedStatement.setInt(
                    DB_SCHEMA_COLUMN_NAME_RESOURCE_SHARING_POLICY_ID,
                    resourceSharingPolicyId));

            // Build and collect the final SharedResourceAttributes objects.
            return new ArrayList<>(attributesMap.values());

        } catch (DataAccessException e) {
            throw handleServerException(ERROR_CODE_RETRIEVING_SHARED_RESOURCE_ATTRIBUTES_FAILED);
        }
    }

}
