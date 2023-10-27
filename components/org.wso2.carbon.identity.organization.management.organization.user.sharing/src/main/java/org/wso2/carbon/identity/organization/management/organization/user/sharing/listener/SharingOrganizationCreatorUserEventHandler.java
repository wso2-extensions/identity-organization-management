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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingServiceImpl;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Collections;
import java.util.Map;

/**
 * The event handler for sharing the organization creator to the child organization.
 */
public class SharingOrganizationCreatorUserEventHandler extends AbstractEventHandler {

    private final OrganizationUserSharingService userSharingService = new OrganizationUserSharingServiceImpl();

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();

        if ("POST_SHARED_CONSOLE_APP".equals(eventName)) {
            Map<String, Object> eventProperties = event.getEventProperties();
            String orgId = (String) eventProperties.get("ORGANIZATION_ID");

            try {
                String tenantDomain = OrganizationUserSharingDataHolder.getInstance().getOrganizationManager()
                        .resolveTenantDomain(orgId);
                if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                    return;
                }

                String associatedUserId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
                String associatedOrgId = (String) IdentityUtil.threadLocalProperties.get().get("USER_RESIDENT_ORG");
                if (StringUtils.isEmpty(associatedOrgId)) {
                    associatedOrgId = getOrganizationManager().resolveOrganizationId(Utils.getTenantDomain());
                }
                try {
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain, true);
                    userSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId);
                    String userId = userSharingService.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId)
                            .getUserId();
                    assignUserToAdminRole(userId, orgId, tenantDomain);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            } catch (OrganizationManagementException e) {
                throw new IdentityEventException("An error occurred while sharing the organization creator to the " +
                        "organization : " + orgId, e);
            }
        }
    }

    private void assignUserToAdminRole(String userId, String organizationId, String tenantDomain)
            throws IdentityEventException {

        String adminRoleName;
        try {
            adminRoleName = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration()
                    .getAdminRoleName();
            adminRoleName = UserCoreUtil.removeDomainFromName(adminRoleName);
        } catch (UserStoreException e) {
            throw new IdentityEventException("An error occurred while retrieving the admin role ", e);
        }

        try {
            String adminRoleId = OrganizationUserSharingDataHolder.getInstance().getRoleManagementService()
                    .getRoleIdByName(adminRoleName, RoleConstants.ORGANIZATION, organizationId, tenantDomain);
            OrganizationUserSharingDataHolder.getInstance().getRoleManagementService()
                    .updateUserListOfRole(adminRoleId, Collections.singletonList(userId), Collections.emptyList(),
                            tenantDomain);
        } catch (IdentityRoleManagementException e) {
            throw new IdentityEventException("An error occurred while assigning the user to the administrator role", e);
        }
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationManager();
    }
}
