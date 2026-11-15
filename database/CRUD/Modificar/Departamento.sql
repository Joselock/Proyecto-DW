CREATE OR REPLACE FUNCTION public.modificarDepartamento(
      p_oldCod VARCHAR,
      p_newCod VARCHAR,
      p_nomDep VARCHAR,
	  p_codHos VARCHAR
)
RETURNS TABLE(
   existe BOOLEAN,
   mensaje VARCHAR,
   codDep VARCHAR,
   nomDep VARCHAR,
   codHos VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN

     -- Validar código actual
     IF p_oldCod IS NULL OR p_oldCod = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El valor actual del codigo del departamento no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR,NULL::VARCHAR;
        RETURN;
     END IF;

	 -- Validar existencia del departamento
     IF NOT EXISTS(SELECT 1 FROM public."Departamento" WHERE "codDep" = p_oldCod) THEN
        RETURN QUERY SELECT FALSE, 'El departamento que desea modificar no existe'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR,NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nuevo código
     IF p_newCod IS NULL OR p_newCod = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nuevo codigo del hospital no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR,NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nombre
     IF p_nomDep IS NULL OR p_nomDep = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del departamento no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR,NULL::VARCHAR;
        RETURN;
     END IF;

	 -- Validar codigo del hospital
     IF p_codHos IS NULL OR p_codHos = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El hospital al que pertenece el departamento no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR,NULL::VARCHAR;
        RETURN;
     END IF;

	 -- Validar existencia del hospital
     IF NOT EXISTS(SELECT 1 FROM public."Hospital" WHERE "codHos" = p_codHos) THEN
        RETURN QUERY SELECT FALSE, 'El hospital al que desea asignar el departamento no existe.'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR,NULL::VARCHAR;
        RETURN;
     END IF;

     -- Modificar
     UPDATE public."Departamento" 
     SET "codDep" = p_newCod,"nomDep" = p_nomDep , "codHos" = p_codHos
	 WHERE "codDep" = p_oldCod;

     --Retornar exito
     RETURN QUERY SELECT TRUE, 'Departamento modificado correctamente'::VARCHAR,
                         p_newCod, p_nomDep,p_codHos;

EXCEPTION
   WHEN OTHERS THEN
       RETURN QUERY SELECT FALSE,
                           ('Error inesperado: ' || SQLERRM)::VARCHAR,
                           NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;