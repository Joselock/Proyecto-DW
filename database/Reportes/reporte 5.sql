CREATE OR REPLACE FUNCTION public.informeDuranteConsultas(
    p_codHos VARCHAR DEFAULT NULL,
    p_codDep VARCHAR DEFAULT NULL,
    p_codUni VARCHAR DEFAULT NULL
)

RETURNS TABLE(
       hospital VARCHAR,
	   departamento VARCHAR,
	   unidad VARCHAR,
	   fecha DATE,
	   numTurno INTEGER,
	   hora_informe TIME,
	   numInforme VARCHAR,
	   cantIniPac INTEGER,
	   cantAdmPac INTEGER,
	   cantAltPac INTEGER,
	   cantAnterior INTEGER,
	   cantDia INTEGER
)AS $$

BEGIN
     RETURN QUERY
	 WITH informe_anterior AS(
          SELECT
		        i."codUni",
				i."fecha",
				i."numIn",
				LAG(i."pacAten") OVER (
                    PARTITION BY i."codUni", i."fecha" 
                    ORDER BY i."hora"
				)AS pac_atendido_anterior
		  FROM public."Informe" i		
	 )

	 SELECT 
        h."nomHos" AS hospital,
        d."nomDep" AS departamento,
        u."nomUni" AS unidad,
        i."fecha" AS fecha,
        t."numTur" AS numTurno,
        i."hora"::TIME AS hora_informe,
        i."numIn" AS numInforme,
        i."pacAten"::INTEGER AS cantIniPac,
        i."cantAdm"::INTEGER AS cantAdmPac,
        i."pacAlt"::INTEGER AS cantAltPac,
        COALESCE(ia."pac_atendido_anterior", 0)::INTEGER AS cantAnterior,
        (i."pacAten" - COALESCE(ia."pac_atendido_anterior", 0))::INTEGER AS cantDia
    FROM public."Informe" i
    INNER JOIN public."Unidad" u ON u."codUni" = i."codUni"
    INNER JOIN public."Departamento" d ON d."codDep" = u."codDep"
    INNER JOIN public."Hospital" h ON h."codHos" = d."codHos"
    INNER JOIN public."Turno" t ON t."numTur" = i."numTur"
    LEFT JOIN informe_anterior ia ON ia."codUni" = i."codUni" 
        AND ia."fecha" = i."fecha" 
        AND ia."numIn" = i."numIn"
    WHERE (p_codHos IS NULL OR h."codHos" = p_codHos)
      AND (p_codDep IS NULL OR d."codDep" = p_codDep)
      AND (p_codUni IS NULL OR u."codUni" = p_codUni)
    ORDER BY h."nomHos", d."nomDep", u."nomUni", i."fecha", i."hora";

END;

$$ LANGUAGE plpgsql;