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
 * Connection criteria type that identifies connections by their names.
 * Key used in the criteria map: {@code "CONNECTION_NAMES"}.
 */
public class ConnectionNameList implements ConnectionCriteriaType {

    private List<String> names;

    /**
     * Constructs a ConnectionNameList with the given list of connection names.
     *
     * @param names list of connection display names used to identify connections
     */
    public ConnectionNameList(List<String> names) {

        this.names = names;
    }

    /**
     * Returns the list of connection display names.
     *
     * @return list of connection names
     */
    public List<String> getNames() {

        return names;
    }

    /**
     * Sets the list of connection display names.
     *
     * @param names list of connection names used to identify connections
     */
    public void setNames(List<String> names) {

        this.names = names;
    }
}
