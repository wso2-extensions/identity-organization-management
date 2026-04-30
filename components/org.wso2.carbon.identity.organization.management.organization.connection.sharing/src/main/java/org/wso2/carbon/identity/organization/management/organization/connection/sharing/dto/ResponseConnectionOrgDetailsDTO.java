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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto;

/**
 * DTO representing details of a single organization that a connection has been shared with.
 * {@code sharingMode} is nullable — only populated when {@code attributes=sharingMode} was requested
 * and the policy is {@code SELECTED_ORG_WITH_ALL_EXISTING_AND_FUTURE_CHILDREN}.
 */
public class ResponseConnectionOrgDetailsDTO {

    private String orgId;
    private String orgName;
    private String orgHandle;
    private String status;
    private String orgRef;
    private Boolean hasChildren;
    private Integer depthFromRoot;
    private String parentOrgId;
    private String parentConnectionId;
    private String sharedConnectionId;
    private ConnectionSharingModeDTO sharingMode;

    public String getOrgId() {

        return orgId;
    }

    public void setOrgId(String orgId) {

        this.orgId = orgId;
    }

    public String getOrgName() {

        return orgName;
    }

    public void setOrgName(String orgName) {

        this.orgName = orgName;
    }

    public String getOrgHandle() {

        return orgHandle;
    }

    public void setOrgHandle(String orgHandle) {

        this.orgHandle = orgHandle;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public String getOrgRef() {

        return orgRef;
    }

    public void setOrgRef(String orgRef) {

        this.orgRef = orgRef;
    }

    public Boolean getHasChildren() {

        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {

        this.hasChildren = hasChildren;
    }

    public Integer getDepthFromRoot() {

        return depthFromRoot;
    }

    public void setDepthFromRoot(Integer depthFromRoot) {

        this.depthFromRoot = depthFromRoot;
    }

    public String getParentOrgId() {

        return parentOrgId;
    }

    public void setParentOrgId(String parentOrgId) {

        this.parentOrgId = parentOrgId;
    }

    public String getParentConnectionId() {

        return parentConnectionId;
    }

    public void setParentConnectionId(String parentConnectionId) {

        this.parentConnectionId = parentConnectionId;
    }

    public String getSharedConnectionId() {

        return sharedConnectionId;
    }

    public void setSharedConnectionId(String sharedConnectionId) {

        this.sharedConnectionId = sharedConnectionId;
    }

    public ConnectionSharingModeDTO getSharingMode() {

        return sharingMode;
    }

    public void setSharingMode(ConnectionSharingModeDTO sharingMode) {

        this.sharingMode = sharingMode;
    }
}
