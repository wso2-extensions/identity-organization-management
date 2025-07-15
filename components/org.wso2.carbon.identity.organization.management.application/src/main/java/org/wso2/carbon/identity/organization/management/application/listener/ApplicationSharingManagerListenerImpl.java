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

package org.wso2.carbon.identity.organization.management.application.listener;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.event.IdentityEventClientException;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareUpdateOperation;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;

/**
 * Listener implementation for organization's application sharing operations.
 * Class implements {@link ApplicationSharingManagerListener}.
 */
public class ApplicationSharingManagerListenerImpl implements ApplicationSharingManagerListener {

    @Override
    public void preShareApplication(String parentOrganizationId, String parentApplicationId,
                                    String sharedOrganizationId, boolean shareWithAllChildren)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARE_WITH_ALL_CHILDREN, shareWithAllChildren);
        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_SHARE_APPLICATION, eventProperties);
    }

    /**
     * Pre listener of sharing an application.
     * @param mainApplicationId This will be the main application id that resides in the root organization.
     * @param sharedOrganizationId This will be the sub-organization id which the application will be shared to.
     * @param applicationShareRolePolicy This will be the role sharing configuration (How the roles are shared).
     * @throws OrganizationManagementException When error occurred during pre application event firing.
     */
    @Override
    public void preShareApplication(String mainOrganizationId, String mainApplicationId,
                                     String sharedOrganizationId, ApplicationShareRolePolicy applicationShareRolePolicy)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID, mainOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID, mainApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_ROLE_SHARING_CONFIG, applicationShareRolePolicy);
        // Adding these two properties to keep the other places that use the event properties intact.
        // Ideally, we should remove these two properties from the event properties.
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, mainOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, mainApplicationId);

        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_SHARE_APPLICATION, eventProperties);
    }


    @Override
    public void postShareApplication(String parentOrganizationId, String parentApplicationId,
                                     String sharedOrganizationId, String sharedApplicationId,
                                     boolean shareWithAllChildren) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID, sharedApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARE_WITH_ALL_CHILDREN, shareWithAllChildren);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_USER_ATTRIBUTES,
                getSharedUserAttributes(sharedApplicationId));
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION, eventProperties);
    }

    @Override
    public void postShareApplication(String mainOrganizationId, String mainApplicationId, String sharedOrganizationId,
                                     String sharedApplicationId, ApplicationShareRolePolicy applicationShareRolePolicy)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID, mainOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID, mainApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID, sharedApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_ROLE_SHARING_CONFIG, applicationShareRolePolicy);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_USER_ATTRIBUTES,
                getSharedUserAttributes(sharedApplicationId));

        // Adding these two properties to keep the other places that use the event properties intact.
        // Ideally, we should remove these two properties from the event properties.
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, mainOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, mainApplicationId);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION, eventProperties);
    }

    @Override
    public void preUpdateRolesOfSharedApplication(String mainOrganizationId, String mainApplicationId,
                                                  String sharedOrganizationId,
                                                  ApplicationShareUpdateOperation.Operation operation,
                                                  List<RoleWithAudienceDO> roleChanges)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID, mainOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID, mainApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_ROLE_AUDIENCES, roleChanges);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_UPDATE_OPERATION, operation);
        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_UPDATE_ROLES_OF_SHARED_APPLICATION, eventProperties);
    }

    @Override
    public void postUpdateRolesOfSharedApplication(String mainOrganizationId, String mainApplicationId,
                                                   String sharedOrganizationId, String sharedApplicationId,
                                                   ApplicationShareUpdateOperation.Operation operation,
                                                   List<RoleWithAudienceDO> roleChanges)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_ORGANIZATION_ID, mainOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_MAIN_APPLICATION_ID, mainApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID, sharedApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_ROLE_AUDIENCES, roleChanges);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_UPDATE_OPERATION, operation);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_UPDATE_ROLES_OF_SHARED_APPLICATION, eventProperties);
    }

    @Override
    public void preDeleteSharedApplication(String parentOrganizationId, String parentApplicationId,
                                           String sharedOrganizationId) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_DELETE_SHARED_APPLICATION, eventProperties);
    }

    @Override
    public void postDeleteSharedApplication(String parentOrganizationId, String parentApplicationId,
                                            String sharedOrganizationId, String sharedApplicationId)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID, sharedApplicationId);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_DELETE_SHARED_APPLICATION, eventProperties);
    }

    @Override
    public void preDeleteAllSharedApplications(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_DELETE_ALL_SHARED_APPLICATIONS, eventProperties);
    }

    @Override
    public void postDeleteAllSharedApplications(String parentOrganizationId, String parentApplicationId,
                                                List<SharedApplicationDO> sharedApplicationDOList)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATIONS_DATA, sharedApplicationDOList);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_DELETE_ALL_SHARED_APPLICATIONS, eventProperties);
    }

    @Override
    public void preGetApplicationSharedOrganizations(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_GET_APPLICATION_SHARED_ORGANIZATIONS, eventProperties);
    }

    @Override
    public void postGetApplicationSharedOrganizations(String parentOrganizationId, String parentApplicationId,
                                                      List<BasicOrganization> sharedOrganizations)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATIONS, sharedOrganizations);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_GET_APPLICATION_SHARED_ORGANIZATIONS, eventProperties);
    }

    @Override
    public void preGetSharedApplications(String parentOrganizationId, String parentApplicationId)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        fireEvent(OrgApplicationMgtConstants.EVENT_PRE_GET_SHARED_APPLICATIONS, eventProperties);
    }

    @Override
    public void postGetSharedApplications(String parentOrganizationId, String parentApplicationId,
                                          List<SharedApplication> sharedApplications)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATIONS_DATA, sharedApplications);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_GET_SHARED_APPLICATIONS, eventProperties);
    }

    private void fireEvent(String eventName, Map<String, Object> eventProperties)
            throws OrganizationManagementException {

        IdentityEventService eventService = OrgApplicationMgtDataHolder.getInstance().getIdentityEventService();
        try {
            Event event = new Event(eventName, eventProperties);
            eventService.handleEvent(event);
        } catch (IdentityEventClientException e) {
            throw new OrganizationManagementClientException(e.getMessage(), e.getMessage(), e.getErrorCode(), e);
        } catch (IdentityEventException e) {
            throw new OrganizationManagementServerException(
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_FIRING_EVENTS.getMessage(),
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_FIRING_EVENTS.getCode(), e);
        }
    }

    private ClaimMapping[] getSharedUserAttributes(String sharedApplicationId) throws OrganizationManagementException {

        try {
            ServiceProvider application = OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService().
                    getApplicationByResourceId(sharedApplicationId, getTenantDomain());

            ClaimMapping[] filteredClaimMappings =
                    Arrays.stream(application.getClaimConfig().getClaimMappings())
                            .filter(claim -> !claim.getLocalClaim().getClaimUri()
                                    .startsWith(OrgApplicationMgtConstants.RUNTIME_CLAIM_URI_PREFIX)).
                            toArray(ClaimMapping[]::new);
            return filteredClaimMappings;
        } catch (IdentityApplicationManagementException e) {
            throw new OrganizationManagementException("An error occurred while getting the application.");
        }
    }
}
