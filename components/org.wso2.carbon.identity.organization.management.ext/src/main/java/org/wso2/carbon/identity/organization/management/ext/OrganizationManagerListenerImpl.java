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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.context.IdentityContext;
import org.wso2.carbon.identity.core.context.model.Flow;
import org.wso2.carbon.identity.core.util.IdentityUtil;
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

    private static final Log LOG = LogFactory.getLog(OrganizationManagerListenerImpl.class);

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
        try {
            IdentityUtil.threadLocalProperties.get().put(Constants.IS_SUBSEQUENT_OPERATION_OF_ADD_ORGANIZATION, true);
            fireEvent(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(Constants.IS_SUBSEQUENT_OPERATION_OF_ADD_ORGANIZATION);
        }
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

    @Deprecated
    @Override
    public void postDeleteOrganization(String organizationId) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        fireEvent(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);
    }

    @Override
    public void postDeleteOrganization(String organizationId, int organizationDepthInHierarchy)
            throws OrganizationManagementException {

        postDeleteOrganization(organizationId, null, organizationDepthInHierarchy);
    }

    @Override
    public void postDeleteOrganization(String organizationId, Organization organization,
                                       int organizationDepthInHierarchy) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_DEPTH_IN_HIERARCHY, organizationDepthInHierarchy);
        if (organization != null) {
            eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        }
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
        fireOrganizationStatusChangeEvent(organizationId, null, resolvePatchedStatus(patchOperations));
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

        postUpdateOrganization(organizationId, organization, null);
    }

    @Override
    public void postUpdateOrganization(String organizationId, Organization organization,
                                       Organization previousOrganization) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        if (previousOrganization != null) {
            eventProperties.put(Constants.EVENT_PROP_PREVIOUS_ORGANIZATION, previousOrganization);
        }
        fireEvent(Constants.EVENT_POST_UPDATE_ORGANIZATION, eventProperties);
        fireOrganizationStatusChangeEvent(organizationId, organization,
                resolveReplacedStatus(organization, previousOrganization));
    }

    /**
     * Fire the organization activated or the organization disabled event, depending on the status the update left
     * the organization with. The event is fired within a sub flow of the flow the request is served under, so that
     * it is published with its own action rather than with the action of the update that carried the status change.
     *
     * @param organizationId    ID of the updated organization.
     * @param organization      The updated organization, or null when the update did not carry it.
     * @param status            The status the organization holds after the update, or null when it did not change.
     * @throws OrganizationManagementException If an error occurs while firing the event.
     */
    private void fireOrganizationStatusChangeEvent(String organizationId, Organization organization, String status)
            throws OrganizationManagementException {

        if (StringUtils.isEmpty(status)) {
            return;
        }
        String eventName;
        Flow.Name flowName;
        if (OrganizationManagementConstants.OrganizationStatus.ACTIVE.name().equals(status)) {
            eventName = Constants.EVENT_POST_ACTIVATE_ORGANIZATION;
            flowName = Flow.Name.ORGANIZATION_ACTIVATE;
        } else if (OrganizationManagementConstants.OrganizationStatus.DISABLED.name().equals(status)) {
            eventName = Constants.EVENT_POST_DISABLE_ORGANIZATION;
            flowName = Flow.Name.ORGANIZATION_DISABLE;
        } else {
            LOG.debug("Skipping the status change event of the organization: " + organizationId +
                    ", since the status is not a supported organization status: " + status);
            return;
        }

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);
        if (organization != null) {
            eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        }

        boolean hasEnteredStatusChangeFlow = enterSubFlow(flowName);
        try {
            fireEvent(eventName, eventProperties);
        } finally {
            if (hasEnteredStatusChangeFlow) {
                IdentityContext.getThreadLocalIdentityContext().exitFlow();
            }
        }
    }

    /**
     * Resolve the status a replacement left the organization with, when the replacement changed it. The replacement
     * carries the complete organization, so the status it changed can only be resolved against the state the
     * organization held before the update.
     *
     * @param organization          The organization carried by the update.
     * @param previousOrganization  The organization as it was before the update, or null when it is not available.
     * @return The status the organization holds after the update, or null when the update did not change it.
     */
    private String resolveReplacedStatus(Organization organization, Organization previousOrganization) {

        if (organization == null || previousOrganization == null) {
            return null;
        }
        String status = StringUtils.trimToNull(organization.getStatus());
        return (status != null && !status.equals(previousOrganization.getStatus())) ? status : null;
    }

    /**
     * Resolve the status a patch left the organization with, when the patch changed it. A removal of the status is
     * rejected by the organization management component, so the value carried by the operation is the new status.
     *
     * @param patchOperations Patch operations applied to the organization.
     * @return The status the organization holds after the patch, or null when the patch did not change it.
     */
    private String resolvePatchedStatus(List<PatchOperation> patchOperations) {

        if (patchOperations == null) {
            return null;
        }
        for (PatchOperation patchOperation : patchOperations) {
            if (patchOperation == null || !OrganizationManagementConstants.PATCH_PATH_ORG_STATUS
                    .equals(StringUtils.trimToEmpty(patchOperation.getPath()))) {
                continue;
            }
            return StringUtils.trimToNull(patchOperation.getValue());
        }
        return null;
    }

    /**
     * Enter a sub flow of the flow the request is served under. The initial flow is entered by the layer serving the
     * request, so a sub flow is entered only when a flow is already present, and it inherits its initiating persona.
     *
     * @param flowName Name of the sub flow to enter.
     * @return true when the sub flow was entered, false otherwise.
     */
    private boolean enterSubFlow(Flow.Name flowName) {

        Flow existingFlow = IdentityContext.getThreadLocalIdentityContext().getCurrentFlow();
        if (existingFlow == null) {
            LOG.debug("No existing flow found in the identity context. Cannot enter the sub flow: " + flowName);
            return false;
        }
        Flow flow = new Flow.Builder()
                .name(flowName)
                .initiatingPersona(existingFlow.getInitiatingPersona())
                .build();
        IdentityContext.getThreadLocalIdentityContext().enterFlow(flow);
        return true;
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
