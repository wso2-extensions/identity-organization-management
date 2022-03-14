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

CREATE TABLE IF NOT EXISTS UM_ORG (
    UM_ID VARCHAR(255) NOT NULL,
    UM_ORG_NAME VARCHAR(255) NOT NULL,
    UM_ORG_DESCRIPTION VARCHAR(1024),
    UM_CREATED_TIME TIMESTAMP NOT NULL,
    UM_LAST_MODIFIED TIMESTAMP  NOT NULL,
    UM_STATUS VARCHAR(255) DEFAULT 'ACTIVE' NOT NULL,
    UM_TENANT_ID INTEGER DEFAULT 0,
    UM_PARENT_ID VARCHAR(255),
    PRIMARY KEY (UM_ID),
    UNIQUE(UM_ORG_NAME, UM_TENANT_ID),
    FOREIGN KEY (UM_PARENT_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS UM_ORG_ATTRIBUTE (
    UM_ID INTEGER NOT NULL AUTO_INCREMENT,
    UM_ORG_ID VARCHAR(255) NOT NULL,
    UM_ATTRIBUTE_KEY VARCHAR(255) NOT NULL,
    UM_ATTRIBUTE_VALUE VARCHAR(512),
    PRIMARY KEY (UM_ID),
    UNIQUE (UM_ORG_ID, UM_ATTRIBUTE_KEY),
    FOREIGN KEY (UM_ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS UM_USER_ROLE_ORG (
    UM_ID VARCHAR2(255) NOT NULL,
    UM_USER_ID VARCHAR2(255) NOT NULL,
    UM_ROLE_ID VARCHAR2(1024) NOT NULL,
    UM_TENANT_ID INTEGER DEFAULT 0,
    ORG_ID VARCHAR2(255) NOT NULL,
    ASSIGNED_AT VARCHAR2(255) NOT NULL,
    FORCED INTEGER DEFAULT 0,
    PRIMARY KEY (UM_ID),
    CONSTRAINT FK_UM_USER_ROLE_ORG_UM_ORG FOREIGN KEY (ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE,
    CONSTRAINT FK_UM_USER_ROLE_ORG_ASSIGNED_AT FOREIGN KEY (ASSIGNED_AT) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
    );