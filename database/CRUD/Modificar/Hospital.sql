CREATE OR REPLACE FUNCTION public.modificarHospital(
      p_oldCod VARCHAR,
      p_newCod VARCHAR,
      p_nomHos VARCHAR
)
RETURNS TABLE(
   existe BOOLEAN,
   mensaje VARCHAR,
   codHos VARCHAR,
   nomHos VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN

     -- Validar código actual
     IF p_oldCod IS NULL OR p_oldCod = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El valor actual del codigo del hospital no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;
      
      -- Validar existencia del hospital
     IF NOT EXISTS(SELECT 1 FROM public."Hospital" WHERE "codHos" = p_oldCod) THEN
        RETURN QUERY SELECT FALSE, 'El hospital que desea modificar no existe'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nuevo código
     IF p_newCod IS NULL OR p_newCod = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nuevo codigo del hospital no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nombre
     IF p_nomHos IS NULL OR p_nomHos = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del hospital no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Modificar
     UPDATE public."Hospital" 
     SET "codHos" = p_newCod,"nomHos" = p_nomHos
	 WHERE "codHos" = p_oldCod;

     --Retornar exito
     RETURN QUERY SELECT TRUE, 'Hospital modificado correctamente'::VARCHAR,
                         p_newCod, p_nomHos;

EXCEPTION
   WHEN OTHERS THEN
       RETURN QUERY SELECT FALSE,
                           ('Error inesperado: ' || SQLERRM)::VARCHAR,
                           NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;