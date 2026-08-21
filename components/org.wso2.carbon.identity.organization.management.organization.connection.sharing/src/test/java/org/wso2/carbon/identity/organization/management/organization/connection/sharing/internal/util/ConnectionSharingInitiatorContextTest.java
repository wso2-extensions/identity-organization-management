/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.util;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.management.service.util.Utils;

import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectionSharingInitiatorContext}, which snapshots the sharing initiator's Carbon context
 * (user, tenant and organization) so it can be restored on the asynchronous worker thread.
 */
public class ConnectionSharingInitiatorContextTest {

    private static final String USER_ID = "initiator-user-id";
    private static final String USERNAME = "initiator-user";
    private static final int TENANT_ID = 7;
    private static final String TENANT_DOMAIN = "initiator-tenant";
    private static final String ORG_ID = "initiator-org-id";

    private MockedStatic<PrivilegedCarbonContext> mockedCarbonContext;
    private MockedStatic<Utils> mockedUtils;
    private PrivilegedCarbonContext carbonContext;

    @BeforeClass
    public void setUpClass() {

        // Carbon home must be set before PrivilegedCarbonContext is referenced so its static initializer succeeds.
        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());
    }

    @BeforeMethod
    public void setUp() {

        carbonContext = mock(PrivilegedCarbonContext.class);
        when(carbonContext.getUserId()).thenReturn(USER_ID);
        when(carbonContext.getUsername()).thenReturn(USERNAME);
        when(carbonContext.getTenantId()).thenReturn(TENANT_ID);
        when(carbonContext.getTenantDomain()).thenReturn(TENANT_DOMAIN);

        mockedCarbonContext = mockStatic(PrivilegedCarbonContext.class);
        mockedCarbonContext.when(PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);

        mockedUtils = mockStatic(Utils.class);
        mockedUtils.when(Utils::getOrganizationId).thenReturn(ORG_ID);
    }

    @AfterMethod
    public void tearDown() {

        mockedUtils.close();
        mockedCarbonContext.close();
    }

    @Test
    public void testCaptureSnapshotsTheCurrentContext() {

        ConnectionSharingInitiatorContext context = ConnectionSharingInitiatorContext.capture();

        Assert.assertEquals(context.getSharingInitiatedUserId(), USER_ID);
        Assert.assertEquals(context.getSharingInitiatedUsername(), USERNAME);
        Assert.assertEquals(context.getSharingInitiatedTenantId(), TENANT_ID);
        Assert.assertEquals(context.getSharingInitiatedTenantDomain(), TENANT_DOMAIN);
        Assert.assertEquals(context.getSharingInitiatedOrgId(), ORG_ID);
    }

    @Test
    public void testCaptureIsImmutableSnapshot() {

        ConnectionSharingInitiatorContext context = ConnectionSharingInitiatorContext.capture();

        // Changing the underlying context after capture must not affect the already-captured snapshot.
        when(carbonContext.getTenantDomain()).thenReturn("changed-tenant");

        Assert.assertEquals(context.getSharingInitiatedTenantDomain(), TENANT_DOMAIN);
    }
}
