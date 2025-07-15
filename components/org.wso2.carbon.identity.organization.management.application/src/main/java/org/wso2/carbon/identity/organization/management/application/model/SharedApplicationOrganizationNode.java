/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.model;

import java.util.List;

/**
 * Represents a node in the shared application organization tree.
 */
public class SharedApplicationOrganizationNode {

    private final String applicationResourceId;
    private final String organizationId;
    private final String organizationName;
    private final String organizationHandle;
    private final String organizationStatus;
    private final String parentOrganizationId;
    private final List<RoleWithAudienceDO> roleWithAudienceDOList;
    private final boolean hasChildren;
    private final int depthFromRoot;
    private final SharingModeDO sharingModeDO;

    public SharedApplicationOrganizationNode(String applicationResourceId, String organizationId,
                                             String organizationName, String organizationStatus,
                                             String parentOrganizationId, String organizationHandle,
                                             List<RoleWithAudienceDO> roleWithAudienceDOList, boolean hasChildren,
                                             int depthFromRoot, SharingModeDO sharingModeDO) {

        this.applicationResourceId = applicationResourceId;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.organizationHandle = organizationHandle;
        this.organizationStatus = organizationStatus;
        this.parentOrganizationId = parentOrganizationId;
        this.roleWithAudienceDOList = roleWithAudienceDOList;
        this.hasChildren = hasChildren;
        this.depthFromRoot = depthFromRoot;
        this.sharingModeDO = sharingModeDO;
    }

    public String getApplicationResourceId() {

        return applicationResourceId;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public String getOrganizationName() {

        return organizationName;
    }

    public String getOrganizationHandle() {

        return organizationHandle;
    }

    public String getParentOrganizationId() {

        return parentOrganizationId;
    }

    public String getOrganizationStatus() {

        return organizationStatus;
    }

    public List<RoleWithAudienceDO> getRoleWithAudienceDOList() {

        return roleWithAudienceDOList;
    }

    public boolean hasChildren() {

        return hasChildren;
    }

    public int getDepthFromRoot() {

        return depthFromRoot;
    }

    public SharingModeDO getSharingModeDO() {

        return sharingModeDO;
    }
}
