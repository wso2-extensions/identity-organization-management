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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GetUserSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.UserSharePatchDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

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
     * Applies patch operations to attributes of shared users.
     * <p>
     * This method updates attributes of users that are already shared with
     * organizations based on the provided patch operations
     * (for example, updating assigned shared roles from the user share request).
     * <p>
     * This method does not support sharing users with new organizations.
     *
     * @param userSharePatchDO Patch request containing update operations to be applied to an existing user sharing
     *                         configuration.
     * @throws OrganizationManagementException If an error occurs while processing the patch operations.
     */
    void updateRoleAssignmentV2(UserSharePatchDO userSharePatchDO) throws OrganizationManagementException;

    /**
     * Retrieves the organizations shared with a specific user based on the provided criteria.
     *
     * @param getUserSharedOrgsDO Data object containing parameters for retrieving shared organizations.
     * @return ResponseSharedOrgsV2DO Object containing the list of organizations shared with the user and related
     * metadata.
     * @throws OrganizationManagementException If an error occurs while retrieving the shared organizations.
     */
    ResponseSharedOrgsV2DO getUserSharedOrganizationsV2(GetUserSharedOrgsDO getUserSharedOrgsDO)
            throws OrganizationManagementException;
}
