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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.usercriteria;

import java.util.List;

/**
 * Represents a user criteria type that contains a list of user IDs.
 */
public class UserIdList implements UserCriteriaType {

    private List<String> ids;

    public UserIdList(List<String> ids) {

        this.ids = ids;
    }

    public List<String> getIds() {

        return ids;
    }

    public void setIds(List<String> ids) {

        this.ids = ids;
    }
}
