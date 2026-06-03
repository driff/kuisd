# Diseño — Overlays (`dialog` · `bottomSheet` · `snackbar`)

> Spec ID: 011 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Overlays **imperativos**: cuatro `UiAction` nuevas en `:sdui-core` (`ShowDialog`, `ShowBottomSheet`,
`ShowSnackbar`, `DismissOverlay`) y, en `:shared`, un estado de overlay propiedad del `SduiHost` + un
`OverlayActionHandler` (`SubHandler`, patrón 009) que las traduce en mutaciones de ese estado. El host
renderiza el overlay activo **por encima** de la pantalla (reutilizando `RenderNode` para el contenido
de la hoja) y aloja el `SnackbarHost` en su propio `Scaffold`. **`:sdui-compose` no se toca** — la
frontera del motor se mantiene: el motor solo despacha la acción por `LocalSduiActionHandler`; el estado
y el render del overlay son de la app, como navegación (003) y red (009). La lógica no-Compose
(resolución de `messageVar`, mapeo de `duration`, transiciones de estado) se extrae a funciones/clases
puras testeables sin UI.

## Arquitectura
- **`:sdui-core`** (`commonMain`): añadir las 4 acciones al `sealed interface UiAction` (`UiAction.kt`).
  `ShowBottomSheet.content` es `List<SduiNode>` (subárbol serializado, análogo a `FireEndpoint.onSuccess`).
- **`:shared`** (`commonMain`):
  - `OverlayController.kt` (nuevo) — estado reactivo (`dialog`/`sheet` como `mutableStateOf`) + helpers.
  - `OverlayActionHandler.kt` (nuevo) — `SubHandler`; muta el controller y dispara el snackbar.
  - `SduiHost.kt` (editar) — crea controller + `SnackbarHostState` por entrada del back stack, añade el
    handler al `AppActionHandler`, y renderiza `OverlayHost(...)` dentro del `CompositionLocalProvider`.
- **`:server`** (editar, solo demo) — pantalla piloto que lanza `ShowDialog`/`ShowBottomSheet`/`ShowSnackbar`.
- Reset-on-nav (HU-4.3): controller y `SnackbarHostState` se crean **dentro** del bloque
  `key(current.id)` del host ⇒ una entrada nueva del back stack obtiene estado fresco (overlay anterior
  desaparece) sin lógica explícita.

```
ShowX action ─► AppActionHandler ─► OverlayActionHandler.handle
                                      ├─ ShowDialog       → controller.showDialog(spec)
                                      ├─ ShowBottomSheet  → controller.showSheet(spec)
                                      ├─ DismissOverlay   → controller.dismissAll()
                                      └─ ShowSnackbar     → scope.launch { snackbar.showSnackbar(...) ; si ActionPerformed → dispatch(onAction) }
SduiHost (dentro de key + CompositionLocalProvider):
  Box { SduiScreen ; SnackbarHost(state) ; controller.dialog?→AlertDialog ; controller.sheet?→ModalBottomSheet{ content.forEach{RenderNode} } }
```

## Componentes y contratos

### Acciones (`:sdui-core/UiAction.kt`)
```kotlin
@Serializable @SerialName("showDialog")
data class ShowDialog(
    val title: String = "",
    val text: String = "",
    val confirmLabel: String? = null,
    val onConfirm: List<UiAction> = emptyList(),
    val dismissLabel: String? = null,
    val onDismiss: List<UiAction> = emptyList(),
) : UiAction

@Serializable @SerialName("showBottomSheet")
data class ShowBottomSheet(
    val content: List<SduiNode> = emptyList(),
    val onDismiss: List<UiAction> = emptyList(),
) : UiAction

@Serializable @SerialName("showSnackbar")
data class ShowSnackbar(
    val message: String = "",
    val messageVar: String? = null,
    val actionLabel: String? = null,
    val onAction: List<UiAction> = emptyList(),
    val duration: String = "short",          // "short" | "long" | "indefinite"
) : UiAction

@Serializable @SerialName("dismissOverlay")
data object DismissOverlay : UiAction
```
- **Decisión:** título/texto del diálogo son **literales** (el server compone el texto final); el caso
  dinámico se cubre por `ShowSnackbar.messageVar`. Evita que el host (no-`RenderScope`) resuelva `$bind`.

### `OverlayController` (`:shared`, puro/estado)
```kotlin
@Stable
internal class OverlayController {
    var dialog by mutableStateOf<ShowDialog?>(null); private set
    var sheet  by mutableStateOf<ShowBottomSheet?>(null); private set
    fun showDialog(a: ShowDialog) { sheet = null; dialog = a }   // un overlay a la vez (HU-1.6/2.5)
    fun showSheet(a: ShowBottomSheet) { dialog = null; sheet = a }
    fun dismissAll() { dialog = null; sheet = null }
}
```

### Resolución pura del mensaje (`:shared`, testeable sin UI)
```kotlin
/** Texto del snackbar: messageVar (resuelto del store) o el literal message (HU-3.4). */
internal fun resolveSnackbarMessage(a: ShowSnackbar, scope: VariableScope): String =
    a.messageVar?.removePrefix("$")?.let { scope.get(it)?.asPlainString() } ?: a.message

/** Stringify seguro de un JsonElement sin depender del `asDisplayString` interno del motor. */
internal fun JsonElement.asPlainString(): String = (this as? JsonPrimitive)?.content ?: toString()

/** Mapea el string de duración del contrato al enum de Material (default Short). */
internal fun String.toSnackbarDuration(): SnackbarDuration = when (this) {
    "long" -> SnackbarDuration.Long
    "indefinite" -> SnackbarDuration.Indefinite
    else -> SnackbarDuration.Short
}
```

### `OverlayActionHandler` (`:shared`, `SubHandler`)
```kotlin
internal class OverlayActionHandler(
    private val overlay: OverlayController,
    private val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
    private val vars: VariableScope,
) : SubHandler {
    var dispatch: (List<UiAction>) -> Unit = {}   // re-dispatch del onAction del snackbar (patrón 009)
        internal set

    override fun supports(action: UiAction): Boolean =
        action is ShowDialog || action is ShowBottomSheet || action is ShowSnackbar || action is DismissOverlay

    override fun handle(actions: List<UiAction>) = actions.forEach { action ->
        when (action) {
            is ShowDialog -> overlay.showDialog(action)
            is ShowBottomSheet -> overlay.showSheet(action)
            is DismissOverlay -> overlay.dismissAll()
            is ShowSnackbar -> scope.launch {
                val result = snackbar.showSnackbar(
                    message = resolveSnackbarMessage(action, vars),
                    actionLabel = action.actionLabel,
                    duration = action.duration.toSnackbarDuration(),
                )
                if (result == SnackbarResult.ActionPerformed) dispatch(action.onAction)
            }
            else -> Unit
        }
    }
}
```
- **Decisión:** `onConfirm`/`onDismiss` del diálogo/hoja NO pasan por el handler async — los despacha el
  **render** del host en el callback del usuario (donde ya tiene el `handler`), tras `dismissAll()`. Solo
  el snackbar necesita `dispatch` porque su resultado llega asíncrono.

### `OverlayHost` (`:shared`, render del host)
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OverlayHost(
    overlay: OverlayController,
    snackbar: SnackbarHostState,
    dispatch: (List<UiAction>) -> Unit,   // = handler::handle del host
) {
    Box(Modifier.fillMaxSize()) {
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    overlay.dialog?.let { d ->
        AlertDialog(
            onDismissRequest = { overlay.dismissAll(); dispatch(d.onDismiss) },
            title = d.title.takeIf { it.isNotBlank() }?.let { { Text(it) } },
            text = d.text.takeIf { it.isNotBlank() }?.let { { Text(it) } },
            confirmButton = {
                d.confirmLabel?.let { l -> TextButton({ overlay.dismissAll(); dispatch(d.onConfirm) }) { Text(l) } }
            },
            dismissButton = d.dismissLabel?.let { l ->
                { TextButton({ overlay.dismissAll(); dispatch(d.onDismiss) }) { Text(l) } }
            },
        )
    }
    overlay.sheet?.let { s ->
        ModalBottomSheet(onDismissRequest = { overlay.dismissAll(); dispatch(s.onDismiss) }) {
            s.content.forEach { RenderNode(it) }   // usa los CompositionLocals del host (registry/vars/tema)
        }
    }
}
```
- **Decisión:** `OverlayHost` se invoca **dentro** del `CompositionLocalProvider` de `SduiHost` para que
  `RenderNode(content)` herede `LocalComponentRegistry`/`LocalVariables`/`LocalKuisdTheme`/handler de la
  entrada actual (las acciones del contenido de la hoja se despachan por el mismo handler compuesto).

### Cableado en `SduiHost.kt`
Dentro de `key(current.id) { … }`, junto a `store`/`scope`/`fire`/`handler`:
```kotlin
val overlay = remember { OverlayController() }
val snackbarHostState = remember { SnackbarHostState() }
val overlayHandler = remember { OverlayActionHandler(overlay, snackbarHostState, scope, store.scope) }
// añadir overlayHandler a la lista del AppActionHandler; tras construir handler:
overlayHandler.dispatch = handler::handle           // re-dispatch del onAction del snackbar
…
CompositionLocalProvider(/* … igual que hoy … */) {
    Box(Modifier.padding(padding)) {                // el padding del Scaffold del host va aquí
        SduiScreen(screenId = current.route, source = source, store = store)   // sin el padding modifier
        OverlayHost(overlay, snackbarHostState, dispatch = handler::handle)
    }
}
```
- **Decisión:** se mueve el `Modifier.padding(padding)` del `SduiScreen` a un `Box` envoltorio para
  poder superponer `OverlayHost` en el mismo espacio. El `AppActionHandler` ahora compone 5 sub-handlers
  (nav, variables, track, fire, overlay).

## Modelo de datos y estados
- Estado nuevo SOLO en la app: `OverlayController` (`dialog`/`sheet`) + `SnackbarHostState`, ambos por
  entrada del back stack (reset-on-nav, HU-4.3). El motor no gana estado.
- Helpers puros: `resolveSnackbarMessage`, `asPlainString`, `toSnackbarDuration` (testeables sin UI).

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| — | — | — | Ninguna. `AlertDialog`/`ModalBottomSheet`/`SnackbarHost` ya en `material3` (007/008/010). |

## Riesgos y mitigaciones
- **`ModalBottomSheet` experimental** → `@OptIn(ExperimentalMaterial3Api::class)` acotado a `OverlayHost`.
- **`ShowBottomSheet.content` con tipos no registrados** → `RenderNode` ya degrada a `UnknownNode` (004).
- **Acción de overlay sin host** (RenderNode aislado) → sin `OverlayActionHandler`, el `AppActionHandler`
  hace no-op + log (HU-4.4); cubierto por test.
- **Fuga entre pantallas** → estado dentro de `key(current.id)` ⇒ se descarta al navegar (HU-4.3).
- **`messageVar` no-primitivo** → `asPlainString` cae a `toString()` (sin crash).
- **Polimorfismo de `UiAction`** → registrar los `@SerialName` nuevos; `polymorphicDefaultDeserializer`
  existente mapea tipos desconocidos a `NoOpAction` (forward-compat preservada). Cubrir round-trip en test.

## Estrategia de verificación
- **Unit `:sdui-core` (commonTest):** `OverlayActionsWireTest` — round-trip JSON de las 4 acciones por el
  `sealed UiAction` (incl. `ShowBottomSheet` con `content` anidado y `DismissOverlay` como `data object`).
- **Unit `:shared` (commonTest):**
  - `OverlayControllerTest` — `showDialog` limpia `sheet` y viceversa; `dismissAll` limpia ambos.
  - `OverlaySnackbarTest` — `resolveSnackbarMessage` (messageVar presente/ausente/no-primitivo) y
    `toSnackbarDuration` (short/long/indefinite/desconocido→Short).
  - `OverlayActionHandlerTest` — `supports()` cubre las 4 acciones; `ShowDialog`/`ShowBottomSheet`/
    `DismissOverlay` mutan el controller; (el snackbar async se valida vía los helpers puros).
  - Ampliar `AppActionHandlerTest` — una acción overlay con el handler presente NO cae a no-op.
- **e2e `:server`:** pantalla piloto (p.ej. `form` o `home`) con un botón `ShowDialog` cuyo `onConfirm`
  es `ShowSnackbar`, y otro botón `ShowBottomSheet`. `:server:build` + `curl` que el árbol los emite.
- **Smoke manual:** `:desktopApp:run` — abrir diálogo, confirmar → snackbar; abrir hoja, descartar.
- **Calidad:** `./gradlew :sdui-core:check :shared:jvmTest :server:build detekt ktlintCheck`.
