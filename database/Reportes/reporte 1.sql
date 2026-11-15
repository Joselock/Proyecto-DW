CREATE OR REPLACE FUNCTION listadoPacientes(
    p_codHos VARCHAR DEFAULT NULL,
    p_codDep VARCHAR DEFAULT NULL,
    p_codUni VARCHAR DEFAULT NULL
)

RETURNS TABLE(
    hospital VARCHAR,
    departamento VARCHAR,
    unidad VARCHAR,
    numHis VARCHAR,
    nombre VARCHAR,
    fechaN DATE,
    direccion VARCHAR
) AS $$

BEGIN
    RETURN QUERY
    SELECT 
        h."nomHos" AS hospital,
        d."nomDep" AS departamento,
        u."nomUni" AS unidad,
        p."numHisCli" AS numHis,
        p."nomPac" AS nombre,
        p."fechaN" AS fechaN,
        p."dirPac" AS direccion
    FROM public."Paciente" p
	INNER JOIN public."Registro" r ON p."numHisCli" = r."numHisCli"
    INNER JOIN public."Unidad" u ON r."codUni" = u."codUni"
    INNER JOIN public."Departamento" d ON u."codDep" = d."codDep"
    INNER JOIN public."Hospital" h ON d."codHos" = h."codHos"
    WHERE (p_codHos IS NULL OR h."codHos" = p_codHos)
      AND (p_codDep IS NULL OR d."codDep" = p_codDep)
      AND (p_codUni IS NULL OR u."codUni" = p_codUni)
    ORDER BY h."nomHos", d."nomDep", u."nomUni", p."nomPac";
END;

$$ LANGUAGE plpgsql;