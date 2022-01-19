/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
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
 * Role inheritance and assigned level details.
 */
public class RoleAssignment {

    private RoleAssignedLevel assignedAt;
    private boolean isMandatory;

    public RoleAssignment() {
    }

    public RoleAssignment (boolean isMandatory, RoleAssignedLevel assignedAt) {

        this.isMandatory = isMandatory;
        this.assignedAt = assignedAt;
    }

    public RoleAssignedLevel getAssignedAt() {

        return assignedAt;
    }

    public void setAssignedAt(RoleAssignedLevel assignedAt) {

        this.assignedAt = assignedAt;
    }

    public boolean isMandatory() {

        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {

        isMandatory = mandatory;
    }
}
