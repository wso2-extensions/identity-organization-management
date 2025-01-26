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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.listener;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.listener.AbstractRoleManagementListener;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;

import java.util.List;

import static org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants.Error.OPERATION_FORBIDDEN;
import static org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants.Error.UNEXPECTED_SERVER_ERROR;

/**
 * Shared user role update governing event listener.
 */
public class SharedUserRoleGovernanceListener extends AbstractRoleManagementListener {

    private static final UserIDResolver USER_ID_RESOLVER = new UserIDResolver();

    @Override
    public int getDefaultOrderId() {

        return 548;
    }

    @Override
    public boolean isEnable() {

        return true;
    }

    @Override
    public void preUpdateUserListOfRole(String roleID, List<String> newUserIDList, List<String> deletedUserIDList,
                                        String tenantDomain) throws IdentityRoleManagementException {

        if (CollectionUtils.isEmpty(deletedUserIDList)) {
            return;
        }

        try {
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return;
            }

            List<String> deletedUserNamesList = USER_ID_RESOLVER.getNamesByIDs(deletedUserIDList, tenantDomain);
            List<String> nonDeletableUserNamesList =
                    getOrganizationUserSharingService().getNonDeletableUserRoleAssignments(roleID, deletedUserNamesList,
                            tenantDomain, Utils.getOrganizationId());

            if (CollectionUtils.isNotEmpty(nonDeletableUserNamesList)) {
                String errorMessage = String.format(
                        "User(s): %s cannot be deleted from the role: %s from this organization.",
                        String.join(", ", nonDeletableUserNamesList),
                        getRoleManagementService().getRoleNameByRoleId(roleID, tenantDomain));
                throw new IdentityRoleManagementException(OPERATION_FORBIDDEN.getCode(), errorMessage);
            }
        } catch (OrganizationManagementException e) {
            String errorMessage = "Error while retrieving the organization id for the given tenantDomain: "
                    + tenantDomain;
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }
}
