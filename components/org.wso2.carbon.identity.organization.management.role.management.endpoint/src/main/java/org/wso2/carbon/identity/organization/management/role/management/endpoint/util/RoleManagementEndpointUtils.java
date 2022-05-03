/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.role.management.endpoint.util;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.exceptions.RoleManagementEndpointException;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.service.RoleManager;
import org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ConflictErrorMessages;
import org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages;
import org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ForbiddenErrorMessages;
import org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.NotFoundErrorMessages;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementClientException;
import org.wso2.carbon.identity.organization.management.role.management.service.exceptions.RoleManagementException;
import org.wso2.carbon.identity.organization.management.role.management.service.util.Utils;

import java.net.URI;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constants.RoleManagementEndpointConstants.GROUP_PATH;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constants.RoleManagementEndpointConstants.ORGANIZATION_PATH;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constants.RoleManagementEndpointConstants.PATH_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constants.RoleManagementEndpointConstants.ROLE_PATH;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constants.RoleManagementEndpointConstants.USER_PATH;
import static org.wso2.carbon.identity.organization.management.role.management.endpoint.constants.RoleManagementEndpointConstants.V1_API_PATH_COMPONENT;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_GROUP_URI;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_ROLE_URI;
import static org.wso2.carbon.identity.organization.management.role.management.service.constants.RoleManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_USER_URI;

/**
 * The utility class for role management endpoints.
 */
public class RoleManagementEndpointUtils {

    private static final Log LOG = LogFactory.getLog(RoleManagementEndpointUtils.class);

    /**
     * Get an instance of role manager.
     */
    public static RoleManager getRoleManager() {

        return (RoleManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getOSGiService(RoleManager.class, null);
    }

    /**
     * Handles client exceptions for role management.
     *
     * @param e   The role management exception.
     * @param log The Log instance.
     * @return The response for the error.
     */
    public static Response handleClientErrorResponse(RoleManagementException e, Log log) {

        if (isNotFoundError(e)) {
            throw buildException(Response.Status.NOT_FOUND, log, e);
        }
        if (isForbiddenError(e)) {
            throw buildException(Response.Status.FORBIDDEN, log, e);
        }
        if (isConflictError(e)) {
            throw buildException(Response.Status.CONFLICT, log, e);
        }
        throw buildException(Response.Status.BAD_REQUEST, log, e);
    }

    /**
     * Handles server exceptions for role management.
     *
     * @param e   The role management exception.
     * @param log The Log instance.
     * @return The response for the error.
     */
    public static Response handleServerErrorResponse(RoleManagementException e, Log log) {

        throw buildException(Response.Status.INTERNAL_SERVER_ERROR, log, e);
    }

    /**
     * Returns a generic error object.
     *
     * @param errorCode        The error code.
     * @param errorMessage     The error message.
     * @param errorDescription The error description.
     * @return A generic error with specified details.
     */
    public static Error getError(String errorCode, String errorMessage, String errorDescription) {

        Error error = new Error();
        error.setCode(errorCode);
        error.setMessage(errorMessage);
        error.setDescription(errorDescription);
        return error;
    }

    public static URI buildURIForPagination(String paginationUrl) {

        String endpoint = V1_API_PATH_COMPONENT + PATH_SEPARATOR + PATH_SEPARATOR + paginationUrl;
        try {
            return URI.create(ServiceURLBuilder.create().addPath(Utils.getContext(endpoint)).
                    build().getRelativePublicURL());
        } catch (URLBuilderException e) {
            LOG.error("Server encountered an error while building paginated URL for the response.", e);
            Error error = RoleManagementEndpointUtils.getError(
                    ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL.getCode(),
                    ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL.getMessage(),
                    ERROR_CODE_ERROR_BUILDING_PAGINATED_RESPONSE_URL.getDescription());
            throw new RoleManagementEndpointException(Response.Status.INTERNAL_SERVER_ERROR, error);
        }
    }

    /**
     * URI builder for Roles, Groups and Users.
     */
    public enum URIBuilder {
        ROLE_URI {
            @Override
            public URI buildURI(String organizationId, String id) {
                return getUri(organizationId, id, ROLE_PATH, ERROR_CODE_ERROR_BUILDING_ROLE_URI);
            }
        },
        GROUP_URI {
            @Override
            public URI buildURI(String organizationId, String id) {
                return getUri(organizationId, id, GROUP_PATH, ERROR_CODE_ERROR_BUILDING_GROUP_URI);
            }
        },
        USER_URI {
            @Override
            public URI buildURI(String organizationId, String id) {
                return getUri(organizationId, id, USER_PATH, ERROR_CODE_ERROR_BUILDING_USER_URI);
            }
        };

        private static URI getUri(String organizationId, String id, String path, ErrorMessages errorMessage) {
            String endpoint = V1_API_PATH_COMPONENT + PATH_SEPARATOR + ORGANIZATION_PATH + PATH_SEPARATOR
                    + organizationId + PATH_SEPARATOR + path + PATH_SEPARATOR + id;
            try {
                return URI.create(ServiceURLBuilder.create().addPath(Utils.getContext(endpoint)).
                        build().getAbsolutePublicURL());
            } catch (URLBuilderException e) {
                Error error = getError(errorMessage.getCode(),
                        errorMessage.getMessage(),
                        String.format(errorMessage.getDescription(), id));
                LOG.error(String.format("Server encountered an error while building URL for %s ",
                        path.substring(0, path.length() - 1)) + id);
                throw new RoleManagementEndpointException(Response.Status.INTERNAL_SERVER_ERROR, error);
            }
        }

        /**
         * To build a URI of a resource.
         *
         * @param organizationId The ID of the organization.
         * @param id             The ID of the resource.
         * @return URI.
         */
        public abstract URI buildURI(String organizationId, String id);
    }

    /**
     * Checks the exception key code and returns true if it is a conflict error.
     *
     * @param e The role management exception.
     * @return If the exception is thrown due to conflict error it returns true, else false.
     */
    private static boolean isConflictError(RoleManagementException e) {

        String code = e.getErrorCode().replace("-", "_");
        return EnumUtils.isValidEnum(ConflictErrorMessages.class, code);
    }

    /**
     * Checks the exception key code and returns true if it is a forbidden error.
     *
     * @param e The role management exception.
     * @return If the exception is thrown due to forbidden error it returns true, else false.
     */
    private static boolean isForbiddenError(RoleManagementException e) {

        String code = e.getErrorCode().replace("-", "_");
        return EnumUtils.isValidEnum(ForbiddenErrorMessages.class, code);
    }

    /**
     * Checks the exception key code and returns true if it is a not-found error.
     *
     * @param e The role management exception.
     * @return If the exception is thrown due to not-found error it returns true, else false.
     */
    private static boolean isNotFoundError(RoleManagementException e) {

        String code = e.getErrorCode().replace("-", "_");
        return EnumUtils.isValidEnum(NotFoundErrorMessages.class, code);
    }

    /**
     * Building the RoleManagementEndpointException according to the RoleManagementException.
     *
     * @param status The status of the reponse.
     * @param log    The Log instance.
     * @param e      The  role management exception.
     * @return A RoleManagementEndpointException.
     */
    private static RoleManagementEndpointException buildException(Response.Status status, Log log,
                                                                  RoleManagementException e) {
        if (e instanceof RoleManagementClientException) {
            logDebug(log, e);
        } else {
            logError(log, e);
        }
        return new RoleManagementEndpointException(status, getError(e.getErrorCode(), e.getMessage(),
                e.getDescription()));
    }

    /**
     * If debug is enabled log the errors.
     *
     * @param log Log instance.
     * @param e   The role management exception.
     */
    private static void logDebug(Log log, RoleManagementException e) {

        if (log.isDebugEnabled()) {
            String errorMessageFormat = "errorCode: %s | message: %s";
            String errorMessage = String.format(errorMessageFormat, e.getErrorCode(), e.getDescription());
            log.debug(errorMessage, e);
        }
    }

    /**
     * Log the error.
     *
     * @param log Log instance.
     * @param e   The role management exception.
     */
    private static void logError(Log log, RoleManagementException e) {

        String errorMessageFormat = "errorCode: %s | message: %s";
        String errorMessage = String.format(errorMessageFormat, e.getErrorCode(), e.getDescription());
        log.error(errorMessage, e);
    }
}
