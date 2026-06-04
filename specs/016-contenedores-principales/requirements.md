# Requisitos — Contenedores principales en el builder (Scaffold)

> Spec ID: 016 · Estado: approved · Fecha: 2026-06-04

## Resumen
Hoy el builder (specs 014/015) trata todos los `type`s por igual: la raíz por defecto es una `column` y
cualquier componente puede colocarse como raíz. El motor, en cambio, espera que la pantalla esté
estructurada por un **scaffold** (que parte sus hijos por `type`: `topAppBar`→topBar, `bottomBar`→bottomBar,
resto→content). Esta feature introduce en el builder el concepto de **contenedor principal**: un tipo de
componente que **envuelve** a los demás y solo puede vivir en la **raíz** del documento. Empezamos con
**Scaffold** —con edición de sus propiedades y de sus slots **topBar / content / bottomBar**— dejando el
modelo **escalable** para añadir más contenedores principales después. Regla dura: si un `type` no está
declarado como contenedor principal, **no puede ser el primer componente** (la raíz).

## Fuera de alcance
- **Más contenedores principales que el Scaffold** en v1 (el modelo debe soportarlos, pero solo se entrega
  Scaffold). Otros (p. ej. una pantalla con `pager`, `navigationRail`, etc.) quedan como follow-up.
- **Cambios en el motor** (`:sdui-core`/`:sdui-compose`): el scaffold ya parte slots por `type`; el builder
  produce esa misma estructura plana. Sin nuevo campo `slots` en `SduiNode`.
- **Anidar contenedores principales** (un scaffold dentro de otro): un contenedor principal solo es raíz.
- **Edición avanzada del topBar/bottomBar** más allá de añadir el nodo de slot y sus props básicas
  (p. ej. acciones del topAppBar, navegación del bottomBar) — se apoya en lo que ya exponga el catálogo.
- **Multiplataforma**: sigue siendo solo desktop (igual que el resto del builder).
- **Migración automática de archivos antiguos** con raíz no-contenedor más allá de la política que se decida
  en HU-4 (no se reescriben en disco salvo que el usuario guarde).

## Historias de usuario y criterios de aceptación

### HU-1 — Concepto de contenedor principal (modelo escalable)
**Como** mantenedor del builder **quiero** declarar qué tipos son "contenedores principales"
**para** centralizar la regla de raíz y poder añadir más tipos sin tocar la lógica de árbol.

Criterios (EARS):
1. The system SHALL declarar, de forma centralizada y extensible, el conjunto de tipos que son
   **contenedores principales** (en v1: solo `scaffold`), incluyendo por cada uno los **slots** que admite.
2. The system SHALL exponer si un `type` dado es contenedor principal, de modo que la paleta, el outline y
   las operaciones de árbol consulten esa única fuente de verdad.
3. WHEN se añada un nuevo contenedor principal a la declaración, the system SHALL aplicarle las mismas
   reglas (solo-raíz, slots) sin requerir cambios en `TreeOps`/`BuilderApp`.

### HU-2 — La raíz debe ser un contenedor principal
**Como** diseñador **quiero** que la pantalla siempre tenga una estructura principal válida
**para** que el blueprint sea coherente con lo que el motor espera renderizar.

Criterios (EARS):
1. The system SHALL garantizar que la raíz del documento es siempre un contenedor principal.
2. WHEN el usuario crea un documento **Nuevo**, the system SHALL inicializarlo con un `scaffold` (id `root`)
   como raíz, con los slots vacíos. **(Decidido: la raíz por defecto cambia de `column` a `scaffold`.)**
3. The system SHALL impedir que un componente que **no** es contenedor principal quede como raíz: las
   inserciones desde la paleta van siempre a un slot del contenedor principal, nunca reemplazan la raíz.
4. The system SHALL impedir **borrar** la raíz (contenedor principal) dejando el documento sin estructura
   principal.
5. The system SHALL impedir colocar un contenedor principal (p. ej. `scaffold`) **dentro** de otro nodo
   (los contenedores principales solo existen en la raíz; nada de scaffolds anidados).

### HU-3 — Editar el Scaffold y sus slots (topBar / content / bottomBar)
**Como** diseñador **quiero** configurar el scaffold y colocar componentes en su topBar, bottomBar y content
**para** componer la estructura de una pantalla real.

Criterios (EARS):
1. The system SHALL permitir editar las **propiedades** del scaffold expuestas por el catálogo
   (p. ej. dirección del content).
2. The system SHALL presentar el scaffold en el outline con **tres slots explícitos** —Top bar, Content,
   Bottom bar— de modo que el usuario seleccione un slot y entienda dónde coloca cada componente.
3. WHEN el usuario tiene seleccionado el slot **Top bar** y añade desde la paleta, the system SHALL colocar
   el componente como `topAppBar` del scaffold (slot **único**: si ya hay topBar, lo reemplaza o impide el
   segundo).
4. WHEN el usuario tiene seleccionado el slot **Bottom bar** y añade desde la paleta, the system SHALL
   colocarlo como `bottomBar` del scaffold (slot **único**).
5. WHEN el usuario tiene seleccionado el slot **Content** (o un nodo dentro de él) y añade desde la paleta,
   the system SHALL añadir el componente al content (slot **múltiple**, ordenado), respetando el anidamiento
   normal de contenedores no-principales.
6. The system SHALL serializar el scaffold como un `SduiNode` con `children` planos que el motor parte por
   `type` (topAppBar→topBar, bottomBar→bottomBar, resto→content), sin introducir un campo nuevo en
   `SduiNode`. Los slots explícitos del outline son una **vista** sobre esos children planos, no un cambio
   de contrato.

### HU-4 — Abrir archivos con raíz no-contenedor-principal
**Como** diseñador **quiero** abrir blueprints existentes sin romper la regla de raíz
**para** seguir editándolos de forma coherente.

Criterios (EARS):
1. WHEN se carga un archivo cuya raíz **ya es** un contenedor principal, the system SHALL cargarlo tal cual
   (round-trip fiel, sin alterar su estructura).
2. WHEN se carga un archivo cuya raíz **no** es un contenedor principal, the system SHALL **rechazar** la
   carga con un mensaje que explique la regla de raíz, **y ofrecer una acción de arreglo automático**.
3. WHEN el usuario acepta el arreglo automático, the system SHALL envolver el árbol cargado dentro del slot
   content de un `scaffold` nuevo y cargarlo como documento (marcado como modificado).
4. IF el árbol cargado contiene un contenedor principal anidado (p. ej. un `scaffold` en cualquier punto que
   no sea la raíz), THEN the system SHALL **no** aplicar el arreglo y mostrar un error (no se puede envolver
   sin anidar contenedores principales).
5. WHEN el usuario rechaza o cancela el arreglo, the system SHALL conservar el documento actual sin alterarlo
   (consistente con HU-2.5 de la spec 015).

## Requisitos no funcionales
- **Solo desktop**; sin dependencias nuevas. Reutiliza catálogo, `TreeOps`, `BuilderDocument` y el render
  de preview existentes.
- **Sin cambios en el motor**; el builder emite la estructura plana que el scaffold ya entiende.
- **Escalabilidad**: añadir un contenedor principal nuevo = una entrada declarativa, sin tocar la lógica de
  inserción/borrado/validación.
- **Resiliencia**: ninguna acción inválida (insertar no-contenedor en raíz, segundo topBar) debe dejar un
  árbol incoherente ni cerrar la app.
- Lint/detekt/ktlint en verde para `:builder`.

## Dependencias y supuestos
- Spec 010 (scaffold en el motor): el scaffold parte `children` por `type` vía `partitionScaffoldSlots`
  (`topAppBar`/`bottomBar`/resto). El builder debe producir esa estructura.
- Specs 014/015 (builder): `BuilderCatalog` (con `acceptsChildren`, ya incluye `scaffold` y `topAppBar`),
  `TreeOps`, `BuilderDocument` (raíz por defecto = `column` "root"), outline/inspector/paleta.
- El catálogo **no** tiene aún entrada `bottomBar` (sí `topAppBar`); habrá que añadirla para el slot bottomBar.

## Decisiones tomadas
1. **Raíz por defecto en "Nuevo"**: cambia de `column` "root" a **`scaffold` "root"** (afecta a
   `newDocument` y al JSON por defecto de 015; se actualizan sus tests).
2. **Política de carga (HU-4)**: raíz no-contenedor → **rechazar con mensaje + acción de arreglo automático**;
   el arreglo envuelve el árbol en el content de un `scaffold`, **salvo** que el árbol contenga un contenedor
   principal anidado, en cuyo caso se rechaza el arreglo con error.
3. **UI de slots**: **slots explícitos** (Top bar / Content / Bottom bar) en el outline; la paleta inserta en
   el slot seleccionado. Vista sobre children planos (sin cambiar `SduiNode`).
4. **Alcance de tipos**: v1 entrega **solo Scaffold** como contenedor principal; el modelo queda declarativo
   y escalable para añadir más después.
