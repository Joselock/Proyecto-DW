CREATE OR REPLACE FUNCTION public.insertarDepartamento(
      p_codDep VARCHAR,
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
     -- Validar código
     IF p_codDep IS NULL OR p_codDep = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El codigo del departamento no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nombre
     IF p_nomDep IS NULL OR p_nomDep = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del departamento no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar código hospital
     IF p_codHos IS NULL OR p_codHos = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El codigo del hospital al que pertenece no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar existencia del hospital
     IF NOT EXISTS(SELECT 1 FROM public."Hospital" WHERE "codHos" = p_codHos) THEN
        RETURN QUERY SELECT FALSE, 'El hospital al que quiere asignar este departamento no existe'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Insertar 
     INSERT INTO public."Departamento" ("codDep", "nomDep", "codHos")
     VALUES (p_codDep, p_nomDep, p_codHos);

     -- Éxito
     RETURN QUERY SELECT TRUE, 'Departamento insertado correctamente'::VARCHAR,
                         p_codDep, p_nomDep, p_codHos;

EXCEPTION
   WHEN OTHERS THEN
       RETURN QUERY SELECT FALSE,
                           ('Error inesperado: ' || SQLERRM)::VARCHAR,
                           NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;