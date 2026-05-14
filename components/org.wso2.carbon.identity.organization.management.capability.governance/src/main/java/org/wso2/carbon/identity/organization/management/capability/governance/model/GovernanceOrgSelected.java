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

/**
 * Model representing a selected org entry in a SELECTED-type governance policy.
 */
public class GovernanceOrgSelected {

    private int id;
    private int policyId;
    private String targetOrgId;
    private boolean allowOverride;

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public int getPolicyId() {

        return policyId;
    }

    public void setPolicyId(int policyId) {

        this.policyId = policyId;
    }

    public String getTargetOrgId() {

        return targetOrgId;
    }

    public void setTargetOrgId(String targetOrgId) {

        this.targetOrgId = targetOrgId;
    }

    public boolean isAllowOverride() {

        return allowOverride;
    }

    public void setAllowOverride(boolean allowOverride) {

        this.allowOverride = allowOverride;
    }
}
