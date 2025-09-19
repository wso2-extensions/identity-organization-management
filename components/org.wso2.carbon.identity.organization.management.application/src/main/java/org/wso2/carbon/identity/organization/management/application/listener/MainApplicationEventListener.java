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

package org.wso2.carbon.identity.organization.management.application.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

/**
 * Listener for handling shared application events during application lifecycle.
 * This listener intercepts application deletion events to perform cleanup
 * operations for shared applications while skipping fragment applications.
 */
public class MainApplicationEventListener extends AbstractApplicationMgtListener {

    private static final Log LOG =
            LogFactory.getLog(MainApplicationEventListener.class);

    /**
     * Get the default order ID for this listener.
     *
     * @return The order ID.
     */
    @Override
    public int getDefaultOrderId() {

        return 50;
    }

    /**
     * Performs post-deletion cleanup for shared applications.
     * This method is called after an application is deleted to handle shared application cleanup operations.
     *
     * @param serviceProvider The deleted service provider.
     * @param tenantDomain    The tenant domain.
     * @param userName        The user who performed the deletion.
     * @return true if the operation completed successfully.
     * @throws IdentityApplicationManagementException If an error occurs
     *                                                during processing.
     */
    @Override
    public boolean doPostDeleteApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        boolean isFragmentApplication = isFragmentedApplication(serviceProvider.getSpProperties());

        if (isFragmentApplication) {
            // This is a fragment application. No need to do resource sharing cleanup.
            return true;
        }
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(IdentityEventConstants.EventProperty.APPLICATION_ID,
                serviceProvider.getApplicationResourceId());
        eventProperties.put(IdentityEventConstants.EventProperty.TENANT_DOMAIN,
                tenantDomain);
        Event event = createEvent(eventProperties,
                IdentityEventConstants.Event.POST_DELETE_MAIN_APPLICATION_WITH_ID);
        doPublishEvent(event);
        return true;
    }

    /**
     * Checks whether the application is a fragmented application by examining its service provider properties.
     * This method is used during application deletion to determine the fragment status from the service provider
     * properties, as this data is no longer accessible from the database after deletion.
     *
     * @param spProperties The service provider properties array. Can be null.
     * @return true if the application is a fragmented application, false otherwise.
     */
    private boolean isFragmentedApplication(ServiceProviderProperty[] spProperties) {

        if (spProperties == null) {
            return false;
        }

        return Arrays.stream(spProperties)
                .filter(Objects::nonNull)
                .anyMatch(property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                        Boolean.parseBoolean(property.getValue()));
    }

    private Event createEvent(Map<String, Object> eventProperties, String eventName) {

        return new Event(eventName, eventProperties);
    }

    private void doPublishEvent(Event event) throws IdentityApplicationManagementException {

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Event: " + event.getEventName() + " is published for the shared application management " +
                        "operation in the tenant with the tenant domain: "
                        + event.getEventProperties().get(IdentityEventConstants.EventProperty.TENANT_DOMAIN));
            }
            IdentityEventService eventService = OrgApplicationMgtDataHolder.getInstance().getIdentityEventService();
            eventService.handleEvent(event);
        } catch (IdentityEventException e) {
            throw new IdentityApplicationManagementException(e.getErrorCode(),
                    "Error while publishing the event: " + event.getEventName() + ".", e);
        }
    }
}
