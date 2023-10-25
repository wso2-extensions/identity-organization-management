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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;

/**
 * Utility class for organization shared user management.
 */
public class OrganizationSharedUserUtil {

    public static String getUserManagedOrganizationClaim(AbstractUserStoreManager userStoreManager, String userId)
            throws UserStoreException {

        String userDomain = userStoreManager.getUser(userId, null).getUserStoreDomain();
        Map<String, String> claimsMap = userStoreManager
                .getUserClaimValuesWithID(userId, new String[]{CLAIM_MANAGED_ORGANIZATION}, userDomain);
        return claimsMap.get(CLAIM_MANAGED_ORGANIZATION);
    }


    /**
     * Get the user ID of the associated user by the organization ID.
     */
    public static Optional<String> getUserIdOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementException {

        UserAssociation userAssociation = OrganizationUserSharingDataHolder.getInstance()
                .getOrganizationUserSharingService()
                .getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
        if (userAssociation == null) {
            return Optional.empty();
        }
        return Optional.of(userAssociation.getUserId());
    }
}
