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
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.SharedResourceAttributes;

import java.util.List;

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
     * Adds shared resource attributes to an existing resource sharing policy.
     *
     * @param sharedResourceAttributes Details about the shared resource attributes
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while adding the shared resource attributes.
     */
    void addSharedResourceAttributes(SharedResourceAttributes sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException;

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
     * Retrieves shared resource attributes for a given resource sharing policy ID.
     *
     * @param resourceSharingPolicyId The ID of the resource sharing policy.
     * @return A list of {@link SharedResourceAttributes} associated with the given policy ID.
     * @throws ResourceSharingPolicyMgtServerException If an error occurs while retrieving the shared resource
     *                                                 attributes.
     */
    List<SharedResourceAttributes> getSharedResourceAttributes(int resourceSharingPolicyId)
            throws ResourceSharingPolicyMgtServerException, DataAccessException;
}
