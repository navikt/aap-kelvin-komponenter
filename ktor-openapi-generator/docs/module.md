# Module ktor-openapi-generator

A Ktor plugin that automatically generates an OpenAPI 3 specification from your
route definitions.

## Documentation

- [Setup](https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/docs/Setup.md) — Installation and initial configuration
- [Basic Routing](https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/docs/Basic-Routing.md) — Defining routes with parameters and response types
- [Documenting Response Objects](https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/docs/Documenting-Response-Objects.md) — `@Response`, examples, and other response-level annotations
- [Documenting Fields](https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/docs/Documenting-Fields.md) — `@property:Description`, examples, numeric and string constraints
- [Extension system](https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/docs/Modules.md) — The module system: `info()`, `status()`, `responseDescription()`
- [Response Description](https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/docs/Response-Description.md) — Setting response descriptions, including for `List<T>`

## Key packages

| Package                                   | Description                                         |
| ----------------------------------------- | --------------------------------------------------- |
| `com.papsign.ktor.openapigen`             | Plugin installation and top-level API               |
| `com.papsign.ktor.openapigen.route`       | Typed routing DSL                                   |
| `com.papsign.ktor.openapigen.annotations` | Annotations for documenting routes and data classes |
| `com.papsign.ktor.openapigen.model`       | OpenAPI model classes                               |
