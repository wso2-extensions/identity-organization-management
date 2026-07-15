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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.constant;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum representing the type of connection being shared. A connection is an umbrella over several distinct
 * resource types, each of which maps to its own {@link ResourceType} for sharing policy persistence and is
 * handled by a dedicated
 * {@link org.wso2.carbon.identity.organization.management.organization.connection.sharing.ConnectionTypeHandler}.
 */
public enum ConnectionType {

    IDP(ResourceType.IDP),
    IDENTITY_VERIFICATION_PROVIDER(ResourceType.IDENTITY_VERIFICATION_PROVIDER),
    CUSTOM_AUTHENTICATOR(ResourceType.CUSTOM_AUTHENTICATOR),
    FLOW_EXTENSION(ResourceType.FLOW_EXTENSION);

    private static final String VALID_CONNECTION_TYPES =
            Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));

    private final ResourceType resourceType;

    ConnectionType(ResourceType resourceType) {

        this.resourceType = resourceType;
    }

    /**
     * Returns the {@link ResourceType} under which this connection type's sharing policies are persisted.
     *
     * @return The mapped {@link ResourceType}.
     */
    public ResourceType getResourceType() {

        return resourceType;
    }

    /**
     * Resolve a {@link ConnectionType} from a string value.
     *
     * @param value Connection type value.
     * @return Matching {@link ConnectionType}.
     * @throws IllegalArgumentException If the value is null, blank, or invalid.
     */
    public static ConnectionType fromString(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "ConnectionType value cannot be null or empty. Valid values are: " + VALID_CONNECTION_TYPES);
        }

        for (ConnectionType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid ConnectionType value: " + value.trim() + ". Valid values are: " + VALID_CONNECTION_TYPES);
    }
}
