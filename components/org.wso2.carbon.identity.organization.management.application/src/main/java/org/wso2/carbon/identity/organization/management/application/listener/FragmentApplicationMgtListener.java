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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.dao.OrgApplicationMgtDAO;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Arrays;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

/**
 * Application listener to restrict actions on shared applications and fragment applications.
 */
public class FragmentApplicationMgtListener extends AbstractApplicationMgtListener {

    @Override
    public int getDefaultOrderId() {

        return 50;
    }

    @Override
    public boolean isEnable() {

        IdentityEventListenerConfig identityEventListenerConfig = IdentityUtil.readEventListenerProperty
                (ApplicationMgtListener.class.getName(), this.getClass().getName());

        if (identityEventListenerConfig == null) {
            return false;
        }

        if (StringUtils.isNotBlank(identityEventListenerConfig.getEnable())) {
            return Boolean.parseBoolean(identityEventListenerConfig.getEnable());
        } else {
            return false;
        }
    }

    @Override
    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain,
                                          String userName) throws IdentityApplicationManagementException {

        // If the application is a shared application, only certain updates to the application are allowed,
        // if any other data has been change, listener will reject the update request.
        ServiceProvider existingApplication =
                getApplicationByResourceId(serviceProvider.getApplicationResourceId(), tenantDomain);
        if (existingApplication != null && Arrays.stream(existingApplication.getSpProperties())
                .anyMatch(p -> IS_FRAGMENT_APP.equalsIgnoreCase(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
            if (!validateUpdatedData(existingApplication, serviceProvider)) {
                return false;
            }
        }

        return super.doPreUpdateApplication(serviceProvider, tenantDomain, userName);
    }

    @Override
    public boolean doPreDeleteApplication(String applicationName, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        ServiceProvider application = getApplicationByName(applicationName, tenantDomain);
        if (application == null) {
            return false;
        }

        // If the application is a fragment application, application cannot be deleted
        if (Arrays.stream(application.getSpProperties())
                .anyMatch(p -> IS_FRAGMENT_APP.equalsIgnoreCase(p.getName()) && Boolean.parseBoolean(p.getValue()))) {
            return false;
        }

        try {
            // If an application has fragment applications, application cannot be deleted.
            if (getOrgApplicationMgtDAO().hasFragments(application.getApplicationResourceId())) {
                return false;
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityApplicationManagementException("Error in validating the application for deletion.", e);
        }

        return super.doPreDeleteApplication(applicationName, tenantDomain, userName);
    }

    private boolean validateUpdatedData(ServiceProvider existingApplication, ServiceProvider serviceProvider) {

        return existingApplication.getInboundAuthenticationConfig() ==
                serviceProvider.getInboundAuthenticationConfig() ||
                existingApplication.getPermissionAndRoleConfig() == serviceProvider.getPermissionAndRoleConfig() ||
                existingApplication.getSpProperties() == serviceProvider.getSpProperties();
    }

    private ServiceProvider getApplicationByResourceId(String applicationResourceId, String tenantDomain)
            throws IdentityApplicationManagementException {

        return getApplicationMgtService().getApplicationByResourceId(applicationResourceId, tenantDomain);
    }

    private ServiceProvider getApplicationByName(String name, String tenantDomain)
            throws IdentityApplicationManagementException {

        return getApplicationMgtService().getServiceProvider(name, tenantDomain);
    }

    private ApplicationManagementService getApplicationMgtService() {

        return OrgApplicationMgtDataHolder.getInstance().getApplicationManagementService();
    }

    private OrgApplicationMgtDAO getOrgApplicationMgtDAO() {

        return OrgApplicationMgtDataHolder.getInstance().getOrgApplicationMgtDAO();
    }
}
