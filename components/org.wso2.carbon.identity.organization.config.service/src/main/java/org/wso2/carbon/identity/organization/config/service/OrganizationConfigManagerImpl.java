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

package org.wso2.carbon.identity.organization.config.service;

import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.exception.ConfigurationManagementException;
import org.wso2.carbon.identity.configuration.mgt.core.model.Attribute;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.internal.OrganizationConfigServiceHolder;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.configuration.mgt.core.constant.ConfigurationConstants.ErrorMessages.ERROR_CODE_RESOURCE_DOES_NOT_EXISTS;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_CONFLICT;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_UPDATE_NOT_ALLOWED;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_DISCOVERY_CONFIG;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_ERROR_DELETING_DISCOVERY_CONFIG;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIG;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.RESOURCE_NAME;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.RESOURCE_TYPE_NAME;
import static org.wso2.carbon.identity.organization.config.service.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.config.service.util.Utils.handleServerException;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Implementation of Organization Configuration Manager Interface.
 */
public class OrganizationConfigManagerImpl implements OrganizationConfigManager {

    @Override
    public void addDiscoveryConfiguration(DiscoveryConfig discoveryConfig) throws OrganizationConfigException {

        try {
            if (!isDiscoveryConfigChangeAllowed()) {
                throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_UPDATE_NOT_ALLOWED);
            }
            Optional<Resource> resourceOptional = getDiscoveryResource();
            if (resourceOptional.isPresent()) {
                throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_CONFLICT, getOrganizationId());
            }
            Resource resource = buildResourceFromValidationConfig(discoveryConfig);
            getConfigurationManager().addResource(RESOURCE_TYPE_NAME, resource);
        } catch (ConfigurationManagementException | OrganizationManagementServerException e) {
            throw handleServerException(ERROR_CODE_ERROR_ADDING_DISCOVERY_CONFIG, e, getOrganizationId());
        }
    }

    @Override
    public DiscoveryConfig getDiscoveryConfiguration() throws OrganizationConfigException {

        Optional<Resource> resourceOptional = getDiscoveryResource();
        if (!resourceOptional.isPresent()) {
            throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST, getOrganizationId());
        }
        List<ConfigProperty> configProperties = resourceOptional.map(resource -> resource.getAttributes().stream()
                .map(attribute -> new ConfigProperty(attribute.getKey(), attribute.getValue()))
                .collect(Collectors.toList())).orElse(Collections.emptyList());
        return new DiscoveryConfig(configProperties);
    }

    @Override
    public void deleteDiscoveryConfiguration() throws OrganizationConfigException {

        try {
            if (!isDiscoveryConfigChangeAllowed()) {
                throw handleClientException(ERROR_CODE_DISCOVERY_CONFIG_UPDATE_NOT_ALLOWED);
            }
            Optional<Resource> resourceOptional = getDiscoveryResource();
            if (resourceOptional.isPresent()) {
                getConfigurationManager().deleteResource(RESOURCE_TYPE_NAME, RESOURCE_NAME);
            }
        } catch (ConfigurationManagementException | OrganizationManagementServerException e) {
            throw handleServerException(ERROR_CODE_ERROR_DELETING_DISCOVERY_CONFIG, e, getOrganizationId());
        }
    }

    private Optional<Resource> getDiscoveryResource() throws OrganizationConfigException {

        try {
            return Optional.ofNullable(getConfigurationManager().getResource(RESOURCE_TYPE_NAME, RESOURCE_NAME));
        } catch (ConfigurationManagementException e) {
            if (!ERROR_CODE_RESOURCE_DOES_NOT_EXISTS.getCode().equals(e.getErrorCode())) {
                throw handleServerException(ERROR_CODE_ERROR_RETRIEVING_DISCOVERY_CONFIG, e, getOrganizationId());
            }
        }
        return Optional.empty();
    }

    private boolean isDiscoveryConfigChangeAllowed() throws OrganizationManagementServerException {

        return getOrganizationManager().isPrimaryOrganization(getOrganizationId());
    }

    private Resource buildResourceFromValidationConfig(DiscoveryConfig discoveryConfig) {

        Map<String, String> configAttributes = new HashMap<>();
        for (ConfigProperty property : discoveryConfig.getConfigProperties()) {
            configAttributes.put(property.getKey(), property.getValue());
        }
        List<Attribute> resourceAttributes = configAttributes.entrySet().stream()
                .filter(attribute -> attribute.getValue() != null && !"null".equals(attribute.getValue()))
                .map(attribute -> new Attribute(attribute.getKey(), attribute.getValue()))
                .collect(Collectors.toList());
        Resource resource = new Resource();
        resource.setResourceName(RESOURCE_NAME);
        resource.setAttributes(resourceAttributes);
        return resource;
    }

    private ConfigurationManager getConfigurationManager() {

        return OrganizationConfigServiceHolder.getInstance().getConfigurationManager();
    }

    private OrganizationManager getOrganizationManager() {

        return OrganizationConfigServiceHolder.getInstance().getOrganizationManager();
    }
}
