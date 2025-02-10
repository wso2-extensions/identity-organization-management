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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;

/**
 * Model that contains the shared organizations details of a user in the response object.
 */
public class ResponseOrgDetailsDO {

    private String organizationId;
    private String organizationName;
    private String sharedUserId;
    private SharedType sharedType;
    private String rolesRef;

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getOrganizationName() {

        return organizationName;
    }

    public void setOrganizationName(String organizationName) {

        this.organizationName = organizationName;
    }

    public String getSharedUserId() {

        return sharedUserId;
    }

    public void setSharedUserId(String sharedUserId) {

        this.sharedUserId = sharedUserId;
    }

    public SharedType getSharedType() {

        return sharedType;
    }

    public void setSharedType(SharedType sharedType) {

        this.sharedType = sharedType;
    }

    public String getRolesRef() {

        return rolesRef;
    }

    public void setRolesRef(String rolesRef) {

        this.rolesRef = rolesRef;
    }
}
