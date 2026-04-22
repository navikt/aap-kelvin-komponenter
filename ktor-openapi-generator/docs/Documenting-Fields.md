# Documenting Fields

All annotations below apply to constructor parameters of data classes and are
reflected in the generated OpenAPI schema. Constraint annotations also enforce
validation at runtime — Ktor will reject requests that violate them.

## `@Description` — field description

Use `@property:Description` on constructor parameters to document individual fields
in the generated schema:

```kotlin
@Response("A single item")
data class Item(
    @property:Description("Unique identifier of the item") val id: String,
    @property:Description("Human-readable display name") val name: String,
)
```

> **Important:** use `@property:Description`, not `@param:Description` or a bare
> `@Description`. The schema generator reads annotations from the Kotlin property
> (`KProperty`), so only the `property` use-site target is picked up.

## `@StringExample` — example values for `String` fields

Provide a representative value for a `String` property. A single example is set as
`example`; when two or more are given they are emitted as `examples`:

```kotlin
data class Item(
    @StringExample("abc-123") val id: String,          // → "example": "abc-123"
    @StringExample("Foo", "Bar") val name: String,     // → "examples": ["Foo", "Bar"]
)
```

## Numeric constraints

These annotations set `minimum`/`maximum` in the schema **and** validate the value
at runtime.

### Integer (`Int`, `Long`)

```kotlin
data class PageRequest(
    @Min(1) val page: Int,
    @Max(100) val pageSize: Int,
    @Clamp(min = 0, max = 999) val offset: Long,
)
```

| Annotation         | Parameters                        |
| ------------------ | --------------------------------- |
| `@Min(value)`      | Inclusive lower bound             |
| `@Max(value)`      | Inclusive upper bound             |
| `@Clamp(min, max)` | Shorthand for both bounds at once |

### Floating-point (`Float`, `Double`)

```kotlin
data class Measurement(
    @FMin(0.0) val temperature: Double,
    @FMax(100.0) val humidity: Double,
    @FClamp(min = -1.0, max = 1.0) val normalised: Double,
)
```

| Annotation          | Parameters                        |
| ------------------- | --------------------------------- |
| `@FMin(value)`      | Inclusive lower bound             |
| `@FMax(value)`      | Inclusive upper bound             |
| `@FClamp(min, max)` | Shorthand for both bounds at once |

## String constraints

These annotations set `minLength`, `maxLength`, and `pattern` in the schema and
validate at runtime.

```kotlin
data class CreateUserRequest(
    @MinLength(2) @MaxLength(50) val name: String,
    @Length(min = 8, max = 72) val password: String,
    @RegularExpression("""^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}$""") val email: String,
)
```

| Annotation                    | Parameters                               |
| ----------------------------- | ---------------------------------------- |
| `@MinLength(value)`           | Minimum character count                  |
| `@MaxLength(value)`           | Maximum character count                  |
| `@Length(min, max)`           | Shorthand for both bounds at once        |
| `@RegularExpression(pattern)` | Regex pattern (sets `pattern` in schema) |

## Combining annotations

All of the above can be combined freely on the same property:

```kotlin
data class CreateUserRequest(
    @Description("The user's display name")
    @MinLength(2) @MaxLength(50)
    @StringExample("Alice", "Bob")
    val name: String,

    @Description("Age in years")
    @Min(0) @Max(150)
    val age: Int,
)
```
