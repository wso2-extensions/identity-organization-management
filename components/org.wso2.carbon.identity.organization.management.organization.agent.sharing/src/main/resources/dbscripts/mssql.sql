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
--
-- Note: MS SQL Server disallows multiple FOREIGN KEY ... ON DELETE CASCADE
-- paths to the same table (multiple cascade paths error). Therefore
-- FK_UM_OAA_ASSOC_ORG_ID uses ON DELETE NO ACTION. Cascaded deletes for
-- UM_ASSOCIATED_ORG_ID must be handled at the application layer or via a
-- trigger when an UM_ORG row is removed.
-- ---------------------------------------------------------------------------

IF NOT EXISTS (
    SELECT 1 FROM sys.objects
    WHERE object_id = OBJECT_ID(N'[dbo].[UM_ORG_AGENT_ASSOCIATION]') AND type = N'U'
)
BEGIN
    CREATE TABLE UM_ORG_AGENT_ASSOCIATION (
        UM_ID                  INT            NOT NULL IDENTITY(1,1),
        UM_AGENT_ID            NVARCHAR(255)  NOT NULL,
        UM_ORG_ID              NVARCHAR(36)   NOT NULL,
        UM_ASSOCIATED_AGENT_ID NVARCHAR(255)  NOT NULL,
        UM_ASSOCIATED_ORG_ID   NVARCHAR(36)   NOT NULL,
        UM_SHARED_TYPE         NVARCHAR(100),
        PRIMARY KEY (UM_ID),
        CONSTRAINT UK_UM_OAA_ASSOC_AGENT_ORG UNIQUE (UM_ASSOCIATED_AGENT_ID, UM_ORG_ID),
        CONSTRAINT FK_UM_OAA_ORG_ID       FOREIGN KEY (UM_ORG_ID)            REFERENCES UM_ORG (UM_ID) ON DELETE CASCADE,
        CONSTRAINT FK_UM_OAA_ASSOC_ORG_ID FOREIGN KEY (UM_ASSOCIATED_ORG_ID) REFERENCES UM_ORG (UM_ID) ON DELETE NO ACTION
    );

    CREATE INDEX IDX_UM_OAA_AGENT_ID       ON UM_ORG_AGENT_ASSOCIATION (UM_AGENT_ID);
    CREATE INDEX IDX_UM_OAA_ORG_ID         ON UM_ORG_AGENT_ASSOCIATION (UM_ORG_ID);
    CREATE INDEX IDX_UM_OAA_ASSOC_AGENT_ID ON UM_ORG_AGENT_ASSOCIATION (UM_ASSOCIATED_AGENT_ID);
    CREATE INDEX IDX_UM_OAA_ASSOC_ORG_ID   ON UM_ORG_AGENT_ASSOCIATION (UM_ASSOCIATED_ORG_ID);
END;
