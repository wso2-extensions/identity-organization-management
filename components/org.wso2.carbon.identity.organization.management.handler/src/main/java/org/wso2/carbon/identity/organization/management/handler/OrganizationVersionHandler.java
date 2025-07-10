/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler;

import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;

import java.util.List;
import java.util.Map;

/**
 * Event handler to handle operations related to organization version updates.
 */
public class OrganizationVersionHandler extends AbstractEventHandler {

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        Map<String, Object> eventProperties = event.getEventProperties();

        /*
         * This handler clears the resident IdP cache when the organization version changes between v0.0.0 and v1.0.0
         * or higher. Between these versions, the inheritance of resident IdP properties is toggled, so cache clearance
         * ensures the correct properties are loaded. The handler listens to PRE_UPDATE and PRE_PATCH events to capture
         * both the previous and new version of the updated organization.
         * Since only cache clearance occurs here, there is no adverse effect if the organization update fails
         * after this handler executes.
         */
        if (Constants.EVENT_PRE_UPDATE_ORGANIZATION.equals(eventName)) {
            handlePreUpdateOrganization(eventProperties);
        } else if (Constants.EVENT_PRE_PATCH_ORGANIZATION.equals(eventName)) {
            handlePrePatchOrganization(eventProperties);
        }
    }

    private void handlePrePatchOrganization(Map<String, Object> eventProperties) throws IdentityEventException {

        String organizationId = (String) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID);
        List<PatchOperation> patchOperations =
                (List<PatchOperation>) eventProperties.get(Constants.EVENT_PROP_PATCH_OPERATIONS);

        try {
            if (OrganizationManagementUtil.isOrganization(
                    getOrganizationManager().resolveTenantDomain(organizationId))) {
                return;
            }

            String existingOrgVersion = getOrganizationManager().getOrganization(
                    organizationId, false, false).getVersion();
            String newOrgVersion = null;

            for (PatchOperation patchOperation : patchOperations) {
                if (OrganizationManagementConstants.PATCH_PATH_ORG_VERSION.equals(patchOperation.getPath())) {
                    newOrgVersion = patchOperation.getValue();
                    break;
                }
            }

            if (isLoginAndRegistrationConfigInheritanceUpdated(existingOrgVersion, newOrgVersion)) {
                clearIdpCache(organizationId);
            }
        } catch (OrganizationManagementException | IdentityProviderManagementException e) {
            throw new IdentityEventException(String.format(
                    "Error while handling pre-patch organization event in %s for organization ID: %s", this.getName(),
                    organizationId), e);
        }
    }

    private void handlePreUpdateOrganization(Map<String, Object> eventProperties) throws IdentityEventException {

        String organizationId = (String) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION_ID);
        Organization updatedOrganization =
                (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);

        try {
            if (OrganizationManagementUtil.isOrganization(
                    getOrganizationManager().resolveTenantDomain(organizationId))) {
                return;
            }

            String existingOrgVersion = getOrganizationManager().getOrganization(
                    organizationId, false, false).getVersion();
            String newOrgVersion = updatedOrganization.getVersion();

            if (isLoginAndRegistrationConfigInheritanceUpdated(existingOrgVersion, newOrgVersion)) {
                clearIdpCache(organizationId);
            }
        } catch (OrganizationManagementException | IdentityProviderManagementException e) {
            throw new IdentityEventException(String.format(
                    "Error while handling pre-update organization event in %s for organization ID: %s", this.getName(),
                    organizationId), e);
        }
    }

    /**
     * Clear the IDP cache for the resident IDP of the organization and all child organizations.
     *
     * @param organizationId Organization ID.
     * @throws OrganizationManagementException     If an error occurs while retrieving tenant information.
     * @throws IdentityProviderManagementException If an error occurs while clearing the IDP cache.
     */
    private void clearIdpCache(String organizationId)
            throws OrganizationManagementException, IdentityProviderManagementException {

        IdPManagementUtil.clearIdPCache(
                IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME,
                getOrganizationManager().resolveTenantDomain(organizationId));
    }

    private static OrganizationManager getOrganizationManager() {

        return OrganizationManagementHandlerDataHolder.getInstance().getOrganizationManager();
    }

    private boolean isLoginAndRegistrationConfigInheritanceUpdated(String existingVersion, String newVersion) {

        return newVersion != null && !newVersion.equals(existingVersion) &&
                (OrganizationManagementConstants.OrganizationVersion.ORG_VERSION_V0.equals(existingVersion) ||
                        OrganizationManagementConstants.OrganizationVersion.ORG_VERSION_V0.equals(newVersion));
    }
}
