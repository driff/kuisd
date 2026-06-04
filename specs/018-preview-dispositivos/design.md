# Diseño — Preview multi-dispositivo

> Spec ID: 018 · Estado: approved · Trazabilidad: ./requirements.md

## Enfoque
Enmarcar el contenido del preview del `:builder` en un **lienzo de tamaño fijo (dp)** según un preset de
dispositivo (Phone/Tablet/Desktop), con un **selector** y **scroll** cuando el lienzo excede el panel. Es
**solo UI del `:builder`**: no cambia el motor, el árbol ni el documento. El mismo `SduiPreviewEnvironment`
renderiza el mismo `RenderNode(document.root)`, pero dentro de un `Box` dimensionado, de modo que
`fillMaxWidth`/alineación se evalúen contra el ancho del dispositivo (no del panel).

## Arquitectura
Módulo afectado: **solo `:builder`**. Sin cambios de motor.

```
ui/DevicePreset.kt   ── (NUEVO) enum/data de presets (label, width dp, height dp)
ui/PreviewPane.kt    ── (NUEVO) selector + lienzo dimensionado con scroll, envuelve SduiPreviewEnvironment
ui/BuilderApp.kt     ── el Box izquierdo del preview pasa a usar PreviewPane(document, handler)
```

## Componentes y contratos

### DevicePreset — presets de dispositivo
- **Ubicación:** `ui/DevicePreset.kt` (nuevo).
```kotlin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class DevicePreset(val label: String, val width: Dp, val height: Dp) {
    Phone("Phone", 360.dp, 800.dp),
    Tablet("Tablet", 800.dp, 1280.dp),
    Desktop("Desktop", 1280.dp, 800.dp),
}

internal val DefaultDevice = DevicePreset.Phone
```

### PreviewPane — selector + lienzo + scroll
- **Ubicación:** `ui/PreviewPane.kt` (nuevo).
- **Responsabilidad:** mostrar el selector de dispositivo y el lienzo dimensionado con scroll; dentro, el
  `SduiPreviewEnvironment` con el árbol. Mantiene su propio estado de dispositivo (efímero, no en el documento).
- **Contrato:**
```kotlin
@Composable
internal fun PreviewPane(document: BuilderDocument, handler: SduiActionHandler, modifier: Modifier = Modifier) {
    var device by remember { mutableStateOf(DefaultDevice) }
    Column(modifier) {
        DeviceSelector(selected = device, onSelect = { device = it })          // HU-1
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Surface( // lienzo a tamaño real (1:1), distinguible (borde + sombra)  // HU-2.1
                modifier = Modifier.size(device.width, device.height),
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                SduiPreviewEnvironment(handler) { RenderNode(document.root) }    // HU-2.2 (mismo árbol)
            }
        }
    }
}
```
- **`DeviceSelector`:** una fila de 3 controles seleccionables (p. ej. `FilterChip` o `SegmentedButton`); al
  pulsar uno fija `device`. Resalta el seleccionado.
- **Decisiones:**
  - **Scroll a tamaño real (1:1)**: `horizontalScroll` + `verticalScroll` envolviendo el lienzo de
    `Modifier.size(width, height)`. Tablet/desktop exceden el panel → se recorren con scroll (HU-2.3).
  - El estado `device` vive en `PreviewPane` (efímero); no se guarda en el documento (NFR).
  - El `Surface` con `border`+`tonalElevation` separa visualmente el lienzo del panel.
  - El árbol y el `handler` se siguen pasando desde `BuilderApp`; no se re-crea el entorno al cambiar de
    dispositivo (solo cambia el `size` del `Box`), evitando coste (NFR rendimiento).

### BuilderApp — usar PreviewPane
- **Ubicación:** `ui/BuilderApp.kt` (cambio mínimo).
- **Cambio:** el `Box(Modifier.weight(PREVIEW_WEIGHT)…) { SduiPreviewEnvironment(handler) { RenderNode(root) } }`
  se reemplaza por `PreviewPane(document, handler, Modifier.weight(PREVIEW_WEIGHT).fillMaxHeight())`.

## Modelo de datos y estados
- Tipo nuevo `DevicePreset` (enum UI). Estado `device` efímero en `PreviewPane`. Sin cambios en
  `BuilderDocument`/`SduiNode`/envelope.

## Dependencias nuevas (catálogo de versiones)
| Librería | Versión | Source set | Motivo |
|----------|---------|------------|--------|
| _(ninguna)_ | — | — | Compose layout estándar (`size`, `horizontalScroll`, `verticalScroll`, `Surface`, `BorderStroke`). |

## Riesgos y mitigaciones
- **Scroll anidado dentro del split**: el panel ya vive en un `Row` con `weight`; el `Box` con doble scroll es
  autónomo (no compite con scroll del outline, que está en la columna derecha). Smoke lo valida.
- **Lienzo enorme (desktop 1280)**: a tamaño real puede requerir scroll en ambos ejes; es lo decidido (1:1).
- **`fillMaxWidth` del root**: el `scaffold` raíz con `fillMaxSize`/`fillMaxWidth` se ajusta al `size` del
  lienzo, no al panel — que es justo el objetivo (HU-2.2).

## Estrategia de verificación
- **Sin lógica pura nueva** relevante (es UI); el preset es datos. Test mínimo opcional: `DevicePresetTest`
  comprueba que los tres presets tienen dimensiones > 0 y `DefaultDevice == Phone`.
- **Compilación/lint:** `./gradlew :builder:test :builder:detekt :builder:ktlintCheck` en verde.
- **Smoke (`:builder:run`):** el selector cambia el tamaño del lienzo; Phone entra en el panel; Tablet/Desktop
  muestran scroll; el árbol se ve idéntico dentro del marco; `fillMaxWidth` ocupa el ancho del dispositivo.
