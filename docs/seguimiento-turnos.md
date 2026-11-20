# Seguimiento real de turnos · Proyecto-DW

Este documento describe el flujo completo de fecha/hora de los turnos, estados
por reloj del sistema, cola de atención, notificaciones, edición desde el menú
de tres puntos y cambio de día. Incluye un análisis de la interfaz real frente a
la interfaz que todavía es decorativa, para que no se confunda una con otra.

La implementación quedó verificada en PostgreSQL 17, Spring Boot 3.2.5 y el
frontend servido por el propio backend en `http://localhost:8080`.

---

## 1. Alcance y decisiones

| Tema | Decisión |
| --- | --- |
| Visibilidad | `ADMIN` ve todos los pacientes y turnos de todos los hospitales. `MEDICO` ve solo sus turnos (`codMed` de la sesión) y los pacientes atendidos por él o ingresados en su unidad. |
| Identificadores de estado | En BD: `pendiente`, `confirmado`, `en_progreso`, `finalizado`, `no_atendido`. En la API/UI: `pending`, `confirmed`, `in_progress`, `done`, `no_atendido`. |
| Transición automática | `pendiente`/`confirmado` → `en_progreso` al llegar `fecha + hora`; → `finalizado` al llegar el fin estimado. |
| Fin estimado | Hora del siguiente turno del mismo médico y día; si no hay siguiente, `inicio + 30 min`; mínimo `1 min`. |
| Consulta | Al finalizar automáticamente se marca `Consulta.atendido = true` y se limpia `causa`. El usuario puede dejar constancia manual de `no_atendido`, que el motor respeta. |
| Notificaciones | Una de `inicio` y una de `finalizacion` por turno, con `UNIQUE(numTur, tipo)` para no duplicarlas. `ADMIN` ve todas; `MEDICO` solo las suyas. |
| Cambio de día | `state.fecha` (`AAAA-MM-DD`) se envía a `/api/panel/turnos`; el mismo día alimenta Resumen, Turnos, cola, stats y —si se activa el switcher— Pacientes. |
| Arquitectura | `AltasService` es la única capa Java de inserción/actualización. `service/CRUD/` no existe y no debe restaurarse. |

---

## 2. Archivos implicados

### Base de datos

| Archivo | Contenido |
| --- | --- |
| `database/01_schema.sql` | Agrega `Turno.fecha`, `Turno.hora`, `Turno.nota_clinica` y el índice `(fecha, hora)` al esquema base. |
| `database/05_seguimiento_turnos.sql` | Migración reejecutable: columnas, backfill de fecha/hora y nombres de prueba, tabla `Notificacion`, función `actualizar_turnos_en_vivo()` y ejecución inicial. |
| `database/CRUD/Insertar/Turno.sql` | `insertarTurno` con sobrecarga de 5 y 8 parámetros; la de 8 guarda fecha, hora y nota clínica. |
| `database/Datos de Prueba/04_datos_prueba_masivos.sql` | Seed reejecutable con 3.806 pacientes, 3.220 turnos, reparto entre `-7` y `+7` días, 19 franjas horarias y nombres de prueba únicos. |

Para aplicar la migración sobre una base ya creada:

```bash
PGPASSWORD=admin123 psql -h localhost -U postgres -d sistema \
  -v ON_ERROR_STOP=1 -f database/05_seguimiento_turnos.sql
PGPASSWORD=admin123 psql -h localhost -U postgres -d sistema \
  -v ON_ERROR_STOP=1 -f database/CRUD/Insertar/Turno.sql
```

### Reparto de horas y nombres de prueba

El seed anterior calculaba la hora con `numTur % 20` mientras el día usaba
`(numTur - 1001) % 15`. Como 15 y 20 comparten divisor, cada día caía siempre
en las mismas cuatro horas (09:00, 11:30, 14:00 y 16:30). Además, el nombre se
formaba con un único índice `%40`, así que solo existían 40 nombres para 3200
pacientes y la agenda parecía repetir siempre el mismo paciente.

La corrección aplicada tanto al seed como a la migración es:

- día: `(numTur - 1001) % 15`;
- hora: `((numTur - 1001) / 15) % 19` → 19 franjas de 08:00 a 17:00. `%19` es
  coprimo con los 16 médicos, de modo que ningún médico repite hora en un mismo
  día;
- nombre: tres índices (`i % 40`, `((i - 1001) / 40) % 40` y
  `((i - 1001) / 1600) % 40`) → 3200 combinaciones distintas.

La migración solo reescribe `numTur BETWEEN 1001 AND 4200` y
`numHisCli BETWEEN 'P1001' AND 'P4200'`; los turnos creados desde la interfaz
conservan la fecha, hora y nota capturadas.

En el frontend, `#timeline` y `#turnoTimeline` ya no toman los 8 primeros
turnos ordenados por hora (`slice(0, 8)`): muestran primero los que están en
consulta y después una muestra repartida por franjas horarias. Así la vista
rápida no aparenta un único paciente repetido a una sola hora.

### Backend

| Archivo | Responsabilidad |
| --- | --- |
| `service/PanelService.java` | Consultas de pacientes/turnos por rol y `fecha`; estados convertidos; `fin`, `duracion` y `notaClinica`; llama al motor antes de responder. |
| `controller/PanelController.java` | Expone `fecha` como query param en `/api/panel/pacientes` y `/api/panel/turnos`. |
| `service/AltasService.java` | Altas con fecha/hora/nota y edición de turnos (`editarTurno`), con validación de propiedad. |
| `controller/AltasController.java` | `PUT /api/altas/turnos/{numero}`. |
| `service/NotificacionService.java` | Listado, conteo y marcado de leídas; ejecuta el motor. |
| `controller/NotificacionController.java` | `GET /api/notificaciones`, `POST /api/notificaciones/leer`. |
| `service/SeguimientoScheduler.java` | Reloj: `@Scheduled(fixedDelayString="${hospital.seguimiento.intervalo:30000}", initialDelay=5000)`. |
| `HospitalApplication.java` | `@EnableScheduling`. |
| `exception/ApiExceptionHandler.java` | `SecurityException` → HTTP 403 con `{ok:false,error}`. |

### Frontend

| Archivo | Responsabilidad |
| --- | --- |
| `frontend/js/api.js` | `pacientesPanel`/`turnosPanel` con `{fecha, limit, offset}`, `editarTurno`, `notificaciones`, `marcarNotificacionesLeidas`. |
| `frontend/js/dashboard.js` | Estado global con `fecha`, switchers, campana, panel, cola real, menú de tres puntos, modales y polling de 30 s. |
| `frontend/dashboard.html` | Campana/badge/panel, switchers de fecha, estados nuevos, formulario con fecha/hora/nota, menús y modales de edición/eliminación, exportación CSV, etiquetas dinámicas del hospital, selector de tema y buscador de Pacientes sin lupa. Cache bust: `dashboard.js?v=12`, `dashboard.css?v=16`. |
| `frontend/css/dashboard.css` | Estilos de notificaciones, `in_progress`, `no_atendido`, menú de fila, switcher de Pacientes y cuatro temas (`light`, `dark`, `navy`, `sand`). |

---

## 3. Modelo y motor de estados

Columnas nuevas en `Turno`:

```sql
"fecha"        date NOT NULL DEFAULT CURRENT_DATE
"hora"         time NOT NULL DEFAULT CURRENT_TIME
"nota_clinica" varchar(500)
```

`actualizar_turnos_en_vivo()` calcula, para cada turno pendiente/confirmado/en
progreso:

```text
inicio_ts = fecha + hora
fin_ts    = GREATEST(
              COALESCE(LEAD(inicio_ts) OVER (PARTITION BY codMed, fecha
                                            ORDER BY inicio_ts, numTur),
                       inicio_ts + interval '30 minutes'),
              inicio_ts + interval '1 minute')
```

| Momento | Estado resultante | Efecto en `Consulta` | Notificación |
| --- | --- | --- | --- |
| `LOCALTIMESTAMP < inicio_ts` | sin cambio | — | — |
| `inicio_ts <= LOCALTIMESTAMP < fin_ts` | `en_progreso` | — | `inicio` |
| `LOCALTIMESTAMP >= fin_ts` | `finalizado` | `atendido = true`, `causa = NULL` | `finalizacion` |

El usuario puede editar un turno a `no_atendido`. Ese estado sale del conjunto
que procesa el motor, por lo que no se sobrescribe automáticamente. En la
interfaz el estado se muestra como **No atendido** y la consulta queda con
`atendido = false`.

El backfill de la migración solo toca `numTur BETWEEN 1001 AND 4200`, de modo
que los turnos creados desde la interfaz nunca pierden la fecha/hora capturada.

---

## 4. API

Todas las respuestas son JSON con `ok`. Errores: `{ok:false, error:"..."}`.
Se requiere sesión HTTP (`credentials: "include"`).

| Método | Ruta | Parámetros / cuerpo | Uso |
| --- | --- | --- | --- |
| `GET` | `/api/panel/pacientes` | `q`, `fecha`, `limit`, `offset` | `fecha` reduce el directorio a pacientes con turno ese día; sin `fecha` se mantiene el directorio completo. |
| `GET` | `/api/panel/turnos` | `q`, `fecha`, `limit`, `offset` | Filtra la agenda por día y por rol. Devuelve `estatus`, `estadoOriginal`, `hora`, `fin`, `duracion`, `notaClinica`. |
| `POST` | `/api/altas/turnos` | `{fecha, hora, pacienteId, codMed, codUni, estado, notaClinica}` | Alta real. `fecha`/`hora` ausentes caen al día/hora actuales. |
| `PUT` | `/api/altas/turnos/{numero}` | `{fecha?, hora?, estatus?, notaClinica?, atendido?}` | Edita turno. `MEDICO` solo puede editar los suyos; si no, 403. |
| `GET` | `/api/notificaciones?limite=30` | — | Devuelve `rows`, `noLeidas`, `ultimoId`, `alcance`. |
| `POST` | `/api/notificaciones/leer` | `{ids?: number[]}` | Marca las indicadas o todas. Devuelve `actualizadas`. |

Respuesta típica de `/api/panel/turnos`:

```json
{
  "ok": true,
  "data": {
    "rows": [{
      "id": "T-4208",
      "numero": 4208,
      "estatus": "in_progress",
      "estadoOriginal": "en_progreso",
      "fecha": "2026-11-20",
      "hora": "17:05",
      "fin": "17:35",
      "duracion": 30,
      "notaClinica": "Consulta en curso",
      "paciente": "Renata Salas",
      "profesional": "Juan Pérez"
    }],
    "total": 13,
    "alcance": "medico",
    "medico": "Juan Pérez",
    "codMed": "M001",
    "fecha": "2026-11-20"
  }
}
```

---

## 5. Flujo de interfaz implementado

### Resumen

- Las cuatro tarjetas se calculan con los turnos **del día seleccionado** y del
  alcance del rol: total, finalizados, duración promedio y pacientes.
- El pie de total muestra cuántos turnos están `en consulta` y cuántos
  `no_atendido`; el porcentaje de agenda completada usa `done`.
- La agenda y la cola se ordenan por hora real. La cola prioriza lo que ya está
  en consulta y muestra `En consulta` o `HH:MM · N min`.
- `prevDay`/`nextDay`/`agendaToday` cambian `state.fecha` y recargan todo.
- Un clic en un turno de la agenda abre directamente el modal de edición.

### Turnos

- Switcher propio (`turnosPrevDay`, `turnosNextDay`, `turnosToday`) sincronizado
  con el de Resumen.
- Stats: total del día, en consulta, pendientes/confirmados, finalizados y
  no atendidos.
- Filtro de estados con las cinco opciones reales.
- Agenda y tabla muestran fecha real, rango `hora – fin`, nota clínica y chip de
  estado con colores propios para `in_progress` y `no_atendido`.
- Paginación visual de 20 filas.
- Menú de tres puntos por fila con **Editar turno**, **Marcar en consulta**,
  **Marcar finalizado** y **Marcar no atendido**.
- Modal de edición con fecha, hora, estado, nota clínica y casilla
  “La consulta fue exitosa”.

### Pacientes

- Por defecto se muestra el directorio completo (`3.806` para `ADMIN`).
- El switcher `pacientesPrevDay`/`pacientesNextDay` activa
  `pacientesSoloDelDia = true` y filtra por pacientes con turno ese día.
- “Todos” restaura el directorio completo y el subtítulo lo explica.

### Identidad visual y tema

- `hospitalLabel()` resuelve el nombre que se muestra en toda la interfaz:
  `Sistema Hospitalario` para `ADMIN` y el hospital de la ficha del médico para
  `MEDICO` (`state.contexto.hospital.nombre` o `state.user.hospital`).
- `renderHospitalLabels()` sincroniza marca lateral, breadcrumb, pie de página,
  subhead de Resumen, texto de ayuda de Profesionales, hospital en
  Configuración, pills de alcance y `document.title`.
- El buscador de Pacientes de Resumen conserva `#patientSearch` pero ya no
  muestra la lupa decorativa; tampoco existe el botón de búsqueda genérico de la
  barra superior.
- Todas las tarjetas con cantidad usan `setStat()`: animación de 0 al valor
  final en 750 ms con ease-out cúbico, `data-stat-value` para no repetir la
  animación si el valor no cambia y respeto de `animations = off` y
  `body.reduce-motion`.
- Configuración incluye **Tema visual** con cuatro opciones (`light`, `dark`,
  `navy`, `sand`), aplicadas en vivo mediante `body.dataset.theme`. El acento
  elegido se aplica encima con `--teal` y `--teal-dark`, por lo que ambos
  ajustes son independientes. Los cambios se guardan con “Guardar” y “Restablecer”
  devuelve el tema `light`.

### Campana y notificaciones

- La campana de la barra superior muestra badge con el número de no leídas.
- El panel lista mensajes de inicio/finalización con paciente y hora del turno.
- El panel muestra únicamente avisos sin leer. Cada fila tiene una palomita:
  al pulsarla se envía `ids: [id]` a `/api/notificaciones/leer`, esa fila
  desaparece y el badge baja en una unidad; las demás quedan intactas.
- “Marcar todo como leído” envía la petición sin `ids`, actualiza el badge y
  limpia por completo la vista de la campana.
- Cada cambio detectado por el polling produce un aviso emergente (`toast`),
  respetando la preferencia `notifications`.

### Seguimiento en vivo

- `setInterval(..., 30000)` y `visibilitychange` recargan solo los turnos del
  día, cola, agenda, stats y notificaciones.
- El backend también ejecuta el motor al responder `/api/panel/*` y
  `/api/notificaciones`, así que el estado está al día aunque el scheduler no
  haya corrido.

### Altas y permisos

- El modal de alta pide fecha y hora reales, profesional, paciente, estado y
  nota clínica. La unidad se deriva de la ficha del profesional.
- `MEDICO` no ve los botones de **Profesionales**, **Reportes** ni **Nuevo
  médico**; el backend valida el rol de forma adicional.

---

## 6. Análisis de UI: implementado frente a pendiente

| Funcionalidad | Estado | Detalle |
| --- | --- | --- |
| Turnos con fecha/hora/nota reales | ✅ Implementado | Columnas reales, alta y edición persistidas. |
| Estados por reloj del sistema | ✅ Implementado | Motor SQL + scheduler + sincronización al consultar. |
| Fin estimado y duración | ✅ Implementado | `LEAD` por médico/día, 30 min por defecto, mínimo 1 min. |
| Cola de atención | ✅ Implementado | Prioriza `in_progress`, luego hora de inicio; mostrar fecha y duración reales. |
| Notificaciones: campana, badge, panel y toast | ✅ Implementado | Filtradas por rol; palomita por aviso para marcar solo ese y “Marcar todo” limpia la vista. |
| Menú de tres puntos y `no_atendido` | ✅ Implementado | En la tabla de Turnos; también accesible desde la agenda. |
| Cambio de día en Resumen y Turnos | ✅ Implementado | Agendas, tablas, stats, cola y timeline comparten `state.fecha`. |
| Cambio de día en Pacientes | ✅ Implementado | Switcher opcional; “Todos” restaura el directorio. |
| Visibilidad `ADMIN`/`MEDICO` | ✅ Implementado | Backend filtra y frontend oculta módulos administrativos. |
| Nombre del hospital dinámico | ✅ Implementado | `Sistema Hospitalario` para `ADMIN`; hospital de la ficha para `MEDICO`, en marca, breadcrumb, pie, subhead, Configuración y título. |
| Buscador de Resumen sin lupa | ✅ Implementado | `#patientSearch` sigue funcional; se eliminó el icono decorativo y el botón global de búsqueda. |
| Contadores animados | ✅ Implementado | De 0 al valor final en todas las tarjetas numéricas, con opción de animaciones desactivadas. |
| Temas visuales | ✅ Implementado | Claro, oscuro, azul noche y arena cálida, con acento independiente y persistencia local. |
| Paginación de 20 filas | ✅ Implementado | Pacientes y Turnos. |
| Profesionales | ✅ Implementado | Directorio real con búsqueda, filtro por especialidad y alta exclusiva de `ADMIN`. |
| Reportes | ✅ Implementado | Nueve reportes reales del backend con filtros por área. |
| Configuración | ⚠️ Parcial | Preferencias guardadas en `localStorage`; perfil real sobre `"Medico"`; correo, usuario y rol son de solo lectura. |
| Centro de ayuda | ⚠️ Parcial | FAQS y estado de conexión reales; “contacto” y “chat” son mensajes informativos. |
| Calendario mensual | ❌ No implementado | “Ver calendario” solo navega a Turnos; no existe vista mensual. |
| Exportar Turnos/Pacientes/Reportes | ❌ No implementado | Muestran un toast; solo “Resumen de hospitales” genera CSV real. |
| Ficha de paciente | ❌ No implementado | El menú de tres puntos de Pacientes solo muestra un toast. |
| Notificaciones push/websocket | ❌ No implementado | Se usa polling de 30 s, suficiente para la demo. |
| Preferencia `emailDigest` | ❌ No implementado | Se guarda en el navegador pero no dispara correos. |
| Disponibilidad/horarios de médicos | ❌ No implementado | No hay alta de bloqueos ni franjas; el fin se estima del siguiente turno. |
| Auditoría de ediciones | ❌ No implementado | El turno guarda el estado final, no quién lo cambió ni cuándo. |
| Recordatorios previos al turno | ❌ No implementado | Solo se avisa al inicio y al final. |

---

## 7. Flujo pendiente documentado

Los puntos marcados como parciales o no implementados siguen este orden
recomendado:

1. **Exportaciones reales**: generar CSV desde los mismos `rows` del panel y de
   cada reporte; hoy solo `exportReportesCsv()` funciona de verdad.
2. **Ficha de paciente**: sustituir el toast por un modal con datos de
   `"Paciente"`, `"Registro"`, historia clínica y turnos relacionados.
3. **Calendario mensual**: nueva vista que consuma `/api/panel/turnos?fecha=...`
   para cada día del mes, reutilizando el mismo contrato.
4. **Persistencia de preferencias**: mover `clara.dashboard.settings` a una
   tabla por usuario si se necesita sincronización entre dispositivos. El
   interruptor de notificaciones ya gobierna los toasts.
5. **Notificaciones en tiempo real**: sustituir el polling por SSE/WebSocket y
   añadir `emailDigest` real si se requiere correo.
6. **Disponibilidad médica**: tabla de franjas y validación al crear turnos para
   evitar solapamientos; el fin estimado actual es una aproximación.
7. **Auditoría**: tabla de cambios de turno con usuario, fecha y valores
   anteriores, útil para justificar `no_atendido`.
8. **Limpieza de datos de prueba**: borrar los turnos creados durante la
   verificación (`T-4207`, `T-4208`, `T-4209`) antes de una demo; al borrar el
   turno se eliminan en cascada sus consultas y notificaciones.

---

## 8. Verificación realizada

Comandos y comprobaciones efectivas:

- Migración ejecutada con `ON_ERROR_STOP=1`: `UPDATE 3200` turnos,
  `UPDATE 3200` pacientes, `UPDATE 1491` consultas y
  `cambios_iniciales = 0`. Tras la corrección del reparto:
  - `19` franjas por día (08:00–17:00), con 11–12 turnos y la misma cantidad
    de pacientes distintos por franja.
  - `0` combinaciones de médico + día + hora repetidas.
  - `3200` nombres distintos entre `P1001` y `P4200`.
  - `T-4207`, `T-4208` y `T-4209` conservan su fecha/hora original.
- `mvn -o -q clean package` correcto y backend reiniciado con
  `./stop.sh && NO_OPEN=1 ./start.sh`.
- API con sesión `ADMIN`:
  - `3806` pacientes en el directorio completo; `213` con
    `fecha=2026-11-19`.
  - `213` turnos el `2026-11-18`, `213` el `2026-11-19`, `214` el
    `2026-11-25` (antes de los turnos de prueba).
  - `49` notificaciones sin leer antes de marcarlas, `0` después.
- API con sesión `MEDICO`: `13` turnos propios el `2026-11-19` y el
  `2026-11-20`; notificación de inicio/fin del turno de prueba.
- Alta desde la UI: turno con `fecha = 2031-02-02`, `hora = 09:30`,
  paciente `P3900`, médico `M006` y nota “Alta desde interfaz”, persistido como
  `T-4209`.
- Edición desde el menú de tres puntos: `T-4208` pasó a `no_atendido` y luego
  se restauró a `in_progress` con nota clínica modificada; persistencia
  verificada en la tabla.
- Intento de edición ajena con sesión `MEDICO`: HTTP `403` y
  `{"ok":false,"error":"Solo puedes editar tus propios turnos."}`.
- Navegador: login de ambos roles, agendas y tablas por día, campana, badge,
  panel, marcado de leídas, menú de tres puntos, modal de edición y alta con
  fecha/hora. Sin errores de consola propios de la aplicación.
- Reparto visible en la UI: las agendas de Resumen y Turnos muestran 8 turnos en
  8 horas distintas (08:00, 08:30, …, 11:30) con 8 nombres distintos y fines
  estimados de 30 min en lugar de `09:01` repetido. El switcher de Pacientes
  pasa de `3806` a `213` pacientes con turno del día y “Todos” lo restaura.
- Sintaxis JS validada con `node --check` y 228 IDs HTML sin duplicados.
- Verificación UI de la nueva identidad visual:
  - Con `ADMIN`, marca, breadcrumb, pie de página, Configuración, subhead y
    título muestran `Sistema Hospitalario`.
  - Con `MEDICO` (`M001`), los mismos puntos muestran
    `Hospital General de la Ciudad de México`.
  - `#patientSearch` sigue presente; la lupa de Resumen y el botón global de
    búsqueda ya no existen. Sin errores de consola propios.
  - Al cambiar de día, `resumenPacientesValue` anima `3806 → 2 → 202 → 213`
    (valores muestreados durante 750 ms).
  - Los temas `dark`, `navy`, `sand` y `light` cambian `body.dataset.theme` y
    los fondos calculados; “Guardar” persiste el tema y “Restablecer” vuelve a
    `light`.
  - Campana con `54` no leídas y `30` filas visibles: la palomita del aviso
    `145` lo retiró dejando las otras `29` filas intactas y el badge en `53`;
    “Marcar todo como leído” dejó `0` filas, ocultó el badge, deshabilitó el
    botón y mostró “No tienes notificaciones sin leer.” Tras recargar se mantuvo
    vacía.

### Credenciales demo

| Correo | Contraseña | Usuario interno | Rol |
| --- | --- | --- | --- |
| `admin.demo@hospital.test` | `hospital123` | `admin.demo` | `ADMIN` |
| `medico.demo@hospital.test` | `hospital123` | `medico.demo` | `MEDICO` (ficha `M001`) |

> Las sesiones del backend son en memoria: si se reinicia el servidor hay que
> volver a iniciar sesión en el navegador.
