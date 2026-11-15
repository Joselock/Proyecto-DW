CREATE OR REPLACE FUNCTION public.modificarConsulta(
    p_numActual INTEGER,
    p_numNuevo INTEGER,
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
    IF p_numActual <= 0 THEN
        RETURN QUERY SELECT FALSE, 'El número de turno actual no es válido'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Consulta" WHERE "numTur" = p_numActual) THEN
        RETURN QUERY SELECT FALSE, 'La consulta que desea modificar no existe'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_numHistoria IS NULL OR p_numHistoria = '' THEN
        RETURN QUERY SELECT FALSE, 'Debe ingresar el número de historia'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Registro" WHERE "numHisCli" = p_numHistoria) THEN
        RETURN QUERY SELECT FALSE, 'El número de historia no existe'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_atendido IS NULL THEN
        RETURN QUERY SELECT FALSE, 'Debe decir si el paciente fue atendido o no'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_causa IS NULL OR p_causa = '' THEN
        RETURN QUERY SELECT FALSE, 'La causa no puede estar vacía'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
        RETURN;
    END IF;

    UPDATE public."Consulta"
    SET "numTur" = p_numNuevo,
        "numHisCli" = p_numHistoria,
        "atendido" = p_atendido,
        "causa" = p_causa
    WHERE "numTur" = p_numActual;

    RETURN QUERY SELECT TRUE, 'Consulta modificada correctamente'::VARCHAR,
                        p_numNuevo, p_numHistoria, p_atendido, p_causa;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR, NULL::BOOLEAN, NULL::VARCHAR;
END;
$$;