package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

/**
 * Enum representing the types of asynchronous operation type.
 */
public enum AsyncOperationStatus {
    SUCCESS("success"),
    FAILED("failed"),
    PARTIAL("partial");

    private final String value;

    AsyncOperationStatus(String value) {
        this.value = value;
    }

    /**
     * Returns the exact value as stored in the database.
     *
     * @return The database value of the enum.
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Custom method to get the enum from a string value, handling spaces.
     *
     * @param stringValueOfAsyncOperationStatus The string value of AsyncOperationStatus.
     * @return The corresponding AsyncOperationStatus enum.
     * @throws IllegalArgumentException if the value does not match any enum.
     */
    public static AsyncOperationStatus fromString(String stringValueOfAsyncOperationStatus) {

        for (AsyncOperationStatus status : AsyncOperationStatus.values()) {
            if (status.value.equalsIgnoreCase(stringValueOfAsyncOperationStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid AsyncOperationStatus value: " + stringValueOfAsyncOperationStatus);
    }
}
