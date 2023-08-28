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

package org.wso2.carbon.identity.organization.user.invitation.management.dao;

import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtServerException;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;

import java.util.List;

/**
 * DAO interface for invitations
 */
public interface UserInvitationDAO {

    /**
     * Create an invitation in the DB layer with the provided details.
     *
     * @param invitation Contains the invitation details which needs to be stored.
     * @throws UserInvitationMgtServerException If an error occurs while creating the invitation.
     */
    void createInvitation(Invitation invitation) throws UserInvitationMgtServerException;

    /**
     * Get the invitation details by using the confirmation code of the invitation.
     *
     * @param confirmationCode The confirmation code of the invitation.
     * @return The invitation details for the confirmation code.
     * @throws UserInvitationMgtServerException If an error occurs while retrieving the invitation.
     */
    Invitation getInvitationByConfirmationCode(String confirmationCode)
            throws UserInvitationMgtServerException;

    /**
     * Get the invitation details with role assignments by using the confirmation code of the invitation.
     *
     * @param confirmationCode The confirmation code of the invitation.
     * @return The invitation details with the role assignment for the confirmation code.
     * @throws UserInvitationMgtServerException If an error occurs while retrieving the invitation.
     */
    Invitation getInvitationWithRolesByConfirmationCode(String confirmationCode)
            throws UserInvitationMgtServerException;

    /**
     * Get the invitation details by using the invitation id of the invitation.
     *
     * @param invitationId The invitation id of the invitation.
     * @return The invitation details for the invitation id.
     * @throws UserInvitationMgtServerException If an error occurs while retrieving the invitation.
     */
    Invitation getInvitationByInvitationId(String invitationId)
            throws UserInvitationMgtServerException;

    /**
     * Get the invitation details by using the organization id of the organization.
     *
     * @param organizationId The organization id of the organization.
     * @return The invitations details list for the organization id.
     * @throws UserInvitationMgtServerException If an error occurs while retrieving the invitations.
     */
    List<Invitation> getInvitationsByOrganization(String organizationId, String filterParam, String filterOp,
                                                  String filterValue) throws UserInvitationMgtServerException;

    /**
     * Remove the invitation details by using the invitation id of the invitation.
     *
     * @param invitationId The invitation id of the invitation.
     * @return True if the invitation is deleted successfully.
     * @throws UserInvitationMgtServerException If an error occurs while deleting the invitation.
     */
    boolean deleteInvitation(String invitationId) throws UserInvitationMgtServerException;

    /**
     * Get the invitation details of a user based on the details.
     *
     * @param username The username of the user.
     * @param domain The user domain of the user.
     * @param userOrganizationId The organization id of the user.
     * @param invitedOrganizationId The organization id of the invited organization.
     * @return The invitation details for the user.
     * @throws UserInvitationMgtException If an error occurs while retrieving the invitation.
     */
    Invitation getActiveInvitationByUser(String username, String domain, String userOrganizationId,
                                         String invitedOrganizationId) throws UserInvitationMgtException;

    /**
     * Creates the association between the shared user and the actual user.
     *
     * @param realUserId Actual user id of the user in the parent organization.
     * @param residentOrgId Organization id of the organization in which the user resides.
     * @param sharedUserId ID of the user which is created in the invited organization.
     * @param sharedOrgId Organization id of the invited organization.
     * @throws UserInvitationMgtException If an error occurs while creating the association.
     */
    void createOrganizationAssociation(String realUserId, String residentOrgId, String sharedUserId,
                                       String sharedOrgId) throws UserInvitationMgtException;

    /**
     * Delete the invited organization user association to the child organization from the child organization.
     *
     * @param userId The user id of the invited organization user.
     * @param organizationId The organization id of the invited/child organization.
     * @return True if the association is deleted successfully.
     * @throws UserInvitationMgtException If an error occurs while deleting the association.
     */
    boolean deleteOrgUserAssociationToSharedOrg(String userId, String organizationId)
            throws UserInvitationMgtException;

    /**
     * Delete all the associations for the child organizations when the user is deleting from the parent organization.
     *
     * @param userId Actual user id of the user in the parent organization.
     * @param organizationId The organization id of the parent organization.
     * @return True if the associations are deleted successfully.
     * @throws UserInvitationMgtException If an error occurs while deleting the associations.
     */
    boolean deleteAllAssociationsOfOrgUserToSharedOrgs(String userId, String organizationId)
            throws UserInvitationMgtException;
}
