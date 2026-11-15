CREATE OR REPLACE FUNCTION public.eliminarHospital(
   p_codHos VARCHAR
)

RETURNS TEXT AS $$

DECLARE
       existe BOOLEAN;

BEGIN

	 SELECT EXISTS(SELECT 1 FROM public."Hospital" WHERE "codHos" = p_codHos)INTO existe;

	 IF NOT existe THEN 
	    RETURN 'Error: hospital no encontrado';
	 END IF;

	 DELETE FROM public."Hospital"
	 WHERE "codHos" = p_codHos;

	 RETURN 'Hospital eliminado correctamente';

EXCEPTION 
   WHEN OTHERS THEN
       RETURN 'Error:'|| SQLERRM;
    
END;
$$ LANGUAGE plpgsql;