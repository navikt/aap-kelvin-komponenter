# Response Description

When a route returns a type annotated with [`@Response`](../src/main/kotlin/com/papsign/ktor/openapigen/annotations/Response.kt),
its `description` field is automatically used as the response description in the generated OpenAPI specification:

```kotlin
@Response("A list of items")
data class Item(val id: String, val name: String)

get<Unit, Item> {
    respond(Item("1", "Foo"))
}
```

## The `responseDescription()` module

When the route's response type is a generic wrapper — most commonly `List<T>` — the `@Response` annotation
on the element type `T` is **not** picked up automatically, because the OpenAPI generator only inspects
the top-level type.

Use the `responseDescription()` route module to set the description explicitly in those cases:

```kotlin
get<Unit, List<Item>>(responseDescription("All items for a person")) {
    respond(listOf(Item("1", "Foo")))
}
```

`responseDescription()` works the same way as [`info()`](Basic-Routing.md) and [`status()`](Status-Codes.md) —
it is passed as an argument to the route method and applies only to that endpoint.

### Priority

When multiple sources provide a description, the priority is:

1. `responseDescription()` module — highest priority, always wins
2. `@Response.description` on the response type
3. Default HTTP status description (e.g. `"OK"`) — fallback

### Example: overriding `@Response.description`

`responseDescription()` can also be used to override the description from `@Response` on a
per-route basis without changing the annotation:

```kotlin
@Response("Generic item description")
data class Item(val id: String, val name: String)

// Uses the @Response description
get<Unit, Item> {
    respond(Item("1", "Foo"))
}

// Overrides it for this specific route
get<Unit, Item>(responseDescription("The primary item for this context")) {
    respond(Item("1", "Foo"))
}
```
