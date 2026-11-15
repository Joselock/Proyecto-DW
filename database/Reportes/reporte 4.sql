CREATE OR REPLACE FUNCTION public.listadoMasDeCien()

RETURNS TABLE(
       hospital VARCHAR,
	   cantPacientes INTEGER
)AS $$

BEGIN
     RETURN QUERY

	 SELECT
	       h."nomHos" AS hospital,
		   COUNT(DISTINCT p."numHisCli")::INTEGER AS cantPacientes

	 FROM public."Hospital" h
	 LEFT JOIN public."Departamento" d ON d."codHos" = h."codHos"
	 LEFT JOIN public."Unidad" u ON u."codDep" = d."codDep"
	 LEFT JOIN public."Registro" r ON r."codUni" = u."codUni"
	 LEFT JOIN public."Paciente" p ON p."numHisCli" = r."numHisCli"
	 GROUP BY h."nomHos"
	 HAVING COUNT(DISTINCT p."numHisCli") > 100
	 ORDER BY h."nomHos"
	 LIMIT 5;
	
END;

$$ LANGUAGE plpgsql;