/*
 * Copyright (c) 2023-2025, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.organization.management.application.handler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementService;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.AttributeMapping;
import org.wso2.carbon.identity.claim.metadata.mgt.model.Claim;
import org.wso2.carbon.identity.claim.metadata.mgt.model.ClaimDialect;
import org.wso2.carbon.identity.claim.metadata.mgt.model.ExternalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManager;
import org.wso2.carbon.identity.organization.management.application.OrgApplicationManagerImpl;
import org.wso2.carbon.identity.organization.management.application.constant.OrgApplicationMgtConstants;
import org.wso2.carbon.identity.organization.management.application.internal.OrgApplicationMgtDataHolder;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.management.service.model.BasicOrganization;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.util.OrganizationManagementUtil;
import org.wso2.carbon.identity.organization.management.service.util.Utils;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT;

/**
 * This class contains the implementation of the handler for managing sub-organizations' user claims.
 * This handler will be used to manage claims of sub organizations.
 */
public class OrgClaimMgtHandler extends AbstractEventHandler {

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private static final Log LOG = LogFactory.getLog(OrgClaimMgtHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        String eventName = event.getEventName();
        switch (eventName) {
            case OrgApplicationMgtConstants.EVENT_POST_SHARE_APPLICATION:
                handlePostShareApplication(event);
                break;
            case IdentityEventConstants.Event.POST_APP_USER_ATTRIBUTE_UPDATE:
                handleAppUserAttributeUpdate(event);
                break;
            case IdentityEventConstants.Event.POST_DELETE_LOCAL_CLAIM:
                handlePostDeleteLocalClaim(event);
                break;
            case IdentityEventConstants.Event.POST_DELETE_EXTERNAL_CLAIM:
                handlePostDeleteExternalClaim(event);
                break;
            case IdentityEventConstants.Event.POST_UPDATE_LOCAL_CLAIM:
                handleUpdateLocalClaim(event);
                break;
            case IdentityEventConstants.Event.POST_UPDATE_EXTERNAL_CLAIM:
                handleUpdateExternalClaim(event);
                break;
            case IdentityEventConstants.Event.POST_ADD_EXTERNAL_CLAIM:
                handleAddExternalClaim(event);
                break;
            case IdentityEventConstants.Event.POST_ADD_CLAIM_DIALECT:
                handleAddClaimDialect(event);
                break;
            case IdentityEventConstants.Event.POST_UPDATE_CLAIM_DIALECT:
                handleUpdateClaimDialect(event);
                break;
            case IdentityEventConstants.Event.POST_DELETE_CLAIM_DIALECT:
                handleDeleteClaimDialect(event);
                break;
            case Constants.EVENT_POST_ADD_ORGANIZATION:
                handlePostAddOrganization(event);
                break;
            default:
                break;
        }
    }

    private void handlePostShareApplication(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        String parentOrganizationId =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_PARENT_ORGANIZATION_ID);
        String sharedOrganizationID =
                (String) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_ORGANIZATION_ID);
        ClaimMapping[] claimMappings =
                (ClaimMapping[]) eventProperties.get(OrgApplicationMgtConstants.EVENT_PROP_SHARED_USER_ATTRIBUTES);

        try {
            String parentTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrganizationId);
            String sharedOrganizationTenantDomain = getOrganizationManager().resolveTenantDomain(sharedOrganizationID);

            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(sharedOrganizationTenantDomain)) {
                return;
            }
            List<String> missingClaims = getMissingClaims(sharedOrganizationTenantDomain, claimMappings);
            if (!missingClaims.isEmpty()) {
                List<LocalClaim> parentOrgCustomLocalClaims = getClaimMetadataManagementService().
                        getLocalClaims(parentTenantDomain);
                addClaimsToSubOrganization(parentOrgCustomLocalClaims, sharedOrganizationTenantDomain,
                        missingClaims, parentTenantDomain);
            }
            // Add the custom claim dialects to the organization.
            List<String> claimDialectURIListInOrg = getClaimMetadataManagementService()
                    .getClaimDialects(sharedOrganizationTenantDomain).stream().map(ClaimDialect::getClaimDialectURI)
                    .collect(Collectors.toList());
            getClaimMetadataManagementService().getClaimDialects(parentTenantDomain).stream()
                    .filter(claimDialect -> !claimDialectURIListInOrg.contains(claimDialect.getClaimDialectURI()))
                    .forEach(claimDialect -> {
                        try {
                            getClaimMetadataManagementService()
                                    .addClaimDialect(claimDialect, sharedOrganizationTenantDomain);
                        } catch (ClaimMetadataException e) {
                            LOG.error("Error while adding claim dialect " + claimDialect.getClaimDialectURI() +
                                    " to organization " + sharedOrganizationTenantDomain, e);
                        }
                    });
        } catch (OrganizationManagementException | ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred  while adding the claims.", e);
        }
    }

    private void handleAppUserAttributeUpdate(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        List<ClaimMapping> claimMappings =
                (List<ClaimMapping>) eventProperties.get(IdentityEventConstants.EventProperty.UPDATED_CLAIM_MAPPINGS);
        String applicationId =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.APPLICATION_ID);
        String tenantDomain = (String) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
        try {
            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(tenantDomain)) {
                return;
            }
            List<BasicOrganization> sharedOrganizations = getOrgApplicationManager().getApplicationSharedOrganizations(
                    getOrganizationManager().resolveOrganizationId(tenantDomain), applicationId);
            if (!sharedOrganizations.isEmpty()) {
                ClaimMapping[] filteredClaimMappings =
                        claimMappings.stream().filter(claim -> !claim.getLocalClaim().getClaimUri()
                                        .startsWith(OrgApplicationMgtConstants.RUNTIME_CLAIM_URI_PREFIX))
                                .toArray(ClaimMapping[]::new);

                for (BasicOrganization organization : sharedOrganizations) {
                    String sharedOrganizationTenantDomain = getOrganizationManager()
                            .resolveTenantDomain(organization.getId());
                    List<String> missingClaims = getMissingClaims(sharedOrganizationTenantDomain,
                            filteredClaimMappings);
                    List<LocalClaim> parentOrgCustomLocalClaims = getClaimMetadataManagementService().
                            getLocalClaims(tenantDomain);
                    CompletableFuture.runAsync(() -> {
                        try {
                            addClaimsToSubOrganization(parentOrgCustomLocalClaims, sharedOrganizationTenantDomain,
                                    missingClaims, tenantDomain);
                        } catch (IdentityEventException e) {
                            LOG.error("An error occurred while adding the claims:", e);
                        }
                    }, executorService);
                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while adding the claims.", e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while adding the claims.", e);
        }
    }

    private void handlePostDeleteExternalClaim(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        String externalClaimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.CLAIM_DIALECT_URI);
        String externalClaimURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.EXTERNAL_CLAIM_URI);
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);

        try {
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                if (isExistingExternalClaimURI(externalClaimDialectURI, externalClaimURI,
                        sharedOrganizationTenantDomain)) {
                    getClaimMetadataManagementService().removeExternalClaim(externalClaimDialectURI, externalClaimURI,
                            sharedOrganizationTenantDomain);
                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while deleting the external claim " + externalClaimURI,
                    e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while deleting the external claim " + externalClaimURI,
                    e);
        }
    }

    private void handlePostDeleteLocalClaim(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        String localClaimUri = (String) eventProperties.get(IdentityEventConstants.EventProperty.LOCAL_CLAIM_URI);
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);

        try {
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                if (isExistingLocalClaimURI(localClaimUri, sharedOrganizationTenantDomain)) {
                    getClaimMetadataManagementService().removeLocalClaim(localClaimUri, sharedOrganizationTenantDomain);
                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while deleting local claim " + localClaimUri, e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while deleting local claim " + localClaimUri, e);
        }
    }

    private void handleUpdateLocalClaim(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        String localClaimURI = (String) eventProperties.get(IdentityEventConstants.EventProperty.LOCAL_CLAIM_URI);
        Map<String, String> localClaimProperties =
                (Map<String, String>) eventProperties.get(IdentityEventConstants.EventProperty.
                        LOCAL_CLAIM_PROPERTIES);
        List<AttributeMapping> mappedAttributes =
                (List<AttributeMapping>) eventProperties.get(IdentityEventConstants.EventProperty.
                        MAPPED_ATTRIBUTES);

        try {
            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(tenantDomain)) {
                return;
            }
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                Optional<LocalClaim> existingLocalClaim =
                        getExistingLocalClaim(localClaimURI, sharedOrganizationTenantDomain);

                if (existingLocalClaim.isPresent()) {
                    String primaryUserStoreDomain = getPrimaryUserStoreDomain(tenantDomain);
                    List<AttributeMapping> modifiedAttributeMappings = mergeAttributeMappings(
                            existingLocalClaim.get().getMappedAttributes(), mappedAttributes, primaryUserStoreDomain);

                    if (!modifiedAttributeMappings.isEmpty()) {
                        Map<String, String> modifiedLocalClaimProperties = processLocalClaimProperties(
                                existingLocalClaim.get().getClaimProperties(),
                                localClaimProperties,
                                primaryUserStoreDomain);
                        getClaimMetadataManagementService().updateLocalClaim(
                                new LocalClaim(localClaimURI, modifiedAttributeMappings, modifiedLocalClaimProperties),
                                sharedOrganizationTenantDomain);
                    }

                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while updating the local claim " + localClaimURI, e);
        } catch (ClaimMetadataException | UserStoreException e) {
            throw new IdentityEventException("An error occurred while updating the local claim " + localClaimURI, e);
        }
    }

    /**
     * Merges attribute mappings from the existing claim and the incoming update.
     * Preserve all non-primary mappings from the existing claim and user primary mappings from the incoming update.
     *
     * @param existingMappings       the attribute mappings from the existing claim.
     * @param incomingMappings       the attribute mappings from the incoming update.
     * @param primaryUserStoreDomain the primary user store domain.
     * @return a merged list of attribute mappings.
     */
    private List<AttributeMapping> mergeAttributeMappings(List<AttributeMapping> existingMappings,
                                                          List<AttributeMapping> incomingMappings,
                                                          String primaryUserStoreDomain) {

        List<AttributeMapping> mergedMappings = new ArrayList<>();
        existingMappings.stream()
                .filter(mapping -> !StringUtils.equals(primaryUserStoreDomain, mapping.getUserStoreDomain()))
                .forEach(mergedMappings::add);

        Optional<AttributeMapping> incomingPrimary = incomingMappings.stream()
                .filter(mapping -> StringUtils.equals(primaryUserStoreDomain, mapping.getUserStoreDomain()))
                .findFirst();

        incomingPrimary.ifPresent(mergedMappings::add);
        return mergedMappings;
    }

    /**
     * Processes the local claim properties to handle the excluded user stores property.
     * If the property value contains the primary user store, the value is replaced with just the primary user store.
     * Otherwise, the property is removed.
     *
     * @param existingClaimProperties existing sub org local claim properties.
     * @param incomingClaimProperties the updating local claim properties.
     * @param primaryUserStoreDomain the primary user store domain.
     * @return a modified map of local claim properties with the excluded property handled.
     */
    private Map<String, String> processLocalClaimProperties(Map<String, String> existingClaimProperties,
                                                            Map<String, String> incomingClaimProperties,
                                                            String primaryUserStoreDomain) {

        Map<String, String> modifiedProperties = new HashMap<>(incomingClaimProperties);
        String excludedUserStoresProperty = ClaimConstants.EXCLUDED_USER_STORES_PROPERTY;

        String existingValue = existingClaimProperties.get(excludedUserStoresProperty);
        String incomingValue = incomingClaimProperties.get(excludedUserStoresProperty);

        List<String> existingExcludedUserStores = StringUtils.isNotBlank(existingValue)
                ? new ArrayList<>(Arrays.asList(existingValue.split(","))) : new ArrayList<>();
        List<String> incomingExcludedUserStores = StringUtils.isNotBlank(incomingValue)
                ? new ArrayList<>(Arrays.asList(incomingValue.split(","))) : new ArrayList<>();

        boolean isPrimaryUserStoreExcludedInExistingProperties = existingExcludedUserStores.stream()
                .anyMatch(store -> StringUtils.equalsIgnoreCase(store, primaryUserStoreDomain));
        boolean isPrimaryUserStoreExcludedInIncomingProperties = incomingExcludedUserStores.stream()
                .anyMatch(store -> StringUtils.equalsIgnoreCase(store, primaryUserStoreDomain));

        if (isPrimaryUserStoreExcludedInIncomingProperties) {
            if (!isPrimaryUserStoreExcludedInExistingProperties) {
                existingExcludedUserStores.add(primaryUserStoreDomain);
            }
        } else {
            existingExcludedUserStores.remove(primaryUserStoreDomain);
        }

        if (existingExcludedUserStores.isEmpty()) {
            modifiedProperties.remove(excludedUserStoresProperty);
        } else {
            modifiedProperties.put(excludedUserStoresProperty, String.join(",", existingExcludedUserStores));
        }
        return modifiedProperties;
    }

    private void handleUpdateExternalClaim(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        String claimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.CLAIM_DIALECT_URI);
        String claimURI = (String) eventProperties.get(IdentityEventConstants.EventProperty.EXTERNAL_CLAIM_URI);
        String mappedLocalClaim =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.MAPPED_LOCAL_CLAIM_URI);
        Map<String, String> claimProperties =
                (Map<String, String>) eventProperties.get(IdentityEventConstants.EventProperty.
                        EXTERNAL_CLAIM_PROPERTIES);

        try {
            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(tenantDomain)) {
                return;
            }
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                if (isExistingExternalClaimURI(claimDialectURI, claimURI, sharedOrganizationTenantDomain)) {
                    getClaimMetadataManagementService().updateExternalClaim(new ExternalClaim(claimDialectURI,
                            claimURI, mappedLocalClaim, claimProperties), sharedOrganizationTenantDomain);
                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while updating the external claim " + claimURI, e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while updating the external claim " + claimURI, e);
        }
    }

    private void handleAddExternalClaim(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        String claimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.CLAIM_DIALECT_URI);
        String claimURI = (String) eventProperties.get(IdentityEventConstants.EventProperty.EXTERNAL_CLAIM_URI);
        String mappedLocalClaim =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.MAPPED_LOCAL_CLAIM_URI);
        Map<String, String> claimProperties =
                (Map<String, String>) eventProperties.get(IdentityEventConstants.EventProperty.
                        EXTERNAL_CLAIM_PROPERTIES);
        try {
            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(tenantDomain)) {
                return;
            }
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                /* This checks if the local claim exists within the organization. Local claims are added to
                sub-organizations when updating application user attributes or when sharing applications with already
                added requested attributes. */
                if (isExistingLocalClaimURI(mappedLocalClaim, sharedOrganizationTenantDomain)) {
                    getClaimMetadataManagementService().addExternalClaim(new ExternalClaim(claimDialectURI,
                            claimURI, mappedLocalClaim, claimProperties), sharedOrganizationTenantDomain);
                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while adding the external claim " + claimURI, e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while adding the external claim " + claimURI, e);
        }
    }

    private void handleAddClaimDialect(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        String claimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.CLAIM_DIALECT_URI);
        try {
            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(tenantDomain)) {
                return;
            }
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                if (!isDialectExists(claimDialectURI, sharedOrganizationTenantDomain)) {
                    getClaimMetadataManagementService().addClaimDialect(
                            new ClaimDialect(claimDialectURI), sharedOrganizationTenantDomain);
                }
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException("An error occurred while adding the claim dialect " + claimDialectURI, e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while adding the claim dialect " + claimDialectURI, e);
        }
    }

    private void handleUpdateClaimDialect(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        String oldClaimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.OLD_CLAIM_DIALECT_URI);
        String newClaimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.NEW_CLAIM_DIALECT_URI);
        try {
            if (Utils.isClaimAndOIDCScopeInheritanceEnabled(tenantDomain)) {
                return;
            }
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                getClaimMetadataManagementService().renameClaimDialect(new ClaimDialect(oldClaimDialectURI),
                        new ClaimDialect(newClaimDialectURI), sharedOrganizationTenantDomain);
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException(
                    "An error occurred while updating the claim dialect " + oldClaimDialectURI, e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException(
                    "An error occurred while updating the claim dialect " + oldClaimDialectURI, e);
        }
    }

    private void handleDeleteClaimDialect(Event event) throws IdentityEventException {

        Map<String, Object> eventProperties = event.getEventProperties();
        int tenantId = (int) eventProperties.get(IdentityEventConstants.EventProperty.TENANT_ID);
        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        String claimDialectURI =
                (String) eventProperties.get(IdentityEventConstants.EventProperty.CLAIM_DIALECT_URI);
        try {
            String organizationId = getOrganizationManager().resolveOrganizationId(tenantDomain);
            List<BasicOrganization> childOrganizations = getOrganizationManager().
                    getChildOrganizations(organizationId, true);
            for (BasicOrganization organization : childOrganizations) {
                String sharedOrganizationTenantDomain = getOrganizationManager().
                        resolveTenantDomain(organization.getId());
                getClaimMetadataManagementService().removeClaimDialect(
                        new ClaimDialect(claimDialectURI), sharedOrganizationTenantDomain);
            }
        } catch (OrganizationManagementException e) {
            // This is to handle the scenario where the tenant is not modeled as an organization.
            if (ERROR_CODE_ORGANIZATION_NOT_FOUND_FOR_TENANT.getCode().equals(e.getErrorCode())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Organization not found for the tenant: " + tenantDomain);
                }
                return;
            }
            throw new IdentityEventException(
                    "An error occurred while deleting the claim dialect " + claimDialectURI, e);
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException(
                    "An error occurred while deleting the claim dialect " + claimDialectURI, e);
        }
    }

    private void handlePostAddOrganization(Event event) throws IdentityEventException {

        Organization createdOrganization =
                (Organization) event.getEventProperties().get(Constants.EVENT_PROP_ORGANIZATION);
        try {
            if (!OrganizationManagementUtil.isOrganization(createdOrganization.getId())) {
                return;
            }
        } catch (OrganizationManagementException e) {
            throw new IdentityEventException(
                    "Error while checking if the created organization is a sub organization.", e);
        }

        ParentOrganizationDO parentOrganization = createdOrganization.getParent();
        if (parentOrganization != null) {
            try {
                String parentOrgTenantDomain = getOrganizationManager().resolveTenantDomain(parentOrganization.getId());
                if (Utils.isClaimAndOIDCScopeInheritanceEnabled(parentOrgTenantDomain)) {
                    return;
                }
                inheritClaimPropertiesAndAttributeMapping(createdOrganization.getId(), parentOrgTenantDomain);
            } catch (OrganizationManagementException e) {
                throw new IdentityEventException("Error retrieving the tenant domain of parent organization.", e);
            }
        }
    }

    private void inheritClaimPropertiesAndAttributeMapping(String createdOrgTenantDomain, String parentOrgTenantDomain)
            throws IdentityEventException {

        try {
            List<LocalClaim> createdOrgLocalClaims =
                    getClaimMetadataManagementService().getLocalClaims(createdOrgTenantDomain);
            List<LocalClaim> parentOrgLocalClaims =
                    getClaimMetadataManagementService().getLocalClaims(parentOrgTenantDomain);
            Map<String, LocalClaim> parentClaimMap = parentOrgLocalClaims.stream()
                    .collect(Collectors.toMap(LocalClaim::getClaimURI, claim -> claim));

            // The custom local claims are not inherited as the created organization's claims are only updated.
            for (LocalClaim createdOrgClaim : createdOrgLocalClaims) {
                LocalClaim parentClaim = parentClaimMap.get(createdOrgClaim.getClaimURI());

                if (parentClaim != null) {
                    Map<String, String> updatedClaimProperties = new HashMap<>(parentClaim.getClaimProperties());

                    // The ExcludedUserStores claim property is excluded when inheriting the properties.
                    updatedClaimProperties.remove(ClaimConstants.EXCLUDED_USER_STORES_PROPERTY);
                    boolean isClaimPropertiesDifferent = !Objects.equals(
                            createdOrgClaim.getClaimProperties(), updatedClaimProperties);

                    // Only the PRIMARY user store attribute mapping gets inherited.
                    List<AttributeMapping> primaryDomainParentMappedAttributes = parentClaim.getMappedAttributes()
                            .stream().filter(attr -> UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME
                                    .equals(attr.getUserStoreDomain()))
                            .collect(Collectors.toList());
                    boolean isMappedAttributesDifferent = !Objects.equals(
                            createdOrgClaim.getMappedAttributes().get(0).getAttributeName(),
                            primaryDomainParentMappedAttributes.get(0).getAttributeName());

                    // If either the PRIMARY user store attribute mapping or any of the claim properties differ from
                    // the parent organization, the claim is updated in the created organization.
                    if (isClaimPropertiesDifferent || isMappedAttributesDifferent) {
                        parentClaim.setClaimProperties(updatedClaimProperties);
                        parentClaim.setMappedAttributes(primaryDomainParentMappedAttributes);
                        getClaimMetadataManagementService().updateLocalClaim(parentClaim, createdOrgTenantDomain);
                    }
                }
            }
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while inheriting parent organization claims.", e);
        }
    }

    private void addClaimsToSubOrganization(List<LocalClaim> parentOrgCustomLocalClaims,
                                            String sharedOrganizationTenantDomain,
                                            List<String> missingClaims, String parentTenantDomain)
            throws IdentityEventException {

        for (String claimURI : missingClaims) {
            Optional<LocalClaim> matchingClaim = parentOrgCustomLocalClaims.stream()
                    .filter(claim -> claimURI.equals(claim.getClaimURI()))
                    .findFirst();
            if (matchingClaim.isPresent()) {
                LocalClaim claim = matchingClaim.get();
                try {
                    getClaimMetadataManagementService().addLocalClaim(claim, sharedOrganizationTenantDomain);

                    List<Claim> externalClaimMappingsInParentOrg =
                            getMappedExternalClaims(claimURI, parentTenantDomain);
                    if (!externalClaimMappingsInParentOrg.isEmpty()) {
                        for (Claim externalClaim : externalClaimMappingsInParentOrg) {
                            if (!isDialectExists(externalClaim.getClaimDialectURI(), sharedOrganizationTenantDomain)) {
                                getClaimMetadataManagementService().addClaimDialect(new ClaimDialect(
                                        externalClaim.getClaimDialectURI()), sharedOrganizationTenantDomain);
                            }
                            getClaimMetadataManagementService().addExternalClaim((new ExternalClaim(
                                            externalClaim.getClaimDialectURI(), externalClaim.getClaimURI(), claimURI)),
                                    sharedOrganizationTenantDomain);
                        }
                    }
                } catch (ClaimMetadataException e) {
                    throw new IdentityEventException("An error occurred while adding claims to the sub-organization",
                            e);
                }
            }
        }
    }

    private List<String> getMissingClaims(String sharedOrganizationTenantDomain, ClaimMapping[] claimMappings)
            throws IdentityEventException {

        try {
            List<LocalClaim> subOrgLocalClaims = getClaimMetadataManagementService().
                    getLocalClaims(sharedOrganizationTenantDomain);
            Set<String> subOrgClaimURIs = subOrgLocalClaims.stream().map(LocalClaim::getClaimURI)
                    .collect(Collectors.toSet());
            List<String> missingClaims = new ArrayList<>();
            for (ClaimMapping claimMapping : claimMappings) {
                String claimURI = claimMapping.getLocalClaim().getClaimUri();
                if (!subOrgClaimURIs.contains(claimURI)) {
                    missingClaims.add(claimURI);
                }
            }
            return missingClaims;
        } catch (ClaimMetadataException e) {
            throw new IdentityEventException("An error occurred while getting claims from the sub-organization", e);
        }
    }

    private List<Claim> getMappedExternalClaims(String localClaimURI, String tenantDomain)
            throws ClaimMetadataException {

        return getClaimMetadataManagementService().getMappedExternalClaimsForLocalClaim(localClaimURI, tenantDomain);
    }

    private ClaimMetadataManagementService getClaimMetadataManagementService() {

        return OrgApplicationMgtDataHolder.getInstance().getClaimMetadataManagementService();
    }

    private OrganizationManager getOrganizationManager() {

        return OrgApplicationMgtDataHolder.getInstance().getOrganizationManager();
    }

    private OrgApplicationManager getOrgApplicationManager() {

        return new OrgApplicationManagerImpl();
    }

    private Optional<LocalClaim> getExistingLocalClaim(String localClaimURI, String tenantDomain)
            throws ClaimMetadataException {

        return getClaimMetadataManagementService().getLocalClaims(tenantDomain).stream().filter(
                claim -> claim.getClaimURI().equalsIgnoreCase(localClaimURI)).findFirst();
    }

    private boolean isExistingLocalClaimURI(String localClaimURI, String tenantDomain) throws ClaimMetadataException {

        return getClaimMetadataManagementService().getLocalClaims(tenantDomain).stream().filter(
                claim -> claim.getClaimURI().equalsIgnoreCase(localClaimURI)).findFirst().isPresent();
    }

    private boolean isExistingExternalClaimURI(String externalClaimDialectURI, String externalClaimURI,
                                               String tenantDomain) throws ClaimMetadataException {

        return getClaimMetadataManagementService().getExternalClaims(externalClaimDialectURI, tenantDomain).stream().
                filter(claim -> claim.getClaimURI().equalsIgnoreCase(externalClaimURI)).findFirst().isPresent();
    }

    private boolean isDialectExists(String dialectURI, String tenantDomain) throws ClaimMetadataException {

        List<ClaimDialect> claimDialectList = getClaimMetadataManagementService().getClaimDialects(tenantDomain);
        return claimDialectList.stream()
                .anyMatch(dialect -> StringUtils.equals(dialect.getClaimDialectURI(), dialectURI));
    }

    private String getPrimaryUserStoreDomain(String tenantDomain) throws UserStoreException {

        String primaryUserStoreDomain = OrgApplicationMgtDataHolder.getInstance().getRealmService()
                .getTenantUserRealm(IdentityTenantUtil.getTenantId(tenantDomain)).getRealmConfiguration()
                .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_DOMAIN_NAME);
        if (StringUtils.isEmpty(primaryUserStoreDomain)) {
            primaryUserStoreDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
        }
        return primaryUserStoreDomain;
    }
}
