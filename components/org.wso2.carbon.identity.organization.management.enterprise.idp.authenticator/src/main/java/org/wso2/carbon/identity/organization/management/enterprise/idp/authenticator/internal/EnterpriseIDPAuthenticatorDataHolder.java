/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.enterprise.idp.authenticator.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.EnterpriseLoginManagementService;

/**
 * This class acts as a data holder to the enterprise idp login authenticator service.
 */
public class EnterpriseIDPAuthenticatorDataHolder {

    private static EnterpriseIDPAuthenticatorDataHolder instance = new EnterpriseIDPAuthenticatorDataHolder();

    private ApplicationManagementService applicationManagementService;

    private OAuthAdminServiceImpl oAuthAdminService;

    private EnterpriseLoginManagementService enterpriseLoginManagementService;

    private EnterpriseIDPAuthenticatorDataHolder() {

    }

    public static EnterpriseIDPAuthenticatorDataHolder getInstance() {

        return instance;
    }

    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }

    public OAuthAdminServiceImpl getOAuthAdminService() {

        return oAuthAdminService;
    }

    public void setOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        this.oAuthAdminService = oAuthAdminService;
    }

    public EnterpriseLoginManagementService getEnterpriseLoginManagementService() {

        return enterpriseLoginManagementService;
    }

    public void setEnterpriseLoginManagementService(EnterpriseLoginManagementService enterpriseLoginManagementService) {

        this.enterpriseLoginManagementService = enterpriseLoginManagementService;
    }
}
