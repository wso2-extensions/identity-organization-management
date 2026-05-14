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
import org.wso2.carbon.identity.organization.management.capability.governance.model.PolicyType;
import org.wso2.carbon.identity.organization.management.capability.governance.model.ResourceGovernancePolicy;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
    private static final String ORG_L2_ID = "org-l2-id";
    private static final String ORG_SELECTED_ID = "org-selected-id";

    private static final String RESOURCE_TYPE_APP = "APPLICATION";
    private static final String CAPABILITY_ADAPTIVE_SCRIPT = "ADAPTIVE_SCRIPT";
    private static final String RESOURCE_ID_APP1 = "app-resource-id-1";

    @Mock
    private OrganizationManager organizationManager;

    private GovernancePolicyServiceImpl service;

    @BeforeClass
    public void setUp() throws Exception {

        service = new GovernancePolicyServiceImpl();
        openMocks(this);
        initiateH2Base();
        mockDataSource();
        GovernancePolicyDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    @AfterClass
    public void tearDown() throws Exception {

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

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.ALL, true);
        OrgGovernancePolicy created = service.addOrgGovernancePolicy(policy);

        Assert.assertTrue(created.getId() > 0);
        Assert.assertEquals(created.getGoverningOrgId(), SUPER_ORG_ID);
        Assert.assertEquals(created.getPolicyType(), PolicyType.ALL);
        Assert.assertTrue(created.isAllowOverride());

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

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.IMMEDIATE, false);
        policy.setCapability("SOME_OTHER_CAPABILITY");
        service.addOrgGovernancePolicy(policy);

        OrgGovernancePolicy updates = new OrgGovernancePolicy();
        updates.setPolicyType(PolicyType.ALL);
        updates.setAllowOverride(true);
        OrgGovernancePolicy updated = service.updateOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "SOME_OTHER_CAPABILITY", updates);

        Assert.assertEquals(updated.getPolicyType(), PolicyType.ALL);
        Assert.assertTrue(updated.isAllowOverride());
    }

    @Test(dependsOnMethods = "testUpdateOrgGovernancePolicy")
    public void testDeleteOrgGovernancePolicy() throws Exception {

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.DENY, false);
        policy.setCapability("CAPABILITY_TO_DELETE");
        service.addOrgGovernancePolicy(policy);

        service.deleteOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "CAPABILITY_TO_DELETE");

        try {
            service.getOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "CAPABILITY_TO_DELETE");
            Assert.fail("Expected GovernancePolicyMgtClientException for deleted policy.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        }
    }

    @Test
    public void testAddOrgPolicyFromChildWithOverridePermission() throws Exception {

        when(organizationManager.isPrimaryOrganization(ORG_L1_ID)).thenReturn(false);
        when(organizationManager.getAncestorOrganizationIds(ORG_L1_ID))
                .thenReturn(Arrays.asList(ORG_L1_ID, SUPER_ORG_ID));

        OrgGovernancePolicy childPolicy = buildOrgPolicy(ORG_L1_ID, PolicyType.IMMEDIATE, false);
        OrgGovernancePolicy created = service.addOrgGovernancePolicy(childPolicy);
        Assert.assertTrue(created.getId() > 0);
    }

    @Test
    public void testAddOrgPolicyFromChildWithoutOverridePermissionFails() throws Exception {

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);
        OrgGovernancePolicy denyPolicy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.DENY, false);
        denyPolicy.setCapability("NO_OVERRIDE_CAPABILITY");
        service.addOrgGovernancePolicy(denyPolicy);

        when(organizationManager.isPrimaryOrganization(ORG_L1_ID)).thenReturn(false);
        when(organizationManager.getAncestorOrganizationIds(ORG_L1_ID))
                .thenReturn(Arrays.asList(ORG_L1_ID, SUPER_ORG_ID));

        OrgGovernancePolicy childPolicy = buildOrgPolicy(ORG_L1_ID, PolicyType.ALL, false);
        childPolicy.setCapability("NO_OVERRIDE_CAPABILITY");

        try {
            service.addOrgGovernancePolicy(childPolicy);
            Assert.fail("Expected GovernancePolicyMgtClientException for override not permitted.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60003"));
        }
    }

    @Test
    public void testAddOrgPolicyWithSelectedType() throws Exception {

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.SELECTED, true);
        policy.setCapability("SELECTED_CAPABILITY");

        GovernanceOrgSelected selected = new GovernanceOrgSelected();
        selected.setTargetOrgId(ORG_L1_ID);
        selected.setAllowOverride(false);
        policy.setSelectedOrgs(Collections.singletonList(selected));

        OrgGovernancePolicy created = service.addOrgGovernancePolicy(policy);
        Assert.assertTrue(created.getId() > 0);

        OrgGovernancePolicy fetched = service.getOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "SELECTED_CAPABILITY");
        Assert.assertEquals(fetched.getPolicyType(), PolicyType.SELECTED);
        Assert.assertFalse(fetched.getSelectedOrgs().isEmpty());
        Assert.assertEquals(fetched.getSelectedOrgs().get(0).getTargetOrgId(), ORG_L1_ID);
    }

    // -------------------------------------------------------------------------
    // Resource-level CRUD
    // -------------------------------------------------------------------------

    @Test
    public void testAddAndGetResourceGovernancePolicy() throws Exception {

        ResourceGovernancePolicy policy = buildResourcePolicy(SUPER_ORG_ID, PolicyType.ALL, true);
        ResourceGovernancePolicy created = service.addResourceGovernancePolicy(policy);

        Assert.assertTrue(created.getId() > 0);
        Assert.assertEquals(created.getGoverningOrgId(), SUPER_ORG_ID);
        Assert.assertEquals(created.getPolicyType(), PolicyType.ALL);

        ResourceGovernancePolicy fetched = service.getResourceGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, RESOURCE_ID_APP1, CAPABILITY_ADAPTIVE_SCRIPT);
        Assert.assertEquals(fetched.getId(), created.getId());
        Assert.assertEquals(fetched.getResourceId(), RESOURCE_ID_APP1);
    }

    @Test(dependsOnMethods = "testAddAndGetResourceGovernancePolicy")
    public void testGetResourceGovernancePolicies() throws Exception {

        List<ResourceGovernancePolicy> policies = service.getResourceGovernancePolicies(SUPER_ORG_ID);
        Assert.assertFalse(policies.isEmpty());
    }

    @Test(dependsOnMethods = "testGetResourceGovernancePolicies")
    public void testUpdateResourceGovernancePolicy() throws Exception {

        ResourceGovernancePolicy policy = buildResourcePolicy(SUPER_ORG_ID, PolicyType.IMMEDIATE, false);
        policy.setResourceId("app-resource-id-2");
        policy.setCapability("SOME_RESOURCE_CAPABILITY");
        service.addResourceGovernancePolicy(policy);

        ResourceGovernancePolicy updates = new ResourceGovernancePolicy();
        updates.setPolicyType(PolicyType.ALL);
        updates.setAllowOverride(false);
        ResourceGovernancePolicy updated = service.updateResourceGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "app-resource-id-2", "SOME_RESOURCE_CAPABILITY", updates);
        Assert.assertEquals(updated.getPolicyType(), PolicyType.ALL);
    }

    @Test(dependsOnMethods = "testUpdateResourceGovernancePolicy")
    public void testDeleteResourceGovernancePolicy() throws Exception {

        ResourceGovernancePolicy policy = buildResourcePolicy(SUPER_ORG_ID, PolicyType.DENY, false);
        policy.setResourceId("app-resource-id-to-delete");
        policy.setCapability("RESOURCE_CAP_TO_DELETE");
        service.addResourceGovernancePolicy(policy);

        service.deleteResourceGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "app-resource-id-to-delete", "RESOURCE_CAP_TO_DELETE");

        try {
            service.getResourceGovernancePolicyByKey(
                    SUPER_ORG_ID, RESOURCE_TYPE_APP, "app-resource-id-to-delete", "RESOURCE_CAP_TO_DELETE");
            Assert.fail("Expected GovernancePolicyMgtClientException for deleted policy.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        }
    }

    // -------------------------------------------------------------------------
    // coversOrg helper tests
    // -------------------------------------------------------------------------

    @Test
    public void testCoversOrgAll() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicyType(PolicyType.ALL);
        Assert.assertTrue(GovernancePolicyServiceImpl.coversOrg(policy, "any-org", false));
    }

    @Test
    public void testCoversOrgDeny() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicyType(PolicyType.DENY);
        Assert.assertTrue(GovernancePolicyServiceImpl.coversOrg(policy, "any-org", false));
    }

    @Test
    public void testCoversOrgImmediate() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicyType(PolicyType.IMMEDIATE);
        Assert.assertTrue(GovernancePolicyServiceImpl.coversOrg(policy, "any-org", true));
        Assert.assertFalse(GovernancePolicyServiceImpl.coversOrg(policy, "any-org", false));
    }

    @Test
    public void testCoversOrgSelected() {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setPolicyType(PolicyType.SELECTED);
        GovernanceOrgSelected selected = new GovernanceOrgSelected();
        selected.setTargetOrgId("target-org");
        policy.setSelectedOrgs(Collections.singletonList(selected));

        Assert.assertTrue(GovernancePolicyServiceImpl.coversOrg(policy, "target-org", false));
        Assert.assertFalse(GovernancePolicyServiceImpl.coversOrg(policy, "other-org", false));
    }

    // -------------------------------------------------------------------------
    // Natural key — org-level
    // -------------------------------------------------------------------------

    @Test
    public void testGetOrgGovernancePolicyByKey() throws Exception {

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.ALL, true);
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
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGetOrgGovernancePolicyByKey")
    public void testUpdateOrgGovernancePolicyByKey() throws Exception {

        OrgGovernancePolicy updates = new OrgGovernancePolicy();
        updates.setPolicyType(PolicyType.IMMEDIATE);
        updates.setAllowOverride(false);

        OrgGovernancePolicy updated = service.updateOrgGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "KEY_LOOKUP_CAPABILITY", updates);
        Assert.assertEquals(updated.getPolicyType(), PolicyType.IMMEDIATE);
        Assert.assertFalse(updated.isAllowOverride());
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
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        }
    }

    @Test
    public void testDeleteOrgGovernancePolicyByKeyNotFound() {

        try {
            service.deleteOrgGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "NONEXISTENT");
            Assert.fail("Expected 404.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Natural key — resource-level
    // -------------------------------------------------------------------------

    @Test
    public void testGetResourceGovernancePolicyByKey() throws Exception {

        ResourceGovernancePolicy policy = buildResourcePolicy(SUPER_ORG_ID, PolicyType.ALL, true);
        policy.setCapability("RES_KEY_LOOKUP_CAPABILITY");
        policy.setResourceId("res-key-app-id");
        service.addResourceGovernancePolicy(policy);

        ResourceGovernancePolicy fetched = service.getResourceGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "res-key-app-id", "RES_KEY_LOOKUP_CAPABILITY");
        Assert.assertEquals(fetched.getCapability(), "RES_KEY_LOOKUP_CAPABILITY");
        Assert.assertEquals(fetched.getResourceId(), "res-key-app-id");
    }

    @Test
    public void testGetResourceGovernancePolicyByKeyNotFound() {

        try {
            service.getResourceGovernancePolicyByKey(SUPER_ORG_ID, RESOURCE_TYPE_APP, "nonexistent-res", "CAP");
            Assert.fail("Expected 404.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGetResourceGovernancePolicyByKey")
    public void testUpdateResourceGovernancePolicyByKey() throws Exception {

        ResourceGovernancePolicy updates = new ResourceGovernancePolicy();
        updates.setPolicyType(PolicyType.DENY);
        updates.setAllowOverride(false);

        ResourceGovernancePolicy updated = service.updateResourceGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "res-key-app-id", "RES_KEY_LOOKUP_CAPABILITY", updates);
        Assert.assertEquals(updated.getPolicyType(), PolicyType.DENY);
        Assert.assertEquals(updated.getResourceId(), "res-key-app-id");
        Assert.assertEquals(updated.getGoverningOrgId(), SUPER_ORG_ID);
    }

    @Test(dependsOnMethods = "testUpdateResourceGovernancePolicyByKey")
    public void testDeleteResourceGovernancePolicyByKey() throws Exception {

        service.deleteResourceGovernancePolicyByKey(
                SUPER_ORG_ID, RESOURCE_TYPE_APP, "res-key-app-id", "RES_KEY_LOOKUP_CAPABILITY");

        try {
            service.getResourceGovernancePolicyByKey(
                    SUPER_ORG_ID, RESOURCE_TYPE_APP, "res-key-app-id", "RES_KEY_LOOKUP_CAPABILITY");
            Assert.fail("Expected 404 after delete.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60001"));
        }
    }

    // -------------------------------------------------------------------------
    // 409 Conflict — duplicate policy
    // -------------------------------------------------------------------------

    @Test
    public void testAddOrgPolicyConflict() throws Exception {

        when(organizationManager.isPrimaryOrganization(SUPER_ORG_ID)).thenReturn(true);

        OrgGovernancePolicy policy = buildOrgPolicy(SUPER_ORG_ID, PolicyType.ALL, true);
        policy.setCapability("CONFLICT_CAP");
        service.addOrgGovernancePolicy(policy);

        try {
            OrgGovernancePolicy duplicate = buildOrgPolicy(SUPER_ORG_ID, PolicyType.IMMEDIATE, false);
            duplicate.setCapability("CONFLICT_CAP");
            service.addOrgGovernancePolicy(duplicate);
            Assert.fail("Expected 409 for duplicate org policy.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60002"));
        }
    }

    @Test
    public void testAddResourcePolicyConflict() throws Exception {

        ResourceGovernancePolicy policy = buildResourcePolicy(SUPER_ORG_ID, PolicyType.ALL, true);
        policy.setCapability("RES_CONFLICT_CAP");
        policy.setResourceId("conflict-app-id");
        service.addResourceGovernancePolicy(policy);

        try {
            ResourceGovernancePolicy duplicate = buildResourcePolicy(SUPER_ORG_ID, PolicyType.DENY, false);
            duplicate.setCapability("RES_CONFLICT_CAP");
            duplicate.setResourceId("conflict-app-id");
            service.addResourceGovernancePolicy(duplicate);
            Assert.fail("Expected 409 for duplicate resource policy.");
        } catch (GovernancePolicyMgtClientException e) {
            Assert.assertTrue(e.getErrorCode().contains("60002"));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OrgGovernancePolicy buildOrgPolicy(String governingOrgId, PolicyType type, boolean allowOverride) {

        OrgGovernancePolicy policy = new OrgGovernancePolicy();
        policy.setGoverningOrgId(governingOrgId);
        policy.setResourceType(RESOURCE_TYPE_APP);
        policy.setCapability(CAPABILITY_ADAPTIVE_SCRIPT);
        policy.setPolicyType(type);
        policy.setAllowOverride(allowOverride);
        return policy;
    }

    private ResourceGovernancePolicy buildResourcePolicy(String governingOrgId, PolicyType type,
            boolean allowOverride) {

        ResourceGovernancePolicy policy = new ResourceGovernancePolicy();
        policy.setGoverningOrgId(governingOrgId);
        policy.setResourceType(RESOURCE_TYPE_APP);
        policy.setResourceId(RESOURCE_ID_APP1);
        policy.setResourceOwnerOrgId(SUPER_ORG_ID);
        policy.setCapability(CAPABILITY_ADAPTIVE_SCRIPT);
        policy.setPolicyType(type);
        policy.setAllowOverride(allowOverride);
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
