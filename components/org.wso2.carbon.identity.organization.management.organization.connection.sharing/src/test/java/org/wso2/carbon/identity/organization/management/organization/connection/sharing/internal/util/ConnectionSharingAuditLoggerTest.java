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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;

import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectionSharingAuditLogger}, verifying that every share / unshare audit entry point
 * publishes an audit event through {@link LoggerUtils}.
 */
public class ConnectionSharingAuditLoggerTest {

    private static final String RESOURCE_TYPE = "CONNECTION_IDENTITY_PROVIDER";
    private static final String CONNECTION_ID = "connection-id";
    private static final String RESIDENT_ORG_ID = "resident-org-id";
    private static final String SHARED_ORG_ID = "shared-org-id";
    private static final String SHARED_RESOURCE_ID = "shared-resource-id";

    private MockedStatic<LoggerUtils> mockedLoggerUtils;
    private MockedStatic<CarbonContext> mockedCarbonContext;

    @BeforeClass
    public void setUpClass() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());
    }

    @BeforeMethod
    public void setUp() {

        mockedLoggerUtils = mockStatic(LoggerUtils.class);
        mockedCarbonContext = mockStatic(CarbonContext.class);
        // A blank username makes the initiator resolve to "System" without touching IdentityUtil.
        CarbonContext carbonContext = mock(CarbonContext.class);
        when(carbonContext.getUsername()).thenReturn(null);
        mockedCarbonContext.when(CarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
    }

    @AfterMethod
    public void tearDown() {

        mockedLoggerUtils.close();
        mockedCarbonContext.close();
    }

    @Test
    public void testAllAuditEntryPointsPublishAuditEvents() {

        ConnectionSharingAuditLogger.logConnectionShared(RESOURCE_TYPE, CONNECTION_ID, RESIDENT_ORG_ID, SHARED_ORG_ID,
                SHARED_RESOURCE_ID);
        ConnectionSharingAuditLogger.logConnectionShareFailure(RESOURCE_TYPE, CONNECTION_ID, RESIDENT_ORG_ID,
                SHARED_ORG_ID, "reason");
        ConnectionSharingAuditLogger.logConnectionUnshared(RESOURCE_TYPE, CONNECTION_ID, RESIDENT_ORG_ID, SHARED_ORG_ID,
                SHARED_RESOURCE_ID);
        ConnectionSharingAuditLogger.logConnectionUnshareFailure(RESOURCE_TYPE, CONNECTION_ID, RESIDENT_ORG_ID,
                SHARED_ORG_ID, SHARED_RESOURCE_ID);
        ConnectionSharingAuditLogger.logConnectionUnshareBlocked(RESOURCE_TYPE, CONNECTION_ID, RESIDENT_ORG_ID,
                SHARED_ORG_ID, SHARED_RESOURCE_ID, "blocked-reason");

        mockedLoggerUtils.verify(() -> LoggerUtils.triggerAuditLogEvent(any()), times(5));
    }
}
