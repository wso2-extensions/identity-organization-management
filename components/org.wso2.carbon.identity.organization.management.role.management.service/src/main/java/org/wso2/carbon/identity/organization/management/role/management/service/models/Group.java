/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.role.management.service.models;

import java.util.List;

/**
 * This class represents a basic model of a group.
 */
public class Group {

    private String groupId;
    private String groupName;
    private List<Role> roleList;
    private List<User> userList;

    public Group(String groupId, String groupName, List<Role> roleList, List<User> userList) {

        this.groupId = groupId;
        this.groupName = groupName;
        this.roleList = roleList;
        this.userList = userList;
    }

    public Group(String groupId, String groupName) {

        this.groupId = groupId;
        this.groupName = groupName;
    }

    public Group(String groupId) {

        this.groupId = groupId;
    }

    public String getGroupId() {

        return groupId;
    }

    public void setGroupId(String groupId) {

        this.groupId = groupId;
    }

    public String getGroupName() {

        return groupName;
    }

    public void setGroupName(String groupName) {

        this.groupName = groupName;
    }

    public List<Role> getRoleList() {

        return roleList;
    }

    public void setRoleList(List<Role> roleList) {

        this.roleList = roleList;
    }

    public List<User> getUserList() {

        return userList;
    }

    public void setUserList(List<User> userList) {

        this.userList = userList;
    }
}
