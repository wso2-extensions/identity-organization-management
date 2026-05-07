/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal;

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.OrganizationConnectionSharingService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.idp.mgt.IdpManager;

/**
 * Data holder for connection sharing management.
 */
public class ConnectionSharingDataHolder {

    private static final ConnectionSharingDataHolder instance = new ConnectionSharingDataHolder();

    private OrganizationManager organizationManager;
    private ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService;
    private IdpManager idpManager;
    private OrganizationConnectionSharingService organizationConnectionSharingService =
            new OrganizationConnectionSharingService() {
            };

    private ConnectionSharingDataHolder() {

    }

    public static ConnectionSharingDataHolder getInstance() {

        return instance;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    public ResourceSharingPolicyHandlerService getResourceSharingPolicyHandlerService() {

        return resourceSharingPolicyHandlerService;
    }

    public void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        this.resourceSharingPolicyHandlerService = resourceSharingPolicyHandlerService;
    }

    public IdpManager getIdpManager() {

        return idpManager;
    }

    public void setIdpManager(IdpManager idpManager) {

        this.idpManager = idpManager;
    }

    public OrganizationConnectionSharingService getOrganizationConnectionSharingService() {

        return organizationConnectionSharingService;
    }

    public void setOrganizationConnectionSharingService(
            OrganizationConnectionSharingService organizationConnectionSharingService) {

        this.organizationConnectionSharingService = organizationConnectionSharingService;
    }
}
