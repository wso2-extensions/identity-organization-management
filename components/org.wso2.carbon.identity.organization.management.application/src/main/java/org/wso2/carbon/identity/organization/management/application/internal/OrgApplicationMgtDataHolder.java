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

package org.wso2.carbon.identity.organization.management.application.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Data holder for organization application management.
 */
public class OrgApplicationMgtDataHolder {

    private static final OrgApplicationMgtDataHolder dataHolder = new OrgApplicationMgtDataHolder();

    private OrgApplicationMgtDAO orgApplicationMgtDAO;

    private RealmService realmService;
    private ApplicationManagementService applicationManagementService;
    private OAuthAdminServiceImpl oAuthAdminService;
    private OrganizationManager organizationManager;
    private OrganizationUserResidentResolverService organizationUserResidentResolverService;
    private IdpManager idpManager;

    private OrgApplicationMgtDataHolder() {

    }

    public static OrgApplicationMgtDataHolder getInstance() {

        return dataHolder;
    }

    public OrganizationUserResidentResolverService getOrganizationUserResidentResolverService() {

        return organizationUserResidentResolverService;
    }

    public void setOrganizationUserResidentResolverService(OrganizationUserResidentResolverService
                                                                   organizationUserResidentResolverService) {

        this.organizationUserResidentResolverService = organizationUserResidentResolverService;
    }

    public OrgApplicationMgtDAO getOrgApplicationMgtDAO() {

        return orgApplicationMgtDAO;
    }

    public void setOrgApplicationMgtDAO(OrgApplicationMgtDAO orgApplicationMgtDAO) {

        this.orgApplicationMgtDAO = orgApplicationMgtDAO;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
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

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    public IdpManager getIdpManager() {

        return idpManager;
    }

    public void setIdpManager(IdpManager idpManager) {

        this.idpManager = idpManager;
    }
}
