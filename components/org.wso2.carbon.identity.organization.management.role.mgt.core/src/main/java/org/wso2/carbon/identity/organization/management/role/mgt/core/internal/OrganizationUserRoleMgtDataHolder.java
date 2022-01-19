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

package org.wso2.carbon.identity.organization.management.role.mgt.core.internal;

import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.organization.management.role.mgt.core.dao.OrganizationUserRoleMgtDAO;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Organization-User-Role Management Data Holder.
 */
public class OrganizationUserRoleMgtDataHolder {

    private static final OrganizationUserRoleMgtDataHolder orgAndUserRoleMgtDataHolder =
            new OrganizationUserRoleMgtDataHolder();
    private OrganizationUserRoleMgtDAO orgAndUserRoleMgtDao;
    private RealmService realmService;
    private IdentityEventService identityEventService;

    public static OrganizationUserRoleMgtDataHolder getInstance() {

        return orgAndUserRoleMgtDataHolder;
    }

    public OrganizationUserRoleMgtDAO getOrgAndUserRoleMgtDataHolder() {

        return orgAndUserRoleMgtDao;
    }

    public void setOrganizationAndUserRoleMgtDAO(OrganizationUserRoleMgtDAO orgAndUserRoleMgtDao) {

        this.orgAndUserRoleMgtDao = orgAndUserRoleMgtDao;
    }

    public RealmService getRealmService() {

        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    public IdentityEventService getIdentityEventService() {

        return identityEventService;
    }

    public void setIdentityEventService(IdentityEventService identityEventService) {

        this.identityEventService = identityEventService;
    }
}
