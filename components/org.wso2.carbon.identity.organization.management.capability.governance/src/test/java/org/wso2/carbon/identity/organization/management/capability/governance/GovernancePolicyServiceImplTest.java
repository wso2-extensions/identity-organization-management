/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.carbon.identity.organization.management.capability.governance;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.internal.CarbonContextDataHolder;
import org.wso2.carbon.identity.organization.management.capability.governance.exception.GovernancePolicyMgtClientException;
import org.wso2.carbon.identity.organization.management.capability.governance.internal.GovernancePolicyDataHolder;
import org.wso2.carbon.identity.organization.management.capability.governance.model.GovernanceOrgSelected;
import org.wso2.carbon.identity.organization.management.capability.governance.model.OrgGovernancePolicy;
import org.wso2.carbon.identity.organization.management.capability.governance.model.Policy;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * Unit tests for {@link GovernancePolicyServiceImpl}.
 */
public class GovernancePolicyServiceImplTest {

    private static final String DB_NAME = "testGovernancePolicyMgt_db";
    private static final String H2_SCRIPT_NAME = "h2.sql";
    private static final Map<String, BasicDataSource> DATA_SOURCE_MAP = new HashMap<>();

    private static final String SUPER_ORG_ID = "super-org-id";
    private static final String ORG_L1_ID = "org-l1-id";

    private static final String RESOURCE_TYPE_APP = "APPLICATION";
    private static final String CAPABILITY_ADAPTIVE_SCRIPT = "ADAPTIVE_SCRIPT";

    @Mock
    private OrganizationManager organizationManager;

    private GovernancePolicyServiceImpl service;
    private MockedStatic<Utils> mockedUtils;
    private MockedStatic<OrganizationManagementUtil> mockedOrgMgmtUtil;

    @BeforeClass
    public void setUp() throws Exception {

        openMocks(this);
        service = new GovernancePolicyServiceImpl();
        initiateH2Base();
        mockDataSource();
        GovernancePolicyDataHolder.getInstance().setOrganizationManager(organizationManager);
        mockedUtils = mockStatic(Utils.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        mockedUtils.when(Utils::getOrganizationId).thenReturn(SUPER_ORG_ID);
        mockedOrgMgmtUtil = mockStatic(OrganizationManagementUtil.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        mockedOrgMgmtUtil.when(() -> OrganizationManagementUtil.isOrganization(SUPER_ORG_ID)).thenReturn(false);
    }

    @AfterClass
    public void tearDown() throws Exception {

        if (mockedUtils != null) {
            mockedUtils.close();
        }
        if (mockedOrgMgmtUtil != null) {
            mockedOrgMgmtUtil.close();
        }
        BasicDataSource dataSource = DATA_SOURCE_MAP.remove(DB_NAME);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    // -------------------------------------------------------------------------
    // Org-level CRUD
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndGetOrgGovernancePolicyFromRoot() throws Exception {

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, Policy.ALLOW_ALL);
        OrgGovernancePolicy created = service.addOrgGovernancePolicy(policy);

        Assert.assertTrue(created.getId() > 0);
        Assert.assertEquals(created.getGoverningOrgId(), SUPER_ORG_ID);
        Assert.assertEquals(created.getPolicy(), Policy.ALLOW_ALL);

        OrgGovernancePolicy fetched = service.getOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, CAPABILITY_ADAPTIVE_SCRIPT);
        Assert.assertEquals(fetched.getId(), created.getId());
        Assert.assertEquals(fetched.getCapability(), CAPABILITY_ADAPTIVE_SCRIPT);
    }

    @Test(dependsOnMethods = "testAddAndGetOrgGovernancePolicyFromRoot")
    public void testGetOrgGovernancePolicies() throws Exception {

        List<OrgGovernancePolicy> policies = service.getOrgGovernancePolicies(SUPER_ORG_ID);
        Assert.assertFalse(policies.isEmpty());
        Assert.assertTrue(policies.stream().allMatch(p -> SUPER_ORG_ID.equals(p.getGoverningOrgId())));
    }

    @Test(dependsOnMethods = "testGetOrgGovernancePolicies")
    public void testUpdateOrgGovernancePolicy() throws Exception {

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, Policy.ALLOW_IMMEDIATE);
        policy.setCapability("SOME_OTHER_CAPABILITY");
        service.addOrgGovernancePolicy(policy);

        OrgGovernancePolicy updates = new OrgGovernancePolicy();
        updates.setPolicy(Policy.ALLOW_ALL);
        OrgGovernancePolicy updated = service.updateOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "SOME_OTHER_CAPABILITY", updates);

        Assert.assertEquals(updated.getPolicy(), Policy.ALLOW_ALL);
    }

    @Test(dependsOnMethods = "testUpdateOrgGovernancePolicy")
    public void testDeleteOrgGovernancePolicy() throws Exception {

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, Policy.DENY_ALL);
        policy.setCapability("CAPABILITY_TO_DELETE");
        service.addOrgGovernancePolicy(policy);

        service.deleteOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "CAPABILITY_TO_DELETE");

        try {
            service.getOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "CAPABILITY_TO_DELETE");
            Assert.fail("Expected GovernancePolicyMgtClientException for deleted policy.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertEquals(e.getErrorCode(), "OCGM-60001");
        }
    }

    @Test
    public void testAddOrgPolicyFromChildOrgFails() throws Exception {

        OrgGovernancePolicy childPolicy = buildOrgPolicy(ORG_L1_ID, Policy.ALLOW_IMMEDIATE);
        try {
            service.addOrgGovernancePolicy(childPolicy);
            Assert.fail("Expected GovernancePolicyMgtClientException for non-primary org.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertEquals(e.getErrorCode(), "OCGM-60003");
        }
    }

    @Test
    public void testAddOrgPolicyWithSelectedType() throws Exception {

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, Policy.ALLOW_SELECTED);
        policy.setCapability("SELECTED_CAPABILITY");

        GovernanceOrgSelected selected = new GovernanceOrgSelected();
        selected.setTargetOrgId(ORG_L1_ID);
        policy.setSelectedOrgs(Collections.singletonList(selected));

        OrgGovernancePolicy created = service.addOrgGovernancePolicy(policy);
        Assert.assertTrue(created.getId() > 0);

        OrgGovernancePolicy fetched = service.getOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "SELECTED_CAPABILITY");
        Assert.assertEquals(fetched.getPolicy(), Policy.ALLOW_SELECTED);
        Assert.assertFalse(fetched.getSelectedOrgs().isEmpty());
        Assert.assertEquals(fetched.getSelectedOrgs().get(0).getTargetOrgId(), ORG_L1_ID);
    }

    // -------------------------------------------------------------------------
    // coversOrg helper tests
    // -------------------------------------------------------------------------

    @Test
    public void testCoversOrgAll() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicy(Policy.ALLOW_ALL);
        Assert.assertTrue(policy.coversOrg("any-org", false));
    }

    @Test
    public void testCoversOrgDeny() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicy(Policy.DENY_ALL);
        Assert.assertTrue(policy.coversOrg("any-org", false));
    }

    @Test
    public void testCoversOrgImmediate() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicy(Policy.ALLOW_IMMEDIATE);
        Assert.assertTrue(policy.coversOrg("any-org", true));
        Assert.assertFalse(policy.coversOrg("any-org", false));
    }

    @Test
    public void testCoversOrgSelected() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicy(Policy.ALLOW_SELECTED);
        GovernanceOrgSelected selected = new GovernanceOrgSelected();
        selected.setTargetOrgId("target-org");
        policy.setSelectedOrgs(Collections.singletonList(selected));

        Assert.assertTrue(policy.coversOrg("target-org", false));
        Assert.assertFalse(policy.coversOrg("other-org", false));
    }

    // -------------------------------------------------------------------------
    // Natural key lookups
    // -------------------------------------------------------------------------

    @Test
    public void testGetOrgGovernancePolicyByKey() throws Exception {

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, Policy.ALLOW_ALL);
        policy.setCapability("KEY_LOOKUP_CAPABILITY");
        service.addOrgGovernancePolicy(policy);

        OrgGovernancePolicy fetched = service.getOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "KEY_LOOKUP_CAPABILITY");
        Assert.assertEquals(fetched.getCapability(), "KEY_LOOKUP_CAPABILITY");
        Assert.assertEquals(fetched.getGoverningOrgId(), SUPER_ORG_ID);
    }

    @Test
    public void testGetOrgGovernancePolicyByKeyNotFound() {

        try {
            service.getOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "NONEXISTENT_CAPABILITY");
            Assert.fail("Expected 404.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertEquals(e.getErrorCode(), "OCGM-60001");
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGetOrgGovernancePolicyByKey")
    public void testUpdateOrgGovernancePolicyByKey() throws Exception {

        OrgGovernancePolicy updates = new OrgGovernancePolicy();
        updates.setPolicy(Policy.ALLOW_IMMEDIATE);

        OrgGovernancePolicy updated = service.updateOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "KEY_LOOKUP_CAPABILITY", updates);
        Assert.assertEquals(updated.getPolicy(), Policy.ALLOW_IMMEDIATE);
        Assert.assertEquals(updated.getGoverningOrgId(), SUPER_ORG_ID);
        Assert.assertEquals(updated.getCapability(), "KEY_LOOKUP_CAPABILITY");
    }

    @Test(dependsOnMethods = "testUpdateOrgGovernancePolicyByKey")
    public void testDeleteOrgGovernancePolicyByKey() throws Exception {

        service.deleteOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "KEY_LOOKUP_CAPABILITY");

        try {
            service.getOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "KEY_LOOKUP_CAPABILITY");
            Assert.fail("Expected 404 after delete.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertEquals(e.getErrorCode(), "OCGM-60001");
        }
    }

    @Test
    public void testDeleteOrgGovernancePolicyByKeyNotFound() {

        try {
            service.deleteOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "NONEXISTENT");
            Assert.fail("Expected 404.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertEquals(e.getErrorCode(), "OCGM-60001");
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 409 Conflict
    // -------------------------------------------------------------------------

    @Test
    public void testAddOrgPolicyConflict() throws Exception {

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, Policy.ALLOW_ALL);
        policy.setCapability("CONFLICT_CAP");
        service.addOrgGovernancePolicy(policy);

        try {
            OrgGovernancePolicy duplicate = buildOrgPolicy(SUPER_ORG_ID, Policy.ALLOW_IMMEDIATE);
            duplicate.setCapability("CONFLICT_CAP");
            service.addOrgGovernancePolicy(duplicate);
            Assert.fail("Expected 409 for duplicate org policy.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertEquals(e.getErrorCode(), "OCGM-60002");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OrgGovernancePolicy buildOrgPolicy(String governingOrgId, Policy type) {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setGoverningOrgId(governingOrgId);
        policy.setResourceType(RESOURCE_TYPE_APP);
        policy.setCapability(CAPABILITY_ADAPTIVE_SCRIPT);
        policy.setPolicy(type);
        return policy;
    }

    // -------------------------------------------------------------------------
    // H2 / DataSource setup
    // -------------------------------------------------------------------------

    private void initiateH2Base() throws Exception {

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("username");
        dataSource.setPassword("password");
        dataSource.setUrl("jdbc:h2:mem:test" + DB_NAME);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("select 1");
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().executeUpdate(
                    "RUNSCRIPT FROM '" + getFilePath(H2_SCRIPT_NAME) + "'");
        }
        DATA_SOURCE_MAP.put(DB_NAME, dataSource);
    }

    private void mockDataSource() throws Exception {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH,
                Paths.get(carbonHome, "repository", "conf").toString());

        DataSource dataSource = DATA_SOURCE_MAP.get(DB_NAME);

        setStatic(DatabaseUtil.class.getDeclaredField("dataSource"), dataSource);

        Field carbonContextHolderField =
                CarbonContext.getThreadLocalCarbonContext().getClass().getDeclaredField("carbonContextHolder");
        carbonContextHolderField.setAccessible(true);
        CarbonContextDataHolder carbonContextHolder =
                (CarbonContextDataHolder) carbonContextHolderField.get(CarbonContext.getThreadLocalCarbonContext());
        carbonContextHolder.setUserRealm(mock(UserRealm.class));

        setStatic(Utils.class.getDeclaredField("dataSource"), dataSource);
    }

    private static void setStatic(Field field, Object newValue) throws Exception {

        field.setAccessible(true);
        field.set(null, newValue);
    }

    private static String getFilePath(String fileName) {

        if (StringUtils.isNotBlank(fileName)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "dbscripts",
                    fileName).toString();
        }
        throw new IllegalArgumentException("DB Script file name cannot be empty.");
    }
}
