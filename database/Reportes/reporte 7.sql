CREATE OR REPLACE FUNCTION public.resumenProceso(
    p_codHos VARCHAR DEFAULT NULL,
    p_codDep VARCHAR DEFAULT NULL,
    p_codUni VARCHAR DEFAULT NULL
)
RETURNS TABLE(
    hospital VARCHAR,
    departamento VARCHAR,
    unidad VARCHAR,
    numeroTurno INTEGER,
    horaInforme TIME,
    cantPacInicioConsulta INTEGER,
    cantPacAtendidos INTEGER,
    cantPacTotal INTEGER,
    porcentajePacAtend NUMERIC(5,2),
    cantPacNoAtendidos INTEGER,
    cantPacAlta INTEGER,
    cantPacExtranjero INTEGER,
    cantPacProvincia INTEGER,
    cantPacOtraUnidad INTEGER,
    cantPacOtrasCausas INTEGER,
    cantPacDesconoce INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        h."nomHos" AS hospital,
        d."nomDep" AS departamento,
        u."nomUni" AS unidad,
        i."numTur" AS numeroTurno,
        i."hora"::TIME AS horaInforme,
        i."total" - i."cantAdm" AS cantPacInicioConsulta,
        i."pacAten" AS cantPacAtendidos,
        i."total" AS cantPacTotal,
        CASE 
            WHEN i."total" > 0 THEN ROUND((i."pacAten" * 100.0 / i."total"), 2)
            ELSE 0
        END AS porcentajePacAtend,
        i."total" - i."pacAten" AS cantPacNoAtendidos,
        i."pacAlt" AS cantPacAlta,
        COALESCE((
            SELECT COUNT(*)::INTEGER
            FROM "Consulta" c
            WHERE c."numTur" = i."numTur"
              AND c."atendido" = false
              AND c."causa" = 'Extranjero'
        ), 0) AS cantPacExtranjero,
        COALESCE((
            SELECT COUNT(*)::INTEGER
            FROM "Consulta" c
            WHERE c."numTur" = i."numTur"
              AND c."atendido" = false
              AND c."causa" = 'FueraProvincia'
        ), 0) AS cantPacProvincia,
        COALESCE((
            SELECT COUNT(*)::INTEGER
            FROM "Consulta" c
            WHERE c."numTur" = i."numTur"
              AND c."atendido" = false
              AND c."causa" = 'OtraUnidad'
        ), 0) AS cantPacOtraUnidad,
        COALESCE((
            SELECT COUNT(*)::INTEGER
            FROM "Consulta" c
            WHERE c."numTur" = i."numTur"
              AND c."atendido" = false
              AND c."causa" = 'OtrasCausas'
        ), 0) AS cantPacOtrasCausas,
        COALESCE((
            SELECT COUNT(*)::INTEGER
            FROM "Consulta" c
            WHERE c."numTur" = i."numTur"
              AND c."atendido" = false
              AND (c."causa" IS NULL OR c."causa" = '')
        ), 0) AS cantPacDesconoce
    FROM "Informe" i
    INNER JOIN "Turno" t ON i."codUni" = t."codUni" AND i."numTur" = t."numTur"
    INNER JOIN "Unidad" u ON t."codUni" = u."codUni"
    INNER JOIN "Departamento" d ON u."codDep" = d."codDep"
    INNER JOIN "Hospital" h ON d."codHos" = h."codHos"
    WHERE (p_codHos IS NULL OR d."codHos" = p_codHos)
      AND (p_codDep IS NULL OR u."codDep" = p_codDep)
      AND (p_codUni IS NULL OR u."codUni" = p_codUni)
      -- Solo turnos con al menos un paciente NO atendido
      AND EXISTS (
          SELECT 1 FROM "Consulta" c
          WHERE c."numTur" = i."numTur"
            AND c."atendido" = false
      )
    ORDER BY h."nomHos", d."nomDep", u."nomUni", i."numTur";
END;
$$ LANGUAGE plpgsql;