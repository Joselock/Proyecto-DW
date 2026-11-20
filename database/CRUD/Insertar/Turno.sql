-- Inserta un turno. Se conserva la firma original de 5 parámetros y se añade
-- una sobrecarga con fecha, hora y nota clínica, que es la que usa el dashboard.
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
    RETURN QUERY
    SELECT * FROM public.insertarTurno(
        p_numTur, p_cantAtendidos, p_estadoTur, p_codUni, p_codMed,
        CURRENT_DATE, CURRENT_TIME::time, NULL
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.insertarTurno(
    p_numTur INTEGER,
    p_cantAtendidos INTEGER,
    p_estadoTur VARCHAR,
    p_codUni VARCHAR,
    p_codMed VARCHAR,
    p_fecha DATE,
    p_hora TIME,
    p_notaClinica VARCHAR
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

    IF p_estadoTur IS NULL OR btrim(p_estadoTur) = '' THEN
        RETURN QUERY SELECT FALSE, 'El estado del turno no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codUni IS NULL OR btrim(p_codUni) = '' THEN
        RETURN QUERY SELECT FALSE, 'El código de unidad no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codMed IS NULL OR btrim(p_codMed) = '' THEN
        RETURN QUERY SELECT FALSE, 'El código del médico no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_fecha IS NULL THEN
        RETURN QUERY SELECT FALSE, 'La fecha del turno no puede estar vacía'::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_hora IS NULL THEN
        RETURN QUERY SELECT FALSE, 'La hora del turno no puede estar vacía'::VARCHAR,
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

    INSERT INTO public."Turno"
        ("numTur", "cantAtenTur", "estTur", "codUni", "codMed", "fecha", "hora", "nota_clinica")
    VALUES
        (p_numTur, p_cantAtendidos, p_estadoTur, p_codUni, p_codMed,
         p_fecha, p_hora, NULLIF(btrim(COALESCE(p_notaClinica, '')), ''));

    RETURN QUERY SELECT TRUE, 'Turno insertado correctamente'::VARCHAR,
                        p_numTur, p_cantAtendidos, p_estadoTur, p_codUni, p_codMed;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::INTEGER, NULL::INTEGER, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR;
END;
$$;
