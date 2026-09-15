/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.executor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowOrganization;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowUser;
import org.wso2.carbon.identity.organization.management.executor.internal.OrganizationManagementExecutorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.TenantTypeOrganization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Flow executor that creates an organization from the details collected by the flow.
 * <p>
 * It is registered as an OSGi {@link Executor} service by
 * {@code OrganizationManagementExecutorServiceComponent} and is bound to a flow step by the name
 * returned from {@link #getName()}. The organization name, handle and attributes are read from
 * {@link FlowOrganization}; the creating user is read from {@link FlowUser}. Fields are routed into
 * {@link FlowOrganization} by the flow engine based on the {@code identifierType} of each input, so this
 * executor never inspects raw user input itself.
 * <p>
 * This executor does not provision users. When the flow has already provisioned one, that user creates
 * the organization and becomes its owner; when it has not, the organization is created under the
 * administrator of the organization the flow is executing in, so a user can be provisioned inside it
 * afterwards.
 */
public class OrganizationProvisioningExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(OrganizationProvisioningExecutor.class);

    private static final String EXECUTOR_NAME = "OrganizationProvisioningExecutor";

    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        String organizationName = context.getFlowOrganization().getOrganizationName();

        // The organization name is user supplied and cannot be derived. Input validation rejects a
        // blank name before the flow reaches this executor, so reaching here means the caller bypassed
        // it, and this node has no page to send them back to.
        if (StringUtils.isBlank(organizationName)) {
            return executorResponse(Constants.ExecutorStatus.STATUS_USER_ERROR,
                    "Please provide a valid organization name.");
        }

        // The new organization is created under the organization that initiated the request, so a
        // sub-organization can onboard its own children. Creating it under an unintended parent is worse
        // than not creating it at all.
        String requestInitiatedOrgId;
        try {
            requestInitiatedOrgId = OrganizationManagementExecutorDataHolder.getInstance().getOrganizationManager()
                    .resolveOrganizationId(context.getTenantDomain());
        } catch (OrganizationManagementException e) {
            LOG.error("Could not resolve the organization the flow is executing in, so the parent of the "
                    + "new organization is unknown. Organization creation is aborted.", e);
            return executorResponse(Constants.ExecutorStatus.STATUS_ERROR,
                    "Could not resolve the parent organization.");
        }

        try {
            createOrganization(context, requestInitiatedOrgId);

            ExecutorResponse response = new ExecutorResponse();
            response.setResult(Constants.ExecutorStatus.STATUS_COMPLETE);
            return response;
        } catch (OrganizationManagementClientException e) {
            // The caller's input is at fault, most often a name taken since it was validated.
            return executorResponse(Constants.ExecutorStatus.STATUS_USER_ERROR, e.getMessage());
        } catch (OrganizationManagementException e) {
            // Retrying cannot resolve a server side failure, and its message is not for the end user.
            LOG.error("Failed to create organization: " + organizationName, e);
            return executorResponse(Constants.ExecutorStatus.STATUS_ERROR,
                    "Organization creation failed.");
        }
    }

    /**
     * Builds and persists the organization through the {@link OrganizationManager} bound into this
     * bundle's own data holder by {@code OrganizationManagementExecutorServiceComponent}.
     *
     * @param context              Flow execution context carrying the organization and user details.
     * @param parentOrganizationId ID of the organization the new organization is created under.
     * @throws OrganizationManagementException If the organization could not be created.
     */
    private void createOrganization(FlowExecutionContext context, String parentOrganizationId)
            throws OrganizationManagementException {

        OrganizationManager organizationManager =
                OrganizationManagementExecutorDataHolder.getInstance().getOrganizationManager();

        FlowUser flowUser = context.getFlowUser();
        FlowOrganization flowOrganization = context.getFlowOrganization();
        String organizationId = Utils.generateUniqueID();
        String organizationHandle = resolveOrganizationHandle(organizationManager, flowOrganization, organizationId);
        Map<String, String> attributes = flowOrganization.getAttributes();

        TenantTypeOrganization organization = new TenantTypeOrganization(organizationHandle);
        organization.setId(organizationId);
        organization.setName(flowOrganization.getOrganizationName());
        organization.setDescription(flowOrganization.getOrganizationDescription());
        organization.setStatus(OrganizationManagementConstants.OrganizationStatus.ACTIVE.toString());
        organization.setType(OrganizationManagementConstants.OrganizationTypes.TENANT.toString());
        organization.setCreated(Instant.now());
        organization.setLastModified(Instant.now());
        organization.getParent().setId(parentOrganizationId);
        organization.setVersion(OrganizationManagementConstants.OrganizationVersion.BASE_ORG_VERSION);
        organization.setOrganizationHandle(organizationHandle);
        setCreator(organization, flowUser);

        // Carry the custom organization attributes collected by the flow onto the organization.
        if (attributes != null) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                organization.setAttribute(new OrganizationAttribute(attribute.getKey(), attribute.getValue()));
            }
        }

        organizationManager.addOrganization(organization);

        // The handle is the new organization's tenant domain. Recording it lets a later step in the flow act
        // inside the organization, and lets a rollback find it.
        flowOrganization.setOrganizationHandle(organizationHandle);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Organization created via onboarding flow. ID: " + organization.getId());
        }
    }

    /**
     * Sets the user the organization is created under. The creator becomes the owner and the
     * administrator of the new organization's tenant, and organization management requires it to be an
     * existing user of the organization the flow is executing in.
     * <p>
     * When the flow has already provisioned a user, that user is the creator. When it has not, the
     * organization is created under the administrator of the organization the flow is executing in, so
     * that a user can be provisioned inside the new organization afterwards.
     *
     * @param organization Organization being created.
     * @param flowUser     User collected by the flow, which may not have been provisioned yet.
     * @throws OrganizationManagementException If the administrator of the current organization cannot
     *                                         be resolved.
     */
    private void setCreator(TenantTypeOrganization organization, FlowUser flowUser)
            throws OrganizationManagementException {

        if (StringUtils.isNotBlank(flowUser.getUserId())) {
            organization.setCreatorId(flowUser.getUserId());
            organization.setCreatorUsername(flowUser.getUsername());
            return;
        }

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            RealmConfiguration realmConfiguration = OrganizationManagementExecutorDataHolder.getInstance()
                    .getRealmService().getTenantUserRealm(tenantId).getRealmConfiguration();
            organization.setCreatorId(realmConfiguration.getAdminUserId());
            organization.setCreatorUsername(realmConfiguration.getAdminUserName());
        } catch (UserStoreException e) {
            throw Utils.handleServerException(
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_VALIDATING_ORGANIZATION_OWNER,
                    e, organization.getId());
        }
    }

    /**
     * Returns the handle to create the organization with. A handle submitted through the flow is used if it
     * is not taken; otherwise the organization ID is the handle, as in admin initiated organization creation.
     *
     * @param organizationManager Organization manager used to check handle availability.
     * @param flowOrganization    Organization details collected by the flow.
     * @param organizationId      ID of the organization being created.
     * @return The organization handle.
     * @throws OrganizationManagementException If the submitted handle is taken, or the check fails.
     */
    private String resolveOrganizationHandle(OrganizationManager organizationManager,
                                             FlowOrganization flowOrganization, String organizationId)
            throws OrganizationManagementException {

        String submittedHandle = flowOrganization.getOrganizationHandle();
        if (StringUtils.isBlank(submittedHandle)) {
            return organizationId;
        }
        String handle = submittedHandle.trim();
        if (organizationManager.isOrganizationExistByHandle(handle)) {
            throw Utils.handleClientException(
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_EXISTING_ORGANIZATION_HANDLE, handle);
        }
        return handle;
    }

    /**
     * Builds an executor response carrying a status and a user facing message.
     *
     * @param status  One of {@link Constants.ExecutorStatus}.
     * @param message Message surfaced to the caller.
     * @return The executor response.
     */
    private ExecutorResponse executorResponse(String status, String message) {

        ExecutorResponse response = new ExecutorResponse();
        response.setResult(status);
        response.setErrorMessage(message);
        return response;
    }

    @Override
    public List<String> getInitiationData() {

        return Collections.emptyList();
    }

    /**
     * Deletes the organization this executor created. Call only after {@link #execute} completed: a handle
     * submitted through the flow is present before creation, and can name an existing organization.
     */
    @Override
    public ExecutorResponse rollback(FlowExecutionContext context) {

        FlowOrganization flowOrganization = context.getFlowOrganization();
        if (flowOrganization == null) {
            return null;
        }

        String organizationHandle = flowOrganization.getOrganizationHandle();
        if (StringUtils.isBlank(organizationHandle)) {
            return null;
        }

        try {
            OrganizationManager organizationManager =
                    OrganizationManagementExecutorDataHolder.getInstance().getOrganizationManager();
            String organizationId = organizationManager.resolveOrganizationId(organizationHandle);
            if (StringUtils.isBlank(organizationId)
                    || !organizationManager.isOrganizationExistById(organizationId)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No organization found for handle: " + organizationHandle + ". Nothing to roll back.");
                }
                return null;
            }

            organizationManager.deleteOrganization(organizationId);
            // Clearing the handle keeps a second rollback a no-op rather than a second delete attempt.
            flowOrganization.setOrganizationHandle(null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Rolled back the organization created via onboarding flow. ID: " + organizationId);
            }
        } catch (OrganizationManagementException e) {
            // A failed rollback must not replace the failure that caused it, so it is logged and the
            // caller reports its own outcome.
            LOG.error("Failed to roll back the organization created for handle: " + organizationHandle, e);
        }
        return null;
    }
}
