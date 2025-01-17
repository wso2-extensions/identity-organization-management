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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria.UserCriteriaType;

import java.util.Map;

/**
 * Abstract class for common properties and methods for user unshare data objects.
 *
 * @param <T> The type of user criteria used in the user unsharing operations.
 */
public abstract class BaseUserUnshareDO<T extends UserCriteriaType> {

    private Map<String, T> userCriteria;

    public Map<String, T> getUserCriteria() {

        return userCriteria;
    }

    public void setUserCriteria(Map<String, T> userCriteria) {

        this.userCriteria = userCriteria;
    }
}
