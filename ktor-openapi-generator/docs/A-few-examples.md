# A few Examples

## Setup
```kotlin
application.install(OpenAPIGen) {
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
    //optional custom schema object namer
    replaceModule(DefaultSchemaNamer, object: SchemaNamer {
        val regex = Regex("[A-Za-z0-9_.]+")
        override fun get(type: KType): String {
           return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
        }
    })
}

application.apiRouting {
// routing goes here
}
```

## Expose the OpenAPI.json and swagger-ui

```kotlin
application.routing {
    get("/openapi.json") {
        call.respond(application.openAPIGen.api.serialize())
    }
    get("/") {
        call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
    }
}
```


## Routing
```kotlin
    get<ParameterType, ResponseType> { params ->       // get request on '/' because no path is specified
        respond(ResponseType(...))
    }

    route("someroute").get<ParameterType, ResponseType>(   //  get request on '/someroute'
        info("String Param Endpoint", "This is a String Param Endpoint"), // A Route module that adds a name and description to the OpenAPI data for the endpoint, it is optional
        example = ResponseType(...) // example for the OpenAPI data
    ) { params ->
        respond(ResponseType(...))
    }
```

Routes can be written in a chained manner:
```kotlin
route("a").route("b")...
```
or in blocks if you want a hierarchy
```kotlin
route("a") {
    route("b") {
        ...
    }
}
```
## Parameters
You may have noticed the `ParameterType` or `Params` in the `get` and `post` handlers, these allow to configure the parameters.
### Path Parameters:
```kotlin
@Path("{a}")
data class LongParam(@PathParam("A simple Long Param") val a: Long)

data class StringParam(@PathParam("A simple String Param") val str: Long)

route("someroute") {
    get<LongParam, ResponseType> { params ->  // get request on '/someroute/{a}'
        respond(ResponseType(...))
    }
}
route("{str}") {
    get<StringParam, ResponseType> { params ->  // get request on '/{str}', str will be the string 
        respond(ResponseType(...))
    }
}
```
Note that the name of the parameter's field must be the one in the brackets, if you have multiple ones it is undefined behavior.

### Query Parameters:
```kotlin
data class LongParam(@QueryParam("A simple Long Param") val a: Long)

route("someroute") {
    get<LongParam, ResponseType> { params ->  // get request on '/someroute/?a='
        respond(ResponseType(...))
    }
}
```
### Header Parameters:
```kotlin
data class LongParam(@HeaderParam("A simple Long Param") val `A-HEADER`: Long)

route("someroute") {
    get<LongParam, ResponseType> { params ->  // get request on '/someroute', and expects a header 'A-HEADER'
        respond(ResponseType(...))
    }
}
```
### Styles
All Parameter annotations have a `style` optional parameter that allows you to specify the style according to spec.

## Exceptions and multiple responses

The options you have:
```kotlin
throws(HttpStatusCode.BadRequest, CustomException::class) { ... // no example, just the exception handling
throws(HttpStatusCode.BadRequest, "example", CustomException::class) { ... // exception  handling with example, will respond example
throws(HttpStatusCode.BadRequest, "example", {ex: CustomException -> ex.toString()}) { ... // exception handling, will respond generated content
```

Now we want to respond a custom generic `Error` object when an exception is thrown:
```kotlin
data class Error<P>(val id: String, val payload: P)

throws(HttpStatusCode.BadRequest, Error("bad.request", mapOf<String, String>()), {ex: CustomException -> Error(ex.id, ex.payload)}) {
    get<ParameterType, ResponseType> { params -> 
        respond(ResponseType(...))
    }
}
```
You can also define multiple ones:
```kotlin
data class Error<P>(val id: String, val payload: P)

throws(HttpStatusCode.BadRequest, Error("bad.request", mapOf<String, String>()), {ex: CustomException -> Error(ex.id, ex.payload)}) {
    throws(HttpStatusCode.InternalServerError, Error("internal.error", mapOf<String, String>()), {ex: OtherCustomException -> Error(ex.id, ex.payload)}) {
        get<ParameterType, ResponseType> { params -> 
            respond(ResponseType(...))
        }
    }
}
```
And different response types:
```kotlin
data class Error<P>(val id: String, val payload: P)

throws(HttpStatusCode.BadRequest, Error("bad.request", mapOf<String, String>()), {ex: CustomException -> Error(ex.id, ex.payload)}) {
    throws(HttpStatusCode.InternalServerError, "err", {ex: OtherCustomException -> ex.id}) {
        get<ParameterType, ResponseType> { params -> 
            respond(ResponseType(...))
        }
    }
}
```
If you want to respond normally you can also do:
```kotlin
// null example means no example, you can of course also add one if you want
throws(HttpStatusCode.OK, null, {ex: CustomException -> OtherResponseType(ex.response)}) {
    get<ParameterType, ResponseType> { params -> 
        if (shouldRespondOther) throw CustomException(response)
        respond(ResponseType(...))
    }
}
```

## Sealed Class Support / Jackson Polymorphic Deserialization
If you use polymorphic json, and you:
You have Type A
```json
{
    "@type" : "a",
    "str": "Some String"
}
```
You have Type B
```json
{
    "@type" : "b",
    "i": 1
}
```
You have Type C
```json
{
    "@type" : "c",
    "l": 0
}
```
You would define it like this
```kotlin
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes(
        JsonSubTypes.Type(Base.A::class, name = "a"),
        JsonSubTypes.Type(Base.B::class, name = "b"),
        JsonSubTypes.Type(Base.C::class, name = "c")
    )
    sealed class Base {

        class A(val str: String) : Base()

        class B(val i: @Min(0) @Max(2) Int) : Base() // Min and Max constrain the value, it will be shown in the OpenAPI spec, you can also implement custom ones as the feature is not hard-coded, look at how they are defined

        @WithExample // provide an example in a subtype, the companion object and the annotation are required
        class C(val l: @Clamp(0, 10) Long) : Base() {
            companion object: ExampleProvider<C> {
                override val example: C? = C(5)
            }
        }
    }
// and then use as always as request or response, here it just responds what it receives
    post<Params, Base, Base>(
        info("Sealed class Endpoint", "This is a Sealed class Endpoint"),
        exampleRequest = Base.A("Hi"),
        exampleResponse = Base.A("Hi")
    ) { params, base ->
        respond(base)
    }
```

## Binary Request/Response

```kotlin

const val contentType = "image/png"

@BinaryRequest([contentType]) // can be omitted if you don't want to use it as request
@BinaryResponse([contentType]) // can be omitted if you don't want to use it as response
data class RawPng(val stream: InputStream)

// then in your route like usual

post<Params, Response, RawPng> { params, body->
...
}
post<Params, RawPng, Body> { params, body ->
...
}
```

## Multipart Request

```kotlin
@FormDataRequest
data class PDFFileCreateDTO(@PartEncoding("application/pdf") val file: NamedFileInputStream, val name: String, val public: Boolean)

// then in your route like usual

post<Params, Response, PDFFileCreateDTO> { params, fileCreate ->
...
}
```

