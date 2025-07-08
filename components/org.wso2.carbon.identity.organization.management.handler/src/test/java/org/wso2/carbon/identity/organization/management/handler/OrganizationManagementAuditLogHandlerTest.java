/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.common.testng.WithCarbonHome;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.organization.management.ext.Constants;
import org.wso2.carbon.identity.organization.management.service.model.Organization;
import org.wso2.carbon.identity.organization.management.service.model.OrganizationAttribute;
import org.wso2.carbon.identity.organization.management.service.model.ParentOrganizationDO;
import org.wso2.carbon.identity.organization.management.service.model.PatchOperation;
import org.wso2.carbon.utils.AuditLog;
import org.wso2.carbon.utils.CarbonUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.management.service.constant.OrganizationManagementConstants.PATCH_OP_REPLACE;

/**
 * Organization management audit logger test class.
 */
@WithCarbonHome
public class OrganizationManagementAuditLogHandlerTest {

    private static final String TEST_INITIATOR_ID = "e90d87d8-b4d7-4428-a227-b62e727d4c6e";
    private static final String TEST_ID = "ca7cf330-964b-4257-94d1-328fc4842c78";
    private static final String TEST_NAME = "TEST_ORG_NAME";
    private static final String TEST_DESCRIPTION = "TEST_ORG_DESCRIPTION";
    private static final String TEST_STATUS = "ACTIVE";
    private static final String TEST_TYPE = "TENANT";
    private static final String TEST_PARENT_ID = "10084a8d-113f-4211-a0d5-efe36b082211";
    private static final Instant TEST_CREATED_TIME = Instant.now();
    private static final Instant TEST_LAST_MODIFIED_TIME = TEST_CREATED_TIME.plusSeconds(5);
    private static final String TEST_CREATOR_ID = "e4d6337c-e6f6-459a-9d46-3c7d983b447e";
    private static final String TEST_CREATOR_USERNAME = "alex";
    private static final String TEST_CREATOR_EMAIL = "alex@abc.com";
    private static final String TEST_ATTRIBUTE_KEY_1 = "TEST_ATTRIBUTE_KEY_1";
    private static final String TEST_ATTRIBUTE_VALUE_1 = "TEST_ATTRIBUTE_VALUE_1";
    private static final String TEST_ATTRIBUTE_KEY_2 = "TEST_ATTRIBUTE_KEY_2";
    private static final String TEST_ATTRIBUTE_VALUE_2 = "TEST_ATTRIBUTE_VALUE_2";
    private static final String TEST_ATTRIBUTE_KEY_3 = "TEST_ATTRIBUTE_KEY_3";
    private static final String TEST_ATTRIBUTE_VALUE_3 = "TEST_ATTRIBUTE_VALUE_3";
    private static final String TEST_DISABLED_STATUS = "DISABLED";
    private static final String TEST_PATCHED_NAME = "TEST_ORG_PATCHED_NAME";
    private static final String TEST_PATCHED_ATTRIBUTE_VALUE_1 = "TEST_PATCHED_ATTRIBUTE_VALUE_1";

    private static final String ID = "Id";
    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";
    private static final String STATUS = "Status";
    private static final String TYPE = "Type";
    private static final String PARENT_ORG_ID = "ParentOrganizationId";
    private static final String LAST_MODIFIED_TIME = "LastModifiedTime";
    private static final String CREATED_TIME = "CreatedTime";
    private static final String ATTRIBUTES = "Attributes";
    private static final String CREATOR = "Creator";
    private static final String CREATOR_ID = "Id";
    private static final String CREATOR_USERNAME = "Username";
    private static final String CREATOR_EMAIL = "Email";

    private static final String PATCH_ADDED = "Added";
    private static final String PATCH_REPLACED = "Replaced";
    private static final String PATCH_REMOVED = "Removed";

    private static final String ADD_ORGANIZATION = "add-organization";
    private static final String UPDATE_ORGANIZATION = "update-organization";
    private static final String DELETE_ORGANIZATION = "delete-organization";

    private OrganizationManagementAuditLogHandler auditLogger;
    private MockedStatic<CarbonContext> mockedCarbonContext;
    private MockedStatic<IdentityUtil> mockedIdentityUtil;
    private MockedStatic<LoggerUtils> mockedLoggerUtils;
    private MockedStatic<CarbonUtils> mockedCarbonUtils;
    private Log mockV1Logger;
    private Organization organization;
    private Gson gson;

    @BeforeMethod
    public void setUp(Method method) throws Exception {

        auditLogger = OrganizationManagementAuditLogHandler.getInstance();
        gson = new Gson();
        mockedCarbonContext = mockStatic(CarbonContext.class);
        CarbonContext carbonContext = mock(CarbonContext.class);
        mockedCarbonContext.when(CarbonContext::getThreadLocalCarbonContext).thenReturn(carbonContext);
        when(carbonContext.getUsername()).thenReturn("testUser");
        when(carbonContext.getTenantDomain()).thenReturn("carbon.super");

        mockedIdentityUtil = mockStatic(IdentityUtil.class);
        mockedIdentityUtil.when(() -> IdentityUtil.getInitiatorId("testUser", "carbon.super"))
                .thenReturn(TEST_INITIATOR_ID);

        mockedLoggerUtils = mockStatic(LoggerUtils.class);
        mockedLoggerUtils.when(() -> LoggerUtils.getMaskedContent(any(String.class))).thenCallRealMethod();
        mockedLoggerUtils.when(() -> LoggerUtils.getInitiatorType(any(String.class)))
                .thenReturn(LoggerUtils.Initiator.User.name());

        mockedCarbonUtils = mockStatic(CarbonUtils.class);

        if (method.getAnnotation(Test.class).groups()[0].equals("V1")) {
            mockV1Logger = mock(Log.class);
            setFinalFieldWithReflection(auditLogger, mockV1Logger);
            mockedLoggerUtils.when(LoggerUtils::isEnableV2AuditLogs).thenReturn(false);
            mockedCarbonUtils.when(CarbonUtils::isLegacyAuditLogsDisabled).thenReturn(false);
        }
        if (method.getAnnotation(Test.class).groups()[0].equals("V2")) {
            mockedLoggerUtils.when(LoggerUtils::isEnableV2AuditLogs).thenReturn(true);
            mockedCarbonUtils.when(CarbonUtils::isLegacyAuditLogsDisabled).thenReturn(true);
        }

        organization = new Organization();
        organization.setId(TEST_ID);
        organization.setName(TEST_NAME);
        organization.setDescription(TEST_DESCRIPTION);
        organization.setStatus(TEST_STATUS);
        organization.setType(TEST_TYPE);
        ParentOrganizationDO parentOrganization = new ParentOrganizationDO();
        parentOrganization.setId(TEST_PARENT_ID);
        organization.setParent(parentOrganization);
        organization.setCreated(TEST_CREATED_TIME);
        organization.setLastModified(TEST_LAST_MODIFIED_TIME);
        organization.setCreatorId(TEST_CREATOR_ID);
        organization.setCreatorUsername(TEST_CREATOR_USERNAME);
        organization.setCreatorEmail(TEST_CREATOR_EMAIL);
        OrganizationAttribute attribute1 = new OrganizationAttribute();
        attribute1.setKey(TEST_ATTRIBUTE_KEY_1);
        attribute1.setValue(TEST_ATTRIBUTE_VALUE_1);
        OrganizationAttribute attribute2 = new OrganizationAttribute();
        attribute2.setKey(TEST_ATTRIBUTE_KEY_2);
        attribute2.setValue(TEST_ATTRIBUTE_VALUE_2);
        organization.getAttributes().add(attribute1);
        organization.getAttributes().add(attribute2);
    }

    @AfterMethod
    public void tearDown() {

        auditLogger = null;
        mockedCarbonContext.close();
        mockedIdentityUtil.close();
        mockedLoggerUtils.close();
        mockedCarbonUtils.close();
    }

    @Test(groups = "V2")
    public void testLogOrganizationCreation() throws NoSuchFieldException,
            IllegalAccessException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);

        auditLogger.handleEvent(event);
        AuditLog.AuditLogBuilder capturedOrgCreationAuditLog = captureTriggerAuditLogEventArgs();

        Assert.assertNotNull(capturedOrgCreationAuditLog);
        assertAuditLoggerData(capturedOrgCreationAuditLog, ADD_ORGANIZATION);

        Field dataField = AuditLog.AuditLogBuilder.class.getDeclaredField("data");
        dataField.setAccessible(true);
        Map<String, Object> dataMap = (Map<String, Object>) dataField.get(capturedOrgCreationAuditLog);
        Assert.assertEquals(dataMap.get(ID).toString(), TEST_ID);
        Assert.assertEquals(dataMap.get(NAME).toString(), TEST_NAME);
        Assert.assertEquals(dataMap.get(DESCRIPTION).toString(), TEST_DESCRIPTION);
        Assert.assertEquals(dataMap.get(STATUS).toString(), TEST_STATUS);
        Assert.assertEquals(dataMap.get(TYPE).toString(), TEST_TYPE);
        Assert.assertEquals(dataMap.get(PARENT_ORG_ID).toString(), TEST_PARENT_ID);
        Assert.assertEquals(dataMap.get(CREATED_TIME).toString(), TEST_CREATED_TIME.toString());
        Assert.assertEquals(dataMap.get(LAST_MODIFIED_TIME).toString(), TEST_LAST_MODIFIED_TIME.toString());

        Map<String, Object> creator = (Map<String, Object>) dataMap.get(CREATOR);
        Assert.assertEquals(creator.get(CREATOR_ID).toString(), TEST_CREATOR_ID);
        Assert.assertEquals(creator.get(CREATOR_USERNAME).toString(),
                LoggerUtils.getMaskedContent(TEST_CREATOR_USERNAME));
        Assert.assertEquals(creator.get(CREATOR_EMAIL).toString(),
                LoggerUtils.getMaskedContent(TEST_CREATOR_EMAIL));

        Map<String, Object> attributes = (Map<String, Object>) dataMap.get(ATTRIBUTES);
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_1).toString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_1));
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_2).toString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_2));
    }

    @Test(groups = "V2")
    public void testLogUpdateOrganization() throws NoSuchFieldException,
            IllegalAccessException {

        organization.setCreatorId(null);
        organization.setCreatorUsername(null);
        organization.setCreatorEmail(null);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, TEST_ID);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_UPDATE_ORGANIZATION, eventProperties);

        auditLogger.handleEvent(event);
        AuditLog.AuditLogBuilder capturedArg = captureTriggerAuditLogEventArgs();

        Assert.assertNotNull(capturedArg);
        assertAuditLoggerData(capturedArg, UPDATE_ORGANIZATION);

        Field dataField = AuditLog.AuditLogBuilder.class.getDeclaredField("data");
        dataField.setAccessible(true);
        Map<String, Object> dataMap = (Map<String, Object>) dataField.get(capturedArg);
        Assert.assertEquals(dataMap.get(ID).toString(), TEST_ID);
        Assert.assertEquals(dataMap.get(NAME).toString(), TEST_NAME);
        Assert.assertEquals(dataMap.get(DESCRIPTION).toString(), TEST_DESCRIPTION);
        Assert.assertEquals(dataMap.get(STATUS).toString(), TEST_STATUS);
        Assert.assertEquals(dataMap.get(TYPE).toString(), TEST_TYPE);
        Assert.assertEquals(dataMap.get(PARENT_ORG_ID).toString(), TEST_PARENT_ID);
        Assert.assertEquals(dataMap.get(CREATED_TIME).toString(), TEST_CREATED_TIME.toString());
        Assert.assertEquals(dataMap.get(LAST_MODIFIED_TIME).toString(), TEST_LAST_MODIFIED_TIME.toString());
        Assert.assertNull(dataMap.get(CREATOR));

        Map<String, Object> attributes = (Map<String, Object>) dataMap.get(ATTRIBUTES);
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_1).toString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_1));
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_2).toString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_2));
    }

    @Test(groups = "V2")
    public void testLogPatchOrganization() throws NoSuchFieldException,
            IllegalAccessException {

        auditLogger.handleEvent(getMockPatchEvent());
        AuditLog.AuditLogBuilder capturedArg = captureTriggerAuditLogEventArgs();

        Assert.assertNotNull(capturedArg);
        assertAuditLoggerData(capturedArg, UPDATE_ORGANIZATION);

        Field dataField = AuditLog.AuditLogBuilder.class.getDeclaredField("data");
        dataField.setAccessible(true);
        Map<String, Object> dataMap = (Map<String, Object>) dataField.get(capturedArg);
        Assert.assertEquals(dataMap.get(ID).toString(), TEST_ID);

        Map<String, Object> addedDataMap = (Map<String, Object>) dataMap.get(PATCH_ADDED);
        Assert.assertEquals(addedDataMap.get(ATTRIBUTES + "/" + TEST_ATTRIBUTE_KEY_3).toString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_3));

        Map<String, Object> replacedDataMap = (Map<String, Object>) dataMap.get(PATCH_REPLACED);
        Assert.assertEquals(replacedDataMap.get(STATUS).toString(), TEST_DISABLED_STATUS);
        Assert.assertEquals(replacedDataMap.get(NAME).toString(), TEST_PATCHED_NAME);
        Assert.assertEquals(replacedDataMap.get(ATTRIBUTES + "/" + TEST_ATTRIBUTE_KEY_1).toString(),
                LoggerUtils.getMaskedContent(TEST_PATCHED_ATTRIBUTE_VALUE_1));

        List<String> removedDataMap = (List<String>) dataMap.get(PATCH_REMOVED);
        Assert.assertTrue(removedDataMap.contains(DESCRIPTION));
        Assert.assertTrue(removedDataMap.contains(ATTRIBUTES + "/" + TEST_ATTRIBUTE_KEY_2));
    }

    @Test(groups = "V2")
    public void testLogDeleteOrganization() throws NoSuchFieldException,
            IllegalAccessException {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, TEST_ID);
        Event event = new Event(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);

        auditLogger.handleEvent(event);
        AuditLog.AuditLogBuilder capturedArg = captureTriggerAuditLogEventArgs();

        Assert.assertNotNull(capturedArg);
        assertAuditLoggerData(capturedArg, DELETE_ORGANIZATION);

        Field dataField = AuditLog.AuditLogBuilder.class.getDeclaredField("data");
        dataField.setAccessible(true);
        Map<String, Object> dataMap = (Map<String, Object>) dataField.get(capturedArg);
        Assert.assertEquals(dataMap.get(ID).toString(), TEST_ID);
    }

    private AuditLog.AuditLogBuilder captureTriggerAuditLogEventArgs() {

        ArgumentCaptor<AuditLog.AuditLogBuilder> auditLogBuilderCaptor = ArgumentCaptor.
                forClass(AuditLog.AuditLogBuilder.class);
        mockedLoggerUtils.verify(() -> LoggerUtils.triggerAuditLogEvent(auditLogBuilderCaptor.capture()));
        return auditLogBuilderCaptor.getValue();
    }

    private void assertAuditLoggerData(AuditLog.AuditLogBuilder auditLogBuilder, String operation)
            throws NoSuchFieldException, IllegalAccessException {

        Assert.assertEquals(extractField("initiatorId", auditLogBuilder), TEST_INITIATOR_ID);
        Assert.assertEquals(extractField("initiatorType", auditLogBuilder), LoggerUtils.Initiator.User.name());
        Assert.assertEquals(extractField("targetId", auditLogBuilder), TEST_ID);
        Assert.assertEquals(extractField("targetType", auditLogBuilder),
                LoggerUtils.Target.Organization.name());
        switch (operation) {
            case ADD_ORGANIZATION:
                Assert.assertEquals(extractField("action", auditLogBuilder), ADD_ORGANIZATION);
                break;
            case UPDATE_ORGANIZATION:
                Assert.assertEquals(extractField("action", auditLogBuilder), UPDATE_ORGANIZATION);
                break;
            case DELETE_ORGANIZATION:
                Assert.assertEquals(extractField("action", auditLogBuilder), DELETE_ORGANIZATION);
                break;
        }
    }

    private String extractField(String fieldName, AuditLog.AuditLogBuilder auditLogBuilder)
            throws NoSuchFieldException, IllegalAccessException {

        Field dataField = AuditLog.AuditLogBuilder.class.getDeclaredField(fieldName);
        dataField.setAccessible(true);
        return (String) dataField.get(auditLogBuilder);
    }

    private static Event getMockPatchEvent() {

        PatchOperation statusPatch = new PatchOperation();
        statusPatch.setOp(PATCH_OP_REPLACE);
        statusPatch.setPath("/status");
        statusPatch.setValue(TEST_DISABLED_STATUS);

        PatchOperation namePatch = new PatchOperation();
        namePatch.setOp(PATCH_OP_REPLACE);
        namePatch.setPath("/name");
        namePatch.setValue(TEST_PATCHED_NAME);

        PatchOperation descriptionPatch = new PatchOperation();
        descriptionPatch.setOp(PATCH_OP_REMOVE);
        descriptionPatch.setPath("/description");

        PatchOperation attributePatch1 = new PatchOperation();
        attributePatch1.setOp(PATCH_OP_REPLACE);
        attributePatch1.setPath(TEST_ATTRIBUTE_KEY_1);
        attributePatch1.setValue(TEST_PATCHED_ATTRIBUTE_VALUE_1);

        PatchOperation attributePatch2 = new PatchOperation();
        attributePatch2.setOp(PATCH_OP_REMOVE);
        attributePatch2.setPath(TEST_ATTRIBUTE_KEY_2);

        PatchOperation attributePatch3 = new PatchOperation();
        attributePatch3.setOp(PATCH_OP_ADD);
        attributePatch3.setPath(TEST_ATTRIBUTE_KEY_3);
        attributePatch3.setValue(TEST_ATTRIBUTE_VALUE_3);

        List<PatchOperation> patchOperations = new ArrayList<>();
        patchOperations.add(statusPatch);
        patchOperations.add(namePatch);
        patchOperations.add(descriptionPatch);
        patchOperations.add(attributePatch1);
        patchOperations.add(attributePatch2);
        patchOperations.add(attributePatch3);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, TEST_ID);
        eventProperties.put(Constants.EVENT_PROP_PATCH_OPERATIONS, patchOperations);
        return new Event(Constants.EVENT_POST_PATCH_ORGANIZATION, eventProperties);
    }

    @Test(groups = "V1")
    public void testV1LogOrganizationCreation() {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_ADD_ORGANIZATION, eventProperties);

        auditLogger.handleEvent(event);

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockV1Logger).info(logCaptor.capture());

        String logMessage = logCaptor.getValue();
        assertV1LogMessage(logMessage, ADD_ORGANIZATION);

        // Extract and verify data JSON
        String dataJson = extractDataFromV1LogMessage(logMessage);
        JsonObject dataObject = gson.fromJson(dataJson, JsonObject.class);

        Assert.assertEquals(dataObject.get(ID).getAsString(), TEST_ID);
        Assert.assertEquals(dataObject.get(NAME).getAsString(), TEST_NAME);
        Assert.assertEquals(dataObject.get(DESCRIPTION).getAsString(), TEST_DESCRIPTION);
        Assert.assertEquals(dataObject.get(STATUS).getAsString(), TEST_STATUS);
        Assert.assertEquals(dataObject.get(TYPE).getAsString(), TEST_TYPE);
        Assert.assertEquals(dataObject.get(PARENT_ORG_ID).getAsString(), TEST_PARENT_ID);
        Assert.assertEquals(dataObject.get(CREATED_TIME).getAsString(), TEST_CREATED_TIME.toString());
        Assert.assertEquals(dataObject.get(LAST_MODIFIED_TIME).getAsString(), TEST_LAST_MODIFIED_TIME.toString());

        JsonObject creator = dataObject.getAsJsonObject(CREATOR);
        Assert.assertEquals(creator.get(CREATOR_ID).getAsString(), TEST_CREATOR_ID);
        Assert.assertEquals(creator.get(CREATOR_USERNAME).getAsString(),
                LoggerUtils.getMaskedContent(TEST_CREATOR_USERNAME));
        Assert.assertEquals(creator.get(CREATOR_EMAIL).getAsString(),
                LoggerUtils.getMaskedContent(TEST_CREATOR_EMAIL));

        JsonObject attributes = dataObject.getAsJsonObject(ATTRIBUTES);
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_1).getAsString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_1));
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_2).getAsString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_2));
    }

    @Test(groups = "V1")
    public void testV1LogUpdateOrganization() {

        organization.setCreatorId(null);
        organization.setCreatorUsername(null);
        organization.setCreatorEmail(null);

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, TEST_ID);
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION, organization);
        Event event = new Event(Constants.EVENT_POST_UPDATE_ORGANIZATION, eventProperties);

        auditLogger.handleEvent(event);

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockV1Logger).info(logCaptor.capture());

        String logMessage = logCaptor.getValue();
        assertV1LogMessage(logMessage, UPDATE_ORGANIZATION);

        // Extract and verify data JSON
        String dataJson = extractDataFromV1LogMessage(logMessage);
        JsonObject dataObject = gson.fromJson(dataJson, JsonObject.class);

        Assert.assertEquals(dataObject.get(ID).getAsString(), TEST_ID);
        Assert.assertEquals(dataObject.get(NAME).getAsString(), TEST_NAME);
        Assert.assertEquals(dataObject.get(DESCRIPTION).getAsString(), TEST_DESCRIPTION);
        Assert.assertEquals(dataObject.get(STATUS).getAsString(), TEST_STATUS);
        Assert.assertEquals(dataObject.get(TYPE).getAsString(), TEST_TYPE);
        Assert.assertEquals(dataObject.get(PARENT_ORG_ID).getAsString(), TEST_PARENT_ID);
        Assert.assertEquals(dataObject.get(CREATED_TIME).getAsString(), TEST_CREATED_TIME.toString());
        Assert.assertEquals(dataObject.get(LAST_MODIFIED_TIME).getAsString(), TEST_LAST_MODIFIED_TIME.toString());
        Assert.assertNull(dataObject.get(CREATOR));

        JsonObject attributes = dataObject.getAsJsonObject(ATTRIBUTES);
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_1).getAsString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_1));
        Assert.assertEquals(attributes.get(TEST_ATTRIBUTE_KEY_2).getAsString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_2));
    }

    @Test(groups = "V1")
    public void testV1LogPatchOrganization() {

        auditLogger.handleEvent(getMockPatchEvent());

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockV1Logger).info(logCaptor.capture());

        String logMessage = logCaptor.getValue();
        assertV1LogMessage(logMessage, UPDATE_ORGANIZATION);

        // Extract and verify data JSON
        String dataJson = extractDataFromV1LogMessage(logMessage);
        JsonObject dataObject = gson.fromJson(dataJson, JsonObject.class);

        Assert.assertEquals(dataObject.get(ID).getAsString(), TEST_ID);

        JsonObject addedData = dataObject.getAsJsonObject(PATCH_ADDED);
        Assert.assertEquals(addedData.get(ATTRIBUTES + "/" + TEST_ATTRIBUTE_KEY_3).getAsString(),
                LoggerUtils.getMaskedContent(TEST_ATTRIBUTE_VALUE_3));

        JsonObject replacedData = dataObject.getAsJsonObject(PATCH_REPLACED);
        Assert.assertEquals(replacedData.get(STATUS).getAsString(), TEST_DISABLED_STATUS);
        Assert.assertEquals(replacedData.get(NAME).getAsString(), TEST_PATCHED_NAME);
        Assert.assertEquals(replacedData.get(ATTRIBUTES + "/" + TEST_ATTRIBUTE_KEY_1).getAsString(),
                LoggerUtils.getMaskedContent(TEST_PATCHED_ATTRIBUTE_VALUE_1));

        JsonArray removedData = dataObject.getAsJsonArray(PATCH_REMOVED);
        Assert.assertTrue(removedData.toString().contains(DESCRIPTION));
        Assert.assertTrue(removedData.toString().contains(ATTRIBUTES + "/" + TEST_ATTRIBUTE_KEY_2));
    }

    @Test(groups = "V1")
    public void testV1LogDeleteOrganization() {

        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put(Constants.EVENT_PROP_ORGANIZATION_ID, TEST_ID);
        Event event = new Event(Constants.EVENT_POST_DELETE_ORGANIZATION, eventProperties);

        auditLogger.handleEvent(event);

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockV1Logger).info(logCaptor.capture());

        String logMessage = logCaptor.getValue();
        assertV1LogMessage(logMessage, DELETE_ORGANIZATION);

        // Extract and verify data JSON
        String dataJson = extractDataFromV1LogMessage(logMessage);
        JsonObject dataObject = gson.fromJson(dataJson, JsonObject.class);

        Assert.assertEquals(dataObject.get(ID).getAsString(), TEST_ID);
    }

    private void assertV1LogMessage(String logMessage, String expectedAction) {

        Assert.assertTrue(logMessage.contains("Initiator : " + TEST_INITIATOR_ID));
        Assert.assertTrue(logMessage.contains("Action : " + expectedAction));
        Assert.assertTrue(logMessage.contains("Target : " + TEST_ID));
    }

    private String extractDataFromV1LogMessage(String logMessage) {

        String dataPrefix = "Data : { ";
        String dataSuffix = " } | Result :";

        int startIndex = logMessage.indexOf(dataPrefix) + dataPrefix.length();
        int endIndex = logMessage.indexOf(dataSuffix);

        return logMessage.substring(startIndex, endIndex);
    }

    private void setFinalFieldWithReflection(Object target, Object value) throws Exception {

        Field field;
        try {
            field = target.getClass().getDeclaredField("LOG");
        } catch (NoSuchFieldException e) {
            field = target.getClass().getSuperclass().getDeclaredField("LOG");
        }

        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(target, value);
    }
}
