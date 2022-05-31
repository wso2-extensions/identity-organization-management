package org.wso2.carbon.identity.organization.management.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.wso2.carbon.identity.organization.management.application.exception.OrgApplicationMgtException;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Optional;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORG_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.handleServerException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;

/**
 * Service implementation to process applications across organizations. Class implements {@link OrgApplicationManager}.
 */
public class OrgApplicationManagerImpl implements OrgApplicationManager {

    private static final Log LOG = LogFactory.getLog(OrgApplicationManagerImpl.class);

    @Override
    public ServiceProvider getOrgApplication(String applicationId, String tenantDomain)
            throws OrgApplicationMgtException {

        try {
            return getApplicationManagementService().getApplicationByResourceId(applicationId, tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_ORG_APPLICATION, e, applicationId);
        }
    }

    @Override
    public String shareOrganizationApplication(Organization parentOrg, Organization sharedOrg,
                                               ServiceProvider mainApplication) throws OrgApplicationMgtException {

        try {

            int parentOrgTenantId = getTenantId();
            String tenantDomain = getTenantDomain();
            int sharedOrgTenantId = IdentityTenantUtil.getTenantId(sharedOrg.getId());

            //Use tenant of the organization to whom the application getting shared.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedOrg.getId(), true);
            RealmService realmService = OrgApplicationMgtDataHolder.getInstance().getRealmService();
            String sharedOrgAdmin = realmService.getTenantUserRealm(sharedOrgTenantId).getRealmConfiguration().
                    getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(sharedOrgAdmin);

            Optional<String> mayBeSharedAppId = resolveOrganizationSpResourceId(sharedOrg.getId(),
                    mainApplication.getApplicationName(), tenantDomain);

            if (mayBeSharedAppId.isPresent()) {
                return mayBeSharedAppId.get();
            }

            // Create Oauth consumer app.
            OAuthConsumerAppDTO createdOAuthApp = createOAuthApplication();

            ServiceProvider delegatedApplication = prepareSharedApplication(mainApplication, createdOAuthApp);

            String sharedApplicationId = getApplicationManagementService().createApplication(delegatedApplication,
                    sharedOrg.getId(), getAuthenticatedUsername());

            OrgApplicationMgtDataHolder.getInstance()
                    .getOrgApplicationMgtDAO().addSharedApplication(parentOrgTenantId,
                            mainApplication.getApplicationResourceId(), sharedOrgTenantId, sharedApplicationId);

            return sharedApplicationId;
        } catch (IdentityOAuthAdminException | URLBuilderException | IdentityApplicationManagementException
                | UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Override
    public Optional<String> resolveOrganizationSpResourceId(String sharedOrgName, String mainAppName,
                                                            String ownerTenant) throws OrgApplicationMgtException {

        try {
            int ownerTenantId = IdentityTenantUtil.getTenantId(ownerTenant);
            int sharedTenantId = IdentityTenantUtil.getTenantId(sharedOrgName);
            ServiceProvider mainApplication =
                    OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService().
                            getServiceProvider(mainAppName, ownerTenant);

            return mainApplication == null ? Optional.empty() : OrgApplicationMgtDataHolder.getInstance()
                    .getOrgApplicationMgtDAO().getSharedApplicationResourceId(ownerTenantId, sharedTenantId,
                            mainApplication.getApplicationResourceId());

        } catch (IdentityApplicationManagementException | OrgApplicationMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, mainAppName);
        }
    }

    private OAuthConsumerAppDTO createOAuthApplication() throws URLBuilderException, IdentityOAuthAdminException {

        ServiceURL commonAuthServiceUrl = ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build();
        String callbackUrl = commonAuthServiceUrl.getAbsolutePublicURL();

        OAuthConsumerAppDTO consumerApp = new OAuthConsumerAppDTO();
        String clientId = UUID.randomUUID().toString();
        consumerApp.setOauthConsumerKey(clientId);
        consumerApp.setOAuthVersion("OAuth-2.0");
        consumerApp.setGrantTypes("authorization_code");
        consumerApp.setCallbackUrl(callbackUrl);

        return getOAuthAdminService().registerAndRetrieveOAuthApplicationData(consumerApp);
    }

    private ServiceProvider prepareSharedApplication(ServiceProvider mainApplication,
                                                     OAuthConsumerAppDTO oAuthConsumerApp)  {

        // Obtain oauth consumer app configs.
        InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig =
                new InboundAuthenticationRequestConfig();
        inboundAuthenticationRequestConfig.setInboundAuthType("oauth2");
        inboundAuthenticationRequestConfig.setInboundAuthKey(oAuthConsumerApp.getOauthConsumerKey());

        InboundAuthenticationConfig inboundAuthConfig = new InboundAuthenticationConfig();
        inboundAuthConfig.setInboundAuthenticationRequestConfigs(
                new InboundAuthenticationRequestConfig[]{inboundAuthenticationRequestConfig});

        ServiceProvider delegatedApplication = new ServiceProvider();
        delegatedApplication.setApplicationName("internal-" + mainApplication.getApplicationName());
        delegatedApplication.setDescription("delegate access from:" + mainApplication.getApplicationName());
        delegatedApplication.setInboundAuthenticationConfig(inboundAuthConfig);

        return delegatedApplication;
    }

    private OAuthAdminServiceImpl getOAuthAdminService() {

        return OrgApplicationMgtDataHolder.getInstance().getoAuthAdminService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }
}
