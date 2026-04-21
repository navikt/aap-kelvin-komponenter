# Setup

To setup the library in the ktor context, a few initial informations must be registered.

## Minimal configuration
```kotlin
install(OpenAPIGen) {
    // basic info
    info {
        version = "0.0.1"
        title = "Test API"
        description = "The Test API"
        contact {
            name = "Support"
            email = "support@test.com"
        }
    }
    // describe the server, add as many as you want
    server("http://localhost:8080/") {
        description = "Test server"
    }
    //optional
    schemaNamer = {
        //rename DTOs from java type name to generator compatible form
        val regex = Regex("[A-Za-z0-9_.]+")
        it.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
    }
}

// Content negotiation is required
install(ContentNegotiation) {
    jackson()
}
```

## Routing

Enable routing in an `Application` or `Routing` context

```kotlin
apiRouting {
  // declare routes here as you would in ktor, with specified types
}
```