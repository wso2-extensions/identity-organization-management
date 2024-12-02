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

import java.util.Collections;
import java.util.List;

/**
 * Enum representing the type of resource being shared.
 */
public enum ResourceType {
    USER(Collections.singletonList(SharedAttributeType.ROLE));

    private final List<SharedAttributeType> applicableAttributes;

    ResourceType(List<SharedAttributeType> applicableAttributes) {
        this.applicableAttributes = applicableAttributes;
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
}
