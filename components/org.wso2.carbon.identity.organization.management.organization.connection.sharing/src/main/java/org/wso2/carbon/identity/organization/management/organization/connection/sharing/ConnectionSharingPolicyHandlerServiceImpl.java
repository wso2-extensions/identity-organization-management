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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GeneralConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GeneralConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.GetConnectionSharedOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.ResponseSharedConnectionOrgsDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionShareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.dto.SelectiveConnectionUnshareDTO;
import org.wso2.carbon.identity.organization.management.organization.connection.sharing.exception.ConnectionSharingMgtException;

/**
 * Stub implementation of {@link ConnectionSharingPolicyHandlerService}.
 * Real logic is added in the implementation phase when DAO layer is built.
 */
public class ConnectionSharingPolicyHandlerServiceImpl implements ConnectionSharingPolicyHandlerService {

    private static final Log LOG = LogFactory.getLog(ConnectionSharingPolicyHandlerServiceImpl.class);

    @Override
    public void populateSelectiveConnectionShare(SelectiveConnectionShareDTO selectiveConnectionShareDTO)
            throws ConnectionSharingMgtException {

        LOG.info("populateSelectiveConnectionShare invoked — not yet implemented.");
    }

    @Override
    public void populateGeneralConnectionShare(GeneralConnectionShareDTO generalConnectionShareDTO)
            throws ConnectionSharingMgtException {

        LOG.info("populateGeneralConnectionShare invoked — not yet implemented.");
    }

    @Override
    public void populateSelectiveConnectionUnshare(SelectiveConnectionUnshareDTO selectiveConnectionUnshareDTO)
            throws ConnectionSharingMgtException {

        LOG.info("populateSelectiveConnectionUnshare invoked — not yet implemented.");
    }

    @Override
    public void populateGeneralConnectionUnshare(GeneralConnectionUnshareDTO generalConnectionUnshareDTO)
            throws ConnectionSharingMgtException {

        LOG.info("populateGeneralConnectionUnshare invoked — not yet implemented.");
    }

    @Override
    public ResponseSharedConnectionOrgsDTO getConnectionSharedOrganizations(
            GetConnectionSharedOrgsDTO getConnectionSharedOrgsDTO)
            throws ConnectionSharingMgtException {

        LOG.info("getConnectionSharedOrganizations invoked — not yet implemented.");
        return new ResponseSharedConnectionOrgsDTO();
    }
}
