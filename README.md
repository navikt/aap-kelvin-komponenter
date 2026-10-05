# AAP Kelvin Komponenter

Felles-bibliotek for apper for AAP.

Dokumentasjon: https://navikt.github.io/aap-kelvin-komponenter/

# Komme i gang

For oppdatert oppskrift for å kjøre koden, se stegene i Github Actions.

## Unleash ApiToken

Hvis du må rullere token for Unleash, er du nødt til å slette det gamle først.

**OBS:** Husk å sette riktig miljø _før_ du kjører kommandoene under. `dev-gcp` for `apply *-dev.yaml` filen, osv.

Slette gammelt token:

```shell
kubectl delete apitoken kelvin-unleash-api-token -n aap
```

Opprette nytt token:

```shell
kubectl apply -f unleash-apitoken-dev.yaml
```

Se dokumentasjon her: \
https://doc.nais.io/services/feature-toggling/?h=unleash#creating-a-new-api-token

## Delt versjonskatalog for konsumerende repoer

I tillegg til bibliotekene publiseres et Gradle [version catalog](https://docs.gradle.org/current/userguide/platforms.html)
som `no.nav.aap.kelvin:version-catalog:<versjon>`. Katalogen genereres direkte fra
[`gradle/libs.versions.toml`](gradle/libs.versions.toml) i dette repoet, og inneholder felles
tredjepartsversjoner (ktor, jackson, logback, testcontainers, junit, micrometer, caffeine, m.m.)
som ellers dupliseres i repoer som `aap-behandlingsflyt`, `aap-oppgave` og `aap-tilgang`.

Konsumerende repoer kan ta den i bruk i `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("kelvinLibs") {
            from("no.nav.aap.kelvin:version-catalog:2.0.155")
        }
    }
}
```

og deretter referere til biblioteker som `kelvinLibs.ktor.server.netty`,
`kelvinLibs.logback.classic` osv., i tillegg til sin egen `libs.versions.toml` for
repo-spesifikke avhengigheter (kontrakter, `komponenter`-versjonen selv, osv.).

Et bibliotek i katalogen er kun tilgjengelig, ikke automatisk inkludert – hvert konsumerende
modul må selv skrive f.eks. `implementation(kelvinLibs.caffeine)` for å ta det i bruk. Katalogen
tvinger dermed ikke fram noen avhengigheter; den gjør det bare enklere å holde versjonene like på
tvers av repoene.

## Navngitte SQL-parametre i dbconnect

SQL kan bruke `:navn` i stedet for posisjonelle `?`-parametre. Bruk de samme typede
setterne i `setParams`, men send parameternavnet i stedet for indeksen:

```kotlin
dataSource.transaction(readOnly = true) { connection ->
    connection.queryList("SELECT id FROM test WHERE id = ANY(:ids::bigint[])") {
        setParams {
            setLongArray("ids", listOf(1L, 2L))
        }
        setRowMapper { row -> row.getLong("id") }
    }
}
```

Dette fungerer for alle query- og execute-metoder, inkludert genererte nøkler og
`executeBatch`. Rekkefølgen på setterne er valgfri, og et navn som forekommer flere
ganger i SQL, bindes på alle plasseringene. Navn er case-sensitive og følger
`[A-Za-z_][A-Za-z0-9_]*`. Eksisterende indeksbaserte settere fungerer som før.

Navngitte og posisjonelle plassholdere kan ikke blandes i samme SQL-statement.
Ukjente eller manglende navn gir en feil; i batch må hvert element binde alle navn,
også når verdien er `null`. Verdiene bindes med JDBC, ikke ved tekstinterpolering.
Lister ekspanderes ikke automatisk i `IN`; bruk array-setterne sammen med `ANY`.

PostgreSQL-casts (`::`), strenger, dollar-quoting, identifikatorer og kommentarer
bevares. Parsing bruker PostgreSQL-standardinnstillingen `standard_conforming_strings = on`;
bruk `E'...'` for strenger med backslash-escapes. JSON-operatorer som inneholder `?`,
må escapes som `??`, `??|` eller `??&`, som ellers ved bruk av pgJDBC.
I array-subscripts bevares `:` som slice-syntaks; bruk parenteser rundt navngitte
parametre, for eksempel `values[(:index)]` eller `values[(:low):(:high)]`.

## Bygge dokumentasjon

```
./gradlew dokkaGenerate
```

Åpne `index.html` i `build/dokka/html`.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#po-aap-team-aap`.
