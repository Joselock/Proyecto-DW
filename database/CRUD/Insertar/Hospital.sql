CREATE OR REPLACE FUNCTION public.insertarHospital(
      p_codHos VARCHAR,
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
     -- Validar código
     IF p_codHos IS NULL OR p_codHos = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El codigo del hospital no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nombre
     IF p_nomHos IS NULL OR p_nomHos = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del hospital no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar existencia del hospital
     IF EXISTS(SELECT 1 FROM public."Hospital" WHERE "codHos" = p_codHos) THEN
        RETURN QUERY SELECT FALSE, 'El hospital ya existe'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Insertar 
     INSERT INTO public."Hospital" ("codHos", "nomHos")
     VALUES (p_codHos, p_nomHos);

     --Retornar exito
     RETURN QUERY SELECT TRUE, 'Hospital insertado correctamente'::VARCHAR,
                         p_codHos, p_nomHos;

EXCEPTION
   WHEN OTHERS THEN
       RETURN QUERY SELECT FALSE,
                           ('Error inesperado: ' || SQLERRM)::VARCHAR,
                           NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;