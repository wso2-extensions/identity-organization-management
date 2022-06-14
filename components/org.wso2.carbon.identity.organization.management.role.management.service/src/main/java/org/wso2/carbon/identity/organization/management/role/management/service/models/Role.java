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
 * This class represents a Role model.
 */
public class Role {

    private String id;
    private String name;
    private List<Group> groups;
    private List<User> users;
    private List<String> permissions;

    public Role(String id, String name, List<Group> groups, List<User> users, List<String> permissions) {

        this.id = id;
        this.name = name;
        this.groups = groups;
        this.users = users;
        this.permissions = permissions;
    }

    public Role(String id, String name) {

        this.id = id;
        this.name = name;
    }

    public Role(){
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getDisplayName() {

        return name;
    }

    public void setDisplayName(String name) {

        this.name = name;
    }

    public List<Group> getGroups() {

        return groups;
    }

    public void setGroups(List<Group> groups) {

        this.groups = groups;
    }

    public List<User> getUsers() {

        return users;
    }

    public void setUsers(List<User> users) {

        this.users = users;
    }

    public List<String> getPermissions() {

        return permissions;
    }

    public void setPermissions(List<String> permissions) {

        this.permissions = permissions;
    }
}
