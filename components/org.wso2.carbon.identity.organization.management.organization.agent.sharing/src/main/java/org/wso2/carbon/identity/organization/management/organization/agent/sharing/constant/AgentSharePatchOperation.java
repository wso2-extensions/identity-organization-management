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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum representing the types of agent share role assignment operations. Applicable to role assignment updates.
 */
public enum AgentSharePatchOperation {

    ADD("add"),
    REMOVE("remove");

    private static final String VALID_OPERATIONS =
            Arrays.stream(values()).map(AgentSharePatchOperation::getValue).collect(Collectors.joining(", "));

    private final String value;

    AgentSharePatchOperation(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }

    /**
     * Resolve {@link AgentSharePatchOperation} from a string value.
     *
     * @param value Operation value (e.g. {@code add}, {@code remove})
     * @return Matching {@link AgentSharePatchOperation}
     * @throws IllegalArgumentException if the value is null, blank, or invalid
     */
    public static AgentSharePatchOperation fromValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Operation value cannot be null or empty. Valid operations are: " + VALID_OPERATIONS);
        }

        for (AgentSharePatchOperation operation : values()) {
            if (operation.value.equalsIgnoreCase(value.trim())) {
                return operation;
            }
        }

        throw new IllegalArgumentException(
                "Invalid operation: " + value.trim() + ". Valid operations are: " + VALID_OPERATIONS);
    }
}
