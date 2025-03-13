/*
 * Copyright (c) 2022-2024, WSO2 LLC. (http://www.wso2.com).
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
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
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
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventClientException;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.framework.async.status.mgt.AsyncStatusMgtService;
import org.wso2.carbon.identity.framework.async.status.mgt.models.dos.*;
import org.wso2.carbon.identity.framework.async.status.mgt.queue.SubOperationStatusObject;
import org.wso2.carbon.identity.framework.async.status.mgt.queue.SubOperationStatusQueue;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.dto.OAuthAppRevocationRequestDTO;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.constant.ShareOperationType;
import org.wso2.carbon.identity.organization.management.application.constant.SharePolicy;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.listener.ApplicationSharingManagerListener;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplication;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementClientException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.User;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.OAUTH2;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ORGANIZATION_CONTEXT_PREFIX;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.TENANT_CONTEXT_PREFIX;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_DEFAULT;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.AUTH_TYPE_FLOW;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.DEFAULT_BACKCHANNEL_LOGOUT_URL;
import static org.wso2.carbon.identity.application.mgt.ApplicationConstants.MYACCOUNT_PORTAL_PATH;
import static org.wso2.carbon.identity.base.IdentityConstants.SKIP_CONSENT;
import static org.wso2.carbon.identity.oauth.Error.DUPLICATE_OAUTH_CLIENT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.AUTH_TYPE_OAUTH_2;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_FRAGMENT_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.DELETE_SHARE_FOR_MAIN_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ORGANIZATION_LOGIN_AUTHENTICATOR;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.TENANT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.TENANT_CONTEXT_PATH_COMPONENT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.createOrganizationSSOIDP;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.getDefaultAuthenticationConfig;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setIsAppSharedProperty;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.setShareWithAllChildrenProperty;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_APPLICATION_NOT_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_BLOCK_SHARING_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_OAUTH_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_CREATING_ORG_LOGIN_IDP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_REMOVING_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_TENANT_DOMAIN_FROM_ORGANIZATION_DOMAIN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_IDP_LIST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_REVOKING_SHARED_APP_TOKENS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_DELETE_SHARE_REQUEST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_APPLICATION_SHARE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_FRAGMENT_APP_ACCESS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.IS_APP_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Service implementation to process applications across organizations. Class implements {@link OrgApplicationManager}.
 */
public class OrgApplicationManagerImpl implements OrgApplicationManager {

    private static final Log LOG = LogFactory.getLog(OrgApplicationManagerImpl.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    AsyncStatusMgtService asyncStatusMgtService = getAsyncStatusMgtService();
    private final ConcurrentMap<String, SubOperationStatusQueue> asyncOperationStatusList = new ConcurrentHashMap<>();
    private final InheritableThreadLocal<String> ASYNC_OPERATION_ID = new InheritableThreadLocal<>();

    @Override
    public void shareOrganizationApplication(String ownerOrgId, String originalAppId, boolean shareWithAllChildren,
                                             List<String> sharedOrgs) throws OrganizationManagementException {
        if (!shareWithAllChildren && CollectionUtils.isEmpty(sharedOrgs)) {
            return;
        }
        String requestInvokingOrganizationId = getOrganizationId();
        if (requestInvokingOrganizationId == null) {
            requestInvokingOrganizationId = SUPER_ORG_ID;
        }

        String userID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();

        String sharePolicy = shareWithAllChildren ? SharePolicy.SHARE_WITH_ALL.getValue() : SharePolicy.SELECTIVE_SHARE.getValue();
        String operationId = asyncStatusMgtService.registerOperationStatus(new OperationRecord(ShareOperationType.APPLICATION_SHARE.getValue(), originalAppId, ownerOrgId, userID, sharePolicy), true);
        ASYNC_OPERATION_ID.set(operationId);
        SubOperationStatusQueue statusQueue = new SubOperationStatusQueue();
        asyncOperationStatusList.put(operationId,statusQueue);

        validateApplicationShareAccess(requestInvokingOrganizationId, ownerOrgId);
        Organization organization = getOrganizationManager().getOrganization(ownerOrgId, false, false);
        String ownerTenantDomain = getTenantDomain();
        ServiceProvider rootApplication = getOrgApplication(originalAppId, ownerTenantDomain);
        if (isAlreadySharedApplication(rootApplication)) {
            throw handleClientException(ERROR_CODE_BLOCK_SHARING_SHARED_APP, originalAppId);
        }

        List<BasicOrganization> childOrganizations = getOrganizationManager().getChildOrganizations(ownerOrgId, true);
        // Filter the child organization in case user send a list of organizations to share the original application.
        List<BasicOrganization> filteredChildOrgs = shareWithAllChildren
                ? childOrganizations
                : childOrganizations.stream().filter(o -> sharedOrgs.contains(o.getId())).collect(Collectors.toList());

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

        // Skip adding the SSO if there are no sub orgs.
        if (childOrganizations.isEmpty()) {
            return;
        }

        if (shareWithAllChildren || !filteredChildOrgs.isEmpty()) {
            // Adding Organization login IDP to the root application.
            modifyRootApplication(rootApplication, ownerTenantDomain);
        }

        setIsAppSharedProperty(rootApplication, !filteredChildOrgs.isEmpty());
        try {
            getApplicationManagementService().updateApplication(rootApplication, ownerTenantDomain,
                    getAuthenticatedUsername());
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION, e,
                    rootApplication.getApplicationResourceId());
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (BasicOrganization child : filteredChildOrgs) {
            Organization childOrg = getOrganizationManager().getOrganization(child.getId(), false, false);
            if (TENANT.equalsIgnoreCase(childOrg.getType())) {
                String asyncOperationId = ASYNC_OPERATION_ID.get();
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        ASYNC_OPERATION_ID.set(asyncOperationId);
                        shareApplication(organization.getId(), childOrg.getId(), rootApplication, shareWithAllChildren);
                    } catch (OrganizationManagementException e) {
                        LOG.error(String.format("Error in sharing application: %s to organization: %s",
                                rootApplication.getApplicationID(), childOrg.getId()), e);
                    }
                }, executorService);

                futures.add(future);
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            asyncStatusMgtService.updateOperationStatus(ASYNC_OPERATION_ID.get(), getOperationStatus(ASYNC_OPERATION_ID.get()));
        }).join();
        ASYNC_OPERATION_ID.remove();
    }
    private String getOperationStatus(String operationId){
        SubOperationStatusQueue list = asyncOperationStatusList.get(operationId);
        String status = list.getOperationStatus();
        asyncOperationStatusList.remove(operationId);
        return status;
    }

    @Override
    public void deleteSharedApplication(String organizationId, String applicationId, String sharedOrganizationId)
            throws OrganizationManagementException {

        validateFragmentApplicationAccess(getOrganizationId(), organizationId);
        ServiceProvider serviceProvider = getOrgApplication(applicationId, getTenantDomain());

        String userID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserId();

        String operationId = asyncStatusMgtService.registerOperationStatus(new OperationRecord(ShareOperationType.APPLICATION_SHARE.getValue(), applicationId, organizationId, userID, SharePolicy.DO_NOT_SHARE.getValue()), true);
        ASYNC_OPERATION_ID.set(operationId);

        if (sharedOrganizationId == null) {
            getListener().preDeleteAllSharedApplications(organizationId, applicationId);
            // Unshare application for all shared organizations.
            List<SharedApplicationDO> sharedApplicationDOList =
                    getOrgApplicationMgtDAO().getSharedApplications(organizationId, applicationId);
            for (SharedApplicationDO sharedApplicationDO : sharedApplicationDOList) {
                IdentityUtil.threadLocalProperties.get().put(DELETE_SHARE_FOR_MAIN_APPLICATION, true);
                Optional<String> sharedApplicationId =
                        resolveSharedApp(serviceProvider.getApplicationResourceId(), organizationId,
                                sharedApplicationDO.getOrganizationId());
                if (sharedApplicationId.isPresent()) {
                    revokeSharedAppAccessTokens(organizationId, applicationId,
                            sharedApplicationDO.getOrganizationId());
                    deleteSharedApplication(sharedApplicationDO.getOrganizationId(), sharedApplicationId.get());
                }
                IdentityUtil.threadLocalProperties.get().remove(DELETE_SHARE_FOR_MAIN_APPLICATION);
            }
            getListener().postDeleteAllSharedApplications(organizationId, applicationId, sharedApplicationDOList);
            boolean isSharedWithAllChildren = stream(serviceProvider.getSpProperties())
                    .anyMatch(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && Boolean.parseBoolean(p.getValue()));
            boolean isAppShared = stream(serviceProvider.getSpProperties())
                    .anyMatch(p -> IS_APP_SHARED.equals(p.getName()) && Boolean.parseBoolean(p.getValue()));
            if (isSharedWithAllChildren || isAppShared) {
                setShareWithAllChildrenProperty(serviceProvider, false);
                setIsAppSharedProperty(serviceProvider, false);
                IdentityUtil.threadLocalProperties.get().put(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN, true);
                try {
                    getApplicationManagementService().updateApplication(
                            serviceProvider, getTenantDomain(), getAuthenticatedUsername());
                } catch (IdentityApplicationManagementException e) {
                    throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, applicationId);
                } finally {
                    IdentityUtil.threadLocalProperties.get().remove(UPDATE_SP_METADATA_SHARE_WITH_ALL_CHILDREN);
                }
            }
        } else {
            getListener().preDeleteSharedApplication(organizationId, applicationId, sharedOrganizationId);
            if (stream(serviceProvider.getSpProperties())
                    .anyMatch(p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
                throw handleClientException(ERROR_CODE_INVALID_DELETE_SHARE_REQUEST,
                        serviceProvider.getApplicationResourceId(), sharedOrganizationId);
            }
            Optional<String> sharedApplicationId =
                    resolveSharedApp(serviceProvider.getApplicationResourceId(), organizationId, sharedOrganizationId);
            if (sharedApplicationId.isPresent()) {
                revokeSharedAppAccessTokens(organizationId, applicationId, sharedOrganizationId);
                deleteSharedApplication(sharedOrganizationId, sharedApplicationId.get());
                List<BasicOrganization> applicationSharedOrganizations =
                        getApplicationSharedOrganizations(organizationId, serviceProvider.getApplicationResourceId());
                if (CollectionUtils.isEmpty(applicationSharedOrganizations)) {
                    setIsAppSharedProperty(serviceProvider, false);
                    try {
                        getApplicationManagementService().updateApplication(serviceProvider, getTenantDomain(),
                                getAuthenticatedUsername());
                    } catch (IdentityApplicationManagementException e) {
                        throw handleServerException(ERROR_CODE_ERROR_UPDATING_APPLICATION_ATTRIBUTE, e, applicationId);
                    }
                }
                getListener().postDeleteSharedApplication(organizationId, applicationId, sharedOrganizationId,
                        sharedApplicationId.get());
            }
        }
        asyncStatusMgtService.updateOperationStatus(ASYNC_OPERATION_ID.get(), "SUCCESS");
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
            ServiceProvider sharedApplication =
                    getApplicationManagementService().getApplicationByResourceId(sharedApplicationId,
                            sharedTenantDomain);
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

        getListener().preGetApplicationSharedOrganizations(organizationId, applicationId);
        ServiceProvider application = getOrgApplication(applicationId, getTenantDomain());
        List<SharedApplicationDO> sharedApps =
                getOrgApplicationMgtDAO().getSharedApplications(organizationId, application.getApplicationResourceId());

        List<String> sharedOrganizationIds = sharedApps.stream().map(SharedApplicationDO::getOrganizationId).collect(
                Collectors.toList());

        List<BasicOrganization> organizations = getOrganizationManager().getChildOrganizations(organizationId, true);
        List<BasicOrganization> applicationSharedOrganizationsList =
                organizations.stream().filter(o -> sharedOrganizationIds.contains(o.getId())).collect(
                        Collectors.toList());
        getListener().postGetApplicationSharedOrganizations(organizationId, applicationId,
                applicationSharedOrganizationsList);
        return applicationSharedOrganizationsList;
    }

    @Override
    public List<SharedApplication> getSharedApplications(String organizationId, String applicationId)
            throws OrganizationManagementException {

        getListener().preGetSharedApplications(organizationId, applicationId);
        ServiceProvider application = getOrgApplication(applicationId, getTenantDomain());
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
            boolean idpAlreadyConfigured =
                    stream(first.getFederatedIdentityProviders()).map(
                                    IdentityProvider::getDefaultAuthenticatorConfig)
                            .anyMatch(auth -> ORGANIZATION_LOGIN_AUTHENTICATOR.equals(auth.getName()));
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
                (IdentityProvider[]) ArrayUtils.addAll(first.getFederatedIdentityProviders(),
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
    public void
    shareApplication(String ownerOrgId, String sharedOrgId, ServiceProvider mainApplication,
                                 boolean shareWithAllChildren) throws OrganizationManagementException {
        String statusMessage = "";
        String status = "SUCCESS";
        try {
            getListener().preShareApplication(ownerOrgId, mainApplication.getApplicationResourceId(), sharedOrgId,
                    shareWithAllChildren);
            // Use tenant of the organization to whom the application getting shared. When the consumer application is
            // loaded, tenant domain will be derived from the user who created the application.
            String sharedTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrgId);
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
                        .resolveUserFromResidentOrganization(null, adminUserId, sharedOrgId)
                        .map(User::getDomainQualifiedUsername)
                        .orElse(MultitenantUtils.getTenantAwareUsername(mainApplication.getOwner()
                                .toFullQualifiedUsername()));
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(domainQualifiedUserName);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUserId(adminUserId);
            } catch (UserStoreException e) {
                if(ASYNC_OPERATION_ID.get() != null){
                    status = "FAIL";
                    statusMessage = "UserStoreException: " + e.getMessage();
                }
                throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e,
                        mainApplication.getApplicationResourceId(), sharedOrgId);
            }

            Optional<String> mayBeSharedAppId = resolveSharedApp(
                    mainApplication.getApplicationResourceId(), ownerOrgId, sharedOrgId);
            if (mayBeSharedAppId.isPresent()) {
                return;
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
                    createdOAuthApp = handleOAuthClientExistsError(ownerOrgId, sharedOrgId, mainApplication);
                } else {
                    if(ASYNC_OPERATION_ID.get() != null){
                        status = "FAIL";
                        statusMessage = "OAuth App Creation Error: " + e.getMessage();
                    }
                    throw handleServerException(ERROR_CODE_ERROR_CREATING_OAUTH_APP, e,
                            mainApplication.getApplicationResourceId(), sharedOrgId);
                }
            }
            String sharedApplicationId;
            try {
                ServiceProvider delegatedApplication =
                        prepareSharedApplication(mainApplication, createdOAuthApp, sharedOrgId);
                sharedApplicationId = getApplicationManagementService().createApplication(delegatedApplication,
                        sharedTenantDomain, getAuthenticatedUsername());
                getOrgApplicationMgtDAO().addSharedApplication(mainApplication.getApplicationResourceId(), ownerOrgId,
                        sharedApplicationId, sharedOrgId, shareWithAllChildren);
            } catch (IdentityApplicationManagementException e) {
                removeOAuthApplication(createdOAuthApp);
                if(ASYNC_OPERATION_ID.get() != null){
                    status = "FAIL";
                    statusMessage = "IdentityApplicationManagementException: " + e.getMessage();
                }
                throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e,
                        mainApplication.getApplicationResourceId(), sharedOrgId);
            }
            getListener().postShareApplication(ownerOrgId, mainApplication.getApplicationResourceId(), sharedOrgId,
                    sharedApplicationId, shareWithAllChildren);
        } finally {
            if(ASYNC_OPERATION_ID.get() != null){
                UnitOperationRecord operationStatus = new UnitOperationRecord(ASYNC_OPERATION_ID.get(), "application_share", mainApplication.getApplicationResourceId(), sharedOrgId, status, statusMessage);

                asyncStatusMgtService.registerUnitOperationStatus(operationStatus);
                asyncOperationStatusList.get(ASYNC_OPERATION_ID.get()).add(new SubOperationStatusObject(status));
            }
            PrivilegedCarbonContext.endTenantFlow();
        }

        /*
            If the sharing main application is Console, Create the shared admin user in shared organization
            and assign the admin role.
        */
        if ("Console".equals(mainApplication.getApplicationName())) {
            fireOrganizationCreatorSharingEvent(sharedOrgId);
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
            if (!isFragmentApp) {
                // Given app is a main application.
                return true;
            }
        }
        return false;
    }

    /**
     * This method checks whether the exception is thrown due to the OAuth client already existing.
     * @param e The IdentityException thrown upon OAuth app creation failure.
     * @return  Boolean indicating whether the exception is thrown due to the OAuth client already existing.
     */
    private boolean isOAuthClientExistsError(IdentityException e) {

        return DUPLICATE_OAUTH_CLIENT.getErrorCode().equals(e.getErrorCode());
    }

    /**
     * This method is to handle the exception due to an app already existing during the Oauth app creation process.
     * It is possible that the error is due to stale data, hence a retry mechanism is implemented to check
     * whether it is a stale app and if so, delete the stale app and retry the oauth app creation.
     *
     * @param ownerOrgId        ID of the owner organization.
     * @param sharedOrgId       ID of the shared sub organization.
     * @param mainApplication   The application that is being shared.
     * @return                  OAuth app that is created.
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

    private boolean isAlreadySharedApplication(ServiceProvider serviceProvider) {

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
        if (shareWithAllChildren && !(stream(mainApplication.getSpProperties()).anyMatch(
                p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName())))) {
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
        if (!shareWithAllChildren && stream(mainApplication.getSpProperties()).anyMatch(
                p -> SHARE_WITH_ALL_CHILDREN.equals(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
            return true;
        }

        return false;
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

    private ClaimMetadataManagementService getClaimMetadataManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getClaimMetadataManagementService();
    }

    private ApplicationSharingManagerListener getListener() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationSharingManagerListener();
    }

    private AsyncStatusMgtService getAsyncStatusMgtService(){
        return OrgApplicationMgtDataHolder.getInstance().getAsyncStatusMgtService();
    }
}
