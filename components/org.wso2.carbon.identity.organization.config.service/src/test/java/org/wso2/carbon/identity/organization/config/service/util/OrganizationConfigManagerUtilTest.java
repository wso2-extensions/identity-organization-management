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

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigException;
import org.wso2.carbon.identity.organization.config.service.internal.OrganizationConfigServiceHolder;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.DEFAULT_PARAM;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.EMAIL_DOMAIN_ENABLE;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ErrorMessages.ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ORG_HANDLE;
import static org.wso2.carbon.identity.organization.config.service.constant.OrganizationConfigConstants.ORG_PARAMETER;

/**
 * This test class provides unit tests for organization configuration management utility functions.
 */
public class OrganizationConfigManagerUtilTest {

    @Mock
    OrganizationConfigManager mockOrganizationConfigManager;
    @Mock
    DiscoveryConfig mockDiscoveryConfig;

    @BeforeMethod
    void setup() {

        MockitoAnnotations.openMocks(this);
        OrganizationConfigServiceHolder.getInstance().setOrganizationConfigManager(mockOrganizationConfigManager);
    }

    @DataProvider(name = "defaultDiscoveryParamDataProvider")
    public Object[][] defaultDiscoveryParamDataProvider() {

        ConfigProperty defaultDiscoveryProperty = new ConfigProperty(DEFAULT_PARAM, ORG_HANDLE);
        ConfigProperty unrelatedProperty = new ConfigProperty(EMAIL_DOMAIN_ENABLE, "true");

        return new Object[][]{
                {Collections.singletonList(defaultDiscoveryProperty), ORG_HANDLE},
                {Collections.singletonList(unrelatedProperty), ORG_PARAMETER},
                {Collections.EMPTY_LIST, ORG_PARAMETER}
        };
    }

    @Test(dataProvider = "defaultDiscoveryParamDataProvider")
    void testResolveDefaultDiscoveryParam(List<ConfigProperty> configProperties, String expectedParam)
            throws Exception {

        when(mockDiscoveryConfig.getConfigProperties()).thenReturn(configProperties);
        when(mockOrganizationConfigManager.getDiscoveryConfiguration()).thenReturn(mockDiscoveryConfig);

        String defaultDiscoveryParam = OrganizationConfigManagerUtil.resolveDefaultDiscoveryParam();
        assertEquals(expectedParam, defaultDiscoveryParam);
    }

    @Test
    void testResolveDefaultDiscoveryParamWhenConfigNotExist() throws Exception {

        OrganizationConfigException configException = new OrganizationConfigException(
                        ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getCode(),
                        ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getMessage(),
                        ERROR_CODE_DISCOVERY_CONFIG_NOT_EXIST.getDescription());

        when(mockOrganizationConfigManager.getDiscoveryConfiguration()).thenThrow(configException);
        String defaultDiscoveryParam = OrganizationConfigManagerUtil.resolveDefaultDiscoveryParam();
        assertEquals(ORG_PARAMETER, defaultDiscoveryParam);
    }
}
