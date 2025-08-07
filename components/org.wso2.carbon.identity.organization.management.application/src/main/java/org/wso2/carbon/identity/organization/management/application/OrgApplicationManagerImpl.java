/*
 * Copyright (c) 2022-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.model.ExpressionNode;
import org.wso2.carbon.identity.core.model.FilterTreeBuilder;
import org.wso2.carbon.identity.core.model.Node;
import org.wso2.carbon.identity.core.model.OperationNode;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventClientException;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.buffer.SubOperationStatusQueue;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.constants.OperationStatus;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.exception.AsyncOperationStatusMgtException;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.models.OperationInitDTO;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.models.UnitOperationInitDTO;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.service.AsyncOperationStatusMgtService;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.dto.OAuthAppRevocationRequestDTO;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.listener.ApplicationSharingManagerListener;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.RoleWithAudienceDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationOrganizationNode;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationOrganizationNodePage;
import org.wso2.carbon.identity.organization.management.application.model.SharingModeDO;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareUpdateOperation;
import org.wso2.carbon.identity.organization.management.application.model.operation.GeneralApplicationShareOperation;
import org.wso2.carbon.identity.organization.management.application.model.operation.SelectiveShareApplicationOperation;
import org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil;
import org.wso2.carbon.identity.organization.management.application.util.OrgApplicationScimFilterParser;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationNode;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtClientException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.model.SharedResourceAttribute;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.Role;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.AuditLog;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.OAUTH2;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ORGANIZATION_CONTEXT_PREFIX;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.TENANT_CONTEXT_PREFIX;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_DEFAULT;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_FLOW;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.DEFAULT_BACKCHANNEL_LOGOUT_URL;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.MYACCOUNT_PORTAL_PATH;
import static org.wso2.carbon.identity.application.mgt.ApplicationMgtUtil.getAppId;
import static org.wso2.carbon.identity.base.IdentityConstants.SKIP_CONSENT;
import static org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils.triggerAuditLogEvent;
import static org.wso2.carbon.identity.core.util.IdentityUtil.getInitiatorId;
import static org.wso2.carbon.identity.oauth.Error.DUPLICATE_OAUTH_CLIENT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.AUTH_TYPE_OAUTH_2;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.B2B_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.CORRELATION_ID_MDC;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_FRAGMENT_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_SHARE_FOR_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APP_ROLE_ALLOWED_AUDIENCE;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_SHARED_APP_ROLES;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_SHARE_CONFIGURATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ROLE_SHARING_MODE;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ROLE_SHARING_OPERATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SELECTIVE_SHARING_POLICY;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_SHARING_ORG_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ORGANIZATION_LOGIN_AUTHENTICATOR;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.PARENT_ORGANIZATION_ID;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SP_SHARED_ROLE_EXCLUDED_KEY;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SP_SHARED_SHARING_MODE_INCLUDED_KEY;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SP_SHARED_SUPPORTED_EXCLUDED_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SP_SHARED_SUPPORTED_INCLUDED_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ShareOperationType.APPLICATION_SHARE;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.TENANT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.TENANT_CONTEXT_PATH_COMPONENT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.createOrganizationSSOIDP;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getDefaultAuthenticationConfig;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.removeShareWithAllChildrenProperty;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setAppAssociatedRoleSharingMode;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setIsAppSharedProperty;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setShareWithAllChildrenProperty;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationScimFilterParser.parseFilter;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationShareProcessor.getAllOrganizationIdsInBfsOrder;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationShareProcessor.getOrganizationIdsInBfsOrder;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationShareProcessor.getValidOrganizationsInReverseBfsOrder;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationShareProcessor.processAndSortOrganizationShares;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationShareProcessor.sortOrganizationsByHierarchy;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.AND;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ASC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.DESC_SORT_ORDER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ADDING_SHARED_RESOURCE_ATTRIBUTES_FAILED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_APPLICATION_NOT_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_BLOCK_SHARING_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_OAUTH_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_ORG_LOGIN_IDP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_REMOVING_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APPLICATION_SHARED_ACCESS_STATUS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_IDP_LIST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_REVOKING_SHARED_APP_TOKENS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION_NAME_CONFLICT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_CURSOR_FOR_PAGINATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_DELETE_SHARE_REQUEST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_FILTER_FORMAT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_SHARE_OPERATION_TYPE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_APPLICATION_SHARE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_FRAGMENT_APP_ACCESS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_COMPLEX_QUERY_IN_FILTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_FILTER_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNSUPPORTED_SHARE_OPERATION_PATH;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.IS_APP_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ATTRIBUTES_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ATTRIBUTES_FIELD_PREFIX;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_ID_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ORGANIZATION_NAME_FIELD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_AFTER;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PAGINATION_BEFORE;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Service implementation to process applications across organizations. Class implements {@link OrgApplicationManager}.
 */
public class OrgApplicationManagerImpl implements OrgApplicationManager {

    private static final String PARENT_APP_ID = "parentAppId";
    private static final String SHARED_TENANT_DOMAINS = "sharedTenantDomains";
    private static final String ACTION_PROCESSING_SHARE_APP = "processing-share-application-with-selected-orgs";

    private static final Log LOG = LogFactory.getLog(OrgApplicationManagerImpl.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final ConcurrentMap<String, SubOperationStatusQueue> asyncOperationStatusList = new ConcurrentHashMap<>();

    @Override
    public void shareOrganizationApplication(String ownerOrgId, String originalAppId, boolean shareWithAllChildren,
                                             List<String> sharedOrgs) throws OrganizationManagementException {

        if (!shareWithAllChildren && CollectionUtils.isEmpty(sharedOrgs)) {
            return;
        }
        String ownerTenantDomain = getOrganizationManager().resolveTenantDomain(ownerOrgId);
        ServiceProvider rootApplication = getOrgApplication(originalAppId, ownerTenantDomain);
        // check if share with all children property needs to be updated.
        boolean updateShareWithAllChildren = shouldUpdateShareWithAllChildren(shareWithAllChildren, rootApplication);
        if (updateShareWithAllChildren) {
            try {
                IdentityUtil.threadLocalProperties.get().put(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN, true);
                setShareWithAllChildrenProperty(rootApplication, shareWithAllChildren);
                getApplicationManagementService().updateApplication(rootApplication,
                        ownerTenantDomain, getAuthenticatedUsername());
                getOrgApplicationMgtDAO().updateShareWithAllChildren(rootApplication.getApplicationResourceId(),
                        ownerOrgId, shareWithAllChildren);
            } catch (IdentityApplicationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, originalAppId);
            } finally {
                IdentityUtil.threadLocalProperties.get().remove(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN);
            }
        }

        if (shareWithAllChildren) {
            ApplicationShareRolePolicy applicationShareRolePolicy = new ApplicationShareRolePolicy.Builder()
                    .mode(ApplicationShareRolePolicy.Mode.ALL)
                    .build();
            GeneralApplicationShareOperation shareWithAllChildrenWithAllRoles = new GeneralApplicationShareOperation(
                    PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS, applicationShareRolePolicy);
            shareApplicationWithAllOrganizations(ownerOrgId, originalAppId, shareWithAllChildrenWithAllRoles);
            return;
        }
        List<SelectiveShareApplicationOperation> selectiveShareApplicationList = new ArrayList<>();
        ApplicationShareRolePolicy applicationShareRolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();

        for (String sharingOrg : sharedOrgs) {
            SelectiveShareApplicationOperation selectiveShareApplication = new SelectiveShareApplicationOperation(
                    sharingOrg, PolicyEnum.SELECTED_ORG_ONLY, applicationShareRolePolicy);
            selectiveShareApplicationList.add(selectiveShareApplication);
        }
        shareApplicationWithSelectedOrganizations(ownerOrgId, originalAppId, selectiveShareApplicationList);
    }

    @Override
    public void shareApplicationWithSelectedOrganizations(String mainOrganizationId, String mainApplicationId,
                                                          List<SelectiveShareApplicationOperation>
                                                              selectiveShareApplicationList)
            throws OrganizationManagementException {

        validateSelectiveApplicationShareConfigs(selectiveShareApplicationList);

        // Extract application and organization details.
        String ownerTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        ServiceProvider mainApplication = getOrgApplication(mainApplicationId, ownerTenantDomain);

        // Prevent sharing if the application is a shared (fragment) app.
        if (isSharedApplication(mainApplication)) {
            throw handleClientException(ERROR_CODE_BLOCK_SHARING_SHARED_APP, mainApplicationId);
        }

        // Get organization share config list.
        if (CollectionUtils.isEmpty(selectiveShareApplicationList)) {
            return;
        }

        // Get all child organizations of the owner organization and create a lookup map for faster access.
        List<OrganizationNode> childOrganizationGraph = getOrganizationManager().getChildOrganizationGraph(
                mainOrganizationId, true);

        Set<String> subOrganizationIdsSet = new HashSet<>(getOrganizationIdsInBfsOrder(childOrganizationGraph));

        if (childOrganizationGraph.isEmpty()) {
            return;
        }

        // Filter only valid child org configs that exist in the owner's child organizations.
        List<SelectiveShareApplicationOperation> filteredChildOrgConfigs = selectiveShareApplicationList.stream()
                .filter(config -> {
                    String organizationId = config.getOrganizationId();
                    boolean isValid = subOrganizationIdsSet.contains(organizationId);
                    if (!isValid && LOG.isDebugEnabled() && organizationId != null) {
                        LOG.debug("Application can only be shared with child organizations within the hierarchy. " +
                                "Provided organization ID: " + organizationId + " is not found within the hierarchy.");
                    }
                    return isValid;
                }).collect(Collectors.toList());

        if (filteredChildOrgConfigs.isEmpty()) {
            return;
        }

        /*
         * Process and sort organization shares.
         *
         * Organizations are arranged so that parent organizations are processed before their
         * corresponding child organizations. This ordering ensures that application sharing
         * follows the correct hierarchical structure and respects the organization hierarchy.
         */
        List<SelectiveShareApplicationOperation> selectiveShareApplicationOperations = processAndSortOrganizationShares(
                childOrganizationGraph, filteredChildOrgConfigs);

        // If there are valid orgs to share with, update the root application with the org login IDP.
        modifyRootApplication(mainApplication, ownerTenantDomain);

        String userID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
        String sharePolicy = OrgApplicationMgtConstants.SharePolicy.SELECTIVE_SHARE.getValue();
        String operationId = getOperationId(mainApplicationId, mainApplicationId, userID, sharePolicy);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        List<String> orgIdsToShare = new ArrayList<>();
        // Share the application with each valid child organization as per the config.
        for (SelectiveShareApplicationOperation selectiveShareApplication : selectiveShareApplicationOperations) {
            String childOrgId = selectiveShareApplication.getOrganizationId();
            if (StringUtils.isBlank(childOrgId)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION, mainApplicationId);
            }

            Organization sharingChildOrg = getOrganizationManager().getOrganization(childOrgId, false, false);
            // Only share with organizations of type TENANT.
            if (TENANT.equalsIgnoreCase(sharingChildOrg.getType())) {
                orgIdsToShare.add(sharingChildOrg.getId());
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        shareApplicationWithPolicy(
                                mainOrganizationId,
                                mainApplication,
                                sharingChildOrg.getId(),
                                selectiveShareApplication.getPolicy(),
                                selectiveShareApplication.getRoleSharing(),
                                operationId
                        );
                    } catch (OrganizationManagementException e) {
                        LOG.error(String.format("Error in sharing application: %s to sharingChildOrg: %s",
                                mainApplication.getApplicationResourceId(), sharingChildOrg.getId()), e);
                    }
                }, executorService);
                futures.add(future);
            }
        }
        if (StringUtils.isNotBlank(operationId)) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                try {
                    getAsyncStatusMgtService().updateOperationStatus(operationId, getOperationStatus(operationId));
                } catch (AsyncOperationStatusMgtException e) {
                    try {
                        throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION_SHARED_ACCESS_STATUS, e);
                    } catch (OrganizationManagementServerException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }).join();
        }
        if (LoggerUtils.isEnableV2AuditLogs()) {
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(username, ownerTenantDomain),  LoggerUtils.Target.User.name(),
                    getAppId(mainApplication), LoggerUtils.Target.Application.name(), ACTION_PROCESSING_SHARE_APP)
                    .data(buildShareAppAuditData(mainApplicationId, orgIdsToShare));
            triggerAuditLogEvent(auditLogBuilder, true);
        }
    }

    private Map<String, String> buildShareAppAuditData(String mainApplicationId, List<String> sharedTenantDomainList) {

        Map<String, String> auditData = new HashMap<>();
        auditData.put(PARENT_APP_ID, mainApplicationId);
        auditData.put(SHARED_TENANT_DOMAINS, sharedTenantDomainList.toString());
        return auditData;
    }

    private void validateSelectiveApplicationShareConfigs(List<SelectiveShareApplicationOperation>
                                                                  selectiveShareApplicationList)
            throws OrganizationManagementClientException {

        if (selectiveShareApplicationList == null) {
            throw OrgApplicationManagerUtil.handleClientException(ERROR_CODE_INVALID_ORGANIZATION_SHARE_CONFIGURATION);
        }
        for (SelectiveShareApplicationOperation selectiveShareApplication : selectiveShareApplicationList) {
            if (StringUtils.isBlank(selectiveShareApplication.getOrganizationId())) {
                throw OrgApplicationManagerUtil.handleClientException(ERROR_CODE_INVALID_SHARING_ORG_ID);
            }
            if (selectiveShareApplication.getPolicy() == null) {
                throw OrgApplicationManagerUtil.handleClientException(ERROR_CODE_INVALID_SELECTIVE_SHARING_POLICY);
            }
            if (selectiveShareApplication.getRoleSharing() == null) {
                throw OrgApplicationManagerUtil.handleClientException(ERROR_CODE_INVALID_ROLE_SHARING_OPERATION);
            }
            if (selectiveShareApplication.getRoleSharing().getMode() == null) {
                throw OrgApplicationManagerUtil.handleClientException(ERROR_CODE_INVALID_ROLE_SHARING_MODE);
            }
        }
    }

    @Override
    public void shareApplicationWithAllOrganizations(String mainOrganizationId, String mainApplicationId,
                                                    GeneralApplicationShareOperation generalApplicationShare)
            throws OrganizationManagementException {

        // Extract application and organization details.
        String ownerTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        ServiceProvider mainApplication = getOrgApplication(mainApplicationId, ownerTenantDomain);

        // Prevent sharing if the application is already a shared (fragment) app.
        if (isSharedApplication(mainApplication)) {
            throw handleClientException(ERROR_CODE_BLOCK_SHARING_SHARED_APP, mainApplicationId);
        }

        // Adding the main org ID to the list of orgs to share with if there's future sharing policy.
        addOrUpdatePolicy(mainApplicationId, mainOrganizationId, mainOrganizationId, ownerTenantDomain,
                generalApplicationShare.getPolicy(), generalApplicationShare.getRoleSharing());

        // Add the role sharing config to the main application.
        ApplicationShareRolePolicy.Mode roleSharingMode = generalApplicationShare.getRoleSharing().getMode();
        setAppAssociatedRoleSharingMode(mainApplication, roleSharingMode);
        if (ApplicationShareRolePolicy.Mode.ALL.ordinal() == roleSharingMode.ordinal()) {
            setShareWithAllChildrenProperty(mainApplication, true);
        } else {
            removeShareWithAllChildrenProperty(mainApplication);
        }
        try {
            getApplicationManagementService().updateApplication(mainApplication, ownerTenantDomain,
                    getAuthenticatedUsername());
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, mainApplicationId);
        }

        // Get all child organizations of the owner organization and create a lookup map for faster access.
        List<BasicOrganization> childOrganizations = getOrganizationManager().getChildOrganizations(mainOrganizationId,
                true);
        if (childOrganizations.isEmpty()) {
            return;
        }

        List<String> allOrganizationIdsInBfsOrder = getAllOrganizationIdsInBfsOrder(mainOrganizationId);

        if (PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.ordinal() == generalApplicationShare.getPolicy().ordinal() ||
                !allOrganizationIdsInBfsOrder.isEmpty()) {
            // If there are valid orgs to share with, update the root application with the org login IDP.
            modifyRootApplication(mainApplication, ownerTenantDomain);
        }

        if (allOrganizationIdsInBfsOrder.isEmpty()) {
            return;
        }

        String userID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
        String sharePolicy = generalApplicationShare.getPolicy().getValue();
        String operationId = getOperationId(mainApplicationId, mainApplicationId, userID, sharePolicy);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        List<String> orgIdsToShare = new ArrayList<>();
        // Share the application with each valid child organization as per the config.
        for (String childOrgId : allOrganizationIdsInBfsOrder) {
            Organization sharingChildOrg = getOrganizationManager().getOrganization(childOrgId, false, false);
            // Only share with organizations of type TENANT.
            if (TENANT.equalsIgnoreCase(sharingChildOrg.getType())) {
                orgIdsToShare.add(sharingChildOrg.getId());
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        shareApplicationWithPolicy(
                                mainOrganizationId,
                                mainApplication,
                                sharingChildOrg.getId(),
                                PolicyEnum.SELECTED_ORG_ONLY,
                                generalApplicationShare.getRoleSharing(),
                                operationId
                        );
                    } catch (OrganizationManagementException e) {
                        LOG.error(String.format("Error in sharing application: %s to sharingChildOrg: %s",
                                mainApplication.getApplicationResourceId(), sharingChildOrg.getId()), e);
                    }
                }, executorService);
                futures.add(future);
            }
        }
        if (StringUtils.isNotBlank(operationId)) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                try {
                    getAsyncStatusMgtService().updateOperationStatus(operationId, getOperationStatus(operationId));
                } catch (AsyncOperationStatusMgtException e) {
                    try {
                        throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION_SHARED_ACCESS_STATUS, e);
                    } catch (OrganizationManagementServerException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }).join();
        }
        if (LoggerUtils.isEnableV2AuditLogs()) {
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(username, ownerTenantDomain), LoggerUtils.Target.User.name(),
                    getAppId(mainApplication), LoggerUtils.Target.Application.name(),
                    "processing-share-application-with-all-orgs")
                    .data(buildShareAppAuditData(mainApplicationId, orgIdsToShare));
            triggerAuditLogEvent(auditLogBuilder, true);
        }
    }

    @Override
    public void updateSharedApplication(String mainOrganizationId, String mainApplicationId,
                                        List<ApplicationShareUpdateOperation> updateOperationList)
            throws OrganizationManagementException {

        String mainTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        ServiceProvider mainApplication = getOrgApplication(mainApplicationId, mainTenantDomain);

        Map<String, Map<ApplicationShareUpdateOperation.Operation, List<RoleWithAudienceDO>>>
                organizationIdToOperationMap = new HashMap<>();
        Set<String> organizationsToBeUpdated = new HashSet<>();

        for (ApplicationShareUpdateOperation updateOperation : updateOperationList) {
            ApplicationShareUpdateOperation.Operation operation = updateOperation.getOperation();
            if (!(ApplicationShareUpdateOperation.Operation.ADD.ordinal() == operation.ordinal() ||
                    ApplicationShareUpdateOperation.Operation.REMOVE.ordinal() == operation.ordinal())) {
                throw handleClientException(ERROR_CODE_INVALID_SHARE_OPERATION_TYPE, operation.name());
            }
            OrgApplicationScimFilterParser.ParsedFilterResult parsedFilterResult = parseFilter(
                    updateOperation.getPath());
            if (!parsedFilterResult.hasPathAttribute()) {
                throw handleClientException(ERROR_CODE_UNSUPPORTED_SHARE_OPERATION_PATH, updateOperation.getPath());
            }
            String orgId = parsedFilterResult.getOrganizationId();
            List<RoleWithAudienceDO> roleChanges = (List<RoleWithAudienceDO>) updateOperation.getValues();
            organizationsToBeUpdated.add(orgId);
            organizationIdToOperationMap
                    .computeIfAbsent(orgId, k -> new HashMap<>())
                    .merge(operation, roleChanges, (existing, incoming) -> {
                        existing.addAll(incoming);
                        return existing;
                    });
        }
        // Sort out the organization list.
        List<String> sortedOrganizations = sortOrganizationsByHierarchy(mainOrganizationId,
                new ArrayList<>(organizationsToBeUpdated));
        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        String userID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();
        for (String sharedOrganizationId : sortedOrganizations) {
            Organization organization = getOrganizationManager().getOrganization(sharedOrganizationId, false, false);
            if (TENANT.equalsIgnoreCase(organization.getType())) {
                Map<ApplicationShareUpdateOperation.Operation, List<RoleWithAudienceDO>> operationListMap =
                        organizationIdToOperationMap.get(sharedOrganizationId);
                for (Map.Entry<ApplicationShareUpdateOperation.Operation, List<RoleWithAudienceDO>> entry :
                        operationListMap.entrySet()) {
                    ApplicationShareUpdateOperation.Operation operation = entry.getKey();
                    List<RoleWithAudienceDO> roleList = entry.getValue();
                    CompletableFuture.runAsync(() -> {
                        try {
                            updateRolesOfSharedApplication(mainApplication, mainOrganizationId, sharedOrganizationId,
                                    operation, roleList, username, userID);
                        } catch (OrganizationManagementException e) {
                            LOG.error(String.format("Error in updating roles of application: %s for organization: %s",
                                    mainApplicationId, sharedOrganizationId), e);
                        }
                    }, executorService);
                }
            }
        }
        if (LoggerUtils.isEnableV2AuditLogs()) {
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(username, mainTenantDomain), LoggerUtils.Target.User.name(),
                    getAppId(mainApplication), LoggerUtils.Target.Application.name(),
                    "processing-update-shared-application")
                    .data(buildShareAppAuditData(mainApplicationId, sortedOrganizations));
            triggerAuditLogEvent(auditLogBuilder, true);
        }
    }


    private String getOperationId(String originalAppId, String ownerOrgId, String userID, String sharePolicy)
            throws OrganizationManagementServerException {

        try {
            String operationId = getAsyncStatusMgtService().registerOperationStatus(
                    new OperationInitDTO(getCorrelation(), APPLICATION_SHARE.getValue(), B2B_APPLICATION,
                            originalAppId, ownerOrgId, userID, sharePolicy), false);

            // If Async Operation Status persistence is disabled, operationId will not be returned.
            if (StringUtils.isNotBlank(operationId)) {
                SubOperationStatusQueue statusQueue = new SubOperationStatusQueue();
                asyncOperationStatusList.put(operationId, statusQueue);
            }
            return operationId;
        } catch (AsyncOperationStatusMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION_SHARED_ACCESS_STATUS, e, originalAppId);
        }
    }

    private OperationStatus getOperationStatus(String operationId) {

        SubOperationStatusQueue list = asyncOperationStatusList.get(operationId);
        OperationStatus status = list.getOperationStatus();
        asyncOperationStatusList.remove(operationId);
        return status;
    }

    private String getCorrelation() {

        String ref;
        if (isCorrelationIDPresent()) {
            ref = MDC.get(CORRELATION_ID_MDC);
        } else {
            ref = UUID.randomUUID().toString();
        }
        return ref;
    }

    private boolean isCorrelationIDPresent() {

        return MDC.get(CORRELATION_ID_MDC) != null;
    }

    private void updateRolesOfSharedApplication(ServiceProvider mainApplication, String mainOrgId, String sharedOrgId,
                                                ApplicationShareUpdateOperation.Operation operation,
                                                List<RoleWithAudienceDO> roleChanges, String requestInitiatedUserName,
                                                String requestInitiatedUserId) throws OrganizationManagementException {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            String sharedOrgHandle = getOrganizationManager().resolveTenantDomain(sharedOrgId);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedOrgHandle, true);
            getListener().preUpdateRolesOfSharedApplication(mainOrgId, mainApplication.getApplicationResourceId(),
                    sharedOrgId, operation, roleChanges);
            // Set the request initiated user information, which will be used for the auditing purposes.
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserId(requestInitiatedUserId);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(requestInitiatedUserName);
            Optional<String> sharedApplicationResourceId;
            try {
                sharedApplicationResourceId = resolveSharedApp(mainApplication.getApplicationResourceId(), mainOrgId,
                        sharedOrgId);
            } catch (OrganizationManagementException e) {
                LOG.warn("Error while resolving shared application: " + mainApplication.getApplicationResourceId(), e);
                return;
            }
            if (!sharedApplicationResourceId.isPresent()) {
                LOG.warn("Shared application not found for organization: " + sharedOrgId);
                return;
            }
            // We will manage the role changes from the listeners since it was how the role management is already
            // done with shared application.
            getListener().postUpdateRolesOfSharedApplication(mainOrgId, mainApplication.getApplicationResourceId(),
                    sharedOrgId, sharedApplicationResourceId.get(), operation, roleChanges);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Override
    public void unshareApplicationFromSelectedOrganizations(String mainOrganizationId, String mainApplicationId,
                                   List<String> sharedOrganizationList) throws OrganizationManagementException {

        String mainTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        ServiceProvider mainApplication = getOrgApplication(mainApplicationId, mainTenantDomain);
        List<String> validOrganizationsInReverseBfsOrder = getValidOrganizationsInReverseBfsOrder(mainOrganizationId,
                sharedOrganizationList);
        for (String sharedOrganizationId : validOrganizationsInReverseBfsOrder) {
            Organization organization = getOrganizationManager().getOrganization(sharedOrganizationId, false, false);
            if (TENANT.equalsIgnoreCase(organization.getType())) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Optional<String> optionalSharedApplicationId =
                                resolveSharedApp(mainApplicationId, mainOrganizationId, organization.getId());
                        if (!optionalSharedApplicationId.isPresent()) {
                            LOG.debug(String.format("Shared application not found for organization: %s",
                                    organization.getId()));
                            return;
                        }
                        getListener().preDeleteSharedApplication(mainOrganizationId, mainApplicationId,
                                sharedOrganizationId);
                        String shareApplicationId = optionalSharedApplicationId.get();
                        deleteExistingSharedApplication(mainOrganizationId, organization.getId(), mainApplication,
                                shareApplicationId);
                        getListener().postDeleteSharedApplication(mainOrganizationId, mainApplicationId,
                                sharedOrganizationId, shareApplicationId);
                    } catch (OrganizationManagementException e) {
                        LOG.error(String.format("Error in unsharing application: %s from organization: %s",
                                mainApplicationId, sharedOrganizationId), e);
                    }
                }, executorService);
            }
        }
        if (LoggerUtils.isEnableV2AuditLogs()) {
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(username, mainTenantDomain), LoggerUtils.Target.User.name(),
                    getAppId(mainApplication), LoggerUtils.Target.Application.name(),
                    "processing-unshare-application-from-selected-orgs")
                    .data(buildShareAppAuditData(mainApplicationId, validOrganizationsInReverseBfsOrder));
            triggerAuditLogEvent(auditLogBuilder, true);
        }
    }

    @Override
    public void unshareAllApplicationFromAllOrganizations(String mainOrganizationId, String mainApplicationId)
            throws OrganizationManagementException {

        String mainTenantDomain = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        ServiceProvider mainApplication = getOrgApplication(mainApplicationId, mainTenantDomain);
        getListener().preDeleteAllSharedApplications(mainOrganizationId, mainApplicationId);
        // Unshare application for all shared organizations.
        List<SharedApplicationDO> sharedApplicationDOList =
                getOrgApplicationMgtDAO().getSharedApplications(mainOrganizationId, mainApplicationId);
        IdentityUtil.threadLocalProperties.get().put(DELETE_SHARE_FOR_MAIN_APPLICATION, true);
        List<String> unsharingOrganizations = new ArrayList<>();
        for (SharedApplicationDO sharedApplicationDO : sharedApplicationDOList) {
            unsharingOrganizations.add(sharedApplicationDO.getOrganizationId());
            CompletableFuture.runAsync(() -> {
                try {
                    IdentityUtil.threadLocalProperties.get().put(DELETE_SHARE_FOR_MAIN_APPLICATION, true);
                    deleteExistingSharedApplication(mainOrganizationId, sharedApplicationDO.getOrganizationId(),
                            mainApplication, sharedApplicationDO.getFragmentApplicationId());
                } catch (OrganizationManagementException e) {
                    LOG.error(String.format("Error in unsharing application: %s from organization: %s",
                            mainApplicationId, sharedApplicationDO.getOrganizationId()), e);
                }
            }, executorService);
        }
        if (LoggerUtils.isEnableV2AuditLogs()) {
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                    getInitiatorId(username, mainTenantDomain), LoggerUtils.Target.User.name(),
                    getAppId(mainApplication), LoggerUtils.Target.Application.name(),
                    "processing-unshare-application-from-all-orgs")
                    .data(buildShareAppAuditData(mainApplicationId, unsharingOrganizations));
            triggerAuditLogEvent(auditLogBuilder, true);
        }
        getListener().postDeleteAllSharedApplications(mainOrganizationId, mainApplicationId, sharedApplicationDOList);
        boolean isSharedWithAllChildren = stream(mainApplication.getSpProperties())
                .anyMatch(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && Boolean.parseBoolean(p.getValue()));
        boolean isAppShared = stream(mainApplication.getSpProperties())
                .anyMatch(p -> IS_APP_SHARED.equals(p.getName()) && Boolean.parseBoolean(p.getValue()));
        if (isSharedWithAllChildren || isAppShared) {
            setShareWithAllChildrenProperty(mainApplication, false);
            setIsAppSharedProperty(mainApplication, false);
            IdentityUtil.threadLocalProperties.get().put(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN, true);
            try {
                getApplicationManagementService().updateApplication(
                        mainApplication, getTenantDomain(), getAuthenticatedUsername());
            } catch (IdentityApplicationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, mainApplicationId);
            } finally {
                IdentityUtil.threadLocalProperties.get().remove(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN);
            }
        }
        try {
            // This is to delete the main organization application sharing policy.
            getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                    mainOrganizationId, ResourceType.APPLICATION, mainApplicationId, mainOrganizationId);
        } catch (ResourceSharingPolicyMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, mainApplicationId);
        }
    }

    @Override
    public void deleteSharedApplication(String organizationId, String applicationId, String sharedOrganizationId)
            throws OrganizationManagementException {

        validateFragmentApplicationAccess(getOrganizationId(), organizationId);
        if (sharedOrganizationId == null) {
            unshareAllApplicationFromAllOrganizations(organizationId, applicationId);
        } else {
            ServiceProvider serviceProvider = getOrgApplication(applicationId, getTenantDomain());
            if (stream(serviceProvider.getSpProperties())
                    .anyMatch(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
                throw handleClientException(ERROR_CODE_INVALID_DELETE_SHARE_REQUEST,
                        serviceProvider.getApplicationResourceId(), sharedOrganizationId);
            }
            unshareApplicationFromSelectedOrganizations(organizationId, applicationId, Collections.singletonList(
                    sharedOrganizationId));
        }
    }

    /**
     * Deletes the existing shared application in the given organization. Also
     *
     * @param mainOrgId            Main organization ID.
     * @param sharedOrganizationId Shared organization ID.
     * @param mainApplication      Main application to be shared.
     * @param sharedApplicationId  Shared application ID to be deleted.
     * @throws OrganizationManagementException If an error occurs while deleting the shared application.
     */
    private void deleteExistingSharedApplication(String mainOrgId, String sharedOrganizationId,
                                                 ServiceProvider mainApplication, String sharedApplicationId)
            throws OrganizationManagementException {

        String applicationId = mainApplication.getApplicationResourceId();
        revokeSharedAppAccessTokens(mainOrgId, applicationId, sharedOrganizationId);
        deleteSharedApplication(sharedOrganizationId, sharedApplicationId);

        if (!hasSharedApps(applicationId)) {
            // Mark as non-shared only if there are no shared organizations.
            setIsAppSharedProperty(mainApplication, false);
            try {
                getApplicationManagementService().updateApplication(mainApplication, getTenantDomain(),
                        getAuthenticatedUsername());
            } catch (IdentityApplicationManagementException e) {
                throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, applicationId);
            }
        }
        try {
            getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(
                    sharedOrganizationId, ResourceType.APPLICATION, applicationId, mainOrgId);
        } catch (ResourceSharingPolicyMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, applicationId);
        }
    }

    private void revokeSharedAppAccessTokens(String rootOrganizationId, String rootApplicationId,
                                             String sharedOrganizationId) throws OrganizationManagementException {

        String rootTenantDomain = getOrganizationManager().resolveTenantDomain(rootOrganizationId);
        ServiceProvider rootApplication = getOrgApplication(rootApplicationId, rootTenantDomain);
        revokeTokensForAppInOrg(rootApplication, sharedOrganizationId);
    }

    private void revokeTokensForAppInOrg(ServiceProvider serviceProvider, String sharedOrganizationId)
            throws OrganizationManagementException {

        String consumerKey = null;
        if (serviceProvider.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs() != null) {
            for (InboundAuthenticationRequestConfig oauth2config :
                    serviceProvider.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs()) {
                if (oauth2config.getInboundAuthType().equals(OAUTH2)) {
                    consumerKey = oauth2config.getInboundAuthKey();
                    break;
                }
            }
        }
        if (StringUtils.isNotBlank(consumerKey)) {
            OAuthAppRevocationRequestDTO applicationDTO = new OAuthAppRevocationRequestDTO();
            applicationDTO.setConsumerKey(consumerKey);
            try {
                OrgApplicationMgtDataHolder.getInstance().getOAuthAdminService()
                        .revokeIssuedTokensForOrganizationByApplication(applicationDTO, sharedOrganizationId);
            } catch (IdentityOAuthAdminException e) {
                throw handleServerException(ERROR_CODE_ERROR_REVOKING_SHARED_APP_TOKENS, e,
                        serviceProvider.getApplicationResourceId(), sharedOrganizationId);
            }
        }
    }

    private void deleteSharedApplication(String sharedOrganizationId, String sharedApplicationId)
            throws OrganizationManagementException {

        try {
            String sharedTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrganizationId);
            String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String userID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();

            // Setting the thread local property to allow deleting fragment application. Otherwise
            // FragmentApplicationMgtListener will reject application deletion.
            IdentityUtil.threadLocalProperties.get().put(DELETE_FRAGMENT_APPLICATION, true);

            /* The shared application resides in a different tenant. Hence, before performing that operation, a tenant
            flow corresponds to that tenant is started. */
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedTenantDomain, true);

                // Set the request initiated user information, which will be used for the auditing purposes.
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserId(userID);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(username);

                ServiceProvider sharedApplication = getApplicationManagementService().getApplicationByResourceId(
                        sharedApplicationId, sharedTenantDomain);
                getApplicationManagementService().deleteApplication(sharedApplication.getApplicationName(),
                        sharedTenantDomain, username);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_REMOVING_FRAGMENT_APP, e, sharedApplicationId,
                    sharedOrganizationId);
        } finally {
            IdentityUtil.threadLocalProperties.get().remove(DELETE_FRAGMENT_APPLICATION);
            IdentityUtil.threadLocalProperties.get().remove(DELETE_SHARE_FOR_MAIN_APPLICATION);
        }
    }

    @Override
    public List<BasicOrganization> getApplicationSharedOrganizations(String organizationId, String applicationId)
            throws OrganizationManagementException {

        List<BasicOrganization> sharedAppsOrgList = new ArrayList<>();
        SharedApplicationOrganizationNodePage applicationSharedOrganizations;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(organizationId);
            applicationSharedOrganizations = getApplicationSharedOrganizations(
                    organizationId, applicationId, null, 0, 0, SP_SHARED_ROLE_EXCLUDED_KEY, null, 0, true);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        for (SharedApplicationOrganizationNode sharedApplicationOrganizationNode :
                applicationSharedOrganizations.getSharedApplicationOrganizationNodes()) {
            String sharedOrganizationId = sharedApplicationOrganizationNode.getOrganizationId();
            String organizationStatus = sharedApplicationOrganizationNode.getOrganizationStatus();
            BasicOrganization sharedOrganization = new BasicOrganization();
            sharedOrganization.setId(sharedOrganizationId);
            sharedOrganization.setName(sharedApplicationOrganizationNode.getOrganizationName());
            sharedOrganization.setStatus(organizationStatus);
            sharedAppsOrgList.add(sharedOrganization);
        }
        return sharedAppsOrgList;
    }

    @Override
    public SharedApplicationOrganizationNodePage getApplicationSharedOrganizations(String mainOrganizationId,
                                                                                   String mainApplicationId,
                                                                                   String filter, int beforeCursor,
                                                                                   int afterCursor,
                                                                                   String excludedAttributes,
                                                                                   String attributes,
                                                                                   int limit, boolean recursive)
            throws OrganizationManagementException {

        getListener().preGetApplicationSharedOrganizations(mainOrganizationId, mainApplicationId);
        List<SharedApplicationOrganizationNode> applicationSharedOrganizationsList = new ArrayList<>();

        String sortOrder = beforeCursor != 0 ? ASC_SORT_ORDER : DESC_SORT_ORDER;
        List<ExpressionNode> expressionNodeList = getExpressionNodes(filter, afterCursor, beforeCursor);

        String parentOrgId = null;
        Optional<String> optionalParentOrgId = removeAndGetOrganizationIdFromTheExpressionNodeList(expressionNodeList);
        if (optionalParentOrgId.isPresent()) {
            parentOrgId = optionalParentOrgId.get();
        } else {
            Optional<String> optionalOrgName = removeAndGetOrganizationNameFromTheExpressionNodeList(
                    expressionNodeList);
            if (optionalOrgName.isPresent()) {
                String orgName = optionalOrgName.get();
                parentOrgId = getOrganizationManager().getOrganizationIdByName(orgName);
            }
        }

        if (StringUtils.isBlank(parentOrgId)) {
            LOG.debug("Parent organization ID is not provided. Fetching all shared organizations.");
            parentOrgId = mainOrganizationId;
        }

        List<String> organizationIds;
        if (StringUtils.isNotBlank(parentOrgId)) {
            organizationIds = getOrganizationManager().getChildOrganizationsIds(parentOrgId, recursive);
        } else {
            organizationIds = new ArrayList<>();
        }

        List<String> includedAttributesList = getIncludedAttributes(attributes);
        String mainOrgHandle = getOrganizationManager().resolveTenantDomain(mainOrganizationId);
        SharingModeDO sharingModeDO = null;
        if (includedAttributesList.contains(SP_SHARED_SHARING_MODE_INCLUDED_KEY)) {
            sharingModeDO = resolveGeneralSharingMode(mainOrganizationId, mainApplicationId, mainOrgHandle);
        }

        // Fetch one more item than requested to determine if there are more items.
        // Limit == 0 means no limit has been set. So we should get all items.
        int fetchLimit = limit == 0 ? limit : limit + 1;
        List<SharedApplicationDO> sharedApplications = getOrgApplicationMgtDAO().getSharedApplications(
                mainOrganizationId, mainApplicationId, organizationIds, expressionNodeList, sortOrder, fetchLimit);

        if (CollectionUtils.isEmpty(sharedApplications)) {
            return new SharedApplicationOrganizationNodePage(Collections.emptyList(), sharingModeDO, 0, 0);
        }

        boolean hasMoreItems = sharedApplications.size() > limit;
        if (limit == 0) {
            // If no limit is set, we assume no pagination.
            hasMoreItems = false;
        }

        // If we have more items, remove the extra one from the list.
        if (hasMoreItems) {
            sharedApplications.remove(sharedApplications.size() - 1);
        }

        // If 'previous' was provided, we need to reverse the order for correct display.
        boolean needsReverse = beforeCursor != 0;
        if (needsReverse) {
            Collections.reverse(sharedApplications);
        }
        List<String> excludedAttributesList = getExcludedAttributes(excludedAttributes);
        for (SharedApplicationDO sharedApplicationDO : sharedApplications) {
            applicationSharedOrganizationsList.add(getApplicationSharedOrganizationNode(sharedApplicationDO,
                    mainOrganizationId, mainApplicationId, excludedAttributesList, includedAttributesList));
        }

        // Calculate next and previous tokens.
        int nextToken = 0;
        int previousToken = 0;

        // First page check: either no pagination params were provided or we used 'before' and there are no more results
        boolean isFirstPage = (beforeCursor == 0 && afterCursor == 0) ||
                (beforeCursor != 0 && !hasMoreItems);

        // Last page check: no more items and we either used 'after' or didn't use 'before'.
        boolean isLastPage = !hasMoreItems && (afterCursor != 0 || beforeCursor == 0);

        if (!isFirstPage && !applicationSharedOrganizationsList.isEmpty()) {
            // Generate previous token from first item.
            SharedApplicationDO firstItem = sharedApplications.get(0);
            previousToken = firstItem.getAppId();
        }

        if (!isLastPage && !applicationSharedOrganizationsList.isEmpty()) {
            // Generate next token from last item.
            SharedApplicationDO lastItem = sharedApplications.get(sharedApplications.size() - 1);
            nextToken = lastItem.getAppId();
        }
        return new SharedApplicationOrganizationNodePage(
                applicationSharedOrganizationsList, sharingModeDO, nextToken, previousToken);
    }

    private List<String> getExcludedAttributes(String excludedAttributes) {

        if (StringUtils.isBlank(excludedAttributes)) {
            return Collections.emptyList();
        }
        return stream(excludedAttributes.split(","))
                .map(String::trim)
                .peek(attribute -> {
                    if (StringUtils.isBlank(attribute)) {
                        LOG.debug("Empty or blank attribute found in excluded attributes list.");
                    } else if (!SP_SHARED_SUPPORTED_EXCLUDED_ATTRIBUTES.contains(attribute)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Unsupported attribute found in excluded attributes: " + attribute);
                        }
                    }
                }).collect(Collectors.toList());
    }

    private List<String> getIncludedAttributes(String attributes) {

        if (StringUtils.isBlank(attributes)) {
            return Collections.emptyList();
        }
        return stream(attributes.split(","))
                .map(String::trim)
                .peek(attribute -> {
                    if (StringUtils.isBlank(attribute)) {
                        LOG.debug("Empty or blank attribute found in included attributes list.");
                    } else if (!SP_SHARED_SUPPORTED_INCLUDED_ATTRIBUTES.contains(attribute)) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Unsupported attribute found in included attributes: " + attribute);
                        }
                    }
                }).collect(Collectors.toList());
    }

    private SharedApplicationOrganizationNode getApplicationSharedOrganizationNode(
            SharedApplicationDO sharedApplicationDO, String mainOrgId, String mainApplicationId,
            List<String> excludedAttributesList, List<String> includedAttributesList)
            throws OrganizationManagementException {

        // 1. Get the sub organization ID and the handle.
        String mainOrgHandle = getOrganizationManager().resolveTenantDomain(mainOrgId);
        String subOrgId = sharedApplicationDO.getOrganizationId();
        String subOrgHandle = getOrganizationManager().resolveTenantDomain(subOrgId);
        String sharedAppResourceId = sharedApplicationDO.getFragmentApplicationId();
        Organization organization = getOrganizationManager().getOrganization(subOrgId, true, false);
        String subOrgName = organization.getName();

        // 2. Get all children of the sub organization.
        List<ChildOrganizationDO> childOrganizations = organization.getChildOrganizations();
        List<String> childOrgIds = childOrganizations.stream()
                .map(ChildOrganizationDO::getId).collect(Collectors.toList());

        // If it has children, check if any child has a shared application.
        List<SharedApplicationDO> childSharedApplications = getOrgApplicationMgtDAO().getSharedApplications(
                mainApplicationId, mainOrgId, childOrgIds);
        boolean hasChildren = CollectionUtils.isNotEmpty(childSharedApplications);
        String organizationStatus = organization.getStatus();
        String parentOrganizationId = organization.getParent().getId();

        // 4. Get the depth from root.
        int depthFromRoot = getOrganizationManager().getOrganizationDepthInHierarchy(subOrgId);

        // 5. Get the role sharing config.
        SharingModeDO sharingModeDO = null;
        if (includedAttributesList.contains(SP_SHARED_SHARING_MODE_INCLUDED_KEY)) {
            sharingModeDO = resolveOrganizationSharingMode(mainOrgId, subOrgId, mainApplicationId,
                    sharedAppResourceId, subOrgHandle);
        }

        if (!excludedAttributesList.contains(SP_SHARED_ROLE_EXCLUDED_KEY)) {
            List<RoleWithAudienceDO> sharedAppRoles = getSharedAppRoles(mainOrgHandle, mainApplicationId, subOrgId,
                    sharedApplicationDO.getFragmentApplicationId());
            return new SharedApplicationOrganizationNode(sharedAppResourceId, subOrgId, subOrgName, organizationStatus,
                    parentOrganizationId, subOrgHandle, sharedAppRoles, hasChildren, depthFromRoot,
                    sharingModeDO);
        }
        // If roles are excluded, we do not need to fetch roles and returning null so it will differentiate
        // not having any roles and not need to fetch roles.
        return new SharedApplicationOrganizationNode(sharedAppResourceId, subOrgId, subOrgName, organizationStatus,
                parentOrganizationId, subOrgHandle, null, hasChildren, depthFromRoot,
                sharingModeDO);
    }

    private SharingModeDO resolveGeneralSharingMode(String initiatingOrgId, String mainAppId, String mainOrgHandle)
            throws OrganizationManagementException {

        try {
            ServiceProvider application = getApplicationManagementService()
                    .getApplicationByResourceId(mainAppId, mainOrgHandle);
            ApplicationShareRolePolicy.Mode mode = OrgApplicationManagerUtil
                    .getAppAssociatedRoleSharingMode(application);

            if (OrgApplicationManagerUtil.isShareWithAllChildren(application.getSpProperties())) {
                ApplicationShareRolePolicy.Builder applicationShareRolePolicy
                        = new ApplicationShareRolePolicy.Builder().mode(mode);
                // If an app has `sharedWithAllChildren` property, but the role mode has been set to `SELECTED` or
                // `NONE`, that means app was shared with new app sharing mode. So it has been shared with a
                // general share policy. That's why below check is checking for the mode.
                if (ApplicationShareRolePolicy.Mode.ALL.ordinal() == mode.ordinal()) {
                    return new SharingModeDO.Builder()
                            .policy(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS)
                            .applicationShareRolePolicy(applicationShareRolePolicy.build()).build();
                }
            }
            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> result
                    = getResourceSharingPolicyHandlerService().getResourceSharingPolicyAndAttributesByInitiatingOrgId(
                    initiatingOrgId, B2B_APPLICATION, mainAppId);

            if (result != null && !result.isEmpty()) {
                Map.Entry<ResourceSharingPolicy, List<SharedResourceAttribute>> entry
                        = result.entrySet().iterator().next();
                ResourceSharingPolicy resourceSharingPolicy = entry.getKey();
                List<SharedResourceAttribute> resourceAttributes = entry.getValue();

                if (resourceSharingPolicy.getSharingPolicy() == PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS) {
                    ApplicationShareRolePolicy.Builder applicationShareRolePolicy
                            = new ApplicationShareRolePolicy.Builder().mode(mode);
                    if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == mode.ordinal()) {
                        applicationShareRolePolicy = applicationShareRolePolicy.roleWithAudienceDOList(
                                extractRolesWithAudience(resourceAttributes, mainAppId, mainOrgHandle));
                    }
                    return new SharingModeDO.Builder()
                            .policy(resourceSharingPolicy.getSharingPolicy())
                            .applicationShareRolePolicy(applicationShareRolePolicy.build())
                            .build();
                }
            }
        } catch (ResourceSharingPolicyMgtException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        } catch (IdentityApplicationManagementException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        }
        return null;
    }

    private SharingModeDO resolveOrganizationSharingMode(String initiatingOrgId, String subOrgId, String mainAppId,
                                                         String sharedAppId, String subOrgHandle)
            throws OrganizationManagementException {

        try {
            ServiceProvider application = getApplicationManagementService()
                    .getApplicationByResourceId(sharedAppId, subOrgHandle);
            ApplicationShareRolePolicy.Mode mode = OrgApplicationManagerUtil
                    .getAppAssociatedRoleSharingMode(application);
            ApplicationShareRolePolicy.Builder roleSharingConfigBuilder = new ApplicationShareRolePolicy.Builder()
                    .mode(mode);
            SharingModeDO.Builder sharingModeDO = new SharingModeDO.Builder();

            Map<ResourceSharingPolicy, List<SharedResourceAttribute>> result
                    = getResourceSharingPolicyHandlerService().getResourceSharingPolicyAndAttributesByInitiatingOrgId(
                            initiatingOrgId, B2B_APPLICATION, mainAppId);

            if (result != null && !result.isEmpty()) {
                Map.Entry<ResourceSharingPolicy, List<SharedResourceAttribute>> entry
                        = result.entrySet().iterator().next();
                ResourceSharingPolicy resourceSharingPolicy = entry.getKey();
                List<SharedResourceAttribute> resourceAttributes = entry.getValue();
                boolean isPolicyHolderOrg = Objects.equals(resourceSharingPolicy.getPolicyHoldingOrgId(), subOrgId);

                if (resourceSharingPolicy.getSharingPolicy()
                        == PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN && isPolicyHolderOrg) {

                    sharingModeDO.policy(resourceSharingPolicy.getSharingPolicy());
                    if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == mode.ordinal()) {
                        roleSharingConfigBuilder = roleSharingConfigBuilder.roleWithAudienceDOList(
                                extractRolesWithAudience(resourceAttributes, sharedAppId, subOrgHandle));
                    }
                }
            }
            sharingModeDO.applicationShareRolePolicy(roleSharingConfigBuilder.build());
            return sharingModeDO.build();
        } catch (ResourceSharingPolicyMgtException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        } catch (IdentityApplicationManagementException e) {
            throw new OrganizationManagementException(e.getMessage(), e.getDescription(), e.getErrorCode());
        }
    }

    private List<RoleWithAudienceDO> extractRolesWithAudience(List<SharedResourceAttribute> resourceAttributes,
                                                              String appId, String orgHandle)
            throws IdentityApplicationManagementException {

        List<RoleWithAudienceDO> roleWithAudienceDO = new ArrayList<>();

        if (resourceAttributes != null && !resourceAttributes.isEmpty() && resourceAttributes.get(0) != null) {
            String roleAudience = getApplicationManagementService()
                    .getAllowedAudienceForRoleAssociation(appId, orgHandle);

            RoleWithAudienceDO.AudienceType audienceType =
                    StringUtils.equalsIgnoreCase(RoleConstants.APPLICATION, roleAudience)
                            ? RoleWithAudienceDO.AudienceType.APPLICATION
                            : RoleWithAudienceDO.AudienceType.ORGANIZATION;

            for (SharedResourceAttribute attribute : resourceAttributes) {
                if (attribute.getSharedAttributeType() == SharedAttributeType.ROLE) {
                    try {
                        Role role = getRoleManagementServiceV2().getRole(attribute.getSharedAttributeId());
                        if (role != null) {
                            roleWithAudienceDO.add(new RoleWithAudienceDO(
                                    role.getName(), role.getAudienceName(), audienceType));
                        }
                    } catch (IdentityRoleManagementException e) {
                        LOG.error("Failed to retrieve role for shared attribute ID: "
                                + attribute.getSharedAttributeId(), e);
                    }
                }
            }
        }
        return roleWithAudienceDO;
    }

    private List<RoleWithAudienceDO> getSharedAppRoles(String mainOrgName, String mainApplicationId, String subOrgId,
                                                       String sharedAppId) throws OrganizationManagementException {

        List<RoleWithAudienceDO> roleWithAudienceDOList = new ArrayList<>();
        try {
            String applicationRoleAudience = getApplicationManagementService()
                    .getAllowedAudienceForRoleAssociation(mainApplicationId, mainOrgName);
            String roleFilter;
            RoleWithAudienceDO.AudienceType audienceType;
            if (StringUtils.equalsIgnoreCase(RoleConstants.APPLICATION, applicationRoleAudience)) {
                audienceType = RoleWithAudienceDO.AudienceType.APPLICATION;
                roleFilter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + sharedAppId;
            } else {
                audienceType = RoleWithAudienceDO.AudienceType.ORGANIZATION;
                roleFilter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + subOrgId;
            }
            String subOrgTenantDomain = getOrganizationManager().resolveTenantDomain(subOrgId);
            List<RoleBasicInfo> roles = getRoleManagementServiceV2().getRoles(roleFilter, null, 0, null,
                    null, subOrgTenantDomain);

            for (RoleBasicInfo role : roles) {
                String roleName = role.getName();
                String roleAudienceName = role.getAudienceName();
                RoleWithAudienceDO roleWithAudienceDO = new RoleWithAudienceDO(roleName, roleAudienceName,
                        audienceType);
                roleWithAudienceDOList.add(roleWithAudienceDO);
            }
        } catch (IdentityApplicationManagementException e) {
            throw OrgApplicationManagerUtil.handleServerException(ERROR_CODE_ERROR_RETRIEVING_SHARED_APP, e);
        } catch (IdentityRoleManagementException e) {
            throw OrgApplicationManagerUtil.handleServerException(ERROR_CODE_ERROR_RETRIEVING_SHARED_APP_ROLES, e);
        }
        return roleWithAudienceDOList;
    }

    private static RoleManagementService getRoleManagementServiceV2() {

        return OrgApplicationMgtDataHolder.getInstance().getRoleManagementServiceV2();
    }

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
     * Sets the expression nodes required for the retrieval of shared apps from the database.
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

    private Optional<String> removeAndGetOrganizationIdFromTheExpressionNodeList(
            List<ExpressionNode> expressionNodeList) {

        String organizationId = null;
        for (ExpressionNode expressionNode : expressionNodeList) {
            if (expressionNode.getAttributeValue().equalsIgnoreCase(PARENT_ORGANIZATION_ID)) {
                organizationId = expressionNode.getValue();
                break;
            }
        }
        if (organizationId != null) {
            expressionNodeList.removeIf(expressionNode -> expressionNode.getAttributeValue()
                    .equalsIgnoreCase(PARENT_ORGANIZATION_ID));
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
            expressionNodeList.removeIf(expressionNode -> expressionNode.getAttributeValue()
                    .equalsIgnoreCase(ORGANIZATION_NAME_FIELD));
        }
        return Optional.ofNullable(organizationName);
    }

    private boolean isFilteringAttributeNotSupported(String attributeValue) {

        return !attributeValue.equalsIgnoreCase(ORGANIZATION_ID_FIELD) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_AFTER) &&
                !attributeValue.equalsIgnoreCase(PAGINATION_BEFORE) &&
                !attributeValue.equalsIgnoreCase(PARENT_ORGANIZATION_ID);
    }

    @Override
    public List<SharedApplication> getSharedApplications(String organizationId, String applicationId)
            throws OrganizationManagementException {

        getListener().preGetSharedApplications(organizationId, applicationId);
        String tenantDomain = getTenantDomain();
        if (organizationId != null) {
            tenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
        }
        ServiceProvider application = getOrgApplication(applicationId, tenantDomain);
        List<SharedApplicationDO> sharedApplicationDOList =
                getOrgApplicationMgtDAO().getSharedApplications(organizationId, application.getApplicationResourceId());
        List<SharedApplication> sharedApplications = sharedApplicationDOList.stream()
                .map(sharedAppDO -> new SharedApplication(sharedAppDO.getFragmentApplicationId(),
                        sharedAppDO.getOrganizationId())).collect(Collectors.toList());
        getListener().postGetSharedApplications(organizationId, applicationId, sharedApplications);
        return sharedApplications;
    }

    @Override
    public ServiceProvider resolveSharedApplication(String mainAppName, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        String ownerTenantDomain = getOrganizationManager().resolveTenantDomain(ownerOrgId);
        if (StringUtils.isBlank(ownerTenantDomain)) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN, null,
                    ownerOrgId);
        }

        ServiceProvider mainApplication;
        try {
            mainApplication = Optional.ofNullable(
                            getApplicationManagementService().getServiceProvider(mainAppName, ownerTenantDomain))
                    .orElseThrow(() -> handleClientException(ERROR_CODE_INVALID_APPLICATION, mainAppName));
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppName, ownerOrgId);
        }
        return resolveSharedApplicationByMainAppUUID(mainApplication.getApplicationResourceId(), ownerOrgId,
                sharedOrgId);
    }

    @Override
    public ServiceProvider resolveSharedApplicationByMainAppUUID(String mainAppUUID, String ownerOrgId,
                                                                 String sharedOrgId)
            throws OrganizationManagementException {

        String sharedAppId =
                resolveSharedApp(mainAppUUID, ownerOrgId, sharedOrgId).orElseThrow(
                        () -> handleClientException(ERROR_CODE_APPLICATION_NOT_SHARED, mainAppUUID, ownerOrgId));
        String sharedOrgTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrgId);
        try {
            return getApplicationManagementService().getApplicationByResourceId(sharedAppId, sharedOrgTenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppUUID, ownerOrgId);
        }
    }

    @Override
    public boolean isApplicationSharedWithGivenOrganization(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        return resolveSharedApp(mainAppId, ownerOrgId, sharedOrgId).isPresent();
    }

    @Override
    public String getMainApplicationIdForGivenSharedApp(String sharedAppId, String sharedOrgId)
            throws OrganizationManagementException {

        return getOrgApplicationMgtDAO()
                .getMainApplication(sharedAppId, sharedOrgId)
                .map(MainApplicationDO::getMainApplicationId)
                .orElse(null);
    }

    /**
     * Retrieve the application ({@link ServiceProvider}) for the given identifier and the tenant domain.
     *
     * @param applicationId application identifier.
     * @param tenantDomain  tenant domain.
     * @return instance of {@link ServiceProvider}.
     * @throws OrganizationManagementException on errors when retrieving the application
     */
    private ServiceProvider getOrgApplication(String applicationId, String tenantDomain)
            throws OrganizationManagementException {

        ServiceProvider application;
        try {
            application = getApplicationManagementService().getApplicationByResourceId(applicationId,
                    tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION, e, applicationId);
        }
        return Optional.ofNullable(application)
                .orElseThrow(() -> handleClientException(ERROR_CODE_INVALID_APPLICATION, applicationId));
    }

    /**
     * This method will update the root application by adding the organization login authenticator and updating the
     * claim configurations to enable use of local subject identifier for JIT provisioned users. Also update the root
     * application with federated_org oidc claim as requested claim of the application.
     */
    private void modifyRootApplication(ServiceProvider rootApplication, String tenantDomain)
            throws OrganizationManagementServerException, OrganizationManagementClientException {

        LocalAndOutboundAuthenticationConfig outboundAuthenticationConfig =
                rootApplication.getLocalAndOutBoundAuthenticationConfig();
        AuthenticationStep[] authSteps = outboundAuthenticationConfig.getAuthenticationSteps();

        if (StringUtils.equalsIgnoreCase(outboundAuthenticationConfig.getAuthenticationType(), AUTH_TYPE_DEFAULT)) {
            // We need to set the default tenant authentication sequence.
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authentication type is set to 'DEFAULT'. Reading the authentication sequence from the " +
                        "'default' application and showing the effective authentication sequence for application " +
                        "with id: " + rootApplication.getApplicationResourceId());
            }
            LocalAndOutboundAuthenticationConfig defaultAuthenticationConfig = getDefaultAuthenticationConfig();
            if (defaultAuthenticationConfig != null) {
                authSteps = defaultAuthenticationConfig.getAuthenticationSteps();
            }
            // Change the authType to flow, since we are adding organization login authenticator.
            outboundAuthenticationConfig.setAuthenticationType(AUTH_TYPE_FLOW);
        }

        AuthenticationStep first = new AuthenticationStep();
        if (ArrayUtils.isNotEmpty(authSteps)) {
            AuthenticationStep exist = authSteps[0];
            boolean idpAlreadyConfigured = false;
            if (ArrayUtils.isNotEmpty(exist.getFederatedIdentityProviders())) {
                idpAlreadyConfigured = stream(exist.getFederatedIdentityProviders()).map(
                                IdentityProvider::getDefaultAuthenticatorConfig)
                        .anyMatch(auth -> ORGANIZATION_LOGIN_AUTHENTICATOR.equals(auth.getName()));
            }
            if (idpAlreadyConfigured) {
                return;
            }
            first.setStepOrder(exist.getStepOrder());
            first.setSubjectStep(exist.isSubjectStep());
            first.setAttributeStep(exist.isAttributeStep());
            first.setFederatedIdentityProviders(exist.getFederatedIdentityProviders());
            first.setLocalAuthenticatorConfigs(exist.getLocalAuthenticatorConfigs());
        }

        AuthenticationStep[] newAuthSteps =
                ArrayUtils.isNotEmpty(authSteps) ? authSteps.clone() : new AuthenticationStep[1];

        IdentityProvider[] idps;
        try {
            idps = getApplicationManagementService().getAllIdentityProviders(tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_IDP_LIST, e, getOrganizationId());
        }
        Optional<IdentityProvider> maybeOrganizationIDP = stream(idps).filter(this::isOrganizationLoginIDP).findFirst();
        IdentityProvider identityProvider;
        try {
            identityProvider = maybeOrganizationIDP.isPresent() ? maybeOrganizationIDP.get() :
                    getIdentityProviderManager().addIdPWithResourceId(createOrganizationSSOIDP(), tenantDomain);
        } catch (IdentityProviderManagementClientException e) {
            throw new OrganizationManagementClientException(e.getMessage(), e.getMessage(), e.getErrorCode());
        } catch (IdentityProviderManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATING_ORG_LOGIN_IDP, e, getOrganizationId());
        }

        first.setFederatedIdentityProviders(
                (IdentityProvider[]) ArrayUtils.addAll(
                        first.getFederatedIdentityProviders() != null ? 
                                first.getFederatedIdentityProviders() : new IdentityProvider[0],
                        new IdentityProvider[]{identityProvider}));
        newAuthSteps[0] = first;
        outboundAuthenticationConfig.setAuthenticationSteps(newAuthSteps);
        rootApplication.setLocalAndOutBoundAuthenticationConfig(outboundAuthenticationConfig);

        // Enabling use of local subject id for provisioned users.
        rootApplication.getClaimConfig().setAlwaysSendMappedLocalSubjectId(true);

        try {
            getApplicationManagementService().updateApplication(rootApplication, tenantDomain,
                    getAuthenticatedUsername());
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION, e,
                    rootApplication.getApplicationResourceId());
        }
    }

    private boolean isOrganizationLoginIDP(IdentityProvider idp) {

        FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = idp.getFederatedAuthenticatorConfigs();
        return ArrayUtils.isNotEmpty(federatedAuthenticatorConfigs) &&
                ORGANIZATION_LOGIN_AUTHENTICATOR.equals(federatedAuthenticatorConfigs[0].getName());
    }

    @Override
    public void shareApplicationWithPolicy(String ownerOrgId, ServiceProvider mainApplication, String sharingOrgId,
                                           PolicyEnum policyEnum, ApplicationShareRolePolicy applicationShareRolePolicy,
                                           String operationId) throws OrganizationManagementException {

        String mainApplicationId = mainApplication.getApplicationResourceId();
        try {
            getListener().preShareApplication(ownerOrgId, mainApplicationId, sharingOrgId, applicationShareRolePolicy);
            // Use tenant of the organization to whom the application getting shared. When the consumer application is
            // loaded, tenant domain will be derived from the user who created the application.
            String sharedTenantDomain = getOrganizationManager().resolveTenantDomain(sharingOrgId);
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedTenantDomain, true);
            int tenantId = IdentityTenantUtil.getTenantId(sharedTenantDomain);

            try {
                String adminUserId =
                        getRealmService().getTenantUserRealm(tenantId).getRealmConfiguration().getAdminUserId();
                if (StringUtils.isBlank(adminUserId)) {
                    // If realms were not migrated after https://github.com/wso2/product-is/issues/14001.
                    adminUserId = getRealmService().getTenantUserRealm(tenantId)
                            .getRealmConfiguration().getAdminUserName();
                }
                String domainQualifiedUserName = OrgApplicationMgtDataHolder.getInstance()
                        .getOrganizationUserResidentResolverService()
                        .resolveUserFromResidentOrganization(null, adminUserId, sharingOrgId)
                        .map(User::getDomainQualifiedUsername)
                        .orElse(MultitenantUtils.getTenantAwareUsername(mainApplication.getOwner()
                                .toFullQualifiedUsername()));
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(domainQualifiedUserName);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserId(adminUserId);
            } catch (UserStoreException e) {
                processUnitOperationStatus(operationId, mainApplicationId, sharingOrgId,
                        OperationStatus.FAILED, "UserStoreException: " + e.getMessage());
                throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e, mainApplicationId, sharingOrgId);
            }

            Optional<String> sharedAppId = resolveSharedApp(mainApplicationId, ownerOrgId, sharingOrgId);
            String sharedApplicationId;
            // Create the OAuth app and the service provider only if the app hasn't been created.
            if (!sharedAppId.isPresent()) {
                // Check if the application is shared to the parentOrg.
                String parentOrgId = getOrganizationManager().getOrganization(sharingOrgId, false, false).getParent()
                        .getId();
                if (!parentOrgId.equals(ownerOrgId)) {
                    Optional<String> parentOrgSharedAppId = resolveSharedApp(mainApplicationId,
                            ownerOrgId, parentOrgId);
                    if (!parentOrgSharedAppId.isPresent()) {
                        processUnitOperationStatus(operationId, mainApplicationId, sharingOrgId, OperationStatus.FAILED,
                                "Application: " + mainApplicationId +
                                        " is not shared with the parent organization: " + parentOrgId);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Application: " + mainApplicationId +
                                    " is not shared with the parent organization: " + parentOrgId);
                        }
                        return;
                    }
                }

                // Create Oauth consumer app to redirect login to shared (fragment) application.
                OAuthConsumerAppDTO createdOAuthApp;
                try {
                    String callbackUrl = resolveCallbackURL(ownerOrgId);
                    String backChannelLogoutUrl = resolveBackChannelLogoutURL(ownerOrgId);
                    createdOAuthApp = createOAuthApplication(
                            mainApplication.getApplicationName(), callbackUrl, backChannelLogoutUrl);
                } catch (URLBuilderException | IdentityOAuthAdminException e) {
                    if (isOAuthClientExistsError(e)) {
                        createdOAuthApp = handleOAuthClientExistsError(ownerOrgId, sharingOrgId, mainApplication);
                    } else {
                        processUnitOperationStatus(operationId, mainApplicationId,
                                sharingOrgId, OperationStatus.FAILED, e.getMessage());
                        throw handleServerException(ERROR_CODE_ERROR_CREATING_OAUTH_APP, e, mainApplicationId,
                                sharingOrgId);
                    }
                }
                try {
                    ServiceProvider delegatedApplication =
                            prepareSharedApplication(mainApplication, createdOAuthApp, sharingOrgId);
                    // Add the role sharing config to the shared application before adding to the DB.
                    setAppAssociatedRoleSharingMode(delegatedApplication, applicationShareRolePolicy.getMode());
                    sharedApplicationId = getApplicationManagementService().createApplication(delegatedApplication,
                            sharedTenantDomain, getAuthenticatedUsername());
                    getOrgApplicationMgtDAO().addSharedApplication(mainApplicationId, ownerOrgId,
                            sharedApplicationId, sharingOrgId);
                } catch (IdentityApplicationManagementException e) {
                    removeOAuthApplication(createdOAuthApp);
                    processUnitOperationStatus(operationId, mainApplicationId, sharingOrgId, OperationStatus.FAILED,
                            e.getMessage());
                    throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e, mainApplicationId,
                            sharingOrgId);
                }
            } else {
                try {
                    // The app is already shared, but the config needs to be updated.
                    sharedApplicationId = sharedAppId.get();
                    ServiceProvider sharedServiceProvider = getApplicationManagementService()
                            .getApplicationByResourceId(sharedApplicationId, sharedTenantDomain);
                    // Add the role sharing config to the shared application.
                    setAppAssociatedRoleSharingMode(sharedServiceProvider, applicationShareRolePolicy.getMode());
                    getApplicationManagementService().updateApplication(sharedServiceProvider,
                            sharedTenantDomain, getAuthenticatedUsername());
                } catch (IdentityApplicationManagementException e) {
                    processUnitOperationStatus(operationId, mainApplicationId, sharingOrgId, OperationStatus.FAILED,
                            e.getMessage());
                    throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e, mainApplicationId,
                            sharingOrgId);
                }
            }
            String ownerTenantDomain = getOrganizationManager().resolveTenantDomain(ownerOrgId);
            addOrUpdatePolicy(mainApplication.getApplicationResourceId(), ownerOrgId, sharingOrgId, ownerTenantDomain,
                    policyEnum, applicationShareRolePolicy);
            getListener().postShareApplication(ownerOrgId, mainApplication.getApplicationResourceId(), sharingOrgId,
                    sharedApplicationId, applicationShareRolePolicy);
            processUnitOperationStatus(operationId, mainApplication.getApplicationResourceId(), sharingOrgId,
                    OperationStatus.SUCCESS, StringUtils.EMPTY);
        } catch (OrganizationManagementException e) {
            handleShareApplicationException(operationId, e, mainApplication.getApplicationResourceId(),
                    sharingOrgId);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        /*
            If the sharing main application is Console, Create the shared admin user in shared organization
            and assign the admin role.
        */
        if ("Console".equals(mainApplication.getApplicationName())) {
            fireOrganizationCreatorSharingEvent(sharingOrgId);
        }
    }

    public void addOrUpdatePolicy(String mainApplicationId, String requestInitiatingOrgId, String sharedOrgId,
                                  String ownerTenantDomain, PolicyEnum applicationSharePolicy,
                                  ApplicationShareRolePolicy applicationShareRolePolicy)
            throws OrganizationManagementException {

        try {
            getResourceSharingPolicyHandlerService().deleteResourceSharingPolicyInOrgByResourceTypeAndId(sharedOrgId,
                    ResourceType.APPLICATION, mainApplicationId, requestInitiatingOrgId);

            if (!(PolicyEnum.ALL_EXISTING_AND_FUTURE_ORGS.ordinal() == applicationSharePolicy.ordinal() ||
                    PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN.ordinal()
                            == applicationSharePolicy.ordinal())) {
                return;
            }
            ResourceSharingPolicy resourceSharingPolicy = ResourceSharingPolicy
                    .builder()
                    .withResourceType(ResourceType.APPLICATION)
                    .withInitiatingOrgId(requestInitiatingOrgId)
                    .withResourceId(mainApplicationId)
                    .withSharingPolicy(applicationSharePolicy)
                    .withPolicyHoldingOrgId(sharedOrgId)
                    .build();

            int resourceSharingPolicyId = getResourceSharingPolicyHandlerService().addResourceSharingPolicy(
                    resourceSharingPolicy);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resource sharing policy added for the Application: " + mainApplicationId +
                        " with policy id: " + resourceSharingPolicyId);
            }
            updateSharedResourceAttributes(resourceSharingPolicyId, requestInitiatingOrgId, mainApplicationId,
                    ownerTenantDomain, applicationShareRolePolicy);
        } catch (ResourceSharingPolicyMgtClientException e) {
            throw new OrganizationManagementClientException(e.getMessage(), e.getMessage(), e.getErrorCode());
        } catch (ResourceSharingPolicyMgtException e) {
            throw new OrganizationManagementServerException(e.getMessage(), e.getMessage(), e);
        }
    }

    private void updateSharedResourceAttributes(int resourceSharingPolicyId, String requestInitiatingOrgId,
                                                String mainApplicationId, String ownerTenantDomain,
                                                ApplicationShareRolePolicy applicationShareRolePolicy)
            throws OrganizationManagementServerException {

        try {
            // Remove all previously shared role attributes for this resource policy.
            getResourceSharingPolicyHandlerService().deleteSharedResourceAttributesByResourceSharingPolicyId(
                    resourceSharingPolicyId, SharedAttributeType.ROLE, requestInitiatingOrgId);

            if (ApplicationShareRolePolicy.Mode.NONE.ordinal() == applicationShareRolePolicy.getMode().ordinal()) {
                return;
            }

            String allowedAudienceForRoleAssociation = getApplicationManagementService()
                    .getAllowedAudienceForRoleAssociation(mainApplicationId, ownerTenantDomain);
            boolean isAppLevelAudience = RoleConstants.APPLICATION.equalsIgnoreCase(allowedAudienceForRoleAssociation);

            List<RoleBasicInfo> allMainAppRoles = getApplicationRoles(
                    isAppLevelAudience ? mainApplicationId : requestInitiatingOrgId, ownerTenantDomain);

            List<String> addedRoles;
            if (ApplicationShareRolePolicy.Mode.SELECTED.ordinal() == applicationShareRolePolicy.getMode().ordinal()) {
                Set<String> selectedRoleAudiencePairs = applicationShareRolePolicy.getRoleWithAudienceDOList()
                        .stream()
                        .map(dao -> dao.getRoleName() + "|" + dao.getAudienceName())
                        .collect(Collectors.toSet());
                addedRoles = allMainAppRoles.stream()
                        .filter(role -> selectedRoleAudiencePairs
                                .contains(role.getName() + "|" + role.getAudienceName()))
                        .map(RoleBasicInfo::getId)
                        .collect(Collectors.toList());
            } else {
                // Mode is ALL: add all the main application roles for this resource policy.
                addedRoles = allMainAppRoles.stream().map(RoleBasicInfo::getId).collect(Collectors.toList());
            }
            List<SharedResourceAttribute> sharedResourceAttributes = new ArrayList<>();
            for (String roleId : addedRoles) {
                SharedResourceAttribute sharedResourceAttribute = SharedResourceAttribute.builder()
                        .withSharedAttributeId(roleId)
                        .withResourceSharingPolicyId(resourceSharingPolicyId)
                        .withSharedAttributeType(SharedAttributeType.ROLE)
                        .build();
                sharedResourceAttributes.add(sharedResourceAttribute);
            }
            getResourceSharingPolicyHandlerService().addSharedResourceAttributes(sharedResourceAttributes);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Resource sharing attributes added for the Application: " + mainApplicationId +
                        " with policy id: " + resourceSharingPolicyId);
            }
        } catch (ResourceSharingPolicyMgtException | IdentityRoleManagementException e) {
            throw handleServerException(ERROR_CODE_ADDING_SHARED_RESOURCE_ATTRIBUTES_FAILED, e,
                    String.valueOf(resourceSharingPolicyId));
        } catch (IdentityApplicationManagementException e) {
            throw OrgApplicationManagerUtil.handleServerException(ERROR_CODE_ERROR_RETRIEVING_APP_ROLE_ALLOWED_AUDIENCE,
                    e, mainApplicationId);
        }
    }

    /**
     * Retrieves all application roles for a given main application ID and tenant domain.
     *
     * @param appId         The ID of the main application.
     * @param tenantDomain  The tenant domain of the main application.
     * @return A list of {@link RoleBasicInfo} representing the application roles.
     * @throws IdentityRoleManagementException If an error occurs during role retrieval.
     */
    private List<RoleBasicInfo> getApplicationRoles(String appId, String tenantDomain)
            throws IdentityRoleManagementException {

        String filter = RoleConstants.AUDIENCE_ID + " " + RoleConstants.EQ + " " + appId;
        return getRoleManagementServiceV2().getRoles(filter, null, 0, null, null, tenantDomain);
    }

    /**
     * This is deprecated. Use shareApplicationWithPolicy instead.
     *
     * @param ownerOrgId           Identifier of the organization owning the application.
     * @param sharedOrgId          Identifier of the organization to which the application being shared to.
     * @param mainApplication      The application which is shared with the child organizations.
     * @param shareWithAllChildren Boolean attribute indicating if the application is shared with all sub-organizations.
     * @throws OrganizationManagementException on errors when sharing the application.
     */
    @Deprecated
    @Override
    public void shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
                                 boolean shareWithAllChildren) throws OrganizationManagementException {

        // Synchronous application sharing. Calls the asynchronous method with operationId as null.
        shareApplication(ownerOrgId, sharedOrgId, mainApplication, shareWithAllChildren, null);
    }

    @Override
    public void shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
                                 boolean shareWithAllChildren, String operationId)
            throws OrganizationManagementException {

        ApplicationShareRolePolicy applicationShareRolePolicy = new ApplicationShareRolePolicy.Builder()
                .mode(ApplicationShareRolePolicy.Mode.ALL)
                .build();
        PolicyEnum policyEnum = PolicyEnum.SELECTED_ORG_ONLY;
        if (shareWithAllChildren) {
            policyEnum = PolicyEnum.SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN;
        }
        shareApplicationWithPolicy(ownerOrgId, mainApplication, sharedOrgId, policyEnum, applicationShareRolePolicy,
                operationId);
    }

    private void handleShareApplicationException(String operationId, OrganizationManagementException e,
                                                 String initiatedResourceId, String targetOrgId)
            throws OrganizationManagementException {

        if (StringUtils.isNotBlank(operationId)) {
            Throwable cause1 = e.getCause();
            if (cause1 instanceof IdentityEventException) {
                Throwable cause2 = cause1.getCause();
                if (cause2 instanceof IdentityApplicationManagementClientException) {
                    IdentityApplicationManagementClientException appException =
                            (IdentityApplicationManagementClientException) cause2;
                    String errorCode = appException.getErrorCode();
                    String sharedApplicationName = appException.getDescription();

                    if (Objects.equals(errorCode, ERROR_CODE_ERROR_SHARING_APPLICATION_NAME_CONFLICT.getCode())) {
                        processUnitOperationStatus(operationId, initiatedResourceId,
                                targetOrgId, OperationStatus.FAILED, String.format("Organization has " +
                                        "a non shared application with name %s.", sharedApplicationName));
                    }
                }
            }
        }
        throw e;
    }

    private void processUnitOperationStatus(String operationId, String initiatedResourceId,
                                            String targetOrgId, OperationStatus status, String statusMessage)
            throws OrganizationManagementServerException {

        if (StringUtils.isNotBlank(operationId)) {
            UnitOperationInitDTO dto =
                    new UnitOperationInitDTO(operationId, initiatedResourceId, targetOrgId, status, statusMessage);
            try {
                getAsyncStatusMgtService().registerUnitOperationStatus(dto);
            } catch (AsyncOperationStatusMgtException ex) {
                throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION_SHARED_ACCESS_STATUS, ex);
            }
            asyncOperationStatusList.get(operationId).add(status);
        }
    }

    @Override
    public Map<String, String> getAncestorAppIds(String childAppId, String childOrgId)
            throws OrganizationManagementException {

        Optional<MainApplicationDO> mainApplicationDO =
                getOrgApplicationMgtDAO().getMainApplication(childAppId, childOrgId);
        if (!mainApplicationDO.isPresent()) {
            // Check if the child app is a main application.
            if (isMainApp(childAppId, childOrgId)) {
                return Collections.singletonMap(childOrgId, childAppId);
            }
            return Collections.emptyMap();
        }

        String ownerOrgId = mainApplicationDO.get().getOrganizationId();
        String mainAppId = mainApplicationDO.get().getMainApplicationId();
        List<String> ancestorOrganizationIds = getOrganizationManager().getAncestorOrganizationIds(childOrgId);
        Map<String, String> ancestorAppIds = new HashMap<>();
        // Add main app to the map.
        ancestorAppIds.put(ownerOrgId, mainAppId);
        if (CollectionUtils.isNotEmpty(ancestorOrganizationIds) && ancestorOrganizationIds.size() > 1) {
            List<SharedApplicationDO> ancestorApplications =
                    getOrgApplicationMgtDAO().getSharedApplications(mainAppId, ownerOrgId,
                            ancestorOrganizationIds.subList(0, ancestorOrganizationIds.size() - 1));
            ancestorApplications.forEach(ancestorApplication -> ancestorAppIds.put(
                    ancestorApplication.getOrganizationId(), ancestorApplication.getFragmentApplicationId()));
        }
        return ancestorAppIds;
    }

    @Override
    public Map<String, String> getChildAppIds(String parentAppId, String parentOrgId, List<String> childOrgIds)
            throws OrganizationManagementException {

        if (CollectionUtils.isEmpty(childOrgIds)) {
            return Collections.emptyMap();
        }

        // Check if the parent application is a main application.
        if (isMainApp(parentAppId, parentOrgId)) {
            return getFilteredChildApplications(parentAppId, parentOrgId, childOrgIds);
        }

        Optional<MainApplicationDO> mainApplicationDO =
                getOrgApplicationMgtDAO().getMainApplication(parentAppId, parentOrgId);
        if (mainApplicationDO.isPresent()) {
            return getFilteredChildApplications(mainApplicationDO.get().getMainApplicationId(),
                    mainApplicationDO.get().getOrganizationId(), childOrgIds);
        }
        return Collections.emptyMap();
    }

    @Override
    public List<ApplicationBasicInfo> getDiscoverableSharedApplicationBasicInfo(int limit, int offset, String filter,
                                                                          String sortOrder, String sortBy,
                                                                          String tenantDomain)
            throws OrganizationManagementException {

        String rootOrgId = getOrganizationManager().getPrimaryOrganizationId(tenantDomain);
        return getOrgApplicationMgtDAO().getDiscoverableSharedApplicationBasicInfo(limit, offset, filter, sortOrder,
                sortBy, tenantDomain, rootOrgId);
    }

    @Override
    public int getCountOfDiscoverableSharedApplications(String filter, String tenantDomain)
            throws OrganizationManagementException {

        String rootOrgId = getOrganizationManager().getPrimaryOrganizationId(tenantDomain);
        return getOrgApplicationMgtDAO().getCountOfDiscoverableSharedApplications(filter, tenantDomain, rootOrgId);
    }

    @Override
    public boolean hasSharedApps(String mainApplicationId) throws OrganizationManagementException {

        return getOrgApplicationMgtDAO().hasFragments(mainApplicationId);
    }

    /**
     * Returns whether the given application is a main application.
     *
     * @param appId The unique ID of the application.
     * @param orgId The organization ID of the given application.
     * @return Returns true if the given application is a main application.
     * @throws OrganizationManagementException If an error occurs while retrieving the application.
     */
    private boolean isMainApp(String appId, String orgId) throws OrganizationManagementException {

        OrganizationManager organizationManager = OrgApplicationMgtDataHolder.getInstance().getOrganizationManager();
        String tenantDomain = organizationManager.resolveTenantDomain(orgId);

        ApplicationManagementService applicationManagementService =
                OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
        ServiceProvider serviceProvider;
        try {
            serviceProvider = applicationManagementService.getApplicationByResourceId(appId, tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION, e, appId);
        }

        if (serviceProvider != null) {
            boolean isFragmentApp = stream(serviceProvider.getSpProperties())
                    .anyMatch(property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                            Boolean.parseBoolean(property.getValue()));
            // Given app is a main application.
            return !isFragmentApp;
        }
        return false;
    }

    /**
     * This method checks whether the exception is thrown due to the OAuth client already existing.
     *
     * @param e The IdentityException thrown upon OAuth app creation failure.
     * @return Boolean indicating whether the exception is thrown due to the OAuth client already existing.
     */
    private boolean isOAuthClientExistsError(IdentityException e) {

        return DUPLICATE_OAUTH_CLIENT.getErrorCode().equals(e.getErrorCode());
    }

    /**
     * This method is to handle the exception due to an app already existing during the Oauth app creation process.
     * It is possible that the error is due to stale data, hence a retry mechanism is implemented to check
     * whether it is a stale app and if so, delete the stale app and retry the oauth app creation.
     *
     * @param ownerOrgId      ID of the owner organization.
     * @param sharedOrgId     ID of the shared sub organization.
     * @param mainApplication The application that is being shared.
     * @return OAuth app that is created.
     * @throws OrganizationManagementException Throws exception if there are any exceptions in the retry mechanism.
     */
    private OAuthConsumerAppDTO handleOAuthClientExistsError(
            String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication)
            throws OrganizationManagementException {

        try {
            // Retrieve oauth app of sub organization using app name.
            OAuthConsumerAppDTO existingOAuthApp = getOAuthAdminService()
                    .getOAuthApplicationDataByAppName(mainApplication.getApplicationName());

            // Check if the SP exists for the app name. If it does not exist, then it's a stale oauth app.
            String sharedTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrgId);
            ServiceProvider application = getApplicationManagementService()
                    .getServiceProvider(mainApplication.getApplicationName(), sharedTenantDomain);
            if (application != null) {
                throw new IdentityOAuthAdminException(String.format("OAuth app and SP with name %s already exists " +
                        "in sub organization with id %s.", mainApplication.getApplicationName(), sharedOrgId));
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("OAuth app with name %s already exists in sub organization with id %s due to " +
                                "stale data. Attempting retry after deleting the application.",
                        mainApplication.getApplicationName(), sharedOrgId));
            }

            // Delete the existing app.
            removeOAuthApplication(existingOAuthApp);

            // Retry the app creation.
            String callbackUrl = resolveCallbackURL(ownerOrgId);
            String backChannelLogoutUrl = resolveBackChannelLogoutURL(ownerOrgId);
            return createOAuthApplication(mainApplication.getApplicationName(), callbackUrl, backChannelLogoutUrl);
        } catch (URLBuilderException | IdentityOAuthAdminException | IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_CREATING_OAUTH_APP, e,
                    mainApplication.getApplicationResourceId(), sharedOrgId);
        }
    }

    private void fireOrganizationCreatorSharingEvent(String organizationId) throws OrganizationManagementException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, organizationId);

        IdentityEventService eventService = OrgApplicationMgtDataHolder.getInstance().getIdentityEventService();
        try {
            Event event = new Event("POST_SHARED_CONSOLE_APP", eventProperties);
            eventService.handleEvent(event);
        } catch (IdentityEventClientException e) {
            throw new OrganizationManagementClientException(e.getMessage(), e.getMessage(), e.getErrorCode(), e);
        } catch (IdentityEventException e) {
            throw new OrganizationManagementServerException(
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_FIRING_EVENTS.getMessage(),
                    OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_FIRING_EVENTS.getCode(), e);
        }
    }

    private Optional<String> resolveSharedApp(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        return getOrgApplicationMgtDAO().getSharedApplicationResourceId(mainAppId, ownerOrgId, sharedOrgId);
    }

    private OAuthConsumerAppDTO createOAuthApplication(String mainAppName, String callbackUrl, String backChannelUrl)
            throws IdentityOAuthAdminException {

        OAuthConsumerAppDTO consumerApp = new OAuthConsumerAppDTO();
        String clientId = UUID.randomUUID().toString();
        consumerApp.setOauthConsumerKey(clientId);
        consumerApp.setOAuthVersion(OAuthConstants.OAuthVersions.VERSION_2);
        consumerApp.setGrantTypes(OAuthConstants.GrantTypes.AUTHORIZATION_CODE);
        consumerApp.setCallbackUrl(callbackUrl);
        consumerApp.setBackChannelLogoutUrl(backChannelUrl);
        consumerApp.setApplicationName(mainAppName);
        return getOAuthAdminService().registerAndRetrieveOAuthApplicationData(consumerApp);
    }

    private String resolveCallbackURL(String ownerOrgId) throws URLBuilderException, OrganizationManagementException {

        String tenantDomain = getOrganizationManager().resolveTenantDomain(ownerOrgId);
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            return ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).setTenant(tenantDomain).build()
                    .getAbsolutePublicURL();
        }
        String context =
                String.format(TENANT_CONTEXT_PATH_COMPONENT, tenantDomain) + "/" + FrameworkConstants.COMMONAUTH;
        return ServiceURLBuilder.create().addPath(context).setTenant(tenantDomain).build().getAbsolutePublicURL();
    }

    private String resolveBackChannelLogoutURL(String ownerOrgId)
            throws URLBuilderException, OrganizationManagementException {

        String tenantDomain = getOrganizationManager().resolveTenantDomain(ownerOrgId);
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            return ServiceURLBuilder.create()
                    .addPath(DEFAULT_BACKCHANNEL_LOGOUT_URL)
                    .setTenant(tenantDomain).build().getAbsolutePublicURL();
        }
        String context = String.format(TENANT_CONTEXT_PATH_COMPONENT, tenantDomain)
                + DEFAULT_BACKCHANNEL_LOGOUT_URL;
        return ServiceURLBuilder.create().addPath(context).build().getAbsolutePublicURL();
    }

    private void removeOAuthApplication(OAuthConsumerAppDTO oauthApp)
            throws OrganizationManagementException {

        try {
            getOAuthAdminService().removeOAuthApplicationData(oauthApp.getOauthConsumerKey());
        } catch (IdentityOAuthAdminException e) {
            throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e, oauthApp.getOauthConsumerKey());
        }
    }

    private ServiceProvider prepareSharedApplication(ServiceProvider mainApplication,
                                                     OAuthConsumerAppDTO oAuthConsumerApp, String sharedOrgId)
            throws OrganizationManagementException {

        // Obtain oauth consumer app configs.
        InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig =
                new InboundAuthenticationRequestConfig();
        inboundAuthenticationRequestConfig.setInboundAuthType(AUTH_TYPE_OAUTH_2);
        inboundAuthenticationRequestConfig.setInboundAuthKey(oAuthConsumerApp.getOauthConsumerKey());
        InboundAuthenticationConfig inboundAuthConfig = new InboundAuthenticationConfig();
        inboundAuthConfig.setInboundAuthenticationRequestConfigs(
                new InboundAuthenticationRequestConfig[]{inboundAuthenticationRequestConfig});

        ServiceProvider delegatedApplication = new ServiceProvider();
        delegatedApplication.setApplicationName(oAuthConsumerApp.getApplicationName());
        delegatedApplication.setDescription(mainApplication.getDescription());
        delegatedApplication.setInboundAuthenticationConfig(inboundAuthConfig);
        if (ApplicationMgtUtil.isConsole(mainApplication.getApplicationName())) {
            delegatedApplication.setAccessUrl(resolveAccessURL(mainApplication.getTenantDomain(), sharedOrgId,
                    FrameworkConstants.Application.CONSOLE_APP_PATH));
        } else if (ApplicationMgtUtil.isMyAccount(mainApplication.getApplicationName())) {
            String portalPath = IdentityUtil.getProperty(MYACCOUNT_PORTAL_PATH);
            if (StringUtils.isEmpty(portalPath)) {
                portalPath = FrameworkConstants.Application.MY_ACCOUNT_APP_PATH;
            }
            delegatedApplication.setAccessUrl(resolveAccessURL(mainApplication.getTenantDomain(), sharedOrgId,
                    portalPath));
        }
        appendFragmentAppProperties(delegatedApplication);

        return delegatedApplication;
    }

    private String resolveAccessURL(String ownerTenantDomain, String sharedOrgId, String appPath) {

        if (!appPath.startsWith("/")) {
            appPath = "/" + appPath;
        }
        String accessUrl = IdentityUtil.getServerURL(appPath, true, true);
        accessUrl = accessUrl.replace(appPath, TENANT_CONTEXT_PREFIX + ownerTenantDomain +
                ORGANIZATION_CONTEXT_PREFIX + sharedOrgId + appPath);
        return accessUrl;
    }

    private void appendFragmentAppProperties(ServiceProvider serviceProvider) {

        ServiceProviderProperty fragmentAppProperty = new ServiceProviderProperty();
        fragmentAppProperty.setName(IS_FRAGMENT_APP);
        fragmentAppProperty.setValue(Boolean.TRUE.toString());

        ServiceProviderProperty skipConsentProp = new ServiceProviderProperty();
        skipConsentProp.setName(SKIP_CONSENT);
        skipConsentProp.setValue(Boolean.TRUE.toString());

        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{fragmentAppProperty, skipConsentProp};
        serviceProvider.setSpProperties(spProperties);
    }

    /**
     * Allow sharing application only from the organization the application exists.
     *
     * @param requestInvokingOrganizationId     The organization qualifier id where the request is authorized to access.
     * @param applicationResidingOrganizationId The id of the organization where the application exist.
     * @throws OrganizationManagementException The exception is thrown when the request invoked organization is not the
     *                                         application resides organization.
     */
    private void validateApplicationShareAccess(String requestInvokingOrganizationId,
                                                String applicationResidingOrganizationId)
            throws OrganizationManagementException {

        if (!StringUtils.equals(requestInvokingOrganizationId, applicationResidingOrganizationId)) {
            throw handleClientException(ERROR_CODE_UNAUTHORIZED_APPLICATION_SHARE, applicationResidingOrganizationId,
                    requestInvokingOrganizationId);
        }
    }

    private boolean isSharedApplication(ServiceProvider serviceProvider) {

        return serviceProvider.getSpProperties() != null && stream(serviceProvider.getSpProperties())
                .anyMatch(property -> IS_FRAGMENT_APP.equals(property.getName()) &&
                        Boolean.parseBoolean(property.getValue()));
    }

    /**
     * Allow managing fragment application only from the organization the fragment application exists.
     *
     * @param requestInvokingOrganizationId     The organization qualifier id where the request is authorized to access.
     * @param applicationResidingOrganizationId The id of the organization where the fragment application exist.
     * @throws OrganizationManagementException The exception is thrown when the request invoked organization is not
     *                                         the fragment application resides organization.
     */
    private void validateFragmentApplicationAccess(String requestInvokingOrganizationId,
                                                   String applicationResidingOrganizationId)
            throws OrganizationManagementException {

        if (requestInvokingOrganizationId == null ||
                !StringUtils.equals(requestInvokingOrganizationId, applicationResidingOrganizationId)) {
            throw handleClientException(ERROR_CODE_UNAUTHORIZED_FRAGMENT_APP_ACCESS, applicationResidingOrganizationId,
                    requestInvokingOrganizationId);
        }
    }

    /**
     * Check if the shareWithAllChildren property in the application should be updated or not.
     *
     * @param shareWithAllChildren Attribute indicating if the application is shared with all sub-organizations.
     * @param mainApplication      Main Application
     * @return if the shareWithAllChildren property in the main application should be updated
     */
    private boolean shouldUpdateShareWithAllChildren(boolean shareWithAllChildren, ServiceProvider mainApplication) {

        /* If shareWithAllChildren is true and in the main application there is no shareWithAllChildren property,
        then the value should be updated. */
        if (shareWithAllChildren && stream(mainApplication.getSpProperties()).noneMatch(
                p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()))) {
            return true;
        }

        /* If shareWithAllChildren is true and in the main application it is set as false,
        then the value should be updated. */
        if (shareWithAllChildren && stream(mainApplication.getSpProperties()).anyMatch(
                p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && !Boolean.parseBoolean(p.getValue()))) {
            return true;
        }

        /* If shareWithAllChildren is false and in the main application it is set as true,
        then the value should be updated. */
        return !shareWithAllChildren && stream(mainApplication.getSpProperties()).anyMatch(
                p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && Boolean.parseBoolean(p.getValue()));
    }

    /**
     * Filter and return shared child applications of the given main application for the given child organization IDs.
     *
     * @param mainAppId Application ID of the main application.
     * @param mainOrgId Organization ID of the main application.
     * @return The map containing organization ID and application ID of the filtered shared applications.
     * @throws OrganizationManagementException If an error occurs while retrieving child applications.
     */
    private Map<String, String> getFilteredChildApplications(String mainAppId, String mainOrgId,
                                                             List<String> childOrgIds)
            throws OrganizationManagementException {

        List<SharedApplicationDO> childApplications =
                getOrgApplicationMgtDAO().getSharedApplications(mainAppId, mainOrgId, childOrgIds);
        return childApplications.stream().collect(Collectors.toMap(
                SharedApplicationDO::getOrganizationId, SharedApplicationDO::getFragmentApplicationId));
    }

    private OAuthAdminServiceImpl getOAuthAdminService() {

        return OrgApplicationMgtDataHolder.getInstance().getOAuthAdminService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrgApplicationMgtDataHolder.getInstance().getOrganizationManager();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }

    private OrgApplicationMgtDAO getOrgApplicationMgtDAO() {

        return OrgApplicationMgtDataHolder.getInstance().getOrgApplicationMgtDAO();
    }

    private RealmService getRealmService() {

        return OrgApplicationMgtDataHolder.getInstance().getRealmService();
    }

    private IdpManager getIdentityProviderManager() {

        return OrgApplicationMgtDataHolder.getInstance().getIdpManager();
    }

    private ApplicationSharingManagerListener getListener() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationSharingManagerListener();
    }

    private ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return OrgApplicationMgtDataHolder.getInstance().getResourceSharingPolicyHandlerService();
    }

    private AsyncOperationStatusMgtService getAsyncStatusMgtService() {

        return OrgApplicationMgtDataHolder.getInstance().getAsyncOperationStatusMgtService();
    }
}
