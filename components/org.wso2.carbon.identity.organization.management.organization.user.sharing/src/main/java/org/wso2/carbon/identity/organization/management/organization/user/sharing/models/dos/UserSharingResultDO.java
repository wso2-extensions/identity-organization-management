/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import org.wso2.carbon.identity.framework.async.operation.status.mgt.api.constants.OperationStatus;

/**
 * Model that contains the user sharing result.
 */
public class UserSharingResultDO {
    private String operationId;
    private String associatedUserId;
    private boolean isUserSharedSuccess;
    private boolean isUserRoleAssignedIfPresentSuccess;
    private OperationStatus operationStatus;
    private String operationStatusMessage;

    public UserSharingResultDO(String operationId, String associatedUserId, boolean isUserSharedSuccess,
                               boolean isUserRoleAssignedIfPresentSuccess,
                               OperationStatus operationStatus, String operationStatusMessage) {

        this.operationId = operationId;
        this.associatedUserId = associatedUserId;
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

    public OperationStatus getOperationStatus() {

        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {

        this.operationStatus = operationStatus;
    }

    public String getOperationStatusMessage() {

        return operationStatusMessage;
    }

    public void setOperationStatusMessage(String operationStatusMessage) {

        this.operationStatusMessage = operationStatusMessage;
    }

    public String getAssociatedUserId() {

        return associatedUserId;
    }

    public void setAssociatedUserId(String associatedUserId) {

        this.associatedUserId = associatedUserId;
    }
}
