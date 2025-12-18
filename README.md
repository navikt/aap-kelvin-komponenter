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

## Bygge dokumentasjon

```
./gradlew dokkaGenerate
```

Åpne `index.html` i `build/dokka/html`.

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen `#po-aap-team-aap`.
