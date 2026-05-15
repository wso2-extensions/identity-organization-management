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

package org.wso2.carbon.identity.organization.management.capability.governance.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing an org-level governance policy.
 */
public class OrgGovernancePolicy {

    private int id;
    private String resourceType;
    private String capability;
    private String governingOrgId;
    private Policy policy;
    private List<GovernanceOrgSelected> selectedOrgs = new ArrayList<>();

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public String getResourceType() {

        return resourceType;
    }

    public void setResourceType(String resourceType) {

        this.resourceType = resourceType;
    }

    public String getCapability() {

        return capability;
    }

    public void setCapability(String capability) {

        this.capability = capability;
    }

    public String getGoverningOrgId() {

        return governingOrgId;
    }

    public void setGoverningOrgId(String governingOrgId) {

        this.governingOrgId = governingOrgId;
    }

    public Policy getPolicy() {

        return policy;
    }

    public void setPolicy(Policy policy) {

        this.policy = policy;
    }

    public List<GovernanceOrgSelected> getSelectedOrgs() {

        return selectedOrgs;
    }

    public void setSelectedOrgs(List<GovernanceOrgSelected> selectedOrgs) {

        this.selectedOrgs = selectedOrgs;
    }

    /**
     * Returns true if this policy covers (applies to) the given org.
     * "Covers" means the policy is applicable — it does not imply the capability is allowed.
     * Callers should check {@link Policy#DENY_ALL} separately to determine the final access decision.
     */
    public boolean coversOrg(String targetOrgId, boolean isDirectChild) {

        switch (policy) {
            case ALLOW_ALL:
            case DENY_ALL:
                return true;
            case ALLOW_IMMEDIATE:
                return isDirectChild;
            case ALLOW_SELECTED:
                return selectedOrgs.stream().anyMatch(s -> targetOrgId.equals(s.getTargetOrgId()));
            default:
                return false;
        }
    }
}
