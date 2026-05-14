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
import org.wso2.carbon.identity.organization.management.capability.governance.model.ResourceGovernancePolicy;

import java.util.List;
import java.util.Optional;

/**
 * DAO interface for managing organization capability governance policies.
 */
public interface GovernancePolicyDAO {

    // -------------------------------------------------------------------------
    // Org-level CRUD
    // -------------------------------------------------------------------------

    void addOrgGovernancePolicy(OrgGovernancePolicy policy) throws GovernancePolicyMgtServerException;

    List<OrgGovernancePolicy> getOrgGovernancePoliciesByGoverningOrg(String governingOrgId)
            throws GovernancePolicyMgtServerException;

    void updateOrgGovernancePolicy(OrgGovernancePolicy policy) throws GovernancePolicyMgtServerException;

    void deleteOrgGovernancePolicyByKey(String governingOrgId, String resourceType, String capability)
            throws GovernancePolicyMgtServerException;

    // -------------------------------------------------------------------------
    // Resource-level CRUD
    // -------------------------------------------------------------------------

    void addResourceGovernancePolicy(ResourceGovernancePolicy policy) throws GovernancePolicyMgtServerException;

    List<ResourceGovernancePolicy> getResourceGovernancePoliciesByGoverningOrg(String governingOrgId)
            throws GovernancePolicyMgtServerException;

    void updateResourceGovernancePolicy(ResourceGovernancePolicy policy) throws GovernancePolicyMgtServerException;

    void deleteResourceGovernancePolicyByKey(String governingOrgId, String resourceType, String resourceId,
            String capability) throws GovernancePolicyMgtServerException;

    // -------------------------------------------------------------------------
    // Evaluation lookups (by natural key)
    // -------------------------------------------------------------------------

    Optional<OrgGovernancePolicy> findOrgGovernancePolicy(String governingOrgId, String capability,
            String resourceType) throws GovernancePolicyMgtServerException;

    Optional<ResourceGovernancePolicy> findResourceGovernancePolicy(String governingOrgId, String capability,
            String resourceType, String resourceId) throws GovernancePolicyMgtServerException;
}
