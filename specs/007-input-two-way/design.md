# Diseño — Inputs two-way (`textField`, API state-based)

> Spec ID: 007 · Estado: approved · Trazabilidad: ./requirements.md · Fecha: 2026-06-01

## Enfoque
Añadir `textField` como un componente más del `CorePack`, usando la **API state-based** recomendada
por Compose (`rememberTextFieldState` + `TextField(state = ...)`,
[guía oficial](https://developer.android.com/develop/ui/compose/text/migrate-state-based#recommended-simple)).
El estado del campo lo posee el propio Composable (no se controla por `value`/`onValueChange`); la
emisión hacia el `LocalSduiActionHandler` se hace observando `state.text` por `snapshotFlow` desde
un `LaunchedEffect`.

Beneficios respecto al patrón `value`/`onValueChange`:
1. **Sin riesgo de cursor jump.** El `TextFieldState` posee texto **y** selección; ninguna
   sincronización externa pisa la posición del caret.
2. **Sin feedback loop síncrono.** Cada keystroke pasa por snapshot-state local; el `SetVar` se
   emite en la misma frame pero sin un round-trip `var → value` recompuesto.
3. **Protección contra re-seed del envelope.** Si el envelope se re-emite (futuro polling/retry),
   el texto en vuelo del usuario se conserva (el estado del campo es independiente del store).

Trade-off documentado: el campo deja de ser un *espejo* del store (HU-2.2 cambia su semántica). La
variable es **fuente al sembrar** la pantalla; a partir de ahí, **el campo es la fuente** y
mutadores externos (p.ej. `Increment` mientras el usuario edita la misma var) actualizan los
`text` que leen `$name` pero **no** reescriben el campo. Es la recomendación del docs de Android y
mitiga la P1 (re-seed) y P2 (cursor jump) del audit sin coste.

Defensa adicional: `VariableStore.seed` se vuelve **idempotente** (no sobrescribe variables ya
fijadas). Así, aunque el `LaunchedEffect(envelope)` se dispare por re-emisión, los `text` que leen
`$count` tampoco rebotan al valor sembrado mientras el usuario teclea.

## Arquitectura (módulos afectados)
```
:sdui-core           contrato @Serializable               (SIN CAMBIOS — SetVar ya existe)
   ▲
:sdui-compose        MOTOR (Compose)                      (un solo archivo: CorePack.kt)
   ▲                  └─ + TextFieldProps + renderer textField (state-based)
:shared              APP (composición, nav, vars, host)   (cambio menor en VariableStore)
                     └─ VariableStore.seed: ahora idempotente (no overwrite)
server/...           screens                               (extiende CounterScreen)
                     └─ añade un nodo textField al árbol
```
La frontera de la 006 (`:sdui-compose` no importa `dev.kuisd.app` ni Ktor) se preserva por
classpath: el `textField` solo usa tipos de `:sdui-core` + Compose Material3 + coroutines (ya
transitivos por `api(runtime)`/`api(foundation)`).

## Mapa de archivos
```
sdui-compose/src/commonMain/kotlin/dev/kuisd/sdui/
└── CorePack.kt                       (cambia)  + TextFieldProps + register("textField", ...) state-based

shared/src/commonMain/kotlin/dev/kuisd/app/variables/
└── VariableStore.kt                  (cambia)  seed: idempotente (no overwrite — defensa P1)

shared/src/commonTest/kotlin/dev/kuisd/app/variables/
└── VariableStoreTest.kt              (cambia)  ajusta el test de seed + añade test de no-overwrite

server/src/main/kotlin/dev/kuisd/server/screens/
└── CounterScreen.kt                  (cambia)  añade textField(bind="count") + saludo $name

sdui-compose/src/commonTest/kotlin/dev/kuisd/sdui/
└── TextFieldPropsTest.kt             (NUEVO)   decodificación de props

shared/src/commonTest/kotlin/dev/kuisd/app/variables/
└── TextFieldTwoWayFlowTest.kt        (NUEVO)   flujo two-way sin UI

server/src/test/kotlin/dev/kuisd/server/
└── ApplicationTest.kt                (cambia)  test que el envelope counter trae textField
```

## Componentes y contratos

### MOTOR · `TextFieldProps` + renderer `textField` — HU-1, HU-2, HU-3, HU-4, HU-6
```kotlin
// dev.kuisd.sdui  (public) — CorePack.kt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import dev.kuisd.sdui.core.SetVar
import dev.kuisd.sdui.core.sduiComponent
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/**
 * Props del input two-way (state-based).
 *  - `bind`: nombre **directo** de la variable (sin `$`); cadena vacía => uncontrolled.
 *  - `placeholder`: texto Material3 mostrado cuando no hay valor.
 *  - `label`: etiqueta opcional; resuelta por `bind(label)` (convención 005: admite `$ref`).
 */
@Serializable
data class TextFieldProps(
    val bind: String = "",
    val placeholder: String = "",
    val label: String? = null,
)

/** Registrado dentro de `CorePack = componentRegistry { ... }` (mismo archivo). */
@OptIn(ExperimentalMaterial3Api::class)
register(sduiComponent<TextFieldProps>("textField")) { p ->
    val handler = LocalSduiActionHandler.current
    val vars = LocalVariables.current
    // Semilla puntual: se lee 1 sola vez (en la 1ª composición) por estar dentro de remember(...).
    // Si la variable cambia externamente después, el campo NO se reescribe (es local-source-of-truth).
    val initial = vars.get(p.bind)?.asDisplayString().orEmpty()
    val state = rememberTextFieldState(initial)

    if (p.bind.isNotEmpty()) {
        LaunchedEffect(state, p.bind) {
            // .drop(1) descarta la emisión inicial (estado actual del campo) para no emitir un
            // SetVar idempotente con la semilla en la 1ª composición.
            snapshotFlow { state.text.toString() }
                .drop(1)
                .collect { newText ->
                    handler.handle(listOf(SetVar(p.bind, JsonPrimitive(newText))))
                }
        }
    }

    OutlinedTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(p.placeholder) },
        label = p.label?.let { { Text(bind(it)) } },
    )
}
```

#### Decisiones / alternativas descartadas
- **API state-based vs `value`/`onValueChange`.** Se sigue el patrón
  *recomendado simple* del docs Android (referencia arriba). El estado del campo lo posee el
  Composable (`TextFieldState`); el sync hacia el store es vía `snapshotFlow` colectado en un
  `LaunchedEffect`. El patrón anterior (`value = vars.get(...)`, `onValueChange = SetVar`) queda
  descartado porque (a) re-genera el `value` en cada recomposición y reposiciona el caret cuando
  store y campo se desincronizan por un frame; (b) hace al campo dependiente de re-seeds del
  envelope; (c) requiere coerción de tipos en cada read para presentar como String.
- **`.drop(1)` en la suscripción.** `snapshotFlow` emite el valor actual al colectar; sin
  `drop(1)` se emitiría un `SetVar(p.bind, JsonPrimitive(initial))` inocuo en cada composición
  fresca. Drop=1 lo elimina sin afectar al primer keystroke real.
- **Campo `bind` dedicado vs prefijo `$nombre`.** Igual que en la versión previa: nombre directo
  (`bind = "count"`, sin `$`). Razones de explícitud, asimetría justificada con 005 (en `text` el
  campo es display libre y `$` lo convierte en binding; en `textField` el `bind` es una referencia
  obligatoria), simplicidad sintáctica para el server, reversibilidad por alias futuro.
- **Uncontrolled (`bind = ""`).** Trivial con el nuevo patrón: si `bind` es vacío, se omite la
  suscripción. El `state` sigue siendo un `rememberTextFieldState()` puro; el campo funciona
  localmente sin emitir. Cero coste.
- **No sincronizar var → field tras la 1ª composición (HU-2.2 ajustada).** Es la recomendación
  del docs; evita cursor-jump y desync visible si hay mutadores concurrentes. Si una futura spec
  necesita "actualización externa con preservación de selección" (p.ej. para reset por server),
  se introduce con `state.edit { ... }` gated por foco; queda fuera de alcance del MVP.
- **`label: String?` opcional.** Pasa por `bind(label)` (regla 005), admite `"$greeting"` y
  literales con escape `$$`.
- **`OutlinedTextField` Material3 con `state =`.** Disponible en CMP 1.11 + Material3 1.9 con
  opt-in `@ExperimentalMaterial3Api`. Sin nuevas deps.
- **Sin debounce.** Cada keystroke emite `SetVar`. Simple y suficiente para el MVP.

### MOTOR · sin cambios fuera de `CorePack.kt`
- `RenderScope.bind` (005), `LocalVariables` (005), `LocalSduiActionHandler` (003),
  `ComponentRegistry`/`RenderNode` (004), `Logging` — **intactos**.
- `asDisplayString` (`internal`, 005) se usa solo en la semilla inicial.

### APP · `VariableStore.seed` se vuelve idempotente — HU-6.5 (nuevo)
**Cambio mínimo en `:shared`:** la única razón es proteger los `text` que leen `$bind` de un
re-seed por re-emisión del envelope mientras el usuario teclea sobre la misma variable. Sin esto,
los displays parpadearían al valor inicial aunque el campo (state-based) mantenga su texto.

```kotlin
// VariableStore.kt — cambia
fun seed(initial: Map<String, JsonElement>) {
    val merged = vars.toMutableMap()
    initial.forEach { (k, v) -> merged.putIfAbsent(k, v) }
    vars = merged.toMap()
}
```
- Primera siembra (mapa vacío): equivalente al comportamiento anterior — todas las claves entran.
- Re-seed con claves ya presentes: **no-op por clave** (mantiene el valor actual).
- Re-seed con claves nuevas: se añaden.

Coste: O(n) por seed; n = nº de variables del envelope (típicamente <20). Indistinguible.

### APP · sin cambios sustanciales en handlers
El flujo two-way encaja sin modificación adicional:
```
state.text cambia (keystroke)
  └─> snapshotFlow emite "<nuevo texto>"
      └─> LaunchedEffect.collect:
          LocalSduiActionHandler.handle(SetVar(p.bind, JsonPrimitive("<nuevo texto>")))
              └─> AppActionHandler (005)
                  └─> VariableActionHandler.supports(SetVar) == true
                      └─> store.set(p.bind, JsonPrimitive("<nuevo texto>"))
                          └─> recompone los `text` que leen `$bind` (snapshot-state)
                              (el textField NO se actualiza por esto — su state es local)
```

### SERVER · `CounterScreen` (extendida) — HU-7
Se amplía la pantalla `counter` existente añadiendo:
1. Variable `name` inicial `""`.
2. Un `textField(bind = "count")` con placeholder `"Escribe un número"`.
3. Un `textField(bind = "name")` con placeholder `"Tu nombre"`.
4. Un saludo `row` con `text` literal `"Hola, "` + `text` con `"$name"`.

```kotlin
override suspend fun build(ctx: ScreenContext): SduiEnvelope = SduiEnvelope(
    schemaVersion = SCHEMA_VERSION,
    screenId = "counter",
    variables = mapOf(
        "count" to JsonPrimitive(0),
        "flag"  to JsonPrimitive(false),
        "name"  to JsonPrimitive(""),
    ),
    root = SduiNode(
        type = "column", id = "root",
        modifier = UiModifier(fillMaxWidth = true, padding = PaddingTokens(l = Tokens.Space.Md, t = Tokens.Space.Md)),
        children = listOf(
            titleNode(),
            bindingTextNode("count-value", "\$count"),
            bindingTextNode("flag-value",  "\$flag"),
            textFieldNode("count-input", bind = "count", placeholder = "Escribe un número"),
            textFieldNode("name-input",  bind = "name",  placeholder = "Tu nombre"),
            greetingRow(),
            buttonNode("inc",    "+1",     Increment("count", by = 1,  min = MIN_COUNT, max = MAX_COUNT)),
            buttonNode("dec",    "-1",     Increment("count", by = -1, min = MIN_COUNT, max = MAX_COUNT)),
            buttonNode("toggle", "Toggle", Toggle("flag")),
            buttonNode("back",   "Atrás",  NavigateBack),
        ),
    ),
)
```

## Modelo de datos y estados
- Contrato `:sdui-core` **sin cambios** (`SetVar(name, value: JsonElement)` ya existe).
- `TextFieldProps(bind, placeholder, label)`: nuevo tipo serializable público en el motor.
- Estado del campo: `TextFieldState` (Compose foundation; almacena `text: CharSequence` + selección
  + edit history). Vida = la del Composable (la entrada del back stack es estable por
  `key(current.id)` en el host, spec 003).
- Estado del store (`VariableStore.vars: Map<String, JsonElement>`): API pública sin cambios; la
  semántica de `seed` cambia a "merge no-overwrite" (HU-6.5).
- **Coerción string-only:** `SetVar` siempre lleva `JsonPrimitive(String)`. Si la variable era Int,
  queda como `JsonPrimitive("7")`; `Increment` posterior la rehidrata por `intOrNull ?: 0` + clamp.

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| (ninguna) | — | — | `OutlinedTextField(state = ...)`, `rememberTextFieldState`, `snapshotFlow`, `Flow.drop` ya están disponibles vía `api(material3)`/`api(foundation)`/`api(runtime)` (que arrastra `kotlinx-coroutines`). |

## Riesgos y mitigaciones
- **Desync visible entre campo y `$bind` ante mutadores concurrentes.** Si el usuario está
  editando `bind = "count"` y a la vez se ejecuta `Increment("count")`, el `text` con `$count`
  refleja el `Increment` pero el campo conserva el texto local. **Aceptado como MVP.** Es el
  comportamiento recomendado para evitar cursor-jump; si surge un caso de uso real (formularios
  con reset desde el server), se introduce una spec con `state.edit { ... }` gated por foco.
- **Coerción string-only ofuscada.** Un autor puede asumir que escribir "abc" sobre `count`
  "funcionará". Documentado en HU-5; `Increment` posterior coerciona a 0. Evolución futura:
  `ParseInt`/`SetVarTyped`.
- **`seed` idempotente puede sorprender en hot-reload de pantalla.** Si la entrada del back stack
  cambia de `id` (nueva instancia de la pantalla), `SduiHost` ya crea un `VariableStore` nuevo via
  `remember(current.id)` (spec 005), así que el `seed` siempre se aplica sobre un mapa vacío y se
  comporta como antes. Solo cambia el caso **re-emisión del envelope dentro de la misma entrada**,
  que hoy no ocurre y antes habría sido un bug latente.
- **`LaunchedEffect` con `state` como key.** `state` se recrea solo si la composición se desecha;
  si se reorganiza la jerarquía y Compose mantiene el mismo `state`, el effect persiste. La
  inclusión de `p.bind` como key extra protege ante cambios del binding sin recrear el state.
- **Variable inexistente.** Primer `SetVar` la crea (`VariableStore.set` la añade al mapa). Los
  nodos que leen `$<bind>` empezarán mostrando `""` hasta el primer keystroke (esperado).
- **`label` interpretado como literal vs binding.** Mitigación: `label` pasa por `bind(label)`
  (regla 005); `label = "$greeting"` resuelve la variable, literal con escape `$$`.
- **Handler default no-op.** Si el `LocalSduiActionHandler` es el default (fuera de un host), los
  `SetVar` se loguean como acción no manejada y el campo conserva su texto en pantalla. El motor
  no diferencia este caso del éxito — solo despacha.

## Estrategia de verificación
- **Unit `:sdui-compose:commonTest` (motor — sin UI):**
  - `TextFieldPropsTest`: `{"bind":"count","placeholder":"x","label":"L"}` →
    `TextFieldProps("count","x","L")`; `{}` → defaults (`""`, `""`, `null`); props inválidas
    (p.ej. `{"bind":42}`) fallan la decodificación (camino `UnknownNode` ya cubierto por 004).
- **Unit `:shared:commonTest` (app — sin UI):**
  - `VariableStoreTest`: existente `seed_replaces_the_whole_map` **se ajusta** a la nueva semántica
    (`seed` ya no reemplaza); test nuevo `seed_preserves_existing_values` (no-overwrite) y
    `seed_adds_missing_keys`.
  - `TextFieldTwoWayFlowTest`: dado un store con `count = 0`, simular un keystroke vía
    `AppActionHandler.handle(listOf(SetVar("count", JsonPrimitive("7"))))` → `store.vars["count"]
    == JsonPrimitive("7")`. Variante: `SetVar("name", JsonPrimitive("Ana"))` sobre store vacío →
    crea la variable. Variante: tras `SetVar("count","7")`, `Increment("count", by=1)` produce
    `JsonPrimitive(8)` (coerción `intOrNull` de 005).
- **Unit `:server:test`:** `GET /screen/counter` → `200`; el envelope contiene un nodo
  `type=="textField"` con `props.bind=="count"`; `variables.name == ""` y un `text` con
  `props.text == "$name"`.
- **Regla de dependencias:** `grep -rn "dev.kuisd.app\|io.ktor\." sdui-compose/src/` sigue vacío;
  el motor importa solo `dev.kuisd.sdui.core.*`, `androidx.compose.*` y `kotlinx.coroutines.flow.drop`
  (transitivo por `runtime`).
- **Smoke manual** (`:server:run` + `:desktopApp:run`):
  - En `counter`, teclear `7` en `count-input` → el `text` `$count` muestra `7`; pulsar `+1` →
    `8`; cinco veces más → clamp en `10`. **Verificación nueva:** el campo `count-input`
    conserva `"7"` aunque el display muestre `10` (no es espejo; HU-2.2 ajustada).
  - Borrar y teclear `abc` → texto del campo es `abc`; `+1` → display `1` (coerción).
  - Teclear `Ana` en `name-input` → saludo "Hola, Ana".
  - Atrás → Adelante a `counter`: nueva entrada de back stack ⇒ store nuevo (spec 003) ⇒ campos
    vuelven a `""`/`0`.
- **Calidad:** `./gradlew :sdui-core:check :sdui-compose:check :shared:assemble :shared:check
  :server:build :androidApp:assembleDebug detekt ktlintCheck` (tras `ktlintFormat`) — en verde.

## public / internal
- **public (motor):** `TextFieldProps`. El renderer se registra dentro de `CorePack` y no expone
  superficie nueva.
- **internal:** sin nuevos símbolos. `asDisplayString` sigue `internal` (005).
- **app/server:** sin cambios de visibilidad.
