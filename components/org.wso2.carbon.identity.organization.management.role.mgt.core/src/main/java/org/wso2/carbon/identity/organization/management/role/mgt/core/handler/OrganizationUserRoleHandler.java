/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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
package org.wso2.carbon.identity.organization.management.role.mgt.core.handler;


import org.apache.commons.logging.Log;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.core.bean.context.MessageContext;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.OrganizationUserRoleMappingForEvent;
import org.wso2.carbon.identity.organization.management.role.mgt.core.models.UserForUserRoleMapping;

import java.util.Map;

import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.DATA;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.POST_ASSIGN_ORGANIZATION_USER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.POST_REVOKE_ORGANIZATION_USER_ROLE;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.STATUS;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.Status;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleEventConstants.USER_NAME;

/**
 * Handler class for Organization-User-Role mapping.
 */
public class OrganizationUserRoleHandler extends AbstractEventHandler {

    private static final Log AUDIT = CarbonConstants.AUDIT_LOG;
    private static final String AUDIT_MESSAGE =
            "Initiator : %s | Action : %s | Target : %s | Data : { %s } | Result : %s ";

    @Override
    public String getName() {

        return "OrganizationUserRoleHandler";
    }

    @Override
    public int getPriority(MessageContext messageContext) {

        return 51;
    }

    @Override
    public void handleEvent(Event event) {

        //common data
        Map<String, Object> eventProperties = event.getEventProperties();
        String status = eventProperties.get(STATUS) instanceof Status ?
                ((Status) eventProperties.get(STATUS)).getStatus() : null;
        String username = eventProperties.get(USER_NAME) instanceof String ?
                eventProperties.get(USER_NAME).toString() : null;
        String organizationId = eventProperties.get(ORGANIZATION_ID) instanceof String ?
                eventProperties.get(ORGANIZATION_ID).toString() : null;
        Object data = eventProperties.get(DATA);

        switch (event.getEventName()) {
            case POST_ASSIGN_ORGANIZATION_USER_ROLE:
                AUDIT.warn(String.format(AUDIT_MESSAGE, username, "assign organization user roles", organizationId,
                        formatRoleMappingAssignmentData(data), status));
                break;
            case POST_REVOKE_ORGANIZATION_USER_ROLE:
                AUDIT.warn(String.format(AUDIT_MESSAGE, username, "revoke organization user roles", organizationId,
                        formatRoleMappingRevokeData(data), status));
                break;
            default:
                break;
        }
    }

    private String formatRoleMappingRevokeData(Object data) {

        OrganizationUserRoleMappingForEvent organizationUserRoleMappingForRevokeEvent =
                data instanceof OrganizationUserRoleMappingForEvent ? (OrganizationUserRoleMappingForEvent) data :
                        new OrganizationUserRoleMappingForEvent();
        return "OrganizationId : " + organizationUserRoleMappingForRevokeEvent.getOrganizationId() +
                ", RoleId : " + organizationUserRoleMappingForRevokeEvent.getRoleId() +
                ", UserId : " + organizationUserRoleMappingForRevokeEvent.getUserId();
    }

    private String formatRoleMappingAssignmentData(Object data) {

        OrganizationUserRoleMappingForEvent organizationUserRoleMappingForRevokeEvent =
                data instanceof OrganizationUserRoleMappingForEvent ? (OrganizationUserRoleMappingForEvent) data :
                        new OrganizationUserRoleMappingForEvent();
        String builder = "OrganizationId : " + organizationUserRoleMappingForRevokeEvent.getOrganizationId() +
                ", RoleId : " + organizationUserRoleMappingForRevokeEvent.getRoleId();
        for (UserForUserRoleMapping userForUserRoleMapping : organizationUserRoleMappingForRevokeEvent
                .getUsersRoleInheritance()) {
            builder = builder.concat(", { UserId : " + userForUserRoleMapping.getUserId() +
                    ", hasMandatoryPrivilege : " + userForUserRoleMapping.hasMandatoryPrivilege() +
                    ", hasIncludeSubOrgsPrivilege: " + userForUserRoleMapping.hasIncludeSubOrgsPrivilege() + " }");
        }
        return builder;
    }
}
