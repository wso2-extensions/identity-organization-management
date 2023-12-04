package org.wso2.carbon.identity.organization.user.invitation.management.models;

import org.wso2.carbon.identity.organization.user.invitation.management.constant.UserInvitationMgtConstants;

/**
 * Model that contains the created invitation result details.
 */
public class InvitationResult {

    private UserInvitationMgtConstants.ErrorMessage errorMsg;
    private String status;

    public UserInvitationMgtConstants.ErrorMessage getErrorMsg() {

        return errorMsg;
    }

    public void setErrorMsg(UserInvitationMgtConstants.ErrorMessage errorMsg) {

        this.errorMsg = errorMsg;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }
}
