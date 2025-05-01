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

package org.wso2.carbon.identity.organization.management.ext.listener;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.utils.AuditLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_STATUS;

/**
 * Action management V2 audit logger class.
 */
public class OrganizationManagementAuditLogHandler extends AbstractEventHandler {

    private static final OrganizationManagementAuditLogHandler INSTANCE = new OrganizationManagementAuditLogHandler();

    private static final String ID = "Id";
    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";
    private static final String STATUS = "Status";
    private static final String TYPE = "Type";
    private static final String PARENT_ORG_ID = "ParentOrganizationId";
    private static final String LAST_MODIFIED_TIME = "LastModifiedTime";
    private static final String CREATED_TIME = "CreatedTime";
    private static final String ATTRIBUTES = "Attributes";

    private static final String PATCH_ADDED = "Added";
    private static final String PATCH_REPLACED = "Replaced";
    private static final String PATCH_REMOVED = "Removed";

    private OrganizationManagementAuditLogHandler() {

    }

    public static OrganizationManagementAuditLogHandler getInstance() {

        return INSTANCE;
    }

    /**
     * Enum for Organization Management Log Actions.
     */
    private enum Action {

        ADD_ORGANIZATION("add-organization"),
        UPDATE_ORGANIZATION("update-organization"),
        DELETE_ORGANIZATION("delete-organization");

        private final String logAction;

        Action(String logAction) {

            this.logAction = logAction;
        }

        private String value() {

            return logAction;
        }
    }

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();

        switch (eventName) {
            case Constants.EVENT_POST_ADD_ORGANIZATION:
                logAddOrganization(eventProperties);
                break;
            case Constants.EVENT_POST_UPDATE_ORGANIZATION:
                logUpdateOrganization(eventProperties);
                break;
            case Constants.EVENT_POST_PATCH_ORGANIZATION:
                logPatchOrganization(eventProperties);
                break;
            case Constants.EVENT_POST_DELETE_ORGANIZATION:
                logDeleteOrganization(eventProperties);
                break;
            default:
                break;
        }
    }

    /**
     * Log add organization operation.
     *
     * @param eventProperties Event properties.
     */
    public void logAddOrganization(Map<String, Object> eventProperties) {

        Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
        triggerAuditLogEvent(organization.getId(), Action.ADD_ORGANIZATION, getDataMap(organization));
    }

    /**
     * Log update organization operation.
     *
     * @param eventProperties Event properties.
     */
    public void logUpdateOrganization(Map<String, Object> eventProperties) {

        Organization updatedOrganization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
        triggerAuditLogEvent(updatedOrganization.getId(), Action.UPDATE_ORGANIZATION, getDataMap(updatedOrganization));
    }

    /**
     * Log patch organization operation.
     *
     * @param eventProperties Event properties.
     */
    public void logPatchOrganization(Map<String, Object> eventProperties) {

        String organizationId = eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID).toString();
        List<PatchOperation> patchOperations =
                (List<PatchOperation>) eventProperties.get(Constants.EVENT_PROP_PATCH_OPERATIONS);
        triggerAuditLogEvent(organizationId, Action.UPDATE_ORGANIZATION, getDataMap(organizationId, patchOperations));
    }

    /**
     * Log delete organization operation.
     *
     * @param eventProperties Event properties.
     */
    public void logDeleteOrganization(Map<String, Object> eventProperties) {

        String organizationId = eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID).toString();
        Map<String, Object> data = new HashMap<>();
        data.put(ID, organizationId);
        triggerAuditLogEvent(organizationId, Action.DELETE_ORGANIZATION, data);
    }

    private Map<String, Object> getDataMap(Organization organization) {

        Map<String, Object> data = new HashMap<>();
        data.put(ID, organization.getId());
        data.put(NAME, organization.getName());
        data.put(DESCRIPTION, organization.getDescription());
        data.put(STATUS, organization.getStatus());
        data.put(TYPE, organization.getType());
        if (organization.getParent() != null) {
            data.put(PARENT_ORG_ID, organization.getParent().getId());
        }
        data.put(LAST_MODIFIED_TIME, organization.getLastModified().toString());
        data.put(CREATED_TIME, organization.getCreated().toString());
        data.put(ATTRIBUTES, getAttributesMap(organization));

        return data;
    }

    private static Map<String, Object> getAttributesMap(Organization organization) {

        if (organization.getAttributes() == null || organization.getAttributes().isEmpty()) {
            return null;
        }

        return organization.getAttributes().stream()
                .collect(Collectors.toMap(OrganizationAttribute::getKey,
                        attribute -> LoggerUtils.getMaskedContent(attribute.getValue())));
    }

    private Map<String, Object> getDataMap(String organizationId, List<PatchOperation> patchOperations) {

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> dataAdded = new HashMap<>();
        Map<String, Object> dataReplaced = new HashMap<>();
        List<String> dataRemoved = new ArrayList<>();

        for (PatchOperation patchOperation : patchOperations) {
            String fieldName = resolvePatchingField(patchOperation.getPath());
            String value = fieldName.startsWith(ATTRIBUTES)
                    ? LoggerUtils.getMaskedContent(patchOperation.getValue())
                    : patchOperation.getValue();
            switch (patchOperation.getOp()) {
                case PATCH_OP_ADD:
                    dataAdded.put(fieldName, value);
                    break;
                case PATCH_OP_REPLACE:
                    dataReplaced.put(fieldName, value);
                    break;
                case PATCH_OP_REMOVE:
                    dataRemoved.add(fieldName);
                    break;
                default:
                    break;
            }
        }

        data.put(ID, organizationId);
        if (!dataAdded.isEmpty()) {
            data.put(PATCH_ADDED, dataAdded);
        }
        if (!dataReplaced.isEmpty()) {
            data.put(PATCH_REPLACED, dataReplaced);
        }
        if (!dataRemoved.isEmpty()) {
            data.put(PATCH_REMOVED, dataRemoved);
        }

        return data;
    }

    private String resolvePatchingField(String path) {

        switch (path) {
            case PATCH_PATH_ORG_NAME:
                return NAME;
            case PATCH_PATH_ORG_DESCRIPTION:
                return DESCRIPTION;
            case  PATCH_PATH_ORG_STATUS:
                return STATUS;
            default:
                return ATTRIBUTES + "/" +  path;
        }
    }

    private void triggerAuditLogEvent(String targetId, Action action, Map<String, Object> dataMap) {

        String initiatorId = getInitiatorId();
        AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                initiatorId,
                LoggerUtils.getInitiatorType(initiatorId),
                targetId,
                LoggerUtils.Target.Organization.name(),
                action.value())
                .data(dataMap);
        LoggerUtils.triggerAuditLogEvent(auditLogBuilder);
    }

    private String getInitiatorId() {

        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (StringUtils.isBlank(username)) {
            return LoggerUtils.Initiator.System.name();
        }

        String initiator = null;
        if (StringUtils.isNotBlank(tenantDomain)) {
            initiator = IdentityUtil.getInitiatorId(username, tenantDomain);
        }

        return StringUtils.isNotBlank(initiator) ? initiator : LoggerUtils.getMaskedContent(username);
    }
}
