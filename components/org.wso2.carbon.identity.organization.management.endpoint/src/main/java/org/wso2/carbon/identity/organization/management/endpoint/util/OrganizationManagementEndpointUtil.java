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

package org.wso2.carbon.identity.organization.management.endpoint.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.organization.management.endpoint.exceptions.OrganizationManagementEndpointException;
import org.wso2.carbon.identity.organization.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.mgt.core.OrganizationUserRoleManager;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ConflictErrorMessages;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ForbiddenErrorMessages;
import org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.NotFoundErrorMessages;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtClientException;
import org.wso2.carbon.identity.organization.management.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.net.URI;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.management.endpoint.constants.OrganizationManagementEndpointConstants.ORGANIZATION_PATH;
import static org.wso2.carbon.identity.organization.management.endpoint.constants.OrganizationManagementEndpointConstants.ORGANIZATION_ROLES_PATH;
import static org.wso2.carbon.identity.organization.management.endpoint.constants.OrganizationManagementEndpointConstants.V1_API_PATH_COMPONENT;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL_FOR_ORG_ROLES;
import static org.wso2.carbon.identity.organization.management.role.mgt.core.constants.OrganizationUserRoleMgtConstants.ErrorMessages.ERROR_CODE_UNEXPECTED;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NAME_CONFLICT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ORGANIZATION;
import static org.wso2.carbon.identity.organization.management.service.util.Utils.getContext;

/**
 * This class provides util functions to the organization management endpoint.
 */
public class OrganizationManagementEndpointUtil {

    private static final Log LOG = LogFactory.getLog(OrganizationManagementEndpointUtil.class);

    /**
     * Get an instance of OrganizationUserRoleManager.
     *
     * @return an instance of OrganizationUserRoleManager
     */
    public static OrganizationUserRoleManager getOrganizationUserRoleManager() {

        return (OrganizationUserRoleManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getOSGiService(OrganizationUserRoleManager.class, null);
    }

    /**
     * Handles the response for client errors.
     *
     * @param e   The client exception thrown.
     * @param log The logger.
     * @return The response for the client error.
     */
    public static Response handleClientErrorResponse(OrganizationManagementClientException e, Log log) {

        if (isNotFoundError(e)) {
            throw buildException(Response.Status.NOT_FOUND, log, e);
        }

        if (isConflictError(e)) {
            throw buildException(Response.Status.CONFLICT, log, e);
        }

        if (isForbiddenError(e)) {
            throw buildException(Response.Status.FORBIDDEN, log, e);
        }

        throw buildException(Response.Status.BAD_REQUEST, log, e);
    }

    /**
     * Handles exceptions for Organization-User-Role management client exceptions.
     *
     * @param e   The error to be thrown.
     * @param log The logger.
     * @return The response for the error.
     */
    public static Response handleClientErrorResponse(OrganizationUserRoleMgtClientException e, Log log) {

        if (isConflictError(e)) {
            throw buildException(Response.Status.CONFLICT, log, e);
        }
        if (isForbiddenError(e)) {
            throw buildException(Response.Status.FORBIDDEN, log, e);
        }
        if (isNotFoundError(e)) {
            throw buildException(Response.Status.NOT_FOUND, log, e);
        }
        throw buildException(Response.Status.BAD_REQUEST, log, e);
    }

    /**
     * Handles the response for server errors.
     *
     * @param e   The server exception thrown.
     * @param log The logger.
     * @return The response for the server error.
     */
    public static Response handleServerErrorResponse(OrganizationManagementException e, Log log) {

        throw buildException(Response.Status.INTERNAL_SERVER_ERROR, log, e);
    }

    /**
     * Handles the response for server errors.
     *
     * @param e   The server exception thrown.
     * @param log The logger.
     * @return The response for the server error.
     */
    public static Response handleServerErrorResponse(OrganizationUserRoleMgtException e, Log log) {

        throw buildException(Response.Status.INTERNAL_SERVER_ERROR, log, e);
    }

    /**
     * Handle the unexpected server errors.
     *
     * @param throwable The throwable error.
     * @param log       The logger.
     * @return The response of the server error.
     */
    public static Response handleUnexpectedServerError(Throwable throwable, Log log) {

        throw buildException(ERROR_CODE_UNEXPECTED.getCode(), log, throwable);
    }

    private static boolean isNotFoundError(OrganizationManagementClientException e) {

        return ERROR_CODE_INVALID_ORGANIZATION.getCode().equals(e.getErrorCode());
    }

    private static boolean isNotFoundError(OrganizationUserRoleMgtClientException e) {

        String code = e.getErrorCode().replace('-', '_');
        return EnumUtils.isValidEnum(NotFoundErrorMessages.class, code);
    }

    private static boolean isConflictError(OrganizationManagementClientException e) {

        return ERROR_CODE_ORGANIZATION_NAME_CONFLICT.getCode().equals(e.getErrorCode());
    }

    private static boolean isForbiddenError(OrganizationManagementClientException e) {

        return ERROR_CODE_USER_NOT_AUTHORIZED_TO_CREATE_ORGANIZATION.getCode().equals(e.getErrorCode());
    }

    private static boolean isConflictError(OrganizationUserRoleMgtClientException e) {

        String code = e.getErrorCode().replace('-', '_');
        return EnumUtils.isValidEnum(ConflictErrorMessages.class, code);
    }

    private static boolean isForbiddenError(OrganizationUserRoleMgtClientException e) {

        String code = e.getErrorCode().replace('-', '_');
        return EnumUtils.isValidEnum(ForbiddenErrorMessages.class, code);
    }

    private static OrganizationManagementEndpointException buildException(Response.Status status, Log log,
                                                                          OrganizationUserRoleMgtException e) {

        if (e instanceof OrganizationUserRoleMgtClientException) {
            logDebug(log, e);
        } else {
            logError(log, e);
        }
        return new OrganizationManagementEndpointException(status, getError(e.getErrorCode(), e.getMessage(),
                e.getDescription()));

    }

    private static OrganizationManagementEndpointException buildException(Response.Status status, Log log,
                                                                          OrganizationManagementException e) {

        if (e instanceof OrganizationManagementClientException) {
            logDebug(log, e);
        } else {
            logError(log, e);
        }
        return new OrganizationManagementEndpointException(status, getError(e.getErrorCode(), e.getMessage(),
                e.getDescription()));
    }

    private static OrganizationManagementEndpointException buildException(String code, Log log, Throwable throwable) {

        Error error = getError(code, Response.Status.INTERNAL_SERVER_ERROR.toString(),
                Response.Status.INTERNAL_SERVER_ERROR.toString());
        logError(log, throwable);
        return new OrganizationManagementEndpointException(Response.Status.INTERNAL_SERVER_ERROR, error);
    }

    /**
     * Returns a generic error object.
     *
     * @param errorCode        The error code.
     * @param errorMessage     The error message.
     * @param errorDescription The error description.
     * @return A generic error with the specified details.
     */
    public static Error getError(String errorCode, String errorMessage, String errorDescription) {

        Error error = new Error();
        error.setCode(errorCode);
        error.setMessage(errorMessage);
        error.setDescription(errorDescription);
        return error;
    }

    private static void logDebug(Log log, OrganizationManagementException e) {

        if (log.isDebugEnabled()) {
            String errorMessageFormat = "errorCode: %s | message: %s";
            String errorMessage = String.format(errorMessageFormat, e.getErrorCode(), e.getDescription());
            log.debug(errorMessage, e);
        }
    }

    private static void logDebug(Log log, Throwable throwable) {

        if (log.isDebugEnabled()) {
            log.debug(Response.Status.BAD_REQUEST, throwable);
        }
    }

    private static void logError(Log log, OrganizationManagementException e) {

        String errorMessageFormat = "errorCode: %s | message: %s";
        String errorMessage = String.format(errorMessageFormat, e.getErrorCode(), e.getDescription());
        log.error(errorMessage, e);
    }

    private static void logError(Log log, Throwable throwable) {

        log.error(throwable.getMessage(), throwable);
    }

    /**
     * Get location of the created organization.
     *
     * @param organizationId The unique identifier of the created organization.
     * @return URI
     */
    public static URI getResourceLocation(String organizationId) {

        return buildURIForHeader(V1_API_PATH_COMPONENT + ORGANIZATION_PATH + organizationId);
    }

    /**
     * Get location of the created organization-user-role mapping.
     *
     * @param organizationId The organizationID.
     * @return URI the resource id.
     */
    public static URI getOrganizationRoleResourceURI(String organizationId) {

        return buildURIForHeader(V1_API_PATH_COMPONENT +
                String.format(ORGANIZATION_ROLES_PATH, organizationId));
    }

    private static URI buildURIForHeader(String endpoint) {

        String context = getContext(endpoint);
        try {
            String url = ServiceURLBuilder.create().addPath(context).build().getAbsolutePublicURL();
            return URI.create(url);
        } catch (URLBuilderException e) {
            Error error;
            if (StringUtils.contains(endpoint, ORGANIZATION_ROLES_PATH)) {
                LOG.error("Server encountered an error while building URL for response header.");
                error = getError(ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL_FOR_ORG_ROLES.getCode(),
                        ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL_FOR_ORG_ROLES.getMessage(),
                        ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL_FOR_ORG_ROLES.getDescription());
            } else {
                LOG.error("Server encountered an error while building URL for response header.");
                error = getError(ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL.getCode(),
                        ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL.getMessage(),
                        ERROR_CODE_ERROR_BUILDING_RESPONSE_HEADER_URL.getDescription());
            }
            throw new OrganizationManagementEndpointException(Response.Status.INTERNAL_SERVER_ERROR, error);
        }
    }
}
