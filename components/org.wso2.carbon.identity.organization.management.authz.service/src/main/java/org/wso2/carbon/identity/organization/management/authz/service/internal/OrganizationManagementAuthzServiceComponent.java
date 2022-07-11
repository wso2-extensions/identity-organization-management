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

package org.wso2.carbon.identity.organization.management.authz.service.internal;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.authz.service.handler.AuthorizationHandler;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.organization.management.authz.service.handler.OrganizationManagementAuthzHandler;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Service component class for the organization management authorization service.
 */
@Component(
        name = "org.wso2.carbon.identity.org.mgt.authz.service",
        immediate = true)
public class OrganizationManagementAuthzServiceComponent {

    public static final String NAME = "name";
    private static final String PERMISSION = "Permission";
    private static final String IDENTITY_PATH = "identity";

    private static final Log LOG = LogFactory.getLog(OrganizationManagementAuthzServiceComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        bundleContext.registerService(AuthorizationHandler.class.getName(), new OrganizationManagementAuthzHandler(),
                null);
        loadOAuthScopePermissionMapping();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Organization management authorization service component activated successfully.");
        }
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting the Realm Service.");
        }
        OrganizationManagementAuthzServiceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Unsetting the Realm Service.");
        }
        OrganizationManagementAuthzServiceHolder.getInstance().setRealmService(null);
    }

    private static void loadOAuthScopePermissionMapping() {

        Map<String, List<String>> scopePermissionMapping = new HashMap<>();
        String configDirPath = CarbonUtils.getCarbonConfigDirPath();
        String confXml = Paths.get(configDirPath, IDENTITY_PATH, OAuthConstants.OAUTH_SCOPE_BINDING_PATH).toString();
        File configFile = new File(confXml);
        if (!configFile.exists()) {
            LOG.warn("OAuth scope binding File is not present at: " + confXml);
            return;
        }

        XMLStreamReader parser = null;
        try (InputStream stream = Files.newInputStream(configFile.toPath())) {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(stream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            OMElement documentElement = builder.getDocumentElement();
            Iterator iterator = documentElement.getChildElements();
            while (iterator.hasNext()) {
                OMElement omElement = (OMElement) iterator.next();
                String scope = omElement.getAttributeValue(new QName(NAME));
                List<String> permissions = loadScopePermissions(omElement);
                scopePermissionMapping.put(scope, permissions);
            }
        } catch (XMLStreamException e) {
            LOG.warn("Error while streaming oauth-scope-bindings config.", e);
        } catch (IOException e) {
            LOG.warn("Error while loading oauth-scope-bindings config.", e);
        } finally {
            try {
                if (parser != null) {
                    parser.close();
                }
            } catch (XMLStreamException e) {
                LOG.error("Error while closing XML stream", e);
            }
        }
        OrganizationManagementAuthzServiceHolder.getInstance().setScopePermissionMapping(scopePermissionMapping);
    }

    private static List<String> loadScopePermissions(OMElement configElement) {

        List<String> permissions = new ArrayList<>();
        Iterator it = configElement.getChildElements();
        while (it.hasNext()) {
            OMElement element = (OMElement) it.next();
            Iterator permissionIterator = element.getChildElements();
            while (permissionIterator.hasNext()) {
                OMElement permissionElement = (OMElement) permissionIterator.next();
                if (PERMISSION.equals(permissionElement.getLocalName())) {
                    String permission = permissionElement.getText();
                    permissions.add(permission);
                }
            }
        }
        return permissions;
    }
}
