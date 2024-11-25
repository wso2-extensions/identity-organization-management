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

import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.SharedResourceAttribute;

import java.util.List;
import java.util.Map;

/**
 * DAO interface for handling user sharing policies.
 */
public interface ResourceSharingPolicyHandlerDAO {

    /**
     * Creates a record of a resource sharing policy.
     *
     * @param resourceSharingPolicy The {@link ResourceSharingPolicy} object containing details about the resource,
     *                              resource type, initiated organization, policy holding organization, and the
     *                              sharing policy.
     * @return The ID of the newly created resource sharing policy record.
     * @throws OrganizationManagementServerException   If an error occurs while creating the sharing policy record.
     * @throws ResourceSharingPolicyMgtServerException If a server error specific to resource sharing policy
     *                                                 management occurs.
     */
    int addResourceSharingPolicyRecord(ResourceSharingPolicy resourceSharingPolicy)
            throws OrganizationManagementServerException, ResourceSharingPolicyMgtServerException, DataAccessException;

    /**
     * Deletes a resource sharing policy record by ID.
     *
     * @param resourceSharingPolicyId The ID of the resource sharing policy to delete.
     * @return True if the record was deleted successfully, false otherwise.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while deleting the resource sharing policy.
     */
    boolean deleteResourceSharingPolicyRecordById(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes a resource sharing policy by resource type and ID.
     *
     * @param resourceType The type of the resource ({@link ResourceType}).
     * @param resourceId   The ID of the resource whose sharing policy is to be deleted.
     * @return True if the resource sharing policy was deleted successfully, false otherwise.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while deleting the resource sharing policy.
     */
    boolean deleteResourceSharingPolicyByResourceTypeAndId(ResourceType resourceType, String resourceId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Fetches ResourceSharingPolicy records for the given organization IDs.
     *
     * @param organizationIds List of organization IDs.
     * @return List of ResourceSharingPolicy.
     */
    List<ResourceSharingPolicy> getResourceSharingPolicies(List<String> organizationIds)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Fetches ResourceSharingPolicy records for the given organization IDs, grouped by ResourceType.
     *
     * @param organizationIds List of organization IDs.
     * @return Map of ResourceType to List of ResourceSharingPolicy.
     */
    Map<ResourceType, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByResourceType(
            List<String> organizationIds)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Fetches ResourceSharingPolicy records for the given organization IDs, grouped by PolicyHoldingOrgId.
     *
     * @param organizationIds List of organization IDs.
     * @return Map of PolicyHoldingOrgId to List of ResourceSharingPolicy.
     */
    Map<String, List<ResourceSharingPolicy>> getResourceSharingPoliciesGroupedByPolicyHoldingOrgId(
            List<String> organizationIds)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Adds shared resource attributes to an existing resource sharing policy.
     *
     * @param sharedResourceAttributes List of shared resource attributes
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while adding the shared resource attributes.
     */
    void addSharedResourceAttributes(List<SharedResourceAttribute> sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes shared resource attributes for a given resource sharing policy ID.
     *
     * @param resourceSharingPolicyId The ID of the resource sharing policy to delete shared attributes for.
     * @param sharedAttributeType     The type of the shared attribute ({@link SharedAttributeType}).
     * @return True if the shared resource attributes were deleted successfully, false otherwise.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while deleting the shared resource attributes.
     */
    boolean deleteSharedResourceAttributesByResourceSharingPolicyId(int resourceSharingPolicyId,
                                                                    SharedAttributeType sharedAttributeType)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Deletes a shared resource attribute by attribute type and ID.
     *
     * @param attributeType The type of the shared attribute ({@link SharedAttributeType}).
     * @param attributeId   The ID of the shared attribute to be deleted.
     * @return True if the shared resource attribute was deleted successfully, false otherwise.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while deleting the shared resource attribute.
     */
    boolean deleteSharedResourceAttributeByAttributeTypeAndId(SharedAttributeType attributeType, String attributeId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves shared resource attributes for a given resource sharing policy ID.
     *
     * @param resourceSharingPolicyId The ID of the resource sharing policy.
     * @return A list of {@link SharedResourceAttribute} associated with the given policy ID.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while retrieving the shared resource
     *                                                 attributes.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesBySharingPolicyId(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException, DataAccessException;

    /**
     * Retrieves shared resource attributes for a specific resource ID and type.
     *
     * @param attributeType The type of the attribute ({@link SharedAttributeType}).
     * @return A list of {@link SharedResourceAttribute} objects associated with the specified resource ID and type.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while retrieving the shared resource
     *                                                 attributes.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesByType(SharedAttributeType attributeType)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves shared resource attributes for a specific resource ID and type.
     *
     * @param attributeId The ID of the attribute for which shared attributes are to be retrieved.
     * @return A list of {@link SharedResourceAttribute} objects associated with the specified resource ID and type.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while retrieving the shared resource
     *                                                 attributes.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesById(String attributeId)
            throws ResourceSharingPolicyMgtServerException;

    /**
     * Retrieves shared resource attributes for a specific resource ID and type.
     *
     * @param attributeType The type of the attribute ({@link SharedAttributeType}).
     * @param attributeId   The ID of the attribute for which shared attributes are to be retrieved.
     * @return A list of {@link SharedResourceAttribute} objects associated with the specified resource ID and type.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while retrieving the shared resource
     *                                                 attributes.
     */
    List<SharedResourceAttribute> getSharedResourceAttributesByTypeAndId(SharedAttributeType attributeType,
                                                                         String attributeId)
            throws ResourceSharingPolicyMgtServerException;

}
