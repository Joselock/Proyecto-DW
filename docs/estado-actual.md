# Estado actual del proyecto

Documento de referencia sobre lo desarrollado hasta ahora en **Proyecto-DW** (Sistema de Gestión Hospitalaria web). La información se basa en el código, scripts y documentación presentes en el repositorio.

> Para el detalle específico del seguimiento de turnos (motor de estados, notificaciones, cambio de día, endpoints y validaciones realizadas), ver [`seguimiento-turnos.md`](seguimiento-turnos.md).

## 1. Visión general

La aplicación es un sistema web hospitalario con:

- **Backend** en Spring Boot que expone una API REST, gestiona la sesión del usuario y sirve los archivos estáticos del frontend.
- **Frontend** en HTML/CSS/JavaScript (sin framework de build) con pantalla de acceso (`index.html`) y panel principal (`dashboard.html`).
- **Base de datos** PostgreSQL con modelo relacional hospitalario, funciones/procedimientos SQL, vistas, triggers, datos de prueba y soporte para seguimiento de turnos en vivo.

## 2. Stack y dependencias principales

### Backend (`backend/`)

- Java 17, Maven, empaquetado JAR (`gestorui` 1.0.0).
- Spring Boot 3.2.5:
  - `spring-boot-starter-web` (API REST y servidor embebido).
  - `spring-boot-starter-data-jpa` (persistencia/JPA).
  - `spring-boot-starter-security` (configuración de seguridad y `BCryptPasswordEncoder`).
- Driver PostgreSQL (`org.postgresql:postgresql`).
- Scheduling habilitado (`@EnableScheduling`) para tareas periódicas.

### Frontend (`frontend/`)

- `index.html`, `dashboard.html`.
- Estilos: `css/index.css`, `css/dashboard.css`.
- Scripts: `js/auth.js`, `js/api.js`, `js/dashboard.js` (módulos ES).
- Recursos locales en `assets/` (Bootstrap Icons, fuentes, imágenes, utilidades JS).

### Base de datos y entorno

- PostgreSQL 17 (según documentación/scripts).
- Configuración local por defecto en `backend/src/resources/application.properties`:
  - `jdbc:postgresql://localhost:5432/sistema`
  - usuario `postgres`
  - `spring.jpa.hibernate.ddl-auto=update` (recomendado usar `validate` en producción y administrar cambios con scripts SQL).
- `docker-compose.yml` incluido para levantar PostgreSQL y backend (revisar coherencia de nombres/credenciales de BD antes de usarlo en un entorno limpio).

## 3. Estructura del repositorio (implementación actual)

```text
Proyecto-DW/
├── backend/                  # API Spring Boot, servicios, controladores y recursos
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/hospital/
│       │   ├── HospitalApplication.java
│       │   ├── config/       # CORS y configuración de seguridad
│       │   ├── controller/   # Auth, Panel, Altas, Reportes, Notificaciones
│       │   ├── exception/    # Manejo de errores API
│       │   ├── model/        # Entidades JPA y de dominio
│       │   ├── repository/   # Repositorios JPA
│       │   └── service/      # Lógica de negocio, panel, altas, auth, reportes, seguimiento
│       └── resources/application.properties
├── database/                 # Esquema, migraciones, CRUD SQL, reportes, triggers y datos
├── docs/                     # Documentación del proyecto
├── frontend/                 # UI web (login/registro y dashboard)
├── docker-compose.yml
├── start.sh / stop.sh        # Arranque y parada local del servidor
└── README.md
```

> Nota: `estructura.txt` describe una estructura objetivo más amplia; la estructura real implementada es la que se observa en el repositorio.

## 4. Ejecución local

### Requisitos

- Java 17 y Maven.
- PostgreSQL en ejecución con la base `sistema` (o ajustar `spring.datasource.*`).
- Esquema y datos cargados según sea necesario (ver `database/README.md`).

### Inicio rápido

```bash
./start.sh
```

- Arranca el backend con `mvn spring-boot:run` desde `backend/`.
- Sirve frontend y API en `http://localhost:8080` (configura `SPRING_WEB_RESOURCES_STATIC_LOCATIONS` hacia `frontend/`).
- Registra PID y log en `.run/backend.pid` y `.run/backend.log`.
- Opciones: `NO_OPEN=1 ./start.sh` para no abrir el navegador, `BACKEND_PORT` para cambiar el puerto.

Para detener el servidor iniciado por el script:

```bash
./stop.sh
```

## 5. Base de datos

El directorio `database/` contiene el modelo y la lógica SQL del sistema. La referencia completa está en [`../database/README.md`](../database/README.md).

### Componentes principales

- **Esquema:** `01_schema.sql`.
- **Vistas:** `03_views.sql`.
- **Seguimiento de turnos:** `05_seguimiento_turnos.sql` (fecha/hora/nota en turnos, tabla de notificaciones y motor de estados).
- **CRUD:** funciones en `CRUD/Insertar`, `CRUD/Modificar`, `CRUD/Eliminar`.
- **Reportes:** consultas/funciones en `Reportes/reporte 1.sql` … `reporte 9.sql`.
- **Triggers:** actualización de informe y límite de médicos en `Trigger/`.
- **Datos de prueba:** `Datos de Prueba/` (incluye `04_datos_prueba_masivos.sql` y otros conjuntos; revisar orden/compatibilidad en el README de base de datos).
- **Migraciones:** scripts en `Migraciones/` y en la raíz de `database/` según el caso documentado.

### Modelo funcional (resumen)

Entidades centrales: `user` y `reset_tokens`; estructura hospitalaria (`Hospital`, `Departamento`, `Unidad`); admisión/pacientes (`Registro`, `Paciente`); operación clínica (`Medico`, `Turno`, `Consulta`, `Informe`) y notificaciones de seguimiento. La relación usuario–médico se modela desde `Medico.username` hacia `user.username`, y el perfil profesional reside en `Medico`.

## 6. Autenticación, sesión y roles

### Implementado en backend

- **Inicio de sesión:** `POST /api/login` (por correo y contraseña; valida credenciales y crea sesión HTTP con `username`, `email` y `role`).
- **Sesión actual:** `GET /api/me`, actualización de perfil `PUT /api/me`.
- **Cierre de sesión:** `POST /api/logout`.
- **Registro:** `POST /api/register` y opciones de registro `GET /api/registro/opciones`.
  - Si no existen usuarios, el flujo contempla la creación del primer administrador.
  - Para cuentas posteriores, el registro se vincula a un médico disponible (hospital → unidad → médico) según las opciones provistas por el backend.
- **Recuperación de contraseña:** `POST /api/forgot-password` y `POST /api/reset-password` (solicitud de recuperación y restablecimiento con token).
- **Seguridad:** Spring Security configurado para permitir las rutas de la aplicación/API, CORS habilitado para orígenes locales y contraseñas cifradas con BCrypt.

### Roles y alcance

- `ADMIN`: acceso global a la información y a operaciones administrativas (según implementación de servicios/controladores).
- `MEDICO`: acceso acotado a sus propios turnos/pacientes y datos asociados a su ficha.
- La visibilidad y permisos se aplican en backend; el frontend adapta la interfaz (módulos/opciones disponibles) al rol.

## 7. Funcionalidades implementadas

### 7.1 Acceso (frontend `index.html` + `auth.js`)

- Pantalla inicial con navegación a inicio de sesión y registro.
- Inicio de sesión con manejo de errores y redirección al dashboard.
- Registro en pasos (selección/vinculación de médico cuando aplica y datos de cuenta).
- Recuperación de contraseña desde la interfaz (solicitud de enlace y flujo de restablecimiento).
- Detección automática del backend API desde el frontend (`js/api.js`, con prioridad al puerto 8080 y uso de cookies de sesión).

### 7.2 Panel principal (`dashboard.html` + `dashboard.js`)

El dashboard integra navegación lateral, cabecera, notificaciones y vistas por módulo. Entre las capacidades observadas/soportadas por la API:

- **Resumen:** métricas y agenda del día, cola/actividad reciente, acceso rápido a turnos y pacientes, cambio de fecha de trabajo.
- **Pacientes:** listado y consulta con alcance por rol, vista de directorio y filtrado por día según selección, paginación visual y acciones soportadas por backend (alta/edición/eliminación vía API de altas).
- **Turnos:** agenda y tabla por día, estados operativos, filtros, paginación, alta de turnos y edición (fecha, hora, estado, nota clínica) según permisos.
- **Profesionales:** módulo orientado a administración; catálogo/directorio y alta/edición/eliminación de médicos disponibles para `ADMIN` según API.
- **Reportes:** consumo de reportes provistos por el backend (resúmenes hospitalarios, listados, consultas, unidades a revisar, etc.) desde la sección correspondiente.
- **Configuración:** preferencias de interfaz y ajustes de usuario (con persistencia local para ciertas opciones de experiencia, como tema/acento/visualización, según implementación del frontend).
- **Centro de ayuda:** sección informativa/soporte dentro del panel.

> El nivel de completitud de cada vista (totalmente funcional, parcial o decorativa) varía por componente. Para turnos, notificaciones y vistas relacionadas, el análisis detallado está en [`seguimiento-turnos.md`](seguimiento-turnos.md).

### 7.3 Seguimiento de turnos y notificaciones

- Estados y horarios reales de turno gestionados con soporte en base de datos y lógica de servicio.
- Sincronización/actualización en vivo mediante scheduler (`SeguimientoScheduler`, cada ~30 s, configurable) y al consultar endpoints relevantes.
- Notificaciones de inicio/finalización con listado, conteo de no leídas y marcado como leídas (`/api/notificaciones`).
- Documentación operativa, pruebas realizadas y pendientes específicos: [`seguimiento-turnos.md`](seguimiento-turnos.md).

## 8. API implementada (resumen)

Todas las rutas se exponen bajo `/api` y, salvo opciones públicas de acceso/registro según flujo, requieren sesión válida. Las respuestas siguen el patrón `{ "ok": true|false, ... }`.

| Área | Endpoints principales | Propósito |
| --- | --- | --- |
| Autenticación | `POST /login`, `POST /register`, `GET /registro/opciones`, `GET /me`, `PUT /me`, `POST /logout`, `POST /forgot-password`, `POST /reset-password` | Acceso, registro, perfil, sesión y recuperación de contraseña. |
| Panel | `GET /panel/pacientes`, `GET /panel/turnos` | Listados para dashboard con filtros (`q`, `fecha`, paginación) y visibilidad por rol. |
| Altas y gestión | `GET /altas/catalogos`, `POST/PUT/DELETE /altas/pacientes...`, `POST/PUT /altas/turnos...`, `POST/PUT/DELETE /altas/medicos...` | Catálogos y operaciones de alta/edición/eliminación según permisos. |
| Notificaciones | `GET /notificaciones`, `POST /notificaciones/leer` | Avisos de turnos, conteo y marcado de leídas. |
| Reportes | `GET /reportes/contexto`, `GET /reportes/profesionales`, `GET /reportes/hospitales/...`, `GET /reportes/proceso`, `GET /reportes/medicos`, `GET /reportes/pacientes...`, `GET /reportes/consultas/...`, `GET /reportes/unidades/turnos-por-revisar` | Contexto y reportes operativos/estadísticos. |

## 9. Pendientes y consideraciones conocidas

- **Documentación de turnos:** pendientes específicos (exportaciones adicionales, ficha de paciente, calendario mensual, notificaciones en tiempo real, auditoría, disponibilidad médica, etc.) están detallados en la sección de análisis/flujo pendiente de [`seguimiento-turnos.md`](seguimiento-turnos.md).
- **Cobertura funcional:** no todos los CRUD o vistas planteadas en estructuras de referencia (`estructura.txt`) están implementados como páginas independientes; el sistema concentra gran parte de la operación en `dashboard.html`.
- **Configuración y despliegue:** conviene validar `docker-compose.yml` (base de datos, nombres y dependencias) frente a la configuración real del backend antes de usarlo como entorno reproducible.
- **Seguridad operativa:** la pantalla de registro crea cuentas (incluido el flujo de primer administrador/vinculación a médico) y debe restringirse adecuadamente si el sistema se expone fuera de un entorno controlado.
- **Persistencia de preferencias:** algunas preferencias de UI se almacenan en el navegador; si se requiere sincronización entre dispositivos, deben persistirse en backend/BD.
- **Datos de prueba:** existen múltiples scripts de datos; revisar compatibilidad y orden de carga en `database/README.md` para evitar duplicados o conflictos.

## 10. Cuentas demo

Si se cargó el conjunto de datos masivos indicado en la documentación de base de datos/seguimiento, existen cuentas de prueba como `admin.demo@hospital.test` y `medico.demo@hospital.test` (contraseña `hospital123`). Ver detalles y alcance en [`../database/README.md`](../database/README.md) y [`seguimiento-turnos.md`](seguimiento-turnos.md).

> Las sesiones del backend son en memoria: tras reiniciar el servidor es necesario volver a iniciar sesión.