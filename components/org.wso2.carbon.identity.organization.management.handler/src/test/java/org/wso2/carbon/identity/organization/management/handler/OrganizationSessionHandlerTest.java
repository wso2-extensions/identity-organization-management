/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.handler;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCache;
import org.wso2.carbon.identity.application.authentication.framework.cache.SessionContextCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.oidc.model.OIDCStateInfo;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.handler.internal.OrganizationManagementHandlerDataHolder;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.testng.Assert.assertEquals;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ORGANIZATION_AUTHENTICATOR;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ORGANIZATION_LOGIN_IDP_NAME;
import static org.wso2.carbon.identity.event.IdentityEventConstants.Event.SESSION_EXTENSION;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.SESSION_CONTEXT;
import static org.wso2.carbon.identity.event.IdentityEventConstants.EventProperty.SESSION_CONTEXT_ID;

public class OrganizationSessionHandlerTest {

    @Mock
    private OrganizationManager organizationManager;
    @Mock
    private SessionContextCache sessionContextCache;
    private MockedStatic<SessionContextCache> sessionContextCacheMockedStatic;
    private MockedStatic<FrameworkUtils> frameworkUtilsMockedStatic;

    @BeforeClass
    public void setUp() {

        openMocks(this);
        OrganizationManagementHandlerDataHolder.getInstance().setOrganizationManager(organizationManager);
        sessionContextCacheMockedStatic = mockStatic(SessionContextCache.class);
        frameworkUtilsMockedStatic = mockStatic(FrameworkUtils.class);
    }

    @AfterClass
    public void tearDown() {

        sessionContextCacheMockedStatic.close();
    }

    @Test(dataProvider = "sessionTypeDataProvider")
    public void testHandleOrgSessionExtendMethod(String idpName, String authenticatorName, boolean isSessionExpired,
                                                 boolean isSessionUpdated) throws Exception {

        Map<String, Object> eventProperties = new HashMap<>();

        SessionContext sessionContext = new SessionContext();

        OIDCStateInfo oidcStateInfo = new OIDCStateInfo();
        oidcStateInfo.setIdTokenHint(
                "eyJ4NXQiOiJPV1JpTXpaaVlURXhZVEl4WkdGa05UVTJOVE0zTWpkaFltTmxNVFZrTnpRMU56a3pa"
                        + "VGc1TVRrNE0yWmxOMkZoWkdaalpURmlNemxsTTJJM1l6ZzJNZyIsImtpZCI6Ik9XUmlNelppWVRFeFlU"
                        + "SXhaR0ZrTlRVMk5UTTNNamRoWW1ObE1UVmtOelExTnprelpUZzVNVGs0TTJabE4yRmhaR1pqWlRGaU16"
                        + "bGxNMkkzWXpnMk1nX1JTMjU2IiwiYWxnIjoiUlMyNTYifQ.eyJpc2siOiI1NmJkNTBmNDE3MzJkZDExN"
                        + "jczMjRkYTM4NDAzZmMxYWE3YmI3ZDc2ZTUxYjAyNTcyMzUzMTEzNDAxZjM3OThhIiwiYXRfaGFzaCI6I"
                        + "mh5dXJhb3lEdzBZTXJ3TTNVV2JIb1EiLCJzdWIiOiI5Zjg0ODhlZi1kNTIyLTQ0NWQtYTllMy05MWRkO"
                        + "TA2OTc4YjEiLCJhbXIiOlsiQmFzaWNBdXRoZW50aWNhdG9yIl0sImlzcyI6Imh0dHBzOlwvXC9sb2Nhb"
                        + "Ghvc3Q6OTQ0M1wvb2F1dGgyXC90b2tlbiIsInNpZCI6IjBmMjBjNWZlLTU3ZWItNDMzNi1iZGU4LTI4N"
                        + "TRmMWE5MzFlMyIsImF1ZCI6InJVWTB3RU85d1N1M0l2SHpmem9vMEdmSE5Pb2EiLCJjX2hhc2giOiJBd"
                        + "ndQTjFvT0Jla3RydHR2MkRoM09nIiwibmJmIjoxNzQyNTYwMTI2LCJhenAiOiJyVVkwd0VPOXdTdTNJd"
                        + "k h6ZnpvbzBHZkhOT29hIiwib3JnX2lkIjoiMTAwODRhOGQtMTEzZi00MjExLWEwZDUtZWZlMzZiMDgyM"
                        + "jExIiwiZXhwIjoxNzQyNTYzNzI2LCJvcmdfbmFtZSI6IlN1cGVyIiwiaWF0IjoxNzQyNTYwMTI2LCJqd"
                        + "GkiOiIxMjA0MGYzMi01OGJkLTQ3ZjYtYjg4NS0wNTMyM2M0NzMxNDgifQ.BNDz2yftQOy_4kOC7iRz7H"
                        + "Tb8znV-1IafR9ZQgF3kS8FU0uhcPZmbTQICn-cbpqsFY3V-p-do6y8bjEuJ1mgNrXVAqETbn2MIGiUYc"
                        + "wmFOb2Va32SjqmcovqkoHSFCv9mn0yxNo-CugeCm6r71RPnDkHm7QvYoY2Pp2S83RgYgpSGdzdFloWAV"
                        + "zkN0gtcfPQEIXKtzFJiR5wxcIH_VdfdMF_B2oc2rcrWH7bnDtsgtSsGr7k8_EFYv0515W4EYsz03rFDb"
                        + "C0VNQGRkh3XYzI-sLUwq9qFIGgiLzcqEpjCf6wz1CCDDmt7_A1OL3JTNoiE4TBYg55sOx6YzW9DQUMLA");

        AuthenticatorConfig authenticatorConfig = new AuthenticatorConfig();
        authenticatorConfig.setName(authenticatorName);
        authenticatorConfig.setAuthenticatorStateInfo(oidcStateInfo);

        Map<String, AuthenticatedIdPData> authenticatedIdPs = new HashMap<>();
        AuthenticatedIdPData authenticatedIdPData = new AuthenticatedIdPData();
        authenticatedIdPData.setIdpName(idpName);
        authenticatedIdPs.put(idpName, authenticatedIdPData);
        authenticatedIdPData.setAuthenticators(List.of(authenticatorConfig));
        sessionContext.setAuthenticatedIdPs(authenticatedIdPs);

        eventProperties.put(SESSION_CONTEXT, sessionContext);
        eventProperties.put(SESSION_CONTEXT_ID, "dummySessionContextId");
        Event event = new Event(SESSION_EXTENSION, eventProperties);

        when(organizationManager.resolveTenantDomain(anyString())).thenReturn("orgTenantDomain");
        sessionContextCacheMockedStatic.when(SessionContextCache::getInstance).thenReturn(sessionContextCache);

        SessionContextCacheEntry sessionContextCacheEntry = new SessionContextCacheEntry();
        SessionContext orgSessionContext = new SessionContext();
        sessionContextCacheEntry.setContext(orgSessionContext);
        when(sessionContextCache.getSessionContextCacheEntry(any(), anyString())).thenReturn(sessionContextCacheEntry);
        when(sessionContextCache.isSessionExpired(any(), any())).thenReturn(isSessionExpired);

        AtomicBoolean sessionUpdated = new AtomicBoolean(false);
        frameworkUtilsMockedStatic.when(() -> FrameworkUtils.updateSessionLastAccessTimeMetadata(anyString(), any()))
                .thenAnswer(invocation -> null);
        frameworkUtilsMockedStatic.when(
                        () -> FrameworkUtils.addSessionContextToCache(anyString(), any(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    sessionUpdated.set(isSessionUpdated);
                    return null;
                });

        OrganizationSessionHandler organizationSessionHandler = new OrganizationSessionHandler();
        organizationSessionHandler.handleEvent(event);
        assertEquals(sessionUpdated.get(), isSessionUpdated);
    }

    @DataProvider(name = "sessionTypeDataProvider")
    public Object[][] sessionTypeDataProvider() {

        return new Object[][]{{"dummyIdp", ORGANIZATION_AUTHENTICATOR, false, true},
                {ORGANIZATION_LOGIN_IDP_NAME, "dummyAuthenticator", false, false},
                {ORGANIZATION_LOGIN_IDP_NAME, ORGANIZATION_AUTHENTICATOR, true, false},
                {ORGANIZATION_LOGIN_IDP_NAME, ORGANIZATION_AUTHENTICATOR, false, true}};
    }
}
