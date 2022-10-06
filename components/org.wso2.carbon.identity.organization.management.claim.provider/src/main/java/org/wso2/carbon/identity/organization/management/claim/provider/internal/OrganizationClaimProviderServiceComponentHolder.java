/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.claim.provider.internal;

import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Service holder for organization claim provider service.
 */
public class OrganizationClaimProviderServiceComponentHolder {

    private static final OrganizationClaimProviderServiceComponentHolder
            ORGANIZATION_CLAIM_PROVIDER_SERVICE_COMPONENT_HOLDER =
            new OrganizationClaimProviderServiceComponentHolder();
    private OrganizationManager organizationManager;

    public static OrganizationClaimProviderServiceComponentHolder getInstance() {

        return ORGANIZATION_CLAIM_PROVIDER_SERVICE_COMPONENT_HOLDER;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }
}
