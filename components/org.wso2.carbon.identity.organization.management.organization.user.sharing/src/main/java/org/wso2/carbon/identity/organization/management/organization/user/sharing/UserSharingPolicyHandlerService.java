/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserShareMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedRolesDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

/**
 * Service that manages the user sharing policy handler.
 */
public interface UserSharingPolicyHandlerService {

    /**
     * Populates the details required for selective user sharing.
     *
     * @param selectiveUserShareDO The object containing details for selective user sharing.
     * @throws UserShareMgtException If an error occurs while populating the selective user share details.
     */
    void populateSelectiveUserShare(SelectiveUserShareDO selectiveUserShareDO) throws UserShareMgtException;

    /**
     * Populates the details required for general user sharing.
     *
     * @param generalUserShareDO The object containing details for general user sharing.
     * @throws UserShareMgtException If an error occurs while populating the general user share details.
     */
    void populateGeneralUserShare(GeneralUserShareDO generalUserShareDO) throws UserShareMgtException;

    /**
     * Populates the details required for selective user unsharing.
     *
     * @param selectiveUserUnshareDO The object containing details for selective user unsharing.
     * @throws UserShareMgtException If an error occurs while populating the selective user unshare details.
     */
    void populateSelectiveUserUnshare(SelectiveUserUnshareDO selectiveUserUnshareDO) throws UserShareMgtException;

    /**
     * Populates the details required for general user unsharing.
     *
     * @param generalUserUnshareDO The object containing details for general user unsharing.
     * @throws UserShareMgtException If an error occurs while populating the general user unshare details.
     */
    void populateGeneralUserUnshare(GeneralUserUnshareDO generalUserUnshareDO) throws UserShareMgtException;

    /**
     * Retrieves the organizations that a user has been shared with.
     *
     * @param associatedUserId The ID of the user whose shared organizations are to be retrieved.
     * @param after            The cursor indicating the start of the page for pagination.
     * @param before           The cursor indicating the end of the page for pagination.
     * @param limit            The maximum number of organizations to retrieve.
     * @param filter           The filter criteria to apply when retrieving organizations.
     * @param recursive        Whether to include child organizations recursively.
     * @return A {@link ResponseSharedOrgsDO} object containing the shared organizations and pagination details.
     * @throws UserShareMgtException If an error occurs while retrieving the shared organizations of the user.
     */
    ResponseSharedOrgsDO getSharedOrganizationsOfUser(String associatedUserId, String after, String before,
                                                      Integer limit, String filter,
                                                      Boolean recursive)
            throws UserShareMgtException;

    /**
     * Retrieves the roles shared with a user in a specific organization.
     *
     * @param associatedUserId The ID of the user whose shared roles are to be retrieved.
     * @param orgId            The ID of the organization from which the roles are to be retrieved.
     * @param after            The cursor indicating the start of the page for pagination.
     * @param before           The cursor indicating the end of the page for pagination.
     * @param limit            The maximum number of roles to retrieve.
     * @param filter           The filter criteria to apply when retrieving roles.
     * @param recursive        Whether to include child organizations recursively.
     * @return A {@link ResponseSharedRolesDO} object containing the shared roles and pagination details.
     * @throws UserShareMgtException If an error occurs while retrieving the roles shared with the user in the organization.
     */
    ResponseSharedRolesDO getRolesSharedWithUserInOrganization(String associatedUserId, String orgId, String after,
                                                               String before, Integer limit,
                                                               String filter, Boolean recursive)
            throws UserShareMgtException;
}
