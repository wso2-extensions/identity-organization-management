/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.internal.association.model;

import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.ResourceType;

/**
 * Model class representing a shadow connection association.
 */
public class ConnectionAssociation {

    private final int id;
    private final ResourceType resourceType;
    private final String sharedConnectionId;
    private final String organizationId;
    private final String parentConnectionId;
    private final String connectionResidentOrganizationId;

    private ConnectionAssociation(Builder builder) {

        this.id = builder.id;
        this.resourceType = builder.resourceType;
        this.sharedConnectionId = builder.sharedConnectionId;
        this.organizationId = builder.organizationId;
        this.parentConnectionId = builder.parentConnectionId;
        this.connectionResidentOrganizationId = builder.connectionResidentOrganizationId;
    }

    public int getId() {

        return id;
    }

    public ResourceType getResourceType() {

        return resourceType;
    }

    public String getSharedConnectionId() {

        return sharedConnectionId;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public String getParentConnectionId() {

        return parentConnectionId;
    }

    public String getConnectionResidentOrganizationId() {

        return connectionResidentOrganizationId;
    }

    /**
     * Builder class for constructing ConnectionAssociation instances.
     */
    public static class Builder {

        private int id;
        private ResourceType resourceType;
        private String sharedConnectionId;
        private String organizationId;
        private String parentConnectionId;
        private String connectionResidentOrganizationId;

        public Builder id(int id) {

            this.id = id;
            return this;
        }

        public Builder resourceType(ResourceType resourceType) {

            this.resourceType = resourceType;
            return this;
        }

        public Builder sharedConnectionId(String sharedConnectionId) {

            this.sharedConnectionId = sharedConnectionId;
            return this;
        }

        public Builder organizationId(String organizationId) {

            this.organizationId = organizationId;
            return this;
        }

        public Builder parentConnectionId(String parentConnectionId) {

            this.parentConnectionId = parentConnectionId;
            return this;
        }

        public Builder connectionResidentOrganizationId(String connectionResidentOrganizationId) {

            this.connectionResidentOrganizationId = connectionResidentOrganizationId;
            return this;
        }

        public ConnectionAssociation build() {

            if (this.resourceType != ResourceType.CONNECTION_IDENTITY_PROVIDER) {
                throw new IllegalArgumentException("Provided resource type: " + this.resourceType +
                        "is not allowed for connection associations.");
            }
            return new ConnectionAssociation(this);
        }
    }
}
