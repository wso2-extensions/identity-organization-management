/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.mock.resource.impl;

import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.mock.resource.impl.model.MockResource;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing mocked resources at both organization and application levels.
 * <p>
 * This class maintains separate mappings for organization-level and application-level
 * resources, providing methods to add and retrieve resources. These mappings simulate a database,
 * offering a simple in-memory representation for resource storage and retrieval operations.
 * <p>
 * This implementation is intended for testing or mocking purposes and simulates a resource management system.
 */
public class MockResourceManagementService {

    private final Map<String, MockResource> orgResources;
    private final Map<String, MockResource> appResources;

    /**
     * Initializes the mock resource management service with empty resource mappings
     * for both organization-level and application-level resources.
     */
    public MockResourceManagementService() {

        this.orgResources = new HashMap<>();
        this.appResources = new HashMap<>();
    }

    /**
     * Adds an organization-level resource to the management service.
     *
     * @param orgResource The resource to be added. The resource's organization ID
     *                    ({@link MockResource#getOrgId}) is used as the key for storage.
     */
    public void addOrgResource(MockResource orgResource) {

        orgResources.put(orgResource.getOrgId(), orgResource);
    }

    /**
     * Retrieves an organization-level resource by its organization ID.
     *
     * @param orgId The unique identifier of the organization.
     * @return The resource associated with the given organization ID, or {@code null}
     * if no resource exists for the provided ID.
     */
    public MockResource getOrgResource(String orgId) {

        return orgResources.get(orgId);
    }

    /**
     * Adds an application-level resource to the management service.
     *
     * @param appResource The resource to be added. The key for storage is derived
     *                    by concatenating the resource's organization ID and
     *                    application ID ({@link MockResource#getAppId}), separated by a colon.
     */
    public void addAppResource(MockResource appResource) {

        appResources.put(appResource.getOrgId() + ":" + appResource.getAppId(), appResource);
    }

    /**
     * Retrieves an application-level resource by organization ID and application ID.
     * <p>
     * If no application-level resource is found for the given organization and application IDs,
     * the method falls back to returning the corresponding organization-level resource, if available.
     *
     * @param orgId The unique identifier of the organization.
     * @param appId The unique identifier of the application within the organization.
     * @return The application-level resource if found; otherwise, the organization-level
     * resource associated with the organization ID. Returns {@code null} if no matching
     * resource is available at either level.
     */
    public MockResource getAppResource(String orgId, String appId) {

        MockResource appResource = appResources.get(orgId + ":" + appId);
        if (appResource == null) {
            return orgResources.get(orgId);
        }
        return appResource;
    }
}
