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
import org.wso2.carbon.identity.flow.execution.engine.Constants;
import org.wso2.carbon.identity.flow.execution.engine.exception.FlowEngineException;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowOrganization;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowUser;
import org.wso2.carbon.identity.organization.management.executor.internal.OrganizationManagementExecutorDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.TenantTypeOrganization;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
 * The user is expected to have been provisioned by an earlier step in the flow. This executor does not
 * provision users itself: if it runs without a resolved user ID the flow is misconfigured, and execution
 * fails rather than silently creating an ownerless organization.
 */
public class OrganizationProvisioningExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(OrganizationProvisioningExecutor.class);

    private static final String EXECUTOR_NAME = "OrganizationProvisioningExecutor";

    private static final String CTX_ORGANIZATION_ID = "org.created.id";
    private static final String CTX_ORGANIZATION_NAME = "org.created.name";

    /**
     * Handle rules mirrored from the organization handle field in the console and the flow UI, which
     * derive a handle from an organization name by lowercasing it and dropping every character outside
     * {@code [a-z0-9]}. The handle becomes the tenant domain of the new organization, so a handle
     * derived here must match what the UI would have produced for the same name.
     */
    private static final String HANDLE_SANITIZATION_REGEX = "^[^a-z]*|[^a-z0-9]";
    private static final int MIN_HANDLE_LENGTH = 4;
    private static final int MAX_HANDLE_LENGTH = 30;

    /** Appended to a handle derived from a name too short to meet the minimum handle length. */
    private static final char HANDLE_PADDING_CHARACTER = '0';

    /** Numeric suffixes tried on a derived handle collision before falling back to a random token. */
    private static final int MAX_HANDLE_COLLISION_ATTEMPTS = 5;

    /** Length of the random token appended when every numeric suffix is taken. */
    private static final int RANDOM_HANDLE_SUFFIX_LENGTH = 6;

    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) throws FlowEngineException {

        String organizationName = context.getFlowOrganization().getOrganizationName();

        // The organization name is user supplied and cannot be derived, so ask for it again.
        if (StringUtils.isBlank(organizationName)) {
            return executorResponse(Constants.ExecutorStatus.STATUS_RETRY,
                    "Please provide a valid organization name");
        }

        // A user provisioning step must run before this executor. Reaching here without a user ID means
        // the flow is wired incorrectly, which the end user cannot resolve by retrying.
        if (StringUtils.isBlank(context.getFlowUser().getUserId())) {
            LOG.error("Organization provisioning executor invoked without a provisioned user. A user "
                    + "provisioning step must be configured before this executor in the flow.");
            return executorResponse(Constants.ExecutorStatus.STATUS_ERROR,
                    "Organization creation requires a provisioned user.");
        }

        // Creating the organization under an unintended parent is worse than not creating it at all.
        String parentOrganizationId = resolveParentOrganizationId();
        if (StringUtils.isBlank(parentOrganizationId)) {
            LOG.error("Could not resolve the organization the flow is executing in, so the parent of the "
                    + "new organization is unknown. Organization creation is aborted.");
            return executorResponse(Constants.ExecutorStatus.STATUS_ERROR,
                    "Could not resolve the parent organization.");
        }

        try {
            String createdOrganizationId = createOrganization(context, parentOrganizationId);

            ExecutorResponse response = new ExecutorResponse();
            response.setResult(Constants.ExecutorStatus.STATUS_COMPLETE);

            Map<String, Object> properties = new HashMap<>();
            properties.put(CTX_ORGANIZATION_ID, createdOrganizationId);
            properties.put(CTX_ORGANIZATION_NAME, organizationName);
            response.setContextProperty(properties);

            return response;
        } catch (OrganizationManagementException e) {
            LOG.error("Failed to create organization: " + organizationName, e);
            return executorResponse(Constants.ExecutorStatus.STATUS_RETRY,
                    "Failed to create organization: " + e.getMessage());
        }
    }

    /**
     * Builds and persists the organization through the {@link OrganizationManager} bound into this
     * bundle's own data holder by {@code OrganizationManagementExecutorServiceComponent}.
     *
     * @param context              Flow execution context carrying the organization and user details.
     * @param parentOrganizationId ID of the organization the new organization is created under.
     * @return ID of the created organization.
     * @throws OrganizationManagementException If the organization could not be created.
     */
    private String createOrganization(FlowExecutionContext context, String parentOrganizationId)
            throws OrganizationManagementException {

        OrganizationManager organizationManager =
                OrganizationManagementExecutorDataHolder.getInstance().getOrganizationManager();

        FlowUser flowUser = context.getFlowUser();
        FlowOrganization flowOrganization = context.getFlowOrganization();
        String creatorId = flowUser.getUserId();
        String organizationHandle = resolveOrganizationHandle(organizationManager, flowOrganization);
        Map<String, String> attributes = flowOrganization.getAttributes();

        TenantTypeOrganization organization = new TenantTypeOrganization(organizationHandle);
        organization.setId(Utils.generateUniqueID());
        organization.setName(flowOrganization.getOrganizationName());
        organization.setDescription(flowOrganization.getOrganizationDescription());
        organization.setStatus(OrganizationManagementConstants.OrganizationStatus.ACTIVE.toString());
        organization.setType(OrganizationManagementConstants.OrganizationTypes.TENANT.toString());
        organization.setCreated(Instant.now());
        organization.setLastModified(Instant.now());
        organization.getParent().setId(parentOrganizationId);
        organization.setVersion(OrganizationManagementConstants.OrganizationVersion.BASE_ORG_VERSION);
        organization.setOrganizationHandle(organizationHandle);
        organization.setCreatorId(creatorId);
        organization.setCreatorUsername(flowUser.getUsername());

        // Carry the custom organization attributes collected by the flow onto the organization.
        if (attributes != null) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                organization.setAttribute(new OrganizationAttribute(attribute.getKey(), attribute.getValue()));
            }
        }

        organizationManager.addOrganization(organization);
        LOG.debug("Organization created via onboarding flow. ID: " + organization.getId());

        return organization.getId();
    }

    /**
     * Resolves the parent of the new organization to the organization the flow is executing in, so a
     * sub-organization can onboard its own child organizations.
     *
     * @return ID of the parent organization, or {@code null} when it cannot be resolved.
     */
    private String resolveParentOrganizationId() {

        try {
            return Utils.getOrganizationId();
        } catch (RuntimeException e) {
            LOG.error("Error while resolving the organization of the executing flow.", e);
            return null;
        }
    }

    /**
     * Returns the handle to create the organization with. A handle submitted through the flow wins;
     * otherwise one is derived from the organization name and made unique. Returns {@code null} when no
     * usable handle can be derived, in which case {@code OrganizationManagerImpl.addOrganization} falls
     * back to the organization ID.
     *
     * @param organizationManager Organization manager used to check handle availability.
     * @param flowOrganization    Organization details collected by the flow.
     * @return A unique organization handle, or {@code null} to let the server derive one.
     * @throws OrganizationManagementException If the handle availability check fails.
     */
    private String resolveOrganizationHandle(OrganizationManager organizationManager,
                                             FlowOrganization flowOrganization)
            throws OrganizationManagementException {

        String submittedHandle = flowOrganization.getOrganizationHandle();
        if (StringUtils.isNotBlank(submittedHandle)) {
            return submittedHandle.trim();
        }

        // Only a name with no usable characters at all is left to the server. A short but usable name
        // is padded instead, so that an organization named "IBM" gets a readable handle rather than
        // falling back to its identifier.
        String baseHandle = padHandle(sanitizeHandle(flowOrganization.getOrganizationName()));
        if (StringUtils.isBlank(baseHandle)) {
            return null;
        }
        if (!organizationManager.isOrganizationExistByHandle(baseHandle)) {
            return baseHandle;
        }
        for (int suffix = 1; suffix <= MAX_HANDLE_COLLISION_ATTEMPTS; suffix++) {
            String candidate = appendHandleSuffix(baseHandle, String.valueOf(suffix));
            if (!organizationManager.isOrganizationExistByHandle(candidate)) {
                return candidate;
            }
        }

        // The short suffixes are taken, so try a random token rather than probing the store further.
        String candidate = appendHandleSuffix(baseHandle,
                UUID.randomUUID().toString().replace("-", "").substring(0, RANDOM_HANDLE_SUFFIX_LENGTH));
        if (!organizationManager.isOrganizationExistByHandle(candidate)) {
            return candidate;
        }

        LOG.debug("Could not derive a unique handle from the organization name. Falling back to a "
                + "server generated handle.");
        return null;
    }

    /**
     * Pads a handle that is shorter than the minimum handle length, so a short organization name still
     * yields a handle the console would accept. An empty handle is returned unchanged, since there is
     * nothing to pad and the server derives one instead.
     *
     * @param handle Sanitized handle derived from the organization name.
     * @return A handle of at least the minimum length, or an empty string.
     */
    private String padHandle(String handle) {

        if (StringUtils.isBlank(handle)) {
            return StringUtils.EMPTY;
        }
        StringBuilder padded = new StringBuilder(handle);
        while (padded.length() < MIN_HANDLE_LENGTH) {
            padded.append(HANDLE_PADDING_CHARACTER);
        }
        return padded.toString();
    }

    /**
     * Appends a suffix to a handle, trimming the base so the result stays within the handle length limit.
     *
     * @param baseHandle Sanitized handle derived from the organization name.
     * @param suffix     Suffix that makes the handle unique.
     * @return The suffixed handle.
     */
    private String appendHandleSuffix(String baseHandle, String suffix) {

        int allowedBaseLength = MAX_HANDLE_LENGTH - suffix.length();
        return baseHandle.length() > allowedBaseLength
                ? baseHandle.substring(0, allowedBaseLength) + suffix
                : baseHandle + suffix;
    }

    /**
     * Converts an organization name into a handle, applying the same rules as the organization handle
     * field in the console and the flow UI: lowercased, every character outside {@code [a-z0-9]}
     * dropped, leading non-letters removed, and the result capped at the maximum handle length.
     *
     * @param organizationName Organization name to convert.
     * @return The sanitized handle, or an empty string if the name holds no usable characters.
     */
    private String sanitizeHandle(String organizationName) {

        if (StringUtils.isBlank(organizationName)) {
            return StringUtils.EMPTY;
        }
        String handle = organizationName.trim().toLowerCase(Locale.ENGLISH)
                .replaceAll(HANDLE_SANITIZATION_REGEX, StringUtils.EMPTY);
        return handle.length() > MAX_HANDLE_LENGTH ? handle.substring(0, MAX_HANDLE_LENGTH) : handle;
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

    @Override
    public ExecutorResponse rollback(FlowExecutionContext context) throws FlowEngineException {

        return null;
    }
}
