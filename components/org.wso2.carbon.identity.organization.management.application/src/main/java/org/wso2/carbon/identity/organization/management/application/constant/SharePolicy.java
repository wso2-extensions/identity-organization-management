package org.wso2.carbon.identity.organization.management.application.constant;

import java.util.List;

public enum SharePolicy {

    DO_NOT_SHARE("DO_NOT_SHARE"),
    SELECTIVE_SHARE("SELECTIVE_SHARE"),
    SHARE_WITH_ALL("SHARE_WITH_ALL");

    private final String value;

    SharePolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
