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

package org.wso2.carbon.identity.organization.management.application.model;

import org.wso2.carbon.identity.organization.management.application.model.operation.ApplicationShareRolePolicy;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.constant.PolicyEnum;

/**
 * Data object that represents the application sharing mode,
 * including the sharing policy and the role-sharing policy configuration.
 */
public class SharingModeDO  {

    private final PolicyEnum policy;
    private final ApplicationShareRolePolicy applicationShareRolePolicy;

    private SharingModeDO(PolicyEnum policy, ApplicationShareRolePolicy applicationShareRolePolicy) {

        this.policy = policy;
        this.applicationShareRolePolicy = applicationShareRolePolicy;
    }

    public PolicyEnum getPolicy() {

        return policy;
    }

    public ApplicationShareRolePolicy getApplicationShareRolePolicy() {

        return applicationShareRolePolicy;
    }

    /**
     * This class is used to build the SharingModeDO object.
     */
    public static class Builder {

        private PolicyEnum policy;
        private ApplicationShareRolePolicy applicationShareRolePolicy;

        public Builder policy(PolicyEnum policy) {

            this.policy = policy;
            return this;
        }

        public Builder applicationShareRolePolicy(ApplicationShareRolePolicy applicationShareRolePolicy) {

            this.applicationShareRolePolicy = applicationShareRolePolicy;
            return this;
        }

        public SharingModeDO build() {

            return new SharingModeDO(policy, applicationShareRolePolicy);
        }
    }
}
