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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos;

import java.util.List;

/**
 * Model that contains the response object for retrieving the shared organizations of a user.
 */
public class ResponseSharedOrgsDO {

    List<ResponseLinkDO> responseLinks;
    List<ResponseOrgDetailsDO> sharedOrgs;

    public ResponseSharedOrgsDO(List<ResponseLinkDO> responseLinks, List<ResponseOrgDetailsDO> sharedOrgs) {

        this.responseLinks = responseLinks;
        this.sharedOrgs = sharedOrgs;
    }

    public List<ResponseLinkDO> getResponseLinks() {

        return responseLinks;
    }

    public void setResponseLinks(List<ResponseLinkDO> responseLinks) {

        this.responseLinks = responseLinks;
    }

    public List<ResponseOrgDetailsDO> getSharedOrgs() {

        return sharedOrgs;
    }

    public void setSharedOrgs(List<ResponseOrgDetailsDO> sharedOrgs) {

        this.sharedOrgs = sharedOrgs;
    }
}
