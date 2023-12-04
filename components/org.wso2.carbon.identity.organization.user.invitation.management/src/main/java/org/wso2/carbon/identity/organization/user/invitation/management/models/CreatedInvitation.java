package org.wso2.carbon.identity.organization.user.invitation.management.models;

/**
 * Model that contains the created invitation details.
 */
public class CreatedInvitation {

    private String username;
    private InvitationResult result;

    public InvitationResult getResult() {

        return result;
    }

    public void setResult(InvitationResult result) {

        this.result = result;
    }

    public String getUsername() {

        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }
}
