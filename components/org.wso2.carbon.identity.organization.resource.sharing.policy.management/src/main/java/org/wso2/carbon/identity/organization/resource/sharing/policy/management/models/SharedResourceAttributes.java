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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.models;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;

import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_MISSING_MANDATORY_FIELDS;

/**
 * Model representing the Shared Resource Attribute.
 */
public class SharedResourceAttributes {

    private int resourceSharingPolicyId;
    private String sharedAttributeId;
    private SharedAttributeType sharedAttributeType;

    public int getResourceSharingPolicyId() {
        return resourceSharingPolicyId;
    }

    public void setResourceSharingPolicyId(int resourceSharingPolicyId) {
        this.resourceSharingPolicyId = resourceSharingPolicyId;
    }

    public String getSharedAttributeId() {
        return sharedAttributeId;
    }

    public void setSharedAttributeId(String sharedAttributeId) {
        this.sharedAttributeId = sharedAttributeId;
    }

    public SharedAttributeType getSharedAttributeType() {
        return sharedAttributeType;
    }

    public void setSharedAttributeType(SharedAttributeType sharedAttributeType) {
        this.sharedAttributeType = sharedAttributeType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SharedResourceAttributes} instances.
     */
    public static class Builder {
        private int resourceSharingPolicyId;
        private String sharedAttributeId;
        private SharedAttributeType sharedAttributeType;

        public Builder withResourceSharingPolicyId(int resourceSharingPolicyId) {
            this.resourceSharingPolicyId = resourceSharingPolicyId;
            return this;
        }

        public Builder withSharedAttributeId(String sharedAttributeId) {
            this.sharedAttributeId = sharedAttributeId;
            return this;
        }

        public Builder withSharedAttributeType(SharedAttributeType sharedAttributeType) {
            this.sharedAttributeType = sharedAttributeType;
            return this;
        }

        public SharedResourceAttributes build() throws ResourceSharingPolicyMgtException {
            if (sharedAttributeId == null || sharedAttributeType == null) {
                throw new ResourceSharingPolicyMgtException(ERROR_CODE_MISSING_MANDATORY_FIELDS.getCode(),
                        ERROR_CODE_MISSING_MANDATORY_FIELDS.getMessage());
            }
            SharedResourceAttributes attributes = new SharedResourceAttributes();
            attributes.setResourceSharingPolicyId(this.resourceSharingPolicyId);
            attributes.setSharedAttributeId(this.sharedAttributeId);
            attributes.setSharedAttributeType(this.sharedAttributeType);
            return attributes;
        }
    }
}
