CREATE OR REPLACE FUNCTION public.insertarUnidad(
    p_codUni VARCHAR,
    p_nomUni VARCHAR,
    p_ubicacion VARCHAR,
    p_codDep VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN,
    mensaje VARCHAR,
    codUni VARCHAR,
    nomUni VARCHAR,
    ubiUni VARCHAR,
    codDep VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_codUni IS NULL OR p_codUni = '' THEN
        RETURN QUERY SELECT FALSE, 'El código de la unidad no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_nomUni IS NULL OR p_nomUni = '' THEN
        RETURN QUERY SELECT FALSE, 'El nombre de la unidad no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_ubicacion IS NULL OR p_ubicacion = '' THEN
        RETURN QUERY SELECT FALSE, 'La ubicación de la unidad no puede estar vacía'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codDep IS NULL OR p_codDep = '' THEN
        RETURN QUERY SELECT FALSE, 'El código del departamento no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'La unidad ya existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Departamento" WHERE "codDep" = p_codDep) THEN
        RETURN QUERY SELECT FALSE, 'El departamento asignado no existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    INSERT INTO public."Unidad" ("codUni", "nomUni", "ubiUni", "codDep")
    VALUES (p_codUni, p_nomUni, p_ubicacion, p_codDep);

    RETURN QUERY SELECT TRUE, 'Unidad insertada correctamente'::VARCHAR,
                        p_codUni, p_nomUni, p_ubicacion, p_codDep;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;