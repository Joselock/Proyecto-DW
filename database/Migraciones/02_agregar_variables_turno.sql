-- Agregar los atributos siguientes a la tabla turno para 
-- poder derle seguimiento a los turnos mediante la fecha y
-- hora del sistema.

ALTER TABLE public."Turno" 
ADD COLUMN fecha DATE,
ADD COLUMN hora TIME,
ADD COLUMN notaClinica VARCHAR(500);