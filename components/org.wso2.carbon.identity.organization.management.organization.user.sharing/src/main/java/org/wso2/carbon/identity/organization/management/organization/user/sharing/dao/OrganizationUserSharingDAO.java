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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.dao;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.NotImplementedException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

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
     * Creates the association between the shared user and the actual user in the shared organization.
     *
     * @param userId           ID of the user who gets created in the organization.
     * @param orgId            Organization ID of the user's shared organization.
     * @param associatedUserId Actual user ID of the associated user.
     * @param associatedOrgId  The organization ID where the associated user is managed.
     * @param sharedType       The type of sharing for the user in the organization.
     * @throws OrganizationManagementServerException If an error occurs while creating the organization user
     *                                               association.
     */
    default void createOrganizationUserAssociation(String userId, String orgId, String associatedUserId,
                                                   String associatedOrgId, SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("createOrganizationUserAssociation method is not implemented.");
    }

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
     * Get all the user associations for a given user filtered by shared type.
     *
     * @param associatedUserId Actual user ID of the user.
     * @param associatedOrgId  The organization ID where the user is managed.
     * @param sharedType       The type of sharing relationship to filter the associations.
     * @return the list of {@link UserAssociation}s.
     * @throws OrganizationManagementServerException If an error occurs while fetching user associations.
     */
    default List<UserAssociation> getUserAssociationsOfAssociatedUser(String associatedUserId, String associatedOrgId,
                                                              SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("getUserAssociationsOfGivenUser method is not implemented.");
    }

    /**
     * Checks if the given user has at least one association with any child organization.
     *
     * @param associatedUserId The ID of the associated user.
     * @param associatedOrgId  The organization ID where the user's identity is managed.
     * @return True if the user has at least one association with any organization.
     * @throws OrganizationManagementServerException If an error occurs while checking user associations.
     */
    default boolean hasUserAssociations(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("hasUserAssociations method is not implemented.");
    }

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

    /**
     * Retrieve the list of usernames that are not eligible to be removed from the specified role within the given
     * tenant domain, based on the permissions of the requesting organization.
     *
     * @param roleId                              The role ID from which the users are to be removed.
     * @param deletedDomainQualifiedUserNamesList The list of usernames with domain intended for removal.
     * @param tenantDomain                        The tenant domain where the operation is being performed.
     * @param requestingOrgId                     The ID of the requesting organization performing the operation.
     * @return A list of usernames that the requesting organization is not permitted to remove from the given role.
     * @throws IdentityRoleManagementException If an error occurs while validating the permissions or retrieving
     *                                         eligible usernames.
     */
    default List<String> getNonDeletableUserRoleAssignments(String roleId,
                                                            List<String> deletedDomainQualifiedUserNamesList,
                                                            String tenantDomain, String requestingOrgId)
            throws IdentityRoleManagementException {

        throw new NotImplementedException("getNonDeletableUserRoleAssignments method is not implemented.");
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
     * @param domainName     The domain name associated with the user.
     * @param editOperation  The type of edit operation being performed.
     * @param permittedOrgId The organization ID with permitted access.
     * @throws UserSharingMgtServerException If an error occurs while retrieving shared user roles.
     */
    default void addEditRestrictionsForSharedUserRole(String roleId, String username, String tenantDomain,
                                                       String domainName, EditOperation editOperation,
                                                       String permittedOrgId)
            throws UserSharingMgtServerException {

        throw new NotImplementedException("addEditRestrictionsForSharedUserRoles method is not implemented.");
    }

    /**
     * Retrieves the IDs of roles shared with a user in a specific organization.
     *
     * @param username   The username of the user whose shared roles are to be retrieved.
     * @param tenantId   The ID of the tenant to which the user belongs.
     * @param domainName The name of the domain in which the user resides.
     * @return A {@link List<String>} containing the IDs of the shared roles.
     * @throws UserSharingMgtServerException If an error occurs while retrieving the roles shared with the user
     * in the organization.
     */
    default List<String> getRolesSharedWithUserInOrganization(String username, int tenantId, String domainName)
            throws UserSharingMgtServerException {

        throw new NotImplementedException("getRolesSharedWithUserInOrganization method is not implemented.");
    }

    /**
     * Get the user associations of the associated user in the given organizations.
     *
     * @param associatedUserId The ID of the associated user.
     * @param orgIds           The list of organization IDs.
     * @return The list of {@link UserAssociation}s for the given user in the specified organizations.
     * @throws OrganizationManagementServerException If an error occurs while retrieving the user associations.
     */
    default List<UserAssociation> getUserAssociationsOfGivenUserOnGivenOrgs(String associatedUserId,
                                                                            List<String> orgIds)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("getUserAssociationsOfAssociatedUserOnGivenOrgs method is not implemented.");
    }

    /**
     * Updates the shared type of user association.
     *
     * @param id         The ID of the user association.
     * @param sharedType The new shared type to be set for the user association.
     * @throws OrganizationManagementServerException If an error occurs while updating the shared type.
     */
    default void updateSharedTypeOfUserAssociation(int id, SharedType sharedType)
            throws OrganizationManagementServerException {

        throw new NotImplementedException("updateSharedTypeOfUserAssociation method is not implemented.");
    }
}
