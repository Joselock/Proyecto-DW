CREATE OR REPLACE FUNCTION public.revisarTurnos()

RETURNS TABLE(
    hospital VARCHAR,
    departamento VARCHAR,
    unidad VARCHAR,
    totalPacientes INTEGER,
    nombreMedico VARCHAR,
    cantPacAten INTEGER,
    porcentajeAten NUMERIC(5,2)
) AS $$

BEGIN
    RETURN QUERY
    WITH 
    -- Calcular total de pacientes por turno/unidad
    totales_turno AS (
        SELECT 
            t."codUni",
            t."numTur",
            t."codMed",
            i."total" AS total_pacientes_turno,
            i."pacAten" AS atendidos_turno,
            -- Unidad es exitosa si atiende al menos 80%
            CASE 
                WHEN i."total" > 0 AND (i."pacAten" * 100.0 / i."total") >= 80 THEN true
                ELSE false
            END AS es_exitoso
        FROM "Turno" t
        INNER JOIN "Informe" i ON t."codUni" = i."codUni" AND t."numTur" = i."numTur"
    ),
    -- 2. Pacientes atendidos por médico en cada turno
    atenciones_medico AS (
        SELECT 
            c."numTur",
            t."codUni",
            t."codMed",
            COUNT(c."numHisCli") AS pacientes_atendidos
        FROM "Consulta" c
        INNER JOIN "Turno" t ON c."numTur" = t."numTur"
        WHERE c."atendido" = true
        GROUP BY c."numTur", t."codUni", t."codMed"
    ),
	
    -- Unidades que deben revisar turnos
    unidades_revisar AS (
        SELECT DISTINCT
            tt."codUni",
            tt."numTur"
        FROM totales_turno tt
        WHERE tt.es_exitoso = false
    )
    -- Resultado final
    SELECT 
        h."nomHos"::VARCHAR AS hospital,
        d."nomDep"::VARCHAR AS departamento,
        u."nomUni"::VARCHAR AS unidad,
        tt.total_pacientes_turno::INTEGER AS totalPacientes,
        m."nomMed"::VARCHAR AS nombreMedico,
        COALESCE(am.pacientes_atendidos, 0)::INTEGER AS cantPacAten,
        CASE 
            WHEN tt.total_pacientes_turno > 0 
            THEN ROUND((COALESCE(am.pacientes_atendidos, 0) * 100.0 / tt.total_pacientes_turno), 2)
            ELSE 0
        END::NUMERIC(5,2) AS porcentajeAten
    FROM unidades_revisar ur
    INNER JOIN totales_turno tt ON ur."codUni" = tt."codUni" AND ur."numTur" = tt."numTur"
    INNER JOIN "Unidad" u ON ur."codUni" = u."codUni"
    INNER JOIN "Departamento" d ON u."codDep" = d."codDep"
    INNER JOIN "Hospital" h ON d."codHos" = h."codHos"
    INNER JOIN "Medico" m ON tt."codMed" = m."codMed"
    LEFT JOIN atenciones_medico am ON tt."numTur" = am."numTur" AND tt."codUni" = am."codUni" AND tt."codMed" = am."codMed"
    ORDER BY h."nomHos", d."nomDep", u."nomUni", m."nomMed";
END;
$$ LANGUAGE plpgsql;