# Extensions

Extensions are a way to add custom behavior to the Ktor OpenAPI Generator

currently supported hooks (see interface OpenAPIGenExtension):

- onInit(OpenAPIGen)

feel free to request more hooks, we add them on an as needed basis.

## Creating an extension

### 1. Implement OpenAPIGenExtension

```kotlin
// in this example we create an interface to automatically register new implemented global modules (See Modules)
// this interface is available in the library directly, no need to reimplement it
interface OpenAPIGenModuleExtension: OpenAPIModule, OpenAPIGenExtension {
    override fun onInit(gen: OpenAPIGen) {
        gen.globalModuleProvider.registerModule(this)
    }
}
```

### 2. Register the package to search for the implementations

```kotlin
install(OpenAPIGen) {
    ...
    scanPackagesForModules += "com.example.extensions"
}
```
