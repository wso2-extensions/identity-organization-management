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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.util;

import org.wso2.carbon.context.PrivilegedCarbonContext;

import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Captures the thread-local Carbon context of the sharing initiator before async dispatch.
 * Mirrors the pattern of SharingInitiatorContext in the user sharing component,
 * without introducing a cross-component dependency.
 */
public class ConnectionSharingInitiatorContext {

    private final String sharingInitiatedUserId;
    private final String sharingInitiatedUsername;
    private final int sharingInitiatedTenantId;
    private final String sharingInitiatedTenantDomain;
    private final String sharingInitiatedOrgId;

    private ConnectionSharingInitiatorContext(String sharingInitiatedUserId, String sharingInitiatedUsername,
                                              int sharingInitiatedTenantId, String sharingInitiatedTenantDomain,
                                              String sharingInitiatedOrgId) {

        this.sharingInitiatedUserId = sharingInitiatedUserId;
        this.sharingInitiatedUsername = sharingInitiatedUsername;
        this.sharingInitiatedTenantId = sharingInitiatedTenantId;
        this.sharingInitiatedTenantDomain = sharingInitiatedTenantDomain;
        this.sharingInitiatedOrgId = sharingInitiatedOrgId;
    }

    /**
     * Captures the current thread-local {@link PrivilegedCarbonContext} values (userId, username, tenantId,
     * tenantDomain, and organizationId) and returns a {@link ConnectionSharingInitiatorContext} populated from
     * those values. This method must be called on the initiating thread before async dispatch, as
     * thread-local state is not propagated to worker threads.
     *
     * @return a {@link ConnectionSharingInitiatorContext} snapshot of the current Carbon context.
     */
    public static ConnectionSharingInitiatorContext capture() {

        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        return new ConnectionSharingInitiatorContext(ctx.getUserId(), ctx.getUsername(), ctx.getTenantId(),
                ctx.getTenantDomain(), getOrganizationId());
    }

    /**
     * Returns the user ID of the sharing initiator captured at context creation time.
     *
     * @return the sharing-initiated user ID, or {@code null} if not set in the captured context.
     */
    public String getSharingInitiatedUserId() {

        return sharingInitiatedUserId;
    }

    /**
     * Returns the username of the sharing initiator captured at context creation time.
     *
     * @return the sharing-initiated username, or {@code null} if not set in the captured context.
     */
    public String getSharingInitiatedUsername() {

        return sharingInitiatedUsername;
    }

    /**
     * Returns the tenant ID of the sharing initiator captured at context creation time.
     *
     * @return the sharing-initiated tenant ID.
     */
    public int getSharingInitiatedTenantId() {

        return sharingInitiatedTenantId;
    }

    /**
     * Returns the tenant domain of the sharing initiator captured at context creation time.
     *
     * @return the sharing-initiated tenant domain, or {@code null} if not set in the captured context.
     */
    public String getSharingInitiatedTenantDomain() {

        return sharingInitiatedTenantDomain;
    }

    /**
     * Returns the organization ID of the sharing initiator captured at context creation time.
     *
     * @return the sharing-initiated organization ID, or {@code null} if not set in the captured context.
     */
    public String getSharingInitiatedOrgId() {

        return sharingInitiatedOrgId;
    }
}
