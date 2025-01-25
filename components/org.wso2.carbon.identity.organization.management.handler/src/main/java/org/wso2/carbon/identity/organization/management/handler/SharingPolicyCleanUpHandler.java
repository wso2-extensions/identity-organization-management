/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;

import java.util.Map;

import static org.wso2.carbon.identity.event.IdentityEventConstants.Event.POST_DELETE_ROLE_V2_EVENT;
import static org.wso2.carbon.identity.event.IdentityEventConstants.Event.POST_DELETE_USER;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.ROLE_ID;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.USER_ID;

/**
 * Event handler to clean up the stored sharing policies when associated resources are deleted.
 */
public class SharingPolicyCleanUpHandler extends AbstractEventHandler {

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        switch (eventName) {
            case POST_DELETE_ROLE_V2_EVENT:
                String deletedRoleId = (String) eventProperties.get(ROLE_ID);
                deleteSharedResourceAttributesByRoleId(deletedRoleId);
                break;
            case POST_DELETE_USER:
                String deletedUserId = (String) eventProperties.get(USER_ID);
                deleteResourceSharingPoliciesOfUser(deletedUserId);
                break;
            default:
                break;
        }
    }

    private void deleteResourceSharingPoliciesOfUser(String deletedUserId) throws IdentityEventException {

        try {
            getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(
                    ResourceType.USER, deletedUserId);
        } catch (ResourceSharingPolicyMgtException e) {
            throw new IdentityEventException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    private void deleteSharedResourceAttributesByRoleId(String deletedRoleId) throws IdentityEventException {

        try {
            getResourceSharingPolicyHandlerService().deleteSharedResourceAttributeByAttributeTypeAndId(
                    SharedAttributeType.ROLE, deletedRoleId);
        } catch (ResourceSharingPolicyMgtException e) {
            throw new IdentityEventException(e.getErrorCode(), e.getMessage(), e);
        }
    }

    private static ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationManagementHandlerDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }
}
