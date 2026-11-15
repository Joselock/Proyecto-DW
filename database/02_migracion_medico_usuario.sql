-- ============================================================================
-- PROYECTO-DW · MIGRACIÓN NO DESTRUCTIVA
-- Archivo : database/02_migracion_medico_usuario.sql
--
-- Objetivo:
--   Agregar la relación 1:1 entre "Medico" y "user" sobre una base que ya
--   tiene datos, sin borrar tablas ni información.
--
-- Cuándo usarlo:
--   * Si ya tiene la base creada y solo quiere incorporar la relación.
--   * Si va a crear la base desde cero, use directamente 01_schema.sql.
--
-- Qué hace:
--   1. Verifica que existan las tablas "user" y "Medico".
--   2. Agrega UNIQUE(username) y UNIQUE(email) a "user" si faltan.
--   3. Agrega la columna Medico.username si falta.
--   4. Agrega UNIQUE(username) a "Medico" si falta.
--   5. Agrega la clave foránea Medico.username -> user.username.
--
-- Seguridad:
--   * El script es idempotente: puede ejecutarse varias veces.
--   * Si detecta usuarios o médicos duplicados que impidan crear los UNIQUE,
--     se detiene con un mensaje claro en lugar de borrar datos.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 0. Comprobaciones previas
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public."user"') IS NULL THEN
        RAISE EXCEPTION 'No existe la tabla public."user". Ejecute primero 01_schema.sql.';
    END IF;

    IF to_regclass('public."Medico"') IS NULL THEN
        RAISE EXCEPTION 'No existe la tabla public."Medico". Ejecute primero 01_schema.sql.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public."user"
        GROUP BY username
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay usernames duplicados en public."user". Corríjalos antes de continuar.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public."user"
        GROUP BY email
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay emails duplicados en public."user". Corríjalos antes de continuar.';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 1. UNIQUE(username) en "user"
--    Necesario para que Medico.username pueda ser clave foránea.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public."user"'::regclass
          AND conname = 'user_username_key'
    ) THEN
        ALTER TABLE public."user"
            ADD CONSTRAINT "user_username_key" UNIQUE (username);
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 2. UNIQUE(email) en "user"
--    AuthService busca usuarios por email y espera un resultado único.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public."user"'::regclass
          AND conname = 'user_email_key'
    ) THEN
        ALTER TABLE public."user"
            ADD CONSTRAINT "user_email_key" UNIQUE (email);
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 3. Columna Medico.username
-- ----------------------------------------------------------------------------
ALTER TABLE public."Medico"
    ADD COLUMN IF NOT EXISTS username varchar(100);

COMMENT ON COLUMN public."Medico".username IS
    'Cuenta de usuario (rol MEDICO) vinculada al profesional. Relación 1:1 opcional.';

-- ----------------------------------------------------------------------------
-- 4. UNIQUE(username) en "Medico"
--    Evita que una misma cuenta quede vinculada a dos médicos.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public."Medico"
        WHERE username IS NOT NULL
        GROUP BY username
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Hay usernames duplicados en public."Medico". Corríjalos antes de continuar.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public."Medico"'::regclass
          AND conname = 'Medico_username_key'
    ) THEN
        ALTER TABLE public."Medico"
            ADD CONSTRAINT "Medico_username_key" UNIQUE (username);
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 5. Clave foránea Medico.username -> user.username
--    ON DELETE SET NULL: eliminar la cuenta no elimina el registro médico.
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public."Medico"'::regclass
          AND conname = 'Medico_username_fkey'
    ) THEN
        ALTER TABLE public."Medico"
            ADD CONSTRAINT "Medico_username_fkey" FOREIGN KEY (username)
            REFERENCES public."user" (username)
            ON UPDATE CASCADE
            ON DELETE SET NULL;
    END IF;
END $$;

COMMIT;

-- ============================================================================
-- VINCULACIÓN OPCIONAL DE DATOS EXISTENTES
--
-- Si ya tiene médicos y usuarios cargados, puede intentar vincularlos por
-- nombre. Revise el resultado antes de confirmar: los nombres pueden repetirse.
--
-- UPDATE public."Medico" m
-- SET username = u.username
-- FROM public."user" u
-- JOIN public."data_user" d
--   ON d.username = u.username
--  AND d.email = u.email
-- WHERE u.role = 'MEDICO'
--   AND m.username IS NULL
--   AND lower(btrim(d.nombre)) = lower(btrim(m."nomMed"));
--
-- Después de vincular, verifique que cada usuario haya quedado en un solo
-- médico:
--
-- SELECT username, count(*) AS medicos
-- FROM public."Medico"
-- WHERE username IS NOT NULL
-- GROUP BY username
-- HAVING count(*) > 1;
-- ============================================================================
