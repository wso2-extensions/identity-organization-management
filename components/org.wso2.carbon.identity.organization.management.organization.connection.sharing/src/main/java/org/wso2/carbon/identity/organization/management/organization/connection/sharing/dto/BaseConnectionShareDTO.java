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

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.connectioncriteria.ConnectionCriteriaType;

import java.util.Map;

/**
 * Abstract base DTO for connection share operations.
 * The {@code connectionCriteria} map keys correspond to criteria type names
 * (e.g., {@code "CONNECTION_IDS"}, {@code "CONNECTION_NAMES"}).
 *
 * @param <T> the connection criteria type
 */
public abstract class BaseConnectionShareDTO<T extends ConnectionCriteriaType> {

    private Map<String, T> connectionCriteria;

    /**
     * Returns the connection criteria map used to identify connections for sharing.
     *
     * @return map keyed by criteria type name (e.g., {@code "CONNECTION_IDS"}, {@code "CONNECTION_NAMES"})
     *         with values of type {@code T}
     */
    public Map<String, T> getConnectionCriteria() {

        return connectionCriteria;
    }

    /**
     * Sets the connection criteria map used to identify connections for sharing.
     *
     * @param connectionCriteria map keyed by criteria type name (e.g., {@code "CONNECTION_IDS"},
     *                           {@code "CONNECTION_NAMES"}) with values of type {@code T}
     */
    public void setConnectionCriteria(Map<String, T> connectionCriteria) {

        this.connectionCriteria = connectionCriteria;
    }
}
