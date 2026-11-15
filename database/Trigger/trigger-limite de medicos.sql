CREATE TRIGGER trg_limitarMedicosPorUnidad
BEFORE INSERT OR UPDATE OF "codUni" ON public."Medico"
FOR EACH ROW
EXECUTE FUNCTION limitarMedicosPorUnidad();