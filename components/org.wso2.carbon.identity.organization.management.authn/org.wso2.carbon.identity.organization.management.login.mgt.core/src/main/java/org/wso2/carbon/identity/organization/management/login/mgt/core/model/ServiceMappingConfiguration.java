/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein in any form is strictly forbidden, unless permitted by WSO2 expressly.
 * You may not alter or remove any copyright or other notice from copies of this content.
 */

package org.wso2.carbon.identity.organization.management.login.mgt.core.model;

/**
 * Enterprise login management configuration DTO object.
 */
public class ServiceMappingConfiguration {

    private String inboundSpResourceId;
    private int outboundSpTenantId;
    private String outboundSpResourceId;
    private int inboundSpTenantId;

    /**
     * Constructor.
     */
    public ServiceMappingConfiguration(String inboundSpResourceId, int inboundSpTenantId, String outboundSpResourceId,
                                       int outboundSpTenantId) {

        this.inboundSpResourceId = inboundSpResourceId;
        this.inboundSpTenantId = inboundSpTenantId;
        this.outboundSpResourceId = outboundSpResourceId;
        this.outboundSpTenantId = outboundSpTenantId;
    }

    public String getInboundSpResourceId() {
        return inboundSpResourceId;
    }

    public void setInboundSpResourceId(String inboundSpResourceId) {
        this.inboundSpResourceId = inboundSpResourceId;
    }

    public int getOutboundSpTenantId() {
        return outboundSpTenantId;
    }

    public void setOutboundSpTenantId(int outboundSpTenantId) {
        this.outboundSpTenantId = outboundSpTenantId;
    }

    public String getOutboundSpResourceId() {
        return outboundSpResourceId;
    }

    public void setOutboundSpResourceId(String outboundSpResourceId) {
        this.outboundSpResourceId = outboundSpResourceId;
    }

    public int getInboundSpTenantId() {
        return inboundSpTenantId;
    }

    public void setInboundSpTenantId(int inboundSpTenantId) {
        this.inboundSpTenantId = inboundSpTenantId;
    }
}
