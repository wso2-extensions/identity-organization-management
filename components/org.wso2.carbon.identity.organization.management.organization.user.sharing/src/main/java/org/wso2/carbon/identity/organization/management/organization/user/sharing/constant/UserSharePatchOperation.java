/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum representing the types of user share role assignment operations. Applicable to role assignment updates.
 */
public enum UserSharePatchOperation {

    ADD("add"),
    REMOVE("remove");

    private static final String VALID_OPERATIONS =
            Arrays.stream(values()).map(UserSharePatchOperation::getValue).collect(Collectors.joining(", "));

    private final String value;

    UserSharePatchOperation(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }

    /**
     * Resolve {@link UserSharePatchOperation} from a string value.
     *
     * @param value Operation value (e.g. {@code add}, {@code remove})
     * @return Matching {@link UserSharePatchOperation}
     * @throws IllegalArgumentException if the value is null, blank, or invalid
     */
    public static UserSharePatchOperation fromValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Operation value cannot be null or empty. Valid operations are: " + VALID_OPERATIONS);
        }

        for (UserSharePatchOperation operation : values()) {
            if (operation.value.equalsIgnoreCase(value.trim())) {
                return operation;
            }
        }

        throw new IllegalArgumentException(
                "Invalid operation: " + value + ". Valid operations are: " + VALID_OPERATIONS);
    }
}
