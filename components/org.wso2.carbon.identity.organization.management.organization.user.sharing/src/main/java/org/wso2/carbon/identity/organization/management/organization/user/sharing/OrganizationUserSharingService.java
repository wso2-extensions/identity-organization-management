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

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

/**
 * Service that manages the organization user sharing.
 */
public interface OrganizationUserSharingService {

    /**
     * Creates the association between the shared user and the actual user in the organization.
     *
     * @param orgId            Organization ID of the user is shared.
     * @param associatedUserId Actual user who is associated for a shared user.
     * @param associatedOrgId  The organization ID associated user.
     * @throws OrganizationManagementException If an error occurs while creating the organization user association.
     */
    void shareOrganizationUser(String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException;

    /**
     * UnShare all the shared users for the given user.
     *
     * @param associatedUserId The ID of the associated user.
     * @param associatedOrgId  The ID of the organization where the user is managed.
     * @return True if the user associations are deleted successfully.
     * @throws OrganizationManagementException If an error occurs while deleting the user associations.
     */
    boolean unShareOrganizationUsers(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException;

    /**
     * Delete the organization user association of the shared user.
     *
     * @param userId            The ID of the user.
     * @param associatedOrgId The ID of organization where the user's identity is managed.
     * @return True if the organization user association is deleted successfully.
     * @throws OrganizationManagementException If an error occurs while deleting the organization user association.
     */
    boolean deleteUserAssociation(String userId, String associatedOrgId) throws OrganizationManagementException;

    /**
     * Get the user association of the associated user in a given organization.
     *
     * @param associatedUserId The ID of the user who is associated to the organization.
     * @param orgId            The organization ID of the user.
     * @return The user association of the associated user within a given organization.
     * @throws OrganizationManagementException If an error occurs while retrieving the user association.
     */
    UserAssociation getUserAssociationOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementException;

    /**
     * Get the user association of a user.
     *
     * @param userId The ID of user.
     * @param orgId  The organization ID of the user.
     * @return The user association of the user.
     * @throws OrganizationManagementException If an error occurs while retrieving the user association.
     */
    UserAssociation getUserAssociation(String userId, String orgId) throws OrganizationManagementException;
}
