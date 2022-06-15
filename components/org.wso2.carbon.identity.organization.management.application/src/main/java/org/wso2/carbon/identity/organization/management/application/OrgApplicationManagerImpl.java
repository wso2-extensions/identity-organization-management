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
import org.wso2.carbon.identity.application.common.model.ServiceProviderProperty;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.ChildOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.AUTH_TYPE_OAUTH_2;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_SHARED_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.TENANT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_APPLICATION_NOT_SHARED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_APPLICATION;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Service implementation to process applications across organizations. Class implements {@link OrgApplicationManager}.
 */
public class OrgApplicationManagerImpl implements OrgApplicationManager {

    @Override
    public void shareOrganizationApplication(String ownerOrgId, String originalAppId, List<String> sharedOrgs)
            throws OrganizationManagementException {

        Organization organization = getOrganizationManager().getOrganization(ownerOrgId, Boolean.TRUE);
        String ownerTenantDomain = getTenantDomain();
        ServiceProvider rootApplication = getOrgApplication(originalAppId, ownerTenantDomain);

        // Filter the child organization in case user send a list of organizations to share the original application.
        List<ChildOrganizationDO> filteredChildOrgs = CollectionUtils.isEmpty(sharedOrgs) ?
                organization.getChildOrganizations() :
                organization.getChildOrganizations().stream().filter(o -> sharedOrgs.contains(o.getId()))
                        .collect(Collectors.toList());

        for (ChildOrganizationDO child : filteredChildOrgs) {
            Organization childOrg = getOrganizationManager().getOrganization(child.getId(), Boolean.FALSE);
            if (TENANT.equalsIgnoreCase(childOrg.getType())) {
                shareApplication(organization.getId(), childOrg, rootApplication);
            }
        }
    }

    @Override
    public ServiceProvider resolveSharedApplication(String mainAppName, String ownerTenantDomain, String sharedOrgId)
            throws OrganizationManagementException {

        int ownerTenantId = IdentityTenantUtil.getTenantId(ownerTenantDomain);

        //TODO: Implement method in org manager to return the organization based on tenant id??
        String ownerOrgId = ownerTenantId == MultitenantConstants.SUPER_TENANT_ID ?
                getOrganizationManager().getOrganizationIdByName(OrganizationManagementConstants.ROOT) :
                getOrganizationManager().getOrganization(ownerTenantDomain, Boolean.FALSE).getId();

        ServiceProvider mainApplication;
        try {
            mainApplication = Optional.ofNullable(getApplicationManagementService().getServiceProvider(mainAppName,
                            ownerTenantDomain))
                    .orElseThrow(() -> handleClientException(ERROR_CODE_INVALID_APPLICATION, mainAppName));
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppName,
                    ownerTenantDomain);
        }

        Organization sharedOrganization = getOrganizationManager().getOrganization(sharedOrgId, Boolean.FALSE);
        String sharedAppId =
                resolveSharedApp(mainApplication.getApplicationResourceId(), ownerOrgId, sharedOrgId).orElseThrow(
                        () -> handleClientException(ERROR_CODE_APPLICATION_NOT_SHARED));
        String tenantDomain = IdentityTenantUtil.getTenantDomain(sharedOrganization.getTenantId());
        try {
            return getApplicationManagementService().getApplicationByResourceId(sharedAppId, tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppName,
                    ownerTenantDomain);
        }
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

    private void shareApplication(String ownerOrgId, Organization sharedOrg, ServiceProvider mainApplication)
            throws OrganizationManagementException {

        try {
            // Use tenant of the organization to whom the application getting shared. When the consumer application is
            // loaded, tenant domain will be derived from the user who created the application.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedOrg.getId(), true);
            String sharedOrgAdmin =
                    getRealmService().getTenantUserRealm(sharedOrg.getTenantId()).getRealmConfiguration()
                            .getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(sharedOrgAdmin);

            Optional<String> mayBeSharedAppId = resolveSharedApp(
                    mainApplication.getApplicationResourceId(), ownerOrgId, sharedOrg.getId());
            if (mayBeSharedAppId.isPresent()) {
                return;
            }
            // Create Oauth consumer app to redirect login to shared application.
            OAuthConsumerAppDTO createdOAuthApp = createOAuthApplication();
            ServiceProvider delegatedApplication = prepareSharedApplication(mainApplication, createdOAuthApp);
            String sharedApplicationId = getApplicationManagementService().createApplication(delegatedApplication,
                    sharedOrg.getId(), getAuthenticatedUsername());
            getOrgApplicationMgtDAO().addSharedApplication(mainApplication.getApplicationResourceId(), ownerOrgId,
                    sharedApplicationId, sharedOrg.getId());
        } catch (IdentityOAuthAdminException | URLBuilderException | IdentityApplicationManagementException
                | UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e,
                    mainApplication.getApplicationResourceId(), sharedOrg.getId());
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private Optional<String> resolveSharedApp(String mainAppId, String ownerOrgId, String sharedOrgId)
            throws OrganizationManagementException {

        return getOrgApplicationMgtDAO().getSharedApplicationResourceId(mainAppId, ownerOrgId,
                sharedOrgId);
    }

    private OAuthConsumerAppDTO createOAuthApplication() throws URLBuilderException, IdentityOAuthAdminException {

        ServiceURL commonAuthServiceUrl = ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build();
        String callbackUrl = commonAuthServiceUrl.getAbsolutePublicURL();

        OAuthConsumerAppDTO consumerApp = new OAuthConsumerAppDTO();
        String clientId = UUID.randomUUID().toString();
        consumerApp.setOauthConsumerKey(clientId);
        consumerApp.setOAuthVersion(OAuthConstants.OAuthVersions.VERSION_2);
        consumerApp.setGrantTypes(OAuthConstants.GrantTypes.AUTHORIZATION_CODE);
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
        delegatedApplication.setApplicationName(mainApplication.getApplicationName() + "-shared-" + UUID.randomUUID());
        delegatedApplication.setDescription("Delegated access from:" + mainApplication.getApplicationName());
        delegatedApplication.setInboundAuthenticationConfig(inboundAuthConfig);
        appendSharedAppProperty(delegatedApplication);

        return delegatedApplication;
    }

    private void appendSharedAppProperty(ServiceProvider serviceProvider) {

        ServiceProviderProperty sharedAppProperty = new ServiceProviderProperty();
        sharedAppProperty.setName(IS_SHARED_APP);
        sharedAppProperty.setValue(Boolean.TRUE.toString());

        ServiceProviderProperty[] spProperties = new ServiceProviderProperty[]{sharedAppProperty};
        serviceProvider.setSpProperties(spProperties);
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
