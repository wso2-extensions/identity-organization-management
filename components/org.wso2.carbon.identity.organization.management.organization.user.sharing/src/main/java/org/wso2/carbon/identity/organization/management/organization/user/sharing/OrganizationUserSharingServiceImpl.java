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
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.SharedUserAssociation;
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
    public void shareOrganizationUser(String realUserId, String userResidentOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        try {
            int userResidentTenantId =
                    IdentityTenantUtil.getTenantId(getOrganizationManager().resolveTenantDomain(userResidentOrgId));
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(userResidentTenantId);
            String userName = userStoreManager.getUser(realUserId, null).getUsername();

            HashMap<String, String> userClaims = new HashMap<>();
            userClaims.put(CLAIM_MANAGED_ORGANIZATION, userResidentOrgId);
            userClaims.put(ID_CLAIM_READ_ONLY, "true");
            UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(true);

            int sharedOrgTenantId =
                    IdentityTenantUtil.getTenantId(getOrganizationManager().resolveTenantDomain(sharedOrgId));
            userStoreManager = getAbstractUserStoreManager(sharedOrgTenantId);

            userName = "sub-" + userName;
            userStoreManager.addUser(userName, generatePassword(), null, userClaims, DEFAULT_PROFILE);
            String sharedUserId = userStoreManager.getUserIDFromUserName(userName);
            organizationUserSharingDAO.createOrganizationUserAssociation(realUserId, userResidentOrgId, sharedUserId,
                    sharedOrgId);
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATE_SHARED_USER, e, sharedOrgId);
        }
    }

    @Override
    public boolean unShareOrganizationUsers(String realUserId, String userResidentOrgId)
            throws OrganizationManagementException {

        List<SharedUserAssociation> sharedUserAssociationList =
                organizationUserSharingDAO.getOrganizationUserAssociationsOfUser(realUserId, userResidentOrgId);
        // Removing the shared users from the shared organizations.
        for (SharedUserAssociation sharedUserAssociation : sharedUserAssociationList) {
            String sharedOrganizationId = sharedUserAssociation.getSharedOrganizationId();
            String tenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            try {
                AbstractUserStoreManager sharedOrgUserStoreManager = getAbstractUserStoreManager(tenantId);
                sharedOrgUserStoreManager.deleteUserWithID(sharedUserAssociation.getSharedUserId());
            } catch (UserStoreException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETE_SHARED_USER, e,
                        sharedUserAssociation.getSharedUserId(), sharedOrganizationId);
            }
        }
        return true;
    }

    @Override
    public boolean deleteOrganizationUserAssociationOfSharedUser(String sharedUserId, String userResidentOrgId)
            throws OrganizationManagementException {

        return organizationUserSharingDAO.deleteOrganizationUserAssociationOfSharedUser(sharedUserId,
                userResidentOrgId);
    }

    @Override
    public SharedUserAssociation getSharedUserAssociationOfUser(String realUserId, String sharedOrganizationId)
            throws OrganizationManagementException {

        return organizationUserSharingDAO.getOrganizationUserAssociation(realUserId, sharedOrganizationId);
    }

    @Override
    public SharedUserAssociation getSharedUserAssociationOfSharedUser(String sharedUserId, String sharedOrganizationId)
            throws OrganizationManagementException {

        return organizationUserSharingDAO.getSharedUserAssociationOfSharedUser(sharedUserId, sharedOrganizationId);
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
