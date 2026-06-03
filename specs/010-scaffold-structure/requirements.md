# Requisitos — Estructura de pantalla (`scaffold` · `topAppBar` · `bottomBar`)

> Spec ID: 010 · Estado: approved · Fecha: 2026-06-02

## Resumen
Hoy el motor renderiza un único árbol de contenido (`column` raíz) sin andamiaje de pantalla: no
hay barra superior ni barra de navegación inferior. El server tiene que simular títulos con `text` y
la navegación entre secciones (`home`/`feed`/`more`) no tiene una barra persistente. Esta spec añade los
**componentes estructurales** que componen una pantalla Material: `scaffold` (andamiaje con slots
`topBar`/`bottomBar`/`content`), `topAppBar` (título + icono de navegación + acciones) y `bottomBar`
(barra de navegación inferior con ítems seleccionables). Todos respetan la **frontera del motor**:
renderizan y **delegan acciones** al handler; el estado de selección (sección activa) se modela con
**variables locales** (spec 005), sin estado propio del motor.

**Mecanismo de slots (decisión tomada): por tipo de child.** El `scaffold` infiere el slot de cada
child a partir de su `type` — `topAppBar`→`topBar`, `bottomBar`→`bottomBar`, el resto→`content`. No se
toca el contrato compartido `:sdui-core` (`SduiNode` se queda con solo `children`).

## Fuera de alcance
- **`tabs` (pestañas con contenido conmutado)** — diferido a una spec posterior; reutiliza el binding
  reactivo (005) del mismo modo que `bottomBar`, pero es un slice aparte para mantener entregables
  pequeños.
- **Overlays / superposiciones** (`dialog`, `snackbar`, `bottomSheet`) — son transitorios e
  imperativos; van en la **spec 011** (que se apoya en el `snackbarHost` que expone este `scaffold`).
- **`navigationRail` / `drawer` / `modalDrawer`** — andamiaje adaptativo (tablet/desktop); spec posterior.
- **Scroll-behavior del `topAppBar`** (collapsing / pinned / enterAlways) — fuera; la barra es estática.
- **Back-stacks múltiples por sección** — la navegación sigue el modelo de un único back-stack
  (spec 003); seleccionar una sección de `bottomBar` despacha acciones (p.ej. `navigate`/`setVar`), no
  gestiona pilas paralelas.
- **Animaciones de transición** entre secciones — fuera (cambio inmediato).
- **`FloatingActionButton`** como slot del scaffold — fuera de esta primera versión (posible follow-up).

## Historias de usuario y criterios de aceptación

### HU-1 — `scaffold`: andamiaje con slots
**Como** autor de pantallas (server) **quiero** un contenedor que coloque una barra superior, un
contenido desplazable y una barra inferior **para** estructurar pantallas Material sin maquetar a mano.

Criterios (EARS):
1. The engine SHALL registrar en `CorePack` un componente `scaffold` que renderiza un
   `androidx.compose.material3.Scaffold`.
2. The engine SHALL resolver los **slots** del scaffold por el `type` de cada child:
   `topAppBar`→`topBar`, `bottomBar`→`bottomBar`, y **todos los demás children**→`content` (en orden).
   El slot `topBar`/`bottomBar` es opcional; el `content` es el cuerpo (puede tener 0..N children).
3. WHEN no hay ningún child de tipo `topAppBar` (resp. `bottomBar`), the engine SHALL omitir ese slot
   del `Scaffold` (sin barra superior/inferior) sin error.
4. The engine SHALL aplicar el `paddingValues` del `Scaffold` al slot `content` para que el contenido
   no quede tapado por las barras.
5. IF hay más de un child de tipo `topAppBar` (resp. `bottomBar`), THEN the engine SHALL usar el
   **primero** como esa barra e **ignorar** los demás (comportamiento determinista, documentado); el
   slot `content` lo forman únicamente los children cuyo `type` no es `topAppBar` ni `bottomBar`.
6. The engine SHALL respetar el `UiModifier` del nodo `scaffold` en su composable raíz (regla 008).

### HU-2 — `topAppBar`: barra superior
**Como** autor de pantallas **quiero** una barra superior con título, icono de navegación y acciones
**para** dar contexto y navegación a la pantalla.

Criterios (EARS):
1. The engine SHALL registrar un componente `topAppBar` que renderiza un `TopAppBar` de Material3 con
   un `title` (texto bindable, regla 005).
2. WHEN el nodo declara una acción `onNavigationClick`, the engine SHALL mostrar un icono de
   navegación (nombre de icono resoluble por `IconRegistry`, regla 008) y, al pulsarlo, SHALL despachar
   esa lista de acciones al handler.
3. IF no hay acción `onNavigationClick`, THEN the engine SHALL omitir el icono de navegación.
4. The engine SHALL renderizar como **acciones** (lado derecho) los `children` del `topAppBar` (p.ej.
   `iconButton`s), delegando cada `onClick` al handler como cualquier nodo.
5. The engine SHALL resolver el color/typografía de la barra desde el `KuisdTheme`/Material por defecto
   (sin literales `dp`/color propios del motor).

### HU-3 — `bottomBar`: navegación inferior
**Como** usuario **quiero** una barra inferior con secciones seleccionables **para** moverme entre las
áreas principales de la app.

Criterios (EARS):
1. The engine SHALL registrar un componente `bottomBar` que renderiza un `NavigationBar` de Material3.
2. The engine SHALL renderizar un `NavigationBarItem` por cada child de tipo `bottomBarItem`, con su
   `icon` (nombre, `IconRegistry`) y `label` (texto bindable).
3. The engine SHALL marcar como **seleccionado** el ítem cuyo `value` (string del ítem) coincide con el
   valor actual de la variable indicada en la prop `selectedBind` del `bottomBar` (binding `$var`, 005).
4. WHEN el usuario pulsa un ítem, the engine SHALL despachar las acciones `onClick` de ese ítem al
   handler (típicamente `setVar(selected, value)` + `navigate`), sin gestionar el estado por su cuenta.
5. IF `selectedBind` está ausente o su variable no resuelve, THEN the engine SHALL renderizar todos los
   ítems como no seleccionados (sin crash).

### HU-4 — Resiliencia y catálogo
**Como** integrador **quiero** que estos componentes degraden con elegancia **para** no romper la
pantalla ante envelopes incompletos o tipos hijos inesperados.

Criterios (EARS):
1. The engine SHALL exponer todos los componentes nuevos vía `CorePack` (registrados por nombre), de
   modo que `ComponentRegistry` y el patrón OCP (004) se mantengan.
2. IF un child no corresponde al rol esperado por su contenedor (p.ej. un `text` dentro de `bottomBar`
   donde se esperan `bottomBarItem`), THEN the engine SHALL ignorarlo en la zona de ítems (o renderizar
   `UnknownNode` donde aplique), sin crash.
3. The engine SHALL mantener el comportamiento `UnknownNode` (resiliencia 004) para tipos no registrados.

## Requisitos no funcionales
- **Multiplataforma:** todo en `commonMain` de `:sdui-compose`; sin APIs específicas de plataforma.
- **Frontera del motor:** sin estado propio del motor para selección; toda selección vive en variables
  locales (005) y se muta por acciones delegadas (003/009). El motor solo renderiza + delega.
- **Sin números mágicos:** apariencias por defecto desde `KuisdTheme`/Material (regla 008); el motor no
  materializa `dp`/colores literales.
- **Determinismo:** resolución de slots/ítems estable y documentada (primero gana ante duplicados).

## Dependencias y supuestos
- Depende de: 003 (acciones + navegación), 004 (`ComponentRegistry`/OCP), 005 (variables locales
  reactivas + binding `$var`), 008 (`KuisdTheme` + `IconRegistry` + `UiModifier` aplicado).
- **Decisión tomada — mecanismo de slots: por tipo de child.** El `scaffold` particiona sus `children`
  por `type` (`topAppBar`→topBar, `bottomBar`→bottomBar, resto→content). No se modifica el contrato
  `:sdui-core`. (Alternativas descartadas: `id` reservado — sobrecarga la semántica de `id`; campo
  `slot: String?` en `SduiNode` — toca el contrato compartido sin necesidad para este alcance.)
- **Supuesto:** `Material3` ya disponible (usado por 007/008); `TopAppBar`/`Scaffold` pueden requerir
  `@OptIn(ExperimentalMaterial3Api::class)` acotado al renderer (mismo patrón que `textField` en 007).
- **Supuesto:** las pantallas de demo del server (`home`/`feed`/`more`) pueden migrarse a `scaffold`
  para verificación end-to-end; se elegirá una pantalla piloto en §tasks.
