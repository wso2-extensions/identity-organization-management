package org.wso2.carbon.identity.organization.user.invitation.management.models;

import java.util.List;

/**
 * Model that contains the invitation data object.
 */
public class InvitationDO {

    private List<String> usernamesList;
    private String userDomain;
    private RoleAssignments[] roleAssignments;
    private String userRedirectUrl;

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
}
