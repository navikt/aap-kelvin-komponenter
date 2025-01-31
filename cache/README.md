# Kelvine Cache

En proxy warpper rundt Caffeine for å cache metodekall

## Hvordan bruke

For å benytte cachen må vi ha et interface for implementasjonen vi ønsker å cache
```kotlin

interface  ExampleInterface {
    fun metode(): String
}

class EksempelImplementasjon : ExampleInterface {
    override fun metode() = "String"
}
```

Så må vi gi en instans av implementasjonen til `withCache` funksjonen.

```kotlin
val eksempelImplementasjonProxy = withCache(EksempelImplementasjon()) as ExampleInterface
```

Nå har vi en proxy instans av EksempelImplementasjon, som så lang, ikke gjør noen ting. For at en metode i skal caches må vi legge på `@Cacheable` annotasjonen på metodene vi ønsker å cache.

``` kotlin
class EksempelImplementasjon : ExampleInterface {
    @Cacheable
    override fun metode() = "String"
}
```

Uten å gi `@Cacheable` noen parametere blir default konfigurasjonen benyttet.

## Konfigurasjoner

### Tidsbestemt 

expireAfterWrite bestemmer hvor lenge en cache entry skal leve. Default er to minutter

```kotlin
    @Cacheable(ExpireAfterWrite(duration = 5, timeUnit = TimeUnit.SECONDS))
    fun metode() {}
```

### Størrelsesbasert 

maximumSize setter hvor mange cache entries vi ønsker før vi evicter

```kotlin
    @Cacheable(maximumSize = 1)
fun metode() {}
```