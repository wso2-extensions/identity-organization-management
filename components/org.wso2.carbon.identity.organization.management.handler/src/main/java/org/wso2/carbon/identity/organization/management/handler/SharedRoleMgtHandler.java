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

package org.wso2.carbon.identity.organization.management.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event handler to manage shared roles in sub-organizations.
 */
public class SharedRoleMgtHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(SharedRoleMgtHandler.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        switch (eventName) {
            case IdentityEventConstants.Event.POST_ADD_ROLE_V2_EVENT:
                createOrganizationRolesOnNewRoleCreation(eventProperties);
                break;
            default:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unsupported event: " + eventName);
                }
                break;
        }
    }

    private void createOrganizationRolesOnNewRoleCreation(Map<String, Object> eventProperties)
            throws IdentityEventException {

        try {
            String mainRoleUUID = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_ID);
            String mainRoleName = (String) eventProperties.get(IdentityEventConstants.EventProperty.ROLE_NAME);
            String roleTenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            String roleAudienceType = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE);
            String roleAudienceId = (String) eventProperties.get(IdentityEventConstants.EventProperty.AUDIENCE_ID);
            String roleOrgId = getOrganizationManager().resolveOrganizationId(roleTenantDomain);
            if (OrganizationManagementUtil.isOrganization(roleTenantDomain)) {
                return;
            }
            switch (roleAudienceType) {
                case RoleConstants.APPLICATION:
                    /*
                     If the audienced application is a shared application, create the role in
                     the shared apps' org space.
                     */
                    List<SharedApplication> sharedApplications =
                            getOrgApplicationManager().getSharedApplications(roleOrgId, roleAudienceId);
                    int noOfSharedApps = sharedApplications.size();
                    for (int i = 0; i < noOfSharedApps; i++) {
                        final int taskId = i;
                        CompletableFuture.runAsync(() -> {
                            try {
                                String sharedApplicationId = sharedApplications.get(taskId).getSharedApplicationId();
                                String sharedOrganizationId = sharedApplications.get(taskId).getOrganizationId();
                                String shareAppTenantDomain =
                                        getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
                                RoleBasicInfo sharedRoleInfo =
                                        getRoleManagementServiceV2().addRole(mainRoleName, Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList(), RoleConstants.APPLICATION, sharedApplicationId,
                                                shareAppTenantDomain);
                                // Add relationship between main role and shared role.
                                getRoleManagementServiceV2().addMainRoleToSharedRoleRelationship(mainRoleUUID,
                                        sharedRoleInfo.getId(), roleTenantDomain, shareAppTenantDomain);
                            } catch (IdentityRoleManagementException | OrganizationManagementException e) {
                                LOG.error("Error occurred while creating shared role in organization with id: " +
                                        sharedApplications.get(taskId).getOrganizationId(), e);
                            }
                        }, executorService).exceptionally(throwable -> {
                            LOG.error(String.format(
                                    "Exception occurred during creating a shared role: %s in organization: %s",
                                    mainRoleName, sharedApplications.get(taskId).getOrganizationId()), throwable);
                            return null;
                        });
                    }
                    break;
                case RoleConstants.ORGANIZATION:
                    /*
                    Organization audience roles can't be attached to an application at the same time of role creation.
                    Organization audience role get shared to other application only if that role is associated with any
                    shared application in the organization. So nothing do in this case.
                     */
                    break;
                default:
                    LOG.error("Unsupported audience type: " + roleAudienceType);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("Error occurred while retrieving shared applications.", e);
        }
    }

    private static RoleManagementService getRoleManagementServiceV2() {

        return OrganizationManagementHandlerDataHolder.getInstance().getRoleManagementServiceV2();
    }

    private static OrganizationManager getOrganizationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    }

    private static OrgApplicationManager getOrgApplicationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrgApplicationManager();
    }
}
