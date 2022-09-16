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
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.internal.OrganizationManagementTomcatDataHolder;
import org.wso2.carbon.tomcat.ext.utils.URLMappingHolder;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATH_SEPARATOR;
import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.util.Util.getTenantDomain;
import static org.wso2.carbon.identity.organization.management.tomcat.ext.tenant.resolver.util.Util.getTenantDomainFromURLMapping;
import static org.wso2.carbon.tomcat.ext.constants.Constants.TENANT_DOMAIN_FROM_REQUEST_PATH;

/**
 * This valve handles creation of the CarbonContext when an organization specific request comes in.
 */
@SuppressWarnings("unused")
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class CarbonContextCreatorValve extends org.wso2.carbon.tomcat.ext.valves.CarbonContextCreatorValve {

    private static final String ORGANIZATION_PATH_PARAM = "/o/";

    @Override
    public void initCarbonContext(Request request) throws Exception {

        String requestURI = request.getRequestURI();
        if (OrganizationManagementTomcatDataHolder.getInstance().isOrganizationManagementEnabled() &&
                StringUtils.startsWith(requestURI, ORGANIZATION_PATH_PARAM)) {
            String tenantDomain;
            String appName;
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            String requestedHostName = request.getHost().getName();
            String defaultHost = URLMappingHolder.getInstance().getDefaultHost();
            try {
                tenantDomain = resolveTenantDomain(request);
            } catch (OrganizationManagementException e) {
                /**
                 * resolveTenantDomain method includes db calls and user realm has to be set beforehand. Super tenant
                 * domain is set as temporary tenant domain and the correct tenant domain will be set by the next call
                 * for resolveTenantDomain method.
                 */
                super.setValuesToCarbonContext(carbonContext, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, null);
                tenantDomain = resolveTenantDomain(request);
            }
            if (StringUtils.equalsIgnoreCase(requestedHostName, defaultHost)) {
                appName = super.getAppNameFromRequest(request);
            } else {
                appName = super.getAppNameForURLMapping(request);
            }
            request.setAttribute(TENANT_DOMAIN_FROM_REQUEST_PATH, tenantDomain);
            super.setValuesToCarbonContext(carbonContext, tenantDomain, appName);
            super.setMDCValues(carbonContext.getTenantId(), tenantDomain, appName);
            setOrganizationIdToCarbonContext(carbonContext, requestURI);
        } else {
            super.initCarbonContext(request);
        }
    }

    private static String resolveTenantDomain(Request request) throws OrganizationManagementException {

        if (StringUtils.equalsIgnoreCase(request.getHost().getName(),
                URLMappingHolder.getInstance().getDefaultHost())) {
            return getTenantDomain(request);
        } else {
            return getTenantDomainFromURLMapping(request);
        }
    }

    private static void setOrganizationIdToCarbonContext(PrivilegedCarbonContext carbonContext, String requestURI) {

        String organizationIdInRequestPath = requestURI.substring(requestURI.indexOf(ORGANIZATION_PATH_PARAM) + 3);
        if (organizationIdInRequestPath.contains(PATH_SEPARATOR)) {
            organizationIdInRequestPath = organizationIdInRequestPath.substring(0,
                    organizationIdInRequestPath.indexOf(PATH_SEPARATOR));
        }
        carbonContext.setOrganizationId(organizationIdInRequestPath);
    }
}
