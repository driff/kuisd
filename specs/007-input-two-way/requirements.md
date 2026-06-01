# Requisitos — Inputs two-way (`textField`, API state-based)

> Spec ID: 007 · Estado: approved · Fecha: 2026-06-01

## Resumen
Cerrar el lazo **two-way** del SDUI: añadir al `CorePack` un componente nuevo `textField` cuyo valor
**lee** una variable del cliente (vía el seam `LocalVariables` de la spec 005) y, al escribir el
usuario, **emite** `SetVar(name, JsonPrimitive(text))` por `LocalSduiActionHandler` (003). El
`VariableActionHandler` ya existente (005) recibe el `SetVar`, muta el `VariableStore` y la UI
recompone todos los `text`/`label` que muestren `$name`. Es la **primera fuente de entrada** del
SDUI: hasta ahora las variables solo se mutaban por botones (`Increment`/`Toggle`); con `textField`
las muta el propio usuario tecleando.

## Fuera de alcance
- **Validación** de inputs (regex, longitud mínima/máxima, requeridos), tipos numéricos
  (`NumericTextField`), parsing explícito (`ParseInt`/`SetVarTyped`), formato/máscaras.
- **Debounce/throttle**, blur-vs-keystroke (siempre emite en cada cambio), focus management,
  selección/cursor controlados desde el server.
- `FireEndpoint`/red, `SduiPatch`, persistencia de variables (sigue lo de 005: en memoria).
- Componentes de entrada adicionales (`switch`, `slider`, `checkbox`, `dropdown`, formularios
  compuestos `form`, etiqueta de error, ayuda contextual): se reservan para futuras specs.
- Aplicar `UiModifier` al `textField` (sigue ignorado como en 001/003/004/005).
- Sintaxis de binding compleja (interpolación multi-variable `@{x}` o varias `$a $b` en el mismo
  campo): explícitamente deferido (sigue lo de 005, una sola variable por campo string).
- Extracción a módulos Gradle adicionales (`:app-client`/`:app-contract`): no procede.

## Historias de usuario y criterios de aceptación

### HU-1 — `textField` es un componente del CorePack (motor puro, OCP)
**Como** integrador **quiero** que `textField` se registre como un componente más del `CorePack`
**para** demostrar que añadir un input no exige ramas especiales ni nuevos seams en el motor.

Criterios (EARS):
1. The engine SHALL registrar `textField` en `CorePack` con props tipadas
   `TextFieldProps(bind: String = "", placeholder: String = "", label: String? = null)`, de la
   misma forma que `text`/`button` (resolución por `ComponentRegistry`, sin tocar `RenderNode`).
2. The engine SHALL **no** introducir nuevos `CompositionLocal`, nuevas interfaces de handler ni
   nuevas acciones en `:sdui-core` para soportar `textField`; solo reutiliza `LocalVariables` (005)
   para leer y `LocalSduiActionHandler` (003) para emitir `SetVar` (que ya existe en el contrato).
3. The engine SHALL conservar la frontera de la 006: `:sdui-compose` sigue dependiendo solo de
   `:sdui-core` + Compose, sin importar `dev.kuisd.app` ni `io.ktor.*`.
4. WHEN `node.type == "textField"` y sus props no decodifican a `TextFieldProps`, the engine SHALL
   degradar a `UnknownNode` (regla 004), sin crash.

### HU-2 — Lectura inicial: el `textField` arranca con el valor actual de la variable enlazada
**Como** autor de pantallas **quiero** que el `textField` arranque con el valor actual de su
variable **para** que el input no aparezca vacío cuando ya hay estado sembrado.

Criterios (EARS):
1. WHEN se renderiza un `textField` con `bind = "<nombre>"` por primera vez, the engine SHALL leer
   el valor actual de la variable `<nombre>` por `LocalVariables.current.get(<nombre>)` y usarlo
   como **semilla** del `TextFieldState` (regla de display de 005: `JsonPrimitive.content`;
   objetos/array → `toString`; ausente → `""`).
2. AFTER la primera composición, the `TextFieldState` SHALL ser **la fuente de verdad** del texto
   mostrado. Mutaciones externas de la variable (p.ej. `Increment` desde un botón) actualizarán
   los `text` que leen `$<nombre>` pero **no** reescribirán el contenido del campo. Esta asimetría
   es deliberada: sigue el patrón state-based recomendado por Compose y evita cursor-jump /
   IME-glitches cuando hay mutadores concurrentes (ver §design para discusión).
3. The engine SHALL leer la variable **por nombre directo** (`bind = "count"`), **sin** el prefijo
   `$` (que es la convención de lectura de campos string de 005); un campo `bind` dedicado es más
   explícito en inputs y evita ambigüedad string-literal vs binding.

### HU-3 — Escritura: cada cambio del usuario emite `SetVar`
**Como** usuario **quiero** que al teclear se actualice la variable inmediatamente **para** que
otras partes de la pantalla que dependen de ella se actualicen sin red.

Criterios (EARS):
1. WHEN el usuario produce un cambio de valor en el `textField` (cada keystroke, sin debounce),
   the engine SHALL observar el `TextFieldState` por `snapshotFlow { state.text.toString() }` desde
   un `LaunchedEffect` y, para cada nuevo valor, llamar a
   `LocalSduiActionHandler.current.handle(listOf(SetVar(bind, JsonPrimitive(newText))))`.
2. The engine SHALL descartar la emisión inicial del `snapshotFlow` (vía `.drop(1)`) para no
   emitir un `SetVar` con la semilla en la primera composición.
3. The engine SHALL **no** mutar estado de variables por sí mismo: ninguna escritura ocurre fuera
   del `VariableStore` de la app; el motor solo despacha la acción. (El `TextFieldState` local del
   Composable es estado de UI, no estado de variables.)
4. WHEN la app recibe `SetVar(name, value)` por su `AppActionHandler` (005), the app SHALL
   delegarla al `VariableActionHandler`, que llama a `store.set(name, value)`; the system SHALL
   recomponer los nodos que leen `$name` (texto/label) sin pedir nada al server. El propio
   `textField` ya muestra el texto recién tecleado (lo posee él mismo) — no se reescribe.

### HU-4 — `bind` vacío = uncontrolled (input local sin two-way)
**Como** autor **quiero** poder colocar un `textField` puramente local **para** prototipar UIs sin
declarar una variable en el envelope.

Criterios (EARS):
1. WHEN `bind` es la cadena vacía (`""`), the engine SHALL renderizar un `textField` cuyo
   `TextFieldState` es local (`rememberTextFieldState()`) y **no** suscribirse al `snapshotFlow`,
   por lo que **no** emite `SetVar` al cambiar.
2. WHILE el `textField` es uncontrolled, the engine SHALL pintar `placeholder` (si se provee) como
   placeholder Material3 y respetar `label` si se provee (decoración pasiva, sin binding).
3. The system SHALL aceptar tanto `bind` ausente en las props (cae a default `""` por
   `TextFieldProps`) como `bind` presente con cadena vacía: ambos producen el modo uncontrolled.

### HU-5 — Coerción string-only (compromiso documentado)
**Como** integrador **quiero** un comportamiento determinista cuando el `textField` escribe sobre
una variable tipada (Int/Boolean) **para** entender el contrato sin sorpresas.

Criterios (EARS):
1. The engine SHALL emitir **siempre** `SetVar` con valor `JsonPrimitive(<String del campo>)`,
   sin parsear a Int/Boolean/Double; el campo es **string en la frontera**.
2. WHEN una variable enlazada estaba tipada (p.ej. `count: Int = 0` sembrada por el envelope), the
   `SetVar` SHALL sobrescribirla como `JsonPrimitive(String)`; the app SHALL conservar la
   resiliencia de 005: `Increment` posterior coerciona por `intOrNull ?: 0` (clamp), `Toggle` por
   `booleanOrNull ?: false`.
3. The engine SHALL exponer la coerción tipada (acciones `ParseInt`/`SetVarTyped`) como **trabajo
   futuro fuera de alcance**; ver §design para la nota de evolución.

### HU-6 — Resiliencia (variable ausente, props inválidas, sin crash)
**Como** integrador **quiero** que un binding roto o props mal formadas no crasheen **para**
tolerar payloads imperfectos como en 004/005.

Criterios (EARS):
1. IF `bind` referencia una variable **ausente** en el store, THEN the engine SHALL renderizar el
   `textField` con valor inicial `""` y, al primer `SetVar`, the app SHALL **crear** la variable
   con el valor recibido (comportamiento existente de `VariableStore.set` en 005).
2. IF la variable enlazada existe con un tipo no textual (objeto/array JSON), THEN the engine
   SHALL degradar a su forma textual segura (`asDisplayString`, regla 005) como semilla del
   `TextFieldState`; al editar, el `SetVar` la sobrescribe como `JsonPrimitive(String)`.
3. IF las props no decodifican a `TextFieldProps`, THEN the engine SHALL caer a `UnknownNode`
   (HU-1.4) sin crash.
4. The engine SHALL **no** crashear si `LocalSduiActionHandler` es el handler por defecto (no-op +
   log, fuera de un host): el `SetVar` se loguea como acción no manejada y el campo conserva su
   texto en pantalla (el motor no diferencia este caso del éxito; solo despacha).
5. The app SHALL hacer `VariableStore.seed` **idempotente** (merge no-overwrite): si una variable
   ya existe en el store, el `seed` la preserva. Defensa contra re-emisión del envelope sobre la
   misma entrada del back stack — protege los `text` con `$bind` de rebotar al valor inicial
   mientras el usuario teclea sobre la misma variable.

### HU-7 — Demo end-to-end servida por el server
**Como** demo **quiero** una pantalla que combine `textField` + `text` enlazado **para** validar
el ciclo two-way de punta a punta.

Criterios (EARS):
1. The server SHALL **extender** la pantalla `counter` (005) añadiendo un `textField` enlazado a
   `count` y, opcionalmente, un par `textField`+`text` para una variable `name` con un saludo
   (`text` literal `"Hola, "` + `text` con `$name`); ver §design para la decisión final.
2. WHEN el usuario teclea "7" en el `textField` enlazado a `count`, the system SHALL mostrar `7`
   en el `text` con `"$count"` sin nueva petición al server.
3. WHEN el usuario pulsa `+1` tras teclear `"7"`, the app SHALL coercionar `"7"` a `7` y emitir
   `Increment` (clamp `[0, 10]`), mostrando `8`; if el campo contiene `"abc"`, the app SHALL
   tratarlo como `0` y mostrar `1` tras el `+1` (HU-5.2 / 005 coerción).
4. The server SHALL mantener el envelope retrocompatible con 005 (`variables.count == 0`,
   `variables.flag == false`); the demo SHALL servirse en la ruta existente `GET /screen/counter`
   con respuesta `200`.

## Requisitos no funcionales
- **Clean Architecture / OCP (003+004+005+006):** añadir `textField` toca **solo** `CorePack.kt`
  del motor (más sus props) y la pantalla del server. En la app, único cambio mínimo: hacer
  `VariableStore.seed` idempotente (HU-6.5) — encaja en el módulo `variables` ya existente y no
  abre el motor.
- **Motor puro:** `:sdui-compose` no gana mutables propios de variables (el `TextFieldState` es
  estado de UI local de un Composable, conceptualmente equivalente al `state` de un `Checkbox`);
  no importa `dev.kuisd.app` ni Ktor; no hay nuevas acciones en `:sdui-core` (`SetVar` ya existe).
- **Sin delays ni problemas de coroutines:** el ciclo keystroke→SetVar→store→display recompone en
  el mismo frame de Compose; `snapshotFlow` se colecta en el `CoroutineScope` del `LaunchedEffect`
  (dispatcher Main del runtime de Compose), sin cambios de hilo. `.drop(1)` evita un `SetVar`
  redundante en la primera composición. El `TextFieldState` posee selección/IME → cero
  cursor-jump.
- **Resiliencia:** binding roto / variable ausente / props inválidas / handler default no crashean;
  re-emisión del envelope no pisa texto en vuelo (HU-6.5).
- **Rendimiento:** recomposición localizada — los `text` con `$name` se recomponen por
  snapshot-state (005). El campo se recompone solo por sus propios edits (no por mutaciones
  externas de la variable). Sin debounce.
- **Multiplataforma:** todo en `commonMain` de `:sdui-compose`; `OutlinedTextField(state = ...)`
  + `rememberTextFieldState` están disponibles en los tres targets a través de `api(material3)`
  y `api(foundation)`.
- **API mínima:** `TextFieldProps` es la única superficie pública nueva del motor.
- `detekt` / `ktlintCheck` en verde.

## Dependencias y supuestos
- Depende de **003** (frontera motor/app, `SduiActionHandler`/`LocalSduiActionHandler`), **004**
  (`ComponentRegistry`/`RenderScope`, registrar componente sin tocar el motor), **005**
  (`VariableScope`/`LocalVariables`, `VariableStore`, `VariableActionHandler`, `AppActionHandler`
  compuesto con `SubHandler.supports`) y **006** (motor en `:sdui-compose`, frontera por
  classpath). Todas mergeadas en `main`.
- Reutiliza el contrato `:sdui-core` **sin cambios**: `SetVar(name, value: JsonElement)` ya existe.
- Reutiliza Material3 (`OutlinedTextField`) ya disponible en `:sdui-compose` (api(material3)).
- No introduce dependencias nuevas en `gradle/libs.versions.toml`.
- Supone que la implementación NO añade debounce: si se prueba que recompone demasiado en futuro,
  se evaluará una spec posterior (fuera de alcance aquí).
