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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.management.authz.service.OrganizationManagementAuthorizationManager;
import org.wso2.carbon.identity.organization.management.service.dao.OrganizationManagementDAO;
import org.wso2.carbon.identity.organization.management.service.dao.impl.OrganizationManagementDAOImpl;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.management.service.internal.OrganizationManagementDataHolder;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.identity.organization.management.util.TestUtils;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.OrganizationStatus;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.OrganizationTypes.STRUCTURAL;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.OrganizationTypes.TENANT;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

@PrepareForTest({PrivilegedCarbonContext.class, OrganizationManagementDataHolder.class,
        OrganizationManagementAuthorizationManager.class, Utils.class})
public class OrganizationManagerImplTest extends PowerMockTestCase {

    private static final String ROOT = "ROOT";
    private static final String ORG1_NAME = "ABC Builders";
    private static final String ORG2_NAME = "XYZ Builders";
    private static final String ORG_DESCRIPTION = "This is a construction company.";
    private static final String NEW_ORG_NAME = "New Org";
    private static final String NEW_ORG_DESCRIPTION = "new sample description.";
    private static final String ORG_ATTRIBUTE_KEY = "country";
    private static final String ORG_ATTRIBUTE_VALUE = "Sri Lanka";
    private static final String ROOT_ORG_ID = "root_org_id";
    private static final String ORG1_ID = "org_id_1";
    private static final String ORG2_ID = "org_id_2";
    private static final String INVALID_PARENT_ID = "invalid_parent_id";
    private static final String INVALID_ORG_ID = "invalid_org_id";
    private static final int TENANT_ID = -1234;
    private static final String ERROR_MESSAGE = "message";
    private static final String ERROR_DESCRIPTION = "description";
    private static final String ERROR_CODE = "code";

    private OrganizationManagerImpl organizationManager;
    private OrganizationManagementDataHolder organizationManagementDataHolder;

    private final OrganizationManagementDAO organizationManagementDAO = new OrganizationManagementDAOImpl();

    @Mock
    private RealmService realmService;

    @Mock
    private UserRealm userRealm;

    @Mock
    private AuthorizationManager authorizationManager;

    @BeforeMethod
    public void setUp() throws Exception {

        organizationManager = new OrganizationManagerImpl();
        organizationManagementDataHolder = PowerMockito.spy(new OrganizationManagementDataHolder());
        PowerMockito.mockStatic(OrganizationManagementDataHolder.class);
        when(OrganizationManagementDataHolder.getInstance()).thenReturn(organizationManagementDataHolder);

        TestUtils.initiateH2Base();
        DataSource dataSource = TestUtils.mockDataSource();

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            Organization rootOrganization = getOrganization(ROOT_ORG_ID, ROOT, "this is the root organization.",
                    null, TENANT.toString());
            Organization organization1 = getOrganization(ORG1_ID, ORG1_NAME, ORG_DESCRIPTION, ROOT_ORG_ID,
                    STRUCTURAL.toString());
            Organization organization2 = getOrganization(ORG2_ID, ORG2_NAME, ORG_DESCRIPTION, ORG1_ID,
                    STRUCTURAL.toString());
            organizationManagementDAO.addOrganization(TENANT_ID, rootOrganization);
            organizationManagementDAO.addOrganization(TENANT_ID, organization1);
            organizationManagementDAO.addOrganization(TENANT_ID, organization2);
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {

        TestUtils.closeH2Base();
    }

    @Test
    public void testAddOrganization() throws Exception {

        Organization sampleOrganization = getOrganization(UUID.randomUUID().toString(), NEW_ORG_NAME, ORG_DESCRIPTION,
                ROOT_ORG_ID, STRUCTURAL.toString());
        mockCarbonContext();
        mockAuthorizationManager();
        when(authorizationManager.isUserAuthorized(anyString(), anyString(), anyString())).thenReturn(true);

        OrganizationManagementAuthorizationManager authorizationManager =
                PowerMockito.mock(OrganizationManagementAuthorizationManager.class);
        PowerMockito.mockStatic(OrganizationManagementAuthorizationManager.class);
        when(OrganizationManagementAuthorizationManager.getInstance()).thenReturn(authorizationManager);
        when(OrganizationManagementAuthorizationManager.getInstance().isUserAuthorized(anyString(), anyString(),
                anyString(), anyInt())).thenReturn(true);

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);

            Organization addedOrganization = organizationManager.addOrganization(sampleOrganization);
            assertNotNull(addedOrganization.getId(), "Created organization id cannot be null");
            assertEquals(addedOrganization.getName(), sampleOrganization.getName());
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationWithInvalidParentId() throws Exception {

        Organization sampleOrganization = getOrganization(UUID.randomUUID().toString(),
                NEW_ORG_NAME, ORG_DESCRIPTION, INVALID_PARENT_ID, STRUCTURAL.toString());
        mockCarbonContext();
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn
                    (new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.addOrganization(sampleOrganization);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationWithReservedName() throws Exception {

        Organization organization = getOrganization(UUID.randomUUID().toString(), ROOT, ORG_DESCRIPTION, ORG1_NAME,
                TENANT.toString());
        when(Utils.handleClientException(anyObject(), anyString())).
                thenReturn(new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.addOrganization(organization);
    }

    @DataProvider(name = "dataForAddOrganizationRequiredFieldsMissing")
    public Object[][] dataForAddOrganizationRequiredFieldsMissing() {

        return new Object[][]{

                {null, null},
                {StringUtils.EMPTY, StringUtils.EMPTY},
                {null, ORG1_ID},
                {StringUtils.EMPTY, ORG1_ID},
                {ORG1_NAME, null},
                {ORG1_NAME, StringUtils.EMPTY}
        };
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class,
            dataProvider = "dataForAddOrganizationRequiredFieldsMissing")
    public void testAddOrganizationRequiredFieldsMissing(String orgName, String parentId) throws Exception {

        Organization sampleOrganization = getOrganization(UUID.randomUUID().toString(),
                orgName, ORG_DESCRIPTION, parentId, TENANT.toString());
        when(Utils.handleClientException(anyObject(), anyString()))
                .thenReturn(new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.addOrganization(sampleOrganization);
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

        mockCarbonContext();
        Organization organization = getOrganization(UUID.randomUUID().toString(), NEW_ORG_NAME, ORG_DESCRIPTION,
                ROOT_ORG_ID, STRUCTURAL.toString());
        List<OrganizationAttribute> organizationAttributeList = new ArrayList<>();
        OrganizationAttribute organizationAttribute = new OrganizationAttribute(attributeKey, attributeValue);
        organizationAttributeList.add(organizationAttribute);
        organization.setAttributes(organizationAttributeList);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject())).thenReturn(new OrganizationManagementClientException
                    (ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.addOrganization(organization);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationDuplicateAttributeKeys() throws Exception {

        Organization organization = getOrganization(UUID.randomUUID().toString(), NEW_ORG_NAME, ORG_DESCRIPTION,
                ROOT_ORG_ID, STRUCTURAL.toString());
        mockCarbonContext();
        List<OrganizationAttribute> organizationAttributeList = new ArrayList<>();
        OrganizationAttribute organizationAttribute1 = new OrganizationAttribute(ORG_ATTRIBUTE_KEY,
                ORG_ATTRIBUTE_VALUE);
        OrganizationAttribute organizationAttribute2 = new OrganizationAttribute(ORG_ATTRIBUTE_KEY,
                ORG_ATTRIBUTE_VALUE);
        organizationAttributeList.add(organizationAttribute1);
        organizationAttributeList.add(organizationAttribute2);
        organization.setAttributes(organizationAttributeList);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject())).thenReturn(new OrganizationManagementClientException
                    (ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.addOrganization(organization);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationOrganizationNameTaken() throws Exception {

        Organization organization = getOrganization(UUID.randomUUID().toString(), ORG1_NAME, ORG_DESCRIPTION,
                ROOT_ORG_ID, STRUCTURAL.toString());
        mockCarbonContext();
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.addOrganization(organization);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testAddOrganizationUserNotAuthorized() throws Exception {

        Organization organization = getOrganization(UUID.randomUUID().toString(), NEW_ORG_NAME, ORG_DESCRIPTION,
                ROOT_ORG_ID, STRUCTURAL.toString());
        mockCarbonContext();
        mockAuthorizationManager();
        when(authorizationManager.isUserAuthorized(anyString(), anyString(), anyString())).thenReturn(false);
        OrganizationManagementAuthorizationManager authorizationManager =
                PowerMockito.mock(OrganizationManagementAuthorizationManager.class);
        PowerMockito.mockStatic(OrganizationManagementAuthorizationManager.class);
        when(OrganizationManagementAuthorizationManager.getInstance()).thenReturn(authorizationManager);
        when(OrganizationManagementAuthorizationManager.getInstance().isUserAuthorized(anyString(), anyString(),
                anyString(), anyInt())).thenReturn(false);

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.addOrganization(organization);
        }
    }

    @Test
    public void testGetOrganization() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);

            Organization organization = organizationManager.getOrganization(ORG1_ID, false);
            assertEquals(organization.getName(), ORG1_NAME);
            assertEquals(organization.getParent().getId(), ROOT_ORG_ID);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationWithEmptyOrganizationId() throws Exception {

        when(Utils.handleClientException(anyObject())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.getOrganization(StringUtils.EMPTY, false);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationNotExisting() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.getOrganization(INVALID_ORG_ID, false);
        }
    }

    @Test
    public void testGetOrganizationWithChildren() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            Organization organization = organizationManager.getOrganization(ORG1_ID, true);
            assertEquals(organization.getName(), ORG1_NAME);
            assertEquals(organization.getParent().getId(), ROOT_ORG_ID);
            assertEquals(organization.getChildOrganizations().size(), 1);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsWithUnsupportedFilterAttribute() throws Exception {

        mockCarbonContext();
        when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.getOrganizations(10, null, null, "ASC",
                "invalid_attribute co xyz");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsWithUnsupportedComplexQueryInFilter() throws Exception {

        mockCarbonContext();
        when(Utils.handleClientException(anyObject())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.getOrganizations(10, null, null, "ASC",
                "name co xyz or name co abc");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testGetOrganizationsWithInvalidPaginationAttribute() throws Exception {

        mockCarbonContext();
        when(Utils.handleClientException(anyObject())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.getOrganizations(10, "MjAyNjkzMjg=", null, "ASC", "name co xyz");
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganization() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            organizationManager.deleteOrganization(ORG2_ID);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            assertNull(organizationManager.getOrganization(ORG2_ID, false));
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganizationWithEmptyOrganizationId() throws Exception {

        when(Utils.handleClientException(anyObject())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.deleteOrganization(StringUtils.EMPTY);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testDeleteOrganizationWithChildOrganizations() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.deleteOrganization(ORG1_ID);
        }
    }

    @Test
    public void testPatchOrganization() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION,
                NEW_ORG_DESCRIPTION);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            Organization patchedOrganization = organizationManager.patchOrganization(ORG1_ID, patchOperations);
            assertNotNull(patchedOrganization);
            assertEquals(patchedOrganization.getDescription(), NEW_ORG_DESCRIPTION);
            assertEquals(patchedOrganization.getName(), ORG1_NAME);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithEmptyOrganizationId() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION,
                NEW_ORG_DESCRIPTION);
        patchOperations.add(patchOperation);
        when(Utils.handleClientException(anyObject())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.patchOrganization(StringUtils.EMPTY, patchOperations);
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithInvalidOrganizationId() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION,
                NEW_ORG_DESCRIPTION);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(INVALID_ORG_ID, patchOperations);
        }
    }

    @DataProvider(name = "invalidDataSet1ForPatchOrganization")
    public Object[][] invalidData1ForPatchOrganization() {

        return new Object[][]{

                {"invalid patch operation", PATCH_PATH_ORG_DESCRIPTION, "new value"},
                {StringUtils.EMPTY, PATCH_PATH_ORG_DESCRIPTION, "new value"},
                {null, PATCH_PATH_ORG_DESCRIPTION, "new value"},
                {null, null, null},
                {StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY},
                {PATCH_OP_ADD, "invalid patch path", "new value"},
        };
    }

    @Test(dataProvider = "invalidDataSet1ForPatchOrganization",
            expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithInvalidPatchRequest1(String op, String path, String value) throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(op, path, value);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(ORG1_ID, patchOperations);
        }
    }

    @DataProvider(name = "invalidDataSet2ForPatchOrganization")
    public Object[][] invalidData2ForPatchOrganization() {

        return new Object[][]{

                {PATCH_OP_ADD, StringUtils.EMPTY, "new value"},
                {PATCH_OP_ADD, null, "new value"},
                {PATCH_OP_ADD, PATCH_PATH_ORG_DESCRIPTION, null},
                {PATCH_OP_ADD, PATCH_PATH_ORG_ATTRIBUTES, "new value"}
        };
    }

    @Test(dataProvider = "invalidDataSet2ForPatchOrganization",
            expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithInvalidPatchRequest2(String op, String path, String value) throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(op, path, value);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(ORG1_ID, patchOperations);
        }
    }

    @DataProvider(name = "invalidDataSet3ForPatchOrganization")
    public Object[][] invalidData3ForPatchOrganization() {

        return new Object[][]{

                {PATCH_OP_REMOVE, PATCH_PATH_ORG_NAME, null},
        };
    }

    @Test(dataProvider = "invalidDataSet3ForPatchOrganization",
            expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationWithInvalidPatchRequest3(String op, String path, String value) throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(op, path, value);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(ORG1_ID, patchOperations);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationNameUnavailable() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_REPLACE, PATCH_PATH_ORG_NAME, ORG2_NAME);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(ORG1_ID, patchOperations);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationRemoveNonExistingAttribute() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_REMOVE, PATCH_PATH_ORG_ATTRIBUTES + "country",
                null);
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(ORG1_ID, patchOperations);
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testPatchOrganizationReplaceNonExistingAttribute() throws Exception {

        List<PatchOperation> patchOperations = new ArrayList<>();
        PatchOperation patchOperation = new PatchOperation(PATCH_OP_REPLACE, PATCH_PATH_ORG_ATTRIBUTES + "country",
                "India");
        patchOperations.add(patchOperation);
        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.patchOrganization(ORG1_ID, patchOperations);
        }
    }

    @Test
    public void testUpdateOrganization() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            Organization sampleOrganization = getOrganization(ORG1_ID, ORG1_NAME, NEW_ORG_DESCRIPTION, ROOT_ORG_ID,
                    STRUCTURAL.toString());
            Organization updatedOrganization = organizationManager.updateOrganization(ORG1_ID, ORG1_NAME,
                    sampleOrganization);
            assertEquals(NEW_ORG_DESCRIPTION, updatedOrganization.getDescription());
            assertEquals(ROOT_ORG_ID, updatedOrganization.getParent().getId());
        }
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testUpdateOrganizationWithEmptyOrganizationId() throws Exception {

        when(Utils.handleClientException(anyObject())).thenReturn(
                new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
        organizationManager.updateOrganization(StringUtils.EMPTY, ORG1_NAME,
                getOrganization(ORG1_ID, ORG1_NAME, NEW_ORG_DESCRIPTION, ROOT_ORG_ID, STRUCTURAL.toString()));
    }

    @Test(expectedExceptions = OrganizationManagementClientException.class)
    public void testUpdateOrganizationWithInvalidOrganizationId() throws Exception {

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(TestUtils.mockDataSource().getConnection()).thenReturn(spyConnection);
            when(Utils.handleClientException(anyObject(), anyString())).thenReturn(
                    new OrganizationManagementClientException(ERROR_MESSAGE, ERROR_DESCRIPTION, ERROR_CODE));
            organizationManager.updateOrganization(INVALID_ORG_ID, ORG1_NAME, getOrganization(INVALID_ORG_ID, ORG1_NAME,
                    NEW_ORG_DESCRIPTION, ROOT_ORG_ID, STRUCTURAL.toString()));
        }
    }

    private void mockCarbonContext() {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());

        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext privilegedCarbonContext = PowerMockito.mock(PrivilegedCarbonContext.class);
        when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        when(privilegedCarbonContext.getTenantDomain()).thenReturn(SUPER_TENANT_DOMAIN_NAME);
        when(privilegedCarbonContext.getTenantId()).thenReturn(SUPER_TENANT_ID);
        when(privilegedCarbonContext.getUsername()).thenReturn("admin");
    }

    private void mockAuthorizationManager() throws UserStoreException {

        organizationManagementDataHolder.setRealmService(realmService);
        when(organizationManagementDataHolder.getRealmService().getTenantUserRealm(anyInt()))
                .thenReturn(userRealm);
        when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
    }

    private Organization getOrganization(String id, String name, String description, String parent, String type) {

        Organization organization = new Organization();
        organization.setId(id);
        organization.setName(name);
        organization.setDescription(description);
        organization.setStatus(OrganizationStatus.ACTIVE.toString());
        organization.getParent().setId(parent);
        organization.setType(type);
        organization.setCreated(Instant.now());
        organization.setLastModified(Instant.now());
        return organization;
    }
}
