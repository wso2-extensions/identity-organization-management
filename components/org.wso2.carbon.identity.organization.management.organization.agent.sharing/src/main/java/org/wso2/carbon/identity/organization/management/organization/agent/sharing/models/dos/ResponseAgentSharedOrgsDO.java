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

package org.wso2.carbon.identity.organization.management.organization.agent.sharing.models.dos;

import java.util.List;

/**
 * Model that contains the response object for retrieving the shared organizations of an agent.
 */
public class ResponseAgentSharedOrgsDO {

    private SharingModeDO sharingMode;
    private List<ResponseOrgDetailsAgentDO> sharedOrgs;
    private int nextPageCursor;
    private int previousPageCursor;

    public ResponseAgentSharedOrgsDO() {

    }

    public ResponseAgentSharedOrgsDO(SharingModeDO sharingMode, List<ResponseOrgDetailsAgentDO> sharedOrgs,
                                     int nextPageCursor, int previousPageCursor) {

        this.sharingMode = sharingMode;
        this.sharedOrgs = sharedOrgs;
        this.nextPageCursor = nextPageCursor;
        this.previousPageCursor = previousPageCursor;
    }

    public SharingModeDO getSharingMode() {

        return sharingMode;
    }

    public void setSharingMode(SharingModeDO sharingMode) {

        this.sharingMode = sharingMode;
    }

    public List<ResponseOrgDetailsAgentDO> getSharedOrgs() {

        return sharedOrgs;
    }

    public void setSharedOrgs(List<ResponseOrgDetailsAgentDO> sharedOrgs) {

        this.sharedOrgs = sharedOrgs;
    }

    public int getNextPageCursor() {

        return nextPageCursor;
    }

    public void setNextPageCursor(int nextPageCursor) {

        this.nextPageCursor = nextPageCursor;
    }

    public int getPreviousPageCursor() {

        return previousPageCursor;
    }

    public void setPreviousPageCursor(int previousPageCursor) {

        this.previousPageCursor = previousPageCursor;
    }
}
