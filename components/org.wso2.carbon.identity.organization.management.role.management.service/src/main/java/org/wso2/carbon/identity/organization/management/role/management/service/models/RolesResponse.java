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
 * This class represents all the information related to fetch roles response.
 */
public class RolesResponse {

    private String nextCursor;
    private int totalResults;
    private String previousCursor;
    private int itemsPerPage;
    private List<Role> roles;

    public RolesResponse(String nextCursor, int totalResults, String previousCursor, int itemsPerPage,
                         List<Role> roles) {

        this.nextCursor = nextCursor;
        this.totalResults = totalResults;
        this.previousCursor = previousCursor;
        this.itemsPerPage = itemsPerPage;
        this.roles = roles;
    }

    public String getNextCursor() {

        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {

        this.nextCursor = nextCursor;
    }

    public int getTotalResults() {

        return totalResults;
    }

    public void setTotalResults(int totalResults) {

        this.totalResults = totalResults;
    }

    public String getPreviousCursor() {

        return previousCursor;
    }

    public void setPreviousCursor(String previousCursor) {

        this.previousCursor = previousCursor;
    }

    public int getItemsPerPage() {

        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {

        this.itemsPerPage = itemsPerPage;
    }

    public List<Role> getRoles() {

        return roles;
    }

    public void setRoles(List<Role> roles) {

        this.roles = roles;
    }
}
