-- ============================================
-- HOSPITALES ADICIONALES (3 hospitales con menos pacientes)
-- ============================================

-- 1. HOSPITAL H006 (20 pacientes)
INSERT INTO public."Hospital" ("codHos", "nomHos") VALUES
('H006', 'Hospital de la Mujer'),
('H007', 'Hospital Geriátrico Nacional'),
('H008', 'Hospital Oncológico del Centro');

-- 2. DEPARTAMENTOS PARA H006, H007, H008
INSERT INTO public."Departamento" ("codDep", "nomDep", "codHos") VALUES
-- Hospital H006
('D018', 'Ginecología Obstétrica', 'H006'),
('D019', 'Maternidad', 'H006'),
-- Hospital H007
('D020', 'Geriatría', 'H007'),
('D021', 'Rehabilitación', 'H007'),
-- Hospital H008
('D022', 'Quimioterapia', 'H008'),
('D023', 'Radioterapia', 'H008');

-- 3. UNIDADES PARA H006, H007, H008
INSERT INTO public."Unidad" ("codUni", "nomUni", "ubiUni", "codDep") VALUES
-- H006
('U021', 'Unidad de Ginecología', 'Piso 2 Ala Norte', 'D018'),
('U022', 'Sala de Partos', 'Piso 1 Ala Central', 'D019'),
-- H007
('U023', 'Unidad de Geriatría Aguda', 'Piso 3 Ala Sur', 'D020'),
('U024', 'Unidad de Rehabilitación', 'Piso 2 Ala Oeste', 'D021'),
-- H008
('U025', 'Unidad de Quimioterapia', 'Piso 4 Ala Este', 'D022'),
('U026', 'Unidad de Radioterapia', 'Piso 3 Ala Norte', 'D023');

-- 4. MÉDICOS PARA H006, H007, H008
INSERT INTO public."Medico" ("codMed", "nomMed", "espMed", "numLic", "telMed", "codUni", "exp", "datCon") VALUES
('M017', 'Laura Jiménez', 'Ginecóloga', 'LIC12361', '5552001', 'U021', 8, 'Consultas 9:00-14:00'),
('M018', 'Roberto Torres', 'Obstetra', 'LIC12362', '5552002', 'U022', 10, 'Partos programados'),
('M019', 'Carmen Espinoza', 'Geriatra', 'LIC12363', '5552003', 'U023', 15, 'Cuidados paliativos'),
('M020', 'Andrés Muñoz', 'Rehabilitador', 'LIC12364', '5552004', 'U024', 6, 'Fisioterapia'),
('M021', 'Paula Rojas', 'Oncóloga', 'LIC12365', '5552005', 'U025', 12, 'Quimioterapia ambulatoria'),
('M022', 'Francisco Díaz', 'Radioterapeuta', 'LIC12366', '5552006', 'U026', 9, 'Radioterapia externa');

-- ============================================
-- PACIENTES HOSPITAL H006 (20 pacientes)
-- ============================================
INSERT INTO public."Registro" ("estPac", "codUni", "numHisCli", fecha_admision) VALUES
('Alta', 'U021', 'P0601', '2026-04-01'),
('Hospitalizado', 'U021', 'P0602', '2026-04-05'),
('Alta', 'U021', 'P0603', '2026-04-08'),
('Hospitalizado', 'U022', 'P0604', '2026-04-10'),
('Alta', 'U022', 'P0605', '2026-04-12'),
('Hospitalizado', 'U021', 'P0606', '2026-04-15'),
('Alta', 'U021', 'P0607', '2026-04-18'),
('Hospitalizado', 'U022', 'P0608', '2026-04-20'),
('Alta', 'U022', 'P0609', '2026-04-22'),
('Hospitalizado', 'U021', 'P0610', '2026-04-25'),
('Alta', 'U021', 'P0611', '2026-04-28'),
('Hospitalizado', 'U022', 'P0612', '2026-05-01'),
('Alta', 'U022', 'P0613', '2026-05-03'),
('Hospitalizado', 'U021', 'P0614', '2026-05-05'),
('Alta', 'U021', 'P0615', '2026-05-08'),
('Hospitalizado', 'U022', 'P0616', '2026-05-10'),
('Alta', 'U021', 'P0617', '2026-05-12'),
('Hospitalizado', 'U021', 'P0618', '2026-05-14'),
('Alta', 'U022', 'P0619', '2026-05-16'),
('Hospitalizado', 'U022', 'P0620', '2026-05-18');

-- PACIENTES H006
INSERT INTO public."Paciente" ("numHisCli", "nomPac", "dirPac", "fechaN") VALUES
('P0601', 'María José Pérez', 'Calle Flores 120', '1985-03-10'),
('P0602', 'Ana Victoria Rojas', 'Avenida del Parque 234', '1990-07-15'),
('P0603', 'Laura Patricia Mora', 'Calle Alameda 345', '1988-11-22'),
('P0604', 'Carla Andrea Soto', 'Avenida Primavera 456', '1993-04-08'),
('P0605', 'Paula Andrea Méndez', 'Calle Los Álamos 567', '1986-09-19'),
('P0606', 'Sandra Milena Peña', 'Avenida del Bosque 678', '1991-12-01'),
('P0607', 'Verónica Andrea Rivas', 'Calle del Lago 789', '1984-06-25'),
('P0608', 'Tatiana Isabel Vargas', 'Avenida de los Pinos 890', '1995-02-14'),
('P0609', 'Mónica Patricia Salazar', 'Calle del Mar 901', '1987-08-30'),
('P0610', 'Rosa Emilia Torres', 'Avenida del Sol 012', '1992-05-17'),
('P0611', 'Silvia Lorena Castro', 'Calle de la Luna 123', '1983-10-03'),
('P0612', 'María Fernanda Lira', 'Avenida de las Estrellas 234', '1996-03-22'),
('P0613', 'Cecilia Andrea Cáceres', 'Calle del Río 345', '1989-07-11'),
('P0614', 'Patricia Alejandra Sandoval', 'Avenida del Valle 456', '1980-12-28'),
('P0615', 'Andrea Carolina Alarcón', 'Calle de la Montaña 567', '1994-04-05'),
('P0616', 'Lorena Belén González', 'Avenida del Lago 678', '1986-01-18'),
('P0617', 'Carmen Gloria Muñoz', 'Calle del Bosque 789', '1990-09-26'),
('P0618', 'Cristina Paz Gutiérrez', 'Avenida de los Pinos 890', '1982-06-09'),
('P0619', 'Juliana Andrea Herrera', 'Calle del Sol 901', '1995-11-03'),
('P0620', 'Daniela Alejandra Vargas', 'Avenida del Mar 012', '1988-04-21');

-- ============================================
-- PACIENTES HOSPITAL H007 (15 pacientes)
-- ============================================
INSERT INTO public."Registro" ("estPac", "codUni", "numHisCli", fecha_admision) VALUES
('Alta', 'U023', 'P0621', '2026-04-02'),
('Hospitalizado', 'U023', 'P0622', '2026-04-06'),
('Alta', 'U023', 'P0623', '2026-04-09'),
('Hospitalizado', 'U024', 'P0624', '2026-04-11'),
('Alta', 'U024', 'P0625', '2026-04-13'),
('Hospitalizado', 'U023', 'P0626', '2026-04-16'),
('Alta', 'U023', 'P0627', '2026-04-19'),
('Hospitalizado', 'U024', 'P0628', '2026-04-21'),
('Alta', 'U024', 'P0629', '2026-04-23'),
('Hospitalizado', 'U023', 'P0630', '2026-04-26'),
('Alta', 'U023', 'P0631', '2026-04-29'),
('Hospitalizado', 'U024', 'P0632', '2026-05-02'),
('Alta', 'U024', 'P0633', '2026-05-04'),
('Hospitalizado', 'U023', 'P0634', '2026-05-06'),
('Alta', 'U023', 'P0635', '2026-05-09');

-- PACIENTES H007
INSERT INTO public."Paciente" ("numHisCli", "nomPac", "dirPac", "fechaN") VALUES
('P0621', 'Juan Bautista Gómez', 'Calle Los Abuelos 45', '1930-02-15'),
('P0622', 'María Elena Fernández', 'Avenida de la Tercera Edad 67', '1925-08-22'),
('P0623', 'Pedro José Martínez', 'Calle del Recuerdo 89', '1928-11-30'),
('P0624', 'Carmen Rosa Sánchez', 'Avenida de los Mayores 101', '1932-05-18'),
('P0625', 'José Antonio Romero', 'Calle de la Experiencia 112', '1926-09-05'),
('P0626', 'Ana María Díaz', 'Avenida del Tiempo 123', '1929-12-12'),
('P0627', 'Luis Alberto Pérez', 'Calle de la Memoria 134', '1931-03-28'),
('P0628', 'Rosa Elena González', 'Avenida de la Historia 145', '1927-07-09'),
('P0629', 'Carlos Andrés Morales', 'Calle del Pasado 156', '1933-10-17'),
('P0630', 'Isabel Cristina Jiménez', 'Avenida de la Sabiduría 167', '1924-01-04'),
('P0631', 'Francisco Javier García', 'Calle de los Años 178', '1930-06-25'),
('P0632', 'Luisa Fernanda Rivas', 'Avenida de la Longevidad 189', '1928-11-11'),
('P0633', 'Miguel Ángel Castro', 'Calle de la Vida Larga 190', '1926-04-06'),
('P0634', 'Sofía Elena Vásquez', 'Avenida del Recuerdo 191', '1932-09-19'),
('P0635', 'Andrés Felipe Ortega', 'Calle de la Tercera Edad 192', '1929-02-28');

-- ============================================
-- PACIENTES HOSPITAL H008 (10 pacientes)
-- ============================================
INSERT INTO public."Registro" ("estPac", "codUni", "numHisCli", fecha_admision) VALUES
('Alta', 'U025', 'P0636', '2026-04-03'),
('Hospitalizado', 'U025', 'P0637', '2026-04-07'),
('Alta', 'U026', 'P0638', '2026-04-10'),
('Hospitalizado', 'U026', 'P0639', '2026-04-12'),
('Alta', 'U025', 'P0640', '2026-04-14'),
('Hospitalizado', 'U025', 'P0641', '2026-04-17'),
('Alta', 'U026', 'P0642', '2026-04-19'),
('Hospitalizado', 'U026', 'P0643', '2026-04-22'),
('Alta', 'U025', 'P0644', '2026-04-24'),
('Hospitalizado', 'U025', 'P0645', '2026-04-27');

-- PACIENTES H008
INSERT INTO public."Paciente" ("numHisCli", "nomPac", "dirPac", "fechaN") VALUES
('P0636', 'Roberto Carlos Silva', 'Calle de la Esperanza 50', '1965-03-20'),
('P0637', 'Martha Lucía Rojas', 'Avenida del Coraje 60', '1970-07-11'),
('P0638', 'Alberto José Martínez', 'Calle de la Fortaleza 70', '1962-11-30'),
('P0639', 'Gabriela Isabel Muñoz', 'Avenida de la Vida 80', '1978-05-22'),
('P0640', 'Ricardo Andrés Herrera', 'Calle del Valor 90', '1968-09-15'),
('P0641', 'Diana Marcela Castro', 'Avenida de la Lucha 100', '1975-12-03'),
('P0642', 'Jorge Enrique Peña', 'Calle de la Superación 110', '1960-06-25'),
('P0643', 'Patricia Elena Gutiérrez', 'Avenida de la Fuerza 120', '1972-08-19'),
('P0644', 'Oscar Alfonso Mendoza', 'Calle de la Perseverancia 130', '1963-10-31'),
('P0645', 'Adriana Milena Vargas', 'Avenida de la Victoria 140', '1976-04-14');

-- ============================================
-- TURNOS PARA H006, H007, H008
-- ============================================
INSERT INTO public."Turno" ("numTur", "cantAtenTur", "estTur", "codUni", "codMed") VALUES
(21, 5, 'Finalizado', 'U021', 'M017'),
(22, 4, 'Finalizado', 'U022', 'M018'),
(23, 3, 'Finalizado', 'U023', 'M019'),
(24, 3, 'Finalizado', 'U024', 'M020'),
(25, 2, 'Finalizado', 'U025', 'M021'),
(26, 2, 'Finalizado', 'U026', 'M022');

-- ============================================
-- CONSULTAS PARA H006, H007, H008
-- ============================================
INSERT INTO public."Consulta" ("numTur", "numHisCli", "atendido", "causa") VALUES
(21, 'P0601', true, 'Chequeo ginecológico'),
(21, 'P0603', true, 'Infección vaginal'),
(21, 'P0606', true, 'Irregularidad menstrual'),
(22, 'P0604', true, 'Parto natural'),
(22, 'P0608', true, 'Cesárea programada'),
(23, 'P0621', true, 'Deterioro cognitivo'),
(23, 'P0623', true, 'Problemas de movilidad'),
(24, 'P0625', true, 'Fractura de cadera'),
(25, 'P0636', true, 'Cáncer de mama'),
(25, 'P0638', true, 'Leucemia'),
(26, 'P0640', true, 'Radioterapia paliativa'),
(21, 'P0611', true, 'Control prenatal'),
(23, 'P0627', true, 'Artrosis'),
(24, 'P0629', true, 'Accidente cerebrovascular'),
(25, 'P0642', true, 'Quimioterapia preventiva');

-- ============================================
-- INFORMES PARA H006, H007, H008
-- ============================================
INSERT INTO public."Informe" (hora, fecha, "numIn", "pacAten", "pacAlt", "cantAdm", total, "codUni", "numTur") VALUES
('08:00:00-04', '2026-05-14', 'I016', 5, 4, 1, 10, 'U021', 21),
('09:00:00-04', '2026-05-14', 'I017', 4, 3, 1, 8, 'U022', 22),
('10:00:00-04', '2026-05-14', 'I018', 3, 2, 1, 6, 'U023', 23),
('11:00:00-04', '2026-05-14', 'I019', 3, 2, 1, 6, 'U024', 24),
('12:00:00-04', '2026-05-14', 'I020', 2, 1, 1, 4, 'U025', 25),
('13:00:00-04', '2026-05-14', 'I021', 2, 1, 1, 4, 'U026', 26);

-- ============================================
-- RESUMEN DE PACIENTES POR HOSPITAL
-- ============================================
-- H001: 120 pacientes (P0001-P0120)
-- H002: 120 pacientes (P0121-P0240)
-- H003: 120 pacientes (P0241-P0360)
-- H004: 120 pacientes (P0361-P0480)
-- H005: 120 pacientes (P0481-P0600)
-- H006:  20 pacientes (P0601-P0620)
-- H007:  15 pacientes (P0621-P0635)
-- H008:  10 pacientes (P0636-P0645)
-- TOTAL: 645 pacientes