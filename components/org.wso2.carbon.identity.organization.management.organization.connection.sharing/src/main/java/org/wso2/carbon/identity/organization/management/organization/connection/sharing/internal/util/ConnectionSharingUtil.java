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

import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for connection sharing management.
 */
public class ConnectionSharingUtil {

    /**
     * Thread-local property map used to carry connection-sharing flags across a single processing thread. It lets
     * resource-management listeners (e.g. the identity provider management listener) distinguish operations driven
     * by the sharing process from direct operations (e.g. via the REST API).
     */
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_PROPERTIES =
            ThreadLocal.withInitial(HashMap::new);

    // Property key: set while the sharing process is creating a shadow connection.
    private static final String CONNECTION_SHARE_FLOW = "connectionShareFlow";

    // Property key: set while the unsharing process is deleting a shadow connection.
    private static final String CONNECTION_UNSHARE_FLOW = "connectionUnshareFlow";

    // Property key: set while propagating a parent connection's change (name and/or idp groups) to its shadow
    // connections.
    private static final String SHARED_CONNECTION_SYNC_FLOW = "sharedConnectionSyncFlow";

    // Property key: set by the pre-update listener when a (parent) connection's name is being changed, so the
    // post-update listener knows to propagate the new name to its shadow connections.
    private static final String CONNECTION_NAME_UPDATING = "connectionNameUpdating";

    // Property key: set by the pre-update listener when a (parent) connection's idp groups are being changed, so the
    // post-update listener knows to propagate the new groups to its shadow connections.
    private static final String CONNECTION_GROUPS_UPDATING = "connectionGroupsUpdating";

    // Property key: set by the pre-update listener when a (parent) connection's federated authenticators are being
    // structurally changed (an authenticator added or removed, or the default authenticator changed), so the
    // post-update listener knows to propagate the change to its shadow connections.
    private static final String CONNECTION_AUTHENTICATORS_UPDATING = "connectionAuthenticatorsUpdating";

    // Property key: set by the pre-update listener when a (parent) connection's provisioning connectors are being
    // structurally changed (a connector added or removed, or the default connector changed), so the post-update
    // listener knows to propagate the change to its shadow connections.
    private static final String CONNECTION_PROVISIONING_CONNECTORS_UPDATING =
            "connectionProvisioningConnectorsUpdating";

    private ConnectionSharingUtil() {

    }

    /**
     * Creates a new {@link NamedJdbcTemplate} backed by the identity datasource.
     *
     * @return A new {@link NamedJdbcTemplate}.
     */
    public static NamedJdbcTemplate getNewTemplate() {

        return new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
    }

    /**
     * Marks the current thread as executing the shadow-connection creation step of the sharing process. Must be
     * paired with {@link #endConnectionShareFlow()} in a {@code finally} block.
     */
    public static void startConnectionShareFlow() {

        THREAD_LOCAL_PROPERTIES.get().put(CONNECTION_SHARE_FLOW, Boolean.TRUE);
    }

    /**
     * Clears the shadow-connection creation marker for the current thread.
     */
    public static void endConnectionShareFlow() {

        THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_SHARE_FLOW);
    }

    /**
     * Returns whether the current thread is executing the shadow-connection creation step of the sharing process.
     *
     * @return {@code true} if a shadow connection is being created by the sharing process.
     */
    public static boolean isConnectionShareFlow() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().get(CONNECTION_SHARE_FLOW));
    }

    /**
     * Marks the current thread as executing the shadow-connection deletion step of the unsharing process. Must be
     * paired with {@link #endConnectionUnshareFlow()} in a {@code finally} block.
     */
    public static void startConnectionUnshareFlow() {

        THREAD_LOCAL_PROPERTIES.get().put(CONNECTION_UNSHARE_FLOW, Boolean.TRUE);
    }

    /**
     * Clears the shadow-connection deletion marker for the current thread.
     */
    public static void endConnectionUnshareFlow() {

        THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_UNSHARE_FLOW);
    }

    /**
     * Returns whether the current thread is executing the shadow-connection deletion step of the unsharing process.
     *
     * @return {@code true} if a shadow connection is being deleted by the unsharing process.
     */
    public static boolean isConnectionUnshareFlow() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().get(CONNECTION_UNSHARE_FLOW));
    }

    /**
     * Marks the current thread as propagating a parent connection's change (name and/or idp groups) to a shadow
     * connection. While set, the identity provider management listener skips the update deny guard so the internal
     * propagation update is allowed (the shadow itself is fetched raw, so no parent-derived values are round-tripped
     * into its row). Must be paired with {@link #endSharedConnectionSyncFlow()} in a {@code finally} block.
     */
    public static void startSharedConnectionSyncFlow() {

        THREAD_LOCAL_PROPERTIES.get().put(SHARED_CONNECTION_SYNC_FLOW, Boolean.TRUE);
    }

    /**
     * Clears the shadow-connection sync marker for the current thread.
     */
    public static void endSharedConnectionSyncFlow() {

        THREAD_LOCAL_PROPERTIES.get().remove(SHARED_CONNECTION_SYNC_FLOW);
    }

    /**
     * Returns whether the current thread is propagating a parent connection's change to a shadow connection.
     *
     * @return {@code true} if a shadow connection is being synced from its parent.
     */
    public static boolean isSharedConnectionSyncFlow() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().get(SHARED_CONNECTION_SYNC_FLOW));
    }

    /**
     * Records (in the pre-update listener) whether a connection's name is being changed, so the post-update
     * listener can decide whether to propagate it to shadow connections. Always set explicitly per update (true or
     * false) so a value cannot leak across operations on a pooled thread.
     *
     * @param isNameUpdating Whether the connection's name is being changed.
     */
    public static void setIsConnectionNameUpdating(boolean isNameUpdating) {

        if (isNameUpdating) {
            THREAD_LOCAL_PROPERTIES.get().put(CONNECTION_NAME_UPDATING, Boolean.TRUE);
        } else {
            THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_NAME_UPDATING);
        }
    }

    /**
     * Returns whether a connection name change was recorded by the pre-update listener, clearing the marker.
     *
     * @return {@code true} if the connection's name was changed in this update.
     */
    public static boolean consumeConnectionNameUpdated() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_NAME_UPDATING));
    }

    /**
     * Records (in the pre-update listener) whether a connection's idp groups are being changed, so the post-update
     * listener can decide whether to propagate them to shadow connections. Always set explicitly per update (true or
     * false) so a value cannot leak across operations on a pooled thread.
     *
     * @param isGroupsUpdating Whether the connection's idp groups are being changed.
     */
    public static void setIsConnectionGroupsUpdating(boolean isGroupsUpdating) {

        if (isGroupsUpdating) {
            THREAD_LOCAL_PROPERTIES.get().put(CONNECTION_GROUPS_UPDATING, Boolean.TRUE);
        } else {
            THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_GROUPS_UPDATING);
        }
    }

    /**
     * Returns whether a connection idp groups change was recorded by the pre-update listener, clearing the marker.
     *
     * @return {@code true} if the connection's idp groups were changed in this update.
     */
    public static boolean consumeConnectionGroupsUpdated() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_GROUPS_UPDATING));
    }

    /**
     * Records (in the pre-update listener) whether a connection's federated authenticators are being structurally
     * changed, so the post-update listener can decide whether to propagate the change to shadow connections. Always
     * set explicitly per update (true or false) so a value cannot leak across operations on a pooled thread.
     *
     * @param isAuthenticatorsUpdating Whether the connection's federated authenticators are being structurally
     *                                 changed.
     */
    public static void setIsConnectionAuthenticatorsUpdating(boolean isAuthenticatorsUpdating) {

        if (isAuthenticatorsUpdating) {
            THREAD_LOCAL_PROPERTIES.get().put(CONNECTION_AUTHENTICATORS_UPDATING, Boolean.TRUE);
        } else {
            THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_AUTHENTICATORS_UPDATING);
        }
    }

    /**
     * Returns whether a connection federated authenticators change was recorded by the pre-update listener, clearing
     * the marker.
     *
     * @return {@code true} if the connection's federated authenticators were structurally changed in this update.
     */
    public static boolean consumeConnectionAuthenticatorsUpdated() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_AUTHENTICATORS_UPDATING));
    }

    /**
     * Records (in the pre-update listener) whether a connection's provisioning connectors are being structurally
     * changed, so the post-update listener can decide whether to propagate the change to shadow connections. Always
     * set explicitly per update (true or false) so a value cannot leak across operations on a pooled thread.
     *
     * @param isConnectorsUpdating Whether the connection's provisioning connectors are being structurally changed.
     */
    public static void setIsConnectionProvisioningConnectorsUpdating(boolean isConnectorsUpdating) {

        if (isConnectorsUpdating) {
            THREAD_LOCAL_PROPERTIES.get().put(CONNECTION_PROVISIONING_CONNECTORS_UPDATING, Boolean.TRUE);
        } else {
            THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_PROVISIONING_CONNECTORS_UPDATING);
        }
    }

    /**
     * Returns whether a connection provisioning connectors change was recorded by the pre-update listener, clearing
     * the marker.
     *
     * @return {@code true} if the connection's provisioning connectors were structurally changed in this update.
     */
    public static boolean consumeConnectionProvisioningConnectorsUpdated() {

        return Boolean.TRUE.equals(THREAD_LOCAL_PROPERTIES.get().remove(CONNECTION_PROVISIONING_CONNECTORS_UPDATING));
    }
}
