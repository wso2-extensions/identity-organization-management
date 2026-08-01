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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.utils.AuditLog;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.constant.ConnectionSharingConstants.AUDIT_FAILURE;
import static org.wso2.carbon.identity.organization.management.organization.connection.sharing.api.constant.ConnectionSharingConstants.AUDIT_SUCCESS;

/**
 * Publishes v2 audit logs (via {@link LoggerUtils#triggerAuditLogEvent}) for connection sharing operations. The
 * audit target is the connection being shared/unshared. Shared by the orchestration service (policy operations)
 * and the connection type handlers (per-organization resource operations), so resource operations are audited
 * uniformly regardless of the caller.
 */
public class ConnectionSharingAuditLogger {

    private static final String TARGET_TYPE_CONNECTION = "Connection";
    private static final String DATA_RESOURCE_TYPE = "ResourceType";
    private static final String DATA_RESIDENT_ORG_ID = "ResidentOrganizationId";
    private static final String DATA_SHARED_ORG_ID = "SharedOrganizationId";
    private static final String DATA_SHARED_RESOURCE_ID = "SharedResourceId";
    private static final String DATA_RESULT = "Result";
    private static final String DATA_REASON = "Reason";

    /**
     * Connection sharing audit log actions.
     */
    private enum Action {

        SHARE_CONNECTION("share-connection"),
        UNSHARE_CONNECTION("unshare-connection");

        private final String logAction;

        Action(String logAction) {

            this.logAction = logAction;
        }

        private String value() {

            return logAction;
        }
    }

    private ConnectionSharingAuditLogger() {

    }

    /**
     * Logs a successful share of a connection to an organization.
     *
     * @param resourceType     The connection's resource type (e.g. {@code IDP}).
     * @param connectionId     The connection (target) ID.
     * @param residentOrgId    The organization that owns the connection.
     * @param sharedOrgId      The organization the connection was shared with.
     * @param sharedResourceId The ID of the shadow resource created in the shared organization.
     */
    public static void logConnectionShared(String resourceType, String connectionId, String residentOrgId,
                                           String sharedOrgId, String sharedResourceId) {

        log(Action.SHARE_CONNECTION, resourceType, connectionId, residentOrgId, sharedOrgId, sharedResourceId, true);
    }

    /**
     * Logs a failed attempt to share a connection to an organization.
     *
     * @param resourceType  The connection's resource type (e.g. {@code IDP}).
     * @param connectionId  The connection (target) ID.
     * @param residentOrgId The organization that owns the connection.
     * @param sharedOrgId   The organization the connection was being shared with.
     */
    public static void logConnectionShareFailure(String resourceType, String connectionId, String residentOrgId,
                                                 String sharedOrgId, String reason) {

        log(Action.SHARE_CONNECTION, resourceType, connectionId, residentOrgId, sharedOrgId, null, false,
                reason);
    }

    /**
     * Logs a successful unshare of a connection from an organization.
     *
     * @param resourceType     The connection's resource type (e.g. {@code IDP}).
     * @param connectionId     The connection (target) ID.
     * @param residentOrgId    The organization that owns the connection.
     * @param sharedOrgId      The organization the connection was unshared from.
     * @param sharedResourceId The ID of the shadow resource removed from the organization.
     */
    public static void logConnectionUnshared(String resourceType, String connectionId, String residentOrgId,
                                             String sharedOrgId, String sharedResourceId) {

        log(Action.UNSHARE_CONNECTION, resourceType, connectionId, residentOrgId, sharedOrgId, sharedResourceId, true);
    }

    /**
     * Logs a failed attempt to unshare a connection from an organization.
     *
     * @param resourceType     The connection's resource type (e.g. {@code IDP}).
     * @param connectionId     The connection (target) ID.
     * @param residentOrgId    The organization that owns the connection.
     * @param sharedOrgId      The organization the connection was being unshared from.
     * @param sharedResourceId The ID of the shadow resource being removed (if known).
     */
    public static void logConnectionUnshareFailure(String resourceType, String connectionId, String residentOrgId,
                                                   String sharedOrgId, String sharedResourceId) {

        log(Action.UNSHARE_CONNECTION, resourceType, connectionId, residentOrgId, sharedOrgId, sharedResourceId,
                false);
    }

    /**
     * Logs an unshare of a connection that was blocked (e.g. because the shared resource still has connected
     * applications). The shadow resource and its connection association are left intact.
     *
     * @param resourceType     The connection's resource type (e.g. {@code IDP}).
     * @param connectionId     The connection (target) ID.
     * @param residentOrgId    The organization that owns the connection.
     * @param sharedOrgId      The organization the connection was being unshared from.
     * @param sharedResourceId The ID of the shadow resource that could not be removed.
     * @param reason           The reason the unshare was blocked.
     */
    public static void logConnectionUnshareBlocked(String resourceType, String connectionId, String residentOrgId,
                                                   String sharedOrgId, String sharedResourceId, String reason) {

        log(Action.UNSHARE_CONNECTION, resourceType, connectionId, residentOrgId, sharedOrgId, sharedResourceId,
                false, reason);
    }

    private static void log(Action action, String resourceType, String connectionId, String residentOrgId,
                            String sharedOrgId, String sharedResourceId, boolean success) {

        log(action, resourceType, connectionId, residentOrgId, sharedOrgId, sharedResourceId, success, null);
    }

    private static void log(Action action, String resourceType, String connectionId, String residentOrgId,
                            String sharedOrgId, String sharedResourceId, boolean success, String reason) {

        String initiatorId = getInitiatorId();
        Map<String, Object> data = new HashMap<>();
        putIfNotBlank(data, DATA_RESOURCE_TYPE, resourceType);
        putIfNotBlank(data, DATA_RESIDENT_ORG_ID, residentOrgId);
        putIfNotBlank(data, DATA_SHARED_ORG_ID, sharedOrgId);
        putIfNotBlank(data, DATA_SHARED_RESOURCE_ID, sharedResourceId);
        putIfNotBlank(data, DATA_REASON, reason);
        data.put(DATA_RESULT, success ? AUDIT_SUCCESS : AUDIT_FAILURE);

        AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                initiatorId,
                LoggerUtils.getInitiatorType(initiatorId),
                connectionId,
                TARGET_TYPE_CONNECTION,
                action.value())
                .data(data);
        LoggerUtils.triggerAuditLogEvent(auditLogBuilder);
    }

    private static void putIfNotBlank(Map<String, Object> data, String key, String value) {

        if (StringUtils.isNotBlank(value)) {
            data.put(key, value);
        }
    }

    private static String getInitiatorId() {

        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (StringUtils.isBlank(username)) {
            return LoggerUtils.Initiator.System.name();
        }
        String initiator = null;
        if (StringUtils.isNotBlank(tenantDomain)) {
            initiator = IdentityUtil.getInitiatorId(username, tenantDomain);
        }
        return StringUtils.isNotBlank(initiator) ? initiator : LoggerUtils.getMaskedContent(username);
    }
}
