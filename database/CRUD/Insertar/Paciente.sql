CREATE OR REPLACE FUNCTION public.insertarPaciente(
      p_numHistoria VARCHAR,
      p_nomPac VARCHAR,
	  p_direccion VARCHAR,
	  p_fecha DATE,
	  p_estadoPac VARCHAR,
	  p_codUni VARCHAR
)
RETURNS TABLE(
   existe BOOLEAN,
   mensaje VARCHAR,
   numHistoria VARCHAR,
   nomPac VARCHAR,
   direccion VARCHAR,
   fecha DATE,
   estadoPac VARCHAR,
   codUni VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
     -- Validar número de historia
     IF p_numHistoria IS NULL OR p_numHistoria = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El numero de historial del paciente no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar nombre
     IF p_nomPac IS NULL OR p_nomPac = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del paciente no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar dirección
     IF p_direccion IS NULL OR p_direccion = ' ' THEN
        RETURN QUERY SELECT FALSE, 'La direccion particular del paciente no puede estar vacia'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar fecha 
     IF p_fecha IS NULL THEN
        RETURN QUERY SELECT FALSE, 'La fecha de nacimiento del paciente no puede estar vacia'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar estado del paciente
     IF p_estadoPac IS NULL OR p_estadoPac = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El estado del paciente no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar unidad
     IF p_codUni IS NULL OR p_codUni = ' ' THEN
        RETURN QUERY SELECT FALSE, 'El codigo de la unidad no puede estar vacio'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar existencia del paciente en el registro de esa unidad
     IF EXISTS(SELECT 1 FROM public."Registro" WHERE "numHisCli" = p_numHistoria AND "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'El paciente ya existe en el registro de esta unidad'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Validar existencia de la unidad
     IF NOT EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'La unidad asignada no existe'::VARCHAR,
                     NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
     END IF;

     -- Insertar en Registro (incluyendo fecha_admision)
    INSERT INTO public."Registro" ("estPac", "codUni", "numHisCli", "fecha_admision")
    VALUES (p_estadoPac, p_codUni, p_numHistoria, CURRENT_DATE);
    
    INSERT INTO public."Paciente" ("numHisCli", "nomPac", "dirPac", "fechaN")
    VALUES (p_numHistoria, p_nomPac, p_direccion, p_fecha);

    RETURN QUERY SELECT TRUE, 'Paciente insertado correctamente'::VARCHAR,
                        p_numHistoria, p_nomPac, p_direccion, p_fecha, p_estadoPac, p_codUni;
EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;