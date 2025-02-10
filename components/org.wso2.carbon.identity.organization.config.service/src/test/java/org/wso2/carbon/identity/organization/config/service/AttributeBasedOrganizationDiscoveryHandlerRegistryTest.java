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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test class for AttributeBasedOrganizationDiscoveryHandlerRegistry.
 */
public class AttributeBasedOrganizationDiscoveryHandlerRegistryTest {

    private AttributeBasedOrganizationDiscoveryHandlerRegistry registry;

    @BeforeMethod
    public void setUp() {

        registry = AttributeBasedOrganizationDiscoveryHandlerRegistry.getInstance();
    }

    @Test
    public void testGetInstance() {

        Assert.assertNotNull(registry, "Registry instance should not be null");
    }

    @Test
    public void testGetSupportedDiscoveryAttributeKeys() {

        List<String> supportedKeys = registry.getSupportedDiscoveryAttributeKeys();
        Assert.assertNotNull(supportedKeys, "Supported keys list should not be null");
        Assert.assertTrue(supportedKeys.contains("emailDomainBasedSelfSignup.enable"), 
                "Default supported key should be present");
    }

    @Test
    public void testAddSupportedDiscoveryAttributeKey() {

        String newKey = "customAttribute";
        registry.addSupportedDiscoveryAttributeKey(newKey);
        List<String> supportedKeys = registry.getSupportedDiscoveryAttributeKeys();
        Assert.assertTrue(supportedKeys.contains(newKey + ".enable"), 
                "Newly added key should be present in the supported keys list");
    }
}
