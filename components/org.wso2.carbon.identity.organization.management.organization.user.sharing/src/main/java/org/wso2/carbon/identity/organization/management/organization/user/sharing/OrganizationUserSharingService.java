/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

import java.util.List;

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
     * Creates the association between the shared user and the actual user in the organization.
     *
     * @param orgId            Organization ID of the user is shared.
     * @param associatedUserId Actual user who is associated for a shared user.
     * @param associatedOrgId  The organization ID associated user.
     * @param sharedType       The type of sharing for the user in the organization.
     * @throws OrganizationManagementException If an error occurs while creating the organization user association.
     */
    default void shareOrganizationUser(String orgId, String associatedUserId, String associatedOrgId,
                                       SharedType sharedType) throws OrganizationManagementException {

        throw new NotImplementedException("shareOrganizationUser method is not implemented.");
    }

    /**
     * Unshare all the shared users for the given user.
     *
     * @param associatedUserId The ID of the associated user.
     * @param associatedOrgId  The ID of the organization where the user is managed.
     * @return True if the user associations are deleted successfully.
     * @throws OrganizationManagementException If an error occurs while deleting the user associations.
     */
    boolean unshareOrganizationUsers(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException;

    /**
     * Unshare the specified user in the given shared organization.
     *
     * @param associatedUserId  The ID of the associated user.
     * @param sharedOrgId       The ID of the shared organization from which the user will be unshared.
     * @return True if the user is unshared successfully.
     * @throws OrganizationManagementException If an error occurs while unsharing the user in the shared organization.
     */
    default boolean unshareOrganizationUserInSharedOrganization(String associatedUserId, String sharedOrgId)
            throws OrganizationManagementException {

        throw new NotImplementedException("unshareOrganizationUserInSharedOrganization method is not implemented in " +
                this.getClass().getName());
    }

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

    /**
     * Get all the user associations for a given user.
     *
     * @param actualUserId  Actual user ID of the user.
     * @param residentOrgId The organization ID where is the user is managed.
     * @return the list of {@link UserAssociation}s.
     * @throws OrganizationManagementException If an error occurs while fetching user associations.
     */
    default List<UserAssociation> getUserAssociationsOfGivenUser(String actualUserId, String residentOrgId)
            throws OrganizationManagementException {

        return null;
    }

    /**
     * Get all user associations for a given user filtered by shared type.
     *
     * @param actualUserId  Actual user ID of the user.
     * @param residentOrgId The organization ID where the user is managed.
     * @param sharedType    The type of sharing relationship to filter the associations.
     * @return A list of {@link UserAssociation}s.
     * @throws OrganizationManagementException If an error occurs while fetching user associations.
     */
    default List<UserAssociation> getUserAssociationsOfGivenUser(String actualUserId, String residentOrgId,
                                                                 SharedType sharedType)
            throws OrganizationManagementException {

        throw new NotImplementedException("getUserAssociationsOfGivenUser method is not implemented.");
    }

    /**
     * Retrieve the list of usernames that are not eligible to be removed from the specified role within the given
     * tenant domain, based on the permissions of the requesting organization.
     *
     * @param roleId               The role ID from which the users are to be removed.
     * @param deletedUserNamesList The list of usernames intended for removal.
     * @param tenantDomain         The tenant domain where the role assignment is available.
     * @param requestingOrgId      The ID of the requesting organization performing the operation.
     * @return A list of usernames that the requesting organization is not permitted to remove from the given role.
     * @throws IdentityRoleManagementException If an error occurs while validating the permissions or retrieving
     *                                         eligible usernames.
     */
    default List<String> getNonDeletableUserRoleAssignments(String roleId, List<String> deletedUserNamesList,
                                                            String tenantDomain, String requestingOrgId)
            throws IdentityRoleManagementException {

        throw new NotImplementedException(
                "getNonDeletableUserRoleAssignments method is not implemented.");
    }

    /**
     * Retrieves the shared user roles among the given user roles.
     *
     * @param allUserRolesOfSharedUser List of all roles associated with the shared user.
     * @param tenantDomain             The tenant domain of the shared user.
     * @return List of shared user roles.
     * @throws IdentityRoleManagementException If an error occurs while retrieving shared user roles.
     */
    default List<String> getSharedUserRolesFromUserRoles(List<String> allUserRolesOfSharedUser, String tenantDomain)
            throws IdentityRoleManagementException {

        throw new NotImplementedException("getSharedUserRolesFromUserRoles method is not implemented.");
    }

    /**
     * Adds edit restrictions for shared user roles.
     *
     * @param roleId         The roleId of the role to be restricted.
     * @param username       The username of the shared user.
     * @param tenantDomain   The tenant domain of the shared user.
     * @param domainName     The user store domain name associated with the user.
     * @param editOperation  The type of edit operation being performed.
     * @param permittedOrgId The organization ID with permitted access.
     * @throws UserSharingMgtException If an error occurs while retrieving shared user roles.
     */
    default void addEditRestrictionsForSharedUserRole(String roleId, String username, String tenantDomain,
                                                       String domainName, EditOperation editOperation,
                                                       String permittedOrgId)
            throws UserSharingMgtException {

        throw new NotImplementedException("addEditRestrictionsForSharedUserRoles method is not implemented.");
    }

    /**
     * Retrieves the IDs of roles shared with a user in a specific organization.
     *
     * @param username The username of the user whose shared roles are to be retrieved.
     * @param tenantId The ID of the tenant to which the user belongs.
     * @param domainName The name of the domain in which the user resides.
     * @return A {@link List<String>} containing the IDs of the shared roles.
     * @throws UserSharingMgtException If an error occurs while retrieving the roles shared with the user
     * in the organization.
     */
    default List<String> getRolesSharedWithUserInOrganization(String username, int tenantId, String domainName)
            throws UserSharingMgtException {

        throw new NotImplementedException("getRolesSharedWithUserInOrganization method is not implemented.");
    }

    /**
     * Get the user associations of the associated user in the given organizations.
     *
     * @param associatedUserId The ID of the associated user.
     * @param orgIds           The list of organization IDs.
     * @param sharedType       The type of sharing relationship to filter the associations.
     * @return The list of {@link UserAssociation}s for the given user in the specified organizations.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the user associations.
     */
    default List<UserAssociation> getUserAssociationsOfGivenUserOnGivenOrgs(String associatedUserId,
                                                                            List<String> orgIds, SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("getUserAssociationsOfGivenUserOnGivenOrgs method is not implemented.");
    }
}
