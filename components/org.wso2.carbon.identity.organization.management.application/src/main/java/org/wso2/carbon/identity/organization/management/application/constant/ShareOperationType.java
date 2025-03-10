package org.wso2.carbon.identity.organization.management.application.constant;

public enum ShareOperationType {
    APPLICATION_SHARE("APPLICATION_SHARE"),
    APPLICATION_UNSHARE("APPLICATION_UNSHARE");

    private final String value;

    ShareOperationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
