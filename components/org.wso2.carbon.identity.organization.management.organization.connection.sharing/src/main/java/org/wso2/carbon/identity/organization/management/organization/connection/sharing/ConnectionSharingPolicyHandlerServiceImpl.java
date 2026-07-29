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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.association.model.ConnectionAssociation;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.ConnectionSharingModeDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GeneralConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GeneralConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GetConnectionSharedOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.ResponseConnectionOrgDetailsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.ResponseSharedConnectionOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionShareOrgConfigDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.handler.ConnectionTypeHandler;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.ConnectionSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.util.ConnectionSharingInitiatorContext;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ACTION_GENERAL_CONNECTION_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ACTION_GENERAL_CONNECTION_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ACTION_SELECTIVE_CONNECTION_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ACTION_SELECTIVE_CONNECTION_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ASYNC_PROCESSING_LOG_TEMPLATE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_CONNECTION_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_CONNECTION_TYPE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_GET_CHILD_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_GET_SHARED_CONNECTIONS;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_NO_HANDLER_FOR_CONNECTION_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_NULL_INPUT;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_UNSUPPORTED_GET_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.ErrorMessage.ERROR_CODE_UNSUPPORTED_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionSharingConstants.SHARING_MODE_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.AND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ASC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.DESC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ATTRIBUTES_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ATTRIBUTES_FIELD_PREFIX;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_NAME_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_AFTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_BEFORE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PARENT_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;

/**
 * Implementation of {@link ConnectionSharingPolicyHandlerService}.
 * Handles policy persistence and org-scope resolution for connection sharing operations, dispatching the
 * resource-type specific work to the {@link ConnectionTypeHandler} resolved for the request's
 * {@link ResourceType}. Deep shadow-resource creation/propagation internals are deferred to the handler
 * implementations.
 */
public class ConnectionSharingPolicyHandlerServiceImpl implements ConnectionSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(ConnectionSharingPolicyHandlerServiceImpl.class);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);
    private static final Set<String> SUPPORTED_GET_ATTRIBUTES = Collections.singleton(SHARING_MODE_ATTRIBUTE);

    @Override
    public void populateSelectiveConnectionShare(SelectiveConnectionShareDTO dto)
            throws ConnectionSharingMgtException {

        LOG.debug("Starting selective connection share operation.");

        validateSelectiveConnectionShareInput(dto);

        ConnectionSharingInitiatorContext ctx = ConnectionSharingInitiatorContext.capture();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        String connectionId = dto.getConnectionId();
        ConnectionTypeHandler handler = resolveConnectionTypeHandler(dto.getResourceType());
        handler.validateConnectionShareEligibility(connectionId, ctx.getSharingInitiatedOrgId());
        List<SelectiveConnectionShareOrgConfigDTO> validOrgs = filterValidOrganizations(dto.getOrganizations(),
                ctx.getSharingInitiatedOrgId());

        CompletableFuture.runAsync(() -> {
            logAsyncProcessing(ACTION_SELECTIVE_CONNECTION_SHARE, ctx.getSharingInitiatedUserId(),
                    ctx.getSharingInitiatedOrgId());
            try {
                initiateThreadLocalContext(ctx, threadLocalProperties);
                processSelectiveConnectionShare(connectionId, handler, validOrgs, ctx.getSharingInitiatedOrgId());
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, EXECUTOR).exceptionally(ex -> {
            LOG.error("Error occurred during async selective connection share processing.", ex);
            return null;
        });
    }

    @Override
    public void populateGeneralConnectionShare(GeneralConnectionShareDTO dto)
            throws ConnectionSharingMgtException {

        LOG.debug("Starting general connection share operation.");

        validateGeneralConnectionShareInput(dto);

        ConnectionSharingInitiatorContext ctx = ConnectionSharingInitiatorContext.capture();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        String connectionId = dto.getConnectionId();
        ConnectionTypeHandler handler = resolveConnectionTypeHandler(dto.getResourceType());
        handler.validateConnectionShareEligibility(connectionId, ctx.getSharingInitiatedOrgId());

        CompletableFuture.runAsync(() -> {
            logAsyncProcessing(ACTION_GENERAL_CONNECTION_SHARE, ctx.getSharingInitiatedUserId(),
                            ctx.getSharingInitiatedOrgId());
            try {
                initiateThreadLocalContext(ctx, threadLocalProperties);
                processGeneralConnectionShare(connectionId, handler, dto.getPolicy(), ctx.getSharingInitiatedOrgId());
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, EXECUTOR).exceptionally(ex -> {
            LOG.error("Error occurred during async general connection share processing.", ex);
            return null;
        });
    }

    @Override
    public void populateSelectiveConnectionUnshare(SelectiveConnectionUnshareDTO dto)
            throws ConnectionSharingMgtException {

        LOG.debug("Starting selective connection unshare operation.");

        validateSelectiveConnectionUnshareInput(dto);

        ConnectionSharingInitiatorContext ctx = ConnectionSharingInitiatorContext.capture();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        String connectionId = dto.getConnectionId();
        ConnectionTypeHandler handler = resolveConnectionTypeHandler(dto.getResourceType());

        CompletableFuture.runAsync(() -> {
            logAsyncProcessing(ACTION_SELECTIVE_CONNECTION_UNSHARE, ctx.getSharingInitiatedUserId(),
                    ctx.getSharingInitiatedOrgId());
            try {
                initiateThreadLocalContext(ctx, threadLocalProperties);
                processSelectiveConnectionUnshare(connectionId, handler, dto.getOrgIds(),
                        ctx.getSharingInitiatedOrgId());
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, EXECUTOR)
        .exceptionally(ex -> {
            LOG.error("Error occurred during async selective connection unshare processing.", ex);
            return null;
        });
    }

    @Override
    public void populateGeneralConnectionUnshare(GeneralConnectionUnshareDTO dto)
            throws ConnectionSharingMgtException {

        LOG.debug("Starting general connection unshare operation.");

        validateGeneralConnectionUnshareInput(dto);

        ConnectionSharingInitiatorContext ctx = ConnectionSharingInitiatorContext.capture();
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        String connectionId = dto.getConnectionId();
        ConnectionTypeHandler handler = resolveConnectionTypeHandler(dto.getResourceType());

        CompletableFuture.runAsync(() -> {
            logAsyncProcessing(ACTION_GENERAL_CONNECTION_UNSHARE, ctx.getSharingInitiatedUserId(),
                    ctx.getSharingInitiatedOrgId());
            try {
                initiateThreadLocalContext(ctx, threadLocalProperties);
                processGeneralConnectionUnshare(connectionId, handler, ctx.getSharingInitiatedOrgId());
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, EXECUTOR)
        .exceptionally(ex -> {
            LOG.error("Error occurred during async general connection unshare processing.", ex);
            return null;
        });
    }

    @Override
    public ResponseSharedConnectionOrgsDTO getConnectionSharedOrganizations(GetConnectionSharedOrgsDTO dto)
            throws ConnectionSharingMgtException {

        LOG.debug("Starting get connection shared organizations operation.");

        validateGetConnectionSharedOrgsInput(dto);

        int limit = dto.getLimit();
        int beforeCursor = dto.getBefore();
        int afterCursor = dto.getAfter();
        boolean recursive = dto.isRecursive();
        String filter = dto.getFilter();
        String connectionId = dto.getConnectionId();
        String initiatingOrgId = dto.getInitiatingOrgId();
        List<String> attributes = dto.getAttributes();
        ConnectionTypeHandler handler = resolveConnectionTypeHandler(dto.getResourceType());
        ResourceType resourceType = handler.getResourceType();
        List<ResponseConnectionOrgDetailsDTO> sharedOrgsList = new ArrayList<>();

        try {
            String sortOrder = (beforeCursor != 0) ? ASC_SORT_ORDER : DESC_SORT_ORDER;
            List<ExpressionNode> expressionNodes = getExpressionNodes(filter, afterCursor, beforeCursor);
            String parentOrgId = resolveParentOrgId(expressionNodes, initiatingOrgId);
            List<String> childOrgIds = getOrganizationManager().getChildOrganizationsIds(parentOrgId, recursive);

            ConnectionSharingModeDTO generalSharingMode = null;
            if (attributes.contains(SHARING_MODE_ATTRIBUTE)) {
                generalSharingMode = resolveGeneralSharingMode(parentOrgId, connectionId, resourceType);
            }
            boolean includeGeneralSharingMode = (generalSharingMode != null);

            int fetchLimit = (limit == 0) ? limit : limit + 1;
            List<ConnectionAssociation> connectionAssociations = handler.getConnectionAssociations(
                    connectionId, parentOrgId, childOrgIds, expressionNodes, sortOrder, fetchLimit);

            if (CollectionUtils.isEmpty(connectionAssociations)) {
                return buildEmptyResponseToGet(generalSharingMode);
            }

            boolean hasMoreItems = (limit != 0) && (connectionAssociations.size() > limit);
            if (hasMoreItems) {
                connectionAssociations.removeLast();
            }

            if (beforeCursor != 0) {
                Collections.reverse(connectionAssociations);
            }

            for (ConnectionAssociation connectionAssociation : connectionAssociations) {
                sharedOrgsList.add(resolveConnectionOrgDetails(connectionAssociation, attributes));
            }
            if (!includeGeneralSharingMode && attributes.contains(SHARING_MODE_ATTRIBUTE)) {
                int parentOrgDepth = getOrganizationManager().getOrganizationDepthInHierarchy(parentOrgId);
                List<ResponseConnectionOrgDetailsDTO> directChildOrgs = sharedOrgsList.stream()
                        .filter(orgDetails -> orgDetails.getDepthFromRoot() - parentOrgDepth == 1)
                        .toList();
                for (ResponseConnectionOrgDetailsDTO orgDetails : directChildOrgs) {
                    ConnectionSharingModeDTO sharingMode = resolveSelectiveSharingMode(
                            parentOrgId, connectionId, orgDetails.getOrgId(), resourceType);
                    orgDetails.setSharingMode(sharingMode);
                }
            }

            return buildResponseWithCursors(sharedOrgsList, connectionAssociations, generalSharingMode,
                    beforeCursor, afterCursor, hasMoreItems);
        } catch (OrganizationManagementException e) {
            throw new ConnectionSharingMgtServerException(ERROR_CODE_GET_SHARED_CONNECTIONS.getCode(),
                    ERROR_CODE_GET_SHARED_CONNECTIONS.getMessage(),
                    ERROR_CODE_GET_SHARED_CONNECTIONS.getDescription(), e);
        }
    }

    private void processSelectiveConnectionShare(String connectionId, ConnectionTypeHandler handler,
                                                 List<SelectiveConnectionShareOrgConfigDTO> organizations,
                                                 String initiatingOrgId) {

        try {
            for (SelectiveConnectionShareOrgConfigDTO orgConfig : organizations) {
                LOG.debug("Processing selective connection share for connection: " + connectionId + " to organization: "
                        + orgConfig.getOrgId() + " with policy: " + orgConfig.getPolicy());
                saveConnectionSharingPolicy(connectionId, orgConfig.getOrgId(), orgConfig.getPolicy(),
                        initiatingOrgId, handler.getResourceType());
                handler.shareConnectionToOrg(connectionId, orgConfig.getOrgId(), orgConfig.getPolicy(),
                        initiatingOrgId);
                // When the selected organization is shared with its children, also share with its existing
                // descendants now (future children are materialized on organization creation).
                if (orgConfig.getPolicy() == PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN) {
                    handler.shareConnectionToExistingChildOrgs(connectionId, orgConfig.getOrgId(),
                            orgConfig.getPolicy(), initiatingOrgId);
                }
            }
        } catch (ResourceSharingPolicyMgtException | ConnectionSharingMgtException e) {
            LOG.error("Error occurred while processing selective connection share for connection: " + connectionId,
                    e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed selective connection share initiated from " + initiatingOrgId + ".");
        }
    }

    private void processGeneralConnectionShare(String connectionId, ConnectionTypeHandler handler,
                                               PolicyEnum policy, String initiatingOrgId) {

        try {
            deleteExistingSharingPoliciesOfConnection(connectionId, initiatingOrgId, handler.getResourceType());
            saveConnectionSharingPolicy(connectionId, initiatingOrgId, policy, initiatingOrgId,
                    handler.getResourceType());
            handler.shareConnectionToAllOrgs(connectionId, policy, initiatingOrgId);
        } catch (ResourceSharingPolicyMgtException | ConnectionSharingMgtException e) {
            LOG.error("Error occurred while processing general connection share for connection: " + connectionId, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed general connection share initiated from " + initiatingOrgId + ".");
        }
    }

    private void processSelectiveConnectionUnshare(String connectionId, ConnectionTypeHandler handler,
                                                   List<String> orgIds, String initiatingOrgId) {

        try {
            for (String orgId : orgIds) {
                deleteResourceSharingPolicyOfConnectionInOrg(orgId, connectionId, initiatingOrgId,
                        handler.getResourceType());
                handler.unshareConnectionFromOrg(connectionId, orgId, initiatingOrgId);
            }
        } catch (ResourceSharingPolicyMgtException | ConnectionSharingMgtException e) {
            LOG.error("Error occurred while processing selective connection unshare for connection: " +
                    connectionId, e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed selective connection unshare initiated from " + initiatingOrgId + ".");
        }
    }

    private void processGeneralConnectionUnshare(String connectionId, ConnectionTypeHandler handler,
                                                 String initiatingOrgId) {

        try {
            deleteExistingSharingPoliciesOfConnection(connectionId, initiatingOrgId, handler.getResourceType());
            handler.unshareConnectionFromAllOrgs(connectionId, initiatingOrgId);
        } catch (ResourceSharingPolicyMgtException | ConnectionSharingMgtException e) {
            LOG.error("Error occurred while processing general connection unshare for connection: " + connectionId,
                    e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Completed general connection unshare initiated from " + initiatingOrgId + ".");
        }
    }

    private ConnectionSharingModeDTO resolveGeneralSharingMode(String parentOrgId, String connectionId,
                                                               ResourceType resourceType)
            throws OrganizationManagementException {

        try {
            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> result =
                    getResourceSharingPolicyHandlerService().getResourceSharingPolicyAndAttributesByInitiatingOrgId(
                            parentOrgId, resourceType.name(), connectionId);

            if (result != null && result.size() == 1) {
                ResourceSharingPolicy resourceSharingPolicy = result.entrySet().iterator().next().getKey();
                if (resourceSharingPolicy.getSharingPolicy() == PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS) {
                    ConnectionSharingModeDTO modeDTO = new ConnectionSharingModeDTO();
                    modeDTO.setPolicy(resourceSharingPolicy.getSharingPolicy());
                    return modeDTO;
                }
            }
        } catch (ResourceSharingPolicyMgtException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        }
        return null;
    }

    private ConnectionSharingModeDTO resolveSelectiveSharingMode(String initiatingOrgId, String connectionId,
                                                                 String subOrgId, ResourceType resourceType)
            throws OrganizationManagementException {

        try {
            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> result =
                    getResourceSharingPolicyHandlerService().getResourceSharingPolicyAndAttributesByInitiatingOrgId(
                            initiatingOrgId, resourceType.name(), connectionId);

            if (result != null && !result.isEmpty()) {
                for (Map.Entry<ResourceSharingPolicy, List<SharedResourceAttribute>> entry : result.entrySet()) {
                    ResourceSharingPolicy policy = entry.getKey();
                    if (subOrgId.equals(policy.getPolicyHoldingOrgId()) &&
                            policy.getSharingPolicy() ==
                                    PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN) {
                        ConnectionSharingModeDTO modeDTO = new ConnectionSharingModeDTO();
                        modeDTO.setPolicy(policy.getSharingPolicy());
                        return modeDTO;
                    }
                }
            }
        } catch (ResourceSharingPolicyMgtException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        }
        return null;
    }

    private String resolveParentOrgId(List<ExpressionNode> expressionNodes, String initiatingOrgId)
            throws OrganizationManagementException {

        Optional<String> optionalParentId = removeAndGetOrganizationIdFromTheExpressionNodeList(expressionNodes);
        if (optionalParentId.isPresent()) {
            return optionalParentId.get();
        }
        Optional<String> optionalOrgName = removeAndGetOrganizationNameFromTheExpressionNodeList(expressionNodes);
        if (optionalOrgName.isPresent()) {
            return getOrganizationManager().getOrganizationIdByName(optionalOrgName.get());
        }
        return initiatingOrgId;
    }

    private ResponseSharedConnectionOrgsDTO buildEmptyResponseToGet(ConnectionSharingModeDTO sharingMode) {

        ResponseSharedConnectionOrgsDTO response = new ResponseSharedConnectionOrgsDTO();
        response.setSharingMode(sharingMode);
        response.setSharedOrgs(Collections.emptyList());
        response.setNextPageCursor(0);
        response.setPreviousPageCursor(0);
        return response;
    }

    private ResponseConnectionOrgDetailsDTO resolveConnectionOrgDetails(
            ConnectionAssociation connectionAssociation, List<String> attributes)
            throws OrganizationManagementException {

        ResponseConnectionOrgDetailsDTO details = new ResponseConnectionOrgDetailsDTO();
        Organization org = getOrganizationManager().getOrganization(
                connectionAssociation.getOrganizationId(), true, false);
        String tenantDomain = getOrganizationManager().resolveTenantDomain(org.getId());

        details.setOrgId(org.getId());
        details.setOrgName(org.getName());
        details.setOrgHandle(org.getOrganizationHandle());
        details.setStatus(org.getStatus() != null ? org.getStatus().toString() : null);
        details.setOrgRef("/t/" + tenantDomain + "/api/server/v1/organizations/" + org.getId());
        details.setHasChildren(org.hasChildren());
        details.setDepthFromRoot(getOrganizationManager().getOrganizationDepthInHierarchy(org.getId()));
        details.setParentOrgId(org.getParent() != null ? org.getParent().getId() : null);
        details.setParentConnectionId(connectionAssociation.getParentConnectionId());
        details.setSharedConnectionId(connectionAssociation.getSharedConnectionId());
        return details;
    }

    private ResponseSharedConnectionOrgsDTO buildResponseWithCursors(
            List<ResponseConnectionOrgDetailsDTO> sharedOrgsList, List<ConnectionAssociation> connectionAssociations,
            ConnectionSharingModeDTO generalSharingMode, int before, int after, boolean hasMoreItems) {

        ResponseSharedConnectionOrgsDTO response = new ResponseSharedConnectionOrgsDTO();
        response.setSharedOrgs(sharedOrgsList);
        response.setSharingMode(generalSharingMode);

        int nextToken = 0;
        int previousToken = 0;

        // Is First Page? (No cursors provided OR provided 'before' but hit the start)
        boolean isFirstPage = (before == 0 && after == 0) || (before != 0 && !hasMoreItems);

        // Is Last Page? (No more items found AND (provided 'after' OR provided 'before' which hit the end))
        boolean isLastPage = !hasMoreItems && (after != 0 || before == 0);

        if (!isFirstPage && !connectionAssociations.isEmpty()) {
            previousToken = connectionAssociations.get(0).getId();
        }

        if (!isLastPage && !connectionAssociations.isEmpty()) {
            nextToken = connectionAssociations.get(connectionAssociations.size() - 1).getId();
        }

        response.setNextPageCursor(nextToken);
        response.setPreviousPageCursor(previousToken);

        return response;
    }

    private List<ExpressionNode> getExpressionNodes(String filter, int after, int before)
            throws OrganizationManagementClientException {

        List<ExpressionNode> expressionNodes = new ArrayList<>();
        if (StringUtils.isBlank(filter)) {
            filter = StringUtils.EMPTY;
        }
        String paginatedFilter = getPaginatedFilterForDescendingOrder(filter, after, before);
        try {
            if (StringUtils.isNotBlank(paginatedFilter)) {
                FilterTreeBuilder filterTreeBuilder = new FilterTreeBuilder(paginatedFilter);
                Node rootNode = filterTreeBuilder.buildTree();
                setExpressionNodeList(rootNode, expressionNodes);
            }
        } catch (IOException | IdentityException e) {
            throw handleClientException(ERROR_CODE_INVALID_FILTER_FORMAT);
        }
        return expressionNodes;
    }

    private void setExpressionNodeList(Node node, List<ExpressionNode> expression)
            throws OrganizationManagementClientException {

        if (node instanceof ExpressionNode) {
            ExpressionNode expressionNode = (ExpressionNode) node;
            String attributeValue = expressionNode.getAttributeValue();
            if (StringUtils.isNotBlank(attributeValue)) {
                if (attributeValue.startsWith(ORGANIZATION_ATTRIBUTES_FIELD_PREFIX)) {
                    attributeValue = ORGANIZATION_ATTRIBUTES_FIELD;
                }
                if (isFilteringAttributeNotSupported(attributeValue)) {
                    throw handleClientException(ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE, attributeValue);
                }
                expression.add(expressionNode);
            }
        } else if (node instanceof OperationNode) {
            String operation = ((OperationNode) node).getOperation();
            if (!StringUtils.equalsIgnoreCase(AND, operation)) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER);
            }
            setExpressionNodeList(node.getLeftNode(), expression);
            setExpressionNodeList(node.getRightNode(), expression);
        }
    }

    private String getPaginatedFilterForDescendingOrder(String paginatedFilter, int after, int before)
            throws OrganizationManagementClientException {

        try {
            if (before != 0) {
                paginatedFilter += StringUtils.isNotBlank(paginatedFilter) ? " and before gt " + before :
                        "before gt " + before;
            } else if (after != 0) {
                paginatedFilter += StringUtils.isNotBlank(paginatedFilter) ? " and after lt " + after :
                        "after lt " + after;
            }
        } catch (IllegalArgumentException e) {
            throw handleClientException(ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION);
        }
        return paginatedFilter;
    }

    private boolean isFilteringAttributeNotSupported(String attributeValue) {

        return !attributeValue.equalsIgnoreCase(ORGANIZATION_ID_FIELD) &&
                !attributeValue.equalsIgnoreCase(ORGANIZATION_NAME_FIELD) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_AFTER) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_BEFORE) &&
                !attributeValue.equalsIgnoreCase(PARENT_ID_FIELD);
    }

    private Optional<String> removeAndGetOrganizationIdFromTheExpressionNodeList(
            List<ExpressionNode> expressionNodeList) {

        String organizationId = null;
        for (ExpressionNode expressionNode : expressionNodeList) {
            if (expressionNode.getAttributeValue().equalsIgnoreCase(PARENT_ID_FIELD)) {
                organizationId = expressionNode.getValue();
                break;
            }
        }
        if (organizationId != null) {
            expressionNodeList.removeIf(
                    expressionNode -> expressionNode.getAttributeValue().equalsIgnoreCase(PARENT_ID_FIELD));
        }
        return Optional.ofNullable(organizationId);
    }

    private Optional<String> removeAndGetOrganizationNameFromTheExpressionNodeList(
            List<ExpressionNode> expressionNodeList) {

        String organizationName = null;
        for (ExpressionNode expressionNode : expressionNodeList) {
            if (expressionNode.getAttributeValue().equalsIgnoreCase(ORGANIZATION_NAME_FIELD)) {
                organizationName = expressionNode.getValue();
                break;
            }
        }
        if (organizationName != null) {
            expressionNodeList.removeIf(
                    expressionNode -> expressionNode.getAttributeValue().equalsIgnoreCase(ORGANIZATION_NAME_FIELD));
        }
        return Optional.ofNullable(organizationName);
    }

    private List<SelectiveConnectionShareOrgConfigDTO> filterValidOrganizations(
            List<SelectiveConnectionShareOrgConfigDTO> organizations, String initiatingOrgId)
            throws ConnectionSharingMgtServerException {

        List<String> immediateChildOrgs;
        try {
            immediateChildOrgs = getOrganizationManager().getChildOrganizationsIds(initiatingOrgId, false);
        } catch (OrganizationManagementException e) {
            String errorMessage = String.format(ERROR_CODE_GET_CHILD_ORGS.getMessage(), initiatingOrgId);
            throw new ConnectionSharingMgtServerException(ERROR_CODE_GET_CHILD_ORGS.getCode(),
                    errorMessage, ERROR_CODE_GET_CHILD_ORGS.getDescription(), e);
        }

        List<SelectiveConnectionShareOrgConfigDTO> validOrgs = organizations.stream()
                .filter(org -> immediateChildOrgs.contains(org.getOrgId()))
                .toList();

        List<String> skippedOrgs = organizations.stream()
                .map(SelectiveConnectionShareOrgConfigDTO::getOrgId)
                .filter(orgId -> !immediateChildOrgs.contains(orgId))
                .toList();

        if (!skippedOrgs.isEmpty() && LOG.isDebugEnabled()) {
            LOG.debug("Skipping connection share for organizations that are not immediate children: " + skippedOrgs);
        }
        return validOrgs;
    }

    private void saveConnectionSharingPolicy(String connectionId, String policyHoldingOrgId, PolicyEnum policy,
                                             String initiatingOrgId, ResourceType resourceType)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicy resourceSharingPolicy = new ResourceSharingPolicy.Builder()
                .withResourceType(resourceType)
                .withResourceId(connectionId)
                .withInitiatingOrgId(initiatingOrgId)
                .withPolicyHoldingOrgId(policyHoldingOrgId)
                .withSharingPolicy(policy)
                .build();

        getResourceSharingPolicyHandlerService().addResourceSharingPolicyWithAttributes(
                resourceSharingPolicy, Collections.emptyList());
    }

    private void deleteExistingSharingPoliciesOfConnection(String connectionId, String initiatingOrgId,
                                                           ResourceType resourceType)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(
                resourceType, connectionId, initiatingOrgId);
    }

    private void deleteResourceSharingPolicyOfConnectionInOrg(String policyHoldingOrgId, String connectionId,
                                                              String initiatingOrgId, ResourceType resourceType)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                policyHoldingOrgId, resourceType, connectionId, initiatingOrgId);
    }

    private ConnectionTypeHandler resolveConnectionTypeHandler(ResourceType resourceType)
            throws ConnectionSharingMgtClientException {

        ConnectionTypeHandler handler = ConnectionSharingDataHolder.getInstance()
                .getConnectionTypeHandler(resourceType);
        if (handler == null) {
            throw new ConnectionSharingMgtClientException(ERROR_CODE_NO_HANDLER_FOR_CONNECTION_TYPE);
        }
        return handler;
    }

    private void initiateThreadLocalContext(ConnectionSharingInitiatorContext connectionSharingInitiatorContext,
                                            Map<String, Object> threadLocalProperties) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(connectionSharingInitiatorContext.getSharingInitiatedTenantDomain(), true);
        carbonContext.setTenantId(connectionSharingInitiatorContext.getSharingInitiatedTenantId());
        carbonContext.setUsername(connectionSharingInitiatorContext.getSharingInitiatedUsername());
        IdentityUtil.threadLocalProperties.get().putAll(threadLocalProperties);
    }

    private void logAsyncProcessing(String action, String userId, String orgId) {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(ASYNC_PROCESSING_LOG_TEMPLATE, action, userId, orgId));
        }
    }

    private void validateSelectiveConnectionShareInput(SelectiveConnectionShareDTO dto)
            throws ConnectionSharingMgtClientException {

        validateNotNull(dto, ERROR_CODE_NULL_INPUT);
        validateNotNull(dto.getConnectionId(), ERROR_CODE_CONNECTION_ID_NULL);
        validateNotNull(dto.getResourceType(), ERROR_CODE_CONNECTION_TYPE_NULL);
        validateNotNull(dto.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);
        for (SelectiveConnectionShareOrgConfigDTO orgConfig : dto.getOrganizations()) {
            validateNotNull(orgConfig, ERROR_CODE_NULL_INPUT);
            validateNotNull(orgConfig.getOrgId(), ERROR_CODE_ORG_ID_NULL);
            PolicyEnum policy = orgConfig.getPolicy();
            validateNotNull(policy, ERROR_CODE_POLICY_NULL);
            if (!(policy == PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN
                    || policy == PolicyEnum.SELECTED_ORG_ONLY)) {
                throw new ConnectionSharingMgtClientException(ERROR_CODE_UNSUPPORTED_POLICY);
            }
        }
        LOG.debug("Validated selective connection share input successfully.");
    }

    private void validateGeneralConnectionShareInput(GeneralConnectionShareDTO dto)
            throws ConnectionSharingMgtClientException {

        validateNotNull(dto, ERROR_CODE_NULL_INPUT);
        validateNotNull(dto.getConnectionId(), ERROR_CODE_CONNECTION_ID_NULL);
        validateNotNull(dto.getResourceType(), ERROR_CODE_CONNECTION_TYPE_NULL);
        validateNotNull(dto.getPolicy(), ERROR_CODE_POLICY_NULL);
        if (dto.getPolicy() != PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS) {
            throw new ConnectionSharingMgtClientException(ERROR_CODE_UNSUPPORTED_POLICY);
        }
        LOG.debug("Validated general connection share input successfully.");
    }

    private void validateSelectiveConnectionUnshareInput(SelectiveConnectionUnshareDTO dto)
            throws ConnectionSharingMgtClientException {

        validateNotNull(dto, ERROR_CODE_NULL_INPUT);
        validateNotNull(dto.getConnectionId(), ERROR_CODE_CONNECTION_ID_NULL);
        validateNotNull(dto.getResourceType(), ERROR_CODE_CONNECTION_TYPE_NULL);
        validateNotNull(dto.getOrgIds(), ERROR_CODE_ORGANIZATIONS_NULL);
        for (String orgId : dto.getOrgIds()) {
            validateNotNull(orgId, ERROR_CODE_ORG_ID_NULL);
        }
        LOG.debug("Validated selective connection unshare input successfully.");
    }

    private void validateGeneralConnectionUnshareInput(GeneralConnectionUnshareDTO dto)
            throws ConnectionSharingMgtClientException {

        validateNotNull(dto, ERROR_CODE_NULL_INPUT);
        validateNotNull(dto.getConnectionId(), ERROR_CODE_CONNECTION_ID_NULL);
        validateNotNull(dto.getResourceType(), ERROR_CODE_CONNECTION_TYPE_NULL);
        LOG.debug("Validated general connection unshare input successfully.");
    }

    private void validateGetConnectionSharedOrgsInput(GetConnectionSharedOrgsDTO dto)
            throws ConnectionSharingMgtClientException {

        validateNotNull(dto, ERROR_CODE_NULL_INPUT);
        validateNotNull(dto.getConnectionId(), ERROR_CODE_CONNECTION_ID_NULL);
        validateNotNull(dto.getResourceType(), ERROR_CODE_CONNECTION_TYPE_NULL);
        validateNotNull(dto.getInitiatingOrgId(), ERROR_CODE_ORG_ID_NULL);
        if (dto.getFilter() == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Filter is null for connection ID: " + dto.getConnectionId() +
                        ". Normalizing to empty string.");
            }
            dto.setFilter(StringUtils.EMPTY);
        }
        validateGetAttributes(dto.getAttributes());
        LOG.debug("Validated get connection shared organizations input successfully.");
    }

    private void validateGetAttributes(List<String> attributes) throws ConnectionSharingMgtClientException {

        validateNotNull(attributes, ERROR_CODE_NULL_INPUT);
        for (String attribute : attributes) {
            validateNotNull(attribute, ERROR_CODE_NULL_INPUT);
            if (!SUPPORTED_GET_ATTRIBUTES.contains(attribute)) {
                throw new ConnectionSharingMgtClientException(ERROR_CODE_UNSUPPORTED_GET_ATTRIBUTE);
            }
        }
    }

    private void validateNotNull(Object obj, ErrorMessage error) throws ConnectionSharingMgtClientException {

        if (obj == null) {
            throw new ConnectionSharingMgtClientException(error);
        }
    }

    private OrganizationManager getOrganizationManager() {

        return ConnectionSharingDataHolder.getInstance().getOrganizationManager();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return ConnectionSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }
}
