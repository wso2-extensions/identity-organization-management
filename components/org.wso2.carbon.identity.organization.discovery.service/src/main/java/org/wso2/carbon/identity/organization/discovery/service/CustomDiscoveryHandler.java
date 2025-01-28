package org.wso2.carbon.identity.organization.discovery.service;

import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.List;

// TODO: Will be removed in the original PR.
public class CustomDiscoveryHandler implements AttributeBasedOrganizationDiscoveryHandler {
    @Override
    public String getType() {

        return "customOrganizationDiscovery";
    }

    @Override
    public boolean isDiscoveryConfigurationEnabled(String organizationId) throws OrganizationManagementException {

        return true;
    }

    @Override
    public String extractAttributeValue(String discoveryInput) {

        return "custom";
    }

    @Override
    public List<String> requiredEventValidations() {

        return List.of();
    }

    @Override
    public boolean areAttributeValuesInValidFormat(List<String> attributeValues) {

        return true;
    }
}
