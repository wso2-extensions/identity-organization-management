/*
 *
 *  * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.carbon.identity.organization.management.organization.valve;

import com.google.gson.JsonObject;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.organization.management.organization.valve.bean.RewriteContext;
import org.wso2.carbon.identity.organization.management.organization.valve.internal.OrganizationManagementOrganizationValveServiceHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.wso2.carbon.identity.core.util.IdentityCoreConstants.TENANT_NAME_FROM_CONTEXT;
import static org.wso2.carbon.identity.organization.management.organization.valve.constants.OrganizationManagementOrganizationValveConstants.*;

/**
 * Organization Valve for Organization Management.
 */
public class OrganizationManagementOrganizationValve extends ValveBase {

    private static List<RewriteContext> contextsToRewrite;
    private static List<String> contextListToOverwriteDispatch;
    private static List<String> ignorePathListForOverwriteDispatch;
    private boolean isOrganizationQualifiedUrlsEnabled;

    private static final Log LOG = LogFactory.getLog(OrganizationManagementOrganizationValve.class);

    @Override
    protected synchronized void startInternal() throws LifecycleException{

        super.startInternal();
        // Initialize the organization context rewrite valve.
        contextsToRewrite = getContextsToRewrite();
        contextListToOverwriteDispatch = getContextListToOverwriteDispatchLocation();
        ignorePathListForOverwriteDispatch = getIgnorePathListForOverwriteDispatch();
        isOrganizationQualifiedUrlsEnabled = isOrganizationQualifiedUrlsEnabled();

    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        String contextToForward = null;
        boolean isContextRewrite = false;
        boolean isWebApp = false;

        // Get the rewrite contexts and check whether the request URI contains any of the rewrite contexts.
        for(RewriteContext context: contextsToRewrite){
            Pattern patternOrganization = context.getOrganizationContextPattern();
            Pattern patternSuperOrganization = context.getBaseContextPattern();
            if (patternOrganization.matcher(requestURI).find() || patternOrganization.matcher(requestURI + "/").find()) {
                isContextRewrite = true;
                isWebApp = context.isWebApp();
                contextToForward = context.getContext();
                break;
            } else if (isOrganizationQualifiedUrlsEnabled && (patternSuperOrganization.matcher(requestURI).find() ||
                    patternSuperOrganization.matcher(requestURI + "/").find())) {
                IdentityUtil.threadLocalProperties.get().put(ORGANIZATION_NAME_FROM_CONTEXT, DEFAULT_ORGANIZATION_DOMAIN);
                break;
            }
        }
    }

    private boolean isOrganizationQualifiedUrlsEnabled() {

        Map<String, Object> configuration = IdentityConfigParser.getInstance().getConfiguration();
        String enableTenantQualifiedUrls = (String) configuration.get(ENABLE_ORGANIZATION_QUALIFIED_URLS);
        return Boolean.parseBoolean(enableTenantQualifiedUrls);
    }

    private List<RewriteContext> getContextsToRewrite(){

        List<RewriteContext> rewriteContexts = new ArrayList<>();
        Map<String, Object> configuration = IdentityConfigParser.getInstance().getConfiguration();
        Object webAppContexts = configuration.get("OrganizationContextsToRewrite.WebApp.Context");
        if (webAppContexts != null) {
            if (webAppContexts instanceof ArrayList) {
                for (String context : (ArrayList<String>) webAppContexts) {
                    rewriteContexts.add(new RewriteContext(true, context));
                }
            } else {
                rewriteContexts.add(new RewriteContext(true, webAppContexts.toString()));
            }
        }

        Object servletContexts = configuration.get("OrganizationContextsToRewrite.Servlet.Context");
        if (servletContexts != null) {
            if (servletContexts instanceof ArrayList) {
                for (String context : (ArrayList<String>) servletContexts) {
                    rewriteContexts.add(new RewriteContext(false, context));
                }
            } else {
                rewriteContexts.add(new RewriteContext(false, servletContexts.toString()));
            }
        }
        return rewriteContexts;
    }

    private List<String> getContextListToOverwriteDispatchLocation(){

        return getConfigValues("OrganizationContextsToRewrite.OverwriteDispatch.Context");
    }

    private List<String> getIgnorePathListForOverwriteDispatch() {

        return getConfigValues("OrganizationContextsToRewrite.OverwriteDispatch.IgnorePath");
    }

    private List<String> getConfigValues(String elementPath){

        Map<String, Object> configuration = IdentityConfigParser.getInstance().getConfiguration();
        Object elements = configuration.get(elementPath);
        if (elements != null) {
            List<String> configValues = new ArrayList<>();
            if (elements instanceof List) {
                configValues.addAll((List<String>) elements);
            } else {
                configValues.add(elements.toString());
            }
            return configValues;
        }
        return Collections.emptyList();
    }

    private boolean isIgnorePath(String dispatchLocation) {

        for (String path : ignorePathListForOverwriteDispatch) {
            if (dispatchLocation.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private void handleRuntimeErrorResponse(Response response, int error, String organizationDomain) throws
            IOException, ServletException {

        response.setContentType("application/json");
        response.setStatus(error);
        response.setCharacterEncoding("UTF-8");
        JsonObject errorResponse = new JsonObject();
        String errorMsg = "Error occurred while validating organization domain: " + organizationDomain;
        errorResponse.addProperty("code", error);
        errorResponse.addProperty("message", errorMsg);
        errorResponse.addProperty("description", errorMsg);
        response.getWriter().print(errorResponse.toString());
    }

    private void handleInvalidOrganizationDomainErrorResponse(Response response, int error, String organizationDomain) throws
            IOException, ServletException {

        response.setContentType("application/json");
        response.setStatus(error);
        response.setCharacterEncoding("UTF-8");
        JsonObject errorResponse = new JsonObject();
        String errorMsg = "invalid organization domain : " + organizationDomain;
        errorResponse.addProperty("code", error);
        errorResponse.addProperty("message", errorMsg);
        errorResponse.addProperty("description", errorMsg);
        response.getWriter().print(errorResponse.toString());
    }

    private void handleRestrictedOrganizationDomainErrorResponse(Request request, Response response) throws IOException {

        String requestContentType = request.getContentType();
        if (StringUtils.contains(requestContentType, "application/json")) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("code", HttpServletResponse.SC_FORBIDDEN);
            String errorMsg = "Access to super tenant domain over tenanted URL format (/t/carbon.super) is restricted. "
                    + "Please use the server base path instead.";
            errorResponse.addProperty("message", errorMsg);
            errorResponse.addProperty("description", errorMsg);
            response.getWriter().print(errorResponse.toString());
        } else {
            response.setContentType("text/html");
            String errorPage = OrganizationManagementOrganizationValveServiceHolder.getInstance().getPageNotFoundErrorPage();
            response.getWriter().print(errorPage);
        }
    }
}
