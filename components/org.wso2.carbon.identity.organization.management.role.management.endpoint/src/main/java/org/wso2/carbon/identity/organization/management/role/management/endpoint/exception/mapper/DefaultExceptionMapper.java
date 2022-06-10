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

package org.wso2.carbon.identity.organization.management.role.management.endpoint.exception.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.util.RoleManagementEndpointUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_REQUEST_BODY;

/**
 * Handles exception when an incorrect JSON request is received.
 * Sends a default error response.
 */
public class DefaultExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Log LOG = LogFactory.getLog(DefaultExceptionMapper.class);

    @Override
    public Response toResponse(JsonProcessingException e) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Provided JSON request content is not in the valid format ", e);
        }
        Error error = RoleManagementEndpointUtils.getError(ERROR_CODE_INVALID_REQUEST_BODY.getCode(),
                ERROR_CODE_INVALID_REQUEST_BODY.getMessage(), ERROR_CODE_INVALID_REQUEST_BODY.getDescription());
        return Response.status(Response.Status.BAD_REQUEST).entity(error)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }
}
