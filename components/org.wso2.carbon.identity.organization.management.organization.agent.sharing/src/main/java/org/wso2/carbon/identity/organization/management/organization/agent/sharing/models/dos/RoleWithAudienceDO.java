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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos;

/**
 * Model that contains the agent share role with audience details data object.
 */
public class RoleWithAudienceDO {

    private String roleName;
    private String audienceName;
    private String audienceType;

    /**
     * Constructs a {@code RoleWithAudienceDO} with all fields unset.
     */
    public RoleWithAudienceDO() {

    }

    /**
     * Constructs a {@code RoleWithAudienceDO} with the given role and audience details.
     *
     * @param roleName     the name of the role.
     * @param audienceName the name of the audience associated with the role.
     * @param audienceType the type of the audience (e.g., application, organization).
     */
    public RoleWithAudienceDO(String roleName, String audienceName, String audienceType) {

        this.roleName = roleName;
        this.audienceName = audienceName;
        this.audienceType = audienceType;
    }

    /**
     * Returns the name of the role.
     *
     * @return the role name.
     */
    public String getRoleName() {

        return roleName;
    }

    /**
     * Sets the name of the role.
     *
     * @param roleName the role name to set.
     */
    public void setRoleName(String roleName) {

        this.roleName = roleName;
    }

    /**
     * Returns the name of the audience associated with the role.
     *
     * @return the audience name.
     */
    public String getAudienceName() {

        return audienceName;
    }

    /**
     * Sets the name of the audience associated with the role.
     *
     * @param audienceName the audience name to set.
     */
    public void setAudienceName(String audienceName) {

        this.audienceName = audienceName;
    }

    /**
     * Returns the type of the audience associated with the role.
     *
     * @return the audience type.
     */
    public String getAudienceType() {

        return audienceType;
    }

    /**
     * Sets the type of the audience associated with the role.
     *
     * @param audienceType the audience type to set.
     */
    public void setAudienceType(String audienceType) {

        this.audienceType = audienceType;
    }
}
