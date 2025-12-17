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
    private String before;
    private String after;
    private String filter;
    private Integer limit;
    private Boolean recursive;
    private List<String> attributes;

    public GetUserSharedOrgsDO() {

    }

    public GetUserSharedOrgsDO(String userId, String parentOrgId) {

        this.userId = userId;
        this.parentOrgId = parentOrgId;
    }

    public GetUserSharedOrgsDO(String userId, String parentOrgId, String before, String after, String filter,
                               Integer limit, Boolean recursive, List<String> attributes) {

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

    public String getBefore() {

        return before;
    }

    public void setBefore(String before) {

        this.before = before;
    }

    public String getAfter() {

        return after;
    }

    public void setAfter(String after) {

        this.after = after;
    }

    public String getFilter() {

        return filter;
    }

    public void setFilter(String filter) {

        this.filter = filter;
    }

    public Integer getLimit() {

        return limit;
    }

    public void setLimit(Integer limit) {

        this.limit = limit;
    }

    public Boolean getRecursive() {

        return recursive;
    }

    public void setRecursive(Boolean recursive) {

        this.recursive = recursive;
    }

    public List<String> getAttributes() {

        return attributes;
    }

    public void setAttributes(List<String> attributes) {

        this.attributes = attributes;
    }
}
