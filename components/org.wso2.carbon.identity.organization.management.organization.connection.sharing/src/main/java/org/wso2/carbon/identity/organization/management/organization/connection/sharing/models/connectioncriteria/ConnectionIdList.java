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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.models.connectioncriteria;

import java.util.List;

/**
 * Connection criteria type that identifies connections by their IDs.
 * Key used in the criteria map: {@code "CONNECTION_IDS"}.
 */
public class ConnectionIdList implements ConnectionCriteriaType {

    private List<String> ids;

    public ConnectionIdList(List<String> ids) {

        this.ids = ids;
    }

    public List<String> getIds() {

        return ids;
    }

    public void setIds(List<String> ids) {

        this.ids = ids;
    }
}
