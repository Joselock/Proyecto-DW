CREATE OR REPLACE FUNCTION consultasExitosas(
    p_codHos VARCHAR DEFAULT NULL,
    p_codUni VARCHAR DEFAULT NULL
)

RETURNS TABLE(
    hospital VARCHAR,
    departamento VARCHAR,
    unidad VARCHAR,
    numeroTurno INTEGER,
    medico VARCHAR,
    totalPacAten INTEGER,
    pacienteAtend INTEGER
) AS $$

BEGIN
    RETURN QUERY
    WITH 
    -- Calcular éxito del turno basado en el informe (>=80% atendidos)
    turnos_con_exito AS (
        SELECT 
            i."numTur",
            i."codUni",
            t."codMed",
            i."total" AS total_pacientes_turno,
            i."pacAten" AS atendidos_turno,
            -- Éxito si 80% o más
            CASE 
                WHEN i."total" > 0 AND (i."pacAten" * 100.0 / i."total") >= 80 THEN true
                ELSE false
            END AS es_exitoso
        FROM "Informe" i
        JOIN "Turno" t ON i."codUni" = t."codUni" AND i."numTur" = t."numTur"
    ),
    -- Pacientes atendidos por médico en cada turno
    atenciones_medico AS (
        SELECT 
            c."numTur",
            t."codUni",
            t."codMed",
            COUNT(*) AS pacientes_atendidos
        FROM "Consulta" c
        JOIN "Turno" t ON c."numTur" = t."numTur"
        WHERE c."atendido" = true
        GROUP BY c."numTur", t."codUni", t."codMed"
    ),
    -- Turnos exitosos (cumplen condición)
    turnos_exitosos AS (
        SELECT DISTINCT
            te."codUni",
            te."numTur"
        FROM turnos_con_exito te
        WHERE te.es_exitoso = true
    )
	
    -- Resultado final: solo turnos exitosos
    SELECT 
        h."nomHos"::VARCHAR,
        d."nomDep"::VARCHAR,
        u."nomUni"::VARCHAR,
        te."numTur"::INTEGER,
        m."nomMed"::VARCHAR,
        -- Total de pacientes atendidos por ese médico
        (SELECT SUM(am.pacientes_atendidos) 
         FROM atenciones_medico am 
         WHERE am."codMed" = m."codMed" 
           AND EXISTS (SELECT 1 FROM turnos_exitosos tx WHERE tx."numTur" = am."numTur"))
        ::INTEGER AS totalPacAten,
        -- Pacientes atendidos por este médico en este turno específico
        COALESCE(am.pacientes_atendidos, 0)::INTEGER AS pacienteAtend
    FROM turnos_exitosos te
    JOIN "Unidad" u ON te."codUni" = u."codUni"
    JOIN "Departamento" d ON u."codDep" = d."codDep"
    JOIN "Hospital" h ON d."codHos" = h."codHos"
    JOIN "Turno" t ON te."numTur" = t."numTur" AND te."codUni" = t."codUni"
    JOIN "Medico" m ON t."codMed" = m."codMed"
    LEFT JOIN atenciones_medico am ON te."numTur" = am."numTur" AND te."codUni" = am."codUni" AND m."codMed" = am."codMed"
    WHERE (p_codHos IS NULL OR h."codHos" = p_codHos)
      AND (p_codUni IS NULL OR u."codUni" = p_codUni)
    ORDER BY h."nomHos", d."nomDep", u."nomUni", te."numTur", m."nomMed";
END;
$$ LANGUAGE plpgsql;