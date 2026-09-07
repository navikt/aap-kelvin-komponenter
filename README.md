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

## Bygge dokumentasjon

```
./gradlew dokkaGenerate
```

Åpne `index.html` i `build/dokka/html`.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#po-aap-team-aap`.
