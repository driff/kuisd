# Tareas — Imágenes locales (`ImageRegistry`)

> Spec ID: 013 · Trazabilidad: ./requirements.md · ./design.md

Cada tarea es atómica y verificable. Marca `[x]` solo cuando su verificación pasa.

- [x] **T1** — `:sdui-compose`: `ImageRegistry.kt` (espejo de `IconRegistry`): `ImageRegistry`
  (`get`/`plus`/`Empty`, valores `@Composable () -> Painter`), DSL `imageRegistry{}` y `LocalImageRegistry`
  (`staticCompositionLocalOf { Empty }`). Test `ImageRegistryTest` (Empty→null; register→get no-null;
  plus override; presencia sin invocar el factory).
  - _ref:_ HU-2.1/2.2 · design §"ImageRegistry"
  - _verif:_ `./gradlew :sdui-compose:jvmTest` en verde (incluye `ImageRegistryTest`).

- [x] **T2** — `:sdui-compose`: extender `ImageProps` con `name` y ramificar `ImageRenderer`
  (`name` no vacío → `LocalImageRegistry.get(name)` → `foundation.Image(painter)`, fallback `Box`;
  si no → seam remoto 012). Imports `Image`/`Box`. `name` bindable y con prioridad sobre `url`.
  - _ref:_ HU-1.1/1.2/1.3/1.4/1.5, HU-3.1/3.3 · design §"image extendido"
  - _verif:_ `./gradlew :sdui-compose:jvmTest`; `rendererFor("image")` sigue != null; retrocompat 012
    (`ImageProps()` con solo `url` compila y mantiene el camino remoto).

- [x] **T3** — `:shared`: setup Compose Resources — dep `compose.components.resources` en `commonMain`;
  asset `src/commonMain/composeResources/drawable/kuisd_logo.xml` (vector drawable). Confirmar el paquete
  `Res` generado.
  - _ref:_ HU-2.3 · design §"Impl app", §"Riesgos" (paquete Res)
  - _verif:_ `./gradlew :shared:generateComposeResClass` (o build) genera `Res.drawable.kuisd_logo`;
    `:shared:compileKotlinDesktop` en verde.

- [x] **T4** — `:shared`: `image/AppImages.kt` (`appImageRegistry()` = `imageRegistry { register(
  "kuisd_logo") { painterResource(Res.drawable.kuisd_logo) } }`) y cablear `LocalImageRegistry provides
  remember { appImageRegistry() }` en el `CompositionLocalProvider` de `SduiHost`.
  - _ref:_ HU-2.3, HU-3.2 · design §"Impl app"
  - _verif:_ `./gradlew :shared:compileKotlinDesktop` (y `:desktopApp:compile*`) en verde.

- [x] **T5** — `:server`: pantalla piloto. Añadir un `image(name="kuisd_logo")` (con `contentScale` y un
  `UiModifier` de tamaño) a una pantalla existente (candidata: `home`, junto al banner remoto).
  - _ref:_ HU-1 · design §"Estrategia de verificación" (e2e)
  - _verif:_ `./gradlew :server:build`; `curl localhost:8080/screen/home` muestra `type:"image"` con
    `name:"kuisd_logo"`.

- [x] **T6** — Calidad + smoke visual: `./gradlew detekt ktlintCheck` en verde; `:desktopApp:run` y
  observar el logo local (sin red) y que un `name` inexistente cae al hueco neutro.
  - _ref:_ Requisitos no funcionales · design §"Estrategia de verificación"
  - _verif:_ lint/detekt verdes; smoke manual (si el harness no abre UI, se justifica con el árbol del
    piloto T5 + los tests como evidencia).

## Verificación final (Definition of Done)
- [x] `requirements.md` y `design.md` en `approved`.
- [x] `ImageRegistry` + `LocalImageRegistry` en `:sdui-compose` (espejo de `IconRegistry`), SIN assets propios.
- [x] `image` resuelve `name`→local / `url`→remoto; retrocompat 012 intacta; fallback sin crash.
- [x] `ImageRegistryTest` verde; `rendererFor("image") != null`.
- [x] Asset `kuisd_logo` empaquetado en Compose Resources de `:shared` y registrado; `SduiHost` lo provee.
- [x] `:sdui-compose:jvmTest`, `:shared:desktopTest`, `:server:build`, `detekt`, `ktlintCheck` en verde.
- [x] Pantalla piloto sirve `image(name="kuisd_logo")` (T5) — evidencia e2e.
- [x] `tasks.md` todo `[x]` salvo smoke visual manual si el harness no abre UI (justificado).
