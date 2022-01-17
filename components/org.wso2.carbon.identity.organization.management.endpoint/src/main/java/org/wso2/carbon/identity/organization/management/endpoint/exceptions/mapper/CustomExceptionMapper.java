/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.endpoint.exceptions.mapper;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtClientException;
import org.wso2.carbon.identity.organization.management.endpoint.model.Error;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.INVALID_REQUEST;

/**
 * Custom Exception Mapper.
 */
public class CustomExceptionMapper implements ExceptionMapper<JsonMappingException> {
    private static final Log log = LogFactory.getLog(CustomExceptionMapper.class);

    @Override
    public Response toResponse(JsonMappingException e) {
        OrganizationUserRoleMgtClientException clientException = new OrganizationUserRoleMgtClientException(
                INVALID_REQUEST.getMessage(),
                INVALID_REQUEST.getDescription(),
                INVALID_REQUEST.getCode(),
                e);
        Error errorDTO = getErrorDTO(
                INVALID_REQUEST.getMessage(),
                INVALID_REQUEST.getDescription(),
                INVALID_REQUEST.getCode());
        if (log.isDebugEnabled()) {
            log.debug(Response.Status.BAD_REQUEST, clientException);
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(errorDTO)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
    }

    private static Error getErrorDTO(String message, String description, String code) {

        Error errorDTO = new Error();
        errorDTO.setCode(code);
        errorDTO.setMessage(message);
        errorDTO.setDescription(description);
        return errorDTO;
    }
}
