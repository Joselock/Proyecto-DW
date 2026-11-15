CREATE OR REPLACE FUNCTION public.eliminarPaciente(
   p_numHistoria VARCHAR
)

RETURNS TEXT AS $$

DECLARE
       existe BOOLEAN;

BEGIN

	 SELECT EXISTS(SELECT 1 FROM public."Registro" WHERE "numHisCli" = p_numHistoria)INTO existe;

	 IF NOT existe THEN 
	    RETURN 'Error: paciente no encontrado en el registro';
	 END IF;

	 DELETE FROM public."Registro"
	 WHERE "numHisCli" = p_numHistoria;

	 RETURN 'Paciente eliminado correctamente';

EXCEPTION 
   WHEN OTHERS THEN
       RETURN 'Error:'|| SQLERRM;
    
END;
$$ LANGUAGE plpgsql;