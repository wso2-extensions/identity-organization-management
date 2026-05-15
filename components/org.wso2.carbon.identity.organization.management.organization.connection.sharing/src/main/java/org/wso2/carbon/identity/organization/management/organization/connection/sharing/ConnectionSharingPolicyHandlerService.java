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

package org.wso2.carbon.identity.organization.management.organization.connection.sharing;

import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GeneralConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GeneralConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GetConnectionSharedOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.ResponseSharedConnectionOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;

/**
 * Service interface for managing connection sharing policies across organizations.
 */
public interface ConnectionSharingPolicyHandlerService {

    /**
     * Populates the details required for sharing a connection with selected organizations.
     *
     * @param selectiveConnectionShareDTO Details for selective connection sharing.
     * @throws ConnectionSharingMgtException If an error occurs while processing.
     */
    void populateSelectiveConnectionShare(SelectiveConnectionShareDTO selectiveConnectionShareDTO)
            throws ConnectionSharingMgtException;

    /**
     * Populates the details required for sharing a connection with all organizations.
     *
     * @param generalConnectionShareDTO Details for general connection sharing.
     * @throws ConnectionSharingMgtException If an error occurs while processing.
     */
    void populateGeneralConnectionShare(GeneralConnectionShareDTO generalConnectionShareDTO)
            throws ConnectionSharingMgtException;

    /**
     * Populates the details required for unsharing a connection from selected organizations.
     *
     * @param selectiveConnectionUnshareDTO Details for selective connection unsharing.
     * @throws ConnectionSharingMgtException If an error occurs while processing.
     */
    void populateSelectiveConnectionUnshare(SelectiveConnectionUnshareDTO selectiveConnectionUnshareDTO)
            throws ConnectionSharingMgtException;

    /**
     * Populates the details required for unsharing a connection from all organizations.
     *
     * @param generalConnectionUnshareDTO Details for general connection unsharing.
     * @throws ConnectionSharingMgtException If an error occurs while processing.
     */
    void populateGeneralConnectionUnshare(GeneralConnectionUnshareDTO generalConnectionUnshareDTO)
            throws ConnectionSharingMgtException;

    /**
     * Retrieves the organizations that a connection has been shared with.
     *
     * @param getConnectionSharedOrgsDTO Parameters for retrieving shared organizations.
     * @return ResponseSharedConnectionOrgsDTO containing shared organizations and pagination metadata.
     * @throws ConnectionSharingMgtException If an error occurs while retrieving the data.
     */
    ResponseSharedConnectionOrgsDTO getConnectionSharedOrganizations(
            GetConnectionSharedOrgsDTO getConnectionSharedOrgsDTO)
            throws ConnectionSharingMgtException;
}
