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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;

/**
 * Test cases for OrganizationSharedUserUtil functions.
 */
public class OrganizationSharedUserUtilTest {

    @Mock
    IdentityEventListenerConfig identityEventListenerConfig;

    @BeforeClass
    public void init() {

        openMocks(this);
    }

    @DataProvider(name = "dataProviderForIsSharedUserProfileResolverEnabled")
    public Object[][] dataProviderForIsSharedUserProfileResolverEnabled() {

        return new Object[][]{
                {identityEventListenerConfig, String.valueOf(false), false},
                {identityEventListenerConfig, String.valueOf(true), true},
                {identityEventListenerConfig, StringUtils.EMPTY, true},
                {null, String.valueOf(true), true},
        };
    }

    @Test(dataProvider = "dataProviderForIsSharedUserProfileResolverEnabled")
    public void testIsSharedUserProfileResolverEnabled(IdentityEventListenerConfig identityEventListenerConfig,
                                                       String isListenerEnabled, boolean expectedResult) {

        try (MockedStatic<IdentityUtil> identityUtil = Mockito.mockStatic(IdentityUtil.class)) {
            identityUtil.when(() -> IdentityUtil.readEventListenerProperty(anyString(), anyString()))
                    .thenReturn(identityEventListenerConfig);
            if (identityEventListenerConfig != null) {
                when(identityEventListenerConfig.getEnable()).thenReturn(isListenerEnabled);
            }
            assertEquals(OrganizationSharedUserUtil.isSharedUserProfileResolverEnabled(), expectedResult);
        }
    }
}
