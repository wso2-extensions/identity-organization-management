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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.management.executor;

import net.minidev.json.JSONValue;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.flow.execution.engine.graph.Executor;
import org.wso2.carbon.identity.flow.execution.engine.model.ExecutorResponse;
import org.wso2.carbon.identity.flow.execution.engine.model.FlowExecutionContext;
import org.wso2.carbon.identity.flow.mgt.model.NodeConfig;
import org.wso2.carbon.identity.organization.management.executor.internal.OrganizationManagementExecutorDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wso2.carbon.identity.flow.execution.engine.Constants.ExecutorStatus.STATUS_COMPLETE;

/**
 * Assigns configured roles already shared with the newly provisioned organization without failing onboarding.
 */
public class OrganizationRoleAssignmentExecutor implements Executor {

    private static final Log LOG = LogFactory.getLog(OrganizationRoleAssignmentExecutor.class);
    private static final String ROLE_IDS = "roleIds";
    private static final String PROVISION_TARGET = "provisionTarget";
    private static final String NEW_ORGANIZATION = "NEW_ORGANIZATION";

    @Override
    public String getName() {

        return "OrganizationRoleAssignmentExecutor";
    }

    @Override
    public ExecutorResponse execute(FlowExecutionContext context) {

        ExecutorResponse response = new ExecutorResponse();

        // Always complete this executor so role assignment failures do not fail provisioning.
        response.setResult(STATUS_COMPLETE);
        Set<String> roleIds = getRoleIds(context);
        if (roleIds.isEmpty()) {
            return response;
        }

        OrganizationManagementExecutorDataHolder dataHolder = OrganizationManagementExecutorDataHolder.getInstance();
        RoleManagementService roleManagementService = dataHolder.getRoleManagementService();
        OrganizationManager organizationManager = dataHolder.getOrganizationManager();
        if (roleManagementService == null || organizationManager == null || context.getFlowOrganization() == null
                || StringUtils.isBlank(context.getFlowOrganization().getOrganizationHandle())
                || context.getFlowUser() == null || StringUtils.isBlank(context.getFlowUser().getUserId())) {
            LOG.warn("Skipping organization roles because provisioning details or services are unavailable. Flow: "
                    + context.getContextIdentifier());
            return response;
        }

        String targetTenantDomain = context.getFlowOrganization().getOrganizationHandle();
        try {
            String targetUserId = resolveTargetUserId(context, organizationManager, targetTenantDomain);
            if (StringUtils.isBlank(targetUserId)) {
                LOG.warn("Skipping organization roles because the user is not shared with the new organization. "
                        + "Tenant: " + targetTenantDomain + ", flow: " + context.getContextIdentifier());
                return response;
            }

            PrivilegedCarbonContext.startTenantFlow();
            try {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(targetTenantDomain, true);
                Set<String> assignedRoles = new LinkedHashSet<>(
                        roleManagementService.getRoleIdListOfUser(targetUserId, targetTenantDomain));
                for (String roleId : roleIds) {
                    assignRole(roleManagementService, roleId, targetUserId, context, targetTenantDomain, assignedRoles);
                }
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException | RuntimeException e) {
            LOG.warn("Unable to assign configured organization roles. Provisioning remains successful. Tenant: "
                    + targetTenantDomain + ", flow: " + context.getContextIdentifier(), e);
        }
        return response;
    }

    private void assignRole(RoleManagementService roleManagementService, String roleId, String targetUserId,
                            FlowExecutionContext context, String targetTenantDomain, Set<String> assignedRoles) {

        try {
            if (roleManagementService.getRoleBasicInfoById(roleId, context.getTenantDomain()) == null) {
                LOG.warn("Skipping configured role unavailable in the parent tenant. Role: " + roleId
                        + ", tenant: " + context.getTenantDomain() + ", flow: " + context.getContextIdentifier());
                return;
            }
            List<String> sourceRoleIds = Collections.singletonList(roleId);
            Map<String, String> sourceMappings = roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(
                    sourceRoleIds, context.getTenantDomain());
            String mainRoleId = sourceMappings.getOrDefault(roleId, roleId);
            Map<String, String> targetMappings = roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(
                    Collections.singletonList(mainRoleId), targetTenantDomain);
            String sharedRoleId = targetMappings.get(mainRoleId);
            if (StringUtils.isBlank(sharedRoleId)) {
                LOG.warn("Skipping configured role not shared with the new organization. Role: " + roleId
                        + ", tenant: " + targetTenantDomain + ", flow: " + context.getContextIdentifier());
                return;
            }
            if (!assignedRoles.contains(sharedRoleId)) {
                roleManagementService.updateUserListOfRole(sharedRoleId, Collections.singletonList(targetUserId),
                        Collections.emptyList(), targetTenantDomain);
                assignedRoles.add(sharedRoleId);
            }
        } catch (IdentityRoleManagementException | RuntimeException e) {
            LOG.warn("Skipping configured organization role after assignment failure. Role: " + roleId
                    + ", tenant: " + targetTenantDomain + ", flow: " + context.getContextIdentifier(), e);
        }
    }

    private String resolveTargetUserId(FlowExecutionContext context, OrganizationManager organizationManager,
                                       String targetTenantDomain) throws OrganizationManagementException {

        if (NEW_ORGANIZATION.equals(getMetadataValue(context, PROVISION_TARGET))) {
            return context.getFlowUser().getUserId();
        }
        OrganizationUserSharingService userSharingService = OrganizationManagementExecutorDataHolder.getInstance()
                .getOrganizationUserSharingService();
        if (userSharingService == null) {
            return null;
        }
        String organizationId = organizationManager.resolveOrganizationId(targetTenantDomain);
        UserAssociation association = userSharingService.getUserAssociationOfAssociatedUserByOrgId(
                context.getFlowUser().getUserId(), organizationId);
        return association == null ? null : association.getUserId();
    }

    private Set<String> getRoleIds(FlowExecutionContext context) {

        Set<String> roleIds = new LinkedHashSet<>();
        String configuredRoles = getMetadataValue(context, ROLE_IDS);
        if (StringUtils.isBlank(configuredRoles)) {
            return roleIds;
        }
        try {
            Object parsed = JSONValue.parseWithException(configuredRoles);
            if (!(parsed instanceof List)) {
                LOG.warn("Ignoring invalid organization role metadata. Flow: " + context.getContextIdentifier());
                return roleIds;
            }
            for (Object value : (List<?>) parsed) {
                if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                    roleIds.add((String) value);
                } else {
                    LOG.warn("Ignoring invalid organization role entry. Flow: " + context.getContextIdentifier());
                }
            }
        } catch (ParseException e) {
            LOG.warn("Ignoring invalid organization role metadata. Flow: " + context.getContextIdentifier(), e);
        }
        return roleIds;
    }

    private String getMetadataValue(FlowExecutionContext context, String key) {

        NodeConfig currentNode = context.getCurrentNode();
        if (currentNode == null || currentNode.getExecutorConfig() == null
                || currentNode.getExecutorConfig().getMetadata() == null) {
            return null;
        }
        return currentNode.getExecutorConfig().getMetadata().get(key);
    }

    @Override
    public List<String> getInitiationData() {

        return Collections.emptyList();
    }

    @Override
    public ExecutorResponse rollback(FlowExecutionContext context) {

        return null;
    }
}
