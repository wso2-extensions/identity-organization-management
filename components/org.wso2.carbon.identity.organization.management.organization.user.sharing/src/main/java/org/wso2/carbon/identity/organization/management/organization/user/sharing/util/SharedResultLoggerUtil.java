/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.SharedResult;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.models.dos.RoleWithAudienceDO;

import java.util.List;

/**
 * Utility class for logging shared user results.
 */

public class SharedResultLoggerUtil {

    private static final Log log = LogFactory.getLog(SharedResultLoggerUtil.class);

    /**
     * Logs all failed shared results.
     *
     * @param sharedResults List of shared results to process.
     */
    public static void logErroredSharedResults(List<SharedResult> sharedResults) {

        sharedResults.stream()
                .filter(result -> result.getStatusDetail().getStatus() == SharedResult.SharedStatus.FAILED)
                .forEach(result -> {
                    SharedResult.ErrorDetail errorDetail = result.getErrorDetail();
                    RoleWithAudienceDO role = result.getRole();

                    String roleInfo = (role != null && role.getRoleName() != null && role.getAudienceName() != null)
                            ? String.format("%s|%s", role.getRoleName(), role.getAudienceName())
                            : "N/A";

                    String errorMessage = (errorDetail.getError() != null)
                            ? errorDetail.getError().getMessage()
                            : "Unknown Error";

                    log.error(String.format(
                            "User ID: %s, Organization ID: %s, Role: %s, Status: FAILED, " +
                                    "Status Message: %s, Error Message: %s, Fix Suggestion: %s",
                            result.getUserAssociation().getUserId(),
                            result.getUserAssociation().getOrganizationId(),
                            roleInfo,
                            result.getStatusDetail().getStatusMessage(),
                            errorMessage,
                            errorDetail.getFixSuggestion()
                                           ));
                });
    }

    /**
     * Logs all successful shared results.
     *
     * @param sharedResults List of shared results to process.
     */
    public static void logSuccessfulSharedResults(List<SharedResult> sharedResults) {

        sharedResults.stream()
                .filter(result -> result.getStatusDetail().getStatus() == SharedResult.SharedStatus.SUCCESSFUL)
                .forEach(result -> {
                    RoleWithAudienceDO role = result.getRole();

                    String roleInfo = (role != null && role.getRoleName() != null && role.getAudienceName() != null)
                            ? String.format("%s|%s", role.getRoleName(), role.getAudienceName())
                            : "N/A";

                    log.info(String.format(
                            "User ID: %s, Organization ID: %s, Role: %s, Status: SUCCESSFUL, Status Message: %s",
                            result.getUserAssociation().getUserId(),
                            result.getUserAssociation().getOrganizationId(),
                            roleInfo,
                            result.getStatusDetail().getStatusMessage()
                                          ));
                });
    }
}
