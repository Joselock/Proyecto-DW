CREATE OR REPLACE FUNCTION public.eliminarConsulta(
    p_numTur INTEGER,
    p_numHistoria VARCHAR
)
RETURNS TABLE(
    existe BOOLEAN, 
    mensaje VARCHAR, 
    numTur INTEGER, 
    numHistoria VARCHAR
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Validar que el turno sea válido
    IF p_numTur <= 0 THEN
        RETURN QUERY SELECT FALSE, 'El número de turno debe ser mayor a cero'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR;
        RETURN;
    END IF;
    
    -- Validar que la historia clínica no esté vacía
    IF p_numHistoria IS NULL OR p_numHistoria = '' THEN
        RETURN QUERY SELECT FALSE, 'El número de historia no puede estar vacío'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR;
        RETURN;
    END IF;
    
    -- Verificar si la consulta existe
    IF NOT EXISTS(
        SELECT 1 FROM public."Consulta" 
        WHERE "numTur" = p_numTur AND "numHisCli" = p_numHistoria
    ) THEN
        RETURN QUERY SELECT FALSE, 'La consulta no existe'::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR;
        RETURN;
    END IF;
    
    -- Eliminar la consulta
    DELETE FROM public."Consulta"
    WHERE "numTur" = p_numTur AND "numHisCli" = p_numHistoria;
    
    -- Retornar éxito
    RETURN QUERY SELECT TRUE, 'Consulta eliminada correctamente'::VARCHAR,
                        p_numTur, p_numHistoria;
    
EXCEPTION
    WHEN OTHERS THEN
        RETURN QUERY SELECT FALSE, ('Error inesperado: ' || SQLERRM)::VARCHAR,
                            NULL::INTEGER, NULL::VARCHAR;
END;
$$;