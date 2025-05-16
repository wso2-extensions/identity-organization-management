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

/**
 * Utility class to parse SCIM-like filter strings for organization management.
 * This class is specifically designed to handle filters in the format:
 * - organizations[orgId eq "<orgIdValue>"]
 * - organizations[orgId eq "<orgIdValue>"].roles
 * <p>
 * The class enforces a strict format and provides a method to extract the organization ID
 * and an optional path attribute (e.g., ".roles").
 */
public class OrganizationScimFilterParser {

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
    // organizations[orgId eq "<orgIdValue>"]
    // or
    // organizations[orgId eq "<orgIdValue>"].roles
    // Breakdown:
    // ^                                      - Start of the string
    // organizations                          - Literal "organizations"
    // \[                                     - Literal opening square bracket
    // orgId                                  - Literal "orgId"
    // \s+eq\s+                               - "eq" operator surrounded by one or more spaces
    // \"([^\"]+)\"                           - Quoted organization ID. Group 1 captures the ID itself (without quotes).
    //                                          [^\"]+ matches one or more characters that are not a double quote.
    // \]                                     - Literal closing square bracket
    // (                                      - Start of optional group for the path
    //   \.roles                              - Literal ".roles"
    // )?                                     - Makes the entire path group optional. Group 2 captures ".roles".
    // $                                      - End of the string
    private static final Pattern FILTER_PATTERN =
            Pattern.compile("^organizations\\[orgId\\s+eq\\s+\"([^\"]+)\"\\](\\.roles)?$");

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
            // TODO: Add error code.
            throw new OrganizationManagementClientException("Invalid filter string format", "Filter string cannot be null or empty.", "ERRORCODE");
        }

        Matcher matcher = FILTER_PATTERN.matcher(filterString);

        if (matcher.matches()) {
            String organizationId = matcher.group(1); // Extract the orgId from the first capturing group
            String fullPathWithDot = matcher.group(2);   // Extract the optional ".roles" part (Group 2)

            String pathAttribute = null;
            if (fullPathWithDot != null) {
                // If group 2 matched, it means ".roles" was present.
                // We want to return "roles" without the leading dot.
                pathAttribute = fullPathWithDot.substring(1);
            }

            return new ParsedFilterResult(organizationId, pathAttribute);
        } else {
            // The string does not match the strict pattern.
            // TODO: Add error code.
            throw new OrganizationManagementClientException("Invalid filter string format",
                    "Invalid filter string format. Expected format: " +
                            "'organizations[orgId eq \"<orgid>\"]' or " +
                            "'organizations[orgId eq \"<orgid>\"].roles'. " +
                            "Input was: \"" + filterString + "\"", "ERROR CODE");
        }
    }
}
