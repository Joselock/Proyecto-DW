DROP TRIGGER IF EXISTS trg_actualizar_informe ON public."Consulta";
CREATE TRIGGER trg_actualizar_informe
AFTER INSERT OR UPDATE OR DELETE ON public."Consulta"
FOR EACH ROW
EXECUTE FUNCTION public.actualizar_informe_trigger();