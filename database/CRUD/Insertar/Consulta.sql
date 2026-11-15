CREATE OR REPLACE FUNCTION public.insertarConsulta(
    p_numTur INTEGER,
    p_numHistoria VARCHAR,
    p_atendido BOOLEAN,
    p_causa VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN,
    mensaje VARCHAR,
    numTur INTEGER,
    numHistoria VARCHAR,
    atendido BOOLEAN,
    causa VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_numTur <= 0 THEN
        RETURN QUERY SELECT FALSE, 'El número de turno debe ser mayor a cero'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_numHistoria IS NULL OR p_numHistoria = '' THEN
        RETURN QUERY SELECT FALSE, 'El número de historia no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_atendido IS NULL THEN
        RETURN QUERY SELECT FALSE, 'Debe indicar si el paciente fue atendido o no'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_causa IS NULL OR p_causa = '' THEN
        RETURN QUERY SELECT FALSE, 'La causa no puede estar vacía'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Turno" WHERE "numTur" = p_numTur) THEN
        RETURN QUERY SELECT FALSE, 'El turno no existe'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Registro" WHERE "numHisCli" = p_numHistoria) THEN
        RETURN QUERY SELECT FALSE, 'El paciente no existe en el registro'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF EXISTS(SELECT 1 FROM public."Consulta" WHERE "numTur" = p_numTur AND "numHisCli" = p_numHistoria) THEN
        RETURN QUERY SELECT FALSE, 'La consulta ya existe para este turno y paciente'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    INSERT INTO public."Consulta" ("numTur", "numHisCli", "atendido", "causa")
    VALUES (p_numTur, p_numHistoria, p_atendido, p_causa);

    RETURN QUERY SELECT TRUE, 'Consulta insertada correctamente'::VARCHAR,
                        p_numTur, p_numHistoria, p_atendido, p_causa;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
END;
$$;