/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.wso2.com/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.util;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;

/**
 * Utility class for resource sharing util management.
 */
public class ResourceSharingUtils {

    /**
     * Handle server exception by generating a relevant description message.
     *
     * @param error        The ErrorMessage enum containing error details.
     * @param e            The Throwable cause of the exception.
     * @param resourceType The type of the resource (as an enum).
     * @param resourceId   The UUID of the resource.
     * @return A ResourceSharingPolicyMgtServerException.
     */
    public static ResourceSharingPolicyMgtServerException handleServerException(ErrorMessage error, Throwable e,
                                                                                ResourceType resourceType,
                                                                                String resourceId) {

        String description = String.format("%s (Resource Type: %s, Resource ID: %s)",
                error.getDescription(), resourceType.name(), resourceId);

        return new ResourceSharingPolicyMgtServerException(error.getMessage(), e, error.getCode(), description);
    }

    /**
     * Handle server exception for failed shared resource attributes.
     *
     * @param error             The ErrorMessage enum containing error details.
     * @return A ResourceSharingPolicyMgtServerException.
     */
    public static ResourceSharingPolicyMgtServerException handleServerException(ErrorMessage error) {

        return new ResourceSharingPolicyMgtServerException(error.getMessage(), error.getCode());
    }

    private ResourceSharingUtils() {}
}
