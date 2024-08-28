CREATE TABLE JOBB
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    STATUS        VARCHAR(50)  DEFAULT 'KLAR'            NOT NULL,
    TYPE          VARCHAR(50)                            NOT NULL,
    SAK_ID        BIGINT NULL,
    BEHANDLING_ID BIGINT NULL,
    parameters    text NULL,
    payload       text NULL,
    NESTE_KJORING TIMESTAMP(3)                           NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IDX_JOBB_STATUS ON JOBB (STATUS, SAK_ID, BEHANDLING_ID, NESTE_KJORING);
-- Legger inn en unik index per type for en sak+behandling for åpne oppgaver
CREATE UNIQUE INDEX UIDX_JOBB_STATUS_SAK_BEHANDLING_TYPE ON JOBB (TYPE, SAK_ID, BEHANDLING_ID) WHERE (
    STATUS IN ('KLAR', 'FEILET') AND sak_id is not null AND behandling_id is not null);

CREATE TABLE JOBB_HISTORIKK
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    JOBB_ID       BIGINT                                 NOT NULL REFERENCES JOBB (ID),
    STATUS        VARCHAR(50)                            NOT NULL,
    FEILMELDING   TEXT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IDX_JOBB_HISTORIKK_STATUS ON JOBB_HISTORIKK (JOBB_ID, STATUS);