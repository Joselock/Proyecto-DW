CREATE OR REPLACE FUNCTION public.eliminarInforme(
    p_numIn VARCHAR
)
RETURNS TEXT AS $$
DECLARE
    existe BOOLEAN;
BEGIN
    SELECT EXISTS(SELECT 1 FROM public."Informe" WHERE "numIn" = p_numIn) INTO existe;
    IF NOT existe THEN
        RETURN 'Error: informe no encontrado.';
    END IF;
    DELETE FROM public."Informe" WHERE "numIn" = p_numIn;
    RETURN 'Informe eliminado correctamente.';
EXCEPTION
    WHEN OTHERS THEN
        RETURN 'Error: ' || SQLERRM;
END;
$$ LANGUAGE plpgsql;