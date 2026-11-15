CREATE OR REPLACE FUNCTION limitarMedicosPorUnidad()
RETURNS TRIGGER AS $$
DECLARE
    total INTEGER;
BEGIN
    -- Para UPDATE: si la unidad no cambia, permitir sin contar (evita auto-rechazo)
    IF TG_OP = 'UPDATE' AND OLD."codUni" = NEW."codUni" THEN
        RETURN NEW;
    END IF;

    -- Contar médicos en la unidad NUEVA (sin contar el médico actual si es UPDATE)
    SELECT COUNT(*) INTO total
    FROM public."Medico"
    WHERE "codUni" = NEW."codUni"
      AND (TG_OP = 'INSERT' OR "codMed" <> OLD."codMed");  -- Excluir al propio médico

    IF total >= 10 THEN
        RAISE EXCEPTION 'La unidad % ya tiene 10 médicos, no se puede agregar más.', NEW."codUni";
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;