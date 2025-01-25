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

/**
 * Model that contains the user share role with audience details data object.
 */
public class RoleWithAudienceDO {

    private String roleName;
    private String audienceName;
    private String audienceType;

    public String getRoleName() {

        return roleName;
    }

    public void setRoleName(String roleName) {

        this.roleName = roleName;
    }

    public String getAudienceName() {

        return audienceName;
    }

    public void setAudienceName(String audienceName) {

        this.audienceName = audienceName;
    }

    public String getAudienceType() {

        return audienceType;
    }

    public void setAudienceType(String audienceType) {

        this.audienceType = audienceType;
    }
}
