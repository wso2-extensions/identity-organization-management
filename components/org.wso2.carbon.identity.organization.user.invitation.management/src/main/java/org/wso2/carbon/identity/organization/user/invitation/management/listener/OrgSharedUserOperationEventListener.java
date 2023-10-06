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

package org.wso2.carbon.identity.organization.user.invitation.management.listener;

import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.organization.user.invitation.management.InvitationCoreService;
import org.wso2.carbon.identity.organization.user.invitation.management.InvitationCoreServiceImpl;
import org.wso2.carbon.identity.organization.user.invitation.management.exception.UserInvitationMgtException;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.Map;

import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_CHECK_USER_CLAIM_UPDATE_ALLOWED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_DELETE_INVITED_USER_ASSOCIATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ErrorMessage.ERROR_CODE_MANAGED_ORG_CLAIM_UPDATE_NOT_ALLOWED;

/**
 * Organization shared user operation event listener.
 */
public class OrgSharedUserOperationEventListener extends AbstractIdentityUserOperationEventListener {

    @Override
    public int getExecutionOrderId() {

        return 8;
    }

    @Override
    public boolean doPreDeleteUserWithID(String userID, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }

        InvitationCoreService invitationCoreService = new InvitationCoreServiceImpl();
        try {
            return invitationCoreService.deleteInvitedUserAssociation(userID, userStoreManager);
        } catch (UserInvitationMgtException e) {
            throw new UserStoreException(String.format(ERROR_CODE_DELETE_INVITED_USER_ASSOCIATION.getDescription(),
                    userID), ERROR_CODE_DELETE_INVITED_USER_ASSOCIATION.getCode(), e);
        }
    }

    @Override
    public boolean doPreSetUserClaimValuesWithID(String userID, Map<String, String> claims, String profileName,
                                                 UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        if (!claims.isEmpty() && claims.containsKey(CLAIM_MANAGED_ORGANIZATION)) {
            throw new UserStoreException(String.format(
                    ERROR_CODE_MANAGED_ORG_CLAIM_UPDATE_NOT_ALLOWED.getDescription(), CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORG_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        try {
            InvitationCoreService invitationCoreService = new InvitationCoreServiceImpl();
            return invitationCoreService.isUpdateUserClaimValuesAllowed(userID, profileName, userStoreManager);
        } catch (UserInvitationMgtException e) {
            throw new UserStoreException(String.format(ERROR_CODE_CHECK_USER_CLAIM_UPDATE_ALLOWED.getDescription(),
                    userID), ERROR_CODE_CHECK_USER_CLAIM_UPDATE_ALLOWED.getCode(), e);
        }
    }

    @Override
    public boolean doPreSetUserClaimValueWithID(String userID, String claimURI, String claimValue, String profileName,
                                                UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable() || userStoreManager == null) {
            return true;
        }
        if (CLAIM_MANAGED_ORGANIZATION.equals(claimURI)) {
            throw new UserStoreException(String.format(
                    ERROR_CODE_MANAGED_ORG_CLAIM_UPDATE_NOT_ALLOWED.getDescription(), CLAIM_MANAGED_ORGANIZATION),
                    ERROR_CODE_MANAGED_ORG_CLAIM_UPDATE_NOT_ALLOWED.getCode());
        }
        return true;
    }
}
