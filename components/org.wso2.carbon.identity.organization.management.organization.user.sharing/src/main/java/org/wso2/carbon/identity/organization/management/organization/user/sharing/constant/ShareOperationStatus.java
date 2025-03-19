package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

/**
 * Enum representing the types of asynchronous operation type.
 */
public enum ShareOperationStatus {
    SUCCESS("SUCCESS"),
    FAILED("FAIL"),
    PARTIAL("PARTIAL");

    private final String value;

    ShareOperationStatus(String value) {
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
     * @param stringValueOfAsyncOperationStatus The string value of ShareOperationStatus.
     * @return The corresponding ShareOperationStatus enum.
     * @throws IllegalArgumentException if the value does not match any enum.
     */
    public static ShareOperationStatus fromString(String stringValueOfAsyncOperationStatus) {

        for (ShareOperationStatus status : ShareOperationStatus.values()) {
            if (status.value.equalsIgnoreCase(stringValueOfAsyncOperationStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ShareOperationStatus value: " + stringValueOfAsyncOperationStatus);
    }
}
