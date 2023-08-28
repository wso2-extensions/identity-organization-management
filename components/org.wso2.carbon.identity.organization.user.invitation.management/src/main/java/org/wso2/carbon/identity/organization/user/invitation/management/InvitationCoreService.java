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

package org.wso2.carbon.identity.organization.user.invitation.management;

import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.identity.organization.user.invitation.management.models.Invitation;
import org.wso2.carbon.user.api.UserStoreManager;

import java.util.List;

/**
 * Service that manages the invitations for the organization users.
 */
public interface InvitationCoreService {

    /**
     * Creates an invitation with the details coming from the user.
     *
     * @param invitation Contains the details that are required to create an invitation.
     * @return The created invitation.
     * @throws UserInvitationMgtException If an error occurs while creating the invitation.
     */
    Invitation createInvitation(Invitation invitation) throws UserInvitationMgtException;

    /**
     * Accepts the invitation with the given confirmation code.
     *
     * @param confirmationCode The confirmation code of the invitation.
     * @return True if the invitation is accepted successfully.
     */
    boolean acceptInvitation(String confirmationCode) throws UserInvitationMgtException;

    /**
     * Introspects the invitation with the given confirmation code.
     *
     * @param confirmationCode The confirmation code of the invitation which needs to be introspected.
     * @return The introspected details of the invitation.
     * @throws UserInvitationMgtException If an error occurs while introspecting the invitation.
     */
    Invitation introspectInvitation(String confirmationCode) throws UserInvitationMgtException;

    /**
     * Returns the invitations list for the given invitation id.
     *
     * @param filter The filter to be applied.
     * @return The list of invitations.
     * @throws UserInvitationMgtException If an error occurs while retrieving the invitations.
     */
    List<Invitation> getInvitations(String filter) throws UserInvitationMgtException;

    /**
     * Deletes the invitation with the given invitation id.
     *
     * @param invitationId The invitation id of the invitation which needs to be deleted.
     * @return True if the invitation is deleted successfully.
     * @throws UserInvitationMgtException If an error occurs while deleting the invitation.
     */
    boolean deleteInvitation(String invitationId) throws UserInvitationMgtException;

    /**
     * Resends the invitation for the given user.
     *
     * @param username The username of the user.
     * @param domain The user domain of the user.
     * @return The resent invitation.
     * @throws UserInvitationMgtException If an error occurs while resending the invitation.
     */
    Invitation resendInvitation(String username, String domain) throws UserInvitationMgtException;

    /**
     * Delete the associations of the invited user.
     *
     * @param userId The ID of the invited user.
     * @param userStoreManager The user store manager of the invited user.
     * @return True if the associations are deleted successfully.
     * @throws UserInvitationMgtException If an error occurs while deleting the associations.
     */
    boolean deleteInvitedUserAssociation(String userId, UserStoreManager userStoreManager)
            throws UserInvitationMgtException;
}
