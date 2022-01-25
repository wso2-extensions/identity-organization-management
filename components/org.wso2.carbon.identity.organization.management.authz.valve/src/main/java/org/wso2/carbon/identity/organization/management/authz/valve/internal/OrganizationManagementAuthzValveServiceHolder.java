/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.authz.valve.internal;

import org.wso2.carbon.identity.authz.service.AuthorizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold organization management related authorization valve service data.
 */
public class OrganizationManagementAuthzValveServiceHolder {

    private static final OrganizationManagementAuthzValveServiceHolder instance =
            new OrganizationManagementAuthzValveServiceHolder();
    private List<AuthorizationManager> authorizationManagerList = new ArrayList<>();

    public static OrganizationManagementAuthzValveServiceHolder getInstance() {

        return instance;
    }

    public List<AuthorizationManager> getAuthorizationManagerList() {

        return authorizationManagerList;
    }

    public void setAuthorizationManagerList(List<AuthorizationManager> authorizationManagerList) {

        this.authorizationManagerList = authorizationManagerList;
    }
}
