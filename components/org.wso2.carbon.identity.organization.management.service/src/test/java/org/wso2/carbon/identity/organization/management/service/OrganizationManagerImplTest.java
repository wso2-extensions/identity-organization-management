/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

package org.wso2.carbon.identity.organization.management.service;

import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.core.ServiceURL;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationManager;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.internal.OrganizationManagementDataHolder;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

@PrepareForTest({PrivilegedCarbonContext.class, ServiceURLBuilder.class, ServiceURL.class,
        OrganizationManagementDataHolder.class,  OrganizationManagementAuthorizationManager.class})
public class OrganizationManagerImplTest extends PowerMockTestCase {

    private static final String ORG_NAME = "ABC Builders";
    private static final String ORG_DESCRIPTION = "This is a construction company.";
    private static final String ROOT = "ROOT";
    private static final String PARENT_ID = "parent_id_123";
    private static final String INVALID_PARENT_ID = "invalid_parent_id";
    private static final String ORG_ID = "org_id_123";
    private static final String ORG_ATTRIBUTE_KEY = "country";
    private static final String ORG_ATTRIBUTE_VALUE = "Sri Lanka";
    private static final String NEW_ORG_DESCRIPTION = "new sample description.";

    private OrganizationManagerImpl organizationManager;
    private OrganizationManagementDataHolder organizationManagementDataHolder;

    @Mock
    private OrganizationManagementDAO organizationManagementDAO;

    @Mock
    private RealmService realmService;

    @Mock
    private UserRealm userRealm;

    @Mock
    private AuthorizationManager authorizationManager;

    @BeforeMethod
    public void setUp() {

        organizationManager = new OrganizationManagerImpl();

        organizationManagementDataHolder = spy(new OrganizationManagementDataHolder());
        mockStatic(OrganizationManagementDataHolder.class);
        organizationManagementDataHolder.setOrganizationManagementDAO(organizationManagementDAO);
        when(OrganizationManagementDataHolder.getInstance()).thenReturn(organizationManagementDataHolder);
    }

    @AfterMethod
    public void tearDown() {

    }

    @Test
    public void testAddOrganization() throws Exception {

        Organization sampleOrganization = getOrganization(ORG_NAME, ROOT);
        mockCarbonContext();

        mockAuthorizationManager();
        when(authorizationManager.isUserAuthorized(anyString(), anyString(), anyString())).thenReturn(true);

        OrganizationManagementAuthorizationManager authorizationManager =
                mock(OrganizationManagementAuthorizationManager.class);
        mockStatic(OrganizationManagementAuthorizationManager.class);
        when(OrganizationManagementAuthorizationManager.getInstance()).thenReturn(authorizationManager);
        when(OrganizationManagementAuthorizationManager.getInstance().isUserAuthorized(anyString(), anyString(),
                anyString(), anyInt())).thenReturn(true);

        mockBuildURI();

        organizationManager.addOrganization(sampleOrganization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationWithInvalidParentId() throws Exception {

        Organization sampleOrganization = getOrganization(ORG_NAME, INVALID_PARENT_ID);
        mockCarbonContext();

        organizationManager.addOrganization(sampleOrganization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationWithReservedName() throws Exception {

        Organization organization = getOrganization(ROOT, PARENT_ID);
        organizationManager.addOrganization(organization);
    }

    @DataProvider(name = "dataForAddOrganizationRequiredFieldsMissing")
    public Object[][] dataForAddOrganizationRequiredFieldsMissing() {

        return new Object[][]{

                {null, null},
                {StringUtils.EMPTY, StringUtils.EMPTY},
                {null, PARENT_ID},
                {StringUtils.EMPTY, PARENT_ID},
                {ORG_NAME, null},
                {ORG_NAME, StringUtils.EMPTY}
        };
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
            dataProvider = "dataForAddOrganizationRequiredFieldsMissing")
    public void testAddOrganizationRequiredFieldsMissing(String orgName, String parentId) throws Exception {

        Organization organization = getOrganization(orgName, parentId);
        organizationManager.addOrganization(organization);
    }

    @DataProvider(name = "dataForAddOrganizationInvalidOrganizationAttributes")
    public Object[][] dataForAddOrganizationInvalidOrganizationAttributes() {

        return new Object[][]{

                {null, null},
                {StringUtils.EMPTY, StringUtils.EMPTY},
                {null, StringUtils.EMPTY},
                {StringUtils.EMPTY, null},
                {ORG_ATTRIBUTE_KEY, null},
                {null, ORG_ATTRIBUTE_VALUE},
                {StringUtils.EMPTY, ORG_ATTRIBUTE_VALUE}
        };
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
            dataProvider = "dataForAddOrganizationInvalidOrganizationAttributes")
    public void testAddOrganizationInvalidAttributes(String attributeKey, String attributeValue) throws Exception {

        Organization organization = getOrganization(ORG_NAME, ROOT);
        List<OrganizationAttribute> organizationAttributeList = new ArrayList<>();
        OrganizationAttribute organizationAttribute = new OrganizationAttribute(attributeKey, attributeValue);
        organizationAttributeList.add(organizationAttribute);
        organization.setAttributes(organizationAttributeList);
        mockCarbonContext();
        organizationManager.addOrganization(organization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationDuplicateAttributeKeys() throws Exception {

        Organization organization = getOrganization(ORG_NAME, ROOT);
        mockCarbonContext();
        List<OrganizationAttribute> organizationAttributeList = new ArrayList<>();
        OrganizationAttribute organizationAttribute1 = new OrganizationAttribute(ORG_ATTRIBUTE_KEY,
                ORG_ATTRIBUTE_VALUE);
        OrganizationAttribute organizationAttribute2 = new OrganizationAttribute(ORG_ATTRIBUTE_KEY,
                ORG_ATTRIBUTE_VALUE);
        organizationAttributeList.add(organizationAttribute1);
        organizationAttributeList.add(organizationAttribute2);
        organization.setAttributes(organizationAttributeList);
        organizationManager.addOrganization(organization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationOrganizationNameTaken() throws Exception {

        Organization organization = getOrganization(ORG_NAME, ROOT);
        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistByName(anyInt(),
                anyString(), anyString())).thenReturn(true);

        organizationManager.addOrganization(organization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddRootOrganizationUserNotAuthorized() throws Exception {

        Organization sampleOrganization = getOrganization(ORG_NAME, ROOT);
        mockCarbonContext();

        mockAuthorizationManager();
        when(authorizationManager.isUserAuthorized(anyString(), anyString(), anyString())).thenReturn(false);

        organizationManager.addOrganization(sampleOrganization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationUserNotAuthorized() throws Exception {

        Organization sampleOrganization = getOrganization(ORG_NAME, ROOT);
        mockCarbonContext();

        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganizationIdByName(anyInt(),
                anyString(), anyString())).thenReturn(PARENT_ID);

        OrganizationManagementAuthorizationManager authorizationManager =
                mock(OrganizationManagementAuthorizationManager.class);
        mockStatic(OrganizationManagementAuthorizationManager.class);
        when(OrganizationManagementAuthorizationManager.getInstance()).thenReturn(authorizationManager);
        when(OrganizationManagementAuthorizationManager.getInstance().isUserAuthorized(anyString(), anyString(),
                anyString(), anyInt())).thenReturn(false);

        organizationManager.addOrganization(sampleOrganization);
    }

    @Test
    public void testGetOrganization() throws Exception {

        Organization sampleOrganization = getOrganization(ORG_NAME, PARENT_ID);
        mockCarbonContext();

        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganization(anyInt(), anyString(),
                anyString())).thenReturn(sampleOrganization);

        mockBuildURI();

        Organization organization = organizationManager.getOrganization(ORG_ID, false);
        Assert.assertEquals(organization.getName(), ORG_NAME);
        Assert.assertEquals(organization.getParent().getId(), PARENT_ID);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationWithEmptyOrganizationId() throws Exception {

        organizationManager.getOrganization(StringUtils.EMPTY, false);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationNotExisting() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganization(anyInt(), anyString(),
                anyString())).thenReturn(null);
        organizationManager.getOrganization(ORG_ID, false);
    }

    @Test
    public void testGetOrganizationWithChildren() throws Exception {

        Organization sampleOrganization = getOrganization(ORG_NAME, PARENT_ID);
        mockCarbonContext();

        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganization(anyInt(), anyString(),
                anyString())).thenReturn(sampleOrganization);
        List<String> childOrganizationIds = new ArrayList<>();
        childOrganizationIds.add("child_org_id_1");
        childOrganizationIds.add("child_org_id_2");
        when(organizationManagementDataHolder.getOrganizationManagementDAO().getChildOrganizationIds(anyInt(),
                anyString(), anyString(), anyObject())).thenReturn(childOrganizationIds);

        mockBuildURI();

        Organization organization = organizationManager.getOrganization(ORG_ID, true);
        Assert.assertEquals(organization.getName(), ORG_NAME);
        Assert.assertEquals(organization.getParent().getId(), PARENT_ID);
        Assert.assertEquals(organization.getChildOrganizations().size(), childOrganizationIds.size());
    }

    @DataProvider(name = "dataForGetOrganizations")
    public Object[][] dataForGetOrganizations() {

        return new Object[][]{

                {null, null, null},
                {null, null, "name co rs"},
                {"MjAyMS0xMi0yMiAwNDoyMjowOS4xNjkzMjg=", null, null},
                {null, "MjAyMS0xMi0yMiAwNDoyMjowOS4xNjkzMjg=", null},
                {"MjAyMS0xMi0yMiAwNDoyMjowOS4xNjkzMjg=", null, "name co rs"},
                {null, "MjAyMS0xMi0yMiAwNDoyMjowOS4xNjkzMjg=", "name co rs"},
                {"MjAyMS0xMi0yMiAwNDoyMjowOS4xNjkzMjg=", null, "name co il and name co rs"},
        };
    }

    @Test(dataProvider = "dataForGetOrganizations")
    public void testGetOrganizations(String after, String before, String filter) throws Exception {

        mockCarbonContext();
        List<BasicOrganization> organizations = new ArrayList<>();
        organizations.add(getBasicOrganization("ABC Builders", "40c55d3a-c525-4630-8114-432168cf478d",
                "2021-12-21T05:18:31.015696Z"));
        organizations.add(getBasicOrganization("XYZ Motors", "35c55d3a-j525-4030-9114-802268cf472h",
                "2021-12-25T02:12:17.018892Z"));
        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganizations(anyInt(), anyInt(),
                anyString(), anyString(), anyObject())).thenReturn(organizations);
        List<BasicOrganization> organizationList =
                organizationManager.getOrganizations(10, after, before, "ASC", filter);

        Assert.assertEquals(organizationList.size(), 2);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsWithUnsupportedFilterAttribute() throws Exception {

        mockCarbonContext();
        organizationManager.getOrganizations(10, null, null, "ASC",
                "invalid_attribute co xyz");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsWithUnsupportedComplexQueryInFilter() throws Exception {

        mockCarbonContext();
        organizationManager.getOrganizations(10, null, null, "ASC",
                "name co xyz or name co abc");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsWithInvalidPaginationAttribute() throws Exception {

        mockCarbonContext();
        organizationManager.getOrganizations(10, "MjAyNjkzMjg=", null, "ASC", "name co xyz");
    }

    @DataProvider(name = "dataForDeleteOrganization")
    public Object[][] dataForDeleteOrganization() {

        return new Object[][]{

                {true},
                {false}
        };
    }

    @Test(dataProvider = "dataForDeleteOrganization")
    public void testDeleteOrganization(boolean force) throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        organizationManager.deleteOrganization(ORG_ID, force);
    }

    @Test(dataProvider = "dataForDeleteOrganization", expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganizationWithEmptyOrganizationId(boolean force) throws Exception {

        mockCarbonContext();
        organizationManager.deleteOrganization(StringUtils.EMPTY, force);
    }

    @Test(dataProvider = "dataForDeleteOrganization", expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganizationWithInvalidOrganizationId(boolean force) throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(false);
        organizationManager.deleteOrganization(ORG_ID, force);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganizationWithChildOrganizations() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().hasChildOrganizations(anyString(),
                anyString())).thenReturn(true);
        organizationManager.deleteOrganization(ORG_ID, false);
    }

    @Test
    public void testForceDeleteOrganizationWithChildOrganizations() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().hasChildOrganizations(anyString(),
                anyString())).thenReturn(true);
        organizationManager.deleteOrganization(ORG_ID, true);
    }

    @Test
    public void testPatchOrganization() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        Organization sampleOrganization = getOrganization(ORG_NAME, PARENT_ID);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganization(anyInt(), anyString(),
                anyString())).thenReturn(sampleOrganization);
        mockBuildURI();
        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION,
                NEW_ORG_DESCRIPTION);
        patchOperations.add(patchOperation);
        Organization patchedOrganization = organizationManager.patchOrganization(ORG_ID, patchOperations);
        Assert.assertNotNull(patchedOrganization);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithEmptyOrganizationId() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION,
                NEW_ORG_DESCRIPTION);
        patchOperations.add(patchOperation);
        organizationManager.patchOrganization(StringUtils.EMPTY, patchOperations);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithInvalidOrganizationId() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(false);
        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION,
                NEW_ORG_DESCRIPTION);
        patchOperations.add(patchOperation);
        organizationManager.patchOrganization(ORG_ID, patchOperations);
    }

    @DataProvider(name = "invalidDataForPatchOrganization")
    public Object[][] invalidDataForPatchOrganization() {

        return new Object[][]{

                {"invalid patch operation", PATCH_PATH_ORG_DESCRIPTION, "new value"},
                {StringUtils.EMPTY, PATCH_PATH_ORG_DESCRIPTION, "new value"},
                {null, PATCH_PATH_ORG_DESCRIPTION, "new value"},
                {PATCH_OP_ADD, StringUtils.EMPTY, "new value"},
                {PATCH_OP_ADD, null, "new value"},
                {null, null, null},
                {StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY},
                {PATCH_OP_ADD, "invalid patch path", "new value"},
                {PATCH_OP_REMOVE, PATCH_PATH_ORG_NAME, null},
                {PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION, null},
                {PATCH_OP_ADD, PATCH_PATH_ORG_ATTRIBUTES, "new value"}
        };
    }

    @Test(dataProvider = "invalidDataForPatchOrganization",
            expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithInvalidPatchRequest(String op, String path, String value) throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(op, path, value);
        patchOperations.add(patchOperation);
        organizationManager.patchOrganization(ORG_ID, patchOperations);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationNameUnavailable() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistByName(anyInt(),
                anyString(), anyString())).thenReturn(true);
        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_REPLACE, PATCH_PATH_ORG_NAME,
                "new value");
        patchOperations.add(patchOperation);
        organizationManager.patchOrganization(ORG_ID, patchOperations);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationRemoveNonExistingAttribute() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isAttributeExistByKey(anyString(),
                anyString(), anyString())).thenReturn(false);
        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_REMOVE, PATCH_PATH_ORG_ATTRIBUTES + "country",
                null);
        patchOperations.add(patchOperation);
        organizationManager.patchOrganization(ORG_ID, patchOperations);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationReplaceNonExistingAttribute() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isAttributeExistByKey(anyString(),
                anyString(), anyString())).thenReturn(false);
        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_REPLACE, PATCH_PATH_ORG_ATTRIBUTES + "country",
                "India");
        patchOperations.add(patchOperation);
        organizationManager.patchOrganization(ORG_ID, patchOperations);
    }

    @Test
    public void testUpdateOrganization() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(true);
        Organization sampleOrganization = getOrganization(ORG_NAME, PARENT_ID);
        when(organizationManagementDataHolder.getOrganizationManagementDAO().getOrganization(anyInt(), anyString(),
                anyString())).thenReturn(sampleOrganization);
        mockBuildURI();
        organizationManager.updateOrganization(ORG_ID, ORG_NAME, getOrganization(ORG_NAME, PARENT_ID));
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testUpdateOrganizationWithEmptyOrganizationId() throws Exception {

        mockCarbonContext();
        organizationManager.updateOrganization(StringUtils.EMPTY, ORG_NAME, getOrganization(ORG_NAME, PARENT_ID));
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testUpdateOrganizationWithInvalidOrganizationId() throws Exception {

        mockCarbonContext();
        when(organizationManagementDataHolder.getOrganizationManagementDAO().isOrganizationExistById(anyInt(),
                anyString(), anyString())).thenReturn(false);
        organizationManager.updateOrganization(ORG_ID, ORG_NAME
                , getOrganization(ORG_NAME, PARENT_ID));
    }

    private void mockCarbonContext() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());

        mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext privilegedCarbonContext = mock(PrivilegedCarbonContext.class);
        when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        when(privilegedCarbonContext.getTenantDomain()).thenReturn(SUPER_TENANT_DOMAIN_NAME);
        when(privilegedCarbonContext.getTenantId()).thenReturn(SUPER_TENANT_ID);
        when(privilegedCarbonContext.getUsername()).thenReturn("admin");
    }

    private void mockBuildURI() throws URLBuilderException {

        ServiceURLBuilder serviceURLBuilder = mock(ServiceURLBuilder.class);
        mockStatic(ServiceURLBuilder.class);
        when(ServiceURLBuilder.create()).thenReturn(serviceURLBuilder);
        ServiceURL serviceURL = mock(ServiceURL.class);
        mockStatic(ServiceURL.class);
        when(ServiceURLBuilder.create().addPath(anyString())).thenReturn(serviceURLBuilder);
        when(ServiceURLBuilder.create().addPath(anyString()).build()).thenReturn(serviceURL);
    }

    private void mockAuthorizationManager() throws UserStoreException {

        organizationManagementDataHolder.setRealmService(realmService);
        when(organizationManagementDataHolder.getRealmService().getTenantUserRealm(anyInt())).thenReturn(userRealm);
        when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
    }

    private Organization getOrganization(String name, String parent) {

        Organization organization = new Organization();
        organization.setName(name);
        organization.setDescription(ORG_DESCRIPTION);
        organization.getParent().setId(parent);
        return organization;
    }

    private BasicOrganization getBasicOrganization(String name, String id, String created) {

        BasicOrganization organization = new BasicOrganization();
        organization.setName(name);
        organization.setId(id);
        organization.setCreated(created);
        return organization;
    }
}
