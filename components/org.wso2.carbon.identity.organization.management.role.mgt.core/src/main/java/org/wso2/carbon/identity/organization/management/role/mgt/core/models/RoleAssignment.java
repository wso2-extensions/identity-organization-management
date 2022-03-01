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

import java.util.Map;

/**
 * Role inheritance and assigned level details.
 */
public class RoleAssignment {
    //Map of organization id and organization name.
    private Map<String, String> assignedAt;
    private boolean isForced;

    public RoleAssignment (boolean isForced, Map<String, String> assignedAt) {

        this.isForced = isForced;
        this.assignedAt = assignedAt;
    }

    public Map<String, String> getAssignedAt() {

        return assignedAt;
    }

    public void setAssignedAt(Map<String, String> assignedAt) {

        this.assignedAt = assignedAt;
    }

    public boolean isForced() {

        return isForced;
    }

    public void setForced(boolean forced) {

        isForced = forced;
    }
}
