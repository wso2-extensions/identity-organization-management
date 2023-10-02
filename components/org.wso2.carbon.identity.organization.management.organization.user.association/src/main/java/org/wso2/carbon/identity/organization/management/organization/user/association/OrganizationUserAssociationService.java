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

package org.wso2.carbon.identity.organization.management.organization.user.association;

import org.wso2.carbon.identity.organization.management.organization.user.association.models.SharedUserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

/**
 * Service that manages the organization user sharing.
 */
public interface OrganizationUserAssociationService {

    /**
     * Creates the association between the shared user and the actual user in the shared organization.
     *
     * @param userId             Actual user ID of the user in the parent organization.
     * @param userOrganizationId The organization ID where the user's identity is managed.
     * @param sharedUserId       ID of the user which is created in the shared organization.
     * @param sharedOrgId        Organization ID of the user shared organization.
     * @throws OrganizationManagementException If an error occurs while creating the organization user association.
     */
    void createOrganizationUserAssociation(String userId, String userOrganizationId, String sharedUserId,
                                           String sharedOrgId) throws OrganizationManagementException;

    /**
     * Delete the organization user association of the shared user.
     *
     * @param sharedUserId       The ID of the shared user.
     * @param userOrganizationId The ID of organization where the user's identity is managed.
     * @return True if the organization user association is deleted successfully.
     * @throws OrganizationManagementException If an error occurs while deleting the organization user association.
     */
    boolean deleteOrganizationUserAssociationOfSharedUser(String sharedUserId, String userOrganizationId)
            throws OrganizationManagementException;

    /**
     * Delete the organization user associations of a given user.
     *
     * @param userId The ID of the user.
     * @param userOrganizationId The ID of the organization where the user is managed.
     * @return True if the organization user associations are deleted successfully.
     * @throws OrganizationManagementException If an error occurs while deleting the organization user associations.
     */
    boolean deleteOrganizationUserAssociations(String userId, String userOrganizationId)
            throws OrganizationManagementException;

    /**
     * Get the shared user association of the user.
     *
     * @param userId               The actual ID of the user.
     * @param sharedOrganizationId The organization ID of the user.
     * @return The shared user association of the user.
     * @throws OrganizationManagementException If an error occurs while retrieving the shared user association.
     */
    SharedUserAssociation getSharedUserAssociationOfUserAtSubOrg(String userId, String sharedOrganizationId)
            throws OrganizationManagementException;
}
