# Requisitos — Overlays (`dialog` · `bottomSheet` · `snackbar`)

> Spec ID: 011 · Estado: approved · Fecha: 2026-06-03

## Resumen
El catálogo (hasta 010) cubre contenido y estructura, pero no tiene **superposiciones**: modales de
confirmación, hojas inferiores ni avisos transitorios. Esta spec las añade con un **modelo imperativo**
coherente con la frontera del motor (el motor renderiza el árbol + delega acciones; el **estado y el
render de los overlays viven en la app/host**, como navegación en 003 y red en 009):

- Tres **acciones** nuevas en `:sdui-core` — `ShowDialog`, `ShowBottomSheet`, `ShowSnackbar` — más una
  `DismissOverlay` para cierre programático. No son nodos del árbol.
- El **`SduiHost`** (capa app) posee un **estado de overlay** (diálogo/hoja activos + `SnackbarHostState`)
  y renderiza el overlay activo **por encima** de la pantalla, **reutilizando `RenderNode`** para el
  contenido (p.ej. el cuerpo de la hoja). El `SnackbarHost` vive en el `Scaffold` **del host**, así
  funciona haya o no `scaffold` (010) en la pantalla.
- Un `SubHandler` de la app (`OverlayActionHandler`, patrón `Track`/`FireEndpoint` de 009) traduce las
  acciones en mutaciones de ese estado.

**Consecuencia:** `:sdui-compose` (motor) y el `scaffold` de 010 **no se modifican**; el trabajo se
concentra en `:sdui-core` (acciones) y `:shared` (estado de overlay + handler + render en el host).

## Fuera de alcance
- **Menús / dropdowns / tooltips / popups de anclaje** — superposiciones posicionadas; spec posterior.
- **`DatePicker` / `TimePicker` / pickers Material** — entrada compleja; fuera.
- **Diálogos a pantalla completa** y anchos/forma personalizados — v1 usa `AlertDialog` estándar.
- **Estados intermedios del `bottomSheet`** (half-expanded con anclajes) — v1 solo oculto ↔ visible.
- **Pila de overlays (varios a la vez)** — v1: **un** diálogo/hoja activo (el nuevo reemplaza al previo);
  el snackbar es independiente (lo serializa el `SnackbarHostState`). Sin stack ni cola propia.
- **Snackbars con swipe-to-dismiss programático / multi-acción** — v1: una acción opcional.
- **Interpolaciones complejas** en el mensaje del snackbar — solo `message` literal o un `messageVar`
  resuelto del store en dispatch.
- **Animaciones/temas personalizados** más allá de los defaults de Material/`KuisdTheme`.

## Historias de usuario y criterios de aceptación

### HU-1 — `ShowDialog`: modal de confirmación imperativo
**Como** autor de pantallas **quiero** lanzar un diálogo desde una acción **para** pedir confirmación
sin maquetarlo en el árbol.

Criterios (EARS):
1. The contract `:sdui-core` SHALL definir `ShowDialog(title, text, confirmLabel?, onConfirm,
   dismissLabel?, onDismiss)` como `UiAction` serializable (parte del `sealed interface UiAction`),
   donde `onConfirm`/`onDismiss` son `List<UiAction>`.
2. WHEN el handler recibe un `ShowDialog`, the app SHALL fijar ese diálogo como overlay activo y
   the host SHALL renderizar un `AlertDialog` de Material3 con `title`/`text` y los botones presentes.
3. WHEN el usuario pulsa confirmar (resp. descartar), the host SHALL **cerrar** el diálogo y despachar
   `onConfirm` (resp. `onDismiss`) por el handler compuesto (re-dispatch, patrón 009).
4. WHEN el usuario descarta tocando fuera o con back, the host SHALL cerrar el diálogo y despachar
   `onDismiss`.
5. IF `confirmLabel` (resp. `dismissLabel`) es `null`, THEN the host SHALL omitir ese botón.
6. WHEN llega un nuevo `ShowDialog` con uno ya activo, the host SHALL reemplazarlo (v1 sin pila).

### HU-2 — `ShowBottomSheet`: hoja inferior imperativa
**Como** autor de pantallas **quiero** abrir una hoja inferior con contenido SDUI **para** mostrar
detalle contextual.

Criterios (EARS):
1. The contract `:sdui-core` SHALL definir `ShowBottomSheet(content: List<SduiNode>, onDismiss)` como
   `UiAction` serializable; `content` es un subárbol SDUI que el host renderiza con `RenderNode`.
2. WHEN el handler recibe un `ShowBottomSheet`, the host SHALL renderizar un `ModalBottomSheet` de
   Material3 con `content` (vía `RenderNode`, con el `LocalSduiActionHandler`/tema/variables del host).
3. WHEN el usuario desliza para cerrar, toca el scrim o pulsa back, the host SHALL cerrar la hoja y
   despachar `onDismiss`.
4. The host SHALL resolver apariencia (forma/tonal) desde `KuisdTheme`/Material por defecto.
5. WHEN llega un nuevo overlay (diálogo u hoja) con una hoja activa, the host SHALL reemplazar el activo.

### HU-3 — `ShowSnackbar`: aviso transitorio
**Como** autor de pantallas **quiero** lanzar un aviso breve tras una acción **para** dar feedback sin
ocupar layout permanente.

Criterios (EARS):
1. The contract `:sdui-core` SHALL definir `ShowSnackbar(message, messageVar?, actionLabel?, onAction,
   duration?)` como `UiAction` serializable; `duration` es un enum/string (`short`|`long`|`indefinite`).
2. The host SHALL alojar un `SnackbarHostState` en su `Scaffold` (independiente del `scaffold` de 010).
3. WHEN el handler recibe un `ShowSnackbar`, the app SHALL mostrar el mensaje en ese `SnackbarHostState`
   en un `CoroutineScope` (patrón `FireEndpoint`, 009).
4. IF `messageVar` está presente, THEN the app SHALL resolver el texto del `VariableStore` (005) en
   dispatch; en su defecto usa `message` literal.
5. WHEN hay `actionLabel` y el usuario pulsa la acción del snackbar, the app SHALL despachar `onAction`
   por el handler compuesto.

### HU-4 — Estado de overlay en el host y resiliencia
**Como** integrador **quiero** que el sistema de overlays sea robusto **para** no romper la pantalla.

Criterios (EARS):
1. The app SHALL incluir un `OverlayActionHandler` (`SubHandler`) que soporte `ShowDialog`,
   `ShowBottomSheet`, `ShowSnackbar` y `DismissOverlay`, integrado en el `AppActionHandler` compuesto
   **sin** romper el despacho de acciones existentes (003/005/009).
2. The contract `:sdui-core` SHALL definir `DismissOverlay` (`data object`) que cierra el diálogo/hoja
   activo si lo hay (no-op si no hay ninguno).
3. WHEN cambia la entrada del back stack (navegación), the host SHALL descartar cualquier overlay activo
   (no “fugarse” entre pantallas).
4. IF el `OverlayActionHandler` no está presente (p.ej. `RenderNode` fuera del host), THEN estas acciones
   SHALL degradar a no-op + log (como cualquier acción no soportada, 009), sin crash.

## Requisitos no funcionales
- **Multiplataforma:** acciones en `commonMain` de `:sdui-core`; estado/handler/render de overlay en
  `commonMain` de `:shared`. Sin APIs de plataforma; `@OptIn(ExperimentalMaterial3Api::class)` acotado.
- **Frontera del motor:** `:sdui-compose` NO se modifica. Los overlays son estado de app; el motor solo
  aporta `RenderNode` (ya existente) para pintar el contenido de la hoja.
- **Re-dispatch:** `onConfirm`/`onDismiss`/`onAction` se despachan por el handler compuesto (mismo
  mecanismo `dispatch` que `FireEndpointActionHandler`, 009).
- **Sin números mágicos:** apariencias por defecto de Material/`KuisdTheme`.

## Dependencias y supuestos
- Depende de: 003 (acciones + back stack), 005 (variables + `VariableStore`), 009 (`SubHandler` +
  re-dispatch + `CoroutineScope` por entrada del back stack), 010 (`scaffold`; el host ya tiene `Scaffold`).
- **Decisión tomada — modelo imperativo:** overlays vía acciones con estado en el host (no nodos
  declarativos enlazados a variable). Alternativa descartada: `dialog`/`bottomSheet` como componentes con
  `visibleBind` (más motor-puro pero menos alineado con la API imperativa pedida).
- **Decisión tomada — el host posee el estado y renderiza los overlays**, incluido el `SnackbarHost` en
  su propio `Scaffold`; el `scaffold` de 010 no gana un slot nuevo (menos acoplamiento, evita re-abrir el
  tema de Scaffold anidado de 010).
- **Supuesto:** `ShowBottomSheet.content` (subárbol `SduiNode`) viaja serializado en la acción, análogo a
  `FireEndpoint.onSuccess: List<UiAction>` (009).
- **Supuesto:** pantalla piloto del server para e2e (candidata: botón en `home`/`form` que lanza
  `ShowDialog` cuyo `onConfirm` lanza un `ShowSnackbar`; y un botón que abre `ShowBottomSheet`).
