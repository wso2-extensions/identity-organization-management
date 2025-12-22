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
 * Enum representing the modes of role assignments.
 */
public enum RoleAssignmentMode {
    SELECTED("SELECTED"),
    NONE("NONE");

    private static final String VALID_MODES =
            Arrays.stream(values()).map(RoleAssignmentMode::toString).collect(Collectors.joining(", "));

    private final String value;

    RoleAssignmentMode(String value) {

        this.value = value;
    }

    /**
     * Returns the exact value as stored in the database.
     *
     * @return The database value of the enum.
     */
    @Override
    public String toString() {

        return value;
    }

    /**
     * Custom method to get the enum from a string value, handling cases.
     *
     * @param stringValueOfRoleAssignmentMode The string value of RoleAssignmentMode.
     * @return The corresponding RoleAssignmentMode enum.
     * @throws IllegalArgumentException if the value is null/blank or does not match any enum.
     */
    public static RoleAssignmentMode fromString(String stringValueOfRoleAssignmentMode) {

        if (stringValueOfRoleAssignmentMode == null || stringValueOfRoleAssignmentMode.trim().isEmpty()) {
            throw new IllegalArgumentException("RoleAssignmentMode value cannot be null or empty. Valid modes are: "
                    + VALID_MODES);
        }

        for (RoleAssignmentMode mode : values()) {
            if (mode.value.equalsIgnoreCase(stringValueOfRoleAssignmentMode.trim())) {
                return mode;
            }
        }

        throw new IllegalArgumentException(
                "Invalid RoleAssignmentMode value: " + stringValueOfRoleAssignmentMode + ". Valid modes are: " +
                        VALID_MODES);
    }
}
