/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharePatchOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.RoleAssignmentMode;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.exception.AgentSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.internal.OrganizationAgentSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.AgentAssociation;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.agentcriteria.AgentCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.agentcriteria.AgentIdList;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.AgentSharePatchDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.GeneralAgentShareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.GeneralAgentUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.GetAgentSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.PatchOperationDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.ResponseAgentSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.ResponseOrgDetailsAgentDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.RoleAssignmentDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.SelectiveAgentShareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.SelectiveAgentShareOrgDetailsDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.SelectiveAgentUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.SharingModeDO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.OrganizationScope;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ACTION_AGENT_SHARE_ROLE_ASSIGNMENT_UPDATE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ACTION_GENERAL_AGENT_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ACTION_GENERAL_AGENT_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ACTION_SELECTIVE_AGENT_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ACTION_SELECTIVE_AGENT_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.AGENT_IDS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ASYNC_PROCESSING_LOG_TEMPLATE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AGENT_CRITERIA_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AGENT_CRITERIA_MISSING;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AGENT_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AGENT_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ATTRIBUTES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ATTRIBUTE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ATTRIBUTE_UNSUPPORTED;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_AGENT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_INVALID_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_INVALID_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_NULL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_NULL_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ORG_DETAILS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_INVALID_FORMAT;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_OP_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_OP_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_PATH_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_PATH_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_ROLES_VALUE_CONTENT_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_VALUE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_VALUE_TYPE_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_REQUEST_BODY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_GENERAL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ErrorMessage.ERROR_SELECTIVE_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.LOG_WARN_SKIP_ORG_SHARE_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.PATCH_PATH_NONE;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.PATCH_PATH_PREFIX;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.PATCH_PATH_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.PATCH_PATH_SUFFIX_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.SHARED_AGENT_ROLE_INCLUDED_KEY;
import static org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.AgentSharingConstants.SHARED_AGENT_SHARING_MODE_INCLUDED_KEY;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Implementation of the agent sharing policy handler service.
 */
public class AgentSharingPolicyHandlerServiceImpl implements AgentSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(AgentSharingPolicyHandlerServiceImpl.class);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);
    private static final Set<String> SUPPORTED_GET_ATTRIBUTES =
            new HashSet<>(java.util.Arrays.asList(SHARED_AGENT_SHARING_MODE_INCLUDED_KEY,
                    SHARED_AGENT_ROLE_INCLUDED_KEY));
    private final UserIDResolver userIDResolver = new UserIDResolver();

    @Override
    public void populateSelectiveAgentShare(SelectiveAgentShareDO selectiveAgentShareDO)
            throws AgentSharingMgtException {

        validateAgentShareInput(selectiveAgentShareDO);
        String sharingInitiatedOrgId = getOrganizationId();
        List<SelectiveAgentShareOrgDetailsDO> organizations = selectiveAgentShareDO.getOrganizations();
        Map<String, AgentCriteriaType> agentCriteria = selectiveAgentShareDO.getAgentCriteria();
        List<SelectiveAgentShareOrgDetailsDO> validOrganizations =
                filterValidOrganizations(organizations, sharingInitiatedOrgId);

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUserId = carbonContext.getUserId();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_SELECTIVE_AGENT_SHARE, sharingInitiatedUserId, sharingInitiatedOrgId);
                    try {
                        initiateThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                                sharingInitiatedUsername, threadLocalProperties);
                        processSelectiveAgentShare(agentCriteria, validOrganizations, sharingInitiatedOrgId,
                                sharingInitiatedUserId);
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async selective agent share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateGeneralAgentShare(GeneralAgentShareDO generalAgentShareDO) throws AgentSharingMgtException {

        validateAgentShareInput(generalAgentShareDO);
        String sharingInitiatedOrgId = getOrganizationId();
        Map<String, AgentCriteriaType> agentCriteria = generalAgentShareDO.getAgentCriteria();
        PolicyEnum policy = generalAgentShareDO.getPolicy();
        List<String> roleIds = getRoleIds(generalAgentShareDO.getRoleAssignments().getRoles(), sharingInitiatedOrgId);
        RoleAssignmentMode roleAssignmentMode = generalAgentShareDO.getRoleAssignments().getMode();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUserId = carbonContext.getUserId();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_GENERAL_AGENT_SHARE, sharingInitiatedUserId, sharingInitiatedOrgId);
                    try {
                        initiateThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                                sharingInitiatedUsername, threadLocalProperties);
                        processGeneralAgentShare(agentCriteria, policy, roleIds, roleAssignmentMode,
                                sharingInitiatedOrgId, sharingInitiatedUserId);
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general agent share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateSelectiveAgentUnshare(SelectiveAgentUnshareDO selectiveAgentUnshareDO)
            throws AgentSharingMgtException {

        validateAgentUnshareInput(selectiveAgentUnshareDO);
        String sharingInitiatedOrgId = getOrganizationId();
        Map<String, AgentCriteriaType> agentCriteria = selectiveAgentUnshareDO.getAgentCriteria();
        List<String> organizations = selectiveAgentUnshareDO.getOrganizations();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_SELECTIVE_AGENT_UNSHARE, carbonContext.getUserId(),
                            sharingInitiatedOrgId);
                    try {
                        initiateThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                                sharingInitiatedUsername, threadLocalProperties);
                        processSelectiveAgentUnshare(agentCriteria, organizations, sharingInitiatedOrgId);
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async selective agent unshare processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateGeneralAgentUnshare(GeneralAgentUnshareDO generalAgentUnshareDO)
            throws AgentSharingMgtException {

        validateAgentUnshareInput(generalAgentUnshareDO);
        String sharingInitiatedOrgId = getOrganizationId();
        Map<String, AgentCriteriaType> agentCriteria = generalAgentUnshareDO.getAgentCriteria();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_GENERAL_AGENT_UNSHARE, carbonContext.getUserId(), sharingInitiatedOrgId);
                    try {
                        initiateThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                                sharingInitiatedUsername, threadLocalProperties);
                        processGeneralAgentUnshare(agentCriteria, sharingInitiatedOrgId);
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general agent unshare processing.", ex);
                    return null;
                });
    }

    @Override
    public void updateSharedAgentAttributes(AgentSharePatchDO agentSharePatchDO) throws AgentSharingMgtException {

        validateSharedAgentAttributeUpdateInput(agentSharePatchDO);
        String sharingInitiatedOrgId = getOrganizationId();
        Map<String, AgentCriteriaType> agentCriteria = agentSharePatchDO.getAgentCriteria();

        // Capture thread-local properties before async execution.
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        String sharingInitiatedUserId = carbonContext.getUserId();
        String sharingInitiatedUsername = carbonContext.getUsername();
        int sharingInitiatedTenantId = carbonContext.getTenantId();
        String sharingInitiatedTenantDomain = carbonContext.getTenantDomain();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_AGENT_SHARE_ROLE_ASSIGNMENT_UPDATE, sharingInitiatedUserId,
                            sharingInitiatedOrgId);
                    try {
                        initiateThreadLocalContext(sharingInitiatedTenantDomain, sharingInitiatedTenantId,
                                sharingInitiatedUsername, threadLocalProperties);
                        processUpdateSharedAgentAttributes(agentCriteria, sharingInitiatedOrgId, sharingInitiatedUserId,
                                agentSharePatchDO);
                    } finally {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async agent share role assignment update processing.", ex);
                    return null;
                });
    }

    @Override
    public ResponseAgentSharedOrgsDO getAgentSharedOrganizations(GetAgentSharedOrgsDO getAgentSharedOrgsDO)
            throws AgentSharingMgtException {

        validateSharedAgentGetInput(getAgentSharedOrgsDO);

        String mainAgentId = getAgentSharedOrgsDO.getAgentId();
        String parentOrgId = getAgentSharedOrgsDO.getParentOrgId();
        List<String> includedAttributes = getAgentSharedOrgsDO.getAttributes();
        List<ResponseOrgDetailsAgentDO> sharedOrgsList = new ArrayList<>();

        try {
            SharingModeDO generalSharingMode = resolveGeneralSharingMode(includedAttributes, parentOrgId, mainAgentId);
            List<AgentAssociation> agentAssociations =
                    getOrganizationAgentSharingService().getAgentAssociationsOfGivenAgent(mainAgentId, parentOrgId);

            if (CollectionUtils.isEmpty(agentAssociations)) {
                return buildEmptyResponseToGet(generalSharingMode);
            }

            for (AgentAssociation agentAssociation : agentAssociations) {
                sharedOrgsList.add(resolveSharedOrgDetails(agentAssociation, includedAttributes));
            }

            return buildResponseWithCursors(sharedOrgsList, agentAssociations, generalSharingMode);
        } catch (OrganizationManagementException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_AGENT, e);
        }
    }

    /**
     * Processes selective agent sharing based on the provided agent criteria and organization details.
     *
     * @param agentCriteria          A map containing agent criteria, such as agent IDs.
     * @param organizations          A list of organizations to which agents will be shared selectively.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the agent sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the agent sharing.
     */
    private void processSelectiveAgentShare(Map<String, AgentCriteriaType> agentCriteria,
                                            List<SelectiveAgentShareOrgDetailsDO> organizations,
                                            String sharingInitiatedOrgId, String sharingInitiatedUserId) {

        for (Map.Entry<String, AgentCriteriaType> criterion : agentCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            AgentCriteriaType criterionValues = criterion.getValue();

            try {
                if (AGENT_IDS.equals(criterionKey)) {
                    if (criterionValues instanceof AgentIdList) {
                        selectiveAgentShareByAgentIds((AgentIdList) criterionValues, organizations,
                                sharingInitiatedOrgId, sharingInitiatedUserId);
                    } else {
                        LOG.error("Invalid agent criteria provided for selective agent share: " + criterionKey);
                    }
                } else {
                    LOG.error("Invalid agent criteria provided for selective agent share: " + criterionKey);
                }
            } catch (AgentSharingMgtException e) {
                LOG.error("Error occurred while sharing agent from agent criteria: " + AGENT_IDS, e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed selective agent share initiated from " + sharingInitiatedOrgId + ".");
        }
    }

    /**
     * Processes general agent sharing based on the provided agent criteria and sharing policy.
     *
     * @param agentCriteria          A map containing agent criteria, such as agent IDs.
     * @param policy                 The sharing policy defining the scope of sharing.
     * @param roleIds                A list of role IDs to be assigned during sharing.
     * @param roleAssignmentMode     The mode for role assignment.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the agent sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the agent sharing.
     */
    private void processGeneralAgentShare(Map<String, AgentCriteriaType> agentCriteria, PolicyEnum policy,
                                          List<String> roleIds, RoleAssignmentMode roleAssignmentMode,
                                          String sharingInitiatedOrgId, String sharingInitiatedUserId) {

        for (Map.Entry<String, AgentCriteriaType> criterion : agentCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            AgentCriteriaType criterionValues = criterion.getValue();

            try {
                if (AGENT_IDS.equals(criterionKey)) {
                    if (criterionValues instanceof AgentIdList) {
                        generalAgentShareByAgentIds((AgentIdList) criterionValues, policy, roleIds, roleAssignmentMode,
                                sharingInitiatedOrgId, sharingInitiatedUserId);
                    } else {
                        LOG.error("Invalid agent criteria provided for general agent share: " + criterionKey);
                    }
                } else {
                    LOG.error("Invalid agent criteria provided for general agent share: " + criterionKey);
                }
            } catch (AgentSharingMgtException e) {
                LOG.error("Error occurred while sharing agent from agent criteria: " + AGENT_IDS, e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed general agent share initiated from " + sharingInitiatedOrgId + ".");
        }
    }

    /**
     * Processes selective agent unsharing based on the provided agent criteria and organization details.
     *
     * @param agentCriteria         A map containing agent criteria, such as agent IDs.
     * @param organizations         A list of organizations from which agents will be unshared selectively.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the agent unsharing.
     */
    private void processSelectiveAgentUnshare(Map<String, AgentCriteriaType> agentCriteria, List<String> organizations,
                                              String sharingInitiatedOrgId) {

        for (Map.Entry<String, AgentCriteriaType> criterion : agentCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            AgentCriteriaType criterionValues = criterion.getValue();

            try {
                if (AGENT_IDS.equals(criterionKey)) {
                    if (criterionValues instanceof AgentIdList) {
                        selectiveAgentUnshareByAgentIds((AgentIdList) criterionValues, organizations,
                                sharingInitiatedOrgId);
                    } else {
                        LOG.error("Invalid agent criteria provided for selective agent unshare: " + criterionKey);
                    }
                } else {
                    LOG.error("Invalid agent criteria provided for selective agent unshare: " + criterionKey);
                }
            } catch (AgentSharingMgtException e) {
                LOG.error("Error occurred while unsharing agent from agent criteria: " + AGENT_IDS, e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed selective agent unshare initiated from " + sharingInitiatedOrgId + ".");
        }
    }

    /**
     * Processes general agent unsharing based on the provided agent criteria.
     *
     * @param agentCriteria         A map containing agent criteria, such as agent IDs.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the agent unsharing.
     */
    private void processGeneralAgentUnshare(Map<String, AgentCriteriaType> agentCriteria,
                                            String sharingInitiatedOrgId) {

        for (Map.Entry<String, AgentCriteriaType> criterion : agentCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            AgentCriteriaType criterionValues = criterion.getValue();

            try {
                if (AGENT_IDS.equals(criterionKey)) {
                    if (criterionValues instanceof AgentIdList) {
                        generalAgentUnshareByAgentIds((AgentIdList) criterionValues, sharingInitiatedOrgId);
                    } else {
                        LOG.error("Invalid agent criteria provided for general agent unshare: " + criterionKey);
                    }
                } else {
                    LOG.error("Invalid agent criteria provided for general agent unshare: " + criterionKey);
                }
            } catch (AgentSharingMgtException e) {
                LOG.error("Error occurred while unsharing agent from agent criteria: " + AGENT_IDS, e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed general agent unshare initiated from " + sharingInitiatedOrgId + ".");
        }
    }

    /**
     * Processes shared agent attribute update based on the provided agent criteria.
     *
     * @param agentCriteria          A map containing agent criteria, such as agent IDs.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the update.
     * @param sharingInitiatedUserId The ID of the user that initiated the update.
     * @param agentSharePatchDO      The patch data object containing patch operations.
     */
    private void processUpdateSharedAgentAttributes(Map<String, AgentCriteriaType> agentCriteria,
                                                    String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                                    AgentSharePatchDO agentSharePatchDO) {

        for (Map.Entry<String, AgentCriteriaType> criterion : agentCriteria.entrySet()) {
            String criterionKey = criterion.getKey();
            AgentCriteriaType criterionValues = criterion.getValue();

            try {
                if (AGENT_IDS.equals(criterionKey)) {
                    if (criterionValues instanceof AgentIdList) {
                        updateSharedAgentAttributesByAgentIds((AgentIdList) criterionValues, sharingInitiatedOrgId,
                                sharingInitiatedUserId, agentSharePatchDO);
                    } else {
                        LOG.error("Invalid agent criteria provided for agent share role assignment update: " +
                                criterionKey);
                    }
                } else {
                    LOG.error("Invalid agent criteria provided for agent share role assignment update: " +
                            criterionKey);
                }
            } catch (AgentSharingMgtException e) {
                LOG.error("Error occurred while updating role assignments from agent criteria: " + AGENT_IDS, e);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed agent share role assignment update initiated from " + sharingInitiatedOrgId + ".");
        }
    }

    // Agent Sharing & Unsharing Helper Methods.

    /**
     * Shares agents with selected organizations based on the provided agent list and sharing policies.
     *
     * @param agentIds               The list of agent IDs to be selectively shared.
     * @param organizations          The list of organizations where agents should be shared.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the sharing.
     */
    private void selectiveAgentShareByAgentIds(AgentIdList agentIds,
                                               List<SelectiveAgentShareOrgDetailsDO> organizations,
                                               String sharingInitiatedOrgId, String sharingInitiatedUserId)
            throws AgentSharingMgtException {

        for (String associatedAgentId : agentIds.getIds()) {
            try {
                for (SelectiveAgentShareOrgDetailsDO organization : organizations) {
                    List<String> targetOrgs =
                            extractOrgListBasedOnSharingPolicy(organization.getOrganizationId(),
                                    organization.getPolicy());
                    for (String targetOrgId : targetOrgs) {
                        if (!isAgentAlreadySharedInOrg(associatedAgentId, sharingInitiatedOrgId, targetOrgId)) {
                            getOrganizationAgentSharingService().shareOrganizationAgent(targetOrgId, associatedAgentId,
                                    sharingInitiatedOrgId, SharedType.SHARED);
                            AgentAssociation agentAssociation = getOrganizationAgentSharingService()
                                .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, targetOrgId);
                            if (agentAssociation != null &&
                                organization.getRoleAssignments() != null &&
                                organization.getRoleAssignments().getMode() != RoleAssignmentMode.NONE) {
                                List<String> roleIds =
                                    getRoleIds(organization.getRoleAssignments().getRoles(),
                                        sharingInitiatedOrgId);
                                assignRolesIfPresent(agentAssociation, sharingInitiatedOrgId, roleIds);
                            }
                        } else {
                            handleExistingSharedAgent(associatedAgentId, sharingInitiatedOrgId, targetOrgId,
                                organization.getRoleAssignments() != null
                                    ? getRoleIds(organization.getRoleAssignments().getRoles(),
                                    sharingInitiatedOrgId)
                                    : Collections.emptyList(),
                                organization.getRoleAssignments() != null
                                    ? organization.getRoleAssignments().getMode()
                                    : RoleAssignmentMode.NONE);
                        }
                    }
                    if (isApplicableOrganizationScopeForSavingPolicy(organization.getPolicy())) {
                        saveAgentSharingPolicy(associatedAgentId, sharingInitiatedOrgId,
                                organization.getOrganizationId(), organization.getPolicy(),
                                organization.getRoleAssignments() != null
                                        ? getRoleIds(organization.getRoleAssignments().getRoles(),
                                                sharingInitiatedOrgId)
                                        : Collections.emptyList());
                    }
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                String errorMessage =
                        String.format(ERROR_SELECTIVE_SHARE.getMessage(), associatedAgentId, e.getMessage());
                throw new AgentSharingMgtServerException(ERROR_SELECTIVE_SHARE, errorMessage);
            }
        }
    }

    /**
     * Shares agents with all applicable organizations based on the provided policy.
     *
     * @param agentIds               The list of agent IDs to be shared.
     * @param policy                 The policy defining the scope of sharing.
     * @param roleIds                The list of role IDs to be assigned during sharing.
     * @param roleAssignmentMode     The mode for role assignment.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the sharing.
     */
    private void generalAgentShareByAgentIds(AgentIdList agentIds, PolicyEnum policy, List<String> roleIds,
                                             RoleAssignmentMode roleAssignmentMode, String sharingInitiatedOrgId,
                                             String sharingInitiatedUserId)
            throws AgentSharingMgtException {

        for (String associatedAgentId : agentIds.getIds()) {
            try {
                List<String> targetOrgs = extractOrgListBasedOnSharingPolicy(sharingInitiatedOrgId, policy);
                for (String targetOrgId : targetOrgs) {
                    if (!isAgentAlreadySharedInOrg(associatedAgentId, sharingInitiatedOrgId, targetOrgId)) {
                        getOrganizationAgentSharingService().shareOrganizationAgent(targetOrgId, associatedAgentId,
                                sharingInitiatedOrgId, SharedType.SHARED);
                        AgentAssociation agentAssociation = getOrganizationAgentSharingService()
                                .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, targetOrgId);
                        if (agentAssociation != null && roleAssignmentMode != RoleAssignmentMode.NONE) {
                            assignRolesIfPresent(agentAssociation, sharingInitiatedOrgId, roleIds);
                        }
                    } else {
                        handleExistingSharedAgent(associatedAgentId, sharingInitiatedOrgId, targetOrgId, roleIds,
                                roleAssignmentMode);
                    }
                }
                if (isApplicableOrganizationScopeForSavingPolicy(policy)) {
                    saveAgentSharingPolicy(associatedAgentId, sharingInitiatedOrgId, sharingInitiatedOrgId, policy,
                            roleIds);
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                String errorMessage = String.format(ERROR_GENERAL_SHARE.getMessage(), associatedAgentId,
                        e.getMessage());
                throw new AgentSharingMgtServerException(ERROR_GENERAL_SHARE, errorMessage);
            }
        }
    }

    /**
     * Unshares agents from selected organizations based on the provided agent list.
     *
     * @param agentIds               The list of agent IDs to be unshared.
     * @param organizations          The list of organizations from which agents should be unshared.
     * @param unsharingInitiatedOrgId The ID of the organization that initiated the unsharing.
     */
    private void selectiveAgentUnshareByAgentIds(AgentIdList agentIds, List<String> organizations,
                                                 String unsharingInitiatedOrgId)
            throws AgentSharingMgtServerException {

        for (String associatedAgentId : agentIds.getIds()) {
            try {
                for (String organizationId : organizations) {
                    List<String> orgTreeInclusive = new ArrayList<>();
                    orgTreeInclusive.add(organizationId);
                    orgTreeInclusive.addAll(
                            getOrganizationManager().getChildOrganizationsIds(organizationId, true));

                    for (String eachOrg : orgTreeInclusive) {
                        getOrganizationAgentSharingService().unshareOrganizationAgentInSharedOrganization(
                                associatedAgentId, eachOrg);
                    }
                    // Delete resource sharing policy if it has been stored for future shares.
                    deleteResourceSharingPolicyOfAgentInOrg(organizationId, associatedAgentId, unsharingInitiatedOrgId);
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new AgentSharingMgtServerException(ERROR_CODE_AGENT_UNSHARE, e);
            }
        }
    }

    /**
     * Unshares agents from all applicable organizations based on the provided agent list.
     *
     * @param agentIds                The list of agent IDs to be unshared.
     * @param unsharingInitiatedOrgId The ID of the organization that initiated the unsharing.
     */
    private void generalAgentUnshareByAgentIds(AgentIdList agentIds, String unsharingInitiatedOrgId)
            throws AgentSharingMgtServerException {

        for (String associatedAgentId : agentIds.getIds()) {
            try {
                getOrganizationAgentSharingService().unshareOrganizationAgents(associatedAgentId,
                        unsharingInitiatedOrgId);
                deleteAllResourceSharingPoliciesOfAgent(associatedAgentId, unsharingInitiatedOrgId);
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new AgentSharingMgtServerException(ERROR_CODE_AGENT_UNSHARE, e);
            }
        }
    }

    /**
     * Updates shared agent attributes for the given agent IDs.
     *
     * @param agentIds               The list of agent IDs whose attributes should be updated.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the update.
     * @param sharingInitiatedUserId The ID of the user that initiated the update.
     * @param agentSharePatchDO      The patch data object containing patch operations.
     */
    private void updateSharedAgentAttributesByAgentIds(AgentIdList agentIds, String sharingInitiatedOrgId,
                                                       String sharingInitiatedUserId,
                                                       AgentSharePatchDO agentSharePatchDO)
            throws AgentSharingMgtException {

        for (String associatedAgentId : agentIds.getIds()) {
            try {
                updateSharedAgentAttributesForAgent(associatedAgentId, sharingInitiatedOrgId, sharingInitiatedUserId,
                        agentSharePatchDO.getPatchOperations());
            } catch (OrganizationManagementException | IdentityRoleManagementException e) {
                throw new AgentSharingMgtServerException(ERROR_CODE_AGENT_SHARE);
            }
        }
    }

    /**
     * Updates shared agent attributes for a single agent.
     *
     * @param associatedAgentId      The ID of the associated agent.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the update.
     * @param sharingInitiatedUserId The ID of the user that initiated the update.
     * @param patchOperations        The list of patch operations.
     */
    private void updateSharedAgentAttributesForAgent(String associatedAgentId, String sharingInitiatedOrgId,
                                                     String sharingInitiatedUserId,
                                                     List<PatchOperationDO> patchOperations)
            throws AgentSharingMgtException, OrganizationManagementException, IdentityRoleManagementException {

        for (PatchOperationDO patchOperation : patchOperations) {
            if (isPatchOperationPathRoles(patchOperation.getPath().trim())) {
                updateRoleAssignmentsOfSharedAgent(associatedAgentId, sharingInitiatedOrgId, sharingInitiatedUserId,
                        patchOperation);
            }
        }
    }

    /**
     * Updates role assignments for a shared agent based on a patch operation.
     *
     * @param associatedAgentId      The ID of the associated agent.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the update.
     * @param sharingInitiatedUserId The ID of the user that initiated the update.
     * @param patchOperation         The patch operation containing org-scoped role changes.
     */
    private void updateRoleAssignmentsOfSharedAgent(String associatedAgentId, String sharingInitiatedOrgId,
                                                    String sharingInitiatedUserId, PatchOperationDO patchOperation)
            throws AgentSharingMgtException, OrganizationManagementException, IdentityRoleManagementException {

        String orgId = extractOrgIdFromRolesPath(patchOperation.getPath());
        AgentAssociation agentAssociation = getOrganizationAgentSharingService()
                .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, orgId);
        if (agentAssociation == null) {
            LOG.warn("No agent association found for agent: " + associatedAgentId + " in organization: " + orgId +
                    ". Skipping role assignment update.");
            return;
        }
        List<String> roleIds = getRoleIds(castToRoleWithAudienceList(patchOperation.getValues()),
                sharingInitiatedOrgId);
        switch (patchOperation.getOperation()) {
            case ADD:
                assignRolesIfPresent(agentAssociation, sharingInitiatedOrgId, roleIds);
                break;
            case REMOVE:
                unAssignOldSharedRolesFromSharedAgent(agentAssociation, roleIds);
                break;
        }
    }

    // GET operation helper methods.

    /**
     * Builds the final response object for get agent shared organizations.
     *
     * @param sharedOrgsList   List of shared organizations.
     * @param agentAssociations List of agent associations.
     * @param generalSharingMode General sharing mode.
     * @return ResponseAgentSharedOrgsDO.
     */
    private ResponseAgentSharedOrgsDO buildResponseWithCursors(List<ResponseOrgDetailsAgentDO> sharedOrgsList,
                                                               List<AgentAssociation> agentAssociations,
                                                               SharingModeDO generalSharingMode) {

        ResponseAgentSharedOrgsDO response = new ResponseAgentSharedOrgsDO();
        response.setSharedOrgs(sharedOrgsList);
        response.setSharingMode(generalSharingMode);

        int nextToken = 0;
        int previousToken = 0;

        if (!agentAssociations.isEmpty()) {
            nextToken = agentAssociations.get(agentAssociations.size() - 1).getId();
        }

        response.setNextPageCursor(nextToken);
        response.setPreviousPageCursor(previousToken);
        return response;
    }

    /**
     * Builds an empty response for get agent shared organizations.
     *
     * @param sharingMode Sharing mode.
     * @return ResponseAgentSharedOrgsDO.
     */
    private ResponseAgentSharedOrgsDO buildEmptyResponseToGet(SharingModeDO sharingMode) {

        ResponseAgentSharedOrgsDO response = new ResponseAgentSharedOrgsDO();
        response.setSharingMode(sharingMode);
        response.setSharedOrgs(Collections.emptyList());
        response.setNextPageCursor(0);
        response.setPreviousPageCursor(0);
        return response;
    }

    /**
     * Resolves shared organization details from an agent association.
     *
     * @param agentAssociation       Agent association.
     * @param includedAttributesList List of included attributes.
     * @return ResponseOrgDetailsAgentDO.
     */
    private ResponseOrgDetailsAgentDO resolveSharedOrgDetails(AgentAssociation agentAssociation,
                                                              List<String> includedAttributesList)
            throws OrganizationManagementException, AgentSharingMgtException {

        ResponseOrgDetailsAgentDO responseOrgDetailsAgentDO = new ResponseOrgDetailsAgentDO();
        responseOrgDetailsAgentDO.setAgentId(agentAssociation.getAssociatedAgentId());
        responseOrgDetailsAgentDO.setSharedAgentId(agentAssociation.getAgentId());
        responseOrgDetailsAgentDO.setSharedType(agentAssociation.getSharedType());

        org.wso2.carbon.identity.organization.management.service.model.Organization organization =
                getOrganizationManager().getOrganization(agentAssociation.getOrganizationId(), true, false);

        String tenantDomain = getOrganizationManager().resolveTenantDomain(organization.getId());
        responseOrgDetailsAgentDO.setOrganizationId(organization.getId());
        responseOrgDetailsAgentDO.setOrganizationName(organization.getName());
        responseOrgDetailsAgentDO.setOrganizationHandle(organization.getOrganizationHandle());
        responseOrgDetailsAgentDO.setOrganizationStatus(organization.getStatus());
        responseOrgDetailsAgentDO.setOrganizationReference("/t/" + tenantDomain +
                "/api/server/v1/organizations/" + organization.getId());
        responseOrgDetailsAgentDO.setParentOrganizationId(
                organization.getParent() != null ? organization.getParent().getId() : null);
        responseOrgDetailsAgentDO.setHasChildren(organization.hasChildren());
        responseOrgDetailsAgentDO.setDepthFromRoot(
                getOrganizationManager().getOrganizationDepthInHierarchy(organization.getId()));

        if (includedAttributesList.contains(SHARED_AGENT_SHARING_MODE_INCLUDED_KEY)) {
            SharingModeDO sharingModeDO = resolveSelectiveSharingMode(organization.getId(),
                    agentAssociation.getAssociatedAgentId(), organization.getId());
            responseOrgDetailsAgentDO.setSharingModeDO(sharingModeDO);
        }
        if (includedAttributesList.contains(SHARED_AGENT_ROLE_INCLUDED_KEY)) {
            responseOrgDetailsAgentDO.setRoleWithAudienceDOList(
                    getAssignedSharedRolesForSharedAgentInOrganization(agentAssociation, organization.getId()));
        }

        return responseOrgDetailsAgentDO;
    }

    /**
     * Gets assigned shared roles for a shared agent in an organization.
     *
     * @param agentAssociation Agent association.
     * @param orgId            Organization ID.
     * @return List of RoleWithAudienceDO.
     */
    private List<RoleWithAudienceDO> getAssignedSharedRolesForSharedAgentInOrganization(
            AgentAssociation agentAssociation, String orgId) throws AgentSharingMgtException {

        try {
            List<RoleWithAudienceDO> roleWithAudienceList = new ArrayList<>();
            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = org.wso2.carbon.identity.core.util.IdentityTenantUtil.getTenantId(tenantDomain);
            String username = agentAssociation.getAgentId();
            String domainName = IdentityUtil.getAgentIdentityUserstoreName();

            List<String> sharedRoleIdsInOrg =
                    getOrganizationAgentSharingService().getRolesSharedWithAgentInOrganization(username, tenantId,
                            domainName);

            for (String sharedRoleId : sharedRoleIdsInOrg) {
                Role role = getRoleManagementService().getRole(sharedRoleId, tenantDomain);
                RoleWithAudienceDO roleWithAudience = new RoleWithAudienceDO();
                roleWithAudience.setRoleName(role.getName());
                roleWithAudience.setAudienceName(role.getAudienceName());
                roleWithAudience.setAudienceType(role.getAudience());
                roleWithAudienceList.add(roleWithAudience);
            }
            return roleWithAudienceList;
        } catch (OrganizationManagementException | IdentityRoleManagementException |
                 AgentSharingMgtException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_AGENT, e);
        }
    }

    /**
     * Resolves the general sharing mode for supported future general sharing policies.
     *
     * @param includedAttributes List of included attributes.
     * @param parentOrgId        Parent organization ID.
     * @param mainAgentId        Main agent ID.
     * @return SharingModeDO.
     */
    private SharingModeDO resolveGeneralSharingMode(List<String> includedAttributes, String parentOrgId,
                                                    String mainAgentId)
            throws OrganizationManagementException {

        if (includedAttributes.contains(SHARED_AGENT_SHARING_MODE_INCLUDED_KEY)) {
            return resolveSharingMode(parentOrgId, mainAgentId, false, null);
        }
        return null;
    }

    /**
     * Resolves selective sharing mode for supported future selective sharing policies.
     *
     * @param initiatingOrgId Initiating organization ID.
     * @param mainAgentId     Main agent ID.
     * @param subOrgId        Sub organization ID for selective share.
     * @return SharingModeDO.
     */
    private SharingModeDO resolveSelectiveSharingMode(String initiatingOrgId, String mainAgentId, String subOrgId)
            throws OrganizationManagementException {

        return resolveSharingMode(initiatingOrgId, mainAgentId, true, subOrgId);
    }

    /**
     * Resolves sharing mode if the sharing policy is a future policy.
     *
     * @param initiatingOrgId  Initiating organization ID.
     * @param mainAgentId      Main agent ID.
     * @param isSelectiveShare Whether it is a selective share.
     * @param subOrgId         Sub organization ID for selective share.
     * @return SharingModeDO.
     */
    private SharingModeDO resolveSharingMode(String initiatingOrgId, String mainAgentId, boolean isSelectiveShare,
                                             String subOrgId)
            throws OrganizationManagementException {

        try {
            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> result =
                    getResourceSharingPolicyHandlerService()
                            .getResourceSharingPolicyAndAttributesByInitiatingOrgId(
                                    initiatingOrgId, ResourceType.AGENT.name(), mainAgentId);

            if (result != null && !result.isEmpty()) {
                Map.Entry<ResourceSharingPolicy, List<SharedResourceAttribute>> entry =
                        result.entrySet().iterator().next();
                ResourceSharingPolicy resourceSharingPolicy = entry.getKey();
                List<SharedResourceAttribute> resourceAttributes = entry.getValue();

                if (isSelectiveShare) {
                    boolean isPolicyHolderOrg =
                            Objects.equals(resourceSharingPolicy.getPolicyHoldingOrgId(), subOrgId);
                    if (resourceSharingPolicy.getSharingPolicy() ==
                            PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN && isPolicyHolderOrg) {
                        return getSharingModeDO(resourceSharingPolicy, resourceAttributes);
                    }
                } else {
                    if (resourceSharingPolicy.getSharingPolicy() == PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS ||
                            resourceSharingPolicy.getSharingPolicy() == PolicyEnum.IMMEDIATE_EXISTING_AND_FUTURE_ORGS) {
                        return getSharingModeDO(resourceSharingPolicy, resourceAttributes);
                    }
                }
            }
        } catch (ResourceSharingPolicyMgtException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        } catch (IdentityRoleManagementException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getErrorCode());
        }
        return null;
    }

    /**
     * Constructs SharingModeDO from ResourceSharingPolicy and SharedResourceAttributes.
     *
     * @param resourceSharingPolicy ResourceSharingPolicy.
     * @param resourceAttributes    List of SharedResourceAttribute.
     * @return SharingModeDO.
     */
    private SharingModeDO getSharingModeDO(ResourceSharingPolicy resourceSharingPolicy,
                                           List<SharedResourceAttribute> resourceAttributes)
            throws IdentityRoleManagementException {

        SharingModeDO sharingModeDO = new SharingModeDO(resourceSharingPolicy.getSharingPolicy());
        RoleAssignmentDO roleAssignmentDO = new RoleAssignmentDO();
        roleAssignmentDO.setMode(RoleAssignmentMode.NONE);
        roleAssignmentDO.setRoles(Collections.emptyList());
        sharingModeDO.setRoleAssignment(roleAssignmentDO);

        if (resourceAttributes != null && !resourceAttributes.isEmpty()) {
            List<SharedResourceAttribute> roleAttributes = resourceAttributes.stream()
                    .filter(attr -> attr.getSharedAttributeType() == SharedAttributeType.ROLE)
                    .collect(Collectors.toList());

            if (!roleAttributes.isEmpty()) {
                roleAssignmentDO.setMode(RoleAssignmentMode.SELECTED);
                roleAssignmentDO.setRoles(getRoleWithAudienceFromMainRoleIds(roleAttributes,
                        resourceSharingPolicy.getInitiatingOrgId()));
                sharingModeDO.setRoleAssignment(roleAssignmentDO);
            }
        }
        return sharingModeDO;
    }

    /**
     * Gets RoleWithAudienceDO list from main role IDs.
     *
     * @param roleAttributes  List of SharedResourceAttribute.
     * @param initiatingOrgId The ID of the organization that initiated the sharing.
     * @return List of RoleWithAudienceDO.
     */
    private List<RoleWithAudienceDO> getRoleWithAudienceFromMainRoleIds(List<SharedResourceAttribute> roleAttributes,
                                                                        String initiatingOrgId)
            throws IdentityRoleManagementException {

        try {
            String tenantDomain = getOrganizationManager().resolveTenantDomain(initiatingOrgId);
            List<RoleWithAudienceDO> roleWithAudiences = new ArrayList<>();
            for (SharedResourceAttribute attribute : roleAttributes) {
                RoleBasicInfo role = getRoleManagementService().getRoleBasicInfoById(
                        attribute.getSharedAttributeId(), tenantDomain);
                if (role != null) {
                    roleWithAudiences.add(
                            new RoleWithAudienceDO(role.getName(), role.getAudienceName(), role.getAudience()));
                }
            }
            return roleWithAudiences;
        } catch (OrganizationManagementException e) {
            throw new IdentityRoleManagementException(
                    "Error resolving tenant domain for initiating org: " + initiatingOrgId, e);
        }
    }

    // Business Logic Methods.

    /**
     * Checks if an agent is already shared in the specified organization.
     *
     * @param associatedAgentId The ID of the agent.
     * @param associatedOrgId   The ID of the resident organization.
     * @param subOrgId          The target sub-organization ID.
     * @return True if the agent is already shared, false otherwise.
     */
    private boolean isAgentAlreadySharedInOrg(String associatedAgentId, String associatedOrgId, String subOrgId)
            throws OrganizationManagementException {

        return getOrganizationAgentSharingService().hasAgentAssociationsInOrganizations(associatedAgentId,
                associatedOrgId, Collections.singletonList(subOrgId));
    }

    /**
     * Reconciles role assignments for an existing shared agent in the specified organization.
     *
     * @param associatedAgentId     The ID of the associated agent.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param targetOrgId           The ID of the organization where the agent is already shared.
     * @param roleIds               The list of role IDs requested in the initiating organization.
     * @param roleAssignmentMode    The mode for role assignment reconciliation.
     */
    private void handleExistingSharedAgent(String associatedAgentId, String sharingInitiatedOrgId,
                                           String targetOrgId, List<String> roleIds,
                                           RoleAssignmentMode roleAssignmentMode) {

        try {
            AgentAssociation agentAssociation = getOrganizationAgentSharingService()
                    .getAgentAssociationOfAssociatedAgentByOrgId(associatedAgentId, targetOrgId);
            if (agentAssociation == null) {
                return;
            }

            List<String> currentSharedRoleIds = getCurrentSharedRoleIdsForSharedAgent(agentAssociation);
            if (roleAssignmentMode != RoleAssignmentMode.NONE) {
                List<String> newSharedRoleIds =
                        removeObsoleteRolesAndGetRolesToAdd(agentAssociation, currentSharedRoleIds, roleIds);
                assignRolesIfPresent(agentAssociation, sharingInitiatedOrgId, newSharedRoleIds);
            } else {
                unAssignOldSharedRolesFromSharedAgent(agentAssociation, currentSharedRoleIds);
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException |
                 AgentSharingMgtException e) {
            LOG.error("Error occurred while reconciling roles for the previously shared agent: " +
                    associatedAgentId, e);
        }
    }

    /**
     * Assigns roles to a shared agent if any roles are present.
     *
     * @param agentAssociation      The agent association object containing agent and organization details.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     */
    private void assignRolesIfPresent(AgentAssociation agentAssociation, String sharingInitiatedOrgId,
                                      List<String> roleIds) {

        if (!roleIds.isEmpty()) {
            assignRolesToTheSharedAgent(agentAssociation, sharingInitiatedOrgId, roleIds);
        }
    }

    /**
     * Retrieves the role IDs currently assigned to a shared agent within an organization.
     *
     * @param agentAssociation The agent association object containing agent and organization details.
     * @return A list of role IDs currently assigned to the shared agent in the target organization.
     */
    private List<String> getCurrentSharedRoleIdsForSharedAgent(AgentAssociation agentAssociation)
            throws OrganizationManagementException, AgentSharingMgtException {

        String targetOrgTenantDomain =
                getOrganizationManager().resolveTenantDomain(agentAssociation.getOrganizationId());
        int targetTenantId = IdentityTenantUtil.getTenantId(targetOrgTenantDomain);
        String domainName = IdentityUtil.getAgentIdentityUserstoreName();
        String username = agentAssociation.getAgentId();

        return getOrganizationAgentSharingService().getRolesSharedWithAgentInOrganization(username, targetTenantId,
                domainName);
    }

    /**
     * Determines the roles that need to be added after reconciling the current shared agent roles with the requested
     * role set.
     *
     * @param agentAssociation      The agent association object containing agent and organization details.
     * @param currentRoleIds        The list of role IDs currently assigned to the shared agent in the target
     *                              organization.
     * @param newRoleIdsInParentOrg The list of requested role IDs in the initiating organization.
     * @return A list of role IDs to add in the initiating organization space.
     */
    private List<String> removeObsoleteRolesAndGetRolesToAdd(AgentAssociation agentAssociation,
                                                             List<String> currentRoleIds,
                                                             List<String> newRoleIdsInParentOrg)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String targetOrgTenantDomain =
                getOrganizationManager().resolveTenantDomain(agentAssociation.getOrganizationId());
        Map<String, String> mainToSharedRoleMap =
                getRoleManagementService().getMainRoleToSharedRoleMappingsBySubOrg(newRoleIdsInParentOrg,
                        targetOrgTenantDomain);
        Set<String> newTargetOrgRoleIds = new HashSet<>(mainToSharedRoleMap.values());

        List<String> rolesToBeAdded = new ArrayList<>(newTargetOrgRoleIds);
        rolesToBeAdded.removeAll(currentRoleIds);

        List<String> rolesToBeRemoved = new ArrayList<>(currentRoleIds);
        rolesToBeRemoved.removeAll(newTargetOrgRoleIds);

        unAssignOldSharedRolesFromSharedAgent(agentAssociation, rolesToBeRemoved);

        Map<String, String> sharedToMainRoleMap = new HashMap<>();
        for (Map.Entry<String, String> entry : mainToSharedRoleMap.entrySet()) {
            sharedToMainRoleMap.put(entry.getValue(), entry.getKey());
        }

        List<String> rolesToBeAddedInParentOrg = new ArrayList<>();
        for (String targetOrgRoleId : rolesToBeAdded) {
            rolesToBeAddedInParentOrg.add(sharedToMainRoleMap.getOrDefault(targetOrgRoleId, targetOrgRoleId));
        }
        return rolesToBeAddedInParentOrg;
    }

    /**
     * Assigns the specified roles to the shared agent within the target organization.
     *
     * @param agentAssociation      The agent association object containing agent and organization details.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     */
    private void assignRolesToTheSharedAgent(AgentAssociation agentAssociation, String sharingInitiatedOrgId,
                                             List<String> roleIds) {

        String agentId = agentAssociation.getAgentId();
        String orgId = agentAssociation.getOrganizationId();
        try {
            String sharingInitiatedOrgTenantDomain =
                    getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
            String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            String domainName = IdentityUtil.getAgentIdentityUserstoreName();
            String username = agentAssociation.getAgentId();

            RoleManagementService roleManagementService = getRoleManagementService();
            Map<String, String> sharedRoleToMainRoleMappings =
                    roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(roleIds,
                            sharingInitiatedOrgTenantDomain);

            List<String> mainRoles = new ArrayList<>();
            for (String roleId : roleIds) {
                mainRoles.add(sharedRoleToMainRoleMappings.getOrDefault(roleId, roleId));
            }

            Map<String, String> mainRoleToSharedRoleMappings =
                    roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(mainRoles, targetOrgTenantDomain);

            for (String role : mainRoleToSharedRoleMappings.values()) {
                roleManagementService.updateUserListOfRole(role, Collections.singletonList(agentId),
                        Collections.emptyList(), targetOrgTenantDomain);
                getOrganizationAgentSharingService().addEditRestrictionsForSharedAgentRole(role, username,
                        targetOrgTenantDomain, domainName, EditOperation.DELETE, sharingInitiatedOrgId);
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException |
                 AgentSharingMgtException e) {
            LOG.error("Error occurred while assigning roles to the shared agent: " + agentId, e);
        }
    }

    /**
     * Removes roles that are no longer assigned to the shared agent.
     *
     * @param agentAssociation The agent association object containing agent and organization details.
     * @param rolesToBeRemoved The list of role IDs to be removed.
     */
    private void unAssignOldSharedRolesFromSharedAgent(AgentAssociation agentAssociation, List<String> rolesToBeRemoved)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String agentId = agentAssociation.getAgentId();
        String orgId = agentAssociation.getOrganizationId();
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        Map<String, String> mainRoleToSharedRoleMappings =
                getRoleManagementService().getMainRoleToSharedRoleMappingsBySubOrg(rolesToBeRemoved,
                        targetOrgTenantDomain);

        for (String roleId : mainRoleToSharedRoleMappings.values()) {
            getRoleManagementService().updateUserListOfRole(roleId, Collections.emptyList(),
                    Collections.singletonList(agentId), targetOrgTenantDomain);
        }
    }

    /**
     * Filters the list of organizations to include only immediate child organizations.
     *
     * @param organizations         The list of organizations to be filtered.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing process.
     * @return A list of valid immediate-child organizations.
     */
    private List<SelectiveAgentShareOrgDetailsDO> filterValidOrganizations(
            List<SelectiveAgentShareOrgDetailsDO> organizations, String sharingInitiatedOrgId)
            throws AgentSharingMgtServerException {

        List<String> immediateChildOrgs = getImmediateChildOrgsOfSharingInitiatedOrg(sharingInitiatedOrgId);

        List<SelectiveAgentShareOrgDetailsDO> validOrganizations = organizations.stream()
                .filter(org -> immediateChildOrgs.contains(org.getOrganizationId()))
                .collect(Collectors.toList());

        List<String> skippedOrganizations = organizations.stream()
                .map(SelectiveAgentShareOrgDetailsDO::getOrganizationId)
                .filter(orgId -> !immediateChildOrgs.contains(orgId))
                .collect(Collectors.toList());

        if (!skippedOrganizations.isEmpty() && LOG.isDebugEnabled()) {
            LOG.debug(String.format(LOG_WARN_SKIP_ORG_SHARE_MESSAGE, skippedOrganizations));
        }
        return validOrganizations;
    }

    /**
     * Retrieves the list of immediate child organizations for a given organization.
     *
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @return A list of organization IDs representing the immediate child organizations.
     */
    private List<String> getImmediateChildOrgsOfSharingInitiatedOrg(String sharingInitiatedOrgId)
            throws AgentSharingMgtServerException {

        try {
            return getOrganizationManager().getChildOrganizationsIds(sharingInitiatedOrgId, false);
        } catch (OrganizationManagementException e) {
            String errorMessage = String.format(
                    ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS.getMessage(), sharingInitiatedOrgId);
            throw new AgentSharingMgtServerException(ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS, errorMessage);
        }
    }

    /**
     * Extracts a list of organizations based on the given sharing policy.
     *
     * @param policyHoldingOrgId The ID of the organization holding the policy.
     * @param policy             The sharing policy that determines which organizations to include.
     * @return A list of organization IDs that should be included in the sharing scope.
     */
    private List<String> extractOrgListBasedOnSharingPolicy(String policyHoldingOrgId, PolicyEnum policy)
            throws OrganizationManagementException {

        Set<String> agentSharingOrgList = new HashSet<>();

        switch (policy) {
            case ALL_EXISTING_AND_FUTURE_ORGS:
                agentSharingOrgList.addAll(
                        getOrganizationManager().getChildOrganizationsIds(policyHoldingOrgId, true));
                break;
            case SELECTED_ORG_ONLY:
                agentSharingOrgList.add(policyHoldingOrgId);
                break;
            case SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN:
                agentSharingOrgList.add(policyHoldingOrgId);
                agentSharingOrgList.addAll(
                        getOrganizationManager().getChildOrganizationsIds(policyHoldingOrgId, true));
                break;
            case NO_SHARING:
                break;
            default:
                throw new OrganizationManagementClientException(
                        String.format(ERROR_CODE_INVALID_POLICY.getMessage(), policy.getPolicyName()),
                        ERROR_CODE_INVALID_POLICY.getDescription(),
                        ERROR_CODE_INVALID_POLICY.getCode());
        }

        return new ArrayList<>(agentSharingOrgList);
    }

    // Resource Sharing Policy Management Methods.

    /**
     * Saves a new resource sharing policy for an agent.
     *
     * @param associatedAgentId  The ID of the associated agent.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param policyHoldingOrgId The ID of the organization holding the policy.
     * @param policy             The sharing policy.
     * @param roleIds            The list of role IDs.
     */
    private void saveAgentSharingPolicy(String associatedAgentId, String sharingInitiatedOrgId,
                                        String policyHoldingOrgId, PolicyEnum policy, List<String> roleIds)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicy resourceSharingPolicy =
                new ResourceSharingPolicy.Builder().withResourceType(ResourceType.AGENT)
                        .withResourceId(associatedAgentId)
                        .withInitiatingOrgId(sharingInitiatedOrgId)
                        .withPolicyHoldingOrgId(policyHoldingOrgId)
                        .withSharingPolicy(policy).build();

        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
        for (String roleId : roleIds) {
            SharedResourceAttribute sharedResourceAttribute =
                    new SharedResourceAttribute.Builder().withSharedAttributeType(SharedAttributeType.ROLE)
                            .withSharedAttributeId(roleId).build();
            sharedResourceAttributes.add(sharedResourceAttribute);
        }

        deleteAllResourceSharingPoliciesOfAgent(associatedAgentId, sharingInitiatedOrgId);
        getResourceSharingPolicyHandlerService().addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                sharedResourceAttributes);
    }

    /**
     * Determines whether a given policy scope allows saving the resource sharing policy.
     *
     * @param policy The policy enumeration containing the organization scope.
     * @return True if the policy allows saving, false otherwise.
     */
    private boolean isApplicableOrganizationScopeForSavingPolicy(PolicyEnum policy) {

        return OrganizationScope.EXISTING_ORGS_AND_FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope()) ||
                OrganizationScope.FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope());
    }

    /**
     * Deletes the resource sharing policy for a given agent in an organization.
     *
     * @param policyHoldingOrgId    The ID of the organization holding the policy.
     * @param associatedAgentId     The ID of the associated agent.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteResourceSharingPolicyOfAgentInOrg(String policyHoldingOrgId, String associatedAgentId,
                                                         String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                policyHoldingOrgId, ResourceType.AGENT, associatedAgentId, sharingInitiatedOrgId);
    }

    /**
     * Deletes all resource sharing policies for a given agent.
     *
     * @param associatedAgentId     The ID of the associated agent.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteAllResourceSharingPoliciesOfAgent(String associatedAgentId, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(ResourceType.AGENT,
                associatedAgentId, sharingInitiatedOrgId);
    }

    // Role Management Helper Methods.

    /**
     * Retrieves a list of role IDs based on the provided role and audience details.
     *
     * @param rolesWithAudience     A list of roles with associated audience information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @return A list of role IDs that match the given role-audience combination.
     */
    private List<String> getRoleIds(List<RoleWithAudienceDO> rolesWithAudience, String sharingInitiatedOrgId)
            throws AgentSharingMgtException {

        try {
            String sharingInitiatedTenantDomain = getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
            List<String> list = new ArrayList<>();
            for (RoleWithAudienceDO roleWithAudienceDO : rolesWithAudience) {
                String audienceId =
                        getAudienceId(roleWithAudienceDO, sharingInitiatedOrgId, sharingInitiatedTenantDomain);
                Optional<String> roleId =
                        getRoleIdFromAudience(roleWithAudienceDO.getRoleName(), roleWithAudienceDO.getAudienceType(),
                                audienceId, sharingInitiatedTenantDomain);
                roleId.ifPresent(list::add);
            }
            return list;
        } catch (OrganizationManagementException e) {
            throw new AgentSharingMgtServerException(ERROR_CODE_GET_ROLE_IDS);
        }
    }

    /**
     * Determines the audience ID based on the role's audience type.
     *
     * @param role          The role with audience details.
     * @param originalOrgId The ID of the organization where the role is being shared.
     * @param tenantDomain  The tenant domain associated with the organization.
     * @return The audience ID associated with the role.
     */
    private String getAudienceId(RoleWithAudienceDO role, String originalOrgId, String tenantDomain) {

        if (role == null || role.getAudienceType() == null) {
            return null;
        }

        try {
            if (StringUtils.equalsIgnoreCase(ORGANIZATION, role.getAudienceType())) {
                return originalOrgId;
            }
            if (StringUtils.equalsIgnoreCase(APPLICATION, role.getAudienceType())) {
                return getApplicationResourceId(role.getAudienceName(), tenantDomain);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format(ERROR_CODE_INVALID_AUDIENCE_TYPE.getDescription(), role.getAudienceType()));
            }
        } catch (IdentityApplicationManagementException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), role.getAudienceName()));
            }
        }
        return null;
    }

    /**
     * Retrieves the resource ID of an application based on its name within the specified tenant domain.
     *
     * @param audienceName The name of the application.
     * @param tenantDomain The tenant domain where the application exists.
     * @return The resource ID of the application, or null if not found.
     */
    private String getApplicationResourceId(String audienceName, String tenantDomain)
            throws IdentityApplicationManagementException {

        ApplicationBasicInfo applicationBasicInfo = getApplicationManagementService()
                .getApplicationBasicInfoByName(audienceName, tenantDomain);
        if (applicationBasicInfo != null) {
            return applicationBasicInfo.getApplicationResourceId();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), audienceName));
        }
        return null;
    }

    /**
     * Retrieves the role ID associated with a given role name and audience within a specific tenant domain.
     *
     * @param roleName     The name of the role.
     * @param audienceType The type of audience.
     * @param audienceId   The audience ID.
     * @param tenantDomain The tenant domain where the role exists.
     * @return An Optional containing the role ID if found, otherwise empty.
     */
    private Optional<String> getRoleIdFromAudience(String roleName, String audienceType, String audienceId,
                                                   String tenantDomain) {

        if (audienceId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    getRoleManagementService().getRoleIdByName(roleName, audienceType, audienceId, tenantDomain));
        } catch (IdentityRoleManagementException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format(ERROR_CODE_ROLE_NOT_FOUND.getMessage(), roleName, audienceType, audienceId));
            }
            return Optional.empty();
        }
    }

    private List<RoleWithAudienceDO> castToRoleWithAudienceList(Object values) {

        List<RoleWithAudienceDO> roleWithAudienceList = new ArrayList<>();
        if (values instanceof List<?>) {
            for (Object value : (List<?>) values) {
                if (value instanceof RoleWithAudienceDO) {
                    RoleWithAudienceDO roleWithAudience = new RoleWithAudienceDO();
                    roleWithAudience.setRoleName(((RoleWithAudienceDO) value).getRoleName());
                    roleWithAudience.setAudienceName(((RoleWithAudienceDO) value).getAudienceName());
                    roleWithAudience.setAudienceType(((RoleWithAudienceDO) value).getAudienceType());
                    roleWithAudienceList.add(roleWithAudience);
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sized role with audience list: " + roleWithAudienceList.size());
        }
        return roleWithAudienceList;
    }

    // Async helpers.

    /**
     * Restores thread-local properties for async execution.
     *
     * @param tenantDomain         Tenant domain.
     * @param tenantId             Tenant ID.
     * @param username             Username.
     * @param threadLocalProperties Thread-local properties to restore.
     */
    private void initiateThreadLocalContext(String tenantDomain, int tenantId, String username,
                                            Map<String, Object> threadLocalProperties) {

        // startTenantFlow() is called here to push a new context frame. The caller is responsible for
        // always calling endTenantFlow() in a finally block to guarantee the frame is popped.
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(tenantDomain, true);
        carbonContext.setTenantId(tenantId);
        carbonContext.setUsername(username);
        IdentityUtil.threadLocalProperties.get().putAll(threadLocalProperties);
    }

    private void logAsyncProcessing(String action, String sharingInitiatedUserId, String sharingInitiatedOrgId) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(ASYNC_PROCESSING_LOG_TEMPLATE, action, sharingInitiatedUserId,
                    sharingInitiatedOrgId));
        }
    }

    // Input Validation Methods.

    private <T extends AgentCriteriaType> void validateAgentShareInput(
            org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.BaseAgentShareDO<T>
                    baseAgentShareDO) throws AgentSharingMgtClientException {

        if (baseAgentShareDO == null) {
            throwValidationException(ERROR_CODE_NULL_SHARE);
        }

        if (baseAgentShareDO instanceof SelectiveAgentShareDO) {
            validateSelectiveAgentShareDO((SelectiveAgentShareDO) baseAgentShareDO);
        } else if (baseAgentShareDO instanceof GeneralAgentShareDO) {
            validateGeneralAgentShareDO((GeneralAgentShareDO) baseAgentShareDO);
        }
    }

    private void validateSelectiveAgentShareDO(SelectiveAgentShareDO selectiveAgentShareDO)
            throws AgentSharingMgtClientException {

        validateNotNull(selectiveAgentShareDO.getAgentCriteria(), ERROR_CODE_AGENT_CRITERIA_INVALID);
        if (!selectiveAgentShareDO.getAgentCriteria().containsKey(AGENT_IDS) ||
                selectiveAgentShareDO.getAgentCriteria().get(AGENT_IDS) == null) {
            throwValidationException(ERROR_CODE_AGENT_CRITERIA_MISSING);
        }
        validateNotNull(selectiveAgentShareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        for (SelectiveAgentShareOrgDetailsDO orgDetails : selectiveAgentShareDO.getOrganizations()) {
            validateNotNull(orgDetails, ERROR_CODE_ORG_DETAILS_NULL);
            validateNotNull(orgDetails.getOrganizationId(), ERROR_CODE_ORG_ID_NULL);
            validateNotNull(orgDetails.getPolicy(), ERROR_CODE_POLICY_NULL);
            validateRoleAssignments(orgDetails.getRoleAssignments());
        }

        LOG.debug("Validated selective agent share DO successfully.");
    }

    private void validateGeneralAgentShareDO(GeneralAgentShareDO generalAgentShareDO)
            throws AgentSharingMgtClientException {

        validateNotNull(generalAgentShareDO.getAgentCriteria(), ERROR_CODE_AGENT_CRITERIA_INVALID);
        if (!generalAgentShareDO.getAgentCriteria().containsKey(AGENT_IDS) ||
                generalAgentShareDO.getAgentCriteria().get(AGENT_IDS) == null) {
            throwValidationException(ERROR_CODE_AGENT_CRITERIA_MISSING);
        }
        validateNotNull(generalAgentShareDO.getPolicy(), ERROR_CODE_POLICY_NULL);
        validateRoleAssignments(generalAgentShareDO.getRoleAssignments());

        LOG.debug("Validated general agent share DO successfully.");
    }

    private <T extends AgentCriteriaType> void validateAgentUnshareInput(
            org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos.BaseAgentUnshareDO<T>
                    agentUnshareDO) throws AgentSharingMgtClientException {

        if (agentUnshareDO == null) {
            throwValidationException(ERROR_CODE_NULL_UNSHARE);
        }

        if (agentUnshareDO instanceof SelectiveAgentUnshareDO) {
            validateSelectiveAgentUnshareDO((SelectiveAgentUnshareDO) agentUnshareDO);
        } else if (agentUnshareDO instanceof GeneralAgentUnshareDO) {
            validateGeneralAgentUnshareDO((GeneralAgentUnshareDO) agentUnshareDO);
        }
    }

    private void validateSelectiveAgentUnshareDO(SelectiveAgentUnshareDO selectiveAgentUnshareDO)
            throws AgentSharingMgtClientException {

        validateNotNull(selectiveAgentUnshareDO.getAgentCriteria(), ERROR_CODE_AGENT_CRITERIA_INVALID);
        if (!selectiveAgentUnshareDO.getAgentCriteria().containsKey(AGENT_IDS) ||
                selectiveAgentUnshareDO.getAgentCriteria().get(AGENT_IDS) == null) {
            throwValidationException(ERROR_CODE_AGENT_CRITERIA_MISSING);
        }
        validateNotNull(selectiveAgentUnshareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);
        for (String organization : selectiveAgentUnshareDO.getOrganizations()) {
            validateNotNull(organization, ERROR_CODE_ORG_ID_NULL);
        }
        LOG.debug("Validated selective agent unshare DO successfully.");
    }

    private void validateGeneralAgentUnshareDO(GeneralAgentUnshareDO generalAgentUnshareDO)
            throws AgentSharingMgtClientException {

        validateNotNull(generalAgentUnshareDO.getAgentCriteria(), ERROR_CODE_AGENT_CRITERIA_INVALID);
        if (!generalAgentUnshareDO.getAgentCriteria().containsKey(AGENT_IDS) ||
                generalAgentUnshareDO.getAgentCriteria().get(AGENT_IDS) == null) {
            throwValidationException(ERROR_CODE_AGENT_CRITERIA_MISSING);
        }
        LOG.debug("Validated general agent unshare DO successfully.");
    }

    private void validateSharedAgentAttributeUpdateInput(AgentSharePatchDO agentSharePatchDO)
            throws AgentSharingMgtClientException {

        validateNotNull(agentSharePatchDO, ERROR_CODE_REQUEST_BODY_NULL);
        validateNotNull(agentSharePatchDO.getAgentCriteria(), ERROR_CODE_AGENT_CRITERIA_INVALID);
        if (!agentSharePatchDO.getAgentCriteria().containsKey(AGENT_IDS) ||
                agentSharePatchDO.getAgentCriteria().get(AGENT_IDS) == null) {
            throwValidationException(ERROR_CODE_AGENT_CRITERIA_MISSING);
        }
        validateNotNull(agentSharePatchDO.getPatchOperations(), ERROR_CODE_PATCH_OPERATIONS_NULL);

        for (PatchOperationDO patchOperation : agentSharePatchDO.getPatchOperations()) {
            validateNotNull(patchOperation, ERROR_CODE_PATCH_OPERATION_NULL);
            validateNotNull(patchOperation.getOperation(), ERROR_CODE_PATCH_OPERATION_OP_NULL);
            validatePatchOperationValue(patchOperation.getOperation());
            validateNotNull(patchOperation.getPath(), ERROR_CODE_PATCH_OPERATION_PATH_NULL);
            String pathType = resolveAndValidatePatchPath(patchOperation.getPath());
            validateNotNull(patchOperation.getValues(), ERROR_CODE_PATCH_OPERATION_VALUE_NULL);
            validatePatchValuesAgainstPath(pathType, patchOperation.getValues());
        }

        LOG.debug("Validated shared agent attribute update input successfully.");
    }

    private void validateSharedAgentGetInput(GetAgentSharedOrgsDO getAgentSharedOrgsDO)
            throws AgentSharingMgtClientException {

        validateNotNull(getAgentSharedOrgsDO, ERROR_CODE_REQUEST_BODY_NULL);
        validateNotNull(getAgentSharedOrgsDO.getAgentId(), ERROR_CODE_ORG_ID_NULL);
        validateNotNull(getAgentSharedOrgsDO.getParentOrgId(), ERROR_CODE_ORG_ID_NULL);
        validateGetAttributes(getAgentSharedOrgsDO.getAttributes());
    }

    private void validateGetAttributes(List<String> attributes) throws AgentSharingMgtClientException {

        validateNotNull(attributes, ERROR_CODE_GET_ATTRIBUTES_NULL);
        for (String attribute : attributes) {
            validateNotNull(attribute, ERROR_CODE_GET_ATTRIBUTE_NULL);
            if (!SUPPORTED_GET_ATTRIBUTES.contains(attribute)) {
                throwValidationException(ERROR_CODE_GET_ATTRIBUTE_UNSUPPORTED);
            }
        }
    }

    private void validatePatchOperationValue(AgentSharePatchOperation operation) throws AgentSharingMgtClientException {

        if (operation != AgentSharePatchOperation.ADD && operation != AgentSharePatchOperation.REMOVE) {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_OP_INVALID);
        }
    }

    private String resolveAndValidatePatchPath(String path) throws AgentSharingMgtClientException {

        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_PATH_INVALID);
        }

        if (isPatchOperationPathRoles(trimmed)) {
            validateOrgRolesPath(trimmed);
            return PATCH_PATH_ROLES;
        }

        throwValidationException(ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED);
        return PATCH_PATH_NONE;
    }

    private boolean isPatchOperationPathRoles(String path) {

        return path.startsWith(PATCH_PATH_PREFIX) && path.endsWith(PATCH_PATH_SUFFIX_ROLES);
    }

    private void validateOrgRolesPath(String path) throws AgentSharingMgtClientException {

        String orgId = extractOrgIdFromRolesPath(path);
        if (orgId.isEmpty()) {
            throwValidationException(ERROR_CODE_ORG_ID_NULL);
        }
        validateUuid(orgId);
    }

    private String extractOrgIdFromRolesPath(String path) {

        return path.substring(PATCH_PATH_PREFIX.length(), path.length() - PATCH_PATH_SUFFIX_ROLES.length()).trim();
    }

    private void validatePatchValuesAgainstPath(String pathType, Object values) throws AgentSharingMgtClientException {

        if (StringUtils.equals(pathType, PATCH_PATH_ROLES)) {
            validateRoleWithAudienceValues(values);
        } else {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED);
        }
    }

    private void validateRoleWithAudienceValues(Object values) throws AgentSharingMgtClientException {

        if (!(values instanceof List)) {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_VALUE_TYPE_INVALID);
        }

        List<?> list = (List<?>) values;
        for (Object item : list) {
            if (!(item instanceof RoleWithAudienceDO)) {
                throwValidationException(ERROR_CODE_PATCH_OPERATION_ROLES_VALUE_CONTENT_INVALID);
            }
            RoleWithAudienceDO role = (RoleWithAudienceDO) item;
            validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
            validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
            validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
        }
    }

    private void validateRoleAssignments(RoleAssignmentDO roleAssignments) throws AgentSharingMgtClientException {

        validateNotNull(roleAssignments, ERROR_CODE_ROLES_NULL);
        validateNotNull(roleAssignments.getMode(), ERROR_CODE_ROLES_NULL);

        if (RoleAssignmentMode.SELECTED == roleAssignments.getMode()) {
            validateNotNull(roleAssignments.getRoles(), ERROR_CODE_ROLES_NULL);
        }

        if (roleAssignments.getRoles() != null) {
            for (RoleWithAudienceDO role : roleAssignments.getRoles()) {
                validateNotNull(role.getRoleName(), ERROR_CODE_ROLE_NAME_NULL);
                validateNotNull(role.getAudienceName(), ERROR_CODE_AUDIENCE_NAME_NULL);
                validateNotNull(role.getAudienceType(), ERROR_CODE_AUDIENCE_TYPE_NULL);
            }
        }
    }

    private void validateUuid(String uuid) throws AgentSharingMgtClientException {

        if (uuid == null || uuid.trim().isEmpty()) {
            throwValidationException(ERROR_CODE_ORG_ID_NULL);
        }

        try {
            java.util.UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throwValidationException(ERROR_CODE_ORG_ID_INVALID_FORMAT);
        }
    }

    private void validateNotNull(Object obj, AgentSharingConstants.ErrorMessage error)
            throws AgentSharingMgtClientException {

        if (obj == null) {
            throwValidationException(error);
        }
    }

    private void throwValidationException(AgentSharingConstants.ErrorMessage error)
            throws AgentSharingMgtClientException {

        throw new AgentSharingMgtClientException(error.getCode(), error.getMessage(), error.getDescription());
    }

    // Service getters.

    private OrganizationAgentSharingService getOrganizationAgentSharingService() {

        return OrganizationAgentSharingDataHolder.getInstance().getOrganizationAgentSharingService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationAgentSharingDataHolder.getInstance().getOrganizationManager();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationAgentSharingDataHolder.getInstance().getRoleManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationAgentSharingDataHolder.getInstance().getApplicationManagementService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationAgentSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }
}
