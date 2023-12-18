package org.wso2.carbon.identity.organization.user.invitation.management.models;

/**
 * Model that contains the details for the group assignment.
 */
public class GroupAssignments {

    private String groupId;
    private String displayName;

    public String getGroupId() {

        return groupId;
    }

    public void setGroupId(String groupId) {

        this.groupId = groupId;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }
}
