# Modules
Modules are a way to extend behavior of the routing during Initialisation and Runtime.

#### Basic rules of module handling:

1. A Module type can be instantiated, but two equal instances cannot coexist (a set is used on the backend)
2. Modules can be queried by any other module by its class with `ModuleProvider.ofClass<Class>()`
3. The global module provider can be accessed through an extension or the OpenAPIGen instance with `OpenAPIGen.globalModuleProvider`
4. The current module provider on a specific route can be accessed through `OpenAPIRoute.provider` (all provided dsl functions properly scope the registered modules, if you implement custom ones make sure you create child handlers using `route.child().apply { provider.registerModule(YourModule()) }.yourBlockFn()`)

#### Key Interfaces: 
- OpenAPIModule: All modules must implement this interface, it does not have behavior besides indicating intent
- DependentModule: Allows to declare module dependencies from inside a module
- RouteOpenAPIModule: Used to indicate that it is to be used as additional module on one specific route, usually for additional metadata like descriptions, it can only be added explicitly during the final call before the route handle (get post, etc... with the parameters `get<Params, Response>(vararg RouteOpenAPIModule) { ... }`)
- HandlerModule: Provides a hook during route creation, used internally to generate the OpenAPI definition

And more, look at the interfaces that implement OpenAPIModule. The module system is used as much as possible internally to allow for maximal extendability.
