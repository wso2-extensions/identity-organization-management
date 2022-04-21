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

package org.wso2.carbon.identity.organization.management.role.management.endpoint;

import org.wso2.carbon.identity.organization.management.role.management.endpoint.*;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.*;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import java.io.InputStream;
import java.util.List;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.Error;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RoleGetResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePatchResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePostResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutRequestObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolePutResponseObject;
import org.wso2.carbon.identity.organization.management.role.management.endpoint.model.RolesListResponseObject;
import javax.ws.rs.core.Response;


public interface OrganizationsApiService {

      public Response createRole(String organizationId, RolePostRequestObject rolePostRequestObject);

      public Response organizationsOrganizationIdRolesGet(String organizationId, String filter, Integer startIndex, Integer count);

      public Response organizationsOrganizationIdRolesRoleIdDelete(String organizationId, String roleId);

      public Response organizationsOrganizationIdRolesRoleIdGet(String organizationId, String roleId);

      public Response organizationsOrganizationIdRolesRoleIdPatch(String organizationId, String roleId, RolePatchRequestObject rolePatchRequestObject);

      public Response organizationsOrganizationIdRolesRoleIdPut(String organizationId, String roleId, RolePutRequestObject rolePutRequestObject);
}
