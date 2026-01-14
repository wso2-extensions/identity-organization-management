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

import org.wso2.carbon.context.PrivilegedCarbonContext;

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

    private SharingInitiatorContext(String sharingInitiatedUserId, String sharingInitiatedUsername,
                                    int sharingInitiatedTenantId, String sharingInitiatedTenantDomain,
                                    String sharingInitiatedOrgId) {

        this.sharingInitiatedUserId = sharingInitiatedUserId;
        this.sharingInitiatedUsername = sharingInitiatedUsername;
        this.sharingInitiatedTenantId = sharingInitiatedTenantId;
        this.sharingInitiatedTenantDomain = sharingInitiatedTenantDomain;
        this.sharingInitiatedOrgId = sharingInitiatedOrgId;
    }

    public static SharingInitiatorContext capture() {

        PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        return new SharingInitiatorContext(ctx.getUserId(), ctx.getUsername(), ctx.getTenantId(),
                ctx.getTenantDomain(), getOrganizationId());
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
}
