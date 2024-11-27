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

import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;

import java.util.List;
import java.util.Map;

/**
 * Service that manages the core service for managing resource sharing policies.
 */
public interface ResourceSharingPolicyHandlerService {

    /**
     * Saves the resource sharing policy to the data store and return the record id.
     */
    int addResourceSharingPolicy(ResourceSharingPolicy resourceSharingPolicy)
            throws OrganizationManagementServerException, ResourceSharingPolicyMgtServerException, DataAccessException;

    /**
     * Retrieves resource sharing policies for the given organization IDs.
     */
    List<ResourceSharingPolicy> getResourceSharingPolicies(List<String> organizationIds)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves resource sharing policies for the given organization IDs grouped by resource type.
     */
    Map<ResourceType, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByResourceType(
            List<String> organizationIds)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves resource sharing policies for the given organization IDs grouped by resource policy holding org IDs.
     */
    Map<String, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
            List<String> organizationIds)
    throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes a resource sharing policy record by ID.
     */
    boolean deleteResourceSharingPolicyRecordById(Integer resourceSharingPolicyId, String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes a resource sharing policy record by resource Type and ID.
     */
    boolean deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId,
                                                           String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Saves the shared resource attributes to the data store.
     */
    boolean addSharedResourceAttributes(List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves shared resource attributes for a given resource sharing policy ID.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesBySharingPolicyId(Integer resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException, DataAccessException;

    /**
     * Retrieves shared resource attributes for a given attribute Type.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesByType(SharedAttributeType attributeType)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves shared resource attributes for a given attribute ID.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesById(String attributeId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves shared resource attributes for a given attribute Type and ID.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesByTypeAndId(SharedAttributeType attributeType,
                                                                         String attributeId)
    throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes shared resource attributes for a given resource sharing policy ID.
     */
    boolean deleteSharedResourceAttributesByResourceSharingPolicyId(Integer resourceSharingPolicyId,
                                                                    SharedAttributeType sharedAttributeType,
                                                                    String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes shared resource attributes by attribute Type and ID.
     */
    boolean deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType, String attributeId,
                                                              String permittedOrgId)
            throws ResourceSharingPolicyMgtServerException;

}
