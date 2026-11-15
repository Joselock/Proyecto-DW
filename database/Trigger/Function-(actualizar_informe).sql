CREATE OR REPLACE FUNCTION public.actualizar_informe_trigger()
RETURNS TRIGGER AS $$
DECLARE
    v_numTur INTEGER;
    v_codUni VARCHAR;
    v_total INT;
    v_pacAten INT;
    v_pacAlt INT;
    v_cantAdm INT;
    v_informe_existe BOOLEAN;
    v_next_id INTEGER;
BEGIN
    -- Determinar el turno afectado
    IF TG_OP = 'DELETE' THEN
        v_numTur := OLD."numTur";
    ELSE
        v_numTur := NEW."numTur";
    END IF;

    -- Obtener la unidad del turno
    SELECT "codUni" INTO v_codUni
    FROM public."Turno"
    WHERE "numTur" = v_numTur;
    
    IF NOT FOUND THEN
        RAISE WARNING 'Turno % no encontrado. No se puede actualizar informe.', v_numTur;
        RETURN NULL;
    END IF;

    -- 1. Pacientes atendidos en el turno
    SELECT COUNT(*) INTO v_pacAten
    FROM public."Consulta"
    WHERE "numTur" = v_numTur AND atendido = true;

    -- 2. Pacientes dados de alta (atendidos y estado 'Alta')
    SELECT COUNT(*)
    INTO v_pacAlt
    FROM public."Consulta" c
    JOIN public."Registro" r ON c."numHisCli" = r."numHisCli"
    WHERE c."numTur" = v_numTur
      AND c.atendido = true
      AND r."estPac" = 'Alta';

    -- 3. Cantidad de pacientes admitidos el día del informe en la unidad
    --    Se asume que el informe refleja el estado al final del día o en el momento actual.
    --    Los admitidos son aquellos cuya fecha de admisión es igual a la fecha actual.
    SELECT COUNT(*) INTO v_cantAdm
    FROM public."Registro"
    WHERE "codUni" = v_codUni
      AND "fecha_admision" = CURRENT_DATE;

    -- 4. Total actual de pacientes en la unidad (todos, incluyendo los admitidos hoy y anteriores)
    SELECT COUNT(*) INTO v_total
    FROM public."Registro"
    WHERE "codUni" = v_codUni;

    -- Verificar si ya existe informe para este turno
    SELECT EXISTS(SELECT 1 FROM public."Informe" WHERE "numTur" = v_numTur)
    INTO v_informe_existe;

    IF v_informe_existe THEN
        -- Actualizar informe existente
        UPDATE public."Informe"
        SET "pacAten" = v_pacAten,
            "pacAlt"  = v_pacAlt,
            "cantAdm" = v_cantAdm,
            "total"   = v_total,
            "fecha"   = CURRENT_DATE,
            "hora"    = CURRENT_TIME
        WHERE "numTur" = v_numTur;
    ELSE
        -- Crear nuevo informe con secuencia para numIn
        CREATE SEQUENCE IF NOT EXISTS seq_num_informe;
        v_next_id := nextval('seq_num_informe');
        
        INSERT INTO public."Informe" 
            ("numIn", "hora", "fecha", "pacAten", "pacAlt", "cantAdm", "total", "codUni", "numTur")
        VALUES (
            v_next_id::VARCHAR,
            CURRENT_TIME,
            CURRENT_DATE,
            v_pacAten,
            v_pacAlt,
            v_cantAdm,
            v_total,
            v_codUni,
            v_numTur
        );
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;