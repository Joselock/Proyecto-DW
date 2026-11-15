CREATE OR REPLACE FUNCTION public.resumenHospitales()

RETURNS TABLE(
       hospital VARCHAR,
	   cantDepartamentos INTEGER,
	   cantUnidades INTEGER,
	   cantMedicos INTEGER,
	   cantPacientes INTEGER
) AS $$

BEGIN
    RETURN QUERY

	SELECT
	      h."nomHos" AS hospital,
		  COUNT(DISTINCT d."codDep")::INTEGER AS cantDepartamentos,
		  COUNT(DISTINCT u."codUni")::INTEGER AS cantUnidades,
		  COUNT(DISTINCT m."codMed")::INTEGER AS cantMedicos,
		  COUNT(DISTINCT p."numHisCli")::INTEGER AS cantPacientes

	FROM public."Hospital" h
	LEFT JOIN public."Departamento" d ON h."codHos" = d."codHos"
	LEFT JOIN public."Unidad" u ON u."codDep" = d."codDep" 
	LEFT JOIN public."Medico" m ON m."codUni" = u."codUni"
	LEFT JOIN public."Registro" r ON r."codUni" = u."codUni"
	LEFT JOIN public."Paciente" p ON r."numHisCli" = p."numHisCli"
	GROUP BY h."codHos", h."nomHos"
	ORDER BY h."nomHos";
	

END;

$$ LANGUAGE  plpgsql; 
