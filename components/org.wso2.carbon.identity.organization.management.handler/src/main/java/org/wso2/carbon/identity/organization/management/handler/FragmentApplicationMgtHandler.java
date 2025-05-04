/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.central.log.mgt.utils.LogConstants;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.utils.AuditLog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION_NAME_CONFLICT;

/**
 * This class is responsible for handling the events related to the organization application management.
 */
public class FragmentApplicationMgtHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(FragmentApplicationMgtHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();
        switch (eventName) {
            case OrgApplicationMgtConstants.EVENT_PRE_SHARE_APPLICATION:
                checkSharingAppConflicts(eventProperties);
                break;
            default:
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unsupported event: " + eventName);
                }
                break;
        }
    }

    private void checkSharingAppConflicts(Map<String, Object> eventProperties) throws IdentityEventException {

        String parentOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID);
        String parentApplicationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_APPLICATION_ID);
        String sharedOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);

        try {
            String sharedAppTenantDomain = OrganizationManagementHandlerDataHolder.getInstance().
                    getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
            if (OrganizationManagementUtil.isOrganization(sharedAppTenantDomain)) {
                String parentAppTenantDomain = OrganizationManagementHandlerDataHolder.getInstance().
                        getOrganizationManager().resolveTenantDomain(parentOrganizationId);
                ServiceProvider parentApp = OrganizationManagementHandlerDataHolder.getInstance().
                        getApplicationManagementService().getApplicationByResourceId(parentApplicationId,
                                parentAppTenantDomain);
                ServiceProvider orgApp = OrganizationManagementHandlerDataHolder.getInstance().
                        getApplicationManagementService().getServiceProvider(parentApp.getApplicationName(),
                                sharedAppTenantDomain);
                if (orgApp != null) {
                    boolean isFragmentApp = orgApp.getSpProperties() != null &&
                            Arrays.stream(orgApp.getSpProperties()).anyMatch(
                                    property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                                            Boolean.parseBoolean(property.getValue()));
                    if (!isFragmentApp) {
                        if (LoggerUtils.isEnableV2AuditLogs()) {
                            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                                    IdentityUtil.getInitiatorId(username, parentAppTenantDomain),
                                    LoggerUtils.Target.Application.name(),
                                    parentApp.getApplicationName(),
                                    LoggerUtils.Target.Application.name(),
                                    LogConstants.ApplicationManagement.CREATE_APPLICATION_ACTION)
                                    .data(buildAuditData(parentApp.getApplicationName(), sharedAppTenantDomain,
                                            orgApp.getApplicationName(), orgApp.getApplicationResourceId(),
                                            "Application conflict"));
                            LoggerUtils.triggerAuditLogEvent(auditLogBuilder, true);
                        }
                        LOG.warn(String.format("Organization %s has a non shared application with name %s.",
                                sharedOrganizationId, parentApp.getApplicationName()));
                        throw new IdentityApplicationManagementClientException(
                                ERROR_CODE_ERROR_SHARING_APPLICATION_NAME_CONFLICT.getCode(),
                                String.format(ERROR_CODE_ERROR_SHARING_APPLICATION_NAME_CONFLICT.getMessage(),
                                        parentApp.getApplicationName(), sharedOrganizationId));
                    }
                }
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException("Error occurred while resolving the tenant domain.", e);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityEventException("Error occurred while getting the application details.", e);
        }
    }

    private Map<String, String> buildAuditData(String mainApplicationName, String sharedTenantDomain,
                                               String conflictingOrgAppName, String conflictingOrgAppId,
                                               String failureReason) {

        Map<String, String> auditData = new HashMap<>();
        auditData.put("parentAppName", mainApplicationName);
        auditData.put("sharedTenantDomain", sharedTenantDomain);
        auditData.put("conflictingAppName", conflictingOrgAppName);
        auditData.put("conflictingAppId", conflictingOrgAppId);
        auditData.put("failureReason", failureReason);
        return auditData;
    }
}
