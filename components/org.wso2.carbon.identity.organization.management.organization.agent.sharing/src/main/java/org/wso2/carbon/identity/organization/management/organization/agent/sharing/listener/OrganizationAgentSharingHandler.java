/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.listener;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal.OrganizationAgentSharingDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Map;

/**
 * The event handler for cleaning up agent sharing associations when an organization is deleted.
 */
public class OrganizationAgentSharingHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationAgentSharingHandler.class);

    /**
     * Handles cleanup of agent associations for deleted organizations.
     *
     * @param event The event to be handled.
     * @throws IdentityEventException If an error occurs while handling the event.
     */
    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        if (!Constants.EVENT_POST_DELETE_ORGANIZATION.equals(eventName)) {
            return;
        }

        Map<String, Object> eventProperties = event.getEventProperties();
        String deletedOrgId = (String) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID);
        try {
            if (StringUtils.isNotBlank(deletedOrgId)) {
                cleanupAgentAssociations(deletedOrgId);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("Error while cleaning up agent associations for deleted " +
                    "organization: " + deletedOrgId, e);
        }
    }

    /**
     * Returns the priority of this handler, defaulting to 16 when the super implementation returns -1.
     *
     * @param messageContext the message context.
     * @return the handler priority.
     */
    @Override
    public int getPriority(MessageContext messageContext) {

        int priority = super.getPriority(messageContext);
        if (priority == -1) {
            priority = 16;
        }
        return priority;
    }

    private void cleanupAgentAssociations(String deletedOrgId) throws OrganizationManagementException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Cleaning up agent associations for deleted organization: " + deletedOrgId);
        }
        OrganizationAgentSharingDataHolder.getInstance().getOrganizationAgentSharingService()
                .deleteAgentAssociationsByOrganizationId(deletedOrgId);
    }
}
