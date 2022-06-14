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

package org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.catalina.connector.Request;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.tomcat.ext.utils.URLMappingHolder;

import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.util.Util.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.util.Util.getTenantDomainFromURLMapping;

/**
 * This valve handles creation of the CarbonContext when an organization specific request comes in.
 */
@SuppressWarnings("unused")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class CarbonContextCreatorValve extends org.wso2.carbon.tomcat.ext.valves.CarbonContextCreatorValve {

    private static final String ORGANIZATION_PATH_PARAM = "/o/";
    private static final String TENANT_DOMAIN_FROM_REQUEST_PATH = "tenantDomainFromRequestPath";

    @Override
    public void initCarbonContext(Request request) throws Exception {

        if (StringUtils.startsWith(request.getRequestURI(), ORGANIZATION_PATH_PARAM)) {
            String tenantDomain;
            String appName;
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            String requestedHostName = request.getHost().getName();
            String defaultHost = URLMappingHolder.getInstance().getDefaultHost();
            // checking for URLMapping request and set tenant domain and app name accordingly.
            if (!StringUtils.equalsIgnoreCase(requestedHostName, defaultHost)) {
                tenantDomain = getTenantDomainFromURLMapping(request);
                appName = super.getAppNameForURLMapping(request);
            } else {
                // this will get executed with the default host requests.
                tenantDomain = getTenantDomain(request);
                appName = super.getAppNameFromRequest(request);
            }
            request.setAttribute(TENANT_DOMAIN_FROM_REQUEST_PATH, tenantDomain);
            super.setValuesToCarbonContext(carbonContext, tenantDomain, appName);
            super.setMDCValues(carbonContext.getTenantId(), tenantDomain, appName);
        } else {
            super.initCarbonContext(request);
        }
    }
}
