/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.enterprise.login.mgt.core.model;

import java.util.List;

/**
 * Enterprise login management Email domain mapping object.
 */
public class EmailDomainMapping {

    private int outboundSpTenantId;
    private int inboundSpTenantId;
    private List<String> emailDomains;

    /**
     * Email domain mapping constructor.
     */
    public EmailDomainMapping(List<String> emailDomains, int outboundSpTenantId, int inboundSpTenantId) {

        this.emailDomains = emailDomains;
        this.outboundSpTenantId = outboundSpTenantId;
        this.inboundSpTenantId = inboundSpTenantId;
    }

    public int getOutboundSpTenantId() {
        return outboundSpTenantId;
    }

    public void setOutboundSpTenantId(int outboundSpTenantId) {
        this.outboundSpTenantId = outboundSpTenantId;
    }

    public List<String> getEmailDomains() {
        return emailDomains;
    }

    public void setEmailDomains(List<String> emailDomains) {
        this.emailDomains = emailDomains;
    }

    public int getInboundSpTenantId() {
        return inboundSpTenantId;
    }

    public void setInboundSpTenantId(int inboundSpTenantId) {
        this.inboundSpTenantId = inboundSpTenantId;
    }
}
