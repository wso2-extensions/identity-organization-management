/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service.model;

import java.util.List;

/**
 * Model class for holding discovery attributes of organizations.
 */
public class DiscoveryOrganizationsResult {

    private List<OrganizationDiscovery> organizations;
    private int limit;
    private int offset;
    private int totalResults;

    public List<OrganizationDiscovery> getOrganizations() {

        return organizations;
    }

    public void setOrganizations(List<OrganizationDiscovery> organizations) {

        this.organizations = organizations;
    }

    public int getLimit() {

        return limit;
    }

    public void setLimit(int limit) {

        this.limit = limit;
    }

    public int getOffset() {

        return offset;
    }

    public void setOffset(int offset) {

        this.offset = offset;
    }

    public int getTotalResults() {

        return totalResults;
    }

    public void setTotalResults(int totalResults) {

        this.totalResults = totalResults;
    }
}
