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

package org.wso2.carbon.identity.organization.user.invitation.management.models;

/**
 * Model that contains the invitation related details.
 */
public class AudienceInfo {

    private String applicationId;
    private String applicationName;
    private String applicationType;

    public String getApplicationId() {

        return applicationId;
    }

    public void setApplicationId(String applicationId) {

        this.applicationId = applicationId;
    }

    public String getApplicationName() {

        return applicationName;
    }

    public void setApplicationName(String applicationName) {

        this.applicationName = applicationName;
    }

    public String getApplicationType() {

        return applicationType;
    }

    public void setApplicationType(String applicationType) {

        this.applicationType = applicationType;
    }
}
