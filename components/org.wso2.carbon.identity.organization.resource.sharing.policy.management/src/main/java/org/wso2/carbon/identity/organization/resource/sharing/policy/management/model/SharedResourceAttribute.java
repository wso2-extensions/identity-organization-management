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

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.SharedAttributeType;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.exception.ResourceSharingPolicyMgtException;

import static org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceSharingConstants.ErrorMessage.ERROR_CODE_MISSING_MANDATORY_FIELDS;

/**
 * Model representing the Shared Resource Attribute.
 */
public class SharedResourceAttribute {

    private int sharedResourceAttributeId;
    private int resourceSharingPolicyId;
    private SharedAttributeType sharedAttributeType;
    private String sharedAttributeId;

    public int getSharedResourceAttributeId() {

        return sharedResourceAttributeId;
    }

    public void setSharedResourceAttributeId(int sharedResourceAttributeId) {

        this.sharedResourceAttributeId = sharedResourceAttributeId;
    }

    public int getResourceSharingPolicyId() {

        return resourceSharingPolicyId;
    }

    public void setResourceSharingPolicyId(int resourceSharingPolicyId) {

        this.resourceSharingPolicyId = resourceSharingPolicyId;
    }

    public SharedAttributeType getSharedAttributeType() {

        return sharedAttributeType;
    }

    public void setSharedAttributeType(
            SharedAttributeType sharedAttributeType) {

        this.sharedAttributeType = sharedAttributeType;
    }

    public String getSharedAttributeId() {

        return sharedAttributeId;
    }

    public void setSharedAttributeId(String sharedAttributeId) {

        this.sharedAttributeId = sharedAttributeId;
    }

    @Override
    public String toString() {

        return "{" +
                "\"sharedResourceAttributeId\": " + sharedResourceAttributeId + ", " +
                "\"resourceSharingPolicyId\": " + resourceSharingPolicyId + ", " +
                "\"sharedAttributeType\": \"" + sharedAttributeType + "\", " +
                "\"sharedAttributeId\": \"" + sharedAttributeId + "\"" +
                "}";
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * Builder for constructing {@link SharedResourceAttribute} instances.
     */
    public static class Builder {

        private int resourceSharingPolicyId;
        private SharedAttributeType sharedAttributeType;
        private String sharedAttributeId;

        public Builder withResourceSharingPolicyId(int resourceSharingPolicyId) {

            this.resourceSharingPolicyId = resourceSharingPolicyId;
            return this;
        }

        public Builder withSharedAttributeType(SharedAttributeType sharedAttributeType) {

            this.sharedAttributeType = sharedAttributeType;
            return this;
        }

        public Builder withSharedAttributeId(String sharedAttributeId) {

            this.sharedAttributeId = sharedAttributeId;
            return this;
        }

        public SharedResourceAttribute build() throws ResourceSharingPolicyMgtException {

            if (sharedAttributeType == null || sharedAttributeId == null) {
                throw new ResourceSharingPolicyMgtException(ERROR_CODE_MISSING_MANDATORY_FIELDS.getCode(),
                        ERROR_CODE_MISSING_MANDATORY_FIELDS.getMessage(),
                        ERROR_CODE_MISSING_MANDATORY_FIELDS.getDescription());
            }
            SharedResourceAttribute attributes = new SharedResourceAttribute();
            attributes.setResourceSharingPolicyId(this.resourceSharingPolicyId);
            attributes.setSharedAttributeType(this.sharedAttributeType);
            attributes.setSharedAttributeId(this.sharedAttributeId);
            return attributes;
        }
    }
}
