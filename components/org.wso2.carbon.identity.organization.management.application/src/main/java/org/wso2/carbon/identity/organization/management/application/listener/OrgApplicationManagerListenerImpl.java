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

import org.wso2.carbon.identity.event.IdentityEventClientException;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener implementation for organization's application sharing operations.
 * Class implements {@link OrgApplicationManagerListener}.
 */
public class OrgApplicationManagerListenerImpl implements OrgApplicationManagerListener {

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

    @Override
    public void postShareApplication(String parentOrganizationId, String parentApplicationId,
                                     String sharedOrganizationId, String sharedApplicationId,
                                     boolean shareWithAllChildren) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID, parentOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID, parentApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID, sharedOrganizationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARED_APPLICATION_ID, sharedApplicationId);
        eventProperties.put(OrgApplicationMgtConstants.EVENT_PROP_SHARE_WITH_ALL_CHILDREN, shareWithAllChildren);
        fireEvent(OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION, eventProperties);
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
}
