# Requisitos — Extraer el motor a un módulo Gradle `:sdui-compose`

> Spec ID: 006 · Estado: approved · Fecha: 2026-06-01

## Resumen
Hoy el motor de render (paquete `dev.kuisd.sdui`: `RenderNode`, `RenderScope`, `ComponentRegistry`,
`CorePack`, `SduiActionHandler`, `Logging`) vive **dentro de `:shared`**, junto a la app
(`dev.kuisd.app`*). La frontera motor/app es **solo por paquetes** y nadie la fuerza (la regla
Clean Architecture de la spec 003 no está enforzada — ver `config/detekt/detekt.yml`, que cita
"Splitting `:shared` into per-layer Gradle modules" como la opción 3). Esta feature **mueve el motor
a un nuevo módulo Gradle `:sdui-compose`** (lib KMP + Compose, depende de `:sdui-core`) y deja la app
en `:shared`, que pasa a depender de `:sdui-compose`. Resultado: **el compilador de Gradle fuerza** la
regla de dependencias (el motor no puede ni importar `dev.kuisd.app`) y `:sdui-compose` queda
**publicable** (la convención `sdui-*` del `build.gradle.kts` raíz aplica → `dev.kuisd:sdui-compose`).
Es un **refactor mecánico de empaquetado: NO cambia comportamiento.**

## Fuera de alcance
- Extraer también la app a `:app-client` / `:app-contract` (futuro).
- Cualquier cambio funcional: es refactor puro de empaquetado (mismo render, misma navegación,
  mismo catálogo de componentes, mismos contratos).
- Variables locales / binding reactivo (spec 005, en paralelo — no se toca su área).
- Aplicar `UiModifier`, theming por tokens, layout avanzado (siguen fuera, como en 001/003/004).
- Mover `dev.kuisd.app.data` (Ktor), `dev.kuisd.app.nav`, `presentation/` o `SampleScreens`: todo
  eso se queda en `:shared`.

## Historias de usuario y criterios de aceptación

### HU-1 — El motor vive en su propio módulo Gradle
**Como** mantenedor de la librería **quiero** el motor de render en `:sdui-compose` **para** que sea
una unidad de compilación y publicación independiente de la app.

Criterios (EARS):
1. The system SHALL contener un módulo Gradle `:sdui-compose` (lib KMP) con el paquete
   `dev.kuisd.sdui` y los archivos del motor (`RenderNode`, `RenderScope`, `ComponentRegistry`,
   `CorePack`, `SduiActionHandler`, `Logging`) y sus tests del motor.
2. The system SHALL conservar el **mismo paquete** `dev.kuisd.sdui` y las **mismas firmas públicas**
   del motor (solo cambia el módulo que las contiene), de modo que los `import dev.kuisd.sdui.*` de
   la app no cambien.
3. `:sdui-compose` SHALL declarar `api(project(":sdui-core"))` y las dependencias de Compose
   (runtime/foundation/material3/ui) que el motor necesita, espejando los targets de `:shared`
   (android/jvm/iosArm64/iosSimulatorArm64).
4. `:sdui-compose` SHALL **no** declarar `binaries.framework` propio (como `:sdui-core`): es una lib
   intermedia; el framework iOS lo sigue ensamblando `:shared`.

### HU-2 — Gradle fuerza la regla de dependencias (motor → app prohibido)
**Como** equipo **quiero** que el compilador impida que el motor dependa de la app **para** que la
frontera Clean Architecture sea estructural y no una convención frágil.

Criterios (EARS):
1. The system SHALL hacer que `:sdui-compose` dependa **solo** de `:sdui-core` + Compose; `:shared`
   (la app) depende de `:sdui-compose`, no al revés (sin ciclo).
2. WHEN un archivo del motor en `:sdui-compose` intente importar `dev.kuisd.app.*` o `io.ktor.*`,
   the system SHALL **fallar la compilación** (esos símbolos no están en el classpath del módulo).
3. The system SHALL verificar por `grep` que `sdui-compose/src/` no referencia `dev.kuisd.app` ni
   `io.ktor.` (red de seguridad además del classpath).

### HU-3 — La app sigue compilando y funcionando sin cambios de comportamiento
**Como** usuario **quiero** que Android, Desktop e iOS rendericen exactamente igual que antes
**para** confirmar que la extracción no introduce regresiones.

Criterios (EARS):
1. The system SHALL hacer que `:shared` declare la dependencia al motor
   (`project(":sdui-compose")`) y que `:androidApp`, `:desktopApp` (vía `:shared`) y `:server`
   sigan compilando.
2. WHEN la app provee `LocalComponentRegistry` / `LocalSduiActionHandler` y llama a `RenderNode`,
   the system SHALL resolverlos contra los tipos públicos de `:sdui-compose` con el **mismo
   comportamiento** (mismo `CorePack`, misma navegación de la spec 003, mismo `badge` de la 004).
3. WHERE el motor exponía un símbolo `internal` consumido por la app a través de la frontera de
   paquetes (p.ej. `sduiLog`, usado por `dev.kuisd.app.nav.NavActionHandler`), the system SHALL
   resolver esa visibilidad (hacerlo público en el motor o reubicar el uso en la app), porque
   `internal` **deja de cruzar** la nueva frontera de módulo.
4. The system SHALL conservar los tests existentes en verde: los del motor migran con él a
   `:sdui-compose:commonTest`; los de la app/nav siguen en `:shared:commonTest`.

### HU-4 — `:sdui-compose` es publicable bajo `dev.kuisd`
**Como** consumidor externo **quiero** consumir el motor como artefacto Maven **para** reutilizarlo
fuera de este repo.

Criterios (EARS):
1. The system SHALL hacer que la convención `sdui-*` del `build.gradle.kts` raíz aplique a
   `:sdui-compose` automáticamente (por prefijo de nombre), publicándolo como
   `dev.kuisd:sdui-compose:<versión>` (versión vía `-Pkuisd.version`, def. `0.1.0`).
2. WHEN se ejecuta `./gradlew :sdui-compose:publishToMavenLocal`, the system SHALL generar las
   publicaciones por target KMP (metadata/kotlinMultiplatform + android + jvm + iosArm64 +
   iosSimulatorArm64) en `~/.m2`, sin configuración extra en el build del módulo.

## Requisitos no funcionales
- **Refactor sin cambio de comportamiento:** ningún `.kt` del motor cambia de lógica; solo cambian
  de **módulo** (y, si hace falta, de visibilidad). La app no cambia salvo el wiring de build y la
  resolución de la visibilidad de HU-3.3.
- **Sin ciclos de dependencia** entre módulos Gradle.
- **Targets espejo:** `:sdui-compose` declara exactamente los targets de `:shared`
  (android/jvm/iosArm64/iosSimulatorArm64) para que la resolución KMP por atributo de plataforma
  funcione en las tres apps + server.
- **Calidad:** `detekt` y `ktlintCheck` en verde para el nuevo módulo y el resto.

## Dependencias y supuestos
- Depende de 001/002/003/004 (mergeadas): el motor ya existe como paquete `dev.kuisd.sdui` con la
  frontera por paquetes y el `ComponentRegistry` (004).
- Reutiliza la **convención de publicación** `sdui-*` del `build.gradle.kts` raíz (ya validada con
  `:sdui-core`).
- Todos los plugins/librerías necesarios (kotlin-multiplatform, android.kotlin.multiplatform.library,
  compose.multiplatform, compose.compiler, kotlin-serialization, runtime/foundation/material3/ui)
  ya están en `gradle/libs.versions.toml` (los usa `:shared` hoy).
- **Spec 005 en paralelo:** toca el área de variables locales; esta spec solo mueve archivos del
  motor y toca build files — coordinar el merge para evitar choques en `settings.gradle.kts` /
  `shared/build.gradle.kts`.
