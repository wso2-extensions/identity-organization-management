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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models;

/**
 * Model class to represent the shared user association.
 */
public class SharedUserAssociation {

    private String sharedUserId;
    private String sharedOrganizationId;
    private String realUserId;
    private String userResidentOrganizationId;

    public String getSharedUserId() {

        return sharedUserId;
    }

    public void setSharedUserId(String sharedUserId) {

        this.sharedUserId = sharedUserId;
    }

    public String getSharedOrganizationId() {

        return sharedOrganizationId;
    }

    public void setSharedOrganizationId(String sharedOrganizationId) {

        this.sharedOrganizationId = sharedOrganizationId;
    }

    public String getRealUserId() {

        return realUserId;
    }

    public void setRealUserId(String realUserId) {

        this.realUserId = realUserId;
    }

    public String getUserResidentOrganizationId() {

        return userResidentOrganizationId;
    }

    public void setUserResidentOrganizationId(String userResidentOrganizationId) {

        this.userResidentOrganizationId = userResidentOrganizationId;
    }
}
