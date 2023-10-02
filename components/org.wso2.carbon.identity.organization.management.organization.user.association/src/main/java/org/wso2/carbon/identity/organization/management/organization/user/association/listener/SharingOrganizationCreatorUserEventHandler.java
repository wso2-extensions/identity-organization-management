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

package org.wso2.carbon.identity.organization.management.organization.user.association.listener;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.user.association.dao.OrganizationUserAssociationDAO;
import org.wso2.carbon.identity.organization.management.organization.user.association.dao.OrganizationUserAssociationDAOImpl;
import org.wso2.carbon.identity.organization.management.organization.user.association.internal.OrganizationUserAssociationDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.UserSharingConstants.CLAIM_MANAGED_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.UserSharingConstants.DEFAULT_PROFILE;
import static org.wso2.carbon.identity.organization.management.organization.user.association.constant.UserSharingConstants.ID_CLAIM_READ_ONLY;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.isSubOrganization;

/**
 * The event handler for sharing the organization creator to the child organization.
 */
public class SharingOrganizationCreatorUserEventHandler extends AbstractEventHandler {

    private final OrganizationUserAssociationDAO organizationUserAssociationDAO =
            new OrganizationUserAssociationDAOImpl();

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Map<String, Object> eventProperties = event.getEventProperties();
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            String organizationId = organization.getId();

            try {
                int organizationDepth = OrganizationUserAssociationDataHolder.getInstance().getOrganizationManager()
                        .getOrganizationDepthInHierarchy(organizationId);
                if (!isSubOrganization(organizationDepth)) {
                    return;
                }

                int tenantId = IdentityTenantUtil.getTenantId(organizationId);
                String userId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
                String userName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                //#TODO As same user exist with same name in child and parent, the existing user resolving from
                // ancestor logic fails. In order to avoid the "sub-" prefix was added. After userResidentOrganization
                // is available in authenticated user object, user resolving issue will be fixed.
                userName = "sub-" + userName;
                HashMap<String, String> userClaims = new HashMap<>();
                // #TODO Having super-org-id is wrong but till userResidentOrganization is available in the context,
                userClaims.put(CLAIM_MANAGED_ORGANIZATION, SUPER_ORG_ID);
                userClaims.put(ID_CLAIM_READ_ONLY, "true");
                UserCoreUtil.setSkipPasswordPatternValidationThreadLocal(true);
                AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(tenantId);
                userStoreManager.addUser(userName, generatePassword(), null, userClaims, DEFAULT_PROFILE);
                // Adding the association with the organization.
                String sharedUserId = userStoreManager.getUserIDFromUserName(userName);
                // #TODO SUPER_ORG_ID should be replaced by userResidentOrganization.
                organizationUserAssociationDAO.createOrganizationUserAssociation(userId, SUPER_ORG_ID, sharedUserId,
                        organizationId);
            } catch (UserStoreException | OrganizationManagementServerException e) {
                throw new IdentityEventException("An error occurred while sharing the organization creator to the " +
                        "organization : " + organizationId, e);
            }
        }
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = OrganizationUserAssociationDataHolder.getInstance().getRealmService();
        UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
        return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
    }

    private String generatePassword() {

        UUID uuid = UUID.randomUUID();
        return uuid.toString().substring(0, 12);
    }
}
