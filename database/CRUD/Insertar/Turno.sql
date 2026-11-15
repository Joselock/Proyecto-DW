CREATE OR REPLACE FUNCTION public.insertarTurno(
    p_numTur INTEGER,
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
    IF p_numTur <= 0 THEN
        RETURN QUERY SELECT FALSE, 'El número de turno debe ser mayor a cero'::VARCHAR,
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

    IF EXISTS(SELECT 1 FROM public."Turno" WHERE "numTur" = p_numTur) THEN
        RETURN QUERY SELECT FALSE, 'El número de turno ya existe'::VARCHAR,
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

    INSERT INTO public."Turno" ("numTur", "cantAtenTur", "estTur", "codUni", "codMed")
    VALUES (p_numTur, p_cantAtendidos, p_estadoTur, p_codUni, p_codMed);

    RETURN QUERY SELECT TRUE, 'Turno insertado correctamente'::VARCHAR,
                        p_numTur, p_cantAtendidos, p_estadoTur, p_codUni, p_codMed;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;