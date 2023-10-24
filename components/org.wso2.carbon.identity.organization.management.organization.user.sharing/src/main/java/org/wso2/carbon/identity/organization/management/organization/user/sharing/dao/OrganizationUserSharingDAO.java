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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.dao;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.List;

/**
 * DAO interface for organization user sharing.
 */
public interface OrganizationUserSharingDAO {

    /**
     * Creates the association between the shared user and the actual user in the shared organization.
     *
     * @param userId           ID of the user who gets created in the organization.
     * @param orgId            Organization ID of the user shared organization.
     * @param associatedUserId Actual user ID of the associated user.
     * @param associatedOrgId  The organization ID where the associated user is managed.
     * @throws OrganizationManagementServerException If an error occurs while creating the organization user
     *                                               association.
     */
    void createOrganizationUserAssociation(String userId, String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Delete the organization user association for a shared user in a shared organization.
     *
     * @param userId          The ID of the user.
     * @param associatedOrgId The organization ID where the associated user's identity is managed.
     * @return True if the user association is deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the user association.
     */
    boolean deleteUserAssociationOfUserByAssociatedOrg(String userId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Delete all the organization user associations for a given user.
     *
     * @param associatedUserId Actual user ID of the user.
     * @param associatedOrgId  The organization ID where the user's identity is managed.
     * @return True if all the user associations are deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the user associations.
     */
    boolean deleteUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Get all the user associations for a given user.
     *
     * @param associatedUserId Actual user ID of the user.
     * @param associatedOrgId  The organization ID where is the user is managed.
     * @return the list of {@link UserAssociation}s.
     * @throws OrganizationManagementServerException If an error occurs while fetching user associations.
     */
    List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException;

    /**
     * Get the organization user association of a given user in a given organization.
     *
     * @param associatedUserId ID of the associated user.
     * @param orgId            Organization ID where the user is shared.
     * @return The organization users association details.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the user association.
     */
    UserAssociation getUserAssociationOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementServerException;

    /**
     * Get the shared user association of a shared user.
     *
     * @param userId         The user ID of the shared user.
     * @param organizationId The organization ID of the user.
     * @return The user association of the user.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the user association.
     */
    UserAssociation getUserAssociation(String userId, String organizationId)
            throws OrganizationManagementServerException;
}
