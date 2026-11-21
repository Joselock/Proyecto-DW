# Documentación · Proyecto-DW

Esta carpeta reúne la documentación funcional y técnica disponible del proyecto **Sistema de Gestión Hospitalaria** (versión web en desarrollo).

## Contenido

| Documento | Descripción |
| --- | --- |
| [`estado-actual.md`](estado-actual.md) | Resumen actualizado de lo implementado hasta ahora: arquitectura, módulos, ejecución, base de datos, API y pendientes conocidos. |
| [`seguimiento-turnos.md`](seguimiento-turnos.md) | Documentación detallada del flujo de turnos por fecha/hora, estados en vivo, notificaciones, edición, cambio de día y análisis de UI implementada frente a pendiente. |
| [`Resumen_Proyecto_DW.docx`](Resumen_Proyecto_DW.docx) | Resumen general del proyecto (formato Word). |
| [`Resumen_Proyecto_DW.pdf`](Resumen_Proyecto_DW.pdf) | Resumen general del proyecto (formato PDF). |

## Documentación relacionada fuera de `docs/`

- [`../README.md`](../README.md): presentación breve del repositorio.
- [`../database/README.md`](../database/README.md): modelo relacional, scripts SQL, migraciones, datos de prueba, disparadores, reportes y notas de compatibilidad/verificación de la base de datos.
- [`../estructura.txt`](../estructura.txt): estructura de referencia planteada para el proyecto.

## Resumen rápido

- **Backend:** Spring Boot 3.2.5 (Java 17, Maven), API REST bajo `/api`, sesiones HTTP, acceso a PostgreSQL con JPA/JDBC.
- **Frontend:** HTML, CSS y JavaScript vanilla servidos por el propio backend (`index.html` para acceso, `dashboard.html` para el panel).
- **Base de datos:** PostgreSQL (`sistema`), con esquema, vistas, funciones CRUD, reportes SQL, triggers y datos de prueba en `database/`.
- **Funcionalidades destacadas implementadas:** autenticación y registro de usuarios, recuperación de contraseña, panel por rol (`ADMIN`/`MEDICO`), gestión de pacientes y turnos con fecha/hora, seguimiento en vivo de estados, notificaciones, directorio de profesionales, reportes y preferencias de interfaz.
