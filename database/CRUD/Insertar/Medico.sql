CREATE OR REPLACE FUNCTION public.insertarMedico(
    p_codMed VARCHAR,
    p_nomMed VARCHAR,
    p_especialidad VARCHAR,
    p_licencia VARCHAR,
    p_telefono VARCHAR,
    p_experiencia INTEGER,
    p_datosCon VARCHAR,
    p_codUni VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN,
    mensaje VARCHAR,
    codMed VARCHAR,
    nomMed VARCHAR,
    especialidad VARCHAR,
    licencia VARCHAR,
    telefono VARCHAR,
    experiencia INTEGER,
    datosCon VARCHAR,
    codUni VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_codMed IS NULL OR p_codMed = '' THEN
        RETURN QUERY SELECT FALSE, 'El código del médico no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_nomMed IS NULL OR p_nomMed = '' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del médico no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_especialidad IS NULL OR p_especialidad = '' THEN
        RETURN QUERY SELECT FALSE, 'La especialidad no puede estar vacía'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_licencia IS NULL OR p_licencia = '' THEN
        RETURN QUERY SELECT FALSE, 'La licencia no puede estar vacía'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_telefono IS NULL OR p_telefono = '' THEN
        RETURN QUERY SELECT FALSE, 'El teléfono no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_experiencia < 0 THEN
        RETURN QUERY SELECT FALSE, 'La experiencia no puede ser negativa'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_datosCon IS NULL OR p_datosCon = '' THEN
        RETURN QUERY SELECT FALSE, 'Los datos de contacto no pueden estar vacíos'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codUni IS NULL OR p_codUni = '' THEN
        RETURN QUERY SELECT FALSE, 'El código de unidad no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF EXISTS(SELECT 1 FROM public."Medico" WHERE "codMed" = p_codMed) THEN
        RETURN QUERY SELECT FALSE, 'El médico ya existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'La unidad asignada no existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    INSERT INTO public."Medico" ("codMed", "nomMed", "espMed", "numLic", "telMed", "exp", "datCon", "codUni")
    VALUES (p_codMed, p_nomMed, p_especialidad, p_licencia, p_telefono, p_experiencia, p_datosCon, p_codUni);

    RETURN QUERY SELECT TRUE, 'Médico insertado correctamente'::VARCHAR,
                        p_codMed, p_nomMed, p_especialidad, p_licencia,
                        p_telefono, p_experiencia, p_datosCon, p_codUni;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;