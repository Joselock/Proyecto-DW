CREATE OR REPLACE FUNCTION public.listadoMedicos()

RETURNS TABLE(
     hospital VARCHAR,
	 departamento VARCHAR,
	 unidad VARCHAR,
	 nombre VARCHAR,
	 licencia VARCHAR,
	 especialidad VARCHAR,
	 telefono VARCHAR,
	 experiencia INTEGER,
	 datosCon VARCHAR
)AS $$

BEGIN
    RETURN QUERY

    SELECT 
		  h."nomHos" AS hospital,
		  d."nomDep" AS departamento,
		  u."nomUni" AS unidad,
		  m."nomMed" AS nombre,
		  m."numLic" AS licencia,
		  m."espMed" AS especialidad,
		  m."telMed" AS telefono,
		  m."exp" AS experencia,
		  m."datCon" AS datosCon
    FROM public."Medico" m
	     INNER JOIN public."Unidad" u ON m."codUni" = u."codUni"
		 INNER JOIN public."Departamento" d On u."codDep" = d."codDep"
		 INNER JOIN public."Hospital" h ON d."codHos" = h."codHos"
	ORDER BY h."nomHos", d."nomDep", u."nomUni", m."nomMed";	 

END;

$$ LANGUAGE plpgsql;