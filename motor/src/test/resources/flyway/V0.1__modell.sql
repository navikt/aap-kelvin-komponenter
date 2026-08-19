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
    kjorbar boolean not null default false,
    --   ALTER TABLE JOBB ADD COLUMN PRIORITET SMALLINT NOT NULL DEFAULT 100;
    PRIORITET     SMALLINT     DEFAULT 100               NOT NULL
);

CREATE INDEX IDX_JOBB_SAK ON JOBB (SAK_ID);
CREATE INDEX IDX_JOBB_BEHANDLING ON JOBB (BEHANDLING_ID);
CREATE INDEX IDX_JOBB_SAK_BEHANDLING ON JOBB (SAK_ID, BEHANDLING_ID);
CREATE INDEX IDX_JOBB_STATUS ON JOBB (STATUS);
CREATE INDEX IDX_JOBB_TYPE ON JOBB (TYPE);
CREATE INDEX IDX_JOBB_STATUS_NESTE_KJORING ON JOBB (STATUS, NESTE_KJORING);

-- ---------------------------------------------------------------------------
-- Indekser for motorens varme spørringer.
-- Alle er PARTIELLE og speiler WHERE-betingelsen i spørringen de betjener, slik at
-- indeksen bare inneholder rader som faktisk er i spill. FERDIG-jobber utgjør over tid
-- de aller fleste radene i tabellen, og de er ikke med i noen av disse indeksene.
--
-- Konsumenter bør opprette disse med CREATE INDEX CONCURRENTLY i en egen migrasjon
-- markert med `-- flyway:noTransaction`, ellers låses JOBB-tabellen under bygging.
-- ---------------------------------------------------------------------------

-- 1) plukkJobbV2: `status = 'KLAR' and kjorbar and neste_kjoring <= now()`
--    sortert på `prioritet, neste_kjoring`.
--    Kolonnerekkefølgen er den samme som i ORDER BY, og begge er stigende. Det lar
--    Postgres lese radene ferdig sortert rett ut av indeksen - ingen sorteringssteg,
--    og `limit 1` stopper etter første treff. `neste_kjoring <= ?` blir et filter
--    innenfor hver prioritetsgruppe, ikke et eget oppslag.
CREATE INDEX IDX_JOBB_PLUKK ON JOBB (PRIORITET, NESTE_KJORING)
    WHERE STATUS = 'KLAR' AND KJORBAR;

-- 2) skjedulerSelvstendigeJobber: jobber uten eksklusivitetsgruppe som skal forfremmes.
--    Prioritet er bevisst IKKE med her - skjeduleringen forfremmer alle kvalifiserte
--    rader i én UPDATE, så rekkefølgen er irrelevant. Prioritet avgjøres først ved plukk.
CREATE INDEX IDX_JOBB_SKJEDULER_SELVSTENDIG ON JOBB (NESTE_KJORING)
    WHERE STATUS = 'KLAR' AND NOT KJORBAR AND SAK_ID IS NULL AND BEHANDLING_ID IS NULL;

-- 3) skjedulerEkskluderendeJobber, CTE `neste_ekskluderende_jobb`:
--    `distinct on (sak_id, behandling_id, type) ... order by sak_id, behandling_id, type, neste_kjoring`.
--    Kolonnerekkefølgen matcher DISTINCT ON/ORDER BY eksakt, slik at Postgres kan hoppe
--    rett til første rad per gruppe uten å sortere.
--    MERK: `neste_kjoring` er siste kolonne, ikke `prioritet`. Innad i en eksklusivitets-
--    gruppe er rekkefølgegarantien absolutt - eldste jobb først, uansett prioritet.
CREATE INDEX IDX_JOBB_SKJEDULER_EKSKLUDERENDE ON JOBB (SAK_ID, BEHANDLING_ID, TYPE, NESTE_KJORING)
    WHERE STATUS = 'KLAR' AND NOT KJORBAR
      AND (SAK_ID IS NOT NULL OR BEHANDLING_ID IS NOT NULL);

-- 4) skjedulerEkskluderendeJobber, CTE `grupper_blokkert`:
--    `where status = 'FEILET' or (status = 'KLAR' and kjorbar)`.
--    Betingelsen er identisk med UX_JOBB_EKSKLUSIV_AKTIV under, men den indeksen er
--    bygget på COALESCE-uttrykk og begrenset til rader i en eksklusivitetsgruppe, og kan
--    derfor ikke brukes til dette oppslaget. Denne dekker NOT EXISTS-sjekken direkte.
CREATE INDEX IDX_JOBB_GRUPPER_BLOKKERT ON JOBB (SAK_ID, BEHANDLING_ID, TYPE)
    WHERE STATUS = 'FEILET' OR (STATUS = 'KLAR' AND KJORBAR);

-- 5) plukkJobb (V1, fortsatt i bruk bak feature toggle), CTE `ekskluderende_jobb`:
--    samme DISTINCT ON som (3), men over `status IN ('FEILET','KLAR')` uten `kjorbar`-filter.
--    Kan ikke betjenes av IDX_JOBB_SKJEDULER_EKSKLUDERENDE, som krever `NOT KJORBAR`.
--    Denne kan droppes når V2-toggelen er permanent på og V1 er fjernet.
CREATE INDEX IDX_JOBB_STATUS_SAK_BEHANDLING ON JOBB (STATUS, SAK_ID, BEHANDLING_ID, NESTE_KJORING);

-- Følgende indekser fra tidligere versjoner er fjernet fordi de nye partielle indeksene
-- dekker de samme spørringene bedre, og hver ekstra indeks koster på hver INSERT/UPDATE
-- av en jobb (motoren skriver til JOBB flere ganger per jobb: leggTil, kjorbar,
-- neste_kjoring ved backoff, status ved ferdigstilling):
--   IDX_JOBB_NESTE_KJORING (NESTE_KJORING)
--       -> erstattet av IDX_JOBB_PLUKK og IDX_JOBB_SKJEDULER_SELVSTENDIG
--   IDX_JOBB_NESTE_KJORING_SAK_BEHANDLING (SAK_ID, BEHANDLING_ID, NESTE_KJORING)
--       -> prefiks av IDX_JOBB_SKJEDULER_EKSKLUDERENDE
-- Konsumenter bør verifisere med pg_stat_user_indexes (idx_scan = 0 over tid) før de
-- dropper dem i produksjon - egne spørringer mot JOBB kan avhenge av dem.

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