/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.exception;

/**
 * Exception class for server-side errors during organization resource hierarchy traversal.
 * <p>
 * This class handles exceptions related to server-side failures, including error codes, messages,
 * descriptions, and causes, to provide detailed information for debugging.
 */
public class OrgResourceHierarchyTraverseServerException extends OrgResourceHierarchyTraverseException {

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message Detailed message.
     */
    public OrgResourceHierarchyTraverseServerException(String message) {

        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message Detailed message.
     * @param e       Cause as {@link Throwable}.
     */
    public OrgResourceHierarchyTraverseServerException(String message, Throwable e) {

        super(message, e);
    }

    /**
     * Constructs a new exception with the specified error code and message.
     *
     * @param errorCode Error code.
     * @param message   Detailed message.
     */
    public OrgResourceHierarchyTraverseServerException(String errorCode, String message) {

        super(errorCode, message);
    }

    /**
     * Constructs a new exception with the specified error code, message and cause.
     *
     * @param errorCode Error code.
     * @param message   Detailed message.
     * @param cause     Cause as {@link Throwable}.
     */
    public OrgResourceHierarchyTraverseServerException(String errorCode, String message, Throwable cause) {

        super(errorCode, message, cause);
    }

    /**
     * Constructs a new exception with the specified error code, message and description.
     *
     * @param errorCode   Error code.
     * @param message     Error message.
     * @param description Error description.
     */
    public OrgResourceHierarchyTraverseServerException(String errorCode, String message, String description) {

        super(errorCode, message, description);
    }

    /**
     * Constructs a new exception with the specified error code, message, description and cause.
     *
     * @param errorCode   Error code.
     * @param message     Detailed message.
     * @param description Error description.
     * @param cause       Cause as {@link Throwable}.
     */
    public OrgResourceHierarchyTraverseServerException(String errorCode, String message, String description,
                                                       Throwable cause) {

        super(errorCode, message, description, cause);
    }
}
