/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.oauth2.token.handlers.claims.JWTAccessTokenClaimProvider;
import org.wso2.carbon.identity.openidconnect.ClaimProvider;
import org.wso2.carbon.identity.organization.management.claim.provider.OrganizationClaimProvider;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagementInitialize;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * OSGi service component for organization specific claim provider bundle.
 */
@Component(
        name = "identity.organization.management.claim.provider.component",
        immediate = true
)
public class OrganizationClaimProviderServiceComponent {

    private static final Log LOG = LogFactory.getLog(OrganizationClaimProviderServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            OrganizationClaimProvider organizationClaimProvider = new OrganizationClaimProvider();
            context.getBundleContext()
                    .registerService(ClaimProvider.class, organizationClaimProvider, null);
            context.getBundleContext()
                    .registerService(JWTAccessTokenClaimProvider.class, organizationClaimProvider, null);
        } catch (Exception e) {
            LOG.error("Error when registering OrganizationClaimProvider service.", e);
        }
        LOG.debug("OrganizationClaimProvider bundle is activated successfully.");
    }

    @Reference(
            name = "organization.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager"
    )
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the organization management service.");
        }
        OrganizationClaimProviderServiceComponentHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unset organization management service.");
        }
        OrganizationClaimProviderServiceComponentHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "organization.mgt.initialize.service",
            service = OrganizationManagementInitialize.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManagementEnablingService"
    )
    protected void setOrganizationManagementEnablingService(
            OrganizationManagementInitialize organizationManagementInitializeService) {

        OrganizationClaimProviderServiceComponentHolder.getInstance()
                .setOrganizationManagementEnable(organizationManagementInitializeService);
    }

    protected void unsetOrganizationManagementEnablingService(
            OrganizationManagementInitialize organizationManagementInitializeInstance) {

        OrganizationClaimProviderServiceComponentHolder.getInstance().setOrganizationManagementEnable(null);
    }
}
