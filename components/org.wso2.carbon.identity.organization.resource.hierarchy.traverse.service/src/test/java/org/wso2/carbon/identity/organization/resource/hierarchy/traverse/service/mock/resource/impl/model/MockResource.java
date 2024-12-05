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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.mock.resource.impl.model;

/**
 * Represents a mocked resource in an organization or application hierarchy.
 * <p>
 * This class models resources that are associated either at the organization level
 * or at the application level within an organization. It includes identifiers and
 * metadata such as resource ID, resource name, organization ID, and optionally,
 * an application ID for application-specific resources.
 */
public class MockResource {

    private int id;
    private String resourceName;
    private String orgId;
    private String appId;

    /**
     * Constructs a mocked resource associated with an organization.
     *
     * @param id           Unique identifier of the resource.
     * @param resourceName Descriptive name of the resource.
     * @param orgId        Identifier of the organization the resource belongs to.
     */
    public MockResource(int id, String resourceName, String orgId) {

        this.id = id;
        this.resourceName = resourceName;
        this.orgId = orgId;
    }

    /**
     * Constructs a mocked resource associated with a specific application within an organization.
     *
     * @param id           Unique identifier of the resource.
     * @param resourceName Descriptive name of the resource.
     * @param orgId        Identifier of the organization the resource belongs to.
     * @param appId        Identifier of the application the resource is associated with.
     */
    public MockResource(int id, String resourceName, String orgId, String appId) {

        this(id, resourceName, orgId);
        this.appId = appId;
    }

    /**
     * Retrieves the unique identifier of the resource.
     *
     * @return The resource ID.
     */
    public int getId() {

        return id;
    }

    /**
     * Sets the unique identifier of the resource.
     *
     * @param id The resource ID to set.
     */
    public void setId(int id) {

        this.id = id;
    }

    /**
     * Retrieves the name of the resource.
     *
     * @return The resource name.
     */
    public String getResourceName() {

        return resourceName;
    }

    /**
     * Sets the name of the resource.
     *
     * @param resourceName The name to assign to the resource.
     */
    public void setResourceName(String resourceName) {

        this.resourceName = resourceName;
    }

    /**
     * Retrieves the organization ID associated with the resource.
     *
     * @return The organization ID.
     */
    public String getOrgId() {

        return orgId;
    }

    /**
     * Sets the organization ID associated with the resource.
     *
     * @param orgId The organization ID to set.
     */
    public void setOrgId(String orgId) {

        this.orgId = orgId;
    }

    /**
     * Retrieves the application ID associated with the resource.
     *
     * @return The application ID, or {@code null} if the resource is not associated with a specific application.
     */
    public String getAppId() {

        return appId;
    }

    /**
     * Sets the application ID associated with the resource.
     *
     * @param appId The application ID to set.
     */
    public void setAppId(String appId) {

        this.appId = appId;
    }
}
