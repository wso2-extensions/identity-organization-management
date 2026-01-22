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

package org.wso2.carbon.identity.organization.management.organization.user.sharing;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.EditOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.RoleAssignmentMode;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharePatchOperation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtClientException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.exception.UserSharingMgtServerException;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.internal.OrganizationUserSharingDataHolder;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.BaseUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.GeneralUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.SelectiveUserShare;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.UserAssociation;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserShareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.BaseUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GeneralUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.GetUserSharedOrgsDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.PatchOperationDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseOrgDetailsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.ResponseSharedOrgsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleAssignmentDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareOrgDetailsV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserShareV2DO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SelectiveUserUnshareDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.SharingModeDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.UserSharePatchDO;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserIdList;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.SharingInitiatorContext;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
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
import org.wso2.carbon.identity.role.v2.mgt.core.util.UserIDResolver;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ACTION_GENERAL_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ACTION_GENERAL_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ACTION_SELECTIVE_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ACTION_SELECTIVE_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ACTION_USER_SHARE_ATTRIBUTE_UPDATE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ACTION_USER_SHARE_ROLE_ASSIGNMENT_UPDATE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.APPLICATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ASYNC_PROCESSING_LOG_TEMPLATE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_AUDIENCE_TYPE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_FILTER_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ATTRIBUTES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ATTRIBUTE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ATTRIBUTE_UNSUPPORTED;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_ROLE_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_AUDIENCE_TYPE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_INVALID_POLICY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_NULL_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORGANIZATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_DETAILS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_INVALID_FORMAT;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ORG_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATIONS_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_OP_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_OP_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_PATH_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_PATH_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_ROLES_VALUE_CONTENT_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_VALUE_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_PATCH_OPERATION_VALUE_TYPE_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_REQUEST_BODY_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NAME_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLE_NOT_FOUND;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_INVALID;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_CRITERIA_MISSING;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_ID_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_SHARE_ROLE_ASSIGNMENT_UPDATE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_UNSHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_GENERAL_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_SELECTIVE_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.LOG_WARN_NON_RESIDENT_USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.LOG_WARN_SKIP_ORG_SHARE_MESSAGE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.PATCH_PATH_NONE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.PATCH_PATH_PREFIX;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.PATCH_PATH_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.PATCH_PATH_SUFFIX_ROLES;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.SP_SHARED_ROLE_INCLUDED_KEY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.SP_SHARED_SHARING_MODE_INCLUDED_KEY;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_IDS;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.USER_SHARING_LOG_TEMPLATE;
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
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.util.OrgResourceHierarchyTraverseUtil.getOrganizationManager;

/**
 * Implementation of the user sharing policy handler service v2.
 */
public class UserSharingPolicyHandlerServiceImplV2 implements UserSharingPolicyHandlerServiceV2 {

    private static final Log LOG = LogFactory.getLog(UserSharingPolicyHandlerServiceImplV2.class);
    private final UserIDResolver userIDResolver = new UserIDResolver();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(5);
    private static final Set<String> SUPPORTED_GET_ATTRIBUTES =
            new HashSet<>(Arrays.asList(SP_SHARED_SHARING_MODE_INCLUDED_KEY, SP_SHARED_ROLE_INCLUDED_KEY));

    @Override
    public void populateSelectiveUserShareV2(SelectiveUserShareV2DO selectiveUserShareV2DO)
            throws UserSharingMgtException {

        LOG.debug("Starting selective user share operation.");

        validateUserShareInput(selectiveUserShareV2DO);

        // Capture sharing initiator context before async execution.
        SharingInitiatorContext sharingInitiatorContext = SharingInitiatorContext.capture();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        List<SelectiveUserShareOrgDetailsV2DO> organizations = selectiveUserShareV2DO.getOrganizations();
        Map<String, UserCriteriaType> userCriteria = selectiveUserShareV2DO.getUserCriteria();

        List<SelectiveUserShareOrgDetailsV2DO> validOrganizations =
                filterValidOrganizations(organizations, sharingInitiatorContext.getSharingInitiatedOrgId());

        // Run the selective user sharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_SELECTIVE_USER_SHARE,
                            sharingInitiatorContext.getSharingInitiatedUserId(),
                            sharingInitiatorContext.getSharingInitiatedOrgId());
                    restoreThreadLocalContext(sharingInitiatorContext.getSharingInitiatedTenantDomain(),
                            sharingInitiatorContext.getSharingInitiatedTenantId(),
                            sharingInitiatorContext.getSharingInitiatedUsername(), threadLocalProperties);
                    processSelectiveUserShare(userCriteria, validOrganizations,
                            sharingInitiatorContext.getSharingInitiatedOrgId(),
                            sharingInitiatorContext.getSharingInitiatedUserId());
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async user selective share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateGeneralUserShareV2(GeneralUserShareV2DO generalUserShareV2DO) throws UserSharingMgtException {

        LOG.debug("Starting general user share operation.");

        validateUserShareInput(generalUserShareV2DO);

        // Capture thread-local properties before async execution.
        SharingInitiatorContext sharingInitiatorContext = SharingInitiatorContext.capture();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        Map<String, UserCriteriaType> userCriteria = generalUserShareV2DO.getUserCriteria();
        PolicyEnum policy = generalUserShareV2DO.getPolicy();
        List<String> roleIds = getRoleIds(generalUserShareV2DO.getRoleAssignments().getRoles(),
                sharingInitiatorContext.getSharingInitiatedOrgId());
        RoleAssignmentMode roleAssignmentMode = generalUserShareV2DO.getRoleAssignments().getMode();

        // Run the general user sharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_GENERAL_USER_SHARE,
                            sharingInitiatorContext.getSharingInitiatedUserId(),
                            sharingInitiatorContext.getSharingInitiatedOrgId());
                    restoreThreadLocalContext(sharingInitiatorContext.getSharingInitiatedTenantDomain(),
                            sharingInitiatorContext.getSharingInitiatedTenantId(),
                            sharingInitiatorContext.getSharingInitiatedUsername(), threadLocalProperties);
                    processGeneralUserShare(userCriteria, policy, roleIds, roleAssignmentMode,
                            sharingInitiatorContext.getSharingInitiatedOrgId(),
                            sharingInitiatorContext.getSharingInitiatedUserId());
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general user share processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateSelectiveUserUnshareV2(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtException {

        LOG.debug("Starting selective user unshare operation.");

        validateUserUnshareInput(selectiveUserUnshareDO);

        // Capture thread-local properties before async execution.
        SharingInitiatorContext sharingInitiatorContext = SharingInitiatorContext.capture();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        Map<String, UserCriteriaType> userCriteria = selectiveUserUnshareDO.getUserCriteria();
        List<String> organizations = selectiveUserUnshareDO.getOrganizations();

        // Run the selective user unsharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_SELECTIVE_USER_UNSHARE,
                            sharingInitiatorContext.getSharingInitiatedUserId(),
                            sharingInitiatorContext.getSharingInitiatedOrgId());
                    restoreThreadLocalContext(sharingInitiatorContext.getSharingInitiatedTenantDomain(),
                            sharingInitiatorContext.getSharingInitiatedTenantId(),
                            sharingInitiatorContext.getSharingInitiatedUsername(), threadLocalProperties);
                    processSelectiveUserUnshare(userCriteria, organizations,
                            sharingInitiatorContext.getSharingInitiatedOrgId());
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async user selective unshare processing.", ex);
                    return null;
                });
    }

    @Override
    public void populateGeneralUserUnshareV2(GeneralUserUnshareDO generalUserUnshareDO) throws UserSharingMgtException {

        LOG.debug("Starting general user unshare operation.");

        validateUserUnshareInput(generalUserUnshareDO);

        // Capture thread-local properties before async execution.
        SharingInitiatorContext sharingInitiatorContext = SharingInitiatorContext.capture();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        Map<String, UserCriteriaType> userCriteria = generalUserUnshareDO.getUserCriteria();

        // Run the general user unsharing logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_GENERAL_USER_UNSHARE,
                            sharingInitiatorContext.getSharingInitiatedUserId(),
                            sharingInitiatorContext.getSharingInitiatedOrgId());
                    restoreThreadLocalContext(sharingInitiatorContext.getSharingInitiatedTenantDomain(),
                            sharingInitiatorContext.getSharingInitiatedTenantId(),
                            sharingInitiatorContext.getSharingInitiatedUsername(), threadLocalProperties);
                    processGeneralUserUnshare(userCriteria, sharingInitiatorContext.getSharingInitiatedOrgId());
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async general user unshare processing.", ex);
                    return null;
                });
    }

    @Override
    public void updateSharedUserAttributesV2(UserSharePatchDO userSharePatchDO) throws UserSharingMgtException {

        LOG.debug("Starting user share role assignment update operation.");

        validateSharedUserAttributeUpdateInput(userSharePatchDO);

        // Capture thread-local properties before async execution.
        SharingInitiatorContext sharingInitiatorContext = SharingInitiatorContext.capture();

        // Capture additional thread-local properties.
        Map<String, Object> threadLocalProperties = new HashMap<>(IdentityUtil.threadLocalProperties.get());

        Map<String, UserCriteriaType> userCriteria = userSharePatchDO.getUserCriteria();

        // Run the shared user attribute update logic asynchronously.
        CompletableFuture.runAsync(() -> {
                    logAsyncProcessing(ACTION_USER_SHARE_ATTRIBUTE_UPDATE,
                            sharingInitiatorContext.getSharingInitiatedUserId(),
                            sharingInitiatorContext.getSharingInitiatedOrgId());
                    restoreThreadLocalContext(sharingInitiatorContext.getSharingInitiatedTenantDomain(),
                            sharingInitiatorContext.getSharingInitiatedTenantId(),
                            sharingInitiatorContext.getSharingInitiatedUsername(), threadLocalProperties);
                    processUpdateSharedUserAttributes(userCriteria, sharingInitiatorContext.getSharingInitiatedOrgId(),
                            sharingInitiatorContext.getSharingInitiatedUserId(), userSharePatchDO);
                }, EXECUTOR)
                .exceptionally(ex -> {
                    LOG.error("Error occurred during async user share role assignment update processing.", ex);
                    return null;
                });
    }

    @Override
    public ResponseSharedOrgsV2DO getUserSharedOrganizationsV2(GetUserSharedOrgsDO getUserSharedOrgsDO)
            throws UserSharingMgtException {

        LOG.debug("Starting user share role assignment get operation.");

        validateSharedUserGetInput(getUserSharedOrgsDO);

        int limit = getUserSharedOrgsDO.getLimit();
        int beforeCursor = getUserSharedOrgsDO.getBefore();
        int afterCursor = getUserSharedOrgsDO.getAfter();
        boolean recursive = getUserSharedOrgsDO.getRecursive();
        String filter = getUserSharedOrgsDO.getFilter();
        String mainUserId = getUserSharedOrgsDO.getUserId();
        List<String> includedAttributes = getUserSharedOrgsDO.getAttributes();
        List<ResponseOrgDetailsV2DO> sharedOrgsList = new ArrayList<>();

        try {
            // Determine sort order based on pagination cursors.
            String sortOrder = (beforeCursor != 0) ? ASC_SORT_ORDER : DESC_SORT_ORDER;
            List<ExpressionNode> expressionNodes = getExpressionNodes(filter, afterCursor, beforeCursor);

            String parentOrgId = resolveParentOrgId(expressionNodes, getUserSharedOrgsDO);
            List<String> childOrgIds = StringUtils.isNotBlank(parentOrgId)
                    ? getOrganizationManager().getChildOrganizationsIds(parentOrgId, recursive)
                    : new ArrayList<>();

            SharingModeDO generalSharingMode = resolveGeneralSharingMode(includedAttributes, parentOrgId, mainUserId);

            int fetchLimit = (limit == 0) ? limit : limit + 1;
            List<UserAssociation> userAssociations =
                    getOrganizationUserSharingService().getUserAssociationsOfGivenUser(mainUserId, parentOrgId,
                            childOrgIds, expressionNodes, sortOrder, fetchLimit);

            if (CollectionUtils.isEmpty(userAssociations)) {
                return buildEmptyResponseToGet(generalSharingMode);
            }

            boolean hasMoreItems = (limit != 0) && (userAssociations.size() > limit);
            if (hasMoreItems) {
                userAssociations.remove(userAssociations.size() - 1); // Remove the "probe" item.
            }

            // If we fetched in ASC order (for 'before' cursor), reverse back to DESC for display.
            if (beforeCursor != 0) {
                Collections.reverse(userAssociations);
            }

            for (UserAssociation userAssociation : userAssociations) {
                sharedOrgsList.add(resolveSharedOrgDetails(userAssociation, includedAttributes));
            }

            return buildResponseWithCursors(sharedOrgsList, userAssociations, generalSharingMode, beforeCursor,
                    afterCursor, hasMoreItems);
        } catch (OrganizationManagementException e) {
            throw new UserSharingMgtServerException(ERROR_CODE_GET_SHARED_ORGANIZATIONS_OF_USER, e);
        }
    }

    // Asynchronous Processing Methods.

    /**
     * Processes selective user sharing based on the provided user criteria and organization details.
     * This method iterates over the user criteria map and shares users selectively with the specified organizations.
     *
     * @param userCriteria           A map containing user criteria, such as user IDs.
     * @param organizations          A list of organizations to which users will be shared selectively.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the user sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     */
    private void processSelectiveUserShare(Map<String, UserCriteriaType> userCriteria,
                                           List<SelectiveUserShareOrgDetailsV2DO> organizations,
                                           String sharingInitiatedOrgId, String sharingInitiatedUserId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            selectiveUserShareByUserIds((UserIdList) criterionValues, organizations,
                                    sharingInitiatedOrgId, sharingInitiatedUserId);
                        } else {
                            LOG.error("Invalid user criteria provided for selective user share: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for selective user share: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while sharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed selective user share initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Processes general user sharing based on the provided user criteria and sharing policy.
     * This method iterates over the user criteria map and shares users according to the specified policy.
     *
     * @param userCriteria           A map containing user criteria, such as user IDs.
     * @param policy                 The sharing policy defining the scope of sharing.
     * @param roleIds                A list of role IDs to be assigned during sharing.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the user sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     */
    private void processGeneralUserShare(Map<String, UserCriteriaType> userCriteria, PolicyEnum policy,
                                         List<String> roleIds, RoleAssignmentMode roleAssignmentMode,
                                         String sharingInitiatedOrgId, String sharingInitiatedUserId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            generalUserShareByUserIds((UserIdList) criterionValues, policy, roleIds, roleAssignmentMode,
                                    sharingInitiatedOrgId, sharingInitiatedUserId);
                        } else {
                            LOG.error("Invalid user criteria provided for general user share: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for general user share: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while sharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed general user share initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Processes selective user unsharing based on the provided user criteria and organization details.
     * This method iterates over the user criteria map and unshare users selectively from the specified organizations.
     *
     * @param userCriteria          A map containing user criteria, such as user IDs.
     * @param organizations         A list of organizations from which users will be unshared selectively.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the user unsharing.
     */
    private void processSelectiveUserUnshare(Map<String, UserCriteriaType> userCriteria, List<String> organizations,
                                             String sharingInitiatedOrgId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            selectiveUserUnshareByUserIds((UserIdList) criterionValues, organizations,
                                    sharingInitiatedOrgId);
                        } else {
                            LOG.error("Invalid user criteria provided for selective user unshare: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for selective user unshare: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while unsharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed selective user unshare initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Processes general user unsharing based on the provided user criteria.
     * This method iterates over the user criteria map and unshare users from all associated organizations.
     *
     * @param userCriteria          A map containing user criteria, such as user IDs.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the user unsharing.
     */
    private void processGeneralUserUnshare(Map<String, UserCriteriaType> userCriteria, String sharingInitiatedOrgId) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            generalUserUnshareByUserIds((UserIdList) criterionValues, sharingInitiatedOrgId);
                        } else {
                            LOG.error("Invalid user criteria provided for general user unshare: " + criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for general user unshare: " + criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while unsharing user from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed general user unshare initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void processUpdateSharedUserAttributes(Map<String, UserCriteriaType> userCriteria,
                                                   String sharingInitiatedOrgId, String sharingInitiatedUserId,
                                                   UserSharePatchDO userSharePatchDO) {

        try {
            for (Map.Entry<String, UserCriteriaType> criterion : userCriteria.entrySet()) {
                String criterionKey = criterion.getKey();
                UserCriteriaType criterionValues = criterion.getValue();

                try {
                    if (USER_IDS.equals(criterionKey)) {
                        if (criterionValues instanceof UserIdList) {
                            updateSharedUserAttributesByUserIds((UserIdList) criterionValues, sharingInitiatedOrgId,
                                    sharingInitiatedUserId, userSharePatchDO);
                        } else {
                            LOG.error("Invalid user criteria provided for user share role assignment update: " +
                                    criterionKey);
                        }
                    } else {
                        LOG.error("Invalid user criteria provided for user share role assignment update: " +
                                criterionKey);
                    }
                } catch (UserSharingMgtException e) {
                    LOG.error("Error occurred while updating role assignments from user criteria: " + USER_IDS, e);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Completed user share role assignment update initiated from " + sharingInitiatedOrgId + ".");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    // User Sharing & Unsharing Helper Methods.

    /**
     * Shares a user with selected organizations based on the provided user list and sharing policies.
     * If the user is not a resident user in the initiating organization, the sharing is skipped.
     * Each organization is processed with the appropriate role and policy before sharing.
     *
     * @param userIds                The list of user IDs to be selectively shared.
     * @param organizations          The list of organizations where the user should be shared.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     */
    private void selectiveUserShareByUserIds(UserIdList userIds, List<SelectiveUserShareOrgDetailsV2DO> organizations,
                                             String sharingInitiatedOrgId, String sharingInitiatedUserId)
            throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                if (isExistingUser(associatedUserId, sharingInitiatedOrgId) &&
                        isResidentUserInOrg(associatedUserId, sharingInitiatedOrgId)) {

                    List<BaseUserShare> selectiveUserShareObjectsInRequest = new ArrayList<>();
                    for (SelectiveUserShareOrgDetailsV2DO organization : organizations) {
                        SelectiveUserShare selectiveUserShare = new SelectiveUserShare.Builder()
                                .withUserId(associatedUserId)
                                .withOrganizationId(organization.getOrganizationId())
                                .withPolicy(organization.getPolicy())
                                .withRoles(
                                        getRoleIds(organization.getRoleAssignments().getRoles(), sharingInitiatedOrgId))
                                .withRoleAssignmentMode(organization.getRoleAssignments().getMode())
                                .build();
                        selectiveUserShareObjectsInRequest.add(selectiveUserShare);
                    }
                    shareUser(associatedUserId, selectiveUserShareObjectsInRequest, sharingInitiatedOrgId,
                            sharingInitiatedUserId);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format(LOG_WARN_NON_RESIDENT_USER, associatedUserId, sharingInitiatedOrgId));
                    }
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                String errorMessage =
                        String.format(ERROR_SELECTIVE_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserSharingMgtServerException(ERROR_SELECTIVE_SHARE, errorMessage);
            }
        }
    }

    /**
     * Shares a user with all applicable organizations based on the provided policy.
     * If the user is not a resident user in the initiating organization, the sharing is skipped.
     *
     * @param userIds                The list of user IDs to be shared.
     * @param policy                 The policy defining the scope of sharing.
     * @param roleIds                The list of role IDs to be assigned during sharing.
     * @param sharingInitiatedOrgId  The ID of the organization that initiated the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     */
    private void generalUserShareByUserIds(UserIdList userIds, PolicyEnum policy, List<String> roleIds,
                                           RoleAssignmentMode roleAssignmentMode, String sharingInitiatedOrgId,
                                           String sharingInitiatedUserId)
            throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                if (isExistingUser(associatedUserId, sharingInitiatedOrgId) &&
                        isResidentUserInOrg(associatedUserId, sharingInitiatedOrgId)) {
                    GeneralUserShare generalUserShare = new GeneralUserShare.Builder()
                            .withUserId(associatedUserId)
                            .withPolicy(policy)
                            .withRoles(roleIds)
                            .withRoleAssignmentMode(roleAssignmentMode)
                            .build();
                    List<BaseUserShare> generalUserShareObjectsInRequest = Collections.singletonList(generalUserShare);
                    shareUser(associatedUserId, generalUserShareObjectsInRequest, sharingInitiatedOrgId,
                            sharingInitiatedUserId);
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                String errorMessage = String.format(ERROR_GENERAL_SHARE.getMessage(), associatedUserId, e.getMessage());
                throw new UserSharingMgtServerException(ERROR_GENERAL_SHARE, errorMessage);
            }
        }
    }

    /**
     * Unshare a user from selected organizations based on the provided user list.
     * If a resource-sharing policy exists for the user, it is deleted.
     *
     * @param userIds                 The list of user IDs to be unshared.
     * @param organizations           The list of organizations from which the user should be unshared.
     * @param unsharingInitiatedOrgId The ID of the organization that initiated the unsharing.
     */
    private void selectiveUserUnshareByUserIds(UserIdList userIds, List<String> organizations,
                                               String unsharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                for (String organizationId : organizations) {
                    // Unshare user from the organization and its child organizations.
                    List<String> orgTreeInclusive = new ArrayList<>();
                    orgTreeInclusive.add(organizationId);
                    orgTreeInclusive.addAll(getOrganizationManager().getChildOrganizationsIds(organizationId, true));

                    for (String eachOrg : orgTreeInclusive) {
                        getOrganizationUserSharingService().unshareOrganizationUserInSharedOrganization(
                                associatedUserId, eachOrg);
                    }

                    // Delete resource sharing policy if it has been stored for future shares.
                    deleteResourceSharingPolicyOfUserInOrg(organizationId, associatedUserId, unsharingInitiatedOrgId);
                }
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_UNSHARE);
            }
        }
    }

    /**
     * Unshare a user from all applicable organizations based on the provided user list.
     * If a resource-sharing policy exists for the user, it is deleted.
     *
     * @param userIds                 The list of user IDs to be unshared.
     * @param unsharingInitiatedOrgId The ID of the organization that initiated the unsharing.
     */
    private void generalUserUnshareByUserIds(UserIdList userIds, String unsharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                getOrganizationUserSharingService().unshareOrganizationUsers(associatedUserId, unsharingInitiatedOrgId);

                // Delete all resource sharing policies if it has been stored for future shares.
                deleteAllResourceSharingPoliciesOfUser(associatedUserId, unsharingInitiatedOrgId);
            } catch (OrganizationManagementException | ResourceSharingPolicyMgtException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_UNSHARE);
            }
        }
    }

    private void updateSharedUserAttributesByUserIds(UserIdList userIds, String sharingInitiatedOrgId,
                                                     String sharingInitiatedUserId, UserSharePatchDO userSharePatchDO)
            throws UserSharingMgtException {

        for (String associatedUserId : userIds.getIds()) {
            try {
                updateSharedUserAttributesForUser(associatedUserId, sharingInitiatedOrgId, sharingInitiatedUserId,
                        userSharePatchDO.getPatchOperations());
            } catch (OrganizationManagementException | IdentityRoleManagementException e) {
                throw new UserSharingMgtServerException(ERROR_CODE_USER_SHARE_ROLE_ASSIGNMENT_UPDATE);
            }
        }
    }

    // GET operation helper methods.

    /**
     * construct the final response object with calculated cursors.
     *
     * @param sharedOrgsList     List of shared organizations.
     * @param userAssociations   List of user associations.
     * @param generalSharingMode General sharing mode.
     * @param before             Before cursor.
     * @param after              After cursor.
     * @param hasMoreItems       Flag indicating if there are more items.
     * @return ResponseSharedOrgsV2DO.
     */
    private ResponseSharedOrgsV2DO buildResponseWithCursors(List<ResponseOrgDetailsV2DO> sharedOrgsList,
                                                            List<UserAssociation> userAssociations,
                                                            SharingModeDO generalSharingMode,
                                                            int before,
                                                            int after,
                                                            boolean hasMoreItems) {

        ResponseSharedOrgsV2DO response = new ResponseSharedOrgsV2DO();
        response.setSharedOrgs(sharedOrgsList);
        response.setSharingMode(generalSharingMode);

        int nextToken = 0;
        int previousToken = 0;

        // Is First Page? (No cursors provided OR provided 'before' but hit the start)
        boolean isFirstPage = (before == 0 && after == 0) || (before != 0 && !hasMoreItems);

        // Is Last Page? (No more items found AND (provided 'after' OR provided 'before' which hit the end))
        boolean isLastPage = !hasMoreItems && (after != 0 || before == 0);

        if (!isFirstPage && !userAssociations.isEmpty()) {
            previousToken = userAssociations.get(0).getId();
        }

        if (!isLastPage && !userAssociations.isEmpty()) {
            nextToken = userAssociations.get(userAssociations.size() - 1).getId();
        }

        response.setNextPageCursor(nextToken);
        response.setPreviousPageCursor(previousToken);

        return response;
    }

    /**
     * Build an empty response for get user shared organizations.
     *
     * @param sharingMode Sharing mode.
     * @return ResponseSharedOrgsV2DO.
     */
    private ResponseSharedOrgsV2DO buildEmptyResponseToGet(SharingModeDO sharingMode) {

        ResponseSharedOrgsV2DO response = new ResponseSharedOrgsV2DO();
        response.setSharingMode(sharingMode);
        response.setSharedOrgs(Collections.emptyList());
        response.setNextPageCursor(0);
        response.setPreviousPageCursor(0);
        return response;
    }

    /**
     * Resolves the parent organization ID from either the filter expression or the input object.
     *
     * @param expressionNodes     List of expression nodes from the filter.
     * @param getUserSharedOrgsDO Input data object.
     * @return Resolved parent organization ID.
     * @throws OrganizationManagementException OrganizationManagementException.
     */
    private String resolveParentOrgId(List<ExpressionNode> expressionNodes, GetUserSharedOrgsDO getUserSharedOrgsDO)
            throws OrganizationManagementException {

        // Try to get ID from expression
        Optional<String> optionalParentId = removeAndGetOrganizationIdFromTheExpressionNodeList(expressionNodes);
        if (optionalParentId.isPresent()) {
            return optionalParentId.get();
        }

        // Try to get Name from expression and resolve to ID
        Optional<String> optionalOrgName = removeAndGetOrganizationNameFromTheExpressionNodeList(expressionNodes);
        if (optionalOrgName.isPresent()) {
            return getOrganizationManager().getOrganizationIdByName(optionalOrgName.get());
        }

        return getUserSharedOrgsDO.getParentOrgId(); // null validation has been done in validateSharedUserGetInput.
    }

    /**
     * Resolve shared organization details from user association.
     *
     * @param userAssociation        User association.
     * @param includedAttributesList List of included attributes.
     * @return ResponseOrgDetailsV2DO.
     * @throws OrganizationManagementException OrganizationManagementException.
     * @throws UserSharingMgtException         UserSharingMgtException.
     */
    private ResponseOrgDetailsV2DO resolveSharedOrgDetails(UserAssociation userAssociation,
                                                           List<String> includedAttributesList)
            throws OrganizationManagementException, UserSharingMgtException {

        ResponseOrgDetailsV2DO responseOrgDetailsV2DO = new ResponseOrgDetailsV2DO();
        responseOrgDetailsV2DO.setUserId(userAssociation.getAssociatedUserId());
        responseOrgDetailsV2DO.setSharedUserId(userAssociation.getUserId());
        responseOrgDetailsV2DO.setSharedType(userAssociation.getSharedType());

        Organization organization =
                getOrganizationManager().getOrganization(userAssociation.getOrganizationId(), true, false);

        responseOrgDetailsV2DO.setOrganizationId(organization.getId());
        responseOrgDetailsV2DO.setOrganizationName(organization.getName());
        responseOrgDetailsV2DO.setOrganizationHandle(organization.getOrganizationHandle());
        responseOrgDetailsV2DO.setOrganizationStatus(organization.getStatus());
        responseOrgDetailsV2DO.setOrganizationReference(getOrganizationReference(organization));
        responseOrgDetailsV2DO.setParentOrganizationId(
                organization.getParent() != null ? organization.getParent().getId() : null);
        responseOrgDetailsV2DO.setHasChildren(organization.hasChildren());
        responseOrgDetailsV2DO.setDepthFromRoot(
                getOrganizationManager().getOrganizationDepthInHierarchy(organization.getId()));
        if (includedAttributesList.contains(SP_SHARED_SHARING_MODE_INCLUDED_KEY)) {
            SharingModeDO sharingModeDO =
                    resolveSelectiveSharingMode(organization.getId(), userAssociation.getAssociatedUserId(),
                            organization.getId());
            responseOrgDetailsV2DO.setSharingModeDO(sharingModeDO);
        }
        if (includedAttributesList.contains(SP_SHARED_ROLE_INCLUDED_KEY)) {
            responseOrgDetailsV2DO.setRoleWithAudienceDOList(
                    getAssignedSharedRolesForSharedUserInOrganization(userAssociation, organization.getId()));
        }

        return responseOrgDetailsV2DO;

    }

    /**
     * Get organization reference URL.
     *
     * @param organization Organization.
     * @return Organization reference URL.
     */
    private String getOrganizationReference(Organization organization) {

        return "/t/" + organization.getName() + "/api/server/v1/organizations/" + organization.getId();
    }

    /**
     * Get assigned shared roles for a shared user in an organization.
     *
     * @param userAssociation User association.
     * @param orgId           Organization ID.
     * @return List of RoleWithAudienceDO.
     * @throws UserSharingMgtException UserSharingMgtException.
     */
    private List<RoleWithAudienceDO> getAssignedSharedRolesForSharedUserInOrganization(UserAssociation userAssociation,
                                                                                       String orgId)
            throws UserSharingMgtException {

        try {
            List<RoleWithAudienceDO> roleWithAudienceList = new ArrayList<>();

            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);

            String usernameWithDomain = userIDResolver.getNameByID(userAssociation.getUserId(), tenantDomain);
            String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
            String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

            List<String> sharedRoleIdsInOrg =
                    getOrganizationUserSharingService().getRolesSharedWithUserInOrganization(username, tenantId,
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
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            throw new UserSharingMgtClientException(ERROR_CODE_GET_ROLES_SHARED_WITH_SHARED_USER);
        }
    }

    /**
     * Resolve general sharing mode for supported future general sharing policies.
     *
     * @param includedAttributes List of included attributes.
     * @param parentOrgId        Parent organization ID.
     * @param mainUserId         Main user ID.
     * @return SharingModeDO.
     * @throws OrganizationManagementException OrganizationManagementException.
     */
    private SharingModeDO resolveGeneralSharingMode(List<String> includedAttributes, String parentOrgId,
                                                    String mainUserId)
            throws OrganizationManagementException {

        if (includedAttributes.contains(SP_SHARED_SHARING_MODE_INCLUDED_KEY)) {
            return resolveSharingMode(parentOrgId, mainUserId, false, "");
        }
        return null;
    }

    /**
     * Resolve selective sharing mode for supported future selective sharing policies.
     *
     * @param initiatingOrgId Initiating organization ID.
     * @param mainUserId      Main user ID.
     * @param subOrgId        Sub organization ID for selective share.
     * @return SharingModeDO.
     * @throws OrganizationManagementException OrganizationManagementException.
     */
    private SharingModeDO resolveSelectiveSharingMode(String initiatingOrgId, String mainUserId, String subOrgId)
            throws OrganizationManagementException {

        return resolveSharingMode(initiatingOrgId, mainUserId, true, subOrgId);
    }

    /**
     * Resolve sharing mode if the sharing policy is a future policy.
     *
     * @param initiatingOrgId  Initiating organization ID.
     * @param mainUserId       Main user ID.
     * @param isSelectiveShare Whether it's a selective share.
     * @param subOrgId         Sub organization ID for selective share.
     * @return SharingModeDO.
     * @throws OrganizationManagementException OrganizationManagementException.
     */
    private SharingModeDO resolveSharingMode(String initiatingOrgId, String mainUserId, boolean isSelectiveShare,
                                             String subOrgId)
            throws OrganizationManagementException {

        try {
            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> result
                    = getResourceSharingPolicyHandlerService().getResourceSharingPolicyAndAttributesByInitiatingOrgId(
                    initiatingOrgId, USER, mainUserId);

            if (result != null && !result.isEmpty()) {
                Map.Entry<ResourceSharingPolicy, List<SharedResourceAttribute>> entry =
                        result.entrySet().iterator().next();
                ResourceSharingPolicy resourceSharingPolicy = entry.getKey();
                List<SharedResourceAttribute> resourceAttributes = entry.getValue();

                if (isSelectiveShare) {
                    boolean isPolicyHolderOrg = Objects.equals(resourceSharingPolicy.getPolicyHoldingOrgId(), subOrgId);
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
     * Construct SharingModeDO from ResourceSharingPolicy and SharedResourceAttributes.
     *
     * @param resourceSharingPolicy ResourceSharingPolicy.
     * @param resourceAttributes    List of SharedResourceAttribute.
     * @return SharingModeDO.
     * @throws IdentityRoleManagementException IdentityRoleManagementException.
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
                roleAssignmentDO.setRoles(getRoleWithAudienceFromMainRoleIds(roleAttributes));
                sharingModeDO.setRoleAssignment(roleAssignmentDO);
            }
        }

        return sharingModeDO;
    }

    /**
     * Get RoleWithAudienceDO list from main role IDs.
     *
     * @param roleAttributes List of SharedResourceAttribute.
     * @return List of RoleWithAudienceDO.
     * @throws IdentityRoleManagementException IdentityRoleManagementException.
     */
    private List<RoleWithAudienceDO> getRoleWithAudienceFromMainRoleIds(List<SharedResourceAttribute> roleAttributes)
            throws IdentityRoleManagementException {

        List<RoleWithAudienceDO> roleWithAudiences = new ArrayList<>();

        for (SharedResourceAttribute attribute : roleAttributes) {
            Role role = getRoleManagementService().getRole(attribute.getSharedAttributeId());
            if (role != null) {
                roleWithAudiences.add(
                        new RoleWithAudienceDO(role.getName(), role.getAudienceName(), role.getAudience()));
            }
        }

        return roleWithAudiences;
    }

    /**
     * Parses the filter string into a list of expression nodes for pagination and filtering.
     *
     * @param filter The filter string.
     * @param after  The 'after' pagination cursor.
     * @param before The 'before' pagination cursor.
     * @return A list of expression nodes.
     * @throws OrganizationManagementClientException If the filter format is invalid or contains unsupported attributes.
     */
    private List<ExpressionNode> getExpressionNodes(String filter, int after, int before)
            throws OrganizationManagementClientException {

        List<ExpressionNode> expressionNodes = new ArrayList<>();
        if (StringUtils.isBlank(filter)) {
            filter = StringUtils.EMPTY;
        }
        // paginationSortOrder specifies the sorting order for the pagination cursor.
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

    /**
     * Sets the expression nodes required for the retrieval of shared users from the database.
     *
     * @param node       The node.
     * @param expression The list of expression nodes.
     */
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

    /**
     * Appends pagination conditions to the filter for descending order.
     *
     * @param paginatedFilter The existing filter string.
     * @param after           The 'after' pagination cursor.
     * @param before          The 'before' pagination cursor.
     * @return The updated filter string with pagination conditions.
     * @throws OrganizationManagementClientException If the cursor values are invalid.
     */
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

    /**
     * Checks if the filtering attribute is not supported.
     *
     * @param attributeValue The attribute value to check.
     * @return true if the attribute is not supported; false otherwise.
     */
    private boolean isFilteringAttributeNotSupported(String attributeValue) {

        return !attributeValue.equalsIgnoreCase(ORGANIZATION_ID_FIELD) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_AFTER) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_BEFORE) &&
                !attributeValue.equalsIgnoreCase(PARENT_ID_FIELD);
    }

    /**
     * Removes and retrieves the organization ID from the expression node list.
     *
     * @param expressionNodeList The list of expression nodes.
     * @return An Optional containing the organization ID if found; otherwise, an empty Optional.
     */
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
            expressionNodeList.removeIf(expressionNode -> expressionNode.getAttributeValue()
                    .equalsIgnoreCase(PARENT_ID_FIELD));
        }
        return Optional.ofNullable(organizationId);
    }

    /**
     * Removes and retrieves the organization name from the expression node list.
     *
     * @param expressionNodeList The list of expression nodes.
     * @return An Optional containing the organization name if found; otherwise, an empty Optional.
     */
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
            expressionNodeList.removeIf(expressionNode -> expressionNode.getAttributeValue()
                    .equalsIgnoreCase(ORGANIZATION_NAME_FIELD));
        }
        return Optional.ofNullable(organizationName);
    }

    // Business Logic Methods.

    /**
     * Shares a user with the specified organizations.
     *
     * @param associatedUserId       The ID of the user to be shared.
     * @param baseUserShareObjects   The list of user share objects containing sharing details.
     * @param sharingInitiatedOrgId  The ID of the organization initiating the sharing.
     * @param sharingInitiatedUserId The ID of the user that initiated the user sharing.
     */
    private void shareUser(String associatedUserId, List<BaseUserShare> baseUserShareObjects,
                           String sharingInitiatedOrgId, String sharingInitiatedUserId)
            throws OrganizationManagementException, ResourceSharingPolicyMgtException {

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format(USER_SHARING_LOG_TEMPLATE, associatedUserId, sharingInitiatedOrgId,
                    sharingInitiatedUserId));
        }
        if (!baseUserShareObjects.isEmpty()) {
            Map<BaseUserShare, List<String>> userSharingOrgsForEachUserShareObject =
                    getUserSharingOrgsForEachUserShareObject(baseUserShareObjects, sharingInitiatedOrgId);

            createNewUserShare(sharingInitiatedOrgId, userSharingOrgsForEachUserShareObject);
        }
    }

    /**
     * Creates a new user share by sharing the user with the specified organizations based on the sharing policy of
     * each user share object and saving the sharing policy if applicable.
     *
     * @param sharingInitiatedOrgId                 The ID of the organization initiating the sharing.
     * @param userSharingOrgsForEachUserShareObject A map containing user share objects and their corresponding
     *                                              organizations.
     */
    private void createNewUserShare(String sharingInitiatedOrgId, Map<BaseUserShare,
            List<String>> userSharingOrgsForEachUserShareObject)
            throws ResourceSharingPolicyMgtException {

        for (Map.Entry<BaseUserShare, List<String>> entry : userSharingOrgsForEachUserShareObject.entrySet()) {

            if (isApplicableOrganizationScopeForSavingPolicy(entry.getKey().getPolicy())) {
                saveUserSharingPolicy(entry.getKey(), sharingInitiatedOrgId);
            }
            for (String orgId : entry.getValue()) {
                shareAndAssignRolesIfPresent(orgId, entry.getKey(), sharingInitiatedOrgId);
            }
        }
    }

    private void updateSharedUserAttributesForUser(String associatedUserId, String sharingInitiatedOrgId,
                                                   String sharingInitiatedUserId,
                                                   List<PatchOperationDO> patchOperations)
            throws UserSharingMgtException, OrganizationManagementException, IdentityRoleManagementException {

        for (PatchOperationDO patchOperation : patchOperations) {
            if (isPatchOperationPathRoles(patchOperation.getPath().trim())) {
                updateRoleAssignmentsOfSharedUser(associatedUserId, sharingInitiatedOrgId, sharingInitiatedUserId,
                        patchOperation);
            }
        }
    }

    private void updateRoleAssignmentsOfSharedUser(String associatedUserId, String sharingInitiatedOrgId,
                                                   String sharingInitiatedUserId, PatchOperationDO patchOperation)
            throws UserSharingMgtException, OrganizationManagementException, IdentityRoleManagementException {

        logAsyncProcessing(ACTION_USER_SHARE_ROLE_ASSIGNMENT_UPDATE, sharingInitiatedUserId, sharingInitiatedOrgId);
        String orgId = extractOrgIdFromRolesPath(patchOperation.getPath());
        UserAssociation userAssociation =
                getOrganizationUserSharingService().getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
        List<String> roleIds =
                getRoleIds(castToRoleWithAudienceList(patchOperation.getValues()), sharingInitiatedOrgId);
        switch (patchOperation.getOperation()) {
            case ADD:
                handleRoleAssignmentAddition(userAssociation, sharingInitiatedOrgId, roleIds);
                break;
            case REMOVE:
                handleRoleAssignmentRemoval(userAssociation, roleIds);
                break;
        }
    }

    /**
     * Handles the addition of role assignments to a shared user.
     *
     * @param userAssociation       The user association representing the shared user.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     */
    private void handleRoleAssignmentAddition(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                              List<String> roleIds) {

        assignRolesToTheSharedUser(userAssociation, sharingInitiatedOrgId, roleIds);
    }

    /**
     * Handles the removal of role assignments from a shared user.
     *
     * @param userAssociation The user association representing the shared user.
     * @param roleIds         The list of role IDs to be removed.
     */
    private void handleRoleAssignmentRemoval(UserAssociation userAssociation, List<String> roleIds)
            throws OrganizationManagementException, IdentityRoleManagementException {

        unAssignOldSharedRolesFromSharedUser(userAssociation, roleIds);
    }

    /**
     * Retrieves a map of user share objects and their corresponding organizations to share user based on the sharing
     * policy and type.
     *
     * @param baseUserShareObjects  The list of user share objects containing sharing details.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @return A map containing user share objects and their corresponding organizations.
     */
    private Map<BaseUserShare, List<String>> getUserSharingOrgsForEachUserShareObject(
            List<BaseUserShare> baseUserShareObjects, String sharingInitiatedOrgId)
            throws OrganizationManagementException {

        Map<BaseUserShare, List<String>> userSharingOrgsForEachUserShareObject = new HashMap<>();
        for (BaseUserShare baseUserShare : baseUserShareObjects) {
            userSharingOrgsForEachUserShareObject.put(baseUserShare,
                    extractOrgListBasedOnSharingPolicyAndType(baseUserShare, sharingInitiatedOrgId));
        }
        return userSharingOrgsForEachUserShareObject;
    }

    /**
     * Extracts a list of organization with which the user is expected to be shared according to the given policy of
     * the base user share object.
     *
     * @param baseUserShare         The base user share object containing sharing details.
     * @param sharingInitiatedOrgId The ID of the organization initiating the sharing.
     * @return A list of organization IDs based on the sharing policy and type.
     */
    private List<String> extractOrgListBasedOnSharingPolicyAndType(BaseUserShare baseUserShare,
                                                                   String sharingInitiatedOrgId)
            throws OrganizationManagementException {

        if (baseUserShare instanceof SelectiveUserShare) {
            return extractOrgListBasedOnSharingPolicy(((SelectiveUserShare) baseUserShare).getOrganizationId(),
                    baseUserShare.getPolicy());
        }
        return extractOrgListBasedOnSharingPolicy(sharingInitiatedOrgId, baseUserShare.getPolicy());
    }

    /**
     * Extracts a list of organizations based on the given sharing policy.
     * Depending on the policy, this method retrieves child organizations that should be included
     * in the user-sharing scope.
     *
     * @param policyHoldingOrgId The ID of the organization holding the policy.
     * @param policy             The sharing policy that determines which organizations to include.
     * @return A list of organization IDs that should be included in the sharing scope.
     */
    private List<String> extractOrgListBasedOnSharingPolicy(String policyHoldingOrgId, PolicyEnum policy)
            throws OrganizationManagementException {

        Set<String> userSharingOrgList = new HashSet<>();

        switch (policy) {
            case ALL_EXISTING_ORGS_ONLY:
            case ALL_EXISTING_AND_FUTURE_ORGS:
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case IMMEDIATE_EXISTING_ORGS_ONLY:
            case IMMEDIATE_EXISTING_AND_FUTURE_ORGS:
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case SELECTED_ORG_ONLY:
                userSharingOrgList.add(policyHoldingOrgId);
                break;

            case SELECTED_ORG_WITH_ALL_EXISTING_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN:
                userSharingOrgList.add(policyHoldingOrgId);
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, true));
                break;

            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_CHILDREN_ONLY:
            case SELECTED_ORG_WITH_EXISTING_IMMEDIATE_AND_FUTURE_CHILDREN:
                userSharingOrgList.add(policyHoldingOrgId);
                userSharingOrgList.addAll(getOrganizationManager()
                        .getChildOrganizationsIds(policyHoldingOrgId, false));
                break;

            case NO_SHARING:
                break;

            default:
                throw new OrganizationManagementClientException(
                        String.format(ERROR_CODE_INVALID_POLICY.getMessage(), policy.getPolicyName()),
                        ERROR_CODE_INVALID_POLICY.getDescription(),
                        ERROR_CODE_INVALID_POLICY.getCode());
        }

        return new ArrayList<>(userSharingOrgList);
    }

    /**
     * Shares a user with a specified organization and returns the user association.
     * This is where the user association will be created.
     *
     * @param orgId            The ID of the organization to share the user with.
     * @param associatedUserId The ID of the user to be shared.
     * @param associatedOrgId  The ID of the organization that initiated the sharing.
     * @return A {@code UserAssociation} representing the created association.
     */
    private UserAssociation shareUserWithOrganization(String orgId, String associatedUserId, String associatedOrgId)
            throws OrganizationManagementException {

        OrganizationUserSharingService organizationUserSharingService = getOrganizationUserSharingService();
        organizationUserSharingService.shareOrganizationUser(orgId, associatedUserId, associatedOrgId,
                SharedType.SHARED);
        return organizationUserSharingService.getUserAssociationOfAssociatedUserByOrgId(associatedUserId, orgId);
    }

    // Business Logic Helper Methods.

    /**
     * Checks if the specified user is a resident user in the given organization.
     *
     * @param userId The ID of the user.
     * @param orgId  The ID of the organization.
     * @return {@code true} if the user is a resident user, {@code false} otherwise.
     */
    private boolean isResidentUserInOrg(String userId, String orgId) {

        try {
            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(tenantId);
            String associatedOrgId =
                    OrganizationSharedUserUtil.getUserManagedOrganizationClaim(userStoreManager, userId);
            return associatedOrgId == null;
        } catch (UserStoreException | OrganizationManagementException e) {
            LOG.error("Error occurred while checking if the user is a resident user in the organization.", e);
            return false;
        }
    }

    /**
     * Checks if the specified user is an existing user in the given organization.
     *
     * @param userId The ID of the user.
     * @param orgId  The ID of the organization.
     * @return {@code true} if the user is an exiting user in the given organization, {@code false} otherwise.
     */
    private boolean isExistingUser(String userId, String orgId) {

        try {
            String tenantDomain = getOrganizationManager().resolveTenantDomain(orgId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            AbstractUserStoreManager userStoreManager = getAbstractUserStoreManager(tenantId);
            return userStoreManager.isExistingUserWithID(userId);
        } catch (UserStoreException | OrganizationManagementException e) {
            LOG.error("Error occurred while checking if the user is an existing user.", e);
            return false;
        }
    }

    /**
     * Filters the list of organizations to include only those that are immediate child organizations
     * of the sharing-initiated organization.
     * Any organizations that do not meet this criterion will be logged as skipped.
     *
     * @param organizations         The list of organizations to be filtered.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing process.
     * @return A list of organizations that are valid based on the immediate child organization criteria.
     */
    private List<SelectiveUserShareOrgDetailsV2DO> filterValidOrganizations(
            List<SelectiveUserShareOrgDetailsV2DO> organizations, String sharingInitiatedOrgId)
            throws UserSharingMgtServerException {

        List<String> immediateChildOrgs = getImmediateChildOrgsOfSharingInitiatedOrg(sharingInitiatedOrgId);

        List<SelectiveUserShareOrgDetailsV2DO> validOrganizations = organizations.stream()
                .filter(org -> immediateChildOrgs.contains(org.getOrganizationId()))
                .collect(Collectors.toList());

        List<String> skippedOrganizations = organizations.stream()
                .map(SelectiveUserShareOrgDetailsV2DO::getOrganizationId)
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
            throws UserSharingMgtServerException {

        try {
            return getOrganizationManager().getChildOrganizationsIds(getOrganizationId(), false);
        } catch (OrganizationManagementException e) {
            String errorMessage = String.format(
                    ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS.getMessage(), sharingInitiatedOrgId);
            throw new UserSharingMgtServerException(ERROR_CODE_GET_IMMEDIATE_CHILD_ORGS, errorMessage);
        }
    }

    // Resource Sharing Policy Management Methods.

    /**
     * Saves a new resource sharing policy for a user.
     *
     * @param baseUserShare         The user share details containing policy information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void saveUserSharingPolicy(BaseUserShare baseUserShare, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                getResourceSharingPolicyHandlerService();

        ResourceSharingPolicy resourceSharingPolicy =
                new ResourceSharingPolicy.Builder().withResourceType(ResourceType.USER)
                        .withResourceId(baseUserShare.getUserId())
                        .withInitiatingOrgId(sharingInitiatedOrgId)
                        .withPolicyHoldingOrgId(getPolicyHoldingOrgId(baseUserShare, sharingInitiatedOrgId))
                        .withSharingPolicy(baseUserShare.getPolicy()).build();

        List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
        for (String roleId : baseUserShare.getRoles()) {
            SharedResourceAttribute sharedResourceAttribute =
                    new SharedResourceAttribute.Builder().withSharedAttributeType(SharedAttributeType.ROLE)
                            .withSharedAttributeId(roleId).build();
            sharedResourceAttributes.add(sharedResourceAttribute);
        }

        resourceSharingPolicyHandlerService.addResourceSharingPolicyWithAttributes(resourceSharingPolicy,
                sharedResourceAttributes);
    }

    /**
     * Determines the policy-holding organization ID based on the type of user share.
     * For a selective user share, the policy-holding organization is the organization specified in the selective
     * share request.
     * For a general user share, the policy-holding organization is the organization from which the
     * sharing request was initiated.
     *
     * @param baseUserShare         The user share object, which can be either selective or general.
     * @param sharingInitiatedOrgId The ID of the organization from which the sharing request was initiated.
     * @return The ID of the policy-holding organization based on the type of user share.
     */
    private String getPolicyHoldingOrgId(BaseUserShare baseUserShare, String sharingInitiatedOrgId) {

        if (baseUserShare instanceof SelectiveUserShare) {
            return ((SelectiveUserShare) baseUserShare).getOrganizationId();
        }
        return sharingInitiatedOrgId;
    }

    /**
     * Determines whether a given policy scope allows saving the resource sharing policy.
     *
     * @param policy The policy enumeration containing the organization scope.
     * @return {@code true} if the policy allows saving, {@code false} otherwise.
     */
    private boolean isApplicableOrganizationScopeForSavingPolicy(PolicyEnum policy) {

        return OrganizationScope.EXISTING_ORGS_AND_FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope()) ||
                OrganizationScope.FUTURE_ORGS_ONLY.equals(policy.getOrganizationScope());
    }

    /**
     * Deletes the resource sharing policy for a given user in an organization.
     *
     * @param policyHoldingOrgId    The ID of the organization holding the policy.
     * @param associatedUserId      The ID of the associated user.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteResourceSharingPolicyOfUserInOrg(String policyHoldingOrgId, String associatedUserId,
                                                        String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                policyHoldingOrgId, ResourceType.USER, associatedUserId, sharingInitiatedOrgId);
    }

    /**
     * Deletes all resource sharing policies for a given user.
     *
     * @param associatedUserId      The ID of the associated user.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void deleteAllResourceSharingPoliciesOfUser(String associatedUserId, String sharingInitiatedOrgId)
            throws ResourceSharingPolicyMgtException {

        getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyByResourceTypeAndId(ResourceType.USER,
                associatedUserId, sharingInitiatedOrgId);
    }

    // Role Management Helper Methods.

    /**
     * Retrieves a list of role IDs based on the provided role and audience details.
     * This will return the role Ids in the parent organization that match to the given role-audience combination.
     *
     * @param rolesWithAudience     A list of roles with associated audience information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @return A list of role IDs that match the given role-audience combination.
     */
    private List<String> getRoleIds(List<RoleWithAudienceDO> rolesWithAudience, String sharingInitiatedOrgId)
            throws UserSharingMgtException {

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
            throw new UserSharingMgtServerException(ERROR_CODE_GET_ROLE_IDS);
        }
    }

    /**
     * Determines the audience ID based on the role's audience type.
     * If the audience is an organization, it returns the given organization ID.
     * If the audience is an application, it retrieves the application's resource ID.
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
            LOG.warn(String.format(ERROR_CODE_INVALID_AUDIENCE_TYPE.getDescription(), role.getAudienceType()));
        } catch (IdentityApplicationManagementException e) {
            LOG.warn(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), role.getAudienceName()));
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
        LOG.warn(String.format(ERROR_CODE_AUDIENCE_NOT_FOUND.getMessage(), audienceName));
        return null;
    }

    /**
     * Retrieves the role ID associated with a given role name and audience within a specific tenant domain.
     *
     * @param roleName     The name of the role.
     * @param audienceType The type of audience (organization or application).
     * @param audienceId   The audience ID.
     * @param tenantDomain The tenant domain where the role exists.
     * @return An {@code Optional<String>} containing the role ID if found, otherwise empty.
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
            LOG.warn(String.format(ERROR_CODE_ROLE_NOT_FOUND.getMessage(), roleName, audienceType, audienceId));
            return Optional.empty();
        }
    }

    /**
     * Shares a user with a specified organization and assigns roles if present.
     * This is where the user association will be created, and roles will be assigned to the shared user if any
     * roles are present.
     * It attempts to share the user with the specified organization and assigns roles accordingly.
     *
     * @param orgId                 The ID of the organization to share the user with.
     * @param baseUserShare         The base user share object containing user and role information.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void shareAndAssignRolesIfPresent(String orgId, BaseUserShare baseUserShare, String sharingInitiatedOrgId) {

        String associatedUserId = baseUserShare.getUserId();
        List<String> roleIds = baseUserShare.getRoles();
        RoleAssignmentMode roleAssignmentMode = baseUserShare.getRoleAssignmentMode();
        UserAssociation userAssociation;

        try {
            userAssociation = shareUserWithOrganization(orgId, associatedUserId, sharingInitiatedOrgId);

        } catch (OrganizationManagementException e) {

            String errorMessage = String.format(ERROR_CODE_USER_SHARE.getMessage(), associatedUserId, e.getMessage());
            LOG.error(errorMessage, e);
            return;
        }

        // Assign roles if RoleAssignmentMode is not NONE and if any roles are present.
        if (roleAssignmentMode != RoleAssignmentMode.NONE) {
            assignRolesIfPresent(userAssociation, sharingInitiatedOrgId, roleIds);
        }
    }

    /**
     * Assigns roles to the shared user if any roles are present.
     *
     * @param userAssociation       The user association object containing user and organization details.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     */
    private void assignRolesIfPresent(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                      List<String> roleIds) {

        if (!roleIds.isEmpty()) {
            assignRolesToTheSharedUser(userAssociation, sharingInitiatedOrgId, roleIds);
        }
    }

    /**
     * Assigns the specified roles to the shared user within the target organization.
     * This method ensures that the shared user receives the appropriate roles (the roles ids of the corresponding
     * roles in the sub organization) based on the mappings between shared and main roles.
     *
     * @param userAssociation       The user association object containing user and organization details.
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     * @param roleIds               The list of role IDs to be assigned.
     */
    private void assignRolesToTheSharedUser(UserAssociation userAssociation, String sharingInitiatedOrgId,
                                            List<String> roleIds) {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        try {
            String sharingInitiatedOrgTenantDomain =
                    getOrganizationManager().resolveTenantDomain(sharingInitiatedOrgId);
            String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

            String usernameWithDomain = userIDResolver.getNameByID(userId, targetOrgTenantDomain);
            String username = UserCoreUtil.removeDomainFromName(usernameWithDomain);
            String domainName = UserCoreUtil.extractDomainFromName(usernameWithDomain);

            RoleManagementService roleManagementService = getRoleManagementService();
            Map<String, String> sharedRoleToMainRoleMappingsBySubOrg =
                    roleManagementService.getSharedRoleToMainRoleMappingsBySubOrg(roleIds,
                            sharingInitiatedOrgTenantDomain);

            List<String> mainRoles = new ArrayList<>();
            for (String roleId : roleIds) {
                mainRoles.add(sharedRoleToMainRoleMappingsBySubOrg.getOrDefault(roleId, roleId));
            }

            Map<String, String> mainRoleToSharedRoleMappingsBySubOrg =
                    roleManagementService.getMainRoleToSharedRoleMappingsBySubOrg(mainRoles, targetOrgTenantDomain);

            for (String role : mainRoleToSharedRoleMappingsBySubOrg.values()) {
                roleManagementService.updateUserListOfRole(role, Collections.singletonList(userId),
                        Collections.emptyList(), targetOrgTenantDomain);
                roleManagementService.getRoleListOfUser(userId, targetOrgTenantDomain);
                addEditRestrictionsForSharedUserRole(role, username, targetOrgTenantDomain, domainName,
                        EditOperation.DELETE, sharingInitiatedOrgId);
            }
        } catch (OrganizationManagementException | IdentityRoleManagementException e) {
            LOG.error("Error occurred while assigning roles to the shared user: " + userId, e);
        }
    }

    /**
     * Adds or edits restrictions for a shared user role.
     *
     * @param roleId                The ID of the role to which restrictions are to be added or edited.
     * @param username              The username of the shared user.
     * @param targetOrgTenantDomain The tenant domain of the target organization.
     * @param domainName            The domain name of the user.
     * @param editOperation         The type of edit operation (ADD, EDIT, DELETE).
     * @param sharingInitiatedOrgId The ID of the organization that initiated the sharing.
     */
    private void addEditRestrictionsForSharedUserRole(String roleId, String username, String targetOrgTenantDomain,
                                                      String domainName, EditOperation editOperation,
                                                      String sharingInitiatedOrgId) {

        try {
            getOrganizationUserSharingService().addEditRestrictionsForSharedUserRole(roleId, username,
                    targetOrgTenantDomain, domainName, editOperation, sharingInitiatedOrgId);
        } catch (UserSharingMgtException e) {
            LOG.error("Error while adding/editing restrictions to the shared user role: " + roleId, e);
        }
    }

    /**
     * Removes roles that are no longer assigned to the shared user.
     *
     * @param userAssociation  The user association object containing user and organization details.
     * @param rolesToBeRemoved The list of role IDs to be removed.
     */
    private void unAssignOldSharedRolesFromSharedUser(UserAssociation userAssociation, List<String> rolesToBeRemoved)
            throws OrganizationManagementException, IdentityRoleManagementException {

        String userId = userAssociation.getUserId();
        String orgId = userAssociation.getOrganizationId();
        String targetOrgTenantDomain = getOrganizationManager().resolveTenantDomain(orgId);

        Map<String, String> mainRoleToSharedRoleMappingsBySubOrg =
                getRoleManagementService().getMainRoleToSharedRoleMappingsBySubOrg(rolesToBeRemoved,
                        targetOrgTenantDomain);

        for (String roleId : mainRoleToSharedRoleMappingsBySubOrg.values()) {
            getRoleManagementService().updateUserListOfRole(roleId, Collections.emptyList(),
                    Collections.singletonList(userId), targetOrgTenantDomain);
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
        LOG.debug("Sized role with audience list: " + roleWithAudienceList.size());
        return roleWithAudienceList;
    }

    private void logAsyncProcessing(String action, String sharingInitiatedUserId, String sharingInitiatedOrgId) {

        if (!LOG.isDebugEnabled()) {
            LOG.debug(String.format(ASYNC_PROCESSING_LOG_TEMPLATE, action, sharingInitiatedUserId,
                    sharingInitiatedOrgId));
        }
    }

    // Async helpers.

    /**
     * Restores thread-local properties for async execution.
     */
    private void restoreThreadLocalContext(String tenantDomain, int tenantId, String username,
                                           Map<String, Object> threadLocalProperties) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();

        carbonContext.setTenantDomain(tenantDomain, true);
        carbonContext.setTenantId(tenantId);
        carbonContext.setUsername(username);

        // Restore all thread-local properties.
        IdentityUtil.threadLocalProperties.get().putAll(threadLocalProperties);
    }

    // User Share Input Validation Methods.

    private <T extends UserCriteriaType> void validateUserShareInput(BaseUserShareDO<T> baseUserShareDO)
            throws UserSharingMgtClientException {

        if (baseUserShareDO == null) {
            throwValidationException(ERROR_CODE_NULL_SHARE);
        }

        if (baseUserShareDO instanceof SelectiveUserShareV2DO) {
            validateSelectiveUserShareV2DO((SelectiveUserShareV2DO) baseUserShareDO);
        } else if (baseUserShareDO instanceof GeneralUserShareV2DO) {
            validateGeneralUserShareV2DO((GeneralUserShareV2DO) baseUserShareDO);
        }
    }

    private void validateSelectiveUserShareV2DO(SelectiveUserShareV2DO selectiveUserShareV2DO)
            throws UserSharingMgtClientException {

        validateNotNull(selectiveUserShareV2DO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!selectiveUserShareV2DO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserShareV2DO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
        validateNotNull(selectiveUserShareV2DO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        for (SelectiveUserShareOrgDetailsV2DO orgDetails : selectiveUserShareV2DO.getOrganizations()) {
            validateNotNull(orgDetails, ERROR_CODE_ORG_DETAILS_NULL);
            validateNotNull(orgDetails.getOrganizationId(), ERROR_CODE_ORG_ID_NULL);
            validateNotNull(orgDetails.getPolicy(), ERROR_CODE_POLICY_NULL);
            validateRoleAssignments(orgDetails.getRoleAssignments());
        }

        LOG.debug("Validated selective user share V2 DO successfully.");
    }

    private void validateGeneralUserShareV2DO(GeneralUserShareV2DO generalUserShareV2DO)
            throws UserSharingMgtClientException {

        validateNotNull(generalUserShareV2DO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!generalUserShareV2DO.getUserCriteria().containsKey(USER_IDS) ||
                generalUserShareV2DO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }
        validateNotNull(generalUserShareV2DO.getPolicy(), ERROR_CODE_POLICY_NULL);
        validateRoleAssignments(generalUserShareV2DO.getRoleAssignments());

        LOG.debug("Validated general user share V2 DO successfully.");
    }

    // User Unshare Input Validation Methods.

    private <T extends UserCriteriaType> void validateUserUnshareInput(BaseUserUnshareDO<T> userUnshareDO)
            throws UserSharingMgtClientException {

        if (userUnshareDO == null) {
            throwValidationException(ERROR_CODE_NULL_UNSHARE);
        }

        if (userUnshareDO instanceof SelectiveUserUnshareDO) {
            validateSelectiveUserUnshareDO((SelectiveUserUnshareDO) userUnshareDO);
        } else if (userUnshareDO instanceof GeneralUserUnshareDO) {
            validateGeneralUserUnshareDO((GeneralUserUnshareDO) userUnshareDO);
        }
    }

    private void validateSelectiveUserUnshareDO(SelectiveUserUnshareDO selectiveUserUnshareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(selectiveUserUnshareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!selectiveUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                selectiveUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        // Validate organizations list is not null.
        validateNotNull(selectiveUserUnshareDO.getOrganizations(), ERROR_CODE_ORGANIZATIONS_NULL);

        for (String organization : selectiveUserUnshareDO.getOrganizations()) {
            validateNotNull(organization, ERROR_CODE_ORG_ID_NULL);
        }

        LOG.debug("Validated selective user unshare V2 DO successfully.");
    }

    private void validateGeneralUserUnshareDO(GeneralUserUnshareDO generalUserUnshareDO)
            throws UserSharingMgtClientException {

        // Validate userCriteria is not null.
        validateNotNull(generalUserUnshareDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);

        // Validate that userCriteria contains the required USER_IDS key and is not null.
        if (!generalUserUnshareDO.getUserCriteria().containsKey(USER_IDS) ||
                generalUserUnshareDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        LOG.debug("Validated general user unshare V2 DO successfully.");
    }

    // Role Assignment Update Input Validation Methods.

    private void validateSharedUserAttributeUpdateInput(UserSharePatchDO userSharePatchDO)
            throws UserSharingMgtClientException {

        // 1. validate UserSharePatchDO is not null.
        validateNotNull(userSharePatchDO, ERROR_CODE_REQUEST_BODY_NULL);

        // 2. validate user criteria.
        validateNotNull(userSharePatchDO.getUserCriteria(), ERROR_CODE_USER_CRITERIA_INVALID);
        if (!userSharePatchDO.getUserCriteria().containsKey(USER_IDS) ||
                userSharePatchDO.getUserCriteria().get(USER_IDS) == null) {
            throwValidationException(ERROR_CODE_USER_CRITERIA_MISSING);
        }

        // 3. validate operations is not null (empty list is okay).
        validateNotNull(userSharePatchDO.getPatchOperations(), ERROR_CODE_PATCH_OPERATIONS_NULL);

        // 4. for each operation object.
        for (PatchOperationDO patchOperation : userSharePatchDO.getPatchOperations()) {

            // 4.1 validate PatchOperation object is not null.
            validateNotNull(patchOperation, ERROR_CODE_PATCH_OPERATION_NULL);

            // 4.2 validate operation is a valid value of enum
            validateNotNull(patchOperation.getOperation(), ERROR_CODE_PATCH_OPERATION_OP_NULL);
            validatePatchOperationValue(patchOperation.getOperation());

            // 4.3 validate path is a currently supported path
            validateNotNull(patchOperation.getPath(), ERROR_CODE_PATCH_OPERATION_PATH_NULL);
            String pathType = resolveAndValidatePatchPath(patchOperation.getPath());

            // 4.4 values cannot be null (empty list is okay)
            validateNotNull(patchOperation.getValues(), ERROR_CODE_PATCH_OPERATION_VALUE_NULL);

            // 4.4.1 If the path is roles, validate value list as RoleWithAudienceDO list
            validatePatchValuesAgainstPath(pathType, patchOperation.getValues());
        }

        LOG.debug("Validated shared user attribute update input successfully.");
    }

    private void validatePatchOperationValue(UserSharePatchOperation operation) throws UserSharingMgtClientException {

        if (operation != UserSharePatchOperation.ADD && operation != UserSharePatchOperation.REMOVE) {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_OP_INVALID);
        }
    }

    private String resolveAndValidatePatchPath(String path) throws UserSharingMgtClientException {

        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_PATH_INVALID);
        }

        // Patch operation path: roles (patching roles)
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

    private void validateOrgRolesPath(String path) throws UserSharingMgtClientException {

        // organizations[orgId eq <uuid>].roles.
        String orgId = extractOrgIdFromRolesPath(path);

        if (orgId.isEmpty()) {
            throwValidationException(ERROR_CODE_ORG_ID_NULL);
        }

        validateUuid(orgId);
    }

    private String extractOrgIdFromRolesPath(String path) {

        return path.substring(PATCH_PATH_PREFIX.length(), path.length() - PATCH_PATH_SUFFIX_ROLES.length()).trim();
    }

    private void validatePatchValuesAgainstPath(String pathType, Object values)
            throws UserSharingMgtClientException {

        if (StringUtils.equals(pathType, PATCH_PATH_ROLES)) {
            validateRoleWithAudienceValues(values);
        } else { // Should not happen since resolveAndValidatePatchPath guards this.
            throwValidationException(ERROR_CODE_PATCH_OPERATION_PATH_UNSUPPORTED);
        }
    }

    private void validateRoleWithAudienceValues(Object values) throws UserSharingMgtClientException {

        // value can be empty list, but must be a list
        if (!(values instanceof List)) {
            throwValidationException(ERROR_CODE_PATCH_OPERATION_VALUE_TYPE_INVALID);
        }

        List<?> list = (List<?>) values;

        // Empty list is allowed (your requirement)
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

    // Shared User GET Input Validation Methods.

    private void validateSharedUserGetInput(GetUserSharedOrgsDO getUserSharedOrgsDO)
            throws UserSharingMgtClientException {

        // 1. validate request object is not null.
        validateNotNull(getUserSharedOrgsDO, ERROR_CODE_REQUEST_BODY_NULL);

        // 2. validate required identifiers (service invariants).
        validateNotNull(getUserSharedOrgsDO.getUserId(), ERROR_CODE_USER_ID_NULL);
        validateNotNull(getUserSharedOrgsDO.getParentOrgId(), ERROR_CODE_ORG_ID_NULL);

        validateNotNull(getUserSharedOrgsDO.getFilter(), ERROR_CODE_FILTER_NULL);

        validateGetAttributes(getUserSharedOrgsDO.getAttributes());
    }

    private void validateGetAttributes(List<String> attributes) throws UserSharingMgtClientException {

        validateNotNull(attributes, ERROR_CODE_GET_ATTRIBUTES_NULL);

        for (String attribute : attributes) {
            validateNotNull(attribute, ERROR_CODE_GET_ATTRIBUTE_NULL);

            if (!SUPPORTED_GET_ATTRIBUTES.contains(attribute)) {
                throwValidationException(ERROR_CODE_GET_ATTRIBUTE_UNSUPPORTED);
            }
        }
    }

    // Helper Validation Methods.

    private void validateUuid(String uuid) throws UserSharingMgtClientException {

        if (uuid == null || uuid.trim().isEmpty()) {
            throwValidationException(ERROR_CODE_ORG_ID_NULL);
        }

        try {
            java.util.UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throwValidationException(ERROR_CODE_ORG_ID_INVALID_FORMAT);
        }
    }

    private void validateRoleAssignments(RoleAssignmentDO roleAssignments) throws UserSharingMgtClientException {

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

    private void validateNotNull(Object obj, UserSharingConstants.ErrorMessage error)
            throws UserSharingMgtClientException {

        if (obj == null) {
            throwValidationException(error);
        }
    }

    private void throwValidationException(UserSharingConstants.ErrorMessage error)
            throws UserSharingMgtClientException {

        throw new UserSharingMgtClientException(error.getCode(), error.getMessage(), error.getDescription());
    }

    // Service getters.

    private OrganizationUserSharingService getOrganizationUserSharingService() {

        return OrganizationUserSharingDataHolder.getInstance().getOrganizationUserSharingService();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrganizationUserSharingDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private RoleManagementService getRoleManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getRoleManagementService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrganizationUserSharingDataHolder.getInstance().getApplicationManagementService();
    }

    private AbstractUserStoreManager getAbstractUserStoreManager(int tenantId) throws UserStoreException {

        try {
            RealmService realmService = OrganizationUserSharingDataHolder.getInstance().getRealmService();
            UserRealm tenantUserRealm = realmService.getTenantUserRealm(tenantId);
            return (AbstractUserStoreManager) tenantUserRealm.getUserStoreManager();
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new UserStoreException("Error occurred while retrieving the user store manager.", e);
        }
    }
}
