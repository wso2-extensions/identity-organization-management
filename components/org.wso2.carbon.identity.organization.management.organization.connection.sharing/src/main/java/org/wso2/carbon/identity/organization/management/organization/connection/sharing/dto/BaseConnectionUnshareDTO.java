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

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant.ConnectionType;

/**
 * Abstract base DTO for connection unshare operations. An unshare operation targets a single connection
 * (identified by {@code connectionId}) of a single {@link ConnectionType}.
 */
public abstract class BaseConnectionUnshareDTO {

    private String connectionId;
    private ConnectionType connectionType;

    /**
     * Returns the ID of the connection being unshared.
     *
     * @return the connection resource ID
     */
    public String getConnectionId() {

        return connectionId;
    }

    /**
     * Sets the ID of the connection being unshared.
     *
     * @param connectionId the connection resource ID
     */
    public void setConnectionId(String connectionId) {

        this.connectionId = connectionId;
    }

    /**
     * Returns the type of the connection being unshared.
     *
     * @return the {@link ConnectionType}
     */
    public ConnectionType getConnectionType() {

        return connectionType;
    }

    /**
     * Sets the type of the connection being unshared.
     *
     * @param connectionType the {@link ConnectionType}
     */
    public void setConnectionType(ConnectionType connectionType) {

        this.connectionType = connectionType;
    }
}
