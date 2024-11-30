/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.model;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;

import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_MISSING_MANDATORY_FIELDS;

/**
 * Model representing the Resource Sharing Policy.
 */
public class ResourceSharingPolicy {

    private int resourceSharingPolicyId;
    private String resourceId;
    private ResourceType resourceType;
    private String initiatingOrgId;
    private String policyHoldingOrgId;
    private PolicyEnum sharingPolicy;

    public int getResourceSharingPolicyId() {

        return resourceSharingPolicyId;
    }

    public void setResourceSharingPolicyId(int resourceSharingPolicyId) {

        this.resourceSharingPolicyId = resourceSharingPolicyId;
    }

    public String getResourceId() {

        return resourceId;
    }

    public void setResourceId(String resourceId) {

        this.resourceId = resourceId;
    }

    public ResourceType getResourceType() {

        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {

        this.resourceType = resourceType;
    }

    public String getInitiatingOrgId() {

        return initiatingOrgId;
    }

    public void setInitiatingOrgId(String initiatingOrgId) {

        this.initiatingOrgId = initiatingOrgId;
    }

    public String getPolicyHoldingOrgId() {

        return policyHoldingOrgId;
    }

    public void setPolicyHoldingOrgId(String policyHoldingOrgId) {

        this.policyHoldingOrgId = policyHoldingOrgId;
    }

    public PolicyEnum getSharingPolicy() {

        return sharingPolicy;
    }

    public void setSharingPolicy(PolicyEnum sharingPolicy) {

        this.sharingPolicy = sharingPolicy;
    }

    @Override
    public String toString() {

        return "{" +
                "\"resourceSharingPolicyId\": " + resourceSharingPolicyId + ", " +
                "\"resourceId\": \"" + resourceId + "\", " +
                "\"resourceType\": \"" + resourceType + "\", " +
                "\"initiatingOrgId\": \"" + initiatingOrgId + "\", " +
                "\"policyHoldingOrgId\": \"" + policyHoldingOrgId + "\", " +
                "\"sharingPolicy\": \"" + sharingPolicy + "\"" +
                "}";
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * Builder for constructing {@link ResourceSharingPolicy} instances.
     */
    public static class Builder {

        private String resourceId;
        private ResourceType resourceType;
        private String initiatingOrgId;
        private String policyHoldingOrgId;
        private PolicyEnum sharingPolicy;

        public Builder withResourceId(String resourceId) {

            this.resourceId = resourceId;
            return this;
        }

        public Builder withResourceType(ResourceType resourceType) {

            this.resourceType = resourceType;
            return this;
        }

        public Builder withInitiatingOrgId(String initiatingOrgId) {

            this.initiatingOrgId = initiatingOrgId;
            return this;
        }

        public Builder withPolicyHoldingOrgId(String policyHoldingOrgId) {

            this.policyHoldingOrgId = policyHoldingOrgId;
            return this;
        }

        public Builder withSharingPolicy(PolicyEnum sharingPolicy) {

            this.sharingPolicy = sharingPolicy;
            return this;
        }

        public ResourceSharingPolicy build() throws ResourceSharingPolicyMgtException {

            if (resourceId == null || resourceType == null || initiatingOrgId == null || policyHoldingOrgId == null ||
                    sharingPolicy == null) {

                throw new ResourceSharingPolicyMgtException(ERROR_CODE_MISSING_MANDATORY_FIELDS.getCode(),
                        ERROR_CODE_MISSING_MANDATORY_FIELDS.getMessage());
            }
            ResourceSharingPolicy policy = new ResourceSharingPolicy();
            policy.setResourceId(this.resourceId);
            policy.setResourceType(this.resourceType);
            policy.setInitiatingOrgId(this.initiatingOrgId);
            policy.setPolicyHoldingOrgId(this.policyHoldingOrgId);
            policy.setSharingPolicy(this.sharingPolicy);
            return policy;
        }
    }
}
