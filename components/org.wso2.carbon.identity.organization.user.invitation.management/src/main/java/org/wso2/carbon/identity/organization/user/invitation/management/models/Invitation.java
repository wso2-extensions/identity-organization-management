/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

import java.sql.Timestamp;
import java.util.List;

/**
 * Model that contains the invitation related details.
 */
public class Invitation {

    private String invitationId;
    private String confirmationCode;
    private String username;
    private List<String> usernamesList;
    private String userDomain;
    private String email;
    private String userOrganizationId;
    private String userOrganizationName;
    private String invitedOrganizationId;
    private String invitedOrganizationName;
    private String status;
    private Timestamp createdAt;
    private Timestamp expiredAt;
    private String userRedirectUrl;
    private RoleAssignments[] roleAssignments;
    private List<String> skippedUsersList;

    public String getInvitationId() {

        return invitationId;
    }

    public void setInvitationId(String invitationId) {

        this.invitationId = invitationId;
    }

    public String getConfirmationCode() {

        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {

        this.confirmationCode = confirmationCode;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public List<String> getUsernamesList() {

        return usernamesList;
    }

    public void setUsernamesList(List<String> usernames) {

        this.usernamesList = usernames;
    }

    public String getUserDomain() {

        return userDomain;
    }

    public void setUserDomain(String userDomain) {

        this.userDomain = userDomain;
    }

    public String getEmail() {

        return email;
    }

    public void setEmail(String email) {

        this.email = email;
    }

    public String getUserOrganizationId() {

        return userOrganizationId;
    }

    public void setUserOrganizationId(String userOrganizationId) {

        this.userOrganizationId = userOrganizationId;
    }

    public String getUserOrganizationName() {

        return userOrganizationName;
    }

    public void setUserOrganizationName(String userOrganizationName) {

        this.userOrganizationName = userOrganizationName;
    }

    public String getInvitedOrganizationId() {

        return invitedOrganizationId;
    }

    public void setInvitedOrganizationId(String invitedOrganizationId) {

        this.invitedOrganizationId = invitedOrganizationId;
    }

    public String getInvitedOrganizationName() {

        return invitedOrganizationName;
    }

    public void setInvitedOrganizationName(String invitedOrganizationName) {

        this.invitedOrganizationName = invitedOrganizationName;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public Timestamp getCreatedAt() {

        return (Timestamp) createdAt.clone();
    }

    public void setCreatedAt(Timestamp createdAt) {

        this.createdAt = createdAt != null ? new Timestamp(createdAt.getTime()) : null;
    }

    public Timestamp getExpiredAt() {

        return (Timestamp) expiredAt.clone();
    }

    public void setExpiredAt(Timestamp expiredAt) {

        this.expiredAt = expiredAt != null ? new Timestamp(expiredAt.getTime()) : null;
    }

    public String getUserRedirectUrl() {

        return userRedirectUrl;
    }

    public void setUserRedirectUrl(String userRedirectUrl) {

        this.userRedirectUrl = userRedirectUrl;
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

    public List<String> getSkippedUsersList() {

        return skippedUsersList;
    }

    public void setSkippedUsersList(List<String> usernames) {

        this.skippedUsersList = usernames;
    }
}
