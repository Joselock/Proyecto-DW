CREATE OR REPLACE FUNCTION public.eliminarMedico(
   p_codMed VARCHAR
)

RETURNS TEXT AS $$

DECLARE
       existe BOOLEAN;

BEGIN

	 SELECT EXISTS(SELECT 1 FROM public."Medico" WHERE "codMed" = p_codMed)INTO existe;

	 IF NOT existe THEN 
	    RETURN 'Error: medico no encontrado';
	 END IF;

	 DELETE FROM public."Medico"
	 WHERE "codMed" = p_codMed;

	 RETURN 'Medico eliminado correctamente';

EXCEPTION 
   WHEN OTHERS THEN
       RETURN 'Error:'|| SQLERRM;
    
END;
$$ LANGUAGE plpgsql;