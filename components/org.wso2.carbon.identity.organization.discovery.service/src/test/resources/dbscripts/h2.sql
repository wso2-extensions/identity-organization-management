CREATE TABLE IF NOT EXISTS UM_ORG (
    UM_ID VARCHAR(255) NOT NULL,
    UM_ORG_NAME VARCHAR(255) NOT NULL,
    UM_ORG_DESCRIPTION VARCHAR(1024),
    UM_CREATED_TIME TIMESTAMP NOT NULL,
    UM_LAST_MODIFIED TIMESTAMP  NOT NULL,
    UM_STATUS VARCHAR(255) DEFAULT 'ACTIVE' NOT NULL,
    UM_PARENT_ID VARCHAR(255),
    UM_ORG_TYPE VARCHAR(100) NOT NULL,
    PRIMARY KEY (UM_ID),
    FOREIGN KEY (UM_PARENT_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
);

INSERT INTO UM_ORG(UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_ORG_TYPE)
SELECT UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_ORG_TYPE FROM (
	SELECT
	    '10084a8d-113f-4211-a0d5-efe36b082211' AS UM_ID,
	    'Super' AS UM_ORG_NAME,
	    'This is the super organization.' AS UM_ORG_DESCRIPTION,
	    CURRENT_TIMESTAMP AS UM_CREATED_TIME,
	    CURRENT_TIMESTAMP AS UM_LAST_MODIFIED,
	    'ACTIVE' AS UM_STATUS,
	    'TENANT' AS UM_ORG_TYPE
) S
WHERE NOT EXISTS (SELECT * FROM UM_ORG org WHERE org.UM_ID = S.UM_ID);

INSERT INTO UM_ORG(UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_PARENT_ID, UM_ORG_TYPE)
SELECT UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_PARENT_ID, UM_ORG_TYPE FROM (
	SELECT
	    '20084a8d-113f-4211-a0d5-efe36b082212' AS UM_ID,
	    'WSO2' AS UM_ORG_NAME,
	    'This is WSO2 organization.' AS UM_ORG_DESCRIPTION,
	    CURRENT_TIMESTAMP AS UM_CREATED_TIME,
	    CURRENT_TIMESTAMP AS UM_LAST_MODIFIED,
	    'ACTIVE' AS UM_STATUS,
	    '10084a8d-113f-4211-a0d5-efe36b082211' AS UM_PARENT_ID,
	    'TENANT' AS UM_ORG_TYPE
) S
WHERE NOT EXISTS (SELECT * FROM UM_ORG org WHERE org.UM_ID = S.UM_ID);

INSERT INTO UM_ORG(UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_PARENT_ID, UM_ORG_TYPE)
SELECT UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_PARENT_ID, UM_ORG_TYPE FROM (
	SELECT
	    '30084a8d-113f-4211-a0d5-efe36b082213' AS UM_ID,
	    'ABC' AS UM_ORG_NAME,
	    'This is ABC organization.' AS UM_ORG_DESCRIPTION,
	    CURRENT_TIMESTAMP AS UM_CREATED_TIME,
	    CURRENT_TIMESTAMP AS UM_LAST_MODIFIED,
	    'ACTIVE' AS UM_STATUS,
	    '10084a8d-113f-4211-a0d5-efe36b082211' AS UM_PARENT_ID,
	    'TENANT' AS UM_ORG_TYPE
) S
WHERE NOT EXISTS (SELECT * FROM UM_ORG org WHERE org.UM_ID = S.UM_ID);

INSERT INTO UM_ORG(UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_PARENT_ID, UM_ORG_TYPE)
SELECT UM_ID, UM_ORG_NAME, UM_ORG_DESCRIPTION, UM_CREATED_TIME, UM_LAST_MODIFIED, UM_STATUS, UM_PARENT_ID, UM_ORG_TYPE FROM (
	SELECT
	    '40084a8d-113f-4211-a0d5-efe36b082214' AS UM_ID,
	    'XYZ' AS UM_ORG_NAME,
	    'This is XYZ organization.' AS UM_ORG_DESCRIPTION,
	    CURRENT_TIMESTAMP AS UM_CREATED_TIME,
	    CURRENT_TIMESTAMP AS UM_LAST_MODIFIED,
	    'ACTIVE' AS UM_STATUS,
	    '30084a8d-113f-4211-a0d5-efe36b082213' AS UM_PARENT_ID,
	    'TENANT' AS UM_ORG_TYPE
) S
WHERE NOT EXISTS (SELECT * FROM UM_ORG org WHERE org.UM_ID = S.UM_ID);

CREATE TABLE IF NOT EXISTS UM_ORG_DISCOVERY (
            UM_ID INTEGER NOT NULL AUTO_INCREMENT,
            UM_ORG_ID VARCHAR(36) NOT NULL,
            UM_ROOT_ORG_ID VARCHAR(36) NOT NULL,
            UM_DISCOVERY_TYPE VARCHAR(255) NOT NULL,
            UM_DISCOVERY_VALUE VARCHAR(255) NOT NULL,
            PRIMARY KEY (UM_ID),
            UNIQUE (UM_ROOT_ORG_ID, UM_DISCOVERY_TYPE, UM_DISCOVERY_VALUE),
            FOREIGN KEY (UM_ROOT_ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE,
            FOREIGN KEY (UM_ORG_ID) REFERENCES UM_ORG(UM_ID) ON DELETE CASCADE
);