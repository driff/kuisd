# Requisitos — Presets de barras + FAB + top bar centrado

> Spec ID: 020 · Estado: approved · Fecha: 2026-06-04

## Resumen
Acelerar la composición de pantallas en el builder con **presets de barras** listos para usar (Top Bar de
navegación, Top Bar con acciones, Bottom menú) y completar las capacidades de estructura que hoy faltan en el
motor: **Floating Action Button (FAB)** en el `Scaffold` y **top bar con título centrado**. Abarca
**`:sdui-compose`** (FAB: nuevo slot + componente `fab` + posición; `CenterAlignedTopAppBar`) y **`:builder`**
(slot FAB en el scaffold, componente `fab` en la paleta, presets, y campos nuevos en el inspector).
`:sdui-core` no cambia (todo vía `SduiNode`/props serializables del motor).

## Fuera de alcance
- **FAB extendido** (con texto), **FAB sin icono**: v1 = FAB de icono (icono + acción onClick).
- **Más posiciones de FAB** que `end`/`center`; sin posiciones personalizadas.
- **Presets configurables por el usuario / guardar presets propios**: los presets son entradas fijas del catálogo.
- **Editar acciones reales** (navegación) por UI: los presets traen acciones placeholder para que el preview
  muestre el icono de navegación / onClick; editar acciones es otra feature.
- **LazyColumn/LazyRow** con FAB: el FAB es del `Scaffold`.
- `:sdui-core` sin cambios.

## Historias de usuario y criterios de aceptación

### HU-1 — FAB en el Scaffold (motor)
**Como** autor de blueprints **quiero** un FAB en la pantalla
**para** la acción principal (p. ej. crear).

Criterios (EARS):
1. The system SHALL reconocer un child del `scaffold` de `type == "fab"` como el **slot FAB** (igual criterio
   que topAppBar/bottomBar: el primero gana), y montarlo en `Scaffold(floatingActionButton = …)`.
2. The system SHALL ofrecer un componente `fab` que renderiza un `FloatingActionButton` con un icono y delega
   su acción `onClick` al handler.
3. The system SHALL permitir elegir la **posición** del FAB (`end` por defecto, o `center`) vía una prop del
   `scaffold` (`fabPosition`), mapeada a `FabPosition`.
4. IF no hay child `fab`, THEN the system SHALL renderizar el `Scaffold` sin FAB (sin regresión).

### HU-2 — Top bar con título centrado (motor)
**Como** autor de blueprints **quiero** un top bar con el título centrado
**para** estilos de navegación tipo iOS / pantallas de detalle.

Criterios (EARS):
1. The system SHALL soportar en `topAppBar` una prop `centered` (bool, default false); WHEN `centered` es
   true, SHALL renderizar un `CenterAlignedTopAppBar` en vez del `TopAppBar` estándar.
2. The system SHALL conservar el resto del comportamiento del topAppBar (título bindable, navigationIcon con
   `onNavigationClick`, acciones como children) en ambas variantes.

### HU-3 — Editar FAB y centrado en el builder
**Como** diseñador **quiero** añadir un FAB y elegir su posición / centrar el top bar
**para** componer la estructura desde el builder.

Criterios (EARS):
1. The system SHALL declarar en el contenedor principal `scaffold` un **slot FAB** (único), de modo que el
   outline lo muestre y se le pueda asignar un `fab` (consistente con el modelo de slots de la spec 016).
2. The system SHALL ofrecer en la paleta un componente `fab` (que al insertarse va al slot FAB del scaffold).
3. The system SHALL exponer en el inspector del `scaffold` la prop `fabPosition` (enum `end`/`center`) y en el
   `topAppBar` la prop `centered` (bool).

### HU-4 — Presets de barras en la paleta
**Como** diseñador **quiero** insertar barras preconfiguradas
**para** no construirlas nodo a nodo.

Criterios (EARS):
1. The system SHALL ofrecer en la paleta, como entradas distintas, estos presets:
   - **Top Bar · Navegación**: `topAppBar` con título + `navigationIcon` (back) + acción `onNavigationClick`.
   - **Top Bar · Acciones**: `topAppBar` con título + 2 `iconButton` como acciones (end).
   - **Top Bar · Centrado**: `topAppBar` con `centered = true` + título.
   - **Bottom · Menú (3)**: `bottomBar` con 3 `bottomBarItem` (icono + label + value).
   - **Bottom · Menú (5)**: `bottomBar` con 5 `bottomBarItem`.
   - **FAB**: un `fab` con icono de "añadir" + acción `onClick` placeholder (al insertarse va al slot FAB del
     scaffold raíz; equivale al "scaffold con FAB ya colocado").
2. WHEN el usuario inserta un preset de top bar / bottom bar / fab, the system SHALL colocarlo en el slot
   correspondiente del scaffold (topBar/bottomBar/fab), reemplazando el contenido previo del slot (modelo 016).
3. The system SHALL permitir que varios presets produzcan el mismo `type` de nodo (p. ej. tres variantes de
   `topAppBar`, dos de `bottomBar`) **sin colisiones** en la paleta ni en el inspector (identidad de preset ≠
   `type` de componente).
4. WHEN un preset se inserta, the system SHALL asignar ids únicos a todo su subárbol (invariante del builder).

## Requisitos no funcionales
- **`:sdui-compose` + `:builder`**; `:sdui-core` sin cambios. Sin dependencias nuevas (Material3 ya provee
  `FloatingActionButton`/`CenterAlignedTopAppBar`/`FabPosition`).
- **Sin regresión**: scaffolds/top bars existentes (sin fab/centered) renderizan igual; props nuevas con
  defaults (`centered=false`, `fabPosition="end"`).
- **Resiliencia**: `fab` sin icono válido o `fabPosition` desconocido degradan sin crash (icono fallback /
  posición por defecto).
- Tests del motor en verde; lint/detekt/ktlint en `:sdui-compose` y `:builder`.

## Dependencias y supuestos
- Spec 010 (scaffold): `partitionScaffoldSlots` parte children por `type`; `ScaffoldRenderer` monta los slots.
- Spec 016 (contenedores principales): `MainContainerSpec`/slots del scaffold en el builder; `SlotOps`;
  `setSingleSlot` para slots únicos. El slot FAB se añade a la declaración del scaffold.
- Specs 014/017: `BuilderCatalog` (`PaletteEntry`/`FieldSpec`), `PalettePane` (usa `it.type` como key de
  `items`), `catalogByType` (`associateBy { it.type }`) para el inspector. **Restricción a resolver en
  diseño:** hoy la identidad de la paleta == `type`; los presets que comparten `type` requieren desacoplar la
  identidad de la entrada (key/label) del `type` del nodo, sin romper `PalettePane` ni `catalogByType`.
- Motor: `TopAppBarRenderer`/`ScaffoldRenderer`/`BottomBarRenderer` en `CorePack.kt`; iconos vía
  `resolveIcon`/`DefaultIconRegistry` (+ overrides de la app).

## Decisiones tomadas
1. **Posición del FAB**: `end` (default) + `center`.
2. **Bottom menú**: dos presets, **3 y 5** ítems.
3. **Presets armados**: además del componente `fab` y los campos `fabPosition`/`centered`, se entregan los
   presets **Top Bar · Centrado** (topAppBar centered) y **FAB** (`fab` con icono "+" → slot FAB del scaffold
   raíz; el scaffold ya existe como raíz, no se inserta).
