/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application;

import org.apache.commons.collections.CollectionUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.AUTHORIZATION_CODE_GRANT;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.AUTH_TYPE_OAUTH_2;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.OAUTH_VERSION_2;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.TENANT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Service implementation to process applications across organizations. Class implements {@link OrgApplicationManager}.
 */
public class OrgApplicationManagerImpl implements OrgApplicationManager {

    @Override
    public ServiceProvider getOrgApplication(String applicationId, String tenantDomain)
            throws OrganizationManagementException {

        try {
            ServiceProvider application = getApplicationManagementService().getApplicationByResourceId(applicationId,
                    tenantDomain);
            return Optional.ofNullable(application)
                    .orElseThrow(() -> handleClientException(ERROR_CODE_INVALID_APPLICATION, applicationId));
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_APPLICATION, e, applicationId);
        }
    }

    @Override
    public void shareOrganizationApplication(String ownerOrgId, String originalAppId, List<String> sharedOrgs)
            throws OrganizationManagementException {

        Organization organization = getOrganizationManager().getOrganization(ownerOrgId, Boolean.TRUE);

        String ownerTenantDomain = getTenantDomain();
        ServiceProvider rootApplication = getOrgApplication(originalAppId, ownerTenantDomain);

        //Filter the child organization in case user send a list of organizations to share the original application.
        List<ChildOrganizationDO> filteredChildOrgs = CollectionUtils.isEmpty(sharedOrgs) ?
                organization.getChildOrganizations() :
                organization.getChildOrganizations().stream().filter(o -> sharedOrgs.contains(o.getId()))
                        .collect(Collectors.toList());

        for (ChildOrganizationDO child : filteredChildOrgs) {
            Organization childOrg = getOrganizationManager().getOrganization(child.getId(), Boolean.TRUE);

            if (TENANT.equalsIgnoreCase(childOrg.getType())) {
                shareApplication(ownerTenantDomain, childOrg, rootApplication);
            }
        }

    }

    private String shareApplication(String ownerTenantDomain, Organization sharedOrg, ServiceProvider mainApplication)
            throws OrganizationManagementException {

        try {

            //TODO: finalize whether to use org id or tenant id.
            int parentOrgTenantId = getTenantId();
            int sharedOrgTenantId = IdentityTenantUtil.getTenantId(sharedOrg.getId());

            //Use tenant of the organization to whom the application getting shared. When the consumer application is
            //loaded, tenant domain will be derived from the user who created the application.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedOrg.getId(), true);
            String sharedOrgAdmin =
                    getRealmService().getTenantUserRealm(sharedOrgTenantId).getRealmConfiguration().getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(sharedOrgAdmin);

            Optional<String> mayBeSharedAppId = resolveSharedAppResourceId(sharedOrg.getId(),
                    mainApplication.getApplicationName(), ownerTenantDomain);

            if (mayBeSharedAppId.isPresent()) {
                return mayBeSharedAppId.get();
            }

            // Create Oauth consumer app.
            OAuthConsumerAppDTO createdOAuthApp = createOAuthApplication();

            ServiceProvider delegatedApplication = prepareSharedApplication(mainApplication, createdOAuthApp);

            String sharedApplicationId = getApplicationManagementService().createApplication(delegatedApplication,
                    sharedOrg.getId(), getAuthenticatedUsername());

            getOrgApplicationMgtDAO().addSharedApplication(parentOrgTenantId,
                    mainApplication.getApplicationResourceId(), sharedOrgTenantId, sharedApplicationId);

            return sharedApplicationId;
        } catch (IdentityOAuthAdminException | URLBuilderException | IdentityApplicationManagementException
                | UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e,
                    mainApplication.getApplicationName(), sharedOrg.getName());
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Override
    public Optional<String> resolveSharedAppResourceId(String sharedOrgName, String mainAppName,
                                                       String ownerTenant) throws OrganizationManagementException {

        try {
            int ownerTenantId = IdentityTenantUtil.getTenantId(ownerTenant);
            int sharedTenantId = IdentityTenantUtil.getTenantId(sharedOrgName);
            ServiceProvider mainApplication = getApplicationManagementService().getServiceProvider(mainAppName,
                    ownerTenant);

            return mainApplication == null ? Optional.empty() :
                    getOrgApplicationMgtDAO().getSharedApplicationResourceId(ownerTenantId, sharedTenantId,
                            mainApplication.getApplicationResourceId());

        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppName, ownerTenant);
        }
    }

    private OAuthConsumerAppDTO createOAuthApplication() throws URLBuilderException, IdentityOAuthAdminException {

        ServiceURL commonAuthServiceUrl = ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build();
        String callbackUrl = commonAuthServiceUrl.getAbsolutePublicURL();

        OAuthConsumerAppDTO consumerApp = new OAuthConsumerAppDTO();
        String clientId = UUID.randomUUID().toString();
        consumerApp.setOauthConsumerKey(clientId);
        consumerApp.setOAuthVersion(OAUTH_VERSION_2);
        consumerApp.setGrantTypes(AUTHORIZATION_CODE_GRANT);
        consumerApp.setCallbackUrl(callbackUrl);

        return getOAuthAdminService().registerAndRetrieveOAuthApplicationData(consumerApp);
    }

    private ServiceProvider prepareSharedApplication(ServiceProvider mainApplication,
                                                     OAuthConsumerAppDTO oAuthConsumerApp) {

        // Obtain oauth consumer app configs.
        InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig =
                new InboundAuthenticationRequestConfig();
        inboundAuthenticationRequestConfig.setInboundAuthType(AUTH_TYPE_OAUTH_2);
        inboundAuthenticationRequestConfig.setInboundAuthKey(oAuthConsumerApp.getOauthConsumerKey());

        InboundAuthenticationConfig inboundAuthConfig = new InboundAuthenticationConfig();
        inboundAuthConfig.setInboundAuthenticationRequestConfigs(
                new InboundAuthenticationRequestConfig[]{inboundAuthenticationRequestConfig});

        //TODO: Finalize the application name and description.
        ServiceProvider delegatedApplication = new ServiceProvider();
        delegatedApplication.setApplicationName("internal-" + mainApplication.getApplicationName());
        delegatedApplication.setDescription("delegate access from:" + mainApplication.getApplicationName());
        delegatedApplication.setInboundAuthenticationConfig(inboundAuthConfig);

        return delegatedApplication;
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
}
