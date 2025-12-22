/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum representing the type of shared attribute.
 */
public enum SharedAttributeType {
    ROLE;

    private static final String VALID_TYPES =
            Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));

    /**
     * Resolve {@link SharedAttributeType} from a string value.
     *
     * @param value Shared attribute type
     * @return Matching {@link SharedAttributeType}
     * @throws IllegalArgumentException if the value is null, blank, or invalid
     */
    public static SharedAttributeType fromString(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "SharedAttributeType value cannot be null or empty. Valid values are: " + VALID_TYPES);
        }

        for (SharedAttributeType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid SharedAttributeType value: " + value + ". Valid values are: " + VALID_TYPES);
    }
}
