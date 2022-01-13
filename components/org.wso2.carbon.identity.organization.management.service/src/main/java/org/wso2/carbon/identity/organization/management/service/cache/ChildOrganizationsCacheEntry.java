/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.service.cache;

import org.wso2.carbon.identity.core.cache.CacheEntry;

import java.util.List;

/**
 * Cache entry holding the child organizations.
 */
public class ChildOrganizationsCacheEntry extends CacheEntry {

    private List<String> childOrganizations;

    public ChildOrganizationsCacheEntry(List<String> childOrganizations) {

        this.childOrganizations = childOrganizations;
    }

    public List<String> getChildOrganizations() {

        return childOrganizations;
    }

    public void setChildOrganizations(List<String> childOrganizations) {

        this.childOrganizations = childOrganizations;
    }
}
