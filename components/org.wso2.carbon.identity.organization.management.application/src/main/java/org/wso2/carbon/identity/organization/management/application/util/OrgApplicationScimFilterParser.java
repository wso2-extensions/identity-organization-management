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

package org.wso2.carbon.identity.organization.management.application.util;

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_PATH_FILTER;

/**
 * Utility class to parse SCIM-like filter strings for organization management.
 * This class is specifically designed to handle filters in the format:
 * - organizations[orgId eq "<orgIdValue>"]
 * - organizations[orgId eq "<orgIdValue>"].roles
 * <p>
 * The class enforces a strict format and provides a method to extract the organization ID
 * and an optional path attribute (e.g., ".roles").
 */
public class OrgApplicationScimFilterParser {

    /**
     * Holds the parsed results from the SCIM-like filter string.
     */
    public static class ParsedFilterResult {

        private final String organizationId;
        private final String pathAttribute; // Will be "roles" if present, otherwise null

        ParsedFilterResult(String organizationId, String pathAttribute) {

            this.organizationId = organizationId;
            this.pathAttribute = pathAttribute;
        }

        /**
         * Gets the extracted organization ID.
         *
         * @return The organization ID.
         */
        public String getOrganizationId() {

            return organizationId;
        }

        /**
         * Gets the extracted path attribute (e.g., "roles").
         *
         * @return The path attribute name if present (e.g., "roles"), otherwise null.
         */
        public String getPathAttribute() {

            return pathAttribute;
        }

        /**
         * Checks if a path attribute was present in the filter.
         *
         * @return true if a path attribute (like ".roles") was present, false otherwise.
         */
        public boolean hasPathAttribute() {

            return pathAttribute != null;
        }
    }

    // Regex to strictly match the desired filter format:
    // organizations[orgId eq <orgIdValue>]
    // or
    // organizations[orgId eq <orgIdValue>].roles
    //
    // Breakdown:
    // ^                                         - Start of the string
    // organizations                             - Literal "organizations"
    // \[                                        - Literal opening square bracket
    // orgId                                     - Literal "orgId"
    // \s+eq\s+                                  - "eq" operator surrounded by one or more spaces
    // (?:                                       - Start of non-capturing group for quoted or unquoted value
    //   "([^\"]+)"                              - Group 1: quoted orgId (excluding quotes)
    //   |                                       - OR
    //   ([^\s\]]+)                              - Group 2: unquoted orgId (up to space or closing bracket)
    // )
    // \]                                        - Literal closing square bracket
    // (\.roles)?                                - Optional ".roles" segment; Group 3 captures ".roles" if present
    // $                                         - End of the string
    private static final Pattern FILTER_PATTERN =
            Pattern.compile("^organizations\\[orgId\\s+eq\\s+(?:\"([^\"]+)\"|([^\\s\\]]+))\\](\\.roles)?$");

    /**
     * Parses the given SCIM-like filter string to extract the organization ID and
     * an optional path attribute (specifically ".roles").
     * <p>
     * The method enforces a strict format:
     * - Must start with "organizations".
     * - Must contain a filter `[orgId eq "<orgIdValue>"]`. No other attributes or operators are supported.
     * - Optionally, it can end with ".roles". No other path attributes are supported.
     *
     * @param filterString The filter string to parse (e.g., "organizations[orgId eq \"123-abc-456\"].roles").
     * @return A {@link ParsedFilterResult} containing the extracted organization ID and path attribute.
     * @throws OrganizationManagementClientException If the filter string is null, empty, or does not conform to
     * the expected format.
     */
    public static ParsedFilterResult parseFilter(String filterString) throws OrganizationManagementClientException {

        if (filterString == null || filterString.trim().isEmpty()) {
            throw new OrganizationManagementClientException("Invalid filter string format", "Filter string cannot" +
                    " be null or empty.", ERROR_CODE_INVALID_ORGANIZATION_PATH_FILTER.getCode());
        }
        Matcher matcher = FILTER_PATTERN.matcher(filterString);
        if (matcher.matches()) {
            String organizationId = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String fullPathWithDot = matcher.group(3);
            String pathAttribute = null;
            if (fullPathWithDot != null) {
                // If group 2 matched, it means ".roles" was present.
                // We want to return "roles" without the leading dot.
                pathAttribute = fullPathWithDot.substring(1);
            }
            return new ParsedFilterResult(organizationId, pathAttribute);
        } else {
            // The string does not match the strict pattern.
            throw new OrganizationManagementClientException("Invalid filter string format",
                    ERROR_CODE_INVALID_ORGANIZATION_PATH_FILTER.getDescription(),
                    ERROR_CODE_INVALID_ORGANIZATION_PATH_FILTER.getCode());
        }
    }
}
