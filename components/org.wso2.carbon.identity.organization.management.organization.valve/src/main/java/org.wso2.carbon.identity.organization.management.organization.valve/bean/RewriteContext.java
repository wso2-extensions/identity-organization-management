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

package org.wso2.carbon.identity.organization.management.organization.valve.bean;

import java.util.regex.Pattern;

public class RewriteContext {

    private boolean isWebApp;
    private String context;
    private Pattern organizationContextPattern;
    private Pattern baseContextPattern;

    public RewriteContext(boolean isWebApp, String context) {

        this.isWebApp = isWebApp;
        this.context = context;
        this.organizationContextPattern = Pattern.compile("^/o/([^/]+)" + context);
        this.baseContextPattern = Pattern.compile("^" + context);
    }

    public boolean isWebApp() {

        return isWebApp;
    }

    public void setWebApp(boolean webApp) {

        isWebApp = webApp;
    }

    public String getContext() {

        return context;
    }

    public void setContext(String context) {

        this.context = context;
    }

    public Pattern getOrganizationContextPattern() {

        return organizationContextPattern;
    }

    public void setOrganizationContextPattern(Pattern organizationContextPattern) {

        this.organizationContextPattern = organizationContextPattern;
    }

    public Pattern getBaseContextPattern() {

        return baseContextPattern;
    }

    public void setBaseContextPattern(Pattern baseContextPattern) {

        this.baseContextPattern = baseContextPattern;
    }
}
