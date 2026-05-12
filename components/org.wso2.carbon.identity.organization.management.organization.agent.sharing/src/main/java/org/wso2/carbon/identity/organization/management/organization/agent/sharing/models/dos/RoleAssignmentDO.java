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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos;

import org.wso2.carbon.identity.organization.management.organization.agent.sharing.constant.RoleAssignmentMode;

import java.util.List;

/**
 * Model that contains the role assignment block for agent sharing.
 */
public class RoleAssignmentDO {

    private RoleAssignmentMode mode;
    private List<RoleWithAudienceDO> roles;

    public RoleAssignmentMode getMode() {

        return mode;
    }

    public void setMode(RoleAssignmentMode mode) {

        this.mode = mode;
    }

    public List<RoleWithAudienceDO> getRoles() {

        return roles;
    }

    public void setRoles(List<RoleWithAudienceDO> roles) {

        this.roles = roles;
    }
}
