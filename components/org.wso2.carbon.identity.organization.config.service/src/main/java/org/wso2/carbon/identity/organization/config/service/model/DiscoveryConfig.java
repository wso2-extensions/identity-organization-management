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

package org.wso2.carbon.identity.organization.config.service.model;

import java.util.List;

/**
 * This class represents the organization discovery configuration.
 */
public class DiscoveryConfig {

    private List<ConfigProperty> configProperties;

    public DiscoveryConfig(List<ConfigProperty> configProperties) {

        this.configProperties = configProperties;
    }

    public List<ConfigProperty> getConfigProperties() {

        return configProperties;
    }

    public void setConfigProperties(List<ConfigProperty> configProperties) {

        this.configProperties = configProperties;
    }
}
