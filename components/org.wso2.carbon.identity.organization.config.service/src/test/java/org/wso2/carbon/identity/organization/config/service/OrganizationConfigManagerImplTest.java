/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.config.service;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManagerImpl;
import org.wso2.carbon.identity.configuration.mgt.core.dao.ConfigurationDAO;
import org.wso2.carbon.identity.configuration.mgt.core.dao.impl.ConfigurationDAOImpl;
import org.wso2.carbon.identity.configuration.mgt.core.internal.ConfigurationManagerComponentDataHolder;
import org.wso2.carbon.identity.configuration.mgt.core.model.ConfigurationManagerConfigurationHolder;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.config.service.exception.OrganizationConfigClientException;
import org.wso2.carbon.identity.organization.config.service.internal.OrganizationConfigServiceHolder;
import org.wso2.carbon.identity.organization.config.service.model.ConfigProperty;
import org.wso2.carbon.identity.organization.config.service.model.DiscoveryConfig;
import org.wso2.carbon.identity.organization.config.service.model.OrganizationConfig;
import org.wso2.carbon.identity.organization.config.service.util.TestUtils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrganizationConfigManagerImpl class.
 */
public class OrganizationConfigManagerImplTest {

    @Mock
    private OrganizationManager organizationManager;
    private final OrganizationConfigManagerImpl organizationConfigManagerImpl = new OrganizationConfigManagerImpl();
    private AutoCloseable mocks;
    MockedStatic<IdentityTenantUtil> identityTenantUtil;
    MockedStatic<IdentityDatabaseUtil> identityDatabaseUtil;
    MockedStatic<PrivilegedCarbonContext> privilegedCarbonContext;
    private static final int SUPER_TENANT_ID = -1234;
    private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";
    private static final String EMAIL_DOMAIN_ENABLE = "emailDomain.enable";
    private static final String EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE = "emailDomainBasedSelfSignup.enable";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String IS_CONSOLE_BRANDING_ENABLED = "isConsoleBrandingEnabled";

    @BeforeClass
    public void setUp() throws Exception {

        mocks = MockitoAnnotations.openMocks(this);

        TestUtils.initiateH2Base();
        setUpCarbonHome();
        mockCarbonContextForSuperTenant();

        DataSource dataSource = mock(DataSource.class);
        identityTenantUtil = mockStatic(IdentityTenantUtil.class);
        identityDatabaseUtil = mockStatic(IdentityDatabaseUtil.class);
        identityDatabaseUtil.when(IdentityDatabaseUtil::getDataSource).thenReturn(dataSource);

        // Mocking the connection close method to avoid closing the connection before needed.
        Connection connection = spy(TestUtils.getConnection());
        doNothing().when(connection).close();
        when(dataSource.getConnection()).thenReturn(connection);

        ConfigurationManager configurationManager = setUpConfigurationManager(identityTenantUtil);
        OrganizationConfigServiceHolder.getInstance().setOrganizationManager(organizationManager);
        when(organizationManager.isPrimaryOrganization(anyString())).thenReturn(true);
        OrganizationConfigServiceHolder.getInstance().setConfigurationManager(configurationManager);
    }

    @Test(priority = 1)
    public void testAddDiscoveryConfiguration() throws Exception {

        List<ConfigProperty> configProperties = new ArrayList<>();
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(configProperties);

        organizationConfigManagerImpl.addDiscoveryConfiguration(discoveryConfig);
        List<ConfigProperty> returnedConfigProperties =
                organizationConfigManagerImpl.getDiscoveryConfiguration().getConfigProperties();

        Assert.assertEquals(returnedConfigProperties.size(), 1);
        Assert.assertEquals(returnedConfigProperties.get(0).getKey(), EMAIL_DOMAIN_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(0).getValue(), TRUE);
    }

    @Test(priority = 2)
    public void testUpdateDiscoveryConfiguration() throws Exception {

        List<ConfigProperty> configProperties = new ArrayList<>();
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE, TRUE));
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(configProperties);

        organizationConfigManagerImpl.updateDiscoveryConfiguration(discoveryConfig);
        List<ConfigProperty> returnedConfigProperties =
                organizationConfigManagerImpl.getDiscoveryConfiguration().getConfigProperties();

        Assert.assertEquals(returnedConfigProperties.size(), 2);
        Assert.assertEquals(returnedConfigProperties.get(0).getKey(), EMAIL_DOMAIN_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(0).getValue(), TRUE);
        Assert.assertEquals(returnedConfigProperties.get(1).getKey(), EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(1).getValue(), TRUE);
    }

    @Test(priority = 3)
    public void testUpdateDiscoveryConfigurationInvalidConfig() throws Exception {

        try {
            List<ConfigProperty> configProperties = new ArrayList<>();
            configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, FALSE));
            configProperties.add(new ConfigProperty(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE, TRUE));
            DiscoveryConfig discoveryConfig = new DiscoveryConfig(configProperties);

            organizationConfigManagerImpl.updateDiscoveryConfiguration(discoveryConfig);
            Assert.fail("Expected OrganizationConfigClientException was not thrown.");
        } catch (OrganizationConfigClientException e) {
            Assert.assertEquals(e.getMessage(), "Invalid organization discovery attribute values.");
        }
    }

    @Test(priority = 4)
    public void testDeleteDiscoveryConfiguration() throws Exception {

        try {
            organizationConfigManagerImpl.deleteDiscoveryConfiguration();
            organizationConfigManagerImpl.getDiscoveryConfiguration();
            Assert.fail("Expected OrganizationConfigClientException was not thrown.");
        } catch (OrganizationConfigClientException e) {
            Assert.assertEquals(e.getMessage(), "No organization discovery configuration found.");
        }
    }

    @Test(priority = 5)
    public void testAddDiscoveryConfigurationWithCustomAttribute() throws Exception {

        AttributeBasedOrganizationDiscoveryHandlerRegistry.getInstance().addSupportedDiscoveryAttributeKey("custom");
        AttributeBasedOrganizationDiscoveryHandlerRegistry.getInstance().addSupportedDiscoveryAttributeKey(
                "emailDomain");
        List<ConfigProperty> configProperties = new ArrayList<>();
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, FALSE));
        configProperties.add(new ConfigProperty("custom.enable", TRUE));
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(configProperties);

        organizationConfigManagerImpl.addDiscoveryConfiguration(discoveryConfig);
        List<ConfigProperty> returnedConfigProperties =
                organizationConfigManagerImpl.getDiscoveryConfiguration().getConfigProperties();

        Assert.assertEquals(returnedConfigProperties.size(), 2);
        Assert.assertEquals(returnedConfigProperties.get(0).getKey(), EMAIL_DOMAIN_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(1).getKey(), "custom.enable");
        Assert.assertEquals(returnedConfigProperties.get(0).getValue(), FALSE);
        Assert.assertEquals(returnedConfigProperties.get(1).getValue(), TRUE);
    }

    @Test(priority = 6)
    public void testUpdateOrganizationConfiguration() throws Exception {

        List<ConfigProperty> configProperties = new ArrayList<>();
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        configProperties.add(new ConfigProperty(IS_CONSOLE_BRANDING_ENABLED, TRUE));
        OrganizationConfig organizationConfig = new OrganizationConfig(configProperties);

        organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);
        List<ConfigProperty> returnedConfigProperties =
                organizationConfigManagerImpl.getOrganizationConfiguration().getConfigProperties();

        Assert.assertEquals(returnedConfigProperties.size(), 2);
        Assert.assertEquals(returnedConfigProperties.get(0).getKey(), EMAIL_DOMAIN_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(0).getValue(), TRUE);
        Assert.assertEquals(returnedConfigProperties.get(1).getKey(), IS_CONSOLE_BRANDING_ENABLED);
        Assert.assertEquals(returnedConfigProperties.get(1).getValue(), TRUE);
    }

    @Test(priority = 7)
    public void testUpdateOrganizationConfigurationWithInvalidBrandingValue() throws Exception {

        try {
            List<ConfigProperty> configProperties = new ArrayList<>();
            configProperties.add(new ConfigProperty(IS_CONSOLE_BRANDING_ENABLED, "invalid"));
            OrganizationConfig organizationConfig = new OrganizationConfig(configProperties);

            organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);
            Assert.fail("Expected OrganizationConfigClientException was not thrown.");
        } catch (OrganizationConfigClientException e) {
            Assert.assertEquals(e.getMessage(), "Invalid organization configuration attribute values.");
        }
    }

    @Test(priority = 8)
    public void testUpdateOrganizationConfigurationWithInvalidDiscoveryConfig() throws Exception {

        try {
            List<ConfigProperty> configProperties = new ArrayList<>();
            configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, FALSE));
            configProperties.add(new ConfigProperty(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE, TRUE));
            OrganizationConfig organizationConfig = new OrganizationConfig(configProperties);

            organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);
            Assert.fail("Expected OrganizationConfigClientException was not thrown.");
        } catch (OrganizationConfigClientException e) {
            Assert.assertEquals(e.getMessage(), "Invalid organization discovery attribute values.");
        }
    }

    @Test(priority = 9)
    public void testUpdateOrganizationConfigurationWithDiscoveryAndBranding() throws Exception {

        List<ConfigProperty> configProperties = new ArrayList<>();
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        configProperties.add(new ConfigProperty(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE, TRUE));
        configProperties.add(new ConfigProperty(IS_CONSOLE_BRANDING_ENABLED, FALSE));
        OrganizationConfig organizationConfig = new OrganizationConfig(configProperties);

        organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);
        List<ConfigProperty> returnedConfigProperties =
                organizationConfigManagerImpl.getOrganizationConfiguration().getConfigProperties();

        Assert.assertEquals(returnedConfigProperties.size(), 3);
        Assert.assertEquals(returnedConfigProperties.get(0).getKey(), EMAIL_DOMAIN_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(0).getValue(), TRUE);
        Assert.assertEquals(returnedConfigProperties.get(1).getKey(), EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE);
        Assert.assertEquals(returnedConfigProperties.get(1).getValue(), TRUE);
        Assert.assertEquals(returnedConfigProperties.get(2).getKey(), IS_CONSOLE_BRANDING_ENABLED);
        Assert.assertEquals(returnedConfigProperties.get(2).getValue(), FALSE);
    }

    @Test(priority = 10)
    public void testUnifiedBehavior_DiscoveryUpdateReflectsInOrganizationConfig() throws Exception {

        List<ConfigProperty> orgConfigProperties = new ArrayList<>();
        orgConfigProperties.add(new ConfigProperty(IS_CONSOLE_BRANDING_ENABLED, TRUE));
        OrganizationConfig organizationConfig = new OrganizationConfig(orgConfigProperties);
        organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);

        List<ConfigProperty> discoveryConfigProperties = new ArrayList<>();
        discoveryConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        discoveryConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE, TRUE));
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(discoveryConfigProperties);
        organizationConfigManagerImpl.updateDiscoveryConfiguration(discoveryConfig);

        List<ConfigProperty> returnedOrgConfigProperties =
                organizationConfigManagerImpl.getOrganizationConfiguration().getConfigProperties();

        Assert.assertEquals(returnedOrgConfigProperties.size(), 3);
        Assert.assertTrue(returnedOrgConfigProperties.stream()
                .anyMatch(prop -> prop.getKey().equals(EMAIL_DOMAIN_ENABLE) && 
                        prop.getValue().equals(TRUE)));
        Assert.assertTrue(returnedOrgConfigProperties.stream()
                .anyMatch(prop -> prop.getKey().equals(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE) && 
                        prop.getValue().equals(TRUE)));
                Assert.assertTrue(returnedOrgConfigProperties.stream()
                .anyMatch(prop -> prop.getKey().equals(IS_CONSOLE_BRANDING_ENABLED) &&
                        prop.getValue().equals(TRUE)));
    }

    @Test(priority = 11)
    public void testUnifiedBehavior_OrganizationConfigUpdateReflectsInDiscoveryConfig() throws Exception {

        List<ConfigProperty> discoveryConfigProperties = new ArrayList<>();
        discoveryConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, FALSE));
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(discoveryConfigProperties);
        organizationConfigManagerImpl.updateDiscoveryConfiguration(discoveryConfig);

        List<ConfigProperty> orgConfigProperties = new ArrayList<>();
        orgConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        orgConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE, TRUE));
        orgConfigProperties.add(new ConfigProperty(IS_CONSOLE_BRANDING_ENABLED, FALSE));
        OrganizationConfig organizationConfig = new OrganizationConfig(orgConfigProperties);
        organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);

        List<ConfigProperty> returnedDiscoveryConfigProperties =
                organizationConfigManagerImpl.getDiscoveryConfiguration().getConfigProperties();

        Assert.assertEquals(returnedDiscoveryConfigProperties.size(), 2);
        Assert.assertTrue(returnedDiscoveryConfigProperties.stream()
                .anyMatch(prop -> prop.getKey().equals(EMAIL_DOMAIN_ENABLE) && 
                        prop.getValue().equals(TRUE)));
        Assert.assertTrue(returnedDiscoveryConfigProperties.stream()
                .anyMatch(prop -> prop.getKey().equals(EMAIL_DOMAIN_BASED_SELF_SIGNUP_ENABLE) && 
                        prop.getValue().equals(TRUE)));
    }

    @Test(priority = 12)
    public void testUnifiedBehavior_DiscoveryDeletionRemovesFromOrganizationConfig() throws Exception {

        List<ConfigProperty> orgConfigProperties = new ArrayList<>();
        orgConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        orgConfigProperties.add(new ConfigProperty(IS_CONSOLE_BRANDING_ENABLED, TRUE));
        OrganizationConfig organizationConfig = new OrganizationConfig(orgConfigProperties);
        organizationConfigManagerImpl.updateOrganizationConfiguration(organizationConfig);

        organizationConfigManagerImpl.deleteDiscoveryConfiguration();

        List<ConfigProperty> returnedOrgConfigProperties =
                organizationConfigManagerImpl.getOrganizationConfiguration().getConfigProperties();

        Assert.assertEquals(returnedOrgConfigProperties.size(), 1);
        Assert.assertEquals(returnedOrgConfigProperties.get(0).getKey(), IS_CONSOLE_BRANDING_ENABLED);
        Assert.assertEquals(returnedOrgConfigProperties.get(0).getValue(), TRUE);
    }

    @Test(priority = 13)
    public void testBackwardCompatibility_DiscoveryConfigStillWorksIndependently() throws Exception {

        List<ConfigProperty> discoveryConfigProperties = new ArrayList<>();
        discoveryConfigProperties.add(new ConfigProperty(EMAIL_DOMAIN_ENABLE, TRUE));
        DiscoveryConfig discoveryConfig = new DiscoveryConfig(discoveryConfigProperties);
        organizationConfigManagerImpl.addDiscoveryConfiguration(discoveryConfig);

        List<ConfigProperty> returnedDiscoveryConfigProperties =
                organizationConfigManagerImpl.getDiscoveryConfiguration().getConfigProperties();

        Assert.assertEquals(returnedDiscoveryConfigProperties.size(), 1);
        Assert.assertEquals(returnedDiscoveryConfigProperties.get(0).getKey(), EMAIL_DOMAIN_ENABLE);
        Assert.assertEquals(returnedDiscoveryConfigProperties.get(0).getValue(), TRUE);
    }
    
    @AfterClass
    public void tearDown() throws Exception {

        TestUtils.closeH2Base();
        mocks.close();
        identityDatabaseUtil.close();
        identityTenantUtil.close();
        privilegedCarbonContext.close();
    }

    private static ConfigurationManager setUpConfigurationManager(MockedStatic<IdentityTenantUtil> identityTenantUtil) {

        ConfigurationManagerComponentDataHolder.setUseCreatedTime(true);
        ConfigurationManagerConfigurationHolder configurationHolder = new ConfigurationManagerConfigurationHolder();
        ConfigurationDAO configurationDAO = new ConfigurationDAOImpl();
        configurationHolder.setConfigurationDAOS(Collections.singletonList(configurationDAO));
        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(any(Integer.class)))
                .thenReturn(SUPER_TENANT_DOMAIN_NAME);
        return new ConfigurationManagerImpl(configurationHolder);
    }

    private void mockCarbonContextForSuperTenant() {

        privilegedCarbonContext = mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext mockPrivilegedCarbonContext = mock(PrivilegedCarbonContext.class);

        privilegedCarbonContext.when(
                PrivilegedCarbonContext::getThreadLocalCarbonContext).thenReturn(mockPrivilegedCarbonContext);
        when(mockPrivilegedCarbonContext.getTenantDomain()).thenReturn(SUPER_TENANT_DOMAIN_NAME);
        when(mockPrivilegedCarbonContext.getTenantId()).thenReturn(SUPER_TENANT_ID);
        when(mockPrivilegedCarbonContext.getUsername()).thenReturn("admin");
    }

    private static void setUpCarbonHome() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
    }

}
