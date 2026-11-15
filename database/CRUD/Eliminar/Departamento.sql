CREATE OR REPLACE FUNCTION public.eliminarDepartamento(
   p_codDep VARCHAR
)

RETURNS TEXT AS $$

DECLARE
       existe BOOLEAN;

BEGIN

	 SELECT EXISTS(SELECT 1 FROM public."Departamento" WHERE "codDep" = p_codDep)INTO existe;

	 IF NOT existe THEN 
	    RETURN 'Error: departamento no encontrado';
	 END IF;

	 DELETE FROM public."Departamento"
	 WHERE "codDep" = p_codDep;

	 RETURN 'Departamento eliminado correctamente';

EXCEPTION 
   WHEN OTHERS THEN
       RETURN 'Error:'|| SQLERRM;
    
END;
$$ LANGUAGE plpgsql;