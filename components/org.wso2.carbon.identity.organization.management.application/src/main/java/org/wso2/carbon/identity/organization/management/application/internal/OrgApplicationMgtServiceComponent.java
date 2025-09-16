/*
 * Copyright (c) 2022-2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.service.AsyncOperationStatusMgtService;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManagerImpl;
import org.wso2.carbon.identity.organization.management.application.dao.impl.OrgApplicationMgtDAOImpl;
import org.wso2.carbon.identity.organization.management.application.handler.OrgClaimMgtHandler;
import org.wso2.carbon.identity.organization.management.application.listener.ApplicationSharingManagerListenerImpl;
import org.wso2.carbon.identity.organization.management.application.listener.FragmentApplicationMgtListener;
import org.wso2.carbon.identity.organization.management.application.listener.OrganizationCreationHandler;
import org.wso2.carbon.identity.organization.management.application.listener.MainApplicationEventListener;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OSGi service component for organization application management bundle.
 */
@Component(
        name = "identity.organization.application.management.component",
        immediate = true
)
public class OrgApplicationMgtServiceComponent {

    private static final Log log = LogFactory.getLog(OrgApplicationMgtServiceComponent.class);

    /**
     * Register the Organization Application Mgt service in the OSGI context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            OrgApplicationMgtDataHolder.getInstance()
                    .setOrgApplicationMgtDAO(new OrgApplicationMgtDAOImpl());
            OrgApplicationMgtDataHolder.getInstance()
                    .setApplicationSharingManagerListener(new ApplicationSharingManagerListenerImpl());
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(OrgApplicationManager.class.getName(), new OrgApplicationManagerImpl(), null);
            //Fragment application listener.
            bundleContext.registerService(ApplicationMgtListener.class.getName(), new FragmentApplicationMgtListener(),
                    null);
            bundleContext.registerService(ApplicationMgtListener.class.getName(),
                    new MainApplicationEventListener(), null);
            bundleContext.registerService(AbstractEventHandler.class.getName(), new OrganizationCreationHandler(),
                    null);
            bundleContext.registerService(AbstractEventHandler.class.getName(), new OrgClaimMgtHandler(), null);
            if (log.isDebugEnabled()) {
                log.debug("Organization Application Management component activated successfully.");
            }
        } catch (Throwable e) {
            log.error("Error while activating Organization Application Management module.", e);
        }
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        OrgApplicationMgtDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset the Realm Service.");
        }
        OrgApplicationMgtDataHolder.getInstance().setRealmService(null);
    }

    @Reference(name = "identity.application.management.component",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService")
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(applicationManagementService);
    }

    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        OrgApplicationMgtDataHolder.getInstance().setApplicationManagementService(null);
    }

    @Reference(name = "org.wso2.carbon.identity.oauth",
            service = OAuthAdminServiceImpl.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOAuthAdminService")
    protected void setOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        OrgApplicationMgtDataHolder.getInstance().setOAuthAdminService(oAuthAdminService);
    }

    protected void unsetOAuthAdminService(OAuthAdminServiceImpl oAuthAdminService) {

        OrgApplicationMgtDataHolder.getInstance().setOAuthAdminService(null);
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OrgApplicationMgtDataHolder.getInstance().setOrganizationManager(null);
    }

    @Reference(
            name = "organization.user.resident.resolver.service",
            service = OrganizationUserResidentResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationUserResidentResolverService"
    )
    protected void setOrganizationUserResidentResolverService(OrganizationUserResidentResolverService
                                                                      organizationUserResidentResolverService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the organization user resolver service.");
        }
        OrgApplicationMgtDataHolder.getInstance().setOrganizationUserResidentResolverService
                (organizationUserResidentResolverService);
    }

    protected void unsetOrganizationUserResidentResolverService(OrganizationUserResidentResolverService
                                                                        organizationUserResidentResolverService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset organization user resolver service.");
        }
        OrgApplicationMgtDataHolder.getInstance().setOrganizationUserResidentResolverService(null);
    }

    @Reference(
            name = "idp.mgt.dscomponent",
            service = IdpManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdpManager"
    )
    protected void setIdpManager(IdpManager idpManager) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Identity Provider manager service.");
        }
        OrgApplicationMgtDataHolder.getInstance().setIdpManager(idpManager);
    }

    protected void unsetIdpManager(IdpManager idpManager) {

        if (log.isDebugEnabled()) {
            log.debug("Unset the Identity Provider manager service.");
        }
        OrgApplicationMgtDataHolder.getInstance().setIdpManager(null);
    }

    @Reference(
            name = "identity.event.service",
            service = IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService"
    )
    protected void setIdentityEventService(IdentityEventService identityEventService) {

        log.debug("Set Identity Event Service.");
        OrgApplicationMgtDataHolder.getInstance().setIdentityEventService(identityEventService);
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {

        log.debug("Unset Identity Event Service.");
        OrgApplicationMgtDataHolder.getInstance().setIdentityEventService(null);
    }

    @Reference(
            name = "claim.metadata.management.service",
            service = ClaimMetadataManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetClaimMetaDataManagementService"
    )
    protected void setClaimMetaDataManagementService(ClaimMetadataManagementService claimMetadataManagementService) {

        log.debug("Setting the claim metadata management service.");
        OrgApplicationMgtDataHolder.getInstance().setClaimMetadataManagementService(claimMetadataManagementService);
    }

    protected void unsetClaimMetaDataManagementService(ClaimMetadataManagementService claimMetadataManagementService) {

        log.debug("Unset the claim metadata management service.");
        OrgApplicationMgtDataHolder.getInstance().setClaimMetadataManagementService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService",
            service = org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementServiceV2")
    protected void setRoleManagementServiceV2(RoleManagementService roleManagementService) {

        OrgApplicationMgtDataHolder.getInstance().setRoleManagementServiceV2(roleManagementService);
        log.debug("RoleManagementServiceV2 set in OrgApplicationMgtServiceComponent bundle.");
    }

    protected void unsetRoleManagementServiceV2(RoleManagementService roleManagementService) {

        OrgApplicationMgtDataHolder.getInstance().setRoleManagementServiceV2(null);
        log.debug("RoleManagementServiceV2 unset in OrgApplicationMgtServiceComponent bundle.");
    }

    @Reference(
            name = "async.operation.status.mgt.service",
            service = AsyncOperationStatusMgtService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAsyncStatusMgtService"
    )
    protected void setAsyncStatusMgtService(AsyncOperationStatusMgtService asyncOperationStatusMgtService) {

        OrgApplicationMgtDataHolder.getInstance().setAsyncOperationStatusMgtService(asyncOperationStatusMgtService);
        log.debug("Set Async Operation Status Mgt Service On Application Management Component.");
    }

    protected void unsetAsyncStatusMgtService(AsyncOperationStatusMgtService asyncOperationStatusMgtService) {

        OrgApplicationMgtDataHolder.getInstance().setAsyncOperationStatusMgtService(null);
        log.debug("Unset Async Operation Status Mgt Service On Application Management Component.");
    }

    @Reference(
            name = "ResourceSharingPolicyHandlerService",
            service = ResourceSharingPolicyHandlerService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetResourceSharingPolicyHandlerService")
    protected void setResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        OrgApplicationMgtDataHolder.getInstance()
                .setResourceSharingPolicyHandlerService(resourceSharingPolicyHandlerService);
    }

    protected void unsetResourceSharingPolicyHandlerService(
            ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService) {

        OrgApplicationMgtDataHolder.getInstance().setResourceSharingPolicyHandlerService(null);
    }
}
