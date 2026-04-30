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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto;

import java.util.List;

/**
 * DTO for the response of a get-shared-organizations query.
 * {@code sharingMode} at the top level is nullable — only populated when the connection was shared via
 * {@code ALL_EXISTING_AND_FUTURE_ORGS} and {@code attributes=sharingMode} was requested.
 */
public class ResponseSharedConnectionOrgsDTO {

    private ConnectionSharingModeDTO sharingMode;
    private List<ResponseConnectionOrgDetailsDTO> sharedOrgs;
    private int nextPageCursor;
    private int previousPageCursor;

    public ConnectionSharingModeDTO getSharingMode() {

        return sharingMode;
    }

    public void setSharingMode(ConnectionSharingModeDTO sharingMode) {

        this.sharingMode = sharingMode;
    }

    public List<ResponseConnectionOrgDetailsDTO> getSharedOrgs() {

        return sharedOrgs;
    }

    public void setSharedOrgs(List<ResponseConnectionOrgDetailsDTO> sharedOrgs) {

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
