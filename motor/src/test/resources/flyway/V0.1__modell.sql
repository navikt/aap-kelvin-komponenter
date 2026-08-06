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
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    kjorbar boolean not null default false
);

CREATE INDEX IDX_JOBB_STATUS_SAK_BEHANDLING ON JOBB (STATUS, SAK_ID, BEHANDLING_ID, NESTE_KJORING);
CREATE INDEX IDX_JOBB_SAK ON JOBB (SAK_ID);
CREATE INDEX IDX_JOBB_BEHANDLING ON JOBB (BEHANDLING_ID);
CREATE INDEX IDX_JOBB_SAK_BEHANDLING ON JOBB (SAK_ID, BEHANDLING_ID);
CREATE INDEX IDX_JOBB_STATUS ON JOBB (STATUS);
CREATE INDEX IDX_JOBB_TYPE ON JOBB (TYPE);
CREATE INDEX IDX_JOBB_STATUS_NESTE_KJORING ON JOBB (STATUS, NESTE_KJORING);
CREATE INDEX IDX_JOBB_NESTE_KJORING ON JOBB (NESTE_KJORING);
CREATE INDEX IDX_JOBB_NESTE_KJORING_SAK_BEHANDLING ON JOBB (SAK_ID, BEHANDLING_ID, NESTE_KJORING);

-- Sikkerhetsnett: maks én blokkerende jobb per eksklusivitetsgruppe (sak_id, behandling_id, type).
-- «Blokkerende» speiler grupper_blokkert i skjedulerEkskluderendeJobber: status='FEILET' ELLER
-- (status='KLAR' AND kjorbar). Begge deler må være med - en FEILET-rad beholder kjorbar=true
-- (den røres aldri av markerSomFeilet), men mister STATUS='KLAR' og ville falt utenfor
-- indeksen om FEILET ikke var inkludert. Da ville ikke databasen lenger håndheve at kun
-- én rad kan eie eksklusivitets-slotten - sikkerhetsnettet ville hatt et hull nøyaktig i
-- det tilfellet det skal fange opp (en bug i grupper_blokkert-sjekken).
-- Bruker COALESCE til sentinelverdi (-1) fordi NULL != NULL i unike indekser i Postgres.
-- VIKTIG: må begrenses til rader som faktisk inngår i en eksklusivitetsgruppe, altså der
-- sak_id og/eller behandling_id er satt (samme betingelse som skjedulerEkskluderendeJobber).
-- Selvstendige jobber (sak_id OG behandling_id er NULL) er ikke en eksklusivitetsgruppe -
-- flere av samme type SKAL kunne være kjørbar=true samtidig. Uten denne begrensningen ville
-- COALESCE(sak_id,-1), COALESCE(behandling_id,-1) kollapse alle selvstendige jobber av samme
-- type til samme nøkkel (-1, -1, type), og indeksen ville feilaktig blokkere dem.
-- Denne indeksen skal også legges til av konsumenter av dette biblioteket i egne migrasjoner.
CREATE UNIQUE INDEX UX_JOBB_EKSKLUSIV_AKTIV ON JOBB (COALESCE(SAK_ID, -1), COALESCE(BEHANDLING_ID, -1), TYPE)
    WHERE (STATUS = 'FEILET' OR (STATUS = 'KLAR' AND KJORBAR))
      AND (SAK_ID IS NOT NULL OR BEHANDLING_ID IS NOT NULL);

CREATE TABLE JOBB_HISTORIKK
(
    ID            BIGSERIAL                              NOT NULL PRIMARY KEY,
    JOBB_ID       BIGINT                                 NOT NULL REFERENCES JOBB (ID),
    STATUS        VARCHAR(50)                            NOT NULL,
    FEILMELDING   TEXT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IDX_JOBB_HISTORIKK_ID_STATUS ON JOBB_HISTORIKK (JOBB_ID, STATUS);
CREATE INDEX IDX_JOBB_HISTORIKK_STATUS ON JOBB_HISTORIKK (STATUS);
CREATE INDEX IDX_JOBB_HISTORIKK_TID ON JOBB_HISTORIKK (OPPRETTET_TID);
CREATE INDEX IDX_JOBB_HISTORIKK_JOBB_ID ON JOBB_HISTORIKK (JOBB_ID);

CREATE TABLE JOBB_KOMMENTAR
(
    ID            BIGSERIAL    NOT NULL PRIMARY KEY,
    JOBB_ID       BIGINT       NOT NULL REFERENCES JOBB (ID),
    SKREVET_AV    TEXT         NOT NULL,
    TEKST         TEXT         NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) NOT NULL
);

CREATE INDEX IDX_JOBB_KOMMENTAR_JOBB_ID ON JOBB_KOMMENTAR (JOBB_ID);

-- TEST TABELLER

CREATE TABLE TEST_TABLE
(
    ID    BIGSERIAL NOT NULL PRIMARY KEY,
    VALUE TEXT      NOT NULL
);

CREATE TABLE ORDER_TABLE
(
    ID            BIGSERIAL NOT NULL PRIMARY KEY,
    VALUE         TEXT      NOT NULL,
    TRAD_NAVN     TEXT      NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
)