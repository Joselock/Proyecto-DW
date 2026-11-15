CREATE OR REPLACE FUNCTION public.eliminarTurno(
   p_numTur INTEGER
)

RETURNS TEXT AS $$

DECLARE
       existe BOOLEAN;

BEGIN

	 SELECT EXISTS(SELECT 1 FROM public."Turno" WHERE "numTur" = p_numTur)INTO existe;

	 IF NOT existe THEN 
	    RETURN 'Error: turno no encontrado.';
	 END IF;

	 DELETE FROM public."Turno"
	 WHERE "numTur" = p_numTur;

	 RETURN 'Turno eliminado correctamente';

EXCEPTION 
   WHEN OTHERS THEN
       RETURN 'Error:'|| SQLERRM;
    
END;
$$ LANGUAGE plpgsql;