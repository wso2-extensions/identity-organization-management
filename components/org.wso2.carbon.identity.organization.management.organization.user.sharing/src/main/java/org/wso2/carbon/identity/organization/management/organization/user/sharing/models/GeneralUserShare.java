/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_BUILD_USER_SHARE;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_POLICY_MISSING;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_ROLES_NULL;
import static org.wso2.carbon.identity.organization.management.organization.user.sharing.constant.UserSharingConstants.ErrorMessage.ERROR_CODE_USER_ID_MISSING;

/**
 * Model that contains the user share general data object.
 */
public class GeneralUserShare extends BaseUserShare {

    /**
     * Builder for constructing {@link GeneralUserShare} instances.
     */
    public static class Builder {
        private String userId;
        private PolicyEnum policy;
        private List<String> roles;

        public Builder withUserId(String userId) throws OrganizationManagementServerException {
            if (userId == null || userId.isEmpty()) {
                throw new OrganizationManagementServerException(ERROR_CODE_USER_ID_MISSING.getMessage());
            }
            this.userId = userId;
            return this;
        }

        public Builder withPolicy(PolicyEnum policy) throws OrganizationManagementServerException {
            if (policy == null) {
                throw new OrganizationManagementServerException(ERROR_CODE_POLICY_MISSING.getMessage());
            }
            this.policy = policy;
            return this;
        }

        public Builder withRoles(List<String> roles) throws OrganizationManagementServerException {
            if (roles == null) {
                throw new OrganizationManagementServerException(ERROR_CODE_ROLES_NULL.getMessage());
            }
            this.roles = roles;
            return this;
        }

        public GeneralUserShare build() throws OrganizationManagementServerException {
            if (userId == null || policy == null) {
                throw new OrganizationManagementServerException(ERROR_CODE_BUILD_USER_SHARE.getMessage());
            }
            GeneralUserShare generalUserShare = new GeneralUserShare();
            generalUserShare.setUserId(userId);
            generalUserShare.setPolicy(policy);
            generalUserShare.setRoles(roles != null ? roles : new ArrayList<>());
            return generalUserShare;
        }
    }
}
