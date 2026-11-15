CREATE OR REPLACE FUNCTION public.modificarMedico(
    p_oldCod VARCHAR,
    p_newCod VARCHAR,
    p_nomMed VARCHAR,
    p_espMed VARCHAR,
    p_numLic VARCHAR,
    p_telefono VARCHAR,
    p_experiencia INTEGER,
    p_datos VARCHAR,
    p_codUni VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN,
    mensaje VARCHAR,
    codMed VARCHAR,
    nomMed VARCHAR,
    espMed VARCHAR,
    numLic VARCHAR,
    telefono VARCHAR,
    experiencia INTEGER,
    datos VARCHAR,
    codUni VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_oldCod IS NULL OR p_oldCod = '' THEN
        RETURN QUERY SELECT FALSE, 'El código actual del médico no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Medico" WHERE "codMed" = p_oldCod) THEN
        RETURN QUERY SELECT FALSE, 'El médico que desea modificar no existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_newCod IS NULL OR p_newCod = '' THEN
        RETURN QUERY SELECT FALSE, 'El nuevo código del médico no puede estar vacío'::VARCHAR,
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

    IF p_espMed IS NULL OR p_espMed = '' THEN
        RETURN QUERY SELECT FALSE, 'La especialidad no puede estar vacía'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_numLic IS NULL OR p_numLic = '' THEN
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
        RETURN QUERY SELECT FALSE, 'La experiencia debe ser mayor o igual a cero'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_datos IS NULL OR p_datos = '' THEN
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

    IF NOT EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'La unidad asignada no existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    UPDATE public."Medico"
    SET "codMed" = p_newCod,
        "nomMed" = p_nomMed,
        "espMed" = p_espMed,
        "numLic" = p_numLic,
        "telMed" = p_telefono,
        "exp" = p_experiencia,
        "datCon" = p_datos,
        "codUni" = p_codUni
    WHERE "codMed" = p_oldCod;

    RETURN QUERY SELECT TRUE, 'Médico modificado correctamente'::VARCHAR,
                        p_newCod, p_nomMed, p_espMed, p_numLic,
                        p_telefono, p_experiencia, p_datos, p_codUni;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR,
                            NULL::VARCHAR, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;