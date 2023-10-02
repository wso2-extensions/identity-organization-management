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

package org.wso2.carbon.identity.organization.management.organization.user.association.dao;

import org.wso2.carbon.identity.organization.management.organization.user.association.models.SharedUserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.List;

/**
 * DAO interface for organization user association management.
 */
public interface OrganizationUserAssociationDAO {

    /**
     * Creates the association between the shared user and the actual user in the shared organization.
     *
     * @param userId             Actual user ID of the user in the parent organization.
     * @param userOrganizationId The organization ID where the user's identity is managed.
     * @param sharedUserId       ID of the user which is created in the shared organization.
     * @param sharedOrgId        Organization ID of the user shared organization.
     * @throws OrganizationManagementServerException If an error occurs while creating the organization user
     *                                               association.
     */
    void createOrganizationUserAssociation(String userId, String userOrganizationId, String sharedUserId,
                                           String sharedOrgId) throws OrganizationManagementServerException;

    /**
     * Delete the organization user association for a shared user in a shared organization.
     *
     * @param sharedUserId       The shared user ID of the user shared with an organization.
     * @param userOrganizationId The organization ID where the user's identity is managed.
     * @return True if the organization user association is deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the organization user
     *                                               association.
     */
    boolean deleteOrganizationUserAssociationOfSharedUser(String sharedUserId, String userOrganizationId)
            throws OrganizationManagementServerException;

    /**
     * Delete all the organization user associations for a given user.
     *
     * @param userId             Actual user ID of the user.
     * @param userOrganizationId The organization ID where the user's identity is managed.
     * @return True if all the organization user associations are deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the organization user
     *                                               associations.
     */
    boolean deleteOrganizationUserAssociations(String userId, String userOrganizationId)
            throws OrganizationManagementServerException;

    /**
     * Get all the organization user associations for a given user.
     *
     * @param userId         Actual user ID of the user.
     * @param userOrganizationId The organization ID where is the user is managed.
     * @return the list of {@link SharedUserAssociation}s.
     * @throws OrganizationManagementServerException If an error occurs while fetching organization user associations.
     */
    List<SharedUserAssociation> getOrganizationUserAssociationsOfUser(String userId, String userOrganizationId)
            throws OrganizationManagementServerException;

    /**
     * Get the organization user association of a given user in a given organization.
     *
     * @param userId      Actual user ID of the user.
     * @param sharedOrgId Organization ID where the user is shared.
     * @return The organization users association details.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the organization user
     *                                               association.
     */
    SharedUserAssociation getOrganizationUserAssociationOfUserAtSharedOrg(String userId, String sharedOrgId)
            throws OrganizationManagementServerException;
}
