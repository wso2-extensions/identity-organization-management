/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.login.mgt.core.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.cors.mgt.core.CORSManagementService;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This class acts as a data holder to the enterprise login management service.
 */
public class EnterpriseLoginManagerDataHolder {

    private static ApplicationManagementService applicationManagementService;
    private static OAuthAdminServiceImpl oAuthAdminService;
    private static RealmService realmService;
    private static CORSManagementService corsManagementService;

    /**
     * Get ApplicationManagementService.
     *
     * @return ApplicationManagementService.
     */
    public static ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    /**
     * Set ApplicationManagementService.
     *
     * @param applicationManagementService UserTenantAssociationManager.
     */
    public static void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        EnterpriseLoginManagerDataHolder.applicationManagementService = applicationManagementService;
    }

    public static RealmService getRealmService() {

        return realmService;
    }

    public static void setRealmService(RealmService realmService) {

        EnterpriseLoginManagerDataHolder.realmService = realmService;
    }

    public static OAuthAdminServiceImpl getOAuthAdminService() {

        return oAuthAdminService;
    }

    public static void setOAuthAdminService(OAuthAdminServiceImpl oauthAdminService) {
        EnterpriseLoginManagerDataHolder.oAuthAdminService = oauthAdminService;
    }

    public static CORSManagementService getCorsManagementService() {

        return corsManagementService;
    }

    public static void setCorsManagementService(CORSManagementService corsManagementService) {

        EnterpriseLoginManagerDataHolder.corsManagementService = corsManagementService;
    }
}
