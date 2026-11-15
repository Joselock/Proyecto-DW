CREATE OR REPLACE FUNCTION public.modificarTurno(
    p_numActual INTEGER,
    p_numNuevo INTEGER,
    p_cantAtendidos INTEGER,
    p_estadoTur VARCHAR,
    p_codUni VARCHAR,
    p_codMed VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN,
    mensaje VARCHAR,
    numTur INTEGER,
    cantAtendidos INTEGER,
    estadoTur VARCHAR,
    codUni VARCHAR,
    codMed VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_numActual <= 0 THEN
        RETURN QUERY SELECT FALSE, 'El número de turno actual no es válido'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Turno" WHERE "numTur" = p_numActual) THEN
        RETURN QUERY SELECT FALSE, 'El turno que desea modificar no existe'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_cantAtendidos < 0 THEN
        RETURN QUERY SELECT FALSE, 'La cantidad de atendidos no puede ser negativa'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_estadoTur IS NULL OR p_estadoTur = '' THEN
        RETURN QUERY SELECT FALSE, 'El estado del turno no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codUni IS NULL OR p_codUni = '' THEN
        RETURN QUERY SELECT FALSE, 'El código de unidad no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codMed IS NULL OR p_codMed = '' THEN
        RETURN QUERY SELECT FALSE, 'El código del médico no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'La unidad no existe'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Medico" WHERE "codMed" = p_codMed) THEN
        RETURN QUERY SELECT FALSE, 'El médico no existe'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    UPDATE public."Turno"
    SET "numTur" = p_numNuevo,
        "cantAtenTur" = p_cantAtendidos,
        "estTur" = p_estadoTur,
        "codUni" = p_codUni,
        "codMed" = p_codMed
    WHERE "numTur" = p_numActual;

    RETURN QUERY SELECT TRUE, 'Turno modificado correctamente'::VARCHAR,
                        p_numNuevo, p_cantAtendidos, p_estadoTur, p_codUni, p_codMed;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;