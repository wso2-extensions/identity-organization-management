/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.organization.valve.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Class to hold Organization Management Organization Valve related service data.
 */
public class OrganizationManagementOrganizationValveServiceHolder {

    private static OrganizationManagementOrganizationValveServiceHolder instance =
            new OrganizationManagementOrganizationValveServiceHolder();
    private RealmService realmService;
    private String pageNotFoundErrorPage;

    private static final Log LOG = LogFactory.getLog(OrganizationManagementOrganizationValveServiceHolder.class);

    private OrganizationManagementOrganizationValveServiceHolder() {

    }

    public static OrganizationManagementOrganizationValveServiceHolder getInstance() {

        return instance;
    }

    public RealmService getRealmService() {

        if (realmService == null) {
            throw new RuntimeException("RealmService is null.");
        }
        return realmService;
    }

    public void setRealmService(RealmService realmService) {

        this.realmService = realmService;
    }

    /**
     * Get default template from file artifacts.
     */
    public String getPageNotFoundErrorPage() {

        return pageNotFoundErrorPage;
    }

    public void setPageNotFoundErrorPage(String pageNotFoundErrorPage) {

        this.pageNotFoundErrorPage = pageNotFoundErrorPage;
    }
}
