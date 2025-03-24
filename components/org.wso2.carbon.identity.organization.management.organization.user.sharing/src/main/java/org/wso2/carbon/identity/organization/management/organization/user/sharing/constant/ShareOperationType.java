package org.wso2.carbon.identity.organization.management.organization.user.sharing.constant;

/**
 * Enum representing the types of share operations.
 */
public enum ShareOperationType {
    USER_SHARE("B2B_USER_SHARE"),
    USER_UNSHARE("B2B_USER_UNSHARE");

    private final String value;

    ShareOperationType(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
