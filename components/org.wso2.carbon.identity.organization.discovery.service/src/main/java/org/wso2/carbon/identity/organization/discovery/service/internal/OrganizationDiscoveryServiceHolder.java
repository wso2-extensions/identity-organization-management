/*
 * Copyright (c) 2023-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.organization.config.service.AttributeBasedOrganizationDiscoveryHandlerRegistry;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.discovery.service.AttributeBasedOrganizationDiscoveryHandler;
import org.wso2.carbon.identity.organization.discovery.service.OrganizationDiscoveryManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Organization discovery data holder.
 */
public class OrganizationDiscoveryServiceHolder {

    private static final OrganizationDiscoveryServiceHolder instance = new OrganizationDiscoveryServiceHolder();
    private ApplicationManagementService applicationManagementService;
    private OrganizationManager organizationManager = null;
    private OrganizationConfigManager organizationConfigManager = null;
    private OrganizationDiscoveryManager organizationDiscoveryManager;
    private Map<String, AttributeBasedOrganizationDiscoveryHandler> attributeBasedOrganizationDiscoveryHandlerMap;

    private OrganizationDiscoveryServiceHolder() {
        
    }
    
    public static OrganizationDiscoveryServiceHolder getInstance() {

        return instance;
    }

    public OrganizationManager getOrganizationManager() {

        return organizationManager;
    }

    public void setOrganizationManager(OrganizationManager organizationManager) {

        this.organizationManager = organizationManager;
    }

    public OrganizationConfigManager getOrganizationConfigManager() {

        return organizationConfigManager;
    }

    public void setOrganizationConfigManager(OrganizationConfigManager organizationConfigManager) {

        this.organizationConfigManager = organizationConfigManager;
    }

    public AttributeBasedOrganizationDiscoveryHandler getAttributeBasedOrganizationDiscoveryHandler(String type) {

        if (attributeBasedOrganizationDiscoveryHandlerMap == null) {
            return null;
        }
        return attributeBasedOrganizationDiscoveryHandlerMap.get(type);
    }

    public Map<String, AttributeBasedOrganizationDiscoveryHandler> getAttributeBasedOrganizationDiscoveryHandlers() {

        return attributeBasedOrganizationDiscoveryHandlerMap;
    }

    public Set<String> getDiscoveryTypes() {

        return attributeBasedOrganizationDiscoveryHandlerMap.keySet();
    }

    public void setAttributeBasedOrganizationDiscoveryHandler(AttributeBasedOrganizationDiscoveryHandler
                                                                      attributeBasedOrganizationDiscoveryHandler) {

        if (attributeBasedOrganizationDiscoveryHandlerMap == null) {
            attributeBasedOrganizationDiscoveryHandlerMap = new HashMap<>();
        }
        attributeBasedOrganizationDiscoveryHandlerMap.put(attributeBasedOrganizationDiscoveryHandler.getType(),
                attributeBasedOrganizationDiscoveryHandler);
        // Register the supported discovery attribute key in config service.
        AttributeBasedOrganizationDiscoveryHandlerRegistry.getInstance()
                .addSupportedDiscoveryAttributeKey(attributeBasedOrganizationDiscoveryHandler.getType());
    }

    public void unbindAttributeBasedOrganizationDiscoveryHandler(AttributeBasedOrganizationDiscoveryHandler
                                                                         attributeBasedOrganizationDiscoveryHandler) {

        attributeBasedOrganizationDiscoveryHandlerMap.remove(attributeBasedOrganizationDiscoveryHandler.getType());
    }

    public OrganizationDiscoveryManager getOrganizationDiscoveryManager() {

        return organizationDiscoveryManager;
    }

    public void setOrganizationDiscoveryManager(OrganizationDiscoveryManager organizationDiscoveryManager) {

        this.organizationDiscoveryManager = organizationDiscoveryManager;
    }

    public ApplicationManagementService getApplicationManagementService() {

        return applicationManagementService;
    }

    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        this.applicationManagementService = applicationManagementService;
    }
}
