# Transformers and Validators
Inspired by Javax validators

#### Performance considerations:
Reflection is performed during the route build to create a list of transformers on the request payloads, at runtime the parameters are transformed by the built list of transformers.

## Basic Example

(Transformers and validators can be applied to both Parameters and request Bodies)

```kotlin

@Request("Requests an invitation email")
data class EmailInvite(
    @LowerCase // Apply the lowercase transformer
    @Trim // Apply the trim transformer
    val email: String // The received email will be lowercase and trimmed
)

...
// Then just create a route like always
route("invite") {
    post<IDParam, EmailInviteResponse, EmailInvite, APIPrincipal> {params, invite ->
        respond(EmailInviteResponse(EmailService.sendSignatureInvitationEmail(invite, params.processID, principal()).await()))
    }
}
```

## Creating a new Transformer/Validator

### 1. Create the Transformer Validator
```kotlin

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
@ValidationAnnotation
annotation class LowerCase

object LowerCaseValidator: AValidator<String, LowerCase>(String::class, LowerCase::class) {
    override fun validate(subject: String?, annotation: LowerCase): String? {
        return subject?.toLowerCase() // Throw exception if the request is invalid, it will be caught by any appropriate exception handler that was declared on the route
    }
}

```

### 2. Register the package for the reflections library to find the Transformer/Validator

```kotlin
install(OpenAPIGen) {
    ...
    scanPackagesForModules += "com.example.validators"
}
```