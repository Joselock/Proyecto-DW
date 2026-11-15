CREATE OR REPLACE FUNCTION public.eliminarUnidad(
   p_codUni VARCHAR
)

RETURNS TEXT AS $$

DECLARE
       existe BOOLEAN;

BEGIN

	 SELECT EXISTS(SELECT 1 FROM public."Unidad" WHERE "codUni" = p_codUni)INTO existe;

	 IF NOT existe THEN 
	    RETURN 'Error: unidad no encontrada';
	 END IF;

	 DELETE FROM public."Unidad"
	 WHERE "codUni" = p_codUni;

	 RETURN 'Unidad eliminada correctamente';

EXCEPTION 
   WHEN OTHERS THEN
       RETURN 'Error:'|| SQLERRM;
    
END;
$$ LANGUAGE plpgsql;