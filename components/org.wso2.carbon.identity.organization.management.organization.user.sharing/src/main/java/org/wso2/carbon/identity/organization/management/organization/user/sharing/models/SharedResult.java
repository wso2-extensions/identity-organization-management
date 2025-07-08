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

package org.wso2.carbon.identity.organization.management.organization.user.sharing.models;

/**
 * Model that represent each shared result with shared status.
 */
public class SharedResult {

    private int id;
    private SharingType sharingType;
    private StatusDetail statusDetail;
    private ErrorDetail errorDetail;

    private SharedResult(Builder builder) {

        this.id = builder.id;
        this.sharingType = builder.sharingType;
        this.statusDetail = builder.statusDetail;
        this.errorDetail = builder.errorDetail;
    }

    /**
     * Converts the current instance into a Builder for modification.
     */
    public Builder toBuilder() {

        return new Builder()
                .id(this.id)
                .sharingType(this.sharingType)
                .statusDetail(this.statusDetail)
                .errorDetail(this.errorDetail);
    }

    public int getId() {

        return id;
    }

    public void setId(int id) {

        this.id = id;
    }

    public SharingType getSharingType() {

        return sharingType;
    }

    public void setSharingType(SharingType sharingType) {

        this.sharingType = sharingType;
    }

    public StatusDetail getStatusDetail() {

        return statusDetail;
    }

    public void setStatusDetail(StatusDetail statusDetail) {

        this.statusDetail = statusDetail;
    }

    public ErrorDetail getErrorDetail() {

        return errorDetail;
    }

    public void setErrorDetail(ErrorDetail errorDetail) {

        this.errorDetail = errorDetail;
    }

    /**
     * Builder class for SharedResult.
     */
    public static class Builder {

        private int id;
        private SharingType sharingType;
        private StatusDetail statusDetail;
        private ErrorDetail errorDetail;

        public Builder id(int id) {

            this.id = id;
            return this;
        }

        public Builder sharingType(SharingType sharingType) {

            this.sharingType = sharingType;
            return this;
        }

        public Builder statusDetail(StatusDetail statusDetail) {

            this.statusDetail = statusDetail;
            return this;
        }

        public Builder errorDetail(ErrorDetail errorDetail) {

            this.errorDetail = errorDetail;
            return this;
        }

        public SharedResult build() {

            return new SharedResult(this);
        }
    }

    /**
     * Inner class to encapsulate status details.
     */
    public static class StatusDetail {

        private final SharedStatus status;
        private final String statusMessage;

        public StatusDetail(SharedStatus status, String statusMessage) {

            this.status = status;
            this.statusMessage = statusMessage;
        }

        public SharedStatus getStatus() {

            return status;
        }

        public String getStatusMessage() {

            return statusMessage;
        }
    }

    /**
     * Inner class to encapsulate error details.
     */
    public static class ErrorDetail {

        private final Throwable error;
        private final String fixSuggestion;

        public ErrorDetail(Throwable error, String fixSuggestion) {

            this.error = error;
            this.fixSuggestion = fixSuggestion;
        }

        public Throwable getError() {

            return error;
        }

        public String getFixSuggestion() {

            return fixSuggestion;
        }
    }

    /**
     * Enum representing the possible statuses of a shared result.
     */
    public enum SharedStatus {

        SUCCESSFUL,
        FAILED,
        PROCESSING
    }

    /**
     * Enum representing the result is from either sharing or unsharing.
     */
    public enum SharingType {

        SHARE,
        UNSHARE
    }
}
