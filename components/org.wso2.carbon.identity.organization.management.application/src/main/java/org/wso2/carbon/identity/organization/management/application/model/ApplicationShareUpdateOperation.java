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

package org.wso2.carbon.identity.organization.management.application.model;

/**
 * This class represents the application share update operation.
 */
public class ApplicationShareUpdateOperation {

    private final Operation operation;
    private final String path;
    private final Object values;

    public ApplicationShareUpdateOperation(Operation operation, String path, Object values) {

        this.operation = operation;
        this.path = path;
        this.values = values;
    }

    public Operation getOperation() {
        return operation;
    }

    public String getPath() {
        return path;
    }

    public Object getValues() {
        return values;
    }

    /**
     * This enum represents the application share update operation.
     */
    public enum Operation {

        ADD("add"),
        REMOVE("remove");

        private final String value;

        Operation(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Operation fromValue(String value) {
            for (Operation operation : Operation.values()) {
                if (operation.getValue().equals(value)) {
                    return operation;
                }
            }
            throw new IllegalArgumentException("Invalid operation: " + value);
        }
    }
}
