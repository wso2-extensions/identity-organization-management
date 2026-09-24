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
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Flow executor that creates an organization from the details collected by a flow. The organization is
 * created under the organization that initiated the request, by the provisioned user or, when the flow
 * has not provisioned one, by that organization's administrator.
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

        // Input validation rejects a blank name earlier, so reaching here means the flow is misconfigured.
        if (StringUtils.isBlank(organizationName)) {
            return executorResponse(Constants.ExecutorStatus.STATUS_USER_ERROR,
                    "Please provide a valid organization name.");
        }

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
            // The submitted name or handle was rejected by organization management.
            return executorResponse(Constants.ExecutorStatus.STATUS_USER_ERROR, e.getMessage());
        } catch (OrganizationManagementException e) {
            // A server side failure. The internal message is not surfaced to the user.
            LOG.error("Failed to create organization: " + organizationName, e);
            return executorResponse(Constants.ExecutorStatus.STATUS_ERROR,
                    "Organization creation failed.");
        }
    }

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

        // Custom attributes, excluding the creator details set above.
        if (attributes != null) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                if (isCreatorAttribute(attribute.getKey())) {
                    continue;
                }
                organization.setAttribute(new OrganizationAttribute(attribute.getKey(), attribute.getValue()));
            }
        }

        organizationManager.addOrganization(organization);

        // The handle is the tenant domain of the new organization, used by later flow steps and rollback.
        flowOrganization.setOrganizationHandle(organizationHandle);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Organization created via onboarding flow. ID: " + organization.getId());
        }
    }

    private void setCreator(TenantTypeOrganization organization, FlowUser flowUser)
            throws OrganizationManagementException {

        if (StringUtils.isNotBlank(flowUser.getUserId())) {
            organization.setCreatorId(flowUser.getUserId());
            organization.setCreatorUsername(flowUser.getUsername());
            return;
        }

        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            UserRealm userRealm = OrganizationManagementExecutorDataHolder.getInstance().getRealmService()
                    .getTenantUserRealm(tenantId);
            RealmConfiguration realmConfiguration = userRealm.getRealmConfiguration();
            String adminUserName = realmConfiguration.getAdminUserName();
            String adminUserId = realmConfiguration.getAdminUserId();
            if (StringUtils.isBlank(adminUserId)) {
                adminUserId = ((AbstractUserStoreManager) userRealm.getUserStoreManager())
                        .getUserIDFromUserName(adminUserName);
            }
            organization.setCreatorId(adminUserId);
            organization.setCreatorUsername(adminUserName);
        } catch (UserStoreException e) {
            throw Utils.handleServerException(
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_VALIDATING_ORGANIZATION_OWNER,
                    e, organization.getId());
        }
    }

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

    /** Guards the creator set by this executor from being overridden by a flow collected attribute. */
    private static boolean isCreatorAttribute(String attributeKey) {

        return OrganizationManagementConstants.CREATOR_ID.equals(attributeKey)
                || OrganizationManagementConstants.CREATOR_USERNAME.equals(attributeKey)
                || OrganizationManagementConstants.CREATOR_EMAIL.equals(attributeKey);
    }

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

    /** Deletes the organization this executor created. Call only after {@link #execute} completed. */
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
            // A failed rollback must not replace the failure that caused it.
            LOG.error("Failed to roll back the organization created for handle: " + organizationHandle, e);
        }
        return null;
    }
}
