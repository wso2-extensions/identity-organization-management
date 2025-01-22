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

import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementServerException;
import org.wso2.carbon.identity.role.v2.mgt.core.listener.AbstractRoleManagementListener;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants.Error.UNEXPECTED_SERVER_ERROR;

public class SharedUserRoleListener extends AbstractRoleManagementListener {

    private final UserIDResolver userIDResolver = new UserIDResolver();
    private final OrganizationUserSharingDAO organizationUserSharingDAO = new OrganizationUserSharingDAOImpl();

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

        //todo
        //int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
        try {
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return;
            }
//            return getUpdatedUserIDListToBeDeletedBasedOnPermission(roleID, deletedUserIDList, tenantDomain,
//                    Utils.getOrganizationId());


            List<String> deletedUserNamesList = userIDResolver.getNamesByIDs(deletedUserIDList, tenantDomain);

            List<String> modifiedDeletedUserNamesList =
                    organizationUserSharingDAO.getEligibleUsernamesForUserRemovalFromRole(roleID,
                            deletedUserNamesList, tenantDomain, Utils.getOrganizationId());

//            List<String> modifiedDeletedUserNamesList =
//                    roleDAO.getEligibleUsernamesForUserRemovalFromRole(roleID, deletedUserNamesList, tenantDomain,
//                            Utils.getOrganizationId());

            deletedUserIDList = userIDResolver.getIDsByNames(modifiedDeletedUserNamesList, tenantDomain);
            //return getUserIDsByNames(modifiedDeletedUserNamesList, tenantDomain);


        } catch (OrganizationManagementException e) {
            String errorMessage = "Error while retrieving the organization id for the given tenantDomain: "
                    + tenantDomain;
            throw new IdentityRoleManagementServerException(UNEXPECTED_SERVER_ERROR.getCode(), errorMessage, e);
        }
    }


    private List<String> getUserNamesByIDs(List<String> deletedUserIDList, String tenantDomain)
            throws IdentityRoleManagementException {

        //todo
        return new ArrayList<>();
    }




}
