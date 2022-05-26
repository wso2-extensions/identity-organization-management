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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORG_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_ERROR_SHARING_APPLICATION;
import static org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil.handleServerException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantId;

/**
 *
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
                                               ServiceProvider rootApplication) throws OrgApplicationMgtException {

        try {
            ServiceURL commonAuthServiceUrl = ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build();
            String callbackUrl = commonAuthServiceUrl.getAbsolutePublicURL();
            //String allowedOrigin = commonAuthServiceUrl.getAbsolutePublicUrlWithoutPath();

            int parentOrgTenantId = getTenantId();
            int sharedOrgTenantId = IdentityTenantUtil.getTenantId(sharedOrg.getId());

            //Use tenant of the organization to whom the application getting shared.
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(sharedOrg.getId(), true);
            RealmService realmService = OrgApplicationMgtDataHolder.getInstance().getRealmService();
            String sharedOrgAdmin = realmService.getTenantUserRealm(sharedOrgTenantId).getRealmConfiguration().
                    getAdminUserName();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(sharedOrgAdmin);

            // Prepare consumer oauth application.
            OAuthConsumerAppDTO consumerApp = new OAuthConsumerAppDTO();
            String clientId = UUID.randomUUID().toString();
            consumerApp.setOauthConsumerKey(clientId);
            consumerApp.setOAuthVersion("OAuth-2.0");
            consumerApp.setGrantTypes("authorization_code");
            consumerApp.setCallbackUrl(callbackUrl);

            // Create Oauth consumer app.
            OAuthConsumerAppDTO createdOAuthApp =
                    getOAuthAdminService().registerAndRetrieveOAuthApplicationData(consumerApp);

            // Obtain oauth consumer app configs.
            InboundAuthenticationRequestConfig inboundAuthenticationRequestConfig =
                    new InboundAuthenticationRequestConfig();
            inboundAuthenticationRequestConfig.setInboundAuthType("oauth2");
            inboundAuthenticationRequestConfig.setInboundAuthKey(createdOAuthApp.getOauthConsumerKey());

            List<InboundAuthenticationRequestConfig> inbounds = new ArrayList<>();
            inbounds.add(inboundAuthenticationRequestConfig);
            InboundAuthenticationConfig inboundAuthConfig = new InboundAuthenticationConfig();
            inboundAuthConfig.setInboundAuthenticationRequestConfigs(
                    inbounds.toArray(new InboundAuthenticationRequestConfig[0]));

            ServiceProvider delegatedApplication = new ServiceProvider();
            delegatedApplication.setApplicationName("internal-" + rootApplication.getApplicationName());
            delegatedApplication.setDescription("delegate access from:" + rootApplication.getApplicationName());
            delegatedApplication.setTemplateId(null);
            delegatedApplication.setInboundAuthenticationConfig(inboundAuthConfig);

            String sharedApplicationId = getApplicationManagementService().createApplication(delegatedApplication,
                    sharedOrg.getId(), getAuthenticatedUsername());

            OrgApplicationMgtDataHolder.getInstance()
                    .getOrgApplicationMgtDAO().addSharedApplication(parentOrgTenantId,
                            rootApplication.getApplicationResourceId(), sharedOrgTenantId, sharedApplicationId,
                            sharedOrgAdmin);

            return sharedApplicationId;
        } catch (IdentityOAuthAdminException | URLBuilderException | IdentityApplicationManagementException
                | UserStoreException e) {
            throw handleServerException(ERROR_CODE_ERROR_SHARING_APPLICATION, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Override
    public String resolveOrganizationSpResourceId(String orgName, String parentApplication,
                                                            String parentTenant) throws OrgApplicationMgtException {

        try {
            int inboundSpTenantId = IdentityTenantUtil.getTenantId(parentTenant);
            int outboundSpTenantId = IdentityTenantUtil.getTenantId(orgName);
            String inboundSpResourceId = OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService().
                    getServiceProvider(parentApplication, parentTenant).getApplicationResourceId();
            return OrgApplicationMgtDataHolder.getInstance().getOrgApplicationMgtDAO()
                    .getSharedApplicationResourceId(inboundSpTenantId, outboundSpTenantId, inboundSpResourceId);
        } catch (IdentityApplicationManagementException | OrgApplicationMgtException e) {
            throw handleServerException(ERROR_CODE_ERROR_RESOLVING_SHARED_APPLICATION, e, parentApplication);
        }
    }

    private OAuthAdminServiceImpl getOAuthAdminService() {

        return OrgApplicationMgtDataHolder.getInstance().getoAuthAdminService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }
}
