# Specs — Spec-Driven Development en kuisd

Cada feature se construye contra una **spec ejecutable** de 3 archivos. La spec es la
**fuente de verdad**: el código se escribe para satisfacerla, no al revés.

```
specs/
├── templates/                      # plantillas base (no se editan al implementar)
│   ├── requirements.md
│   ├── design.md
│   └── tasks.md
└── NNN-<feature-kebab>/
    ├── requirements.md   # QUÉ y POR QUÉ — historias + criterios EARS
    ├── design.md         # CÓMO — arquitectura, contratos, decisiones
    └── tasks.md          # PASOS — checklist atómico y trazable
```

## Flujo (4 fases con gate de aprobación)

1. **requirements** — historias de usuario + criterios de aceptación en formato **EARS**
   (`WHEN…SHALL`, `WHILE…SHALL`, `IF…THEN…SHALL`, `The system SHALL…`). Gate: aprobar antes de diseñar.
2. **design** — se deriva de los requisitos aprobados: módulos, contratos (firmas Kotlin),
   estados, dependencias nuevas, riesgos y estrategia de verificación. Gate: aprobar antes de tareas.
3. **tasks** — checklist atómico; cada tarea referencia el requisito (HU-x) y la sección de
   diseño (§) que cubre, y dice **cómo se verifica**.
4. **implement** — se ejecutan las tareas en orden; al verificar cada una se marca `[x]` en
   `tasks.md`. No se escribe código de producción antes de que `design.md` esté aprobado.

Estado de cada documento en su cabecera: `Estado: draft | approved`.

## Comando

Usa `/spec` para crear y avanzar specs de forma consistente:

```
/spec new <feature-kebab>      # crea specs/NNN-feature/ desde plantillas + borrador de requisitos
/spec requirements <id>        # redacta/refina requirements.md
/spec design <id>              # redacta design.md (requiere requisitos aprobados)
/spec tasks <id>               # deriva tasks.md
/spec status <id>              # resumen de progreso
/spec implement <id> [Tn]      # implementa la siguiente tarea (o Tn) y la marca hecha
```

## Convenciones

- IDs incrementales de 3 dígitos (`001`, `002`, …).
- Una feature = un slice entregable y verificable de punta a punta.
- Trazabilidad obligatoria: toda tarea apunta a un requisito y a una decisión de diseño.
- La verificación de cada feature se ejecuta de verdad (tests + smoke), no se asume.
