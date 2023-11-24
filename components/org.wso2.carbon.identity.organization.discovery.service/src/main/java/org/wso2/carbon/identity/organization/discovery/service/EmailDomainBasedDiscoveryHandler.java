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

package org.wso2.carbon.identity.organization.discovery.service;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigClientException;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.wso2.carbon.identity.organization.discovery.service.constant.DiscoveryConstants.PRE_ADD_USER_EMAIL_DOMAIN_VALIDATE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_DISABLED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIGURATION;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.handleServerException;

/**
 * Implementation of email domain based organization discovery type.
 */
public class EmailDomainBasedDiscoveryHandler implements AttributeBasedOrganizationDiscoveryHandler {

    private static final String EMAIL_DOMAIN_DISCOVERY_ENABLE_CONFIG = "emailDomain.enable";
    private static final OrganizationConfigManager organizationConfigManager = OrganizationDiscoveryServiceHolder
            .getInstance().getOrganizationConfigManager();
    private static final Pattern EMAIL_DOMAIN_PATTERN = Pattern.compile("^[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override
    public String getType() {

        return "emailDomain";
    }

    @Override
    public boolean isDiscoveryConfigurationEnabled(String organizationId) throws OrganizationManagementException {

        try {
            String tenantDomain = getOrganizationManager().resolveTenantDomain(organizationId);
            int tenantId = IdentityTenantUtil.getTenantId(tenantDomain);
            List<ConfigProperty> configProperties = organizationConfigManager
                    .getDiscoveryConfigurationByTenantId(tenantId).getConfigProperties();
            return Optional.ofNullable(configProperties).orElse(Collections.emptyList()).stream()
                    .filter(prop -> EMAIL_DOMAIN_DISCOVERY_ENABLE_CONFIG.equals(prop.getKey()))
                    .findAny().map(prop -> Boolean.valueOf(prop.getValue())).orElse(false);
        } catch (OrganizationConfigException e) {
            if (e instanceof OrganizationConfigClientException) {
                if (StringUtils.equals(e.getErrorCode(),
                        OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getCode())) {
                    throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_DISABLED);
                }
            }
            throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIGURATION, e, organizationId);
        }
    }

    @Override
    public String extractAttributeValue(String discoveryInput) {

        String[] emailSplit = discoveryInput.split("@");
        if (emailSplit.length == 2) {
            return emailSplit[1];
        }
        return null;
    }

    @Override
    public List<String> requiredEventValidations() {

        return Collections.singletonList(PRE_ADD_USER_EMAIL_DOMAIN_VALIDATE);
    }

    @Override
    public boolean areAttributeValuesInValidFormat(List<String> attributeValues) {

        return attributeValues.stream().allMatch(emailDomain -> EMAIL_DOMAIN_PATTERN.matcher(emailDomain).matches());
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationDiscoveryServiceHolder.getInstance().getOrganizationManager();
    }
}
