/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.endpoint;

import org.wso2.carbon.identity.organization.management.endpoint.*;
import org.wso2.carbon.identity.organization.management.endpoint.model.*;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;
import org.wso2.carbon.identity.organization.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.endpoint.model.GetOrganizationResponse;
import java.util.List;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPOSTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPUTRequest;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationPatchRequestItem;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationResponse;
import org.wso2.carbon.identity.organization.management.endpoint.model.OrganizationsResponse;
import javax.ws.rs.core.Response;


public interface OrganizationsApiService {

      public Response organizationsGet(String filter, Integer limit, String after, String before);

      public Response organizationsOrganizationIdDelete(String organizationId);

      public Response organizationsOrganizationIdGet(String organizationId, Boolean showChildren, Boolean includePermissions);

      public Response organizationsOrganizationIdPatch(String organizationId, List<OrganizationPatchRequestItem> organizationPatchRequestItem);

      public Response organizationsOrganizationIdPut(String organizationId, OrganizationPUTRequest organizationPUTRequest);

      public Response organizationsPost(OrganizationPOSTRequest organizationPOSTRequest);

      public Response shareOrgApplication(String organizationId, String applicationId, List<String> requestBody);
}
