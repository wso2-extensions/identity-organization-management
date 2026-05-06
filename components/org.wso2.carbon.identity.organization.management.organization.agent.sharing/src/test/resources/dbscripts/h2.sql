-- ---------------------------------------------------------------------------
-- Table: UM_ORG
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_ORG (
    UM_ID              VARCHAR(36)                             NOT NULL,
    UM_ORG_NAME        VARCHAR(255)                            NOT NULL,
    UM_ORG_DESCRIPTION VARCHAR(1024),
    UM_CREATED_TIME    TIMESTAMP                               NOT NULL,
    UM_LAST_MODIFIED   TIMESTAMP                               NOT NULL,
    UM_STATUS          VARCHAR(255)    DEFAULT 'ACTIVE'        NOT NULL,
    UM_PARENT_ID       VARCHAR(36),
    UM_ORG_TYPE        VARCHAR(100)                            NOT NULL,
    PRIMARY KEY (UM_ID),
    FOREIGN KEY (UM_PARENT_ID) REFERENCES UM_ORG (UM_ID) ON DELETE CASCADE ON UPDATE RESTRICT
);

-- ---------------------------------------------------------------------------
-- Table: UM_DOMAIN
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_DOMAIN (
    UM_DOMAIN_ID  INTEGER     NOT NULL AUTO_INCREMENT,
    UM_TENANT_ID  INTEGER     DEFAULT 0,
    UM_DOMAIN_NAME VARCHAR(255),
    PRIMARY KEY (UM_DOMAIN_ID)
);

-- ---------------------------------------------------------------------------
-- Table: UM_HYBRID_ROLE
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_HYBRID_ROLE (
    UM_ID         INTEGER     NOT NULL AUTO_INCREMENT,
    UM_ROLE_NAME  VARCHAR(255) NOT NULL,
    UM_TENANT_ID  INTEGER     DEFAULT 0,
    UM_UUID       VARCHAR(36),
    PRIMARY KEY (UM_ID)
);

-- ---------------------------------------------------------------------------
-- Table: UM_HYBRID_USER_ROLE
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_HYBRID_USER_ROLE (
    UM_ID         INTEGER     NOT NULL AUTO_INCREMENT,
    UM_USER_NAME  VARCHAR(255),
    UM_ROLE_ID    INTEGER,
    UM_TENANT_ID  INTEGER     DEFAULT 0,
    UM_DOMAIN_ID  INTEGER,
    PRIMARY KEY (UM_ID),
    FOREIGN KEY (UM_DOMAIN_ID) REFERENCES UM_DOMAIN (UM_DOMAIN_ID) ON DELETE CASCADE ON UPDATE RESTRICT,
    FOREIGN KEY (UM_ROLE_ID) REFERENCES UM_HYBRID_ROLE (UM_ID) ON DELETE CASCADE ON UPDATE RESTRICT
);

-- ---------------------------------------------------------------------------
-- Table: UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS
-- Note: This table is introduced by the user sharing v2 component. It is
--       included here to support agent sharing test scenarios that involve
--       role-level edit restriction queries.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS (
    UM_ID                          INTEGER     NOT NULL AUTO_INCREMENT,
    UM_HYBRID_USER_ROLE_ID         INTEGER     NOT NULL,
    UM_HYBRID_USER_ROLE_TENANT_ID  INTEGER     NOT NULL,
    UM_EDIT_OPERATION              VARCHAR(50) NOT NULL,
    UM_PERMITTED_ORG_ID            VARCHAR(36) NOT NULL,
    PRIMARY KEY (UM_ID),
    FOREIGN KEY (UM_HYBRID_USER_ROLE_ID) REFERENCES UM_HYBRID_USER_ROLE (UM_ID) ON DELETE CASCADE ON UPDATE RESTRICT
);

-- ---------------------------------------------------------------------------
-- Table: UM_ORG_AGENT_ASSOCIATION
-- Stores the association between a shared agent and the organizations it is
-- shared into. UM_ASSOCIATED_AGENT_ID / UM_ASSOCIATED_ORG_ID identify the
-- original (root) agent, while UM_AGENT_ID / UM_ORG_ID identify the
-- corresponding shared agent entry in the target organization.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_ORG_AGENT_ASSOCIATION (
    UM_ID                  INTEGER      NOT NULL AUTO_INCREMENT,
    UM_AGENT_ID            VARCHAR(255) NOT NULL,
    UM_ORG_ID              VARCHAR(36)  NOT NULL,
    UM_ASSOCIATED_AGENT_ID VARCHAR(255) NOT NULL,
    UM_ASSOCIATED_ORG_ID   VARCHAR(36)  NOT NULL,
    UM_SHARED_TYPE         VARCHAR(100),
    PRIMARY KEY (UM_ID),
    UNIQUE (UM_ASSOCIATED_AGENT_ID, UM_ORG_ID),
    FOREIGN KEY (UM_ORG_ID)            REFERENCES UM_ORG (UM_ID) ON DELETE CASCADE ON UPDATE RESTRICT,
    FOREIGN KEY (UM_ASSOCIATED_ORG_ID) REFERENCES UM_ORG (UM_ID) ON DELETE CASCADE ON UPDATE RESTRICT
);

CREATE INDEX IDX_UM_OAA_AGENT_ID           ON UM_ORG_AGENT_ASSOCIATION (UM_AGENT_ID);
CREATE INDEX IDX_UM_OAA_ORG_ID             ON UM_ORG_AGENT_ASSOCIATION (UM_ORG_ID);
CREATE INDEX IDX_UM_OAA_ASSOC_AGENT_ID     ON UM_ORG_AGENT_ASSOCIATION (UM_ASSOCIATED_AGENT_ID);
CREATE INDEX IDX_UM_OAA_ASSOC_ORG_ID       ON UM_ORG_AGENT_ASSOCIATION (UM_ASSOCIATED_ORG_ID);

-- ---------------------------------------------------------------------------
-- Table: UM_ORG_USER_ASSOCIATION
-- Mirrors the user-sharing UM_ORG_USER_ASSOCIATION table. Stores the
-- association between a shared user and the organizations it is shared into.
-- UM_ASSOCIATED_USER_ID / UM_ASSOCIATED_ORG_ID identify the original (root)
-- user, while UM_USER_ID / UM_ORG_ID identify the shared user entry in the
-- target organization.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UM_ORG_USER_ASSOCIATION (
    UM_ID                  INTEGER      NOT NULL AUTO_INCREMENT,
    UM_USER_ID             VARCHAR(255) NOT NULL,
    UM_ORG_ID              VARCHAR(36)  NOT NULL,
    UM_ASSOCIATED_USER_ID  VARCHAR(255) NOT NULL,
    UM_ASSOCIATED_ORG_ID   VARCHAR(36)  NOT NULL,
    UM_SHARED_TYPE         VARCHAR(50),
    PRIMARY KEY (UM_ID),
    UNIQUE (UM_ASSOCIATED_USER_ID, UM_ORG_ID)
);

CREATE INDEX IDX_UM_OUA_USER_ID            ON UM_ORG_USER_ASSOCIATION (UM_USER_ID);
CREATE INDEX IDX_UM_OUA_ORG_ID             ON UM_ORG_USER_ASSOCIATION (UM_ORG_ID);
CREATE INDEX IDX_UM_OUA_ASSOC_USER_ID      ON UM_ORG_USER_ASSOCIATION (UM_ASSOCIATED_USER_ID);
CREATE INDEX IDX_UM_OUA_ASSOC_ORG_ID       ON UM_ORG_USER_ASSOCIATION (UM_ASSOCIATED_ORG_ID);
