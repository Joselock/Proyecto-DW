-- ============================================================================
-- PROYECTO-DW · Datos de prueba masivos
-- ============================================================================
-- Carga 3.200 pacientes, 3.200 turnos y 3.200 consultas (más de 9.600 filas
-- nuevas) usando los hospitales, departamentos, unidades y médicos que ya
-- existen. Los turnos se reparten entre los 7 días anteriores y los 7
-- siguientes para probar el cambio de día en las agendas. También crea dos
-- cuentas de prueba para verificar la visibilidad
-- por rol:
--
--   ADMIN  -> admin.demo   / hospital123   (ve los 3.800 pacientes)
--   MEDICO -> medico.demo  / hospital123   (vinculado a M001, ve solo los suyos)
--
-- El script es reejecutable: usa ON CONFLICT DO NOTHING, por lo que no duplica
-- filas si se ejecuta dos veces. Los informes de cada turno los crea el
-- disparador trg_actualizar_informe al insertar cada consulta.
--
-- Ejecutar sobre la BD ya creada (después de los scripts de datos iniciales):
--   psql -U postgres -d sistema -v ON_ERROR_STOP=1 \
--     -f "database/Datos de Prueba/04_datos_prueba_masivos.sql"
-- ============================================================================

BEGIN;

-- Hash BCrypt de "hospital123".
INSERT INTO public."user" (username, email, password, role)
VALUES
    ('admin.demo', 'admin.demo@hospital.test',
     '$2b$10$8BzssiUVt5pQ54YLlaHxZeMstaSNBTgrbUgOIIWfLGa7TOKFm7VFG', 'ADMIN'),
    ('medico.demo', 'medico.demo@hospital.test',
     '$2b$10$8BzssiUVt5pQ54YLlaHxZeMstaSNBTgrbUgOIIWfLGa7TOKFm7VFG', 'MEDICO')
ON CONFLICT (username) DO NOTHING;

-- El médico de prueba atiende en la unidad U001 (Hospital General).
UPDATE public."Medico"
SET username = 'medico.demo'
WHERE "codMed" = 'M001' AND username IS NULL;

-- ----------------------------------------------------------------------------
-- 1. Pacientes (P1001 .. P4200) + su registro de admisión
-- ----------------------------------------------------------------------------
WITH medicos_idx AS (
    SELECT "codMed", "codUni", row_number() OVER (ORDER BY "codMed") - 1 AS idx
    FROM public."Medico"
),
nums AS (
    SELECT generate_series(1001, 4200) AS i
),
generados AS (
    SELECT n.i,
           'P' || lpad(n.i::text, 4, '0') AS id,
           m."codUni",
           (ARRAY[
             'Ana','Luis','María','Javier','Laura','Diego','Sofía','Mateo',
             'Camila','Andrés','Valentina','Pablo','Isabella','Santiago',
             'Emma','Sebastián','Lucía','Emiliano','Fernanda','Nicolás',
             'Renata','Alejandro','Daniela','Rodrigo','Paula','Ignacio',
             'Gabriela','Tomás','Adriana','Rafael','Elena','Martín',
             'Carolina','Hugo','Verónica','Esteban','Natalia','Cristian',
             'Mariana','Alberto'
           ])[1 + (n.i % 40)] || ' ' ||
           (ARRAY[
             'García','Martínez','López','Hernández','González','Pérez',
             'Rodríguez','Sánchez','Ramírez','Torres','Flores','Rivera',
             'Gómez','Díaz','Reyes','Morales','Cruz','Ortiz','Rojas','Vega',
             'Salas','Castro','Romero','Navarro','Paredes','Leal','Aguirre',
             'Duarte','Ferrer','Herrera','Núñez','Méndez','Silva','Cabrera',
             'Vargas','Molina','Benítez','Soto','Palacios','Contreras'
           ])[1 + (((n.i - 1001) / 40) % 40)] || ' ' ||
           (ARRAY[
             'García','Martínez','López','Hernández','González','Pérez',
             'Rodríguez','Sánchez','Ramírez','Torres','Flores','Rivera',
             'Gómez','Díaz','Reyes','Morales','Cruz','Ortiz','Rojas','Vega',
             'Salas','Castro','Romero','Navarro','Paredes','Leal','Aguirre',
             'Duarte','Ferrer','Herrera','Núñez','Méndez','Silva','Cabrera',
             'Vargas','Molina','Benítez','Soto','Palacios','Contreras'
           ])[1 + (((n.i - 1001) / 1600) % 40)] AS nombre,
           (ARRAY[
             'Calle Principal','Avenida Central','Callejón San José',
             'Paseo de la Reforma','Avenida Libertador','Calle Las Flores',
             'Boulevard Norte','Calle Colón','Avenida Universidad',
             'Camino Real'
           ])[1 + ((n.i * 3) % 10)] || ' #' || (n.i % 250 + 1) AS direccion,
           (CURRENT_DATE - make_interval(years => 18 + (n.i % 55),
                                         months => n.i % 12))::date AS fecha_nacimiento,
           CURRENT_DATE - (n.i % 200) AS fecha_admision,
           CASE
               WHEN n.i % 11 = 0 THEN 'Alta'
               WHEN n.i % 7 = 0 THEN 'Pendiente'
               ELSE 'Internado'
           END AS estado,
           m."codMed"
    FROM nums n
    JOIN medicos_idx m ON m.idx = (n.i - 1001) % (SELECT count(*) FROM public."Medico")
),
ins_registro AS (
    INSERT INTO public."Registro" ("estPac", "codUni", "numHisCli", "fecha_admision")
    SELECT g.estado, g."codUni", g.id, g.fecha_admision
    FROM generados g
    ON CONFLICT ("numHisCli") DO NOTHING
    RETURNING "numHisCli"
)
INSERT INTO public."Paciente" ("numHisCli", "nomPac", "dirPac", "fechaN")
SELECT g.id, g.nombre, g.direccion, g.fecha_nacimiento
FROM generados g
JOIN ins_registro r ON r."numHisCli" = g.id;

-- ----------------------------------------------------------------------------
-- 2. Turnos (T1001 .. T4200) + una consulta por turno
--    Los turnos se asignan al médico de su unidad; las consultas apuntan a un
--    paciente de la serie generada arriba. El disparador crea el informe.
-- ----------------------------------------------------------------------------
WITH medicos_idx AS (
    SELECT "codMed", "codUni", row_number() OVER (ORDER BY "codMed") - 1 AS idx
    FROM public."Medico"
),
nums AS (
    SELECT generate_series(1001, 4200) AS i
),
generados AS (
    SELECT n.i,
           m."codMed",
           m."codUni",
           CASE n.i % 4
               WHEN 0 THEN 'Finalizado'
               WHEN 1 THEN 'confirmado'
               WHEN 2 THEN 'atendido'
               ELSE 'pendiente'
           END AS estado_turno,
           n.i % 9 AS cantidad_atendida,
           'P' || lpad((1001 + ((n.i - 1001) * 7) % 3200)::text, 4, '0') AS paciente_id,
           (CURRENT_DATE - make_interval(days => ((n.i - 1001) % 15) - 7))::date AS fecha,
           -- El día usa %15, así que la hora debe derivar del índice de
           -- repetición dentro de ese día (/15). Se usa %19 (coprimo con los
           -- 16 médicos) para que un mismo médico no repita hora en el día.
           make_time(
               8 + ((((n.i - 1001) / 15) % 19) / 2),
               ((((n.i - 1001) / 15) % 19) % 2) * 30,
               0
           ) AS hora,
           'Turno generado automáticamente' AS nota_clinica
    FROM nums n
    JOIN medicos_idx m ON m.idx = (n.i - 1001) % (SELECT count(*) FROM public."Medico")
),
ins_turno AS (
    INSERT INTO public."Turno"
        ("numTur", "cantAtenTur", "estTur", "codUni", "codMed", "fecha", "hora", "nota_clinica")
    SELECT g.i, g.cantidad_atendida, g.estado_turno, g."codUni", g."codMed",
           g.fecha, g.hora, g.nota_clinica
    FROM generados g
    ON CONFLICT ("numTur") DO NOTHING
    RETURNING "numTur"
)
INSERT INTO public."Consulta" ("numTur", "numHisCli", "atendido", "causa")
SELECT g.i,
       g.paciente_id,
       g.estado_turno <> 'pendiente',
       CASE WHEN g.estado_turno = 'pendiente' THEN 'OtrasCausas' ELSE NULL END
FROM generados g
JOIN ins_turno t ON t."numTur" = g.i;

COMMIT;

-- Verificación rápida
SELECT 'Pacientes' AS tabla, count(*) AS total FROM public."Paciente"
UNION ALL SELECT 'Turnos', count(*) FROM public."Turno"
UNION ALL SELECT 'Consultas', count(*) FROM public."Consulta"
UNION ALL SELECT 'Informes', count(*) FROM public."Informe";
