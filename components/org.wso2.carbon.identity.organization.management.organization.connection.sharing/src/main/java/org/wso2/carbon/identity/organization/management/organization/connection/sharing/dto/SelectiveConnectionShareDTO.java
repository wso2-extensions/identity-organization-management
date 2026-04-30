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

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.connectioncriteria.ConnectionCriteriaType;

import java.util.List;

/**
 * DTO for sharing connections with a selected set of organizations.
 */
public class SelectiveConnectionShareDTO extends BaseConnectionShareDTO<ConnectionCriteriaType> {

    private List<SelectiveConnectionShareOrgConfigDTO> organizations;

    /**
     * Returns the list of per-organization sharing configurations for this selective share operation.
     *
     * @return list of {@link SelectiveConnectionShareOrgConfigDTO}, or {@code null} if not set
     */
    public List<SelectiveConnectionShareOrgConfigDTO> getOrganizations() {

        return organizations;
    }

    /**
     * Sets the list of per-organization sharing configurations for this selective share operation.
     *
     * @param organizations list of {@link SelectiveConnectionShareOrgConfigDTO} specifying each target
     *                      organization and its sharing policy; must not be {@code null} or empty
     */
    public void setOrganizations(List<SelectiveConnectionShareOrgConfigDTO> organizations) {

        this.organizations = organizations;
    }
}
