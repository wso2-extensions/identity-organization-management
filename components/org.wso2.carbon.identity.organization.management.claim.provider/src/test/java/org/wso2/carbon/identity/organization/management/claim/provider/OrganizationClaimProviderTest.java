/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.claim.provider;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.organization.management.claim.provider.internal.OrganizationClaimProviderServiceComponentHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManagementInitialize;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OrganizationClaimProviderTest {

    private static final String USER_ORG_ID = "userOrgId";
    private static final String AUTH_ORG_ID = "authOrgId";
    private static final String AUTHORIZED_ORG_NAME = "AuthorizedOrgName";
    private static final String AUTHORIZED_ORG_HANDLE = "AuthorizedOrgHandle";
    private static final String ORG_ID_CLAIM = "org_id";
    private static final String ORG_NAME_CLAIM = "org_name";
    private static final String ORG_HANDLE_CLAIM = "org_handle";

    private OrganizationClaimProvider organizationClaimProvider;

    @Mock
    private OAuthTokenReqMessageContext oAuthTokenReqMessageContext;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @Mock
    private OrganizationManager organizationManager;

    @Mock
    private OrganizationManagementInitialize organizationManagementInitializeService;

    @BeforeMethod
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        organizationClaimProvider = new OrganizationClaimProvider();
        OrganizationClaimProviderServiceComponentHolder.getInstance().setOrganizationManager(organizationManager);
    }

    @Test
    public void testGetAdditionalClaims() throws IdentityOAuth2Exception, OrganizationManagementException {

        when(organizationManagementInitializeService.isOrganizationManagementEnabled()).thenReturn(true);
        OrganizationClaimProviderServiceComponentHolder.getInstance()
                .setOrganizationManagementEnable(organizationManagementInitializeService);

        when(oAuthTokenReqMessageContext.getAuthorizedUser()).thenReturn(authenticatedUser);
        when(authenticatedUser.getUserResidentOrganization()).thenReturn(USER_ORG_ID);
        when(authenticatedUser.getAccessingOrganization()).thenReturn(AUTH_ORG_ID);

        when(organizationManager.getOrganizationNameById(AUTH_ORG_ID)).thenReturn(AUTHORIZED_ORG_NAME);
        when(organizationManager.resolveTenantDomain(AUTH_ORG_ID)).thenReturn(AUTHORIZED_ORG_HANDLE);

        Map<String, Object>
                additionalClaims = organizationClaimProvider.getAdditionalClaims(oAuthTokenReqMessageContext);

        assertTrue(additionalClaims.containsKey(ORG_ID_CLAIM));
        assertTrue(additionalClaims.containsKey(ORG_NAME_CLAIM));
        assertTrue(additionalClaims.containsKey(ORG_HANDLE_CLAIM));
        assertEquals(AUTH_ORG_ID, additionalClaims.get(ORG_ID_CLAIM));
        assertEquals(AUTHORIZED_ORG_NAME, additionalClaims.get(ORG_NAME_CLAIM));
        assertEquals(AUTHORIZED_ORG_HANDLE, additionalClaims.get(ORG_HANDLE_CLAIM));
    }
}
