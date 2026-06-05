/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.capability.governance.internal;

import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;

/**
 * Singleton data holder for OSGi service references used by the capability governance bundle.
 */
public class GovernancePolicyDataHolder {

    private static final GovernancePolicyDataHolder INSTANCE = new GovernancePolicyDataHolder();

    private OrganizationManager organizationManager;
    private OrgResourceResolverService orgResourceResolverService;

    private GovernancePolicyDataHolder() {

    }

    /**
     * Returns the singleton instance of this data holder.
     *
     * @return the singleton instance.
     */
    public static GovernancePolicyDataHolder getInstance() {

        return INSTANCE;
    }

    /**
     * Returns the OrganizationManager OSGi service reference.
     *
     * @return the organization manager, or {@code null} if not yet bound.
     */
    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    /**
     * Sets the OrganizationManager OSGi service reference.
     *
     * @param organizationManager the organization manager service.
     */
    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    /**
     * Returns the OrgResourceResolverService OSGi service reference.
     *
     * @return the org resource resolver service, or {@code null} if not yet bound.
     */
    public OrgResourceResolverService getOrgResourceResolverService() {

        return orgResourceResolverService;
    }

    /**
     * Sets the OrgResourceResolverService OSGi service reference.
     *
     * @param orgResourceResolverService the org resource resolver service.
     */
    public void setOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        this.orgResourceResolverService = orgResourceResolverService;
    }
}
