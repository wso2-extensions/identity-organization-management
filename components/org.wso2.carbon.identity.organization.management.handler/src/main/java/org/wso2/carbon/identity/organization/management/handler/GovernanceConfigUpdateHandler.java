/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.service.util.Utils.isSubOrganization;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ConnectorConfig.EXPIRY_TIME;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ConnectorConfig.NOTIFICATION_BASED_PW_RECOVERY;
import static org.wso2.carbon.identity.recovery.IdentityRecoveryConstants.ConnectorConfig.NOTIFICATION_SEND_RECOVERY_NOTIFICATION_SUCCESS;

/**
 * Organization creation handler will be used to enable password recovery capability for newly created organizations.
 */
public class GovernanceConfigUpdateHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(GovernanceConfigUpdateHandler.class);

    private static final String EXPIRY_TIME_VALUE = "1440";
    private static final String NOTIFICATION_SEND_RECOVERY_NOTIFICATION_SUCCESS_VALUE = String.valueOf(true);
    private static final String NOTIFICATION_BASED_PW_RECOVERY_VALUE = String.valueOf(true);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Map<String, Object> eventProperties = event.getEventProperties();
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            int organizationDepthInHierarchy;
            try {
                organizationDepthInHierarchy =
                        getOrganizationManager().getOrganizationDepthInHierarchy(organization.getId());
            } catch (OrganizationManagementServerException e) {
                throw new IdentityEventException(
                        String.format("An error occurred while getting depth of the organization with id: %s",
                                organization.getId()), e);
            }
            if (isSubOrganization(organizationDepthInHierarchy)) {
                updateGovernanceConnectorProperty(organization);
            }
        }
    }

    private void updateGovernanceConnectorProperty(Organization organization)
            throws IdentityEventException {

        try {
            IdentityGovernanceService identityGovernanceService = OrganizationManagementHandlerDataHolder.getInstance()
                    .getIdentityGovernanceService();
            String tenantDomain = getOrganizationManager().resolveTenantDomain(organization.getId());
            Map<String, String> configurationDetails = new HashMap<>();
            configurationDetails.put(EXPIRY_TIME, EXPIRY_TIME_VALUE);
            configurationDetails.put(NOTIFICATION_SEND_RECOVERY_NOTIFICATION_SUCCESS,
                    NOTIFICATION_SEND_RECOVERY_NOTIFICATION_SUCCESS_VALUE);
            configurationDetails.put(NOTIFICATION_BASED_PW_RECOVERY, NOTIFICATION_BASED_PW_RECOVERY_VALUE);
            identityGovernanceService.updateConfiguration(tenantDomain, configurationDetails);
        } catch (IdentityGovernanceException | OrganizationManagementException e) {
            throw new IdentityEventException(
                    String.format("An error occurred while enabling password recovery for organization with id: %s",
                            organization.getId()), e);
        }
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    }
}

