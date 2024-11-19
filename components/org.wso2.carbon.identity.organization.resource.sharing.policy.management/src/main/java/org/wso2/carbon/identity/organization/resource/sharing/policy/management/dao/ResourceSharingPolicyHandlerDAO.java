/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.dao;

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;

/**
 * DAO interface for handling user sharing policies.
 */
public interface ResourceSharingPolicyHandlerDAO {

    /**
     * Creates a record of a resource sharing policy.
     *
     * @param resource               The resource being shared.
     * @param resourceType           The type of the resource.
     * @param initiatedOrganization  The organization initiating the sharing.
     * @param policyHoldingOrganization The organization holding the policy for the shared resource.
     * @param policy                 The sharing policy.
     * @throws OrganizationManagementServerException If an error occurs while creating the sharing policy record.
     */
    void createResourceSharingPolicyRecord(String resource, String resourceType, String initiatedOrganization,
                                           String policyHoldingOrganization, String policy)
            throws OrganizationManagementServerException;

    /**
     * Deletes a resource sharing policy record.
     *
     * @param resource               The resource being shared.
     * @param resourceType           The type of the resource.
     * @param initiatedOrganization  The organization initiating the sharing.
     * @param policyHoldingOrganization The organization holding the policy for the shared resource.
     * @return True if the record is deleted successfully.
     * @throws OrganizationManagementServerException If an error occurs while deleting the sharing policy record.
     */
    boolean deleteResourceSharingPolicyRecord(String resource, String resourceType, String initiatedOrganization,
                                              String policyHoldingOrganization)
            throws OrganizationManagementServerException;
}
