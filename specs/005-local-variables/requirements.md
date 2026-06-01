# Requisitos — Variables locales reactivas (estrategia "variables locales")

> Spec ID: 005 · Estado: approved · Fecha: 2026-06-01

## Resumen
Dar **interactividad sin round-trip** al cliente SDUI: un nodo puede **vincular** su contenido a una
variable, y las acciones `SetVar`/`Toggle`/`Increment` (que el contrato ya define y el server ya puede
emitir) **mutan** esas variables en el cliente, recomponiendo solo la UI afectada. El server transporta
los **valores iniciales** en `SduiEnvelope.variables`. Respetando la frontera de la spec 003, el
**estado vive en la app** (`dev.kuisd.app`): un `VariableStore` reactivo (snapshot-state) y un handler que
interpreta las tres acciones. El **motor** (`dev.kuisd.sdui`) **no posee ni muta** estado: solo **lee** los
valores de variables a través de un **seam** (`LocalVariables`, un `CompositionLocal` que la app provee),
análogo a `LocalSduiActionHandler`/`LocalComponentRegistry`. Demo: una pantalla `counter` con un `text`
vinculado a una variable y botones con `Increment`/`Toggle`.

## Fuera de alcance
- `FireEndpoint`/red, `SduiPatch`/patches dirigidos, **persistencia** de variables (se pierden al recargar).
- Binding **two-way** (`bind` de inputs) y componentes de entrada (`textField`, `switch`…): esta spec
  cubre solo **display one-way** (un nodo lee una variable).
- Binding complejo / expresiones: nada de aritmética, formateo, condicionales ni interpolación de varias
  variables en un mismo string. Una referencia resuelve **una** variable (ver §design para la forma exacta).
- **Extracción a módulos Gradle** (`:sdui-compose`, `:app-client`): es la **spec 006**, en paralelo. Esta
  spec mantiene la frontera **por paquetes** dentro de `:shared`, igual que 003/004.
- Nuevos componentes del catálogo (`tabs`, `state`, `dialog`, `bottomSheet`…): solo se demuestra el binding
  sobre los componentes existentes del `CorePack` (`text`/`button`).
- Aplicar `UiModifier` (sigue ignorado como en 001/003/004).

## Historias de usuario y criterios de aceptación

### HU-1 — El motor lee variables por un seam, sin poseer estado (frontera 003/004)
**Como** integrador **quiero** que el motor resuelva bindings leyendo un seam **para** mantenerlo puro y
reutilizable, sin que posea ni mute estado.

Criterios (EARS):
1. The engine SHALL exponer un seam de **solo lectura** de variables (`VariableScope` + `LocalVariables`,
   `CompositionLocal`, default vacío) análogo a `LocalSduiActionHandler`/`LocalComponentRegistry`.
2. The engine SHALL depender **solo** de `:sdui-core` + Compose; SHALL NOT importar Ktor ni `dev.kuisd.app`,
   y SHALL NOT contener `MutableState`/`StateFlow` de variables ni interpretar `SetVar`/`Toggle`/`Increment`.
3. WHEN un renderer resuelve un binding, the engine SHALL leer el valor actual contra `LocalVariables.current`
   y recomponer **solo** ese nodo cuando la variable cambie (lectura de snapshot-state observada).
4. The engine SHALL exponer una API mínima nueva (`VariableScope`, `LocalVariables`, y el helper de binding
   en `RenderScope`); el resto permanece `internal`.

### HU-2 — Binding de un nodo a una variable (display one-way)
**Como** autor de pantallas **quiero** que un `text` muestre el valor de una variable **para** reflejar
cambios locales al instante.

Criterios (EARS):
1. The system SHALL definir una **convención de binding forward-compatible** en las props de un nodo para
   referenciar una variable (ver §design: prefijo `$nombre` en el campo string; p.ej. `{"text":"$count"}`).
2. WHEN un campo bindable contiene una referencia a variable, the engine SHALL renderizar el **valor actual**
   de esa variable (convertido a su representación textual); WHEN el campo es un literal (sin referencia),
   the engine SHALL renderizarlo tal cual (retrocompatible con 004).
3. WHILE la variable referenciada cambia de valor, the engine SHALL recomponer el nodo vinculado para
   mostrar el nuevo valor, sin recargar la pantalla.

### HU-3 — La app posee el `VariableStore` y muta vía acciones
**Como** app **quiero** un store reactivo sembrado por el envelope **para** mutar variables sin red.

Criterios (EARS):
1. The app SHALL definir `VariableStore` (snapshot-state) **sembrado** con `SduiEnvelope.variables` al
   cargar la pantalla, y exponer una vista de **solo lectura** que el motor consume vía el seam de HU-1.
2. WHEN se delega una acción `SetVar(name,value)`, the app SHALL fijar `name` al `value` dado.
3. WHEN se delega una acción `Toggle(name)`, the app SHALL invertir el valor booleano de `name`
   (interpretando ausente/no-booleano como `false` → pasa a `true`).
4. WHEN se delega una acción `Increment(name, by, min, max)`, the app SHALL sumar `by` al valor entero de
   `name` (ausente/no-entero se trata como `0`).
5. WHILE existen `min`/`max` en un `Increment`, the app SHALL **acotar** el resultado a `[min, max]`
   (clamp), sin lanzar ni desbordar.

### HU-4 — Composición de handlers (nav + variables) sin romper SRP
**Como** app **quiero** que un solo `SduiActionHandler` atienda navegación **y** variables **para** que el
host provea un único seam de salida al motor.

Criterios (EARS):
1. The app SHALL componer la navegación (`NavActionHandler`, spec 003) y la mutación de variables
   (`VariableActionHandler`) en un handler único (`AppActionHandler`) que el `SduiHost` provee por
   `LocalSduiActionHandler`.
2. WHEN llega una lista de acciones mezcladas, the app SHALL despachar **cada** acción al sub-handler
   responsable (navegación a nav-store; variables a variable-store) preservando el orden.
3. IF una acción no la maneja ningún sub-handler, THEN the app SHALL hacer no-op + log (sin crash),
   manteniendo el comportamiento de 003.

### HU-5 — Resiliencia (variable ausente, tipos, sin estado en el motor)
**Como** integrador **quiero** que un binding roto nunca crashee **para** tolerar payloads imperfectos.

Criterios (EARS):
1. IF un binding referencia una variable **ausente**, THEN the engine SHALL renderizar un **fallback**
   (string vacío) sin crash y sin propagar excepción.
2. IF el valor de la variable no es un tipo textual directo (p.ej. objeto/array JSON), THEN the engine
   SHALL degradar a su forma textual segura (o al fallback) sin crash.
3. IF `Increment`/`Toggle` operan sobre una variable de tipo incompatible, THEN the app SHALL aplicar la
   regla de coerción de HU-3 (tratar como `0`/`false`) sin crash.
4. The engine SHALL seguir mostrando `UnknownNode` para `type` desconocido y delegando acciones por
   `LocalSduiActionHandler` (specs 003/004 intactas).

### HU-6 — Demo end-to-end servida por el server
**Como** demo **quiero** una pantalla con contador/toggle **para** validar la interactividad sin round-trip.

Criterios (EARS):
1. The server SHALL emitir una pantalla `counter` cuyo `SduiEnvelope.variables` siembre `count` (entero) y
   un flag booleano, con un `text` vinculado al `count` y botones que emiten `Increment(count, …, min, max)`
   y `Toggle(flag)`.
2. WHEN el usuario pulsa el botón de incremento, the system SHALL aumentar el texto mostrado en el cliente
   **sin** nueva petición al server; al alcanzar `max`, SHALL dejar de crecer (clamp).
3. The server SHALL registrar `counter` en `defaultScreenRegistry()` y servir `GET /screen/counter` → 200.

## Requisitos no funcionales
- **Frontera Clean Architecture (spec 003) intacta:** el motor no importa Ktor ni `dev.kuisd.app` y **no
  posee estado mutable** de variables; la app es la única dueña del `VariableStore` y de la interpretación
  de `SetVar`/`Toggle`/`Increment`. Verificable por `grep` de imports y por ausencia de mutables en el motor.
- **OCP (spec 004) intacta:** el binding se resuelve en los renderers vía el `RenderScope`/seam; añadir el
  binding no reintroduce un `when` en el motor.
- **Resiliencia:** binding a variable ausente / tipo raro y acciones sobre tipos incompatibles nunca crashean.
- **Rendimiento:** recomposición **localizada** al nodo que lee la variable (no recarga de pantalla).
- **Multiplataforma:** todo en `commonMain`; sin APIs específicas de plataforma.
- API pública mínima; `detekt`/`ktlintCheck` en verde.

## Dependencias y supuestos
- Depende de **001** (slice), **002** (server), **003** (frontera motor/app, `SduiActionHandler`,
  `NavActionHandler`, `NavBackStack`, `SduiHost`) y **004** (`ComponentRegistry`/`RenderScope`/`CorePack`,
  props tipadas), todas mergeadas en `main`.
- Reutiliza el contrato `:sdui-core` **sin cambios**: `SetVar`/`Toggle`/`Increment` y
  `SduiEnvelope.variables` ya existen.
- No introduce dependencias nuevas (snapshot-state de Compose ya disponible).
- Supone que `SduiScreen` debe **propagar** `envelope.variables` al store de la pantalla (hoy descarta todo
  salvo `envelope.root`): es un punto de cableado de esta spec.
