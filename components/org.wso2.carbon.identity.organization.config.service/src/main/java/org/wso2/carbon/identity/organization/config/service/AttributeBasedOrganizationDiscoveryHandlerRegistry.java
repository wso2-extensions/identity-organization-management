package org.wso2.carbon.identity.organization.config.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attribute based organization discovery handler registry. This class holds the supported discovery attribute keys.
 */
public class AttributeBasedOrganizationDiscoveryHandlerRegistry {

    private static final AttributeBasedOrganizationDiscoveryHandlerRegistry instance = new AttributeBasedOrganizationDiscoveryHandlerRegistry();
    private static final String ENABLED_KEY = ".enable";

    private final List<String> supportedDiscoveryAttributeKeys = new ArrayList<>();

    private AttributeBasedOrganizationDiscoveryHandlerRegistry() {

    }

    /**
     * Get the instance of the registry.
     *
     * @return Instance of the registry.
     */
    public static AttributeBasedOrganizationDiscoveryHandlerRegistry getInstance() {

        return instance;
    }

    /**
     * Get the supported discovery attribute key list
     *
     * @return Supported discovery attribute keys.
     */
    public List<String> getSupportedDiscoveryAttributeKeys() {

        return Collections.unmodifiableList(supportedDiscoveryAttributeKeys);
    }

    /**
     * Add a supported discovery attribute key.
     *
     * @param key Supported discovery attribute key.
     */
    public void addSupportedDiscoveryAttributeKey(String key) {

        supportedDiscoveryAttributeKeys.add(key + ENABLED_KEY);
    }
}
