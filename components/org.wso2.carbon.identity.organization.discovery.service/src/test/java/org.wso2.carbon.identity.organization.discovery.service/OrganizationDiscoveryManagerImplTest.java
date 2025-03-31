/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.discovery.service;

import org.apache.commons.lang.StringUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.organization.config.service.OrganizationConfigManager;
import org.wso2.carbon.identity.organization.discovery.service.dao.OrganizationDiscoveryDAO;
import org.wso2.carbon.identity.organization.discovery.service.dao.OrganizationDiscoveryDAOImpl;
import org.wso2.carbon.identity.organization.discovery.service.internal.OrganizationDiscoveryServiceHolder;
import org.wso2.carbon.identity.organization.discovery.service.model.DiscoveryOrganizationsResult;
import org.wso2.carbon.identity.organization.discovery.service.model.OrgDiscoveryAttribute;
import org.wso2.carbon.identity.organization.discovery.service.util.TestUtils;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.SUPER_ORG_ID;

public class OrganizationDiscoveryManagerImplTest {

    private static final String DISCOVERY_ATTRIBUTE_TYPE = "emailDomain";
    private static final String DISCOVERY_ATTRIBUTE_VALUE = "wso2.lk";
    private static final String DISCOVERY_INPUT = "dewni@wso2.lk";
    private static final String WSO2_ORG_ID = "20084a8d-113f-4211-a0d5-efe36b082212";
    private static final String ABC_ORG_ID = "30084a8d-113f-4211-a0d5-efe36b082213";
    private static final String XYZ_ORG_ID = "40084a8d-113f-4211-a0d5-efe36b082214";
    private final OrganizationDiscoveryDAO organizationDiscoveryDAO = new OrganizationDiscoveryDAOImpl();
    private AuthenticationContext mockAuthenticationContext;

    @InjectMocks
    private OrganizationDiscoveryManagerImpl organizationDiscoveryManager;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private OrganizationConfigManager organizationConfigManager;

    @Mock
    AttributeBasedOrganizationDiscoveryHandler attributeBasedOrganizationDiscoveryHandler;

    @BeforeMethod
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        mockAuthenticationContext = mock(AuthenticationContext.class);

        TestUtils.initiateH2Base();
        TestUtils.mockDataSource();
        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationManager(organizationManager);
        OrganizationDiscoveryServiceHolder.getInstance().setOrganizationConfigManager(organizationConfigManager);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        OrganizationDiscoveryServiceHolder.getInstance().setAttributeBasedOrganizationDiscoveryHandler
                (attributeBasedOrganizationDiscoveryHandler);

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(SUPER_ORG_ID);

        List<OrgDiscoveryAttribute> discoveryAttributesForWso2 = new ArrayList<>();
        OrgDiscoveryAttribute wso2OrgDiscoveryAttribute = new OrgDiscoveryAttribute();
        wso2OrgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        wso2OrgDiscoveryAttribute.setValues(Arrays.asList("wso2.lk", "wso2.uk"));
        discoveryAttributesForWso2.add(wso2OrgDiscoveryAttribute);
        organizationDiscoveryDAO.addOrganizationDiscoveryAttributes(WSO2_ORG_ID, discoveryAttributesForWso2);

        List<OrgDiscoveryAttribute> discoveryAttributesForAbc = new ArrayList<>();
        OrgDiscoveryAttribute abcOrgDiscoveryAttribute = new OrgDiscoveryAttribute();
        abcOrgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        abcOrgDiscoveryAttribute.setValues(Arrays.asList("abc.com", "abc.io"));
        discoveryAttributesForAbc.add(abcOrgDiscoveryAttribute);
        organizationDiscoveryDAO.addOrganizationDiscoveryAttributes(ABC_ORG_ID, discoveryAttributesForAbc);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        TestUtils.closeH2Base();
    }

    @Test
    public void testAddOrganizationDiscoveryAttributes() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Arrays.asList("xyz.com", "xyz.gov"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, discoveryAttributes, true);
        Assert.assertFalse(organizationDiscoveryManager.isDiscoveryAttributeValueAvailable(SUPER_ORG_ID,
                DISCOVERY_ATTRIBUTE_TYPE, "xyz.com"));
        Assert.assertFalse(organizationDiscoveryManager.isDiscoveryAttributeValueAvailable(SUPER_ORG_ID,
                DISCOVERY_ATTRIBUTE_TYPE, "xyz.gov"));
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttrsForInvalidOrg() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Arrays.asList("xyz.com", "xyz.gov"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId("invalid-org-id")).thenReturn(SUPER_ORG_ID);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(WSO2_ORG_ID, discoveryAttributes, true);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttrsForAlreadyAddedOrg() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Arrays.asList("xyz.com", "xyz.gov"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(WSO2_ORG_ID)).thenReturn(SUPER_ORG_ID);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(WSO2_ORG_ID, discoveryAttributes, true);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddDuplicateDiscoveryAttributeTypes() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute1 = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute1.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute1.setValues(Collections.singletonList("xyz.com"));
        OrgDiscoveryAttribute orgDiscoveryAttribute2 = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute2.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute2.setValues(Collections.singletonList("xyz.gov"));
        discoveryAttributes.add(orgDiscoveryAttribute1);
        discoveryAttributes.add(orgDiscoveryAttribute2);

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, discoveryAttributes, true);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttributesWithEmptyAttributes() throws Exception {

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, new ArrayList<>(), true);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttributesWithUnsupportedAttributeType() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType("invalid-attribute-type");
        orgDiscoveryAttribute.setValues(Collections.singletonList("xyz.com"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, discoveryAttributes, false);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttributesWithDiscoveryConfigDisabled() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Collections.singletonList("xyz.com"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(false);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, discoveryAttributes, true);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttributesWithInvalidAttributeValueFormat() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Collections.singletonList("invalid-value"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(false);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, discoveryAttributes, true);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrgDiscoveryAttributesWithTakenAttributeValue() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Collections.singletonList("wso2.lk"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(XYZ_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(
                SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);

        organizationDiscoveryManager.addOrganizationDiscoveryAttributes(XYZ_ORG_ID, discoveryAttributes, true);
    }

    @Test
    public void testGetOrganizationDiscoveryAttributes() throws Exception {

        when(organizationManager.getPrimaryOrganizationId(WSO2_ORG_ID)).thenReturn(SUPER_ORG_ID);
        List<OrgDiscoveryAttribute> orgDiscoveryAttributes = organizationDiscoveryManager
                .getOrganizationDiscoveryAttributes(WSO2_ORG_ID, true);
        Assert.assertEquals(orgDiscoveryAttributes.size(), 1);
        Assert.assertEquals(orgDiscoveryAttributes.get(0).getType(), DISCOVERY_ATTRIBUTE_TYPE);
        Assert.assertTrue(orgDiscoveryAttributes.get(0).getValues().contains("wso2.uk"));
    }

    @Test
    public void testDeleteOrganizationDiscoveryAttributes() throws Exception {

        when(organizationManager.getPrimaryOrganizationId(WSO2_ORG_ID)).thenReturn(SUPER_ORG_ID);
        organizationDiscoveryManager.deleteOrganizationDiscoveryAttributes(WSO2_ORG_ID, true);
        Assert.assertEquals(organizationDiscoveryManager.getOrganizationDiscoveryAttributes(WSO2_ORG_ID, false)
                .size(), 0);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganizationDiscoveryAttributesWithRootOrgAccessFailure() throws Exception {

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(ABC_ORG_ID);
        when(organizationManager.getPrimaryOrganizationId(WSO2_ORG_ID)).thenReturn(SUPER_ORG_ID);
        organizationDiscoveryManager.deleteOrganizationDiscoveryAttributes(WSO2_ORG_ID, true);

        Assert.assertEquals(organizationDiscoveryManager.getOrganizationDiscoveryAttributes(WSO2_ORG_ID, true)
                .size(), 0);
    }

    @Test
    public void testUpdateOrganizationDiscoveryAttributes() throws Exception {

        List<OrgDiscoveryAttribute> discoveryAttributes = new ArrayList<>();
        OrgDiscoveryAttribute orgDiscoveryAttribute = new OrgDiscoveryAttribute();
        orgDiscoveryAttribute.setType(DISCOVERY_ATTRIBUTE_TYPE);
        orgDiscoveryAttribute.setValues(Collections.singletonList("wso2.new"));
        discoveryAttributes.add(orgDiscoveryAttribute);

        when(organizationManager.getPrimaryOrganizationId(WSO2_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);

        List<OrgDiscoveryAttribute> orgDiscoveryAttributeList = organizationDiscoveryManager
                .updateOrganizationDiscoveryAttributes(WSO2_ORG_ID, discoveryAttributes, true);
        Assert.assertEquals(orgDiscoveryAttributeList.size(), 1);
        Assert.assertEquals(orgDiscoveryAttributeList.get(0).getValues().size(), 1);
        Assert.assertTrue(orgDiscoveryAttributeList.get(0).getValues().contains("wso2.new"));
    }

    @Test
    public void testIsDiscoveryAttributeValueAvailable() throws Exception {

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setOrganizationId(WSO2_ORG_ID);
        when(organizationManager.getPrimaryOrganizationId(anyString())).thenReturn(SUPER_ORG_ID);
        Assert.assertTrue(
                organizationDiscoveryManager.isDiscoveryAttributeValueAvailable(DISCOVERY_ATTRIBUTE_TYPE, "wso2.com"));
    }

    @Test
    public void testIsDiscoveryAttributeValueNotAvailable() throws Exception {

        Assert.assertFalse(organizationDiscoveryManager.isDiscoveryAttributeValueAvailable(SUPER_ORG_ID,
                DISCOVERY_ATTRIBUTE_TYPE, "wso2.uk"));
    }

    @Test
    public void testGetOrganizationsDiscoveryAttributes() throws Exception {

        DiscoveryOrganizationsResult discoveryOrganizationsResult = organizationDiscoveryManager
                .getOrganizationsDiscoveryAttributes(null, null, null);

        Assert.assertEquals(discoveryOrganizationsResult.getOrganizations().size(), 2);
    }

    @Test
    public void testGetOrganizationsDiscoveryAttributesWithFilter() throws Exception {

        DiscoveryOrganizationsResult discoveryOrganizationsResult = organizationDiscoveryManager
                .getOrganizationsDiscoveryAttributes(1000, 0, "organizationName co A and organizationName co C");

        Assert.assertEquals(discoveryOrganizationsResult.getOrganizations().size(), 1);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsDiscoveryAttributesWithInvalidComplexFilterQuery() throws Exception {

        organizationDiscoveryManager.getOrganizationsDiscoveryAttributes(10, 0,
                "organizationName co A or organizationName co C");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsDiscoveryAttributesWithInvalidFilterOperator() throws Exception {

        organizationDiscoveryManager.getOrganizationsDiscoveryAttributes(10, 0, "organizationName ne ABC");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsDiscoveryAttributesWithUnsupportedFilterAttribute() throws Exception {

        organizationDiscoveryManager.getOrganizationsDiscoveryAttributes(10, 0, "name eq wso2.com");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsDiscoveryAttributesWithInvalidLimit() throws Exception {

        organizationDiscoveryManager.getOrganizationsDiscoveryAttributes(-10, 0, "organizationName eq ABC");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsDiscoveryAttributesWithInvalidOffset() throws Exception {

        organizationDiscoveryManager.getOrganizationsDiscoveryAttributes(10, -10, "organizationName eq ABC");
    }

    @Test
    public void testGetOrganizationIdByDiscoveryAttribute() throws Exception {

        when(organizationManager.getPrimaryOrganizationId(WSO2_ORG_ID)).thenReturn(SUPER_ORG_ID);
        when(attributeBasedOrganizationDiscoveryHandler.isDiscoveryConfigurationEnabled(SUPER_ORG_ID)).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.getType()).thenReturn(DISCOVERY_ATTRIBUTE_TYPE);
        when(attributeBasedOrganizationDiscoveryHandler.areAttributeValuesInValidFormat(anyList())).thenReturn(true);
        when(attributeBasedOrganizationDiscoveryHandler.extractAttributeValue(anyString())).
                thenReturn(DISCOVERY_ATTRIBUTE_VALUE);
        when(attributeBasedOrganizationDiscoveryHandler.extractAttributeValue(anyString(),
                any(AuthenticationContext.class))).thenReturn(DISCOVERY_ATTRIBUTE_VALUE);

        String organizationId =
                organizationDiscoveryManager.getOrganizationIdByDiscoveryAttribute(DISCOVERY_ATTRIBUTE_TYPE,
                        DISCOVERY_INPUT, SUPER_ORG_ID);
        Assert.assertEquals(organizationId, WSO2_ORG_ID);

        organizationId = organizationDiscoveryManager.getOrganizationIdByDiscoveryAttribute(DISCOVERY_ATTRIBUTE_TYPE,
                DISCOVERY_INPUT, SUPER_ORG_ID, mockAuthenticationContext);
        Assert.assertEquals(organizationId, WSO2_ORG_ID);

        when(attributeBasedOrganizationDiscoveryHandler.extractAttributeValue(anyString())).
        thenReturn(StringUtils.EMPTY);
        organizationId = organizationDiscoveryManager.getOrganizationIdByDiscoveryAttribute(DISCOVERY_ATTRIBUTE_TYPE,
                DISCOVERY_INPUT, SUPER_ORG_ID);
        Assert.assertNull(organizationId);

        when(attributeBasedOrganizationDiscoveryHandler.extractAttributeValue(anyString(),
                any(AuthenticationContext.class))).thenReturn(StringUtils.EMPTY);
        organizationId = organizationDiscoveryManager.getOrganizationIdByDiscoveryAttribute(DISCOVERY_ATTRIBUTE_TYPE,
                DISCOVERY_INPUT, SUPER_ORG_ID, mockAuthenticationContext);
        Assert.assertNull(organizationId);
    }
}
