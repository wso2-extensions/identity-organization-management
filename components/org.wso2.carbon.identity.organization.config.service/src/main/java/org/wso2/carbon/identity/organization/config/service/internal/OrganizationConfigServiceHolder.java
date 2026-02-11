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

package org.wso2.carbon.identity.organization.config.service.internal;

import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

/**
 * Organization configuration data holder.
 */
public class OrganizationConfigServiceHolder {

    private static final OrganizationConfigServiceHolder instance = new OrganizationConfigServiceHolder();
    private ConfigurationManager configurationManager = null;
    private OrganizationManager organizationManager = null;
    private OrganizationConfigManager organizationConfigManager = null;

    private OrganizationConfigServiceHolder() {
        
    }
    
    public static OrganizationConfigServiceHolder getInstance() {

        return instance;
    }

    public ConfigurationManager getConfigurationManager() {

        return configurationManager;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {

        this.configurationManager = configurationManager;
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
}
