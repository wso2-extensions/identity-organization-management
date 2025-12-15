/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleAssignmentUpdateDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;

/**
 * Service that manages the user sharing policy handler.
 */
public interface UserSharingPolicyHandlerServiceV2 {

    /**
     * Populates the details required for selective user sharing.
     *
     * @param selectiveUserShareV2DO The object containing details for selective user sharing V2.
     * @throws UserSharingMgtException If an error occurs while populating the selective user share details.
     */
    void populateSelectiveUserShareV2(SelectiveUserShareV2DO selectiveUserShareV2DO) throws UserSharingMgtException;

    /**
     * Populates the details required for general user sharing.
     *
     * @param generalUserShareV2DO The object containing details for general user sharing V2.
     * @throws UserSharingMgtException If an error occurs while populating the general user share details.
     */
    void populateGeneralUserShareV2(GeneralUserShareV2DO generalUserShareV2DO) throws UserSharingMgtException;

    /**
     * Populates the details required for selective user unsharing.
     *
     * @param selectiveUserUnshareDO The object containing details for selective user unsharing.
     * @throws UserSharingMgtException If an error occurs while populating the selective user unshare details.
     */
    void populateSelectiveUserUnshareV2(SelectiveUserUnshareDO selectiveUserUnshareDO) throws UserSharingMgtException;

    /**
     * Populates the details required for general user unsharing.
     *
     * @param generalUserUnshareDO The object containing details for general user unsharing.
     * @throws UserSharingMgtException If an error occurs while populating the general user unshare details.
     */
    void populateGeneralUserUnshareV2(GeneralUserUnshareDO generalUserUnshareDO) throws UserSharingMgtException;

    /**
     * Updates the role assignments of a shared user with the given update operations.
     * This method is intended to support updating the roles of the shared user in the shared organizations.
     * This method does not support sharing the user with new organizations.
     *
     * @param mainOrganizationId     Main organization ID that owns the primary user.
     * @param userId                 ID of the primary user whose shared associations need to be updated.
     * @param roleAssignmentUpdateDO List of update operations to be performed on the assigned roles of the shared user.
     *                               You have to specify the operation type, path and values to be updated.
     * @throws OrganizationManagementException on errors when updating the shared user.
     */
    void updateRoleAssignmentV2(String mainOrganizationId, String userId,
                                List<RoleAssignmentUpdateDO> roleAssignmentUpdateDO)
            throws OrganizationManagementException;

    /**
     * Returns the list of organizations with whom the primary user is shared. This method provides
     * filtering, pagination, and other options to retrieve the shared organizations. Currently,
     * this has support to filter organizations by organization id and parent organization id.
     *
     * @param mainOrganizationId ID of the main organization owning the primary user.
     * @param userId             ID of the primary user in the main organization.
     * @param filter             (Optional) Filter to search for shared organizations (optional). Currently supports
     *                           filtering by organization id and parent organization id.
     *                           Ex: `id eq 088fb49c-46fa-48c1-a0a8-5538ee4b7ec5` or
     *                           `parentId eq 088fb49c-46fa-48c1-a0a8-5538ee4b7ec5`
     * @param beforeCursor       (Optional) The before cursor to get the previous page of results. This should be the
     *                           shared organization identifier. NOTE: We always prioritize the before cursor over the
     *                           after cursor. Value cannot be 0.
     * @param afterCursor        (Optional) The after cursor to get the next page of results. This should be the
     *                           shared organization identifier.
     * @param excludedAttributes (Optional) A comma separated list of attributes to be excluded from the result.
     *                           Currently supports excluding `roles`.
     * @param attributes         (Optional) A comma separated list of attributes to be included in the result.
     *                           Currently supports including `roles`.
     * @param limit              (Optional) The maximum number of results to be returned. If not specified (that is, 0),
     *                           it will return all the results.
     * @param recursive          (Optional) If true, it will return the shared organizations recursively. If false, it
     *                           will
     *                           return only the immediate child organizations of the main organization.
     * @return A {@code ResponseSharedOrgsV2DO} containing the shared organization nodes for the given user,
     * along with pagination cursor information and active user sharing mode details.
     * @throws OrganizationManagementException If an error occurs while retrieving the list of shared organizations.
     */
    ResponseSharedOrgsV2DO getUserSharedOrganizationsV2(String mainOrganizationId, String userId, String filter,
                                                        int beforeCursor, int afterCursor, String excludedAttributes,
                                                        String attributes, int limit, boolean recursive)
            throws OrganizationManagementException;
}
