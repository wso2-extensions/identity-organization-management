/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception;

/**
 * Exception class for resource sharing policy management for client exceptions.
 */
public class ResourceSharingPolicyMgtClientException extends ResourceSharingPolicyMgtException {

    public ResourceSharingPolicyMgtClientException(String message, String errorCode, String description) {

        super(message, errorCode, description);
    }

    public ResourceSharingPolicyMgtClientException(String errorCode, String description) {

        super(errorCode, description);
    }

    public ResourceSharingPolicyMgtClientException(Throwable cause, String errorCode, String description) {

        super(cause, errorCode, description);
    }

    public ResourceSharingPolicyMgtClientException(String message, Throwable cause, String errorCode,
                                                   String description) {

        super(message, cause, errorCode, description);
    }

    public ResourceSharingPolicyMgtClientException(String message, Throwable cause, boolean enableSuppression,
                                                   boolean writableStackTrace, String errorCode, String description) {

        super(message, cause, enableSuppression, writableStackTrace, errorCode, description);
    }
}
