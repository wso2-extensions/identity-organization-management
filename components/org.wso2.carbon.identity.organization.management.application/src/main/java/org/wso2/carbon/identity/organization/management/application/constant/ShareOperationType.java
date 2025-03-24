package org.wso2.carbon.identity.organization.management.application.constant;

/**
 * Enum representing the types of share operations.
 */
public enum ShareOperationType {
    APPLICATION_SHARE("B2B_APPLICATION_SHARE"),
    APPLICATION_UNSHARE("B2B_APPLICATION_UNSHARE");

    private final String value;

    ShareOperationType(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
