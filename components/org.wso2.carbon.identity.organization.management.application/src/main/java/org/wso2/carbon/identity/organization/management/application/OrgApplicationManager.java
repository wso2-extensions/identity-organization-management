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

package org.wso2.carbon.identity.organization.management.application;

import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.organization.management.application.exception.OrgApplicationMgtException;
import org.wso2.carbon.identity.organization.management.service.model.Organization;

import java.util.Optional;

/**
 * Interface for Organization Application Management.
 */
public interface OrgApplicationManager {

    ServiceProvider getOrgApplication(String applicationId, String tenantDomain) throws OrgApplicationMgtException;

    String shareOrganizationApplication(Organization parent, Organization childOrg, ServiceProvider serviceProvider)
            throws OrgApplicationMgtException;

    Optional<String> resolveOrganizationSpResourceId(String orgName, String parentApplication, String parentTenant)
            throws OrgApplicationMgtException;
}
