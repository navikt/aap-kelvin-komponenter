# AAP Kelvin Komponenter

Felles-bibliotek for apper for AAP.

Dokumentasjon: https://navikt.github.io/aap-kelvin-komponenter/

## Shared dependency artifacts

Repoet publiserer også felles dependency-management artifacts:

- `no.nav.aap.kelvin:kelvin-bom` (Maven BOM / `java-platform`)
- `no.nav.aap.kelvin:kelvin-catalog` (Gradle version catalog)

Dette gjør at AAP-apper kan hente standardiserte versjoner og aliases uten å duplisere store `libs.versions.toml`-filer i hvert repo.

### Konsumere i en AAP-app

Legg til delt catalog i `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("kelvinLibs") {
            from("no.nav.aap.kelvin:kelvin-catalog:<versjon>")
        }
    }
}
```

Og legg til BOM i `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform(kelvinLibs.kelvin.bom))
    implementation(kelvinLibs.bundles.kelvin.core)
    implementation(kelvinLibs.bundles.core.runtime)
    testImplementation(kelvinLibs.bundles.core.test)

    // Optional domain bundles
    implementation(kelvinLibs.bundles.domain.tilgang)
}
```

Policy: bruk `core`-bundles som default, og velg `domain-*` eksplisitt per app for å unngå å dra inn alle kontrakter.

### Pilot-migrering (første repos)

1. `aap-utbetal`
   - Behold lokale domene-spesifikke aliases (`kafkaClients`, `nimbusJoseJwt`, etc.).
   - Fjern dupliserte Kelvin/Ktor/Jackson/logging/test aliases fra lokal `gradle/libs.versions.toml`.
   - Legg til:
     - `implementation(platform(kelvinLibs.kelvin.bom))`
     - `implementation(kelvinLibs.bundles.kelvin.core)`
     - `implementation(kelvinLibs.bundles.core.runtime)`
     - `testImplementation(kelvinLibs.bundles.core.test)`
     - `implementation(kelvinLibs.bundles.domain.tilgang)`
     - `implementation(kelvinLibs.bundles.domain.behandlingsflyt)`

2. `aap-postmottak-backend`
   - Samme baseoppskrift som over.
   - Behold lokale Kafka/Avro/Systemtest aliases.
   - Start med å bytte ut `httpklient/infrastructure/dbconnect/dbmigrering/motor/motorApi/server` til `kelvinLibs.bundles.kelvin.core` + eksplisitte ekstra Kelvin-moduler ved behov.

3. `aap-tilgang`
   - Flytt fra lokal `ktor` + `komponenter` versjonsstyring til `kelvin-catalog`.
   - Bytt lokale `ktorServer*`, `ktorClient*`, logging/Jackson/Kelvin aliases til `core-runtime` + `kelvin-core`.
   - Behold repo-spesifikke aliases (`lettuce`, `mockOauth2Server`, `testcontainersRedis`, `postmottakKontrakt`) lokalt.

### Drift-check under utrulling

For å finne alias-overlapp mellom consumer og `kelvin-catalog`:

```bash
./kelvin-catalog/scripts/check-consumer-drift.sh /path/to/consumer/gradle/libs.versions.toml
```

Tasken feiler (`exit 1`) ved overlapp, slik at repos kan bruke den i CI under migrering.

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

## Bygge dokumentasjon

```
./gradlew dokkaGenerate
```

Åpne `index.html` i `build/dokka/html`.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#po-aap-team-aap`.
