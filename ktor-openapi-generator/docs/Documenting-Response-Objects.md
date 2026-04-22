# Documenting Response Objects

There are several layers at which you can add OpenAPI documentation to a response
object — from the class itself, down to individual properties.

## `@Response` — class-level description and status code

Annotate the response class with `@Response` to set the human-readable description
and (optionally) the default HTTP status code for that type:

```kotlin
@Response("A single item", statusCode = 200)
data class Item(val id: String, val name: String)

get<Unit, Item> {
    respond(Item("1", "Foo"))
}
```

The `statusCode` defaults to `200` and can be omitted in the common case.

## `@Description` — property-level descriptions

Use `@property:Description` on constructor parameters to document individual fields
in the generated schema:

```kotlin
@Response("A single item")
data class Item(
    @property:Description("Unique identifier of the item") val id: String,
    @property:Description("Human-readable display name") val name: String,
)
```

> **Note:** use `@property:Description`, not `@param:Description`. See
> [Documenting Fields](Documenting-Fields.md) for details.

For the full set of field-level annotations — descriptions, examples, and
constraints — see [Documenting Fields](Documenting-Fields.md).

## Inline examples

### Via route parameter

Pass an instance directly to the route method as `example` (GET/DELETE) or
`exampleResponse` (POST/PUT/PATCH):

```kotlin
get<Unit, Item>(
    example = Item("1", "Foo")
) {
    respond(Item("1", "Foo"))
}

post<Unit, Item, CreateItemRequest>(
    exampleResponse = Item("1", "Foo"),
    exampleRequest = CreateItemRequest("Foo"),
) { _, body ->
    respond(Item("1", body.name))
}
```

### Via `@WithExample` on the class

For a reusable example that follows the class everywhere it is used, implement
`ExampleProvider` in a companion object and annotate the class with `@WithExample`:

```kotlin
@Response("A single item")
@WithExample
data class Item(val id: String, val name: String) {
    companion object : ExampleProvider<Item> {
        override val example = Item("1", "Foo")
    }
}
```

Multiple examples are also supported via `examples`:

```kotlin
@WithExample
data class Item(val id: String, val name: String) {
    companion object : ExampleProvider<Item> {
        override val examples = listOf(
            Item("1", "Foo"),
            Item("2", "Bar"),
        )
    }
}
```

### `@StringExample` — examples on `String` properties

For individual `String` fields, use `@StringExample` to provide one or more example
values:

```kotlin
@Response("A single item")
data class Item(
    @StringExample("abc-123", "xyz-789") val id: String,
    val name: String,
)
```

## Response description per route

When the top-level response type is a generic wrapper such as `List<T>`, the
`@Response` annotation on `T` is not picked up automatically. Use the
`responseDescription()` module instead:

```kotlin
get<Unit, List<Item>>(responseDescription("All items")) {
    respond(listOf(Item("1", "Foo")))
}
```

See [Response Description](Response-Description.md) for more detail and priority
rules.

## Summary

| Mechanism                                 | Scope           | What it sets                                                    |
| ----------------------------------------- | --------------- | --------------------------------------------------------------- |
| `@Response(description, statusCode)`      | Class           | Response description and default HTTP status                    |
| `@Description(value)`                     | Property        | Field description in the schema                                 |
| `example` / `exampleResponse` route param | Route           | Inline response example                                         |
| `@WithExample` + `ExampleProvider`        | Class           | Reusable example(s) embedded in the schema                      |
| `@StringExample(vararg examples)`         | String property | Example values for a string field                               |
| `responseDescription()` module            | Route           | Override or supply description for `List<T>` and other generics |
