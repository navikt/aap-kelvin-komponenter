# Module infrastructure

Felles infrastruktur-oppsett, blant annet programmatisk Logback-konfigurasjon som
erstatter `logback.xml`/`logback-test.xml` i konsumerende prosjekter.

## Logback-oppsett

`LogbackOppsett` konfigurerer Logback programmatisk (ingen XML-filer trengs), og
registreres automatisk via Logbacks `Configurator`-SPI (`ServiceLoader`) — det er
altså nok å legge til riktig avhengighet, uten noen eksplisitt kode.

### Produksjonsoppsett

Legg til vanlig avhengighet på `infrastructure`:

```kotlin
dependencies {
    implementation("no.nav.aap.kelvin:infrastructure:<versjon>")
}
```

`LogbackProduksjonConfigurator` blir da automatisk oppdaget og konfigurerer JSON-logging
til konsoll, pluss en egen `team-logs`-appender for sikker/logging som ikke skal havne i
vanlige logger. Er `papertrail-logback-syslog4j` på classpath (og miljøvariabler for Papertrail
satt), aktiveres i tillegg audit-logging.

Ingen `logback.xml` trengs.

### Testoppsett

Bruk Gradles `java-test-fixtures`-mekanisme for å få automatisk testoppsett
(f.eks. lesbart, fargelagt konsoll-format i stedet for JSON) i testkjøringer:

```kotlin
dependencies {
    testImplementation(testFixtures("no.nav.aap.kelvin:infrastructure:<versjon>"))
}
```

`LogbackTestConfigurator` blir da automatisk oppdaget i testscope og gir et
menneskelesbart, ANSI-fargelagt konsollformat, samt at `team-logs`-loggeren rutes
til konsollen på TRACE-nivå.

Fjern eventuelle gamle `src/test/resources/logback.xml`/`logback-test.xml` —
disse vil ellers bli plukket opp av Logbacks vanlige XML-oppdagelse og overstyre
`Configurator`-SPI-et.

#### Tilpasset testoppsett

Trenger prosjektet ditt et annet mønster, egne logger-nivåer, eller audit-logger
også i test, kan du registrere din egen `Configurator` som kaller
`LogbackOppsett.konfigurerTest(...)` med tilpassede parametere (`pattern`,
`loggerNivåer`, `inkluderAuditLogger`), og gi den en høyere
`@ConfiguratorRank` enn `LogbackTestConfigurator` (`CUSTOM_HIGH_PRIORITY`) —
for eksempel `CUSTOM_TOP_PRIORITY` — registrert via
`META-INF/services/ch.qos.logback.classic.spi.Configurator`.

## Key packages

|Package                     |Description                                    |
|----------------------------|-----------------------------------------------|
|`no.nav.aap.komponenter.log`|Programmatisk Logback-oppsett og Configuratorer|
