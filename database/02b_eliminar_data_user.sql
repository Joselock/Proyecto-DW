-- ============================================================================
-- PROYECTO-DW · MIGRACIÓN NO DESTRUCTIVA (PASO 2 DE 2)
-- Archivo : database/02b_eliminar_data_user.sql
--
-- Objetivo:
--   Eliminar data_user una vez que las cuentas están vinculadas a su médico.
--
--   Antes de borrar la tabla:
--     1. Copia data_user.direccion a "Medico"."dirMed" para cada médico
--        vinculado por username.
--     2. Verifica que no queden cuentas con data_user sin médico asignado.
--        Si quedan, se detiene sin borrar nada y muestra cuántas son.
--     3. Elimina data_user.
--     4. Simplifica la PK de "user" a (username); email queda UNIQUE.
--
-- Requisito:
--   Haber ejecutado antes database/02_migracion_medico_usuario.sql, que crea
--   Medico.username, Medico."dirMed" y la FK.
--
-- Seguridad:
--   * El script es idempotente: si data_user ya no existe, no hace nada.
--   * No borra pacientes, médicos, turnos, consultas ni informes.
--   * Al eliminar data_user también se elimina la vista vw_MedicoUsuario
--     (dependía de esa tabla). Vuelva a crearla con database/03_views.sql.
--
-- Si el script se detiene porque hay cuentas sin médico:
--   1. Vincule esas cuentas:
--        UPDATE public."Medico" m
--        SET username = u.username
--        FROM public."user" u
--        WHERE u.username = 'NOMBRE_DE_LA_CUENTA'
--          AND m.username IS NULL
--          AND lower(btrim(m."nomMed")) = lower(btrim(u.username));
--   2. Vuelva a ejecutar este archivo.
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

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'Medico'
          AND column_name = 'username'
    ) THEN
        RAISE EXCEPTION 'Falta Medico.username. Ejecute primero 02_migracion_medico_usuario.sql.';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 1. Copiar direcciones y comprobar que no queden cuentas sin médico
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_sin_medico integer;
    v_cuentas text;
BEGIN
    IF to_regclass('public."data_user"') IS NULL THEN
        RAISE NOTICE 'data_user no existe; no hay nada que migrar.';
        RETURN;
    END IF;

    EXECUTE $migracion$
        UPDATE public."Medico" m
        SET "dirMed" = d.direccion
        FROM public."data_user" d
        WHERE d.username = m.username
          AND m."dirMed" IS NULL
          AND d.direccion IS NOT NULL
          AND btrim(d.direccion) <> ''
    $migracion$;

    SELECT count(*),
           string_agg(d.username, ', ' ORDER BY d.username)
      INTO v_sin_medico, v_cuentas
      FROM public."data_user" d
      LEFT JOIN public."Medico" m ON m.username = d.username
     WHERE m."codMed" IS NULL;

    IF v_sin_medico > 0 THEN
        RAISE EXCEPTION
            'Hay % cuenta(s) con data_user que todavía no están vinculadas a un médico: %.',
            v_sin_medico, v_cuentas
            USING HINT = 'Vincule esas cuentas con Medico.username y vuelva a ejecutar este archivo.';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 2. Eliminar data_user
--    Sus datos ya están en "Medico": nombre -> nomMed, especialidad ->
--    espMed, teléfono -> telMed y dirección -> dirMed.
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS public."data_user" CASCADE;

-- ----------------------------------------------------------------------------
-- 3. Simplificar la PK de "user" a (username)
--    Se hace después de eliminar data_user porque su FK dependía de la PK
--    compuesta (username, email). email queda como UNIQUE, que es lo que
--    necesitan el login por correo y reset_tokens.
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_pk_name text;
    v_pk_cols text;
BEGIN
    SELECT c.conname,
           string_agg(a.attname, ',' ORDER BY a.attname)
      INTO v_pk_name, v_pk_cols
      FROM pg_constraint c
      JOIN pg_attribute a
        ON a.attrelid = c.conrelid
       AND a.attnum = ANY (c.conkey)
     WHERE c.conrelid = 'public."user"'::regclass
       AND c.contype = 'p'
     GROUP BY c.conname;

    IF v_pk_cols = 'email,username' THEN
        EXECUTE format('ALTER TABLE public."user" DROP CONSTRAINT %I', v_pk_name);
        ALTER TABLE public."user"
            ADD CONSTRAINT "user_pkey" PRIMARY KEY (username);
    END IF;
END $$;

COMMIT;

-- ============================================================================
-- Después de ejecutar este archivo:
--   psql -U postgres -d sistema -f database/03_views.sql
-- para recrear vw_MedicoUsuario sin la dependencia de data_user.
-- ============================================================================
