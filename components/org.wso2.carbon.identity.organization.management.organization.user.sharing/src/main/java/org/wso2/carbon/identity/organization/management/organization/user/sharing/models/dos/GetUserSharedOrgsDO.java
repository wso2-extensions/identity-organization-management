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

import java.util.List;

/**
 * Data object for getting user shared organizations.
 */
public class GetUserSharedOrgsDO {

    private String userId;
    private String parentOrgId;
    private int before;
    private int after;
    private String filter;
    private int limit;
    private boolean recursive;
    private List<String> attributes;

    public GetUserSharedOrgsDO() {

    }

    public GetUserSharedOrgsDO(String userId, String parentOrgId) {

        this.userId = userId;
        this.parentOrgId = parentOrgId;
    }

    public GetUserSharedOrgsDO(String userId, String parentOrgId, int before, int after, String filter,
                               int limit, boolean recursive, List<String> attributes) {

        this.userId = userId;
        this.parentOrgId = parentOrgId;
        this.before = before;
        this.after = after;
        this.filter = filter;
        this.limit = limit;
        this.recursive = recursive;
        this.attributes = attributes;
    }

    public String getUserId() {

        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
    }

    public String getParentOrgId() {

        return parentOrgId;
    }

    public void setParentOrgId(String parentOrgId) {

        this.parentOrgId = parentOrgId;
    }

    public int getBefore() {

        return before;
    }

    public void setBefore(int before) {

        this.before = before;
    }

    public int getAfter() {

        return after;
    }

    public void setAfter(int after) {

        this.after = after;
    }

    public String getFilter() {

        return filter;
    }

    public void setFilter(String filter) {

        this.filter = filter;
    }

    public int getLimit() {

        return limit;
    }

    public void setLimit(int limit) {

        this.limit = limit;
    }

    public boolean getRecursive() {

        return recursive;
    }

    public void setRecursive(boolean recursive) {

        this.recursive = recursive;
    }

    public List<String> getAttributes() {

        return attributes;
    }

    public void setAttributes(List<String> attributes) {

        this.attributes = attributes;
    }
}
