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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.models;

/**
 * Model class representing a shadow connection association.
 */
public class ConnectionAssociation {

    private int id;
    private String sharedConnectionId;
    private String organizationId;
    private String parentConnectionId;
    private String connectionResidentOrganizationId;

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public String getSharedConnectionId() {

        return sharedConnectionId;
    }

    public void setSharedConnectionId(String sharedConnectionId) {

        this.sharedConnectionId = sharedConnectionId;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getParentConnectionId() {

        return parentConnectionId;
    }

    public void setParentConnectionId(String parentConnectionId) {

        this.parentConnectionId = parentConnectionId;
    }

    public String getConnectionResidentOrganizationId() {

        return connectionResidentOrganizationId;
    }

    public void setConnectionResidentOrganizationId(String connectionResidentOrganizationId) {

        this.connectionResidentOrganizationId = connectionResidentOrganizationId;
    }
}
