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

package org.wso2.carbon.identity.organization.management.application.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManagerImpl;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.model.MainApplicationDO;
import org.wso2.carbon.identity.organization.management.application.model.SharedApplicationDO;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_ALL_CHILDREN;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;

/**
 * This class contains the implementation of the handler for post organization creation.
 * This handler will be used to add shared applications to newly created organizations.
 */
public class OrganizationCreationHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(OrganizationCreationHandler.class);

    @Override
    public void handleEvent(Event event) {

        String eventName = event.getEventName();

        if (Constants.EVENT_POST_ADD_ORGANIZATION.equals(eventName)) {
            Map<String, Object> eventProperties = event.getEventProperties();
            Organization organization = (Organization) eventProperties.get(Constants.EVENT_PROP_ORGANIZATION);
            addSharedApplicationsToOrganization(organization);
        }
    }

    private void addSharedApplicationsToOrganization(Organization organization) {

        String ownerOrgId = getOrganizationId();
        if (ownerOrgId == null) {
            ownerOrgId = SUPER_ORG_ID;
        }

        ApplicationBasicInfo[] applicationBasicInfos;
        try {
            applicationBasicInfos = getApplicationManagementService().getAllApplicationBasicInfo(
                    getTenantDomain(), getAuthenticatedUsername());
        } catch (IdentityApplicationManagementException e) {
            LOG.error("Encountered an error while retrieving applications in organization " + ownerOrgId, e);
            return;
        }

        for (ApplicationBasicInfo applicationBasicInfo: applicationBasicInfos) {
            try {
                if (getOrgApplicationMgtDAO().isFragmentApplication(applicationBasicInfo.getApplicationId())) {
                    Optional<SharedApplicationDO> sharedApplicationDO;
                    try {
                        sharedApplicationDO = getOrgApplicationMgtDAO().getSharedApplication(
                                applicationBasicInfo.getApplicationId(), ownerOrgId);
                    } catch (OrganizationManagementException e) {
                        LOG.error("Encountered an error while retrieving shared application", e);
                        continue;
                    }

                    if (sharedApplicationDO.isPresent() && sharedApplicationDO.get().shareWithAllChildren()) {
                        Optional<MainApplicationDO> mainApplicationDO;
                        try {
                            mainApplicationDO = getOrgApplicationMgtDAO().getMainApplication(
                                    sharedApplicationDO.get().getFragmentApplicationId(),
                                    sharedApplicationDO.get().getOrganizationId());
                            if (mainApplicationDO.isPresent()) {
                                String tenantDomain = getOrganizationManager().resolveTenantDomain(
                                        mainApplicationDO.get().getOrganizationId());
                                ServiceProvider mainApplication = getApplicationManagementService()
                                        .getApplicationByResourceId(mainApplicationDO.get().getMainApplicationId(),
                                                tenantDomain);
                                ownerOrgId = mainApplicationDO.get().getOrganizationId();
                                getOrgApplicationManager().shareApplication(ownerOrgId, organization.getId(),
                                        mainApplication, true);
                            }
                        } catch (OrganizationManagementException e) {
                            LOG.error("Encountered an error while sharing application", e);
                        } catch (IdentityApplicationManagementException e) {
                            LOG.error("Encountered an error while retrieving application", e);
                        }
                    }
                } else {
                    ServiceProvider mainApplication;
                    try {
                        mainApplication = getApplicationManagementService().getServiceProvider(
                                applicationBasicInfo.getApplicationId());
                        if (mainApplication != null && Arrays.stream(mainApplication.getSpProperties())
                                .anyMatch(p -> SHARE_WITH_ALL_CHILDREN.equalsIgnoreCase(
                                        p.getName()) && Boolean.parseBoolean(p.getValue()))) {
                            getOrgApplicationManager().shareApplication(ownerOrgId, organization.getId(),
                                    mainApplication, true);
                        }
                    } catch (IdentityApplicationManagementException e) {
                        LOG.error("Encountered an error while retrieving application", e);
                    } catch (OrganizationManagementException e) {
                        LOG.error("Encountered an error while sharing application", e);
                    }
                }
            } catch (OrganizationManagementException e) {
                LOG.error("Encountered an error while checking if application is a fragment application", e);
            }

        }
    }


    private ApplicationManagementService getApplicationManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }

    private OrgApplicationManager getOrgApplicationManager() {

        return new OrgApplicationManagerImpl();
    }

    private OrgApplicationMgtDAO getOrgApplicationMgtDAO() {

        return OrgApplicationMgtDataHolder.getInstance().getOrgApplicationMgtDAO();
    }

    private OrganizationManager getOrganizationManager() {

        return OrgApplicationMgtDataHolder.getInstance().getOrganizationManager();
    }

}
