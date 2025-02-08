/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attribute based organization discovery handler registry. This class holds the supported discovery attribute keys.
 */
public class AttributeBasedOrganizationDiscoveryHandlerRegistry {

    private static final AttributeBasedOrganizationDiscoveryHandlerRegistry instance = new
            AttributeBasedOrganizationDiscoveryHandlerRegistry();
    private static final String ENABLED_KEY = ".enable";

    private final List<String> supportedDiscoveryAttributeKeys = new ArrayList<>();

    private AttributeBasedOrganizationDiscoveryHandlerRegistry() {

    }

    /**
     * Get the instance of the registry.
     *
     * @return Instance of the registry.
     */
    public static AttributeBasedOrganizationDiscoveryHandlerRegistry getInstance() {

        return instance;
    }

    /**
     * Get the supported discovery attribute key list
     *
     * @return Supported discovery attribute keys.
     */
    public List<String> getSupportedDiscoveryAttributeKeys() {

        return Collections.unmodifiableList(supportedDiscoveryAttributeKeys);
    }

    /**
     * Add a supported discovery attribute key.
     *
     * @param key Supported discovery attribute key.
     */
    public void addSupportedDiscoveryAttributeKey(String key) {

        supportedDiscoveryAttributeKeys.add(key + ENABLED_KEY);
    }
}
