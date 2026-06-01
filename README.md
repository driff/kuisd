# kuisd

Proyecto **Kotlin Multiplatform (KMP)** para un sistema **SDUI (Server-Driven UI)** con backend **Ktor BFF**.

Arrancado a partir del blueprint `full_app` (mismo layout y versiones) más un módulo de
servidor Ktor y el contrato compartido `:sdui-core`.

## Módulos

| Módulo        | Tipo            | Rol |
|---------------|-----------------|-----|
| `:sdui-core`  | KMP library     | Contrato `@Serializable` del árbol SDUI (`SduiNode`, `SduiEnvelope`, `UiAction`, tokens, patches). Lo consumen cliente y servidor. |
| `:shared`     | KMP library     | Código compartido del cliente (Compose Multiplatform). Depende de `:sdui-core`. |
| `:androidApp` | Android app     | Entrypoint Android. |
| `:desktopApp` | JVM app         | Entrypoint Compose Desktop. |
| `:server`     | JVM app (Ktor)  | BFF SDUI sobre **Netty**. Sirve los árboles construidos con `:sdui-core`. |
| `iosApp`      | Xcode (xcodegen)| Entrypoint iOS (consume el framework `shared`). |

## Stack

Kotlin 2.3.21 · AGP 9.1.1 · Gradle 9.4.1 · Compose Multiplatform 1.11.0 · Ktor 3.5.0
(server Netty) · kotlinx.serialization 1.11.0 · JVM toolchain 25 · Android compileSdk 36 / minSdk 26.

## Tareas comunes

```bash
./gradlew projects                 # lista los módulos
./gradlew :sdui-core:check         # compila + test del contrato
./gradlew :server:build            # compila + test del servidor
./gradlew :server:run              # arranca el BFF en http://localhost:8080
./gradlew :desktopApp:run          # cliente de escritorio
./gradlew :androidApp:assembleDebug
./gradlew detekt ktlintCheck       # calidad
```

Con el servidor arriba:

```bash
curl localhost:8080/health          # -> OK
curl localhost:8080/screen/home     # -> SduiEnvelope JSON
```

## iOS

```bash
cd iosApp && xcodegen   # genera el .xcodeproj a partir de project.yml
```

Luego abrir `iosApp/iosApp.xcodeproj` en Xcode y ejecutar el esquema `iosApp`.

## Publicar las librerías SDUI (consumir desde otro proyecto)

Las librerías reutilizables (`sdui-*`) se publican a Maven bajo el grupo **`dev.kuisd`**.
La convención de publicación (en el `build.gradle.kts` raíz) se aplica **automáticamente
a cualquier módulo cuyo nombre empiece por `sdui-`** — hoy `:sdui-core`; cuando se
añadan `:sdui-compose` / `:sdui-ktor` la heredan sin tocar nada.

```bash
# Publicar a tu repositorio local (~/.m2) para probar
./gradlew publishToMavenLocal            # todos los sdui-*
./gradlew :sdui-core:publishToMavenLocal # solo uno

# Publicar a un repositorio remoto (GitHub Packages, Artifactory, etc.)
./gradlew publishAllPublicationsToRemoteRepository \
    -Pkuisd.publish.url=https://maven.pkg.github.com/<org>/<repo> \
    -Pkuisd.publish.user=<usuario> \
    -Pkuisd.publish.password=<token>
```

La versión por defecto es `0.1.0`; cámbiala con `-Pkuisd.version=x.y.z`.

Para un proyecto **KMP** que consume `sdui-core` se obtienen automáticamente los variants
`jvm`, `android`, `iosArm64` e `iosSimulatorArm64` (más la metadata multiplatform):

```kotlin
// settings.gradle.kts del consumidor
dependencyResolutionManagement {
    repositories {
        mavenLocal()   // o el repo remoto
        mavenCentral()
    }
}

// build.gradle.kts del consumidor
dependencies {
    implementation("dev.kuisd:sdui-core:0.1.0")
}
```

> El contrato expone `kotlinx-serialization` como `api`, así que el consumidor recibe
> transitivamente `Json`/`KSerializer` para (de)serializar los `SduiEnvelope`.
