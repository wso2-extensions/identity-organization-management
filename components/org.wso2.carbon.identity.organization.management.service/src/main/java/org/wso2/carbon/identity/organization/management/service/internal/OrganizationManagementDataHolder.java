/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.service.internal;

import org.wso2.carbon.identity.organization.management.service.dao.OrganizationApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.tenant.mgt.services.TenantMgtService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Organization management data holder.
 */
public class OrganizationManagementDataHolder {

    private static final OrganizationManagementDataHolder instance = new OrganizationManagementDataHolder();
    private OrganizationManagementDAO organizationManagementDAO;
    private OrganizationApplicationMgtDAO organizationApplicationMgtDAO;
    private RealmService realmService;
    private TenantMgtService tenantMgtService;

    public static OrganizationManagementDataHolder getInstance() {

        return instance;
    }

    public OrganizationManagementDAO getOrganizationManagementDAO() {

        return organizationManagementDAO;
    }

    public void setOrganizationManagementDAO(OrganizationManagementDAO organizationManagementDAO) {

        this.organizationManagementDAO = organizationManagementDAO;
    }

    public OrganizationApplicationMgtDAO getOrganizationApplicationMgtDAO() {

        return organizationApplicationMgtDAO;
    }

    public void setOrganizationApplicationMgtDAO(OrganizationApplicationMgtDAO organizationApplicationMgtDAO) {

        this.organizationApplicationMgtDAO = organizationApplicationMgtDAO;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    public TenantMgtService getTenantMgtService() {

        return tenantMgtService;
    }

    public void setTenantMgtService(TenantMgtService tenantMgtService) {

        this.tenantMgtService = tenantMgtService;
    }
}
