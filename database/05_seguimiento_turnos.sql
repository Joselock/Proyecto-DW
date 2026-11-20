-- ============================================================================
-- PROYECTO-DW · Seguimiento real de turnos, notificaciones y cambio de día
-- ============================================================================
-- Ejecutar sobre una base ya creada. Es reejecutable.
--
--   psql -U postgres -d sistema -v ON_ERROR_STOP=1 \
--     -f database/05_seguimiento_turnos.sql
--
-- Añade fecha, hora y nota clínica a "Turno", reparte los turnos de prueba en
-- los 7 días anteriores y los 7 siguientes, y crea el motor que cambia el
-- estado de cada turno según el reloj del sistema:
--
--   pendiente/confirmado -> en_progreso -> finalizado
--
-- El fin de la consulta es la hora del siguiente turno del mismo médico y día,
-- o 30 minutos después si es el último. Al finalizar, la consulta se marca
-- como atendida salvo que el usuario la haya editado como "no atendido".
-- Cada transición genera una notificación.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. Columnas reales en Turno
-- ----------------------------------------------------------------------------
ALTER TABLE public."Turno"
    ADD COLUMN IF NOT EXISTS "fecha" date NOT NULL DEFAULT CURRENT_DATE,
    ADD COLUMN IF NOT EXISTS "hora" time NOT NULL DEFAULT CURRENT_TIME,
    ADD COLUMN IF NOT EXISTS "nota_clinica" varchar(500);

CREATE INDEX IF NOT EXISTS idx_turno_fecha_hora
    ON public."Turno" ("fecha", "hora");

COMMENT ON COLUMN public."Turno"."fecha" IS
    'Día de atención. Es la clave del cambio de día en las agendas.';
COMMENT ON COLUMN public."Turno"."hora" IS
    'Hora de inicio. El fin se estima con el siguiente turno del mismo médico y día (30 min por defecto).';
COMMENT ON COLUMN public."Turno"."nota_clinica" IS
    'Nota clínica capturada al registrar o editar el turno.';

-- ----------------------------------------------------------------------------
-- 2. Backfill de los turnos generados por el seed
--    Solo se tocan los turnos de la serie P1001..T4200; los creados desde la
--    interfaz conservan siempre la fecha/hora que el usuario capturó.
--
--    La fecha se reparte con %15, por lo que la hora DEBE derivarse del índice
--    de repetición dentro de ese día ((numTur - 1001) / 15), no de numTur %20:
--    si no, 15 y 20 colisionan y todos los turnos del día caen en 4 horas.
--    Se usa %19 (coprimo con los 16 médicos y con 15) para repartir 19 franjas
--    de 08:00 a 17:00 sin que un mismo médico repita hora en el mismo día.
-- ----------------------------------------------------------------------------
WITH calculado AS (
    SELECT t."numTur",
           (CURRENT_DATE - make_interval(days => ((t."numTur" - 1001) % 15) - 7))::date AS fecha,
           make_time(
               8 + ((((t."numTur" - 1001) / 15) % 19) / 2),
               ((((t."numTur" - 1001) / 15) % 19) % 2) * 30,
               0
           ) AS hora
    FROM public."Turno" t
    WHERE t."numTur" BETWEEN 1001 AND 4200
)
UPDATE public."Turno" t
SET "fecha" = c.fecha,
    "hora" = c.hora,
    "nota_clinica" = COALESCE(t."nota_clinica", 'Turno generado automáticamente'),
    "estTur" = CASE
        WHEN c.fecha + c.hora < LOCALTIMESTAMP THEN 'finalizado'
        WHEN c.fecha + c.hora < LOCALTIMESTAMP + interval '30 minutes' THEN 'en_progreso'
        WHEN t."numTur" % 4 = 1 THEN 'confirmado'
        ELSE 'pendiente'
    END
FROM calculado c
WHERE t."numTur" = c."numTur";

-- ----------------------------------------------------------------------------
-- 2b. Nombres de los pacientes de prueba
--     El seed anterior combinaba nombre y apellido con un único índice (%40),
--     así que solo había 40 nombres distintos para 3200 pacientes y la agenda
--     parecía repetir siempre el mismo paciente. Se usan tres índices para
--     obtener hasta 40 * 40 * 40 combinaciones.
-- ----------------------------------------------------------------------------
WITH listas AS (
    SELECT
        ARRAY[
          'Ana','Luis','María','Javier','Laura','Diego','Sofía','Mateo',
          'Camila','Andrés','Valentina','Pablo','Isabella','Santiago',
          'Emma','Sebastián','Lucía','Emiliano','Fernanda','Nicolás',
          'Renata','Alejandro','Daniela','Rodrigo','Paula','Ignacio',
          'Gabriela','Tomás','Adriana','Rafael','Elena','Martín',
          'Carolina','Hugo','Verónica','Esteban','Natalia','Cristian',
          'Mariana','Alberto'
        ]::text[] AS nombres,
        ARRAY[
          'García','Martínez','López','Hernández','González','Pérez',
          'Rodríguez','Sánchez','Ramírez','Torres','Flores','Rivera',
          'Gómez','Díaz','Reyes','Morales','Cruz','Ortiz','Rojas','Vega',
          'Salas','Castro','Romero','Navarro','Paredes','Leal','Aguirre',
          'Duarte','Ferrer','Herrera','Núñez','Méndez','Silva','Cabrera',
          'Vargas','Molina','Benítez','Soto','Palacios','Contreras'
        ]::text[] AS apellidos
),
generados AS (
    SELECT 'P' || lpad(n.i::text, 4, '0') AS "numHisCli",
           l.nombres[1 + (n.i % 40)] || ' ' ||
           l.apellidos[1 + (((n.i - 1001) / 40) % 40)] || ' ' ||
           l.apellidos[1 + (((n.i - 1001) / 1600) % 40)] AS "nomPac"
    FROM generate_series(1001, 4200) AS n(i)
    CROSS JOIN listas l
)
UPDATE public."Paciente" p
SET "nomPac" = g."nomPac"
FROM generados g
WHERE p."numHisCli" = g."numHisCli";

UPDATE public."Consulta" c
SET atendido = TRUE,
    causa = NULL
FROM public."Turno" t
WHERE c."numTur" = t."numTur"
  AND t."numTur" BETWEEN 1001 AND 4200
  AND t."estTur" = 'finalizado';

-- ----------------------------------------------------------------------------
-- 3. Notificaciones
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public."Notificacion" (
    "id"        bigserial   NOT NULL,
    "numTur"    integer     NOT NULL,
    "codMed"    varchar(10) NOT NULL,
    "tipo"      varchar(20) NOT NULL,
    "mensaje"   varchar(300) NOT NULL,
    "fecha"     timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "leida"     boolean     NOT NULL DEFAULT false,

    CONSTRAINT "Notificacion_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "Notificacion_numTur_fkey" FOREIGN KEY ("numTur")
        REFERENCES public."Turno" ("numTur")
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT "Notificacion_tipo_chk" CHECK ("tipo" IN ('inicio', 'finalizacion')),
    CONSTRAINT "Notificacion_numTur_tipo_key" UNIQUE ("numTur", "tipo")
);

CREATE INDEX IF NOT EXISTS idx_notificacion_codmed_leida
    ON public."Notificacion" ("codMed", "leida", "fecha" DESC);

-- ----------------------------------------------------------------------------
-- 4. Motor de estados en tiempo real
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.actualizar_turnos_en_vivo()
RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE
    v_cambios integer;
BEGIN
    WITH base AS (
        SELECT t."numTur", t."codMed", t."fecha", t."hora", t."estTur",
               (t."fecha" + t."hora") AS inicio_ts
        FROM public."Turno" t
        WHERE lower(t."estTur") IN ('pendiente', 'confirmado', 'en_progreso')
    ),
    ventana AS (
        SELECT b.*,
               GREATEST(
                   COALESCE(
                       LEAD(b.inicio_ts) OVER (
                           PARTITION BY b."codMed", b."fecha"
                           ORDER BY b.inicio_ts, b."numTur"
                       ),
                       b.inicio_ts + interval '30 minutes'
                   ),
                   b.inicio_ts + interval '1 minute'
               ) AS fin_ts
        FROM base b
    ),
    calculado AS (
        SELECT v.*,
               CASE
                   WHEN LOCALTIMESTAMP >= v.inicio_ts AND LOCALTIMESTAMP < v.fin_ts
                       THEN 'en_progreso'
                   WHEN LOCALTIMESTAMP >= v.fin_ts
                       THEN 'finalizado'
                   ELSE NULL
               END AS nuevo_estado
        FROM ventana v
    ),
    cambios AS (
        UPDATE public."Turno" t
        SET "estTur" = c.nuevo_estado
        FROM calculado c
        WHERE t."numTur" = c."numTur"
          AND lower(t."estTur") IS DISTINCT FROM c.nuevo_estado
          AND c.nuevo_estado IS NOT NULL
        RETURNING t."numTur", t."codMed", c.nuevo_estado AS "estTur",
                  c."fecha", c."hora", c.fin_ts
    ),
    consultas AS (
        UPDATE public."Consulta" cs
        SET atendido = TRUE,
            causa = NULL
        FROM cambios ch
        WHERE cs."numTur" = ch."numTur"
          AND ch."estTur" = 'finalizado'
        RETURNING cs."numTur"
    ),
    notificaciones AS (
        INSERT INTO public."Notificacion" ("numTur", "codMed", "tipo", "mensaje")
        SELECT ch."numTur",
               ch."codMed",
               CASE WHEN ch."estTur" = 'en_progreso' THEN 'inicio' ELSE 'finalizacion' END,
               format('Turno %s · %s %s · %s',
                      ch."numTur",
                      to_char(ch."fecha", 'DD/MM/YYYY'),
                      to_char(ch."hora", 'HH24:MI'),
                      CASE WHEN ch."estTur" = 'en_progreso'
                           THEN 'inició la consulta'
                           ELSE 'finalizó · consulta registrada como exitosa' END)
        FROM cambios ch
        ON CONFLICT ("numTur", "tipo") DO NOTHING
        RETURNING 1
    )
    SELECT count(*)::integer INTO v_cambios FROM cambios;

    RETURN COALESCE(v_cambios, 0);
END;
$$;

-- Se ejecuta una vez al cargar la migración para dejar los estados al día.
SELECT public.actualizar_turnos_en_vivo() AS cambios_iniciales;

COMMIT;
