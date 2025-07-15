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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao.ResourceSharingPolicyHandlerDAO;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao.ResourceSharingPolicyHandlerDAOImpl;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtClientException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_INAPPLICABLE_RESOURCE_TYPE_TO_POLICY;
import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_INVALID_ID;

/**
 * Implementation of the core service for managing resource sharing policies.
 */
public class ResourceSharingPolicyHandlerServiceImpl implements ResourceSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(ResourceSharingPolicyHandlerServiceImpl.class);
    private static final ResourceSharingPolicyHandlerDAO RESOURCE_SHARING_POLICY_HANDLER_DAO =
            new ResourceSharingPolicyHandlerDAOImpl();

    @Override
    public int addResourceSharingPolicy(ResourceSharingPolicy resourceSharingPolicy)
            throws ResourceSharingPolicyMgtException {

        List<ResourceType> applicableResources = resourceSharingPolicy.getSharingPolicy().getApplicableResources();
        if (!applicableResources.contains(resourceSharingPolicy.getResourceType())) {
            throw new ResourceSharingPolicyMgtClientException(ERROR_CODE_INAPPLICABLE_RESOURCE_TYPE_TO_POLICY.getCode(),
                    ERROR_CODE_INAPPLICABLE_RESOURCE_TYPE_TO_POLICY.getMessage(),
                    ERROR_CODE_INAPPLICABLE_RESOURCE_TYPE_TO_POLICY.getDescription());
        }

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.addResourceSharingPolicy(resourceSharingPolicy);
    }

    @Override
    public Optional<ResourceSharingPolicy> getResourceSharingPolicyById(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtException {

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPolicyById(resourceSharingPolicyId);
    }

    @Override
    public List<ResourceSharingPolicy> getResourceSharingPolicies(List<String> policyHoldingOrganizationIds)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(policyHoldingOrganizationIds);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPolicies(policyHoldingOrganizationIds);
    }

    @Override
    public Map<ResourceType, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByResourceType(
            List<String> policyHoldingOrganizationIds) throws ResourceSharingPolicyMgtException {

        validateIdFormat(policyHoldingOrganizationIds);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPoliciesGroupedByResourceType(
                policyHoldingOrganizationIds);
    }

    @Override
    public Map<String, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
            List<String> policyHoldingOrganizationIds) throws ResourceSharingPolicyMgtException {

        validateIdFormat(policyHoldingOrganizationIds);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
                policyHoldingOrganizationIds);
    }

    @Override
    public void deleteResourceSharingPolicyRecordById(int resourceSharingPolicyId,
                                                         String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(sharingPolicyInitiatedOrgId);

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteResourceSharingPolicyRecordById(resourceSharingPolicyId,
                sharingPolicyInitiatedOrgId);
    }

    @Override
    public void deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId,
                                                                  String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(Arrays.asList(resourceId, sharingPolicyInitiatedOrgId));

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteResourceSharingPolicyByResourceTypeAndId(resourceType,
                resourceId, sharingPolicyInitiatedOrgId);
    }

    @Override
    public void deleteResourceSharingPolicyInOrgByResourceTypeAndId(String policyHoldingOrgId,
                                                                    ResourceType resourceType, String resourceId,
                                                                    String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(Arrays.asList(policyHoldingOrgId, resourceId, sharingPolicyInitiatedOrgId));

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteResourceSharingPolicyInOrgByResourceTypeAndId(policyHoldingOrgId,
                resourceType, resourceId, sharingPolicyInitiatedOrgId);
    }

    @Override
    public boolean addSharedResourceAttributes(List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtException {

        List<SharedResourceAttribute> addableSharedResourceAttributes = new ArrayList<>();
        List<Integer> invalidPolicyIds = new ArrayList<>();

        for (SharedResourceAttribute sharedResourceAttribute : sharedResourceAttributes) {
            Optional<ResourceSharingPolicy> resourceSharingPolicy =
                    getResourceSharingPolicyById(sharedResourceAttribute.getResourceSharingPolicyId());
            if (resourceSharingPolicy.isPresent()) {
                if (isValidAttributeForTheResource(resourceSharingPolicy.get(), sharedResourceAttribute)) {
                    addableSharedResourceAttributes.add(sharedResourceAttribute);
                }
            } else {
                invalidPolicyIds.add(sharedResourceAttribute.getResourceSharingPolicyId());
            }
        }

        if (!invalidPolicyIds.isEmpty()) {
            String warnMessage = "Some attributes were skipped due to invalid ResourceSharingPolicy IDs: " +
                    invalidPolicyIds.stream().map(String::valueOf).collect(Collectors.joining(", ", "{", "}"));
            LOG.warn(warnMessage);
        }

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.addSharedResourceAttributes(addableSharedResourceAttributes);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesBySharingPolicyId(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtException {

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getSharedResourceAttributesBySharingPolicyId(
                resourceSharingPolicyId);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesByType(SharedAttributeType attributeType)
            throws ResourceSharingPolicyMgtException {

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getSharedResourceAttributesByType(attributeType);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesById(String attributeId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(attributeId);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getSharedResourceAttributesById(attributeId);
    }

    @Override
    public List<SharedResourceAttribute> getSharedResourceAttributesByTypeAndId(SharedAttributeType attributeType,
                                                                                String attributeId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(attributeId);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getSharedResourceAttributesByTypeAndId(attributeType, attributeId);
    }

    @Override
    public void deleteSharedResourceAttributesByResourceSharingPolicyId(int resourceSharingPolicyId,
                                                                           SharedAttributeType sharedAttributeType,
                                                                           String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(sharingPolicyInitiatedOrgId);

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteSharedResourceAttributesByResourceSharingPolicyId(
                resourceSharingPolicyId, sharedAttributeType, sharingPolicyInitiatedOrgId);
    }

    @Override
    public void deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType,
                                                                     String attributeId,
                                                                     String sharingPolicyInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(sharingPolicyInitiatedOrgId);

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteSharedResourceAttributeByAttributeTypeAndId(attributeType,
                attributeId, sharingPolicyInitiatedOrgId);
    }

    @Override
    public boolean addResourceSharingPolicyWithAttributes(ResourceSharingPolicy resourceSharingPolicy,
                                                          List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtException {

        List<SharedResourceAttribute> addableSharedResourceAttributes = new ArrayList<>();

        for (SharedResourceAttribute sharedResourceAttribute : sharedResourceAttributes) {
            if (isValidAttributeForTheResource(resourceSharingPolicy, sharedResourceAttribute)) {
                addableSharedResourceAttributes.add(sharedResourceAttribute);
            }
        }

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                addableSharedResourceAttributes);
    }

    @Override
    public Map<String, Map<ResourceSharingPolicy, List<SharedResourceAttribute>>>
    getResourceSharingPoliciesWithSharedAttributes(List<String> policyHoldingOrganizationIds)
            throws ResourceSharingPolicyMgtException {

        validateIdFormat(policyHoldingOrganizationIds);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPoliciesWithSharedAttributes(
                policyHoldingOrganizationIds);
    }

    @Override
    public void deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId)
            throws ResourceSharingPolicyMgtException {

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteResourceSharingPolicyByResourceTypeAndId(resourceType, resourceId);
    }

    @Override
    public void deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType, String attributeId)
            throws ResourceSharingPolicyMgtException {

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteSharedResourceAttributeByAttributeTypeAndId(attributeType,
                attributeId);
    }

    @Override
    public void deleteResourceSharingPoliciesAndAttributesByOrganizationId(String organizationId)
            throws ResourceSharingPolicyMgtException {

        RESOURCE_SHARING_POLICY_HANDLER_DAO.deleteResourceSharingPoliciesAndAttributesByOrganizationId(organizationId);
    }

    @Override
    public Map<ResourceSharingPolicy, List<SharedResourceAttribute>> getResourceSharingPolicyByInitiatingOrgId(
            String initiatingOrganizationId, String resourceType, String resourceId)
            throws ResourceSharingPolicyMgtException {

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPolicyByInitiatingOrgId(
                initiatingOrganizationId, resourceType, resourceId);
    }

    @Override
    public List<ResourceSharingPolicy> getResourceSharingPoliciesByResourceType(
            List<String> policyHoldingOrganizationIds, String resourceType) throws ResourceSharingPolicyMgtException {

        validateIdFormat(policyHoldingOrganizationIds);

        return RESOURCE_SHARING_POLICY_HANDLER_DAO.getResourceSharingPoliciesByResourceType(
                policyHoldingOrganizationIds, resourceType);
    }

    private boolean isValidAttributeForTheResource(ResourceSharingPolicy resourceSharingPolicy,
                                                   SharedResourceAttribute sharedResourceAttribute) {

        return resourceSharingPolicy.getResourceType()
                .isApplicableAttributeType(sharedResourceAttribute.getSharedAttributeType());
    }

    private void validateIdFormat(String id) throws ResourceSharingPolicyMgtClientException {

        if (isInvalidId(id)) {
            throw new ResourceSharingPolicyMgtClientException(ERROR_CODE_INVALID_ID.getCode(),
                    ERROR_CODE_INVALID_ID.getMessage(), ERROR_CODE_INVALID_ID.getDescription());
        }
    }

    private void validateIdFormat(List<String> ids) throws ResourceSharingPolicyMgtClientException {

        for (String id : ids) {
            validateIdFormat(id);
        }
    }

    private boolean isInvalidId(String id) {

        return id == null || id.isEmpty();
    }
}
