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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for the per-thread flow markers maintained by {@link ConnectionSharingUtil}. These markers coordinate
 * the share / unshare / sync flows and the per-update "what changed" flags across the listener and the sharing
 * process on the same thread.
 */
public class ConnectionSharingUtilTest {

    @AfterMethod
    public void tearDown() {

        // Clear every marker so state cannot leak across tests running on the same thread.
        ConnectionSharingUtil.endConnectionShareFlow();
        ConnectionSharingUtil.endConnectionUnshareFlow();
        ConnectionSharingUtil.endSharedConnectionSyncFlow();
        ConnectionSharingUtil.consumeConnectionNameUpdated();
        ConnectionSharingUtil.consumeConnectionGroupsUpdated();
        ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated();
        ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated();
    }

    @Test
    public void testConnectionShareFlow() {

        Assert.assertFalse(ConnectionSharingUtil.isConnectionShareFlow());
        ConnectionSharingUtil.startConnectionShareFlow();
        Assert.assertTrue(ConnectionSharingUtil.isConnectionShareFlow());
        ConnectionSharingUtil.endConnectionShareFlow();
        Assert.assertFalse(ConnectionSharingUtil.isConnectionShareFlow());
    }

    @Test
    public void testConnectionUnshareFlow() {

        Assert.assertFalse(ConnectionSharingUtil.isConnectionUnshareFlow());
        ConnectionSharingUtil.startConnectionUnshareFlow();
        Assert.assertTrue(ConnectionSharingUtil.isConnectionUnshareFlow());
        ConnectionSharingUtil.endConnectionUnshareFlow();
        Assert.assertFalse(ConnectionSharingUtil.isConnectionUnshareFlow());
    }

    @Test
    public void testSharedConnectionSyncFlow() {

        Assert.assertFalse(ConnectionSharingUtil.isSharedConnectionSyncFlow());
        ConnectionSharingUtil.startSharedConnectionSyncFlow();
        Assert.assertTrue(ConnectionSharingUtil.isSharedConnectionSyncFlow());
        ConnectionSharingUtil.endSharedConnectionSyncFlow();
        Assert.assertFalse(ConnectionSharingUtil.isSharedConnectionSyncFlow());
    }

    @Test
    public void testFlowsAreIndependent() {

        ConnectionSharingUtil.startConnectionShareFlow();
        Assert.assertTrue(ConnectionSharingUtil.isConnectionShareFlow());
        Assert.assertFalse(ConnectionSharingUtil.isConnectionUnshareFlow());
        Assert.assertFalse(ConnectionSharingUtil.isSharedConnectionSyncFlow());
    }

    @Test
    public void testConsumeConnectionNameUpdated() {

        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionNameUpdated());

        ConnectionSharingUtil.setIsConnectionNameUpdating(true);
        // Consuming reports the recorded change and clears the marker.
        Assert.assertTrue(ConnectionSharingUtil.consumeConnectionNameUpdated());
        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionNameUpdated());

        // Setting explicitly to false clears any prior marker.
        ConnectionSharingUtil.setIsConnectionNameUpdating(true);
        ConnectionSharingUtil.setIsConnectionNameUpdating(false);
        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionNameUpdated());
    }

    @Test
    public void testConsumeConnectionGroupsUpdated() {

        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionGroupsUpdated());
        ConnectionSharingUtil.setIsConnectionGroupsUpdating(true);
        Assert.assertTrue(ConnectionSharingUtil.consumeConnectionGroupsUpdated());
        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionGroupsUpdated());
    }

    @Test
    public void testConsumeConnectionAuthenticatorsUpdated() {

        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated());
        ConnectionSharingUtil.setIsConnectionAuthenticatorsUpdating(true);
        Assert.assertTrue(ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated());
        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionAuthenticatorsUpdated());
    }

    @Test
    public void testConsumeConnectionProvisioningConnectorsUpdated() {

        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated());
        ConnectionSharingUtil.setIsConnectionProvisioningConnectorsUpdating(true);
        Assert.assertTrue(ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated());
        Assert.assertFalse(ConnectionSharingUtil.consumeConnectionProvisioningConnectorsUpdated());
    }
}
