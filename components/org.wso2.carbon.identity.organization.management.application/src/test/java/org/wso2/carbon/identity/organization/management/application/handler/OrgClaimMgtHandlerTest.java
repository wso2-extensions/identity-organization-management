/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.handler;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.AttributeMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.event.IdentityEventConstants.Event.POST_UPDATE_LOCAL_CLAIM;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.CLAIM_DIALECT_URI;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.LOCAL_CLAIM_PROPERTIES;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.LOCAL_CLAIM_URI;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.MAPPED_ATTRIBUTES;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.TENANT_ID;

public class OrgClaimMgtHandlerTest {

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private ClaimMetadataManagementService claimMetadataManagementService;

    @Mock
    private RealmService realmService;

    @Mock
    private RealmConfiguration realmConfiguration;

    @Mock
    private UserRealm userRealm;

    @Mock
    private OrgApplicationMgtDataHolder orgApplicationMgtDataHolder;

    private MockedStatic<OrgApplicationMgtDataHolder> mockedOrgApplicationMgtDataHolder;
    private MockedStatic<IdentityTenantUtil> mockedIdentityTenantUtil;

    private OrgClaimMgtHandler orgClaimMgtHandler;

    // Renamed test constants to avoid overlap with production constants
    private static final String TEST_TENANT_DOMAIN = "test.com";
    private static final String TEST_CHILD_ORD_TENANT_DOMAIN = "child.com";
    private static final int TEST_TENANT_ID = 2;
    private static final String TEST_ORG_ID = "org123";
    private static final String TEST_CHILD_ORG_ID = "child123";
    private static final String TEST_PRIMARY_USER_STORE = "PRIMARY";
    private static final String TEST_LOCAL_CLAIM_URI = "http://wso2.org/claims/test";
    private static final String TEST_LOCAL_CLAIM_DISPLAY_NAME = "test";
    private static final String TEST_LOCAL_CLAIM_DIALECT = ClaimConstants.LOCAL_CLAIM_DIALECT_URI;

    @BeforeMethod
    public void setUp() throws Exception {

        MockitoAnnotations.openMocks(this);
        orgClaimMgtHandler = new OrgClaimMgtHandler();

        mockedOrgApplicationMgtDataHolder = mockStatic(OrgApplicationMgtDataHolder.class);
        mockedIdentityTenantUtil = mockStatic(IdentityTenantUtil.class);

        mockedOrgApplicationMgtDataHolder.when(OrgApplicationMgtDataHolder::getInstance)
                .thenReturn(orgApplicationMgtDataHolder);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantDomain(TEST_TENANT_ID))
                .thenReturn(TEST_TENANT_DOMAIN);
        mockedIdentityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(TEST_TENANT_DOMAIN))
                .thenReturn(TEST_TENANT_ID);

        when(orgApplicationMgtDataHolder.getOrganizationManager()).thenReturn(organizationManager);
        when(orgApplicationMgtDataHolder.getClaimMetadataManagementService()).thenReturn(claimMetadataManagementService);
        when(orgApplicationMgtDataHolder.getRealmService()).thenReturn(realmService);

        when(realmService.getTenantUserRealm(TEST_TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
        when(realmConfiguration.getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME))
                .thenReturn(TEST_PRIMARY_USER_STORE);
    }

    @AfterMethod
    public void tearDown() {

        mockedOrgApplicationMgtDataHolder.close();
        mockedIdentityTenantUtil.close();
    }

    /**
     * Tests a successful update where the claim exists and the primary user store mapping is present.
     */
    @Test
    public void testHandleEventForUpdateLocalClaim() throws Exception {

        String rootOrgSecondaryUserStore = "EMPLOYEE";
        String subOrgSecondaryUserStore = "PARTNER";

        // Create test event properties.
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(TENANT_ID, TEST_TENANT_ID);
        eventProperties.put(LOCAL_CLAIM_URI, TEST_LOCAL_CLAIM_URI);

        // Create mapped attributes; only the primary mapping should be retained by the handler.
        List<AttributeMapping> mappedAttributes = new ArrayList<>();
        mappedAttributes.add(new AttributeMapping(TEST_PRIMARY_USER_STORE, "test"));
        mappedAttributes.add(new AttributeMapping(rootOrgSecondaryUserStore, "test1"));
        eventProperties.put(MAPPED_ATTRIBUTES, new ArrayList<>(mappedAttributes));

        // Create local claim properties.
        Map<String, String> localClaimProperties = new HashMap<>();
        localClaimProperties.put(ClaimConstants.CLAIM_URI_PROPERTY, TEST_LOCAL_CLAIM_URI);
        localClaimProperties.put(ClaimConstants.DIALECT_PROPERTY, TEST_LOCAL_CLAIM_DIALECT);
        localClaimProperties.put(ClaimConstants.DISPLAY_NAME_PROPERTY, TEST_LOCAL_CLAIM_DISPLAY_NAME);
        localClaimProperties.put(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY, String.format("%s,%s",
                TEST_PRIMARY_USER_STORE, rootOrgSecondaryUserStore));
        eventProperties.put(LOCAL_CLAIM_PROPERTIES, localClaimProperties);

        Event event = new Event(POST_UPDATE_LOCAL_CLAIM, eventProperties);

        // Configure organization manager mocks.
        when(organizationManager.resolveOrganizationId(TEST_TENANT_DOMAIN)).thenReturn(TEST_ORG_ID);
        List<BasicOrganization> childOrgs = new ArrayList<>();
        BasicOrganization childOrg = new BasicOrganization();
        childOrg.setId(TEST_CHILD_ORG_ID);
        childOrgs.add(childOrg);
        when(organizationManager.getChildOrganizations(TEST_ORG_ID, true)).thenReturn(childOrgs);
        when(organizationManager.resolveTenantDomain(anyString())).thenReturn(TEST_CHILD_ORD_TENANT_DOMAIN);

        // Mock the existing local claim.
        LocalClaim subOrgLocalClaim = createSubOrgLocalClaim(
                new AttributeMapping(subOrgSecondaryUserStore, "check"),
                subOrgSecondaryUserStore);
        when(claimMetadataManagementService.getLocalClaims(TEST_CHILD_ORD_TENANT_DOMAIN))
                .thenReturn(new ArrayList<>(Collections.singletonList(subOrgLocalClaim)));

        // Method execution.
        orgClaimMgtHandler.handleEvent(event);

        // Capture the LocalClaim passed to updateLocalClaim and verify its content
        ArgumentCaptor<LocalClaim> localClaimCaptor = ArgumentCaptor.forClass(LocalClaim.class);
        verify(claimMetadataManagementService, times(1))
                .updateLocalClaim(localClaimCaptor.capture(), anyString());
        LocalClaim updatedClaim = localClaimCaptor.getValue();

        Assert.assertEquals(updatedClaim.getClaimURI(), TEST_LOCAL_CLAIM_URI);
        Assert.assertFalse(updatedClaim.getClaimProperties().get(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY).contains(
                rootOrgSecondaryUserStore));
        Assert.assertTrue(updatedClaim.getClaimProperties().get(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY).contains(
                TEST_PRIMARY_USER_STORE));
        Assert.assertTrue(updatedClaim.getClaimProperties().get(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY).contains(
                subOrgSecondaryUserStore));

        List<AttributeMapping> updatedAttributeMappings = updatedClaim.getMappedAttributes();
        Assert.assertEquals(updatedAttributeMappings.size(), 2);
        Assert.assertTrue(updatedAttributeMappings.stream()
                .anyMatch(mapping -> TEST_PRIMARY_USER_STORE.equals(mapping.getUserStoreDomain())));
        Assert.assertTrue(updatedAttributeMappings.stream()
                .anyMatch(mapping -> subOrgSecondaryUserStore.equals(mapping.getUserStoreDomain())));
    }

    private LocalClaim createSubOrgLocalClaim(AttributeMapping secondaryUserStoreMapping,
                                              String excludedUserStores) {

        // Existing Local Claim in sub organization.
        LocalClaim localClaim = new LocalClaim(TEST_LOCAL_CLAIM_URI);

        List<AttributeMapping> subOrgClaimMappedAttributes = new ArrayList<>();
        subOrgClaimMappedAttributes.add(new AttributeMapping(TEST_PRIMARY_USER_STORE, "test"));
        subOrgClaimMappedAttributes.add(secondaryUserStoreMapping);
        localClaim.setMappedAttributes(subOrgClaimMappedAttributes);

        Map<String, String> subOrgClaimProperties = new HashMap<>();
        subOrgClaimProperties.put(ClaimConstants.CLAIM_URI_PROPERTY, TEST_LOCAL_CLAIM_URI);
        subOrgClaimProperties.put(ClaimConstants.DIALECT_PROPERTY, TEST_LOCAL_CLAIM_DIALECT);
        subOrgClaimProperties.put(ClaimConstants.DISPLAY_NAME_PROPERTY, "test");
        subOrgClaimProperties.put(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY, excludedUserStores);
        localClaim.setClaimProperties(subOrgClaimProperties);

        return localClaim;
    }
}
