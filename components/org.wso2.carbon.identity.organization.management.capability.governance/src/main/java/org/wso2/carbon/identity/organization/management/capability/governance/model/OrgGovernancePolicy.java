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

    /**
     * Returns the database record ID of this governance policy.
     *
     * @return the policy ID
     */
    public int getId() {

        return id;
    }

    /**
     * Sets the database record ID of this governance policy.
     *
     * @param id the policy ID
     */
    public void setId(int id) {

        this.id = id;
    }

    /**
     * Returns the resource type this policy governs (e.g., an application type).
     *
     * @return the resource type string
     */
    public String getResourceType() {

        return resourceType;
    }

    /**
     * Sets the resource type this policy governs.
     *
     * @param resourceType the resource type string
     */
    public void setResourceType(String resourceType) {

        this.resourceType = resourceType;
    }

    /**
     * Returns the capability identifier this policy controls within the resource type.
     *
     * @return the capability string
     */
    public String getCapability() {

        return capability;
    }

    /**
     * Sets the capability identifier this policy controls.
     *
     * @param capability the capability string
     */
    public void setCapability(String capability) {

        this.capability = capability;
    }

    /**
     * Returns the ID of the organization that owns and enforces this policy.
     *
     * @return the governing organization ID
     */
    public String getGoverningOrgId() {

        return governingOrgId;
    }

    /**
     * Sets the ID of the organization that owns and enforces this policy.
     *
     * @param governingOrgId the governing organization ID
     */
    public void setGoverningOrgId(String governingOrgId) {

        this.governingOrgId = governingOrgId;
    }

    /**
     * Returns the {@link Policy} mode that determines which child organizations this policy covers.
     *
     * @return the policy mode
     */
    public Policy getPolicy() {

        return policy;
    }

    /**
     * Sets the {@link Policy} mode that determines which child organizations this policy covers.
     *
     * @param policy the policy mode
     */
    public void setPolicy(Policy policy) {

        this.policy = policy;
    }

    /**
     * Returns the list of organizations explicitly selected under a {@link Policy#ALLOW_SELECTED} policy.
     *
     * @return list of selected organizations; empty when the policy is not {@link Policy#ALLOW_SELECTED}
     */
    public List<GovernanceOrgSelected> getSelectedOrgs() {

        return selectedOrgs;
    }

    /**
     * Sets the list of organizations explicitly selected under a {@link Policy#ALLOW_SELECTED} policy.
     *
     * @param selectedOrgs list of selected organizations
     */
    public void setSelectedOrgs(List<GovernanceOrgSelected> selectedOrgs) {

        this.selectedOrgs = selectedOrgs;
    }

    /**
     * Returns {@code true} if this policy covers (applies to) the given organization.
     *
     * <p>"Covers" means the policy is applicable to the target org — it does not imply the
     * capability is allowed. Callers must separately check {@link Policy#DENY_ALL} to determine
     * the final access decision.
     *
     * <ul>
     *   <li>{@link Policy#ALLOW_ALL} — covers every child organization; returns {@code true}.</li>
     *   <li>{@link Policy#DENY_ALL} — covers every child organization (capability is denied);
     *       returns {@code true}. Callers must recognize this case and block access accordingly.</li>
     *   <li>{@link Policy#ALLOW_IMMEDIATE} — covers only direct children of the governing org;
     *       returns {@code true} only when {@code isDirectChild} is {@code true}.</li>
     *   <li>{@link Policy#ALLOW_SELECTED} — covers only the organizations explicitly listed in
     *       {@link #getSelectedOrgs()}; returns {@code true} when {@code targetOrgId} matches
     *       any entry in that list.</li>
     * </ul>
     *
     * @param targetOrgId  the ID of the organization being evaluated
     * @param isDirectChild {@code true} if the target org is a direct child of the governing org
     * @return {@code true} if this policy applies to the target organization, {@code false} otherwise
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
