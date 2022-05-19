package org.wso2.carbon.identity.organization.management.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
            throw new OrgApplicationMgtException(e);
        }
    }

    @Override
    public String shareOrganizationApplication(Organization parent, Organization childOrg,
                                               ServiceProvider rootApplication) throws OrgApplicationMgtException {

        try {
            ServiceURL commonAuthServiceUrl = ServiceURLBuilder.create().addPath(FrameworkConstants.COMMONAUTH).build();
            String callbackUrl = commonAuthServiceUrl.getAbsolutePublicURL();
            //String allowedOrigin = commonAuthServiceUrl.getAbsolutePublicUrlWithoutPath();

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
                    childOrg.getDomain(), getAuthenticatedUsername());

            OrgApplicationMgtDataHolder.getInstance()
                    .getOrgApplicationMgtDAO().addSharedApplication(getTenantId(),
                            rootApplication.getApplicationResourceId(),
                            IdentityTenantUtil.getTenantId(childOrg.getDomain()), sharedApplicationId,
                            getAuthenticatedUsername());

            return sharedApplicationId;
        } catch (IdentityOAuthAdminException | URLBuilderException | IdentityApplicationManagementException e) {
            throw new OrgApplicationMgtException(e);
        }
    }

    private OAuthAdminServiceImpl getOAuthAdminService() {

        return OrgApplicationMgtDataHolder.getInstance().getoAuthAdminService();
    }

    private ApplicationManagementService getApplicationManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }
}
