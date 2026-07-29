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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

/**
 * Abstract base DTO for connection share operations. A share operation targets a single connection
 * (identified by {@code connectionId}) of a single {@link ResourceType}.
 */
public abstract class BaseConnectionShareDTO {

    private String connectionId;
    private ResourceType resourceType;

    /**
     * Returns the ID of the connection being shared.
     *
     * @return the connection resource ID
     */
    public String getConnectionId() {

        return connectionId;
    }

    /**
     * Sets the ID of the connection being shared.
     *
     * @param connectionId the connection resource ID
     */
    public void setConnectionId(String connectionId) {

        this.connectionId = connectionId;
    }

    /**
     * Returns the type of the connection being shared.
     *
     * @return the {@link ResourceType}
     */
    public ResourceType getResourceType() {

        return resourceType;
    }

    /**
     * Sets the type of the connection being shared.
     *
     * @param resourceType the {@link ResourceType}
     */
    public void setResourceType(ResourceType resourceType) {

        this.resourceType = resourceType;
    }
}
