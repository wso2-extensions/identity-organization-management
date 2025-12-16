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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.PatchUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

/**
 * Implementation of the user sharing policy handler service v2.
 */
public class UserSharingPolicyHandlerServiceImplV2 implements UserSharingPolicyHandlerServiceV2 {

    @Override
    public void populateSelectiveUserShareV2(SelectiveUserShareV2DO selectiveUserShareV2DO)
            throws UserSharingMgtException {

        // todo: Implement the logic to share users with selective organizations in v2.
    }

    @Override
    public void populateGeneralUserShareV2(GeneralUserShareV2DO generalUserShareV2DO) throws UserSharingMgtException {

        // todo: Implement the logic to share users with all organizations in v2.
    }

    @Override
    public void populateSelectiveUserUnshareV2(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtException {

        // todo: Implement the logic to unshare users from selective organizations in v2.
    }

    @Override
    public void populateGeneralUserUnshareV2(GeneralUserUnshareDO generalUserUnshareDO) throws UserSharingMgtException {

        // todo: Implement the logic to unshare users from all organizations in v2.
    }

    @Override
    public void updateRoleAssignmentV2(PatchUserShareDO patchUserShareDO) throws OrganizationManagementException {

        // todo: Implement the logic to update role assignments for shared users in v2.
    }

    @Override
    public ResponseSharedOrgsV2DO getUserSharedOrganizationsV2(GetUserSharedOrgsDO getUserSharedOrgsDO)
            throws OrganizationManagementException {

        // todo: Implement the logic to get user shared organizations in v2.
        return null;
    }
}
