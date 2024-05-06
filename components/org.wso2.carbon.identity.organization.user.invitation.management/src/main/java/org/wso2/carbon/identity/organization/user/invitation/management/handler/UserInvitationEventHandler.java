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

package org.wso2.carbon.identity.organization.user.invitation.management.handler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.bean.IdentityEventMessageContext;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.user.invitation.management.internal.UserInvitationMgtDataHolder;

import java.util.HashMap;

import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_NAME_POST_ADD_INVITATION;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_CONFIRMATION_CODE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_EMAIL_ADDRESS;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_REDIRECT_URL;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_SEND_TO;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.EVENT_PROP_TEMPLATE_TYPE;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.INVITATION_EVENT_HANDLER_ENABLED;
import static org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants.ORGANIZATION_USER_INVITATION_EMAIL_TEMPLATE_TYPE;

/**
 * Handles the events related to organization user invitations.
 */
public class UserInvitationEventHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(UserInvitationEventHandler.class);

    public String getName() {

        return "UserInvitationEventHandler";
    }

    public int getPriority(MessageContext messageContext) {

        return 100;
    }

    public boolean canHandle(MessageContext messageContext) throws IdentityRuntimeException {

        if (!isUserInvitationEventHandlerEnabled()) {
            LOG.info("[INVITATION_LOG] Organization User Invitation Event Handler is not enabled");
            return false;
        }
        String eventName = ((IdentityEventMessageContext) messageContext).getEvent().getEventName();
        if (EVENT_NAME_POST_ADD_INVITATION.equals(eventName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("canHandle() returning True for the event: " + eventName);
            }
            LOG.info("[INVITATION_LOG] Can handle the invitation event: " + eventName);
            return true;
        }
        return false;
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        LOG.info("[INVITATION_LOG] Handling event: " + eventName);
        if (EVENT_NAME_POST_ADD_INVITATION.equals(eventName)) {
            // Trigger the email notification
            String redirectUrl = event.getEventProperties().get(EVENT_PROP_REDIRECT_URL) + "?code=" +
                    event.getEventProperties().get(EVENT_PROP_CONFIRMATION_CODE);
            event.getEventProperties().put(EVENT_PROP_REDIRECT_URL, redirectUrl);
            LOG.info("[INVITATION_LOG] Triggering email notification for the event: " + eventName);
            triggerEmailNotification(event);
        }
    }

    private boolean isUserInvitationEventHandlerEnabled() {

        if (this.configs == null) {
            LOG.info("[INVITATION_LOG] Module configurations are not available. " +
                    "Hence returning false.");
            return false;
        }

        String handlerEnabled = this.configs.getModuleProperties()
                .getProperty(INVITATION_EVENT_HANDLER_ENABLED);
        LOG.info("[INVITATION_LOG] Is User Invitation Event Handler enabled: " +
                Boolean.parseBoolean(handlerEnabled));
        return Boolean.parseBoolean(handlerEnabled);
    }

    private void triggerEmailNotification(Event event) throws IdentityEventException {

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(EVENT_PROP_SEND_TO, event.getEventProperties().get(EVENT_PROP_EMAIL_ADDRESS));
        properties.put(EVENT_PROP_TEMPLATE_TYPE, ORGANIZATION_USER_INVITATION_EMAIL_TEMPLATE_TYPE);
        LOG.info("[INVITATION_LOG] Invite event properties: " + event.getEventProperties());
        if (CollectionUtils.size(event.getEventProperties()) > 0) {
            properties.putAll(event.getEventProperties());
        }
        Event identityMgtEvent = new Event(IdentityEventConstants.Event.TRIGGER_NOTIFICATION, properties);
        try {
            LOG.info("[INVITATION_LOG] Triggering the notification for invite event : " + event.getEventProperties());
            UserInvitationMgtDataHolder.getInstance().getIdentityEventService().handleEvent(identityMgtEvent);
        } catch (IdentityEventException e) {
            throw new IdentityEventException("Error while sending notification for user", e);
        }
    }
}
