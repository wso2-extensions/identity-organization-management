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

package org.wso2.carbon.identity.organization.management.application.authn.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.authn.EnterpriseIDPAuthenticator;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This class contains the service component of the organization management enterprise idp login authenticator.
 */
@Component(
        name = "identity.organization.application.authenticator.component",
        immediate = true
)
public class EnterpriseIDPAuthenticatorServiceComponent {

    private static final Log log = LogFactory.getLog(EnterpriseIDPAuthenticatorServiceComponent.class);

    @Activate
    protected void activate(ComponentContext ctxt) {

        try {
            EnterpriseIDPAuthenticator enterpriseIDPAuthenticator = new EnterpriseIDPAuthenticator();
            ctxt.getBundleContext()
                    .registerService(ApplicationAuthenticator.class.getName(), enterpriseIDPAuthenticator, null);
            if (log.isDebugEnabled()) {
                log.debug("Enterprise IDP Authenticator bundle is activated");
            }
        } catch (Exception e) {
            log.error(" Error while activating enterprise idp authenticator ", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {

        if (log.isDebugEnabled()) {
            log.debug("Enterprise IDP Authenticator bundle is deactivated");
        }
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset the Realm Service.");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setRealmService(null);
    }

    @Reference(name = "identity.oauth.component",
            service = OAuthAdminServiceImpl.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOAuthAdminService")
    protected void setOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        if (log.isDebugEnabled()) {
            log.debug("OAuth Management Service is set in the OpenID Connect Authenticator");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setOAuthAdminService(oAuthAdminService);
    }

    protected void unsetOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        if (log.isDebugEnabled()) {
            log.debug("OAuth Management Service is unset in the OpenID Connect Authenticator");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setOAuthAdminService(null);
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Organization Manager is set in the Authenticator");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance()
                .setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Organization Manager is unset in the Authenticator");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(name = "identity.organization.application.management.component",
            service = OrgApplicationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgApplicationManager")
    protected void setOrgApplicationManager(OrgApplicationManager orgApplicationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Organization Application Manager is set in the Authenticator");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setOrgApplicationManager(orgApplicationManager);
    }

    protected void unsetOrgApplicationManager(OrgApplicationManager orgApplicationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Organization Application Manager is unset in the Authenticator");
        }
        EnterpriseIDPAuthenticatorDataHolder.getInstance().setOrgApplicationManager(null);
    }
}
