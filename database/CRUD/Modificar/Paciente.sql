CREATE OR REPLACE FUNCTION public.modificarPaciente(
    p_oldHistoria VARCHAR,
    p_newHistoria VARCHAR,
    p_estPac VARCHAR,
    p_nomPac VARCHAR,
    p_direccion VARCHAR,
    p_fechaN DATE,
    p_codUni VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN,
    mensaje VARCHAR,
    numHistoria VARCHAR,
    estPac VARCHAR,
    nomPac VARCHAR,
    direccion VARCHAR,
    fechaN DATE,
    codUni VARCHAR
)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_oldHistoria IS NULL OR p_oldHistoria = '' THEN
        RETURN QUERY SELECT FALSE, 'El número de historia actual no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Registro" WHERE "numHisCli" = p_oldHistoria) THEN
        RETURN QUERY SELECT FALSE, 'El paciente que desea modificar no existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_newHistoria IS NULL OR p_newHistoria = '' THEN
        RETURN QUERY SELECT FALSE, 'El nuevo número de historia no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_nomPac IS NULL OR p_nomPac = '' THEN
        RETURN QUERY SELECT FALSE, 'El nombre del paciente no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_estPac IS NULL OR p_estPac = '' THEN
        RETURN QUERY SELECT FALSE, 'El estado del paciente no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_direccion IS NULL OR p_direccion = '' THEN
        RETURN QUERY SELECT FALSE, 'La dirección no puede estar vacía'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_fechaN IS NULL THEN
        RETURN QUERY SELECT FALSE, 'La fecha de nacimiento no puede estar vacía'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF p_codUni IS NULL OR p_codUni = '' THEN
        RETURN QUERY SELECT FALSE, 'El código de unidad no puede estar vacío'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni) THEN
        RETURN QUERY SELECT FALSE, 'La unidad asignada no existe'::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
        RETURN;
    END IF;

    UPDATE public."Registro"
    SET "estPac" = p_estPac,
        "numHisCli" = p_newHistoria,
        "codUni" = p_codUni
    WHERE "numHisCli" = p_oldHistoria;

    -- La FK Paciente.numHisCli -> Registro.numHisCli usa ON UPDATE CASCADE.
    -- Al actualizar Registro, el número nuevo ya se propagó a Paciente, por
    -- eso este UPDATE busca por p_newHistoria. Si se busca por p_oldHistoria,
    -- el nombre, la dirección y la fecha de nacimiento no se actualizan.
    UPDATE public."Paciente"
    SET "numHisCli" = p_newHistoria,
        "nomPac" = p_nomPac,
        "dirPac" = p_direccion,
        "fechaN" = p_fechaN
    WHERE "numHisCli" = p_newHistoria;

    RETURN QUERY SELECT TRUE, 'Paciente modificado correctamente'::VARCHAR,
                        p_newHistoria, p_estPac, p_nomPac, p_direccion, p_fechaN, p_codUni;

EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::VARCHAR, NULL::DATE, NULL::VARCHAR;
END;
$$;