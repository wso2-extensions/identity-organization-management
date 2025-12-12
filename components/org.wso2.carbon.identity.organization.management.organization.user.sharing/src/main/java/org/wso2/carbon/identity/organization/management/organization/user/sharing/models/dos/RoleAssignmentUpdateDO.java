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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.RoleAssignmentOperation;

/**
 * This class represents the user share update operation in user-sharing v2.
 */
public class RoleAssignmentUpdateDO {

    private RoleAssignmentOperation operation;
    private String path;
    private Object values;

    public RoleAssignmentUpdateDO(RoleAssignmentOperation operation, String path, Object values) {

        this.operation = operation;
        this.path = path;
        this.values = values;
    }

    public RoleAssignmentOperation getOperation() {

        return operation;
    }

    public void setOperation(
            RoleAssignmentOperation operation) {

        this.operation = operation;
    }

    public String getPath() {

        return path;
    }

    public void setPath(String path) {

        this.path = path;
    }

    public Object getValues() {

        return values;
    }

    public void setValues(Object values) {

        this.values = values;
    }
}
