-- ============================================================================
-- PROYECTO-DW · SISTEMA DE GESTIÓN HOSPITALARIA
-- Archivo : database/01_schema.sql
-- Motor   : PostgreSQL 17
--
-- Objetivo:
--   Esquema relacional completo del sistema. Incluye:
--     * Las tablas del dominio hospitalario del ERD entregado.
--     * La tabla "Unidad", que faltaba en el ERD pero ya es usada por las
--       funciones de database/CRUD y database/Reportes.
--     * Las tablas de usuarios que ya existían: "user" y "data_user".
--     * La tabla "reset_tokens" documentada en el proyecto.
--     * La relación 1:1 entre "Medico" y "user" mediante Medico.username.
--
-- IMPORTANTE:
--   1. Este script RECREA el esquema. Elimina las tablas del esquema public
--      antes de crearlas. Úselo en desarrollo o sobre una base vacía.
--   2. Los nombres de tablas y columnas del dominio conservan el estilo
--      PascalCase/camelCase entre comillas que ya usan las funciones
--      PL/pgSQL (por ejemplo public."Medico"("codMed")). No se deben pasar
--      a minúsculas: las funciones existentes dejarían de funcionar.
--   3. "user" es palabra reservada de PostgreSQL. Siempre debe escribirse
--      entre comillas dobles: public."user".
-- ============================================================================

SET client_encoding = 'UTF8';

BEGIN;

-- ----------------------------------------------------------------------------
-- 0. LIMPIEZA
--    Se elimina en orden inverso a las dependencias.
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public."reset_tokens" CASCADE;
DROP TABLE IF EXISTS public."Informe" CASCADE;
DROP TABLE IF EXISTS public."Consulta" CASCADE;
DROP TABLE IF EXISTS public."Turno" CASCADE;
DROP TABLE IF EXISTS public."Medico" CASCADE;
DROP TABLE IF EXISTS public."Paciente" CASCADE;
DROP TABLE IF EXISTS public."Registro" CASCADE;
DROP TABLE IF EXISTS public."Unidad" CASCADE;
DROP TABLE IF EXISTS public."Departamento" CASCADE;
DROP TABLE IF EXISTS public."Hospital" CASCADE;
DROP TABLE IF EXISTS public."data_user" CASCADE;
DROP TABLE IF EXISTS public."user" CASCADE;
DROP SEQUENCE IF EXISTS public.seq_num_informe;

-- Secuencia usada por el trigger actualizar_informe_trigger() para generar
-- el número de informe cuando se crea uno nuevo.
CREATE SEQUENCE public.seq_num_informe
    AS bigint
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- ============================================================================
-- 1. SEGURIDAD Y USUARIOS
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1.1 user
--     Credenciales de acceso. La clave primaria compuesta (username, email)
--     es la que espera la entidad JPA User (@IdClass UserId).
--
--     Se agregan restricciones UNIQUE individuales porque:
--       * AuthService busca por username y por email por separado.
--       * La relación Medico -> user se apoya en username, que debe ser
--         único para poder ser referenciado por una clave foránea.
-- ----------------------------------------------------------------------------
CREATE TABLE public."user" (
    username varchar(100) NOT NULL,
    email    varchar(100) NOT NULL,
    password varchar(255) NOT NULL,
    role     varchar(20)  DEFAULT 'MEDICO',

    CONSTRAINT "user_pkey" PRIMARY KEY (username, email),
    CONSTRAINT "user_username_key" UNIQUE (username),
    CONSTRAINT "user_email_key" UNIQUE (email),

    CONSTRAINT "user_username_chk" CHECK (btrim(username) <> ''),
    CONSTRAINT "user_email_chk" CHECK (btrim(email) <> '' AND position('@' IN email) > 1),
    CONSTRAINT "user_role_chk" CHECK (
        role IS NULL OR upper(role) IN ('ADMIN', 'MEDICO')
    )
);

COMMENT ON TABLE public."user" IS
    'Credenciales y rol de acceso al sistema. Rol permitido: ADMIN o MEDICO.';
COMMENT ON COLUMN public."user".role IS
    'ADMIN: acceso total. MEDICO: operación diaria. NULL se interpreta como MEDICO.';

-- ----------------------------------------------------------------------------
-- 1.2 data_user
--     Perfil extendido de la cuenta. Mantiene la misma clave compuesta que
--     la entidad JPA DataUser: (username, email).
-- ----------------------------------------------------------------------------
CREATE TABLE public."data_user" (
    username     varchar(100) NOT NULL,
    email        varchar(100) NOT NULL,
    nombre       varchar(100),
    especialidad varchar(100),
    direccion    varchar(100),
    telefono     varchar(20),

    CONSTRAINT "data_user_pkey" PRIMARY KEY (username, email),
    CONSTRAINT "data_user_user_fkey" FOREIGN KEY (username, email)
        REFERENCES public."user" (username, email)
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

COMMENT ON TABLE public."data_user" IS
    'Perfil de la cuenta: nombre, especialidad, dirección y teléfono de contacto.';

-- ----------------------------------------------------------------------------
-- 1.3 reset_tokens
--     Soporte para recuperación de contraseña (previsto en la documentación).
--     Si el proyecto no lo usa todavía, la tabla puede quedarse vacía.
-- ----------------------------------------------------------------------------
CREATE TABLE public."reset_tokens" (
    id               bigint GENERATED BY DEFAULT AS IDENTITY,
    email            varchar(100) NOT NULL,
    token            varchar(255) NOT NULL,
    fecha_creacion   timestamptz NOT NULL DEFAULT now(),
    fecha_expiracion timestamptz NOT NULL,
    usado            boolean NOT NULL DEFAULT false,

    CONSTRAINT "reset_tokens_pkey" PRIMARY KEY (id),
    CONSTRAINT "reset_tokens_token_key" UNIQUE (token),
    CONSTRAINT "reset_tokens_email_fkey" FOREIGN KEY (email)
        REFERENCES public."user" (email)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "reset_tokens_fechas_chk" CHECK (fecha_expiracion > fecha_creacion)
);

CREATE INDEX idx_reset_tokens_email ON public."reset_tokens" (email);

COMMENT ON TABLE public."reset_tokens" IS
    'Tokens temporales para recuperación de contraseña.';

-- ============================================================================
-- 2. ESTRUCTURA HOSPITALARIA
--    Hospital 1──N Departamento 1──N Unidad
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 2.1 Hospital
-- ----------------------------------------------------------------------------
CREATE TABLE public."Hospital" (
    "codHos" varchar(10) NOT NULL,
    "nomHos" varchar(50) NOT NULL,

    CONSTRAINT "Hospital_pkey" PRIMARY KEY ("codHos"),
    CONSTRAINT "Hospital_codHos_chk" CHECK (btrim("codHos") <> ''),
    CONSTRAINT "Hospital_nomHos_chk" CHECK (btrim("nomHos") <> '')
);

COMMENT ON TABLE public."Hospital" IS
    'Nodo superior de la estructura hospitalaria.';

-- ----------------------------------------------------------------------------
-- 2.2 Departamento
-- ----------------------------------------------------------------------------
CREATE TABLE public."Departamento" (
    "codDep" varchar(10) NOT NULL,
    "nomDep" varchar(50) NOT NULL,
    "codHos" varchar(10) NOT NULL,

    CONSTRAINT "Departamento_pkey" PRIMARY KEY ("codDep"),
    CONSTRAINT "Departamento_codHos_fkey" FOREIGN KEY ("codHos")
        REFERENCES public."Hospital" ("codHos")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Departamento_codDep_chk" CHECK (btrim("codDep") <> ''),
    CONSTRAINT "Departamento_nomDep_chk" CHECK (btrim("nomDep") <> '')
);

CREATE INDEX idx_departamento_codhos ON public."Departamento" ("codHos");

COMMENT ON TABLE public."Departamento" IS
    'División de un hospital.';

-- ----------------------------------------------------------------------------
-- 2.3 Unidad
--     Tabla ausente en el ERD adjunto. Todas las funciones del proyecto
--     (médicos, pacientes, turnos, informes y reportes) la necesitan.
-- ----------------------------------------------------------------------------
CREATE TABLE public."Unidad" (
    "codUni" varchar(10)  NOT NULL,
    "nomUni" varchar(50)  NOT NULL,
    "ubiUni" varchar(100) NOT NULL,
    "codDep" varchar(10)  NOT NULL,

    CONSTRAINT "Unidad_pkey" PRIMARY KEY ("codUni"),
    CONSTRAINT "Unidad_codDep_fkey" FOREIGN KEY ("codDep")
        REFERENCES public."Departamento" ("codDep")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Unidad_codUni_chk" CHECK (btrim("codUni") <> ''),
    CONSTRAINT "Unidad_nomUni_chk" CHECK (btrim("nomUni") <> ''),
    CONSTRAINT "Unidad_ubiUni_chk" CHECK (btrim("ubiUni") <> '')
);

CREATE INDEX idx_unidad_coddep ON public."Unidad" ("codDep");

COMMENT ON TABLE public."Unidad" IS
    'Área operativa de un departamento. Agrupa médicos, registros, turnos e informes.';

-- ============================================================================
-- 3. PACIENTES Y ADMISIÓN
--    Registro 1──1 Paciente
--    Registro es la admisión actual del paciente en una unidad; Paciente es
--    su ficha demográfica. Esta dirección mantiene compatibles los scripts:
--    insertarPaciente inserta primero Registro y después Paciente, y
--    eliminarPaciente borra Registro esperando el borrado en cascada.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 3.1 Registro
--     Un paciente tiene una admisión vigente (PK numHisCli). Si en el futuro
--     se necesita historial de admisiones, la PK debería ampliarse.
-- ----------------------------------------------------------------------------
CREATE TABLE public."Registro" (
    "estPac"          varchar(50) NOT NULL,
    "codUni"          varchar(10) NOT NULL,
    "numHisCli"       varchar(10) NOT NULL,
    fecha_admision    date        NOT NULL DEFAULT CURRENT_DATE,

    CONSTRAINT "Registro_pkey" PRIMARY KEY ("numHisCli"),
    CONSTRAINT "Registro_codUni_fkey" FOREIGN KEY ("codUni")
        REFERENCES public."Unidad" ("codUni")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Registro_estPac_chk" CHECK (btrim("estPac") <> ''),
    CONSTRAINT "Registro_fecha_admision_chk" CHECK (fecha_admision >= DATE '1900-01-01')
);

CREATE INDEX idx_registro_coduni ON public."Registro" ("codUni");

COMMENT ON TABLE public."Registro" IS
    'Admisión vigente de un paciente en una unidad. Es la raíz de Paciente y Consulta.';

-- ----------------------------------------------------------------------------
-- 3.2 Paciente
--     La PK y la FK hacia Registro corrigen el ERD, que no definía clave
--     primaria para esta tabla.
--     CHECK de edad mínima: 16 años (regla de negocio del proyecto).
-- ----------------------------------------------------------------------------
CREATE TABLE public."Paciente" (
    "numHisCli" varchar(10)  NOT NULL,
    "nomPac"    varchar(100) NOT NULL,
    "dirPac"    varchar(100),
    "fechaN"    date         NOT NULL,

    CONSTRAINT "Paciente_pkey" PRIMARY KEY ("numHisCli"),
    CONSTRAINT "Paciente_numHisCli_fkey" FOREIGN KEY ("numHisCli")
        REFERENCES public."Registro" ("numHisCli")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Paciente_nomPac_chk" CHECK (btrim("nomPac") <> ''),
    CONSTRAINT "Paciente_dirPac_chk" CHECK ("dirPac" IS NULL OR btrim("dirPac") <> ''),
    CONSTRAINT "Paciente_fechaN_chk" CHECK (
        "fechaN" <= (CURRENT_DATE - INTERVAL '16 years')::date
    )
);

COMMENT ON TABLE public."Paciente" IS
    'Ficha demográfica del paciente. Se crea junto con su Registro de admisión.';
COMMENT ON CONSTRAINT "Paciente_fechaN_chk" ON public."Paciente" IS
    'El paciente debe tener al menos 16 años.';

-- ============================================================================
-- 4. PERSONAL MÉDICO Y RELACIÓN CON "user"
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 4.1 Medico
--     Relación 1:1 opcional con "user":
--       * Medico.username -> user.username
--       * UNIQUE(username): una cuenta de usuario no puede estar vinculada a
--         dos médicos.
--       * DELETE SET NULL: si se elimina la cuenta, el registro profesional
--         se conserva y queda sin usuario.
--
--     El usuario vinculado debe tener rol MEDICO. Esa validación de rol se
--     deja a la aplicación para no impedir promociones a ADMIN.
-- ----------------------------------------------------------------------------
CREATE TABLE public."Medico" (
    "codMed"  varchar(10)  NOT NULL,
    "nomMed"  varchar(50)  NOT NULL,
    "espMed"  varchar(50)  NOT NULL,
    "numLic"  varchar(20)  NOT NULL,
    "telMed"  varchar(20)  NOT NULL,
    "codUni"  varchar(10)  NOT NULL,
    "exp"     integer      NOT NULL DEFAULT 0,
    "datCon"  varchar(100) NOT NULL,
    username  varchar(100),

    CONSTRAINT "Medico_pkey" PRIMARY KEY ("codMed"),
    CONSTRAINT "Medico_numLic_key" UNIQUE ("numLic"),
    CONSTRAINT "Medico_username_key" UNIQUE (username),
    CONSTRAINT "Medico_codUni_fkey" FOREIGN KEY ("codUni")
        REFERENCES public."Unidad" ("codUni")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Medico_username_fkey" FOREIGN KEY (username)
        REFERENCES public."user" (username)
        ON UPDATE CASCADE
        ON DELETE SET NULL,
    CONSTRAINT "Medico_exp_chk" CHECK ("exp" >= 0),
    CONSTRAINT "Medico_codMed_chk" CHECK (btrim("codMed") <> ''),
    CONSTRAINT "Medico_nomMed_chk" CHECK (btrim("nomMed") <> ''),
    CONSTRAINT "Medico_espMed_chk" CHECK (btrim("espMed") <> ''),
    CONSTRAINT "Medico_numLic_chk" CHECK (btrim("numLic") <> ''),
    CONSTRAINT "Medico_telMed_chk" CHECK (btrim("telMed") <> ''),
    CONSTRAINT "Medico_datCon_chk" CHECK (btrim("datCon") <> '')
);

CREATE INDEX idx_medico_coduni ON public."Medico" ("codUni");

COMMENT ON TABLE public."Medico" IS
    'Profesional médico asignado a una unidad.';
COMMENT ON COLUMN public."Medico".username IS
    'Cuenta de usuario (rol MEDICO) vinculada al profesional. Relación 1:1 opcional.';

-- ============================================================================
-- 5. OPERACIÓN: TURNOS, CONSULTAS E INFORMES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 5.1 Turno
-- ----------------------------------------------------------------------------
CREATE TABLE public."Turno" (
    "numTur"       integer     NOT NULL,
    "cantAtenTur"  integer     NOT NULL DEFAULT 0,
    "estTur"       varchar(50) NOT NULL,
    "codUni"       varchar(10) NOT NULL,
    "codMed"       varchar(10) NOT NULL,

    CONSTRAINT "Turno_pkey" PRIMARY KEY ("numTur"),
    CONSTRAINT "Turno_codUni_fkey" FOREIGN KEY ("codUni")
        REFERENCES public."Unidad" ("codUni")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Turno_codMed_fkey" FOREIGN KEY ("codMed")
        REFERENCES public."Medico" ("codMed")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Turno_numTur_chk" CHECK ("numTur" > 0),
    CONSTRAINT "Turno_cantAtenTur_chk" CHECK ("cantAtenTur" >= 0),
    CONSTRAINT "Turno_estTur_chk" CHECK (btrim("estTur") <> '')
);

CREATE INDEX idx_turno_coduni ON public."Turno" ("codUni");
CREATE INDEX idx_turno_codmed ON public."Turno" ("codMed");

COMMENT ON TABLE public."Turno" IS
    'Turno de atención de una unidad, asignado a un médico.';

-- ----------------------------------------------------------------------------
-- 5.2 Consulta
--     PK compuesta: un paciente solo puede aparecer una vez en un turno.
--     La FK apunta a Registro, igual que en el ERD entregado, para que al
--     eliminar la admisión se eliminen también sus consultas.
-- ----------------------------------------------------------------------------
CREATE TABLE public."Consulta" (
    "numTur"   integer     NOT NULL,
    "numHisCli" varchar(10) NOT NULL,
    "atendido" boolean     NOT NULL DEFAULT false,
    "causa"    varchar(200),

    CONSTRAINT "Consulta_pkey" PRIMARY KEY ("numTur", "numHisCli"),
    CONSTRAINT "Consulta_numTur_fkey" FOREIGN KEY ("numTur")
        REFERENCES public."Turno" ("numTur")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Consulta_numHisCli_fkey" FOREIGN KEY ("numHisCli")
        REFERENCES public."Registro" ("numHisCli")
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

CREATE INDEX idx_consulta_numhiscli ON public."Consulta" ("numHisCli");

COMMENT ON TABLE public."Consulta" IS
    'Atención de un paciente en un turno. La PK evita duplicar paciente/turno.';
COMMENT ON COLUMN public."Consulta".causa IS
    'Motivo por el que no fue atendido: Extranjero, FueraProvincia, OtraUnidad, OtrasCausas, etc.';

-- ----------------------------------------------------------------------------
-- 5.3 Informe
--     Un informe por turno (UNIQUE numTur). El trigger
--     actualizar_informe_trigger() lo crea o actualiza automáticamente.
--     numIn conserva el tipo varchar para no romper eliminarInforme() ni
--     las funciones de reportes que lo devuelven como texto.
-- ----------------------------------------------------------------------------
CREATE TABLE public."Informe" (
    "hora"    time with time zone NOT NULL DEFAULT CURRENT_TIME,
    "fecha"   date                NOT NULL DEFAULT CURRENT_DATE,
    "numIn"   varchar(10)         NOT NULL DEFAULT (nextval('public.seq_num_informe'::regclass))::text,
    "pacAten" integer             NOT NULL DEFAULT 0,
    "pacAlt"  integer             NOT NULL DEFAULT 0,
    "cantAdm" integer             NOT NULL DEFAULT 0,
    "total"   integer             NOT NULL DEFAULT 0,
    "codUni"  varchar(10)         NOT NULL,
    "numTur"  integer             NOT NULL,

    CONSTRAINT "Informe_pkey" PRIMARY KEY ("numIn"),
    CONSTRAINT "Informe_numTur_key" UNIQUE ("numTur"),
    CONSTRAINT "Informe_codUni_fkey" FOREIGN KEY ("codUni")
        REFERENCES public."Unidad" ("codUni")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Informe_numTur_fkey" FOREIGN KEY ("numTur")
        REFERENCES public."Turno" ("numTur")
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT "Informe_contadores_chk" CHECK (
        "pacAten" >= 0 AND "pacAlt" >= 0 AND "cantAdm" >= 0 AND "total" >= 0
    )
);

CREATE INDEX idx_informe_coduni ON public."Informe" ("codUni");

COMMENT ON TABLE public."Informe" IS
    'Resumen automático de la actividad de un turno en una unidad.';

COMMIT;

-- ============================================================================
-- FIN DEL ESQUEMA
--
-- Después de ejecutar este archivo, ejecute en este orden:
--   * database/CRUD/Insertar/*.sql
--   * database/CRUD/Modificar/*.sql
--   * database/CRUD/Eliminar/*.sql
--   * database/Datos de Prueba/deepseek_sql_20260622_f6000b.sql
--   * database/Datos de Prueba/deepseek_sql_20260622_66b3c9.sql
--   * database/Trigger/Function-*.sql
--   * database/Trigger/trigger-*.sql
--   * database/Reportes/*.sql
--   * database/03_views.sql
--
-- Los disparadores se crean DESPUÉS de los datos de prueba para evitar que
-- trg_actualizar_informe cree informes que choquen con UNIQUE("numTur").
--
-- Si va a actualizar una base que ya tiene datos, use en su lugar
-- database/02_migracion_medico_usuario.sql.
-- ============================================================================
