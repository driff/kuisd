# Plan de Implementación — kuisd (SDUI · Ktor BFF + KMP)

Guía ejecutable para arrancar el proyecto **kuisd** en tu IDE. Sigue los pasos en orden; al final del documento tienes el módulo `:sdui-core` completo y compilable para validar que todo enlaza antes de seguir.

**Versiones (mayo 2026):** Kotlin 2.3.20 · Compose Multiplatform 1.11.0 · Ktor 3.5.0 · kotlinx.serialization 1.10.0 · AGP 8.9.0 · Gradle 8.12

> Nota: a partir de Kotlin 2.3.0 el target Android en KMP usa el plugin `com.android.kotlin.multiplatform.library` en vez de `com.android.library`. Lo reflejo en los builds.

---

## Paso 0 — Crear el proyecto

Lo más rápido es generar el esqueleto base con el wizard oficial (trae Gradle wrapper, estructura iOS, etc.) y luego añadir los módulos SDUI encima.

**Opción A (recomendada):** ve a **kmp.new** o IntelliJ IDEA → `File | New | Project | Kotlin Multiplatform`, marca **Android + iOS** (iOS: "Share UI" con Compose Multiplatform). Nómbralo `kuisd`, ubícalo en `~/dev/Kotlin/`. Esto te crea `composeApp/` + `iosApp/` + el wrapper.

**Opción B (manual):** terminal en `~/dev/Kotlin/`:
```bash
mkdir kuisd && cd kuisd
git init
# copia el gradle wrapper de cualquier proyecto KMP reciente, o:
gradle wrapper --gradle-version 8.12
```

Luego añade los módulos `:sdui-core`, `:sdui-compose`, `:sdui-ktor`, `:app-contract`, `:server` como se indica abajo.

---

## Paso 1 — Estructura objetivo del monorepo

```
kuisd/
├── gradle/
│   ├── wrapper/
│   └── libs.versions.toml          # version catalog (Paso 2)
├── settings.gradle.kts             # (Paso 3)
├── build.gradle.kts                # raíz, plugins apply false (Paso 4)
├── gradle.properties
│
├── sdui-core/                      # KMP lib — el contrato (Paso 6, COMPILABLE)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/dev/kuisd/sdui/core/
│       ├── SduiNode.kt
│       ├── SduiEnvelope.kt
│       ├── SduiPatch.kt
│       ├── UiAction.kt
│       ├── UiModifier.kt
│       ├── DesignTokens.kt
│       ├── SduiComponent.kt
│       └── SduiJson.kt
│
├── sdui-compose/                   # KMP lib — motor de render (Paso 7)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/dev/kuisd/sdui/compose/
│
├── sdui-ktor/                      # JVM lib — plugin de servidor (Paso 8)
│   ├── build.gradle.kts
│   └── src/main/kotlin/dev/kuisd/sdui/ktor/
│
├── app-contract/                   # KMP lib — catálogo propio (Paso 9)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/dev/kuisd/app/contract/
│
├── server/                         # JVM app — Ktor BFF (Paso 10)
│   ├── build.gradle.kts
│   └── src/main/kotlin/dev/kuisd/server/
│
├── composeApp/                     # KMP app — cliente (del wizard)
│   ├── build.gradle.kts
│   └── src/{commonMain,androidMain,iosMain}/
│
└── iosApp/                         # entrypoint iOS (del wizard)
```

---

## Paso 2 — `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "2.3.20"
compose = "1.11.0"
agp = "8.9.0"
ktor = "3.5.0"
serialization = "1.10.0"
coroutines = "1.10.2"
koin = "4.1.0"
coil = "3.4.0"
logback = "1.5.18"
androidx-activity = "1.10.1"
android-minSdk = "24"
android-compileSdk = "35"

[libraries]
# contrato / core
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-core    = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

# ktor server
ktor-server-core         = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
ktor-server-netty        = { module = "io.ktor:ktor-server-netty", version.ref = "ktor" }
ktor-server-content-neg  = { module = "io.ktor:ktor-server-content-negotiation", version.ref = "ktor" }
ktor-serialization-json  = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-server-status-pages = { module = "io.ktor:ktor-server-status-pages", version.ref = "ktor" }
ktor-server-cors         = { module = "io.ktor:ktor-server-cors", version.ref = "ktor" }
ktor-server-call-logging = { module = "io.ktor:ktor-server-call-logging", version.ref = "ktor" }
ktor-server-test-host    = { module = "io.ktor:ktor-server-test-host", version.ref = "ktor" }
logback-classic          = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }

# ktor client (composeApp)
ktor-client-core        = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-neg = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-okhttp      = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin      = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-logging     = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }

# di / imagenes
koin-core         = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-ktor         = { module = "io.insert-koin:koin-ktor", version.ref = "koin" }
coil-compose      = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }

[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinJvm           = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
androidApplication  = { id = "com.android.application", version.ref = "agp" }
androidKmpLibrary   = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
composeMultiplatform= { id = "org.jetbrains.compose", version.ref = "compose" }
composeCompiler     = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ktor                = { id = "io.ktor.plugin", version.ref = "ktor" }
```

---

## Paso 3 — `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "kuisd"
include(
    ":sdui-core",
    ":sdui-compose",
    ":sdui-ktor",
    ":app-contract",
    ":server",
    ":composeApp",
)
```

---

## Paso 4 — `build.gradle.kts` raíz

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
}
```

`gradle.properties`:
```properties
kotlin.code.style=official
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.caching=true
android.useAndroidX=true
```

---

## Paso 5 — Verificación temprana

Antes de escribir código, confirma que el esqueleto enlaza:
```bash
./gradlew projects
```
Debe listar los seis módulos. Si falla aquí, es config de Gradle, no código.

---

## Paso 6 — `:sdui-core` (COMPILABLE — el corazón)

`sdui-core/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    androidLibrary {
        namespace = "dev.kuisd.sdui.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "SduiCore" }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
```

Ahora los archivos del contrato. Todos en `sdui-core/src/commonMain/kotlin/dev/kuisd/sdui/core/`.

**`SduiJson.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphicDefaultDeserializer

fun sduiJson(extra: SerializersModule = EmptySerializersModule()): Json = Json {
    classDiscriminator = "type"
    encodeDefaults = false
    ignoreUnknownKeys = true
    explicitNulls = false
    serializersModule = extra + SerializersModule {
        polymorphicDefaultDeserializer(UiAction::class) { NoOpAction.serializer() }
    }
}

val DefaultSduiJson: Json = sduiJson()
```

**`SduiNode.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiNode(
    val type: String,
    val id: String? = null,
    val props: JsonObject = JsonObject(emptyMap()),
    val modifier: UiModifier = UiModifier(),
    val actions: Map<String, List<UiAction>> = emptyMap(),
    val children: List<SduiNode> = emptyList(),
)
```

**`SduiEnvelope.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SduiEnvelope(
    val schemaVersion: Int,
    val screenId: String,
    val root: SduiNode,
    val variables: Map<String, JsonElement> = emptyMap(),
    val meta: Map<String, String> = emptyMap(),
)
```

**`UiAction.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface UiAction

@Serializable @SerialName("navigate")
data class Navigate(val route: String, val args: Map<String, String> = emptyMap()) : UiAction

@Serializable @SerialName("setVar")
data class SetVar(val name: String, val value: JsonElement) : UiAction

@Serializable @SerialName("increment")
data class Increment(val name: String, val by: Int = 1, val min: Int? = null, val max: Int? = null) : UiAction

@Serializable @SerialName("toggle")
data class Toggle(val name: String) : UiAction

@Serializable @SerialName("network")
data class FireEndpoint(val endpoint: String, val payloadVars: List<String> = emptyList()) : UiAction

@Serializable @SerialName("track")
data class Track(val event: String, val props: Map<String, String> = emptyMap()) : UiAction

@Serializable @SerialName("custom")
data class CustomAction(val name: String, val payload: JsonObject = JsonObject(emptyMap())) : UiAction

@Serializable @SerialName("noop")
data object NoOpAction : UiAction
```

**`UiModifier.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.Serializable

@Serializable
data class UiModifier(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val width: SpaceToken? = null,
    val height: SpaceToken? = null,
    val padding: PaddingTokens? = null,
    val background: ColorToken? = null,
    val cornerRadius: RadiusToken? = null,
    val weight: Float? = null,
    val alignment: String? = null,
)

@Serializable
data class PaddingTokens(
    val l: SpaceToken? = null,
    val t: SpaceToken? = null,
    val r: SpaceToken? = null,
    val b: SpaceToken? = null,
)
```

**`DesignTokens.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable @JvmInline value class ColorToken(val ref: String)
@Serializable @JvmInline value class TypeToken(val ref: String)
@Serializable @JvmInline value class SpaceToken(val ref: String)
@Serializable @JvmInline value class RadiusToken(val ref: String)

object Tokens {
    object Color {
        val Primary = ColorToken("color.primary"); val OnPrimary = ColorToken("color.onPrimary")
        val Surface = ColorToken("color.surface"); val OnSurface = ColorToken("color.onSurface")
        val Error = ColorToken("color.error"); val Outline = ColorToken("color.outline")
    }
    object Type {
        val Display = TypeToken("type.display"); val Title = TypeToken("type.title")
        val Body = TypeToken("type.body"); val Label = TypeToken("type.label"); val Button = TypeToken("type.button")
    }
    object Space {
        val Xs = SpaceToken("space.xs"); val Sm = SpaceToken("space.sm")
        val Md = SpaceToken("space.md"); val Lg = SpaceToken("space.lg"); val Xl = SpaceToken("space.xl")
    }
    object Radius {
        val None = RadiusToken("radius.none"); val Card = RadiusToken("radius.card"); val Pill = RadiusToken("radius.pill")
    }
}
```

**`SduiPatch.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiPatch(val schemaVersion: Int, val changes: List<PatchOp>)

@Serializable
sealed interface PatchOp

@Serializable @SerialName("replace")
data class Replace(val targetId: String, val node: SduiNode) : PatchOp

@Serializable @SerialName("updateProps")
data class UpdateProps(val targetId: String, val props: JsonObject) : PatchOp

@Serializable @SerialName("insert")
data class Insert(val parentId: String, val index: Int, val node: SduiNode) : PatchOp

@Serializable @SerialName("remove")
data class Remove(val targetId: String) : PatchOp

@Serializable @SerialName("setVars")
data class SetVars(val values: Map<String, JsonElement>) : PatchOp
```

**`SduiComponent.kt`**
```kotlin
package dev.kuisd.sdui.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

interface SduiComponent<P : Any> {
    val type: String
    val serializer: KSerializer<P>
}

inline fun <reified P : Any> sduiComponent(type: String): SduiComponent<P> =
    object : SduiComponent<P> {
        override val type = type
        override val serializer = serializer<P>()
    }
```

**Verifica que compila:**
```bash
./gradlew :sdui-core:compileKotlinJvm
```
Si esto pasa, el contrato está sólido y podemos construir las dos librerías encima.

---

## Pasos 7–10 — Siguientes módulos (resumen)

- **`:sdui-compose`** (Paso 7): plugins `kotlinMultiplatform + androidKmpLibrary + composeMultiplatform + composeCompiler`. Depende de `:sdui-core`. Aquí van `ComponentRegistry`, `RenderScope`, `SduiHost`, `VariableStore`, `applyPatch`, mapper de `UiModifier`, `SduiTheme` + resolvers, `CorePack`.
- **`:sdui-ktor`** (Paso 8): plugins `kotlinJvm + kotlinSerialization`. Depende de `:sdui-core` + `ktor-server-core`. Plugin `Sdui`, `SduiConfig`, DSL `sduiTree`, patch DSL, capability negotiation.
- **`:app-contract`** (Paso 9): KMP lib, depende de `:sdui-core`. Props + descriptores de tus componentes propios (Stepper, etc.).
- **`:server`** (Paso 10): plugins `kotlinJvm + ktor + kotlinSerialization`. Depende de `:sdui-ktor` + `:app-contract`. `Application.kt`, pantallas, action handlers.
- **`composeApp`**: añade dep a `:sdui-compose` + `:app-contract`; registra renderers, monta `SduiHost`, dispatcher real.

---

## Instrucciones para pasar a tu IDE (code)

1. Abre `~/dev/Kotlin/kuisd/` en IntelliJ IDEA o Android Studio (con plugin KMP).
2. Crea los archivos de los Pasos 2, 3, 4 (raíz del proyecto).
3. Corre `./gradlew projects` → confirma los 6 módulos.
4. Crea el módulo `:sdui-core` con el `build.gradle.kts` y los 8 archivos del Paso 6.
5. Corre `./gradlew :sdui-core:compileKotlinJvm` → debe compilar.
6. Sync Gradle en el IDE. A partir de aquí seguimos con `:sdui-compose`.

**Orden de trabajo recomendado:** core ✅ → sdui-ktor (server mínimo que devuelve un envelope hardcodeado) → sdui-compose (host + CorePack que renderiza ese envelope) → conectar cliente↔server → app-contract con el primer componente propio → validar en Android e iOS.

---

## Checklist

1. [ ] Proyecto base creado (wizard o manual) en `~/dev/Kotlin/kuisd/`
2. [ ] `libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts` raíz
3. [ ] `./gradlew projects` lista 6 módulos
4. [ ] `:sdui-core` con los 8 archivos del contrato
5. [ ] `./gradlew :sdui-core:compileKotlinJvm` verde
6. [ ] Continuar con `:sdui-ktor` (server mínimo)
7. [ ] `:sdui-compose` (host + CorePack)
8. [ ] Conectar cliente ↔ server (GET /screen/home)
9. [ ] `:app-contract` + primer componente propio
10. [ ] Validar render en Android + iOS
