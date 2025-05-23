/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.resource.sharing.policy.management.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerService;
import org.wso2.carbon.identity.organization.resource.sharing.policy.management.ResourceSharingPolicyHandlerServiceImpl;

/**
 * OSGi service component for Resource Sharing Policy Handler bundle.
 */
@Component(
        name = "identity.organization.resource.sharing.policy.management.component",
        immediate = true
)
public class ResourceSharingPolicyHandlerServiceComponent {

    private static final Log LOG = LogFactory.getLog(ResourceSharingPolicyHandlerServiceComponent.class);

    /**
     * Register the Resource Sharing Policy Mgt service in the OSGI context.
     *
     * @param componentContext OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext componentContext) {

        BundleContext bundleContext = componentContext.getBundleContext();
        ResourceSharingPolicyHandlerService resourceSharingPolicyHandlerService =
                new ResourceSharingPolicyHandlerServiceImpl();
        bundleContext.registerService(ResourceSharingPolicyHandlerService.class.getName(),
                resourceSharingPolicyHandlerService, null);
        if (LOG.isDebugEnabled()) {
            LOG.debug("ResourceSharingPolicyHandlerServiceComponent activated successfully.");
        }
    }
}
