/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementClientException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.script.AuthenticationScriptConfig;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.application.mgt.listener.ApplicationMgtListener;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.application.util.OrgApplicationManagerUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;

import java.util.Arrays;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.IS_FRAGMENT_APP;

/**
 * Application listener to enforce governance policy restrictions on sub-organization native applications.
 */
public class SubOrgApplicationMgtListener extends AbstractApplicationMgtListener {

    @Override
    public int getDefaultOrderId() {

        return 375;
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
        }
        return false;
    }

    @Override
    public boolean doPreCreateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        try {
            if (!OrganizationManagementUtil.isOrganization(tenantDomain)) {
                return super.doPreCreateApplication(serviceProvider, tenantDomain, userName);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityApplicationManagementException(
                    String.format("Error while resolving the organization for the tenant %s.", tenantDomain), e);
        }

        enforceAdaptiveScriptGovernance(serviceProvider, null, tenantDomain);
        return super.doPreCreateApplication(serviceProvider, tenantDomain, userName);
    }

    @Override
    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {

        try {
            if (!OrganizationManagementUtil.isOrganization(tenantDomain) || isFragmentApp(serviceProvider)) {
                return super.doPreUpdateApplication(serviceProvider, tenantDomain, userName);
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityApplicationManagementException(
                    String.format("Error while resolving the organization for the tenant %s.", tenantDomain), e);
        }

        ServiceProvider existingApplication = OrgApplicationMgtDataHolder.getInstance()
                .getApplicationManagementService()
                .getApplicationByResourceId(serviceProvider.getApplicationResourceId(), tenantDomain);
        enforceAdaptiveScriptGovernance(serviceProvider, existingApplication, tenantDomain);
        return super.doPreUpdateApplication(serviceProvider, tenantDomain, userName);
    }

    private void enforceAdaptiveScriptGovernance(ServiceProvider serviceProvider,
            ServiceProvider existingServiceProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        LocalAndOutboundAuthenticationConfig localAndOutBoundAuthenticationConfig =
                serviceProvider.getLocalAndOutBoundAuthenticationConfig();
        if (localAndOutBoundAuthenticationConfig == null ||
                localAndOutBoundAuthenticationConfig.getAuthenticationScriptConfig() == null) {
            return;
        }
        AuthenticationScriptConfig authenticationScriptConfig =
                localAndOutBoundAuthenticationConfig.getAuthenticationScriptConfig();
        if (!authenticationScriptConfig.isEnabled() ||
                StringUtils.isBlank(authenticationScriptConfig.getContent())) {
            return;
        }
        authenticationScriptConfig.setContent(StringUtils.stripEnd(authenticationScriptConfig.getContent(), null));
        if (OrgApplicationManagerUtil.isAdaptiveScriptUnchanged(existingServiceProvider, authenticationScriptConfig)) {
            return;
        }
        if (OrgApplicationManagerUtil.isAdaptiveAuthBlockedByGovernance(tenantDomain)) {
            throw new IdentityApplicationManagementClientException(
                    "Authentication script configuration not allowed for sub-organization applications.");
        }
    }

    private boolean isFragmentApp(ServiceProvider serviceProvider) {

        return serviceProvider != null && serviceProvider.getSpProperties() != null &&
                Arrays.stream(serviceProvider.getSpProperties()).anyMatch(
                        property -> IS_FRAGMENT_APP.equalsIgnoreCase(property.getName()) &&
                                Boolean.parseBoolean(property.getValue()));
    }

}
