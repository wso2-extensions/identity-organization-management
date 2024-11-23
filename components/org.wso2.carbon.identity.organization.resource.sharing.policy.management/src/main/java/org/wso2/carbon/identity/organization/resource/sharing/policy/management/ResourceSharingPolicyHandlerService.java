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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management;

import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.ResourceSharingPolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.models.SharedResourceAttributes;

/**
 * Service that manages the core service for managing resource sharing policies.
 */
public interface ResourceSharingPolicyHandlerService {

    /**
     * Saves the resource sharing policy to the data store and return the record id.
     */
    int addResourceSharingPolicy(ResourceSharingPolicy resourceSharingPolicy)
            throws OrganizationManagementServerException, ResourceSharingPolicyMgtServerException, DataAccessException;

    /**
     * Saves the shared resource attributes to the data store.
     */
    void addSharedResourceAttributes(SharedResourceAttributes sharedResourceAttributes)
            throws ResourceSharingPolicyMgtServerException;

}
