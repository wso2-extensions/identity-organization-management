package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

public class UserSharingResultDO {
    private String operationId;
    private boolean isUserSharedSuccess;
    private boolean isUserRoleAssignedIfPresentSuccess;
    private String operationStatus;
    private String operationStatusMessage;

    public UserSharingResultDO(String operationId, boolean isUserSharedSuccess,
                               boolean isUserRoleAssignedIfPresentSuccess,
                               String operationStatus, String operationStatusMessage) {

        this.operationId = operationId;
        this.isUserSharedSuccess = isUserSharedSuccess;
        this.isUserRoleAssignedIfPresentSuccess = isUserRoleAssignedIfPresentSuccess;
        this.operationStatus = operationStatus;
        this.operationStatusMessage = operationStatusMessage;
    }

    public String getOperationId() {

        return operationId;
    }

    public void setOperationId(String operationId) {

        this.operationId = operationId;
    }

    public boolean isUserSharedSuccess() {

        return isUserSharedSuccess;
    }

    public void setUserSharedSuccess(boolean userSharedSuccess) {

        isUserSharedSuccess = userSharedSuccess;
    }

    public boolean isUserRoleAssignedIfPresentSuccess() {

        return isUserRoleAssignedIfPresentSuccess;
    }

    public void setUserRoleAssignedIfPresentSuccess(boolean userRoleAssignedIfPresentSuccess) {

        isUserRoleAssignedIfPresentSuccess = userRoleAssignedIfPresentSuccess;
    }

    public String getOperationStatus() {

        return operationStatus;
    }

    public void setOperationStatus(String operationStatus) {

        this.operationStatus = operationStatus;
    }

    public String getOperationStatusMessage() {

        return operationStatusMessage;
    }

    public void setOperationStatusMessage(String operationStatusMessage) {

        this.operationStatusMessage = operationStatusMessage;
    }
}
