CREATE OR REPLACE FUNCTION public.pacientesNoAtendidos(
    p_codHos VARCHAR DEFAULT NULL,
    p_codDep VARCHAR DEFAULT NULL,
    p_codUni VARCHAR DEFAULT NULL
)

RETURNS TABLE(
    hospital VARCHAR,
    departamento VARCHAR,
    unidad VARCHAR,
    numTur INTEGER,
    cantNoAtendidos INTEGER,
    numHistoria VARCHAR,
    nombrePac VARCHAR,
    direccion VARCHAR,
    causa VARCHAR
) AS $$

BEGIN
    RETURN QUERY
	
    SELECT 
        h."nomHos" AS hospital,
        d."nomDep" AS departamento,
        u."nomUni" AS unidad,
        t."numTur" AS numTur,
        COUNT(*) OVER (PARTITION BY h."nomHos", d."nomDep", u."nomUni", t."numTur")::INTEGER AS cantNoAtendidos,
        p."numHisCli" AS numHistoria,
        p."nomPac" AS nombrePac,
        p."dirPac" AS direccion,
        c."causa" AS causa
    FROM public."Hospital" h
    INNER JOIN public."Departamento" d ON d."codHos" = h."codHos"
    INNER JOIN public."Unidad" u ON u."codDep" = d."codDep"
    INNER JOIN public."Registro" r ON r."codUni" = u."codUni"
    INNER JOIN public."Paciente" p ON p."numHisCli" = r."numHisCli"
    INNER JOIN public."Consulta" c ON c."numHisCli" = p."numHisCli"
    INNER JOIN public."Turno" t ON t."numTur" = c."numTur"
    WHERE (c."atendido" = FALSE OR c."atendido" IS NULL)
      AND (p_codHos IS NULL OR h."codHos" = p_codHos)
      AND (p_codDep IS NULL OR d."codDep" = p_codDep)
      AND (p_codUni IS NULL OR u."codUni" = p_codUni)
    ORDER BY h."nomHos", d."nomDep", u."nomUni", t."numTur";
	
END;
$$ LANGUAGE plpgsql;