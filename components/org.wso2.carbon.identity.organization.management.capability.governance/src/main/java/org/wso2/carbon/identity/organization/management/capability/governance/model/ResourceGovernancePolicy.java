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
 * Model representing a resource-level governance policy.
 */
public class ResourceGovernancePolicy {

    private int id;
    private String resourceType;
    private String resourceId;
    private String resourceOwnerOrgId;
    private String governingOrgId;
    private String capability;
    private PolicyType policyType;
    private boolean allowOverride;
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

    public String getResourceId() {

        return resourceId;
    }

    public void setResourceId(String resourceId) {

        this.resourceId = resourceId;
    }

    public String getResourceOwnerOrgId() {

        return resourceOwnerOrgId;
    }

    public void setResourceOwnerOrgId(String resourceOwnerOrgId) {

        this.resourceOwnerOrgId = resourceOwnerOrgId;
    }

    public String getGoverningOrgId() {

        return governingOrgId;
    }

    public void setGoverningOrgId(String governingOrgId) {

        this.governingOrgId = governingOrgId;
    }

    public String getCapability() {

        return capability;
    }

    public void setCapability(String capability) {

        this.capability = capability;
    }

    public PolicyType getPolicyType() {

        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {

        this.policyType = policyType;
    }

    public boolean isAllowOverride() {

        return allowOverride;
    }

    public void setAllowOverride(boolean allowOverride) {

        this.allowOverride = allowOverride;
    }

    public List<GovernanceOrgSelected> getSelectedOrgs() {

        return selectedOrgs;
    }

    public void setSelectedOrgs(List<GovernanceOrgSelected> selectedOrgs) {

        this.selectedOrgs = selectedOrgs;
    }
}
