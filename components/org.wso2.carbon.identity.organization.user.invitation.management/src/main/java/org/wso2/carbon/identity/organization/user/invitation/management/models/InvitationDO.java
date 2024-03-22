/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.user.invitation.management.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model that contains the invitation data object.
 */
public class InvitationDO {

    private List<String> usernamesList;
    private String userDomain;
    private RoleAssignments[] roleAssignments;
    private GroupAssignments[] groupAssignments;
    private String userRedirectUrl;
    private Map<String, String> invitationProperties = new HashMap();

    public String getUserRedirectUrl() {

        return userRedirectUrl;
    }

    public void setUserRedirectUrl(String userRedirectUrl) {

        this.userRedirectUrl = userRedirectUrl;
    }

    public List<String> getUsernamesList() {

        return usernamesList;
    }

    public void setUsernamesList(List<String> usernamesList) {

        this.usernamesList = usernamesList;
    }

    public String getUserDomain() {

        return userDomain;
    }

    public void setUserDomain(String userDomain) {

        this.userDomain = userDomain;
    }

    public RoleAssignments[] getRoleAssignments() {

        if (roleAssignments == null) {
            return null;
        }
        return roleAssignments.clone();
    }

    public void setRoleAssignments(RoleAssignments[] roleAssignments) {

        this.roleAssignments = roleAssignments != null ? roleAssignments.clone() : null;
    }

    public GroupAssignments[] getGroupAssignments() {

        return (groupAssignments != null) ? groupAssignments.clone() : null;
    }

    public void setGroupAssignments(GroupAssignments[] groupAssignments) {

        this.groupAssignments = groupAssignments != null ? groupAssignments.clone() : null;
    }

    public Map<String, String> getInvitationProperties() {

        return invitationProperties;
    }

    public void setInvitationProperties(Map<String, String> invitationProperties) {

        this.invitationProperties = invitationProperties;
    }
}
