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

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.dao.OrganizationUserSharingDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.DEFAULT_PROFILE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ID_CLAIM_READ_ONLY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATE_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Implementation of the organization user association service.
 */
public class OrganizationUserSharingServiceImpl implements OrganizationUserSharingService {

    private final OrganizationUserSharingDAO organizationUserSharingDAO = new OrganizationUserSharingDAOImpl();

    @Override
    public void shareOrganizationUser(String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        try {
            int associatedUserTenantId =
                    IdentityTenantUtil.getTenantId(getOrganizationManager().resolveTenantDomain(associatedOrgId));
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(associatedUserTenantId);
            String userName = userStoreManager.getUser(associatedUserId, null).getUsername();

            HashMap<String, String> userClaims = new HashMap<>();
            userClaims.put(CLAIM_MANAGED_ORGANIZATION, associatedOrgId);
            userClaims.put(ID_CLAIM_READ_ONLY, "true");
            UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(true);

            int tenantId = IdentityTenantUtil.getTenantId(getOrganizationManager().resolveTenantDomain(orgId));
            userStoreManager = getAbstractUserStoreManager(tenantId);
            userStoreManager.addUser(userName, generatePassword(), null, userClaims, DEFAULT_PROFILE);
            String userId = userStoreManager.getUserIDFromUserName(userName);
            organizationUserSharingDAO.createOrganizationUserAssociation(userId, orgId, associatedUserId,
                    associatedOrgId);
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_SHARED_USER, e, orgId);
        }
    }

    @Override
    public boolean unshareOrganizationUsers(String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        List<UserAssociation> userAssociationList =
                organizationUserSharingDAO.getUserAssociationsOfAssociatedUser(associatedUserId, associatedOrgId);
        // Removing the shared users from the shared organizations.
        for (UserAssociation userAssociation : userAssociationList) {
            String organizationId = userAssociation.getOrganizationId();
            String tenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            try {
                AbstractUserStoreManager sharedOrgUserStoreManager = getAbstractUserStoreManager(tenantId);
                sharedOrgUserStoreManager.deleteUserWithID(userAssociation.getUserId());
            } catch (UserStoreException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETE_SHARED_USER, e,
                        userAssociation.getUserId(), organizationId);
            }
        }
        return true;
    }

    @Override
    public boolean deleteUserAssociation(String userId, String associatedOrgId) throws OrganizationManagementException {

        return organizationUserSharingDAO.deleteUserAssociationOfUserByAssociatedOrg(userId, associatedOrgId);
    }

    @Override
    public UserAssociation getUserAssociationOfAssociatedUserByOrgId(String associatedUserId, String orgId)
            throws OrganizationManagementException {

        return organizationUserSharingDAO.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
    }

    @Override
    public UserAssociation getUserAssociation(String sharedUserId, String sharedOrganizationId)
            throws OrganizationManagementException {

        return organizationUserSharingDAO.getUserAssociation(sharedUserId, sharedOrganizationId);
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }

    private String generatePassword() {

        UUID uuid = UUID.randomUUID();
        return uuid.toString().substring(0, 12);
    }
}
