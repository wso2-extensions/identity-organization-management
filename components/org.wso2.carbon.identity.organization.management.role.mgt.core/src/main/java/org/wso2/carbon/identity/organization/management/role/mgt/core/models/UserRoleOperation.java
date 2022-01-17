/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.role.mgt.core.models;

/**
 * User Role Operation Implementation.
 */
public class UserRoleOperation {
    private String path;
    private boolean value;
    private String op;

    public UserRoleOperation(String op, String path) {
        this.op = op;
        this.path = path;
    }

    public UserRoleOperation(String op, String path, boolean value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public String getOp() {
        return op;
    }

}
