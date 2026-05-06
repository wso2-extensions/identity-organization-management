-- ---------------------------------------------------------------------------
-- Table: UM_ORG_AGENT_ASSOCIATION
--
-- Stores the association between a shared agent and the organizations it is
-- shared into. UM_ASSOCIATED_AGENT_ID / UM_ASSOCIATED_ORG_ID identify the
-- original (root) agent, while UM_AGENT_ID / UM_ORG_ID identify the
-- corresponding shared agent entry in the target organization.
--
-- Prerequisites: UM_ORG table must exist (defined in the core organization
-- management DB scripts).
--
-- UM_HYBRID_USER_ROLE_RESTRICTED_EDIT_PERMISSIONS must exist (defined in the
-- user sharing v2 DB scripts).
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS UM_ORG_AGENT_ASSOCIATION (
    UM_ID                  INT          NOT NULL AUTO_INCREMENT,
    UM_AGENT_ID            VARCHAR(255) NOT NULL,
    UM_ORG_ID              VARCHAR(36)  NOT NULL,
    UM_ASSOCIATED_AGENT_ID VARCHAR(255) NOT NULL,
    UM_ASSOCIATED_ORG_ID   VARCHAR(36)  NOT NULL,
    UM_SHARED_TYPE         VARCHAR(100),
    PRIMARY KEY (UM_ID),
    UNIQUE KEY UK_UM_OAA_ASSOC_AGENT_ORG (UM_ASSOCIATED_AGENT_ID, UM_ORG_ID),
    CONSTRAINT FK_UM_OAA_ORG_ID            FOREIGN KEY (UM_ORG_ID)            REFERENCES UM_ORG (UM_ID) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT FK_UM_OAA_ASSOC_ORG_ID      FOREIGN KEY (UM_ASSOCIATED_ORG_ID) REFERENCES UM_ORG (UM_ID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IDX_UM_OAA_AGENT_ID       ON UM_ORG_AGENT_ASSOCIATION (UM_AGENT_ID);
CREATE INDEX IDX_UM_OAA_ORG_ID         ON UM_ORG_AGENT_ASSOCIATION (UM_ORG_ID);
CREATE INDEX IDX_UM_OAA_ASSOC_AGENT_ID ON UM_ORG_AGENT_ASSOCIATION (UM_ASSOCIATED_AGENT_ID);
CREATE INDEX IDX_UM_OAA_ASSOC_ORG_ID   ON UM_ORG_AGENT_ASSOCIATION (UM_ASSOCIATED_ORG_ID);
