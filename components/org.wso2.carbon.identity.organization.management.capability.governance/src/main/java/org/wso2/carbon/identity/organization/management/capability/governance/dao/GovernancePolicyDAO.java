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

package org.wso2.carbon.identity.organization.management.capability.governance.dao;

import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtServerException;
import org.wso2.carbon.identity.organization.management.capability.governance.model.OrgGovernancePolicy;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for managing organization capability governance policies.
 */
public interface GovernancePolicyDAO {

    /**
     * Persists a new organization governance policy.
     *
     * @param policy the policy to persist.
     * @throws GovernancePolicyMgtServerException if the policy could not be persisted.
     */
    void addOrgGovernancePolicy(OrgGovernancePolicy policy) throws GovernancePolicyMgtServerException;

    /**
     * Returns all governance policies for the given governing organization.
     *
     * @param governingOrgId the ID of the governing organization.
     * @return list of policies; empty if none exist.
     * @throws GovernancePolicyMgtServerException if the query fails.
     */
    List<OrgGovernancePolicy> getOrgGovernancePoliciesByGoverningOrg(String governingOrgId)
            throws GovernancePolicyMgtServerException;

    /**
     * Deletes the governance policy matching the given composite key.
     *
     * @param governingOrgId the governing organization ID.
     * @param resourceType the resource type.
     * @param capability the capability name.
     * @throws GovernancePolicyMgtServerException if the delete fails.
     */
    void deleteOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability)
            throws GovernancePolicyMgtServerException;

    /**
     * Finds the governance policy matching the given composite key.
     *
     * @param governingOrgId the governing organization ID.
     * @param capability the capability name.
     * @param resourceType the resource type.
     * @return the matching policy wrapped in an Optional, or empty if not found.
     * @throws GovernancePolicyMgtServerException if the lookup fails.
     */
    Optional<OrgGovernancePolicy> findOrgGovernancePolicy(String governingOrgId, String capability,
            String resourceType) throws GovernancePolicyMgtServerException;
}
