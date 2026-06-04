# Requisitos — Preview multi-dispositivo (phone / tablet / desktop)

> Spec ID: 018 · Estado: approved · Fecha: 2026-06-04

## Resumen
El preview del builder (spec 014) renderiza el árbol SDUI ocupando todo el panel izquierdo, sin noción de
tamaño de pantalla. Esta feature permite **visualizar el preview con el tamaño de un dispositivo**
(celular / tablet / desktop), enmarcando el contenido en un lienzo de dimensiones predefinidas y dejando un
selector para cambiar entre ellas. Para quien diseña, esto da una idea fiel de cómo se verá el blueprint en
cada forma de pantalla sin salir del builder. Es **solo UI del `:builder`**: no cambia el motor ni el árbol.

## Fuera de alcance
- **Simular densidad/`Density` o fuentes por dispositivo**: v1 solo fija el tamaño del lienzo (dp); no altera
  la densidad ni escala tipográfica del sistema.
- **Catálogo extenso de dispositivos reales** (Pixel X, iPhone Y…): v1 ofrece 3 presets genéricos
  (phone/tablet/desktop). Más presets = follow-up.
- **Editar/crear presets personalizados por el usuario**: diferido.
- **Cambios en el motor** ni en el árbol/documento: el preview sigue renderizando el mismo `SduiNode`.
- Persistir el dispositivo elegido en el archivo del documento (es estado efímero de UI).

## Historias de usuario y criterios de aceptación

### HU-1 — Selector de dispositivo
**Como** diseñador **quiero** elegir entre celular, tablet y desktop
**para** previsualizar el blueprint en distintas formas de pantalla.

Criterios (EARS):
1. The system SHALL ofrecer en la UI un selector con al menos tres opciones: **Phone**, **Tablet**, **Desktop**.
2. The system SHALL tener **Phone** como dispositivo por defecto al abrir el builder.
3. WHEN el usuario cambia de dispositivo, the system SHALL re-enmarcar el preview al tamaño correspondiente
   sin alterar el árbol ni la selección.

### HU-2 — Lienzo dimensionado
**Como** diseñador **quiero** que el preview se muestre con las dimensiones del dispositivo
**para** ver el layout tal como se ajustaría a ese ancho/alto.

Criterios (EARS):
1. The system SHALL enmarcar el contenido del preview en un lienzo de ancho×alto (dp) propios del dispositivo
   seleccionado, a **tamaño real (1:1)**, visualmente distinguible (borde/sombra) del resto del panel.
2. The system SHALL renderizar el mismo árbol SDUI dentro del lienzo (mismo `SduiPreviewEnvironment`), de modo
   que `fillMaxWidth` y la alineación se evalúen contra el ancho del dispositivo, no del panel.
3. WHEN el lienzo a tamaño real excede el panel disponible, the system SHALL permitir **scroll**
   (horizontal y/o vertical) dentro del panel para recorrerlo, sin escalar ni recortar de forma confusa.

### Tamaños de los presets (decididos)
- **Phone**: 360 × 800 dp · **Tablet**: 800 × 1280 dp · **Desktop**: 1280 × 800 dp.
- Orientación fija (retrato para phone/tablet; apaisado para desktop). El toggle retrato/paisaje queda como
  follow-up (fuera de alcance v1).

## Requisitos no funcionales
- **Solo `:builder`**, sin cambios en el motor; sin dependencias nuevas (Compose layout estándar).
- Estado del dispositivo es **efímero de UI** (no se guarda en el documento).
- Rendimiento: cambiar de dispositivo no debe re-crear el `SduiPreviewEnvironment` de forma costosa.
- Lint/detekt/ktlint en verde para `:builder`.

## Dependencias y supuestos
- Spec 014: `BuilderApp` monta el preview en un `Box` izquierdo con `SduiPreviewEnvironment(handler) { RenderNode(root) }`.
- Compose Desktop: el panel puede ser pequeño; los tamaños tablet/desktop probablemente excedan el panel
  → de ahí HU-2.3 (escalar o scroll).

## Decisiones tomadas
1. **Tamaños** (dp): Phone 360×800, Tablet 800×1280, Desktop 1280×800.
2. **Default**: Phone.
3. **Cuando no cabe**: **scroll** a tamaño real (1:1), sin escalar.
4. **Orientación**: fuera de v1 (follow-up).
