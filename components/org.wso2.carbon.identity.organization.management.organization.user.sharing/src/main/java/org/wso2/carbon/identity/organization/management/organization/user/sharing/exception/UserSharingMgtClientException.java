/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.exception;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants;

/**
 * Exception class for organization user sharing management for client exceptions.
 */
public class UserSharingMgtClientException extends UserSharingMgtException {

    public UserSharingMgtClientException(UserSharingConstants.ErrorMessage error) {

        super(error);
    }

    public UserSharingMgtClientException(UserSharingConstants.ErrorMessage error, Throwable cause) {

        super(error, cause);
    }

    public UserSharingMgtClientException(UserSharingConstants.ErrorMessage error, String specificMessage) {

        super(error, specificMessage);
    }

    public UserSharingMgtClientException(String errorCode, String message, String description) {

        super(errorCode, message, description);
    }

    public UserSharingMgtClientException(String errorCode, String message, String description, Throwable cause) {

        super(errorCode, message, description, cause);
    }

    public UserSharingMgtClientException(String errorCode, String message, String description, Throwable cause,
                                         boolean enableSuppression, boolean writableStackTrace) {

        super(errorCode, message, description, cause, enableSuppression, writableStackTrace);
    }
}
