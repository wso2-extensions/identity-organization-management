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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Exception class for client-side errors during organization resource hierarchy traversal.
 * <p>
 * This class handles exceptions where client-side errors occur, capturing error codes, messages, and descriptions
 * to provide detailed context for troubleshooting.
 */
public class OrgResourceHierarchyTraverseClientException extends OrgResourceHierarchyTraverseException {

    private String[] messages;

    /**
     * Constructs a new exception with an array of specified error messages.
     *
     * @param messages Detailed error messages
     */
    public OrgResourceHierarchyTraverseClientException(String[] messages) {

        super(Arrays.toString(messages));
        if (messages == null) {
            return;
        }
        List<String> msgList = new ArrayList<>();
        for (String msg : messages) {
            if (!msg.trim().isEmpty()) {
                msgList.add(msg);
            }
        }
        this.messages = msgList.toArray(new String[0]);
    }

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message Detailed message
     */
    public OrgResourceHierarchyTraverseClientException(String message) {

        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message Detailed message
     * @param e       Cause as {@link Throwable}
     */
    public OrgResourceHierarchyTraverseClientException(String message, Throwable e) {

        super(message, e);
    }

    /**
     * Constructs a new exception with the specified error code and cause.
     *
     * @param errorCode Error code
     * @param message   Detailed message
     */
    public OrgResourceHierarchyTraverseClientException(String errorCode, String message) {

        super(errorCode, message);
    }

    /**
     * Constructs a new exception with the specified error code, message and cause.
     *
     * @param errorCode Error code
     * @param message   Detailed message
     * @param cause     Cause as {@link Throwable}
     */
    public OrgResourceHierarchyTraverseClientException(String errorCode, String message, Throwable cause) {

        super(errorCode, message, cause);
    }

    /**
     * Constructs a new exception with the specified error code, message and description.
     *
     * @param errorCode   Error code.
     * @param message     Error message.
     * @param description Error description.
     */
    public OrgResourceHierarchyTraverseClientException(String errorCode, String message, String description) {

        super(errorCode, message, description);
    }

    /**
     * Constructs a new exception with the specified error code, message, description and cause.
     *
     * @param errorCode   Error code
     * @param message     Detailed message
     * @param cause       Cause as {@link Throwable}
     * @param description Error description.
     */
    public OrgResourceHierarchyTraverseClientException(String errorCode, String message, String description,
                                                       Throwable cause) {

        super(errorCode, message, description, cause);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Client exception error messages are internally generated from the server side.")
    public String[] getMessages() {

        return messages;
    }
}
