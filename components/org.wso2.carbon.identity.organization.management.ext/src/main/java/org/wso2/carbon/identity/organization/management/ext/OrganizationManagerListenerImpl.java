/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.ext;

import org.wso2.carbon.identity.event.IdentityEventClientException;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.ext.internal.OrganizationManagementExtDataHolder;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.listener.OrganizationManagerListener;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener implementation for organization management operations.
 */
public class OrganizationManagerListenerImpl implements OrganizationManagerListener {

    @Override
    public void preAddOrganization(Organization organization) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        fireEvent(Constants.EVENT_PRE_ADD_ORGANIZATION, eventProperties);
    }

    @Override
    public void postAddOrganization(Organization organization) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        fireEvent(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
    }

    @Override
    public void preGetOrganization(String organizationId) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        fireEvent(Constants.EVENT_PRE_GET_ORGANIZATION, eventProperties);
    }

    @Override
    public void postGetOrganization(String organizationId, Organization organization)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        fireEvent(Constants.EVENT_POST_GET_ORGANIZATION, eventProperties);
    }

    @Override
    public void preDeleteOrganization(String organizationId) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        fireEvent(Constants.EVENT_PRE_DELETE_ORGANIZATION, eventProperties);
    }

    @Override
    public void postDeleteOrganization(String organizationId) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        fireEvent(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);
    }

    @Override
    public void prePatchOrganization(String organizationId, List<PatchOperation> patchOperations)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_PATCH_OPERATIONS, patchOperations);
        fireEvent(Constants.EVENT_PRE_PATCH_ORGANIZATION, eventProperties);
    }

    @Override
    public void postPatchOrganization(String organizationId, List<PatchOperation> patchOperations)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_PATCH_OPERATIONS, patchOperations);
        fireEvent(Constants.EVENT_POST_PATCH_ORGANIZATION, eventProperties);
    }

    @Override
    public void preUpdateOrganization(String organizationId, Organization organization)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        fireEvent(Constants.EVENT_PRE_UPDATE_ORGANIZATION, eventProperties);
    }

    @Override
    public void postUpdateOrganization(String organizationId, Organization organization)
            throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        fireEvent(Constants.EVENT_POST_UPDATE_ORGANIZATION, eventProperties);
    }

    private void fireEvent(String eventName, Map<String, Object> eventProperties)
            throws OrganizationManagementException {

        IdentityEventService eventService = OrganizationManagementExtDataHolder.getInstance().getIdentityEventService();
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
