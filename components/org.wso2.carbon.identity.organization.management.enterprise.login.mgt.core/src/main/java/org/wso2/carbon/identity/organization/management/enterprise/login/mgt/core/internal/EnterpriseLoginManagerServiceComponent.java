/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.cors.mgt.core.CORSManagementService;
import org.wso2.carbon.identity.cors.mgt.core.internal.impl.CORSManagementServiceImpl;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.EnterpriseLoginManagementService;
import org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.EnterpriseLoginManagementServiceImpl;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This class contains the implementation of the enterprise login management service component.
 */
@Component(
        name = "org.wso2.carbon.identity.org.mgt.enterprise.login.mgt.core.component",
        immediate = true
)
public class EnterpriseLoginManagerServiceComponent {

    private static final Log log = LogFactory.getLog(EnterpriseLoginManagerServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            EnterpriseLoginManagementService enterpriseLoginManagementService =
                    new EnterpriseLoginManagementServiceImpl();
            componentContext.getBundleContext().registerService(EnterpriseLoginManagementService.class.getName(),
                    enterpriseLoginManagementService, null);

            CORSManagementService corsManagementService = new CORSManagementServiceImpl();
            componentContext.getBundleContext().registerService(CORSManagementService.class.getName(),
                    corsManagementService, null);

            if (log.isDebugEnabled()) {
                log.debug("Enterprise login management service bundle activated successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to activate EnterpriseLoginManagementService service component.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {

        if (log.isDebugEnabled()) {
            log.debug("Enterprise login management service bundle is deactivated.");
        }
    }

    @Reference(
            name = "RealmService",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        EnterpriseLoginManagerDataHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        EnterpriseLoginManagerDataHolder.setRealmService(null);
    }


    @Reference(name = "identity.application.management.component",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        EnterpriseLoginManagerDataHolder.setApplicationManagementService(applicationManagementService);
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        EnterpriseLoginManagerDataHolder.setApplicationManagementService(null);
    }


    @Reference(name = "org.wso2.carbon.identity.oauth",
            service = OAuthAdminServiceImpl.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unSetOAuthAdminService")
    protected void setOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        EnterpriseLoginManagerDataHolder.setOAuthAdminService(oAuthAdminService);
    }

    protected void unSetOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        EnterpriseLoginManagerDataHolder.setOAuthAdminService(null);
    }

    @Reference(name = "identity.cors.management.component",
               service = CORSManagementService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unsetCorsManagementService")
    protected void setCorsManagementService(CORSManagementService corsManagementService) {

        EnterpriseLoginManagerDataHolder.setCorsManagementService(corsManagementService);
    }

    protected void unsetCorsManagementService(CORSManagementService corsManagementService) {

        EnterpriseLoginManagerDataHolder.setCorsManagementService(null);
    }
}
