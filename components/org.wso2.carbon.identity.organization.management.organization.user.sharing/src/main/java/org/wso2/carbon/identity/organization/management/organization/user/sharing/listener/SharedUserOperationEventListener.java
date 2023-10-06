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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.listener;

import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.core.UserStoreClientException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.Map;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * User operation event listener for shared user management.
 */
public class SharedUserOperationEventListener extends AbstractIdentityUserOperationEventListener {

    private final OrganizationUserSharingService organizationUserSharingService =
            new OrganizationUserSharingServiceImpl();

    @Override
    public int getExecutionOrderId() {

        return 8;
    }

    @Override
    public boolean doPreDeleteUserWithID(String userID, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        try {
            String userManagedOrganizationClaim =
                    getUserManagedOrganizationClaim((AbstractUserStoreManager) userStoreManager, userID);
            if (userManagedOrganizationClaim == null) {
                String organizationId = getOrganizationId();
                if (organizationId == null) {
                    organizationId = SUPER_ORG_ID;
                }
                return organizationUserSharingService.unShareOrganizationUsers(userID, organizationId);
            }
            // Delete the organization user association of the shared user by shared user ID.
            return organizationUserSharingService.deleteOrganizationUserAssociationOfSharedUser(userID,
                    userManagedOrganizationClaim);
        } catch (OrganizationManagementException e) {
            throw new UserStoreException(e.getMessage(), e.getErrorCode(), e);
        }
    }

    @Override
    public boolean doPreSetUserClaimValuesWithID(String userID, Map<String, String> claims, String profileName,
                                                 UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        if (!claims.isEmpty() && claims.containsKey(CLAIM_MANAGED_ORGANIZATION)) {
            throw new UserStoreClientException(
                    String.format(ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getDescription(),
                            CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValueWithID(String userID, String claimURI, String claimValue, String profileName,
                                                UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        if (CLAIM_MANAGED_ORGANIZATION.equals(claimURI)) {
            throw new UserStoreClientException(
                    String.format(ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getDescription(),
                            CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORGANIZATION_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        return true;
    }

    private String getUserManagedOrganizationClaim(AbstractUserStoreManager userStoreManager, String userId)
            throws UserStoreException {

        String userDomain = userStoreManager.getUser(userId, null).getUserStoreDomain();
        Map<String, String> claimsMap = userStoreManager
                .getUserClaimValuesWithID(userId, new String[]{CLAIM_MANAGED_ORGANIZATION}, userDomain);
        return claimsMap.get(CLAIM_MANAGED_ORGANIZATION);
    }
}
