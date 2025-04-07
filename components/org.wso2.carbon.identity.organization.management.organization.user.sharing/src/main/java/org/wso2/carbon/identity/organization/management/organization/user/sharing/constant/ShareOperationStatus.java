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

/**
 * Enum representing the types of asynchronous operation type.
 */
public enum ShareOperationStatus {
    SUCCESS("SUCCESS"),
    FAILED("FAIL"),
    PARTIAL("PARTIAL");

    private final String value;

    ShareOperationStatus(String value) {
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
     * Custom method to get the enum from a string value, handling spaces.
     *
     * @param stringValueOfAsyncOperationStatus The string value of ShareOperationStatus.
     * @return The corresponding ShareOperationStatus enum.
     * @throws IllegalArgumentException if the value does not match any enum.
     */
    public static ShareOperationStatus fromString(String stringValueOfAsyncOperationStatus) {

        for (ShareOperationStatus status : ShareOperationStatus.values()) {
            if (status.value.equalsIgnoreCase(stringValueOfAsyncOperationStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ShareOperationStatus value: " + stringValueOfAsyncOperationStatus);
    }
}
