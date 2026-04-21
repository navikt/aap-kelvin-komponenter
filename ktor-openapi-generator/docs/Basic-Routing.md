# Basic Routing

Basic routing happens like the Locations api from Ktor.

## Minimal GET Request

### 1. Register a response DTO
```kotlin
    @Response("A String Response")
    data class StringResponse(val str: String)
```

### 2. Register a GET request

inside the `apiRouting` as described in [Setup](https://github.com/papsign/Ktor-OpenAPI-Generator/wiki/Setup#routing)
```kotlin
    get<Unit, StringResponse> { 
        respond(StringResponse("Hello World"))
    }
```

## GET Request with parameters

### 1. Register a response DTO
```kotlin
    @Response("A String Response")
    data class StringResponse(val str: String)
```

### 2. Register a parameter handler
```kotlin
    @Path("string/{a}") // `@Path` works like the ktor Locations api `@Location`, if it is declared in a route, it will append the path to the one in the context
    data class StringParam(
        @PathParam("A simple String Param", PathParamStyle.matrix) val a: String, // You can provide a parameter style hint
        @QueryParam("Optional String") val optional: String? // Nullable Types are optional 
    )
```

### 3. Register a GET request

```kotlin
    get<StringParam, StringResponse> { params ->
        respond(StringResponse(params.a))
    }
```