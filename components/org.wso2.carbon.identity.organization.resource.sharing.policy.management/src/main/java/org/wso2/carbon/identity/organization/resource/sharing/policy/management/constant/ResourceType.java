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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum representing the type of resource being shared.
 */
public enum ResourceType {

    USER(Collections.singletonList(SharedAttributeType.ROLE)),
    APPLICATION(Collections.singletonList(SharedAttributeType.ROLE));

    private static final String VALID_RESOURCE_TYPES =
            Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));

    private final List<SharedAttributeType> applicableAttributes;

    ResourceType(List<SharedAttributeType> applicableAttributes) {

        this.applicableAttributes = Collections.unmodifiableList(applicableAttributes);
    }

    /**
     * Checks if the given SharedAttributeType is applicable for this ResourceType.
     *
     * @param attributeType The shared attribute type to be checked.
     * @return True if the attribute type is applicable, false otherwise.
     */
    public boolean isApplicableAttributeType(SharedAttributeType attributeType) {

        return applicableAttributes.contains(attributeType);
    }

    /**
     * Returns the list of applicable shared attribute types for this resource type.
     *
     * @return List of applicable shared attribute types.
     */
    public List<SharedAttributeType> getApplicableAttributes() {

        return applicableAttributes;
    }

    /**
     * Resolve {@link ResourceType} from a string value.
     *
     * @param value Resource type value
     * @return Matching {@link ResourceType}
     * @throws IllegalArgumentException if the value is null, blank, or invalid
     */
    public static ResourceType fromString(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "ResourceType value cannot be null or empty. Valid values are: " + VALID_RESOURCE_TYPES);
        }

        for (ResourceType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid ResourceType value: " + value.trim() + ". Valid values are: " + VALID_RESOURCE_TYPES);
    }
}
