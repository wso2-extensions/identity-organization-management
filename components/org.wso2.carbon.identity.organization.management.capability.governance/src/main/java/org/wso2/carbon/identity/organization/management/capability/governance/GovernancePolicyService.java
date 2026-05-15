/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.capability.governance;

import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtException;
import org.wso2.carbon.identity.organization.management.capability.governance.model.OrgGovernancePolicy;

import java.util.List;

/**
 * OSGi service interface for managing organization capability governance policies.
 */
public interface GovernancePolicyService {

    /**
     * Creates a new organization governance policy.
     *
     * @param policy the policy to create.
     * @return the created policy with its generated ID.
     * @throws GovernancePolicyMgtException if the policy already exists or if a server error occurs.
     */
    OrgGovernancePolicy addOrgGovernancePolicy(OrgGovernancePolicy policy) throws GovernancePolicyMgtException;

    /**
     * Returns the governance policy matching the given composite key.
     *
     * @param governingOrgId the governing organization ID.
     * @param resourceType the resource type.
     * @param capability the capability name.
     * @return the matching policy.
     * @throws GovernancePolicyMgtException if no policy is found for the given key, or if a server error occurs.
     */
    OrgGovernancePolicy getOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability)
            throws GovernancePolicyMgtException;

    /**
     * Returns all governance policies for the given governing organization.
     *
     * @param governingOrgId the governing organization ID.
     * @return list of policies; empty if none exist.
     * @throws GovernancePolicyMgtException if a server error occurs.
     */
    List<OrgGovernancePolicy> getOrgGovernancePolicies(String governingOrgId) throws GovernancePolicyMgtException;

    /**
     * Updates the governance policy matching the given composite key.
     *
     * @param governingOrgId the governing organization ID.
     * @param resourceType the resource type.
     * @param capability the capability name.
     * @param updates the fields to apply; non-null fields overwrite existing values.
     * @return the updated policy.
     * @throws GovernancePolicyMgtException if no policy is found for the given key, or if a server error occurs.
     */
    OrgGovernancePolicy updateOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability,
            OrgGovernancePolicy updates) throws GovernancePolicyMgtException;

    /**
     * Deletes the governance policy matching the given composite key.
     *
     * @param governingOrgId the governing organization ID.
     * @param resourceType the resource type.
     * @param capability the capability name.
     * @throws GovernancePolicyMgtException if no policy is found for the given key, or if a server error occurs.
     */
    void deleteOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability)
            throws GovernancePolicyMgtException;
}
