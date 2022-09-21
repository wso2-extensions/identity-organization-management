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
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;
import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.SHARE_WITH_SUB_ORGS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_ORGANIZATION_APPLICATIONS;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getAuthenticatedUsername;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * This class contains the implementation of the tenant management listener. This listener will be used to add
 * shared applications to newly created organizations.
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

        ApplicationBasicInfo[] applicationBasicInfos;
        try {
            applicationBasicInfos = getApplicationManagementService().getAllApplicationBasicInfo(
                    getTenantDomain(), getAuthenticatedUsername());
        } catch (IdentityApplicationManagementException e) {
            LOG.error("Encountered an error while retrieving applications in tenant domain " + getTenantDomain(), e);
            return;
        }

        for (ApplicationBasicInfo applicationBasicInfo: applicationBasicInfos) {
            ServiceProvider serviceProvider;
            try {
                serviceProvider = getApplicationManagementService()
                        .getServiceProvider(applicationBasicInfo.getApplicationId());
                String ownerOrgId = getOrganizationId();
                if (ownerOrgId == null) {
                    ownerOrgId = SUPER_ORG_ID;
                }

                if (Arrays.stream(serviceProvider.getSpProperties())
                        .anyMatch(p -> IS_FRAGMENT_APP.equalsIgnoreCase(p.getName())
                                && Boolean.parseBoolean(p.getValue()))) {
                    Optional<MainApplicationDO> mainApplicationDO = getOrgApplicationMgtDAO()
                            .getMainApplication(serviceProvider.getApplicationResourceId(), ownerOrgId);
                    if (mainApplicationDO.isPresent()) {
                        ownerOrgId = mainApplicationDO.get().getOrganizationId();
                        serviceProvider = getApplicationManagementService().getServiceProvider(
                                serviceProvider.getApplicationName(),
                                getOrganizationManager().resolveTenantDomain(ownerOrgId));

                    }
                }

                boolean shareWithChildren = Arrays.stream(serviceProvider.getSpProperties())
                        .anyMatch(p -> SHARE_WITH_SUB_ORGS.equalsIgnoreCase(
                                p.getName()) && Boolean.parseBoolean(p.getValue()));
                if (shareWithChildren) {
                    getOrgApplicationManager().shareApplication(ownerOrgId, organization.getId(), serviceProvider);
                }
            } catch (IdentityApplicationManagementException e) {
                LOG.error("Encountered an error while retrieving applications in tenant domain " + getTenantDomain(), e);
            } catch (OrganizationManagementException e) {
                LOG.error(String.format("Encountered an error while sharing application %s to new organization %s",
                        applicationBasicInfo.getApplicationResourceId(), organization.getId()), e);
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
