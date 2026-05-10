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
    UM_SHARED_TYPE         VARCHAR(255) NOT NULL DEFAULT 'NOT SPECIFIED',
    PRIMARY KEY (UM_USER_ID, UM_ORG_ID, UM_ASSOCIATED_USER_ID, UM_ASSOCIATED_ORG_ID),
    UNIQUE (UM_ID)
);

CREATE INDEX IDX_UM_OUA_USER_ID            ON UM_ORG_USER_ASSOCIATION (UM_USER_ID);
CREATE INDEX IDX_UM_OUA_ORG_ID             ON UM_ORG_USER_ASSOCIATION (UM_ORG_ID);
CREATE INDEX IDX_UM_OUA_ASSOC_USER_ID      ON UM_ORG_USER_ASSOCIATION (UM_ASSOCIATED_USER_ID);
CREATE INDEX IDX_UM_OUA_ASSOC_ORG_ID       ON UM_ORG_USER_ASSOCIATION (UM_ASSOCIATED_ORG_ID);
