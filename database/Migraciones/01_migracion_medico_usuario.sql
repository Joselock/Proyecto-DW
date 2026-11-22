-- ============================================================================
-- Objetivo:
--   Adaptar una base que ya tiene datos al modelo en el que el usuario se
--   vincula con un médico y los datos del perfil viven en "Medico".
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
-- 1. UNIQUE(email) en "user"
--    Necesario para el login por correo y para reset_tokens.
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
-- 2. Columna Medico.username
-- ----------------------------------------------------------------------------
ALTER TABLE public."Medico"
    ADD COLUMN IF NOT EXISTS username varchar(100);

COMMENT ON COLUMN public."Medico".username IS
    'Cuenta de usuario (rol MEDICO) vinculada al profesional. Relación 1:1 opcional.';

-- ----------------------------------------------------------------------------
-- 3. UNIQUE(username) en "Medico"
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
-- 4. Clave foránea Medico.username -> user.username
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

-- ----------------------------------------------------------------------------
-- 5. Columna Medico.dirMed
--    Reemplaza data_user.direccion.
-- ----------------------------------------------------------------------------
ALTER TABLE public."Medico"
    ADD COLUMN IF NOT EXISTS "dirMed" varchar(100);

COMMENT ON COLUMN public."Medico"."dirMed" IS
    'Dirección de contacto del profesional. Reemplaza data_user.direccion.';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public."Medico"'::regclass
          AND conname = 'Medico_dirMed_chk'
    ) THEN
        ALTER TABLE public."Medico"
            ADD CONSTRAINT "Medico_dirMed_chk"
            CHECK ("dirMed" IS NULL OR btrim("dirMed") <> '');
    END IF;
END $$;

COMMIT;

