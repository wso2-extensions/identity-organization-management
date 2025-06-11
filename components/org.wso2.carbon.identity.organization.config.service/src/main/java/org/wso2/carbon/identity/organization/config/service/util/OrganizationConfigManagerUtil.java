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

package org.wso2.carbon.identity.organization.config.service.util;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.internal.OrganizationConfigServiceHolder;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;

import java.util.List;

import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.DEFAULT_PARAM;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_GETTING_ORGANIZATION_DISCOVERY_CONFIG;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationDiscoveryDefaultParam;

/**
 * This class provides organization configuration management utility functions.
 */
public class OrganizationConfigManagerUtil {

    public static final String ORGANIZATION_NAME = "orgName";
    public static final String ORG_PARAMETER = "org";

    public static String resolveTheDiscoveryDefaultParam() throws OrganizationConfigException {

        // Priority is given to the tenant-level configuration over the system-level config.
        try {
            OrganizationConfigManager organizationConfigManager = OrganizationConfigServiceHolder.getInstance()
                    .getOrganizationConfigManager();
            DiscoveryConfig discoveryConfig = organizationConfigManager.getDiscoveryConfiguration();
            List<ConfigProperty> configProperties = discoveryConfig.getConfigProperties();
            for (ConfigProperty configProperty : configProperties) {
                if (DEFAULT_PARAM.equals(configProperty.getKey())
                        && StringUtils.isNotBlank(configProperty.getValue())) {
                    return mapToOrgParam(configProperty.getValue());
                }
            }
        } catch (OrganizationConfigException e) {
            if (ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getCode().equals(e.getErrorCode())) {
                return mapToOrgParam(getOrganizationDiscoveryDefaultParam());
            }
            throw new OrganizationConfigException(ERROR_CODE_ERROR_GETTING_ORGANIZATION_DISCOVERY_CONFIG.getCode(),
                    ERROR_CODE_ERROR_GETTING_ORGANIZATION_DISCOVERY_CONFIG.getMessage(),
                    ERROR_CODE_ERROR_GETTING_ORGANIZATION_DISCOVERY_CONFIG.getDescription());
        }
        return mapToOrgParam(getOrganizationDiscoveryDefaultParam());
    }

    private static String mapToOrgParam(String param) {
        if (ORGANIZATION_NAME.equals(param)) {
            return ORG_PARAMETER;
        }
        return param;
    }
}
