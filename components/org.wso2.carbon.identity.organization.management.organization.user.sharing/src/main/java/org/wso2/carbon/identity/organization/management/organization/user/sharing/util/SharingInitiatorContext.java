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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.slf4j.MDC;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.context.IdentityContext;
import org.wso2.carbon.identity.core.context.model.Flow;
import org.wso2.carbon.identity.core.context.model.Request;
import org.wso2.carbon.identity.core.context.model.RootOrganization;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.CORRELATION_ID_MDC;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getOrganizationId;

/**
 * Public utility class to capture sharing initiator context.
 */
public class SharingInitiatorContext {

    private final String sharingInitiatedUserId;
    private final String sharingInitiatedUsername;
    private final int sharingInitiatedTenantId;
    private final String sharingInitiatedTenantDomain;
    private final String sharingInitiatedOrgId;
    private final Flow flow;
    private final String correlationId;
    private final Request request;
    private final RootOrganization rootOrganization;

    private SharingInitiatorContext(String sharingInitiatedUserId, String sharingInitiatedUsername,
                                    int sharingInitiatedTenantId, String sharingInitiatedTenantDomain,
                                    String sharingInitiatedOrgId, Flow flow, String correlationId,
                                    Request request, RootOrganization rootOrganization) {

        this.sharingInitiatedUserId = sharingInitiatedUserId;
        this.sharingInitiatedUsername = sharingInitiatedUsername;
        this.sharingInitiatedTenantId = sharingInitiatedTenantId;
        this.sharingInitiatedTenantDomain = sharingInitiatedTenantDomain;
        this.sharingInitiatedOrgId = sharingInitiatedOrgId;
        this.flow = flow;
        this.correlationId = correlationId;
        this.request = request;
        this.rootOrganization = rootOrganization;
    }

    public static SharingInitiatorContext capture() {

        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        IdentityContext identityContext = IdentityContext.getThreadLocalIdentityContext();
        return new SharingInitiatorContext(ctx.getUserId(), ctx.getUsername(), ctx.getTenantId(),
                ctx.getTenantDomain(), getOrganizationId(), identityContext.getCurrentFlow(),
                MDC.get(CORRELATION_ID_MDC), identityContext.getRequest(), identityContext.getRootOrganization());
    }

    public String getSharingInitiatedUserId() {

        return sharingInitiatedUserId;
    }

    public String getSharingInitiatedUsername() {

        return sharingInitiatedUsername;
    }

    public int getSharingInitiatedTenantId() {

        return sharingInitiatedTenantId;
    }

    public String getSharingInitiatedTenantDomain() {

        return sharingInitiatedTenantDomain;
    }

    public String getSharingInitiatedOrgId() {

        return sharingInitiatedOrgId;
    }

    /**
     * Get the flow that was active when the sharing was initiated, so that it can be restored on the thread the
     * sharing is processed on.
     *
     * @return The flow, or null if no flow was active.
     */
    public Flow getFlow() {

        return flow;
    }

    /**
     * Get the correlation ID of the request that initiated the sharing, so that the events published while the
     * request is processed asynchronously carry the same correlation ID as the request.
     *
     * @return The correlation ID, or null if it was not set.
     */
    public String getCorrelationId() {

        return correlationId;
    }

    /**
     * Get the request that initiated the sharing, so that the events published while the request is processed
     * asynchronously carry the IP address of the request.
     *
     * @return The request, or null if it was not set.
     */
    public Request getRequest() {

        return request;
    }

    /**
     * Get the root organization of the request that initiated the sharing, so that the events published while the
     * request is processed asynchronously carry the issuer of the request.
     *
     * @return The root organization, or null if it was not set.
     */
    public RootOrganization getRootOrganization() {

        return rootOrganization;
    }
}
