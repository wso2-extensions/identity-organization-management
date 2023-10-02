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

package org.wso2.carbon.identity.organization.management.organization.user.association;

import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.association.dao.OrganizationUserAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.user.association.dao.OrganizationUserAssociationDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.association.internal.OrganizationUserAssociationDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.association.models.SharedUserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.List;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_DELETE_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Implementation of the user sharing for organizations.
 */
public class OrganizationUserAssociationServiceImpl implements OrganizationUserAssociationService {

    private final OrganizationUserAssociationDAO organizationUserAssociationDAO =
            new OrganizationUserAssociationDAOImpl();

    @Override
    public void createOrganizationUserAssociation(String userId, String userOrganizationId, String sharedUserId,
                                                  String sharedOrgId) throws OrganizationManagementException {

        organizationUserAssociationDAO.createOrganizationUserAssociation(userId, userOrganizationId, sharedUserId,
                sharedOrgId);
    }

    @Override
    public boolean deleteOrganizationUserAssociationOfSharedUser(String sharedUserId, String userOrganizationId)
            throws OrganizationManagementException {

        return organizationUserAssociationDAO.deleteOrganizationUserAssociationOfSharedUser(sharedUserId,
                userOrganizationId);
    }

    public boolean deleteOrganizationUserAssociations(String userId, String userOrganizationId)
            throws OrganizationManagementException {

        List<SharedUserAssociation> sharedUserAssociationList =
                organizationUserAssociationDAO.getOrganizationUserAssociationsOfUser(userId, userOrganizationId);
        // Removing the shared users from the shared organizations.
        for (SharedUserAssociation sharedUserAssociation : sharedUserAssociationList) {
            String sharedOrganizationId = sharedUserAssociation.getSharedOrganizationId();
            String tenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            try {
                AbstractUserStoreManager sharedOrgUserStoreManager = getAbstractUserStoreManager(tenantId);
                sharedOrgUserStoreManager.deleteUserWithID(sharedUserAssociation.getSharedUserId());
            } catch (UserStoreException e) {
                throw handleServerException(ERROR_CODE_ERROR_DELETE_SHARED_USER, e, userId, sharedOrganizationId);
            }
        }
        return true;
    }

    @Override
    public SharedUserAssociation getSharedUserAssociationOfUserAtSubOrg(String userId, String sharedOrganizationId)
            throws OrganizationManagementException {

        return organizationUserAssociationDAO.getOrganizationUserAssociationOfUserAtSharedOrg(userId,
                sharedOrganizationId);
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = OrganizationUserAssociationDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserAssociationDataHolder.getInstance().getOrganizationManager();
    }
}
