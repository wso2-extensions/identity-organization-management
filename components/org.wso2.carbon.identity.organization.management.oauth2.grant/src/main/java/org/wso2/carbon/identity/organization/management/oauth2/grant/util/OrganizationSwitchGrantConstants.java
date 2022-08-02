/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.oauth2.grant.util;

/**
 Organization switch grant constants.
 */
public class OrganizationSwitchGrantConstants {

    public static final String ORG_SWITCH_PERMISSION = "/permission/admin/manage/identity/organizationmgt/view";
    public static final String ORG_SWITCH_PERMISSION_FOR_SUPER = "/permission/admin";

    /**
     * Constants related to request parameters.
     */
    public static class Params {

        public static final String TOKEN_PARAM = "token";
        public static final String ORG_PARAM = "switching_organization";
    }
}
