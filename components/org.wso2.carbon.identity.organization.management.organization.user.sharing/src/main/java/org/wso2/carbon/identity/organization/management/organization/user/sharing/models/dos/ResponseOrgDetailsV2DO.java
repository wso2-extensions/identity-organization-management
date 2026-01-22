/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.SharedType;

import java.util.List;

/**
 * Model that contains the shared organization's details of a user in the response object in user-sharing v2.
 */
public class ResponseOrgDetailsV2DO {

    private String userId;
    private String sharedUserId;
    private SharedType sharedType;
    private String organizationId;
    private String organizationName;
    private String organizationHandle;
    private String organizationStatus;
    private String organizationReference;
    private String parentOrganizationId;
    private List<RoleWithAudienceDO> roleWithAudienceDOList;
    private boolean hasChildren;
    private int depthFromRoot;
    private SharingModeDO sharingModeDO;

    public ResponseOrgDetailsV2DO() {

    }

    public ResponseOrgDetailsV2DO(String userId, String sharedUserId, SharedType sharedType, String organizationId,
                                  String organizationName, String organizationHandle, String organizationStatus,
                                  String organizationReference, String parentOrganizationId,
                                  List<RoleWithAudienceDO> roleWithAudienceDOList, boolean hasChildren,
                                  int depthFromRoot, SharingModeDO sharingModeDO) {

        this.userId = userId;
        this.sharedUserId = sharedUserId;
        this.sharedType = sharedType;
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.organizationHandle = organizationHandle;
        this.organizationStatus = organizationStatus;
        this.organizationReference = organizationReference;
        this.parentOrganizationId = parentOrganizationId;
        this.roleWithAudienceDOList = roleWithAudienceDOList;
        this.hasChildren = hasChildren;
        this.depthFromRoot = depthFromRoot;
        this.sharingModeDO = sharingModeDO;
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getSharedUserId() {

        return sharedUserId;
    }

    public void setSharedUserId(String sharedUserId) {

        this.sharedUserId = sharedUserId;
    }

    public SharedType getSharedType() {

        return sharedType;
    }

    public void setSharedType(
            SharedType sharedType) {

        this.sharedType = sharedType;
    }

    public String getOrganizationId() {

        return organizationId;
    }

    public void setOrganizationId(String organizationId) {

        this.organizationId = organizationId;
    }

    public String getOrganizationName() {

        return organizationName;
    }

    public void setOrganizationName(String organizationName) {

        this.organizationName = organizationName;
    }

    public String getOrganizationHandle() {

        return organizationHandle;
    }

    public void setOrganizationHandle(String organizationHandle) {

        this.organizationHandle = organizationHandle;
    }

    public String getOrganizationStatus() {

        return organizationStatus;
    }

    public void setOrganizationStatus(String organizationStatus) {

        this.organizationStatus = organizationStatus;
    }

    public String getOrganizationReference() {

        return organizationReference;
    }

    public void setOrganizationReference(String organizationReference) {

        this.organizationReference = organizationReference;
    }

    public String getParentOrganizationId() {

        return parentOrganizationId;
    }

    public void setParentOrganizationId(String parentOrganizationId) {

        this.parentOrganizationId = parentOrganizationId;
    }

    public List<RoleWithAudienceDO> getRoleWithAudienceDOList() {

        return roleWithAudienceDOList;
    }

    public void setRoleWithAudienceDOList(List<RoleWithAudienceDO> roleWithAudienceDOList) {

        this.roleWithAudienceDOList = roleWithAudienceDOList;
    }

    public boolean isHasChildren() {

        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {

        this.hasChildren = hasChildren;
    }

    public int getDepthFromRoot() {

        return depthFromRoot;
    }

    public void setDepthFromRoot(int depthFromRoot) {

        this.depthFromRoot = depthFromRoot;
    }

    public SharingModeDO getSharingModeDO() {

        return sharingModeDO;
    }

    public void setSharingModeDO(SharingModeDO sharingModeDO) {

        this.sharingModeDO = sharingModeDO;
    }
}
