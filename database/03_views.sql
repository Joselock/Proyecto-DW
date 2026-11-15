-- ============================================================================
-- PROYECTO-DW · VISTAS DE APOYO
-- Archivo : database/03_views.sql
--
-- Vistas de solo lectura para consultar la jerarquía hospitalaria, la
-- relación entre médicos y usuarios, la ubicación de los pacientes y la
-- ocupación de médicos por unidad.
--
-- Se pueden ejecutar después de 01_schema.sql. Son idempotentes.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- Jerarquía completa: hospital -> departamento -> unidad
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW public."vw_JerarquiaHospitalaria" AS
SELECT
    h."codHos",
    h."nomHos",
    d."codDep",
    d."nomDep",
    u."codUni",
    u."nomUni",
    u."ubiUni"
FROM public."Hospital" h
LEFT JOIN public."Departamento" d ON d."codHos" = h."codHos"
LEFT JOIN public."Unidad" u ON u."codDep" = d."codDep";

COMMENT ON VIEW public."vw_JerarquiaHospitalaria" IS
    'Hospital, departamento y unidad en una sola fila.';

-- ----------------------------------------------------------------------------
-- Médico + cuenta de usuario + perfil + ubicación
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW public."vw_MedicoUsuario" AS
SELECT
    m."codMed",
    m."nomMed",
    m."espMed",
    m."numLic",
    m."telMed",
    m."exp",
    m."datCon",
    m.username,
    u.email,
    u.role,
    du.nombre       AS nombre_cuenta,
    du.especialidad AS especialidad_cuenta,
    du.direccion,
    du.telefono     AS telefono_cuenta,
    m."codUni",
    un."nomUni",
    dep."codDep",
    dep."nomDep",
    hor."codHos",
    hor."nomHos"
FROM public."Medico" m
LEFT JOIN public."user" u
    ON u.username = m.username
LEFT JOIN public."data_user" du
    ON du.username = u.username
   AND du.email = u.email
JOIN public."Unidad" un
    ON un."codUni" = m."codUni"
JOIN public."Departamento" dep
    ON dep."codDep" = un."codDep"
JOIN public."Hospital" hor
    ON hor."codHos" = dep."codHos";

COMMENT ON VIEW public."vw_MedicoUsuario" IS
    'Médico con su cuenta de usuario, perfil y ubicación organizacional.';

-- ----------------------------------------------------------------------------
-- Paciente + admisión + unidad + departamento + hospital
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW public."vw_PacienteUnidad" AS
SELECT
    p."numHisCli",
    p."nomPac",
    p."fechaN",
    p."dirPac",
    r."estPac",
    r.fecha_admision,
    u."codUni",
    u."nomUni",
    d."codDep",
    d."nomDep",
    h."codHos",
    h."nomHos"
FROM public."Paciente" p
JOIN public."Registro" r ON r."numHisCli" = p."numHisCli"
JOIN public."Unidad" u ON u."codUni" = r."codUni"
JOIN public."Departamento" d ON d."codDep" = u."codDep"
JOIN public."Hospital" h ON h."codHos" = d."codHos";

COMMENT ON VIEW public."vw_PacienteUnidad" IS
    'Paciente con su admisión vigente y ubicación hospitalaria.';

-- ----------------------------------------------------------------------------
-- Ocupación de médicos por unidad (límite de negocio: 10)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW public."vw_OcupacionUnidad" AS
SELECT
    h."nomHos",
    d."nomDep",
    u."codUni",
    u."nomUni",
    count(m."codMed")::integer              AS cant_medicos,
    (10 - count(m."codMed"))::integer       AS cupos_disponibles
FROM public."Unidad" u
JOIN public."Departamento" d ON d."codDep" = u."codDep"
JOIN public."Hospital" h ON h."codHos" = d."codHos"
LEFT JOIN public."Medico" m ON m."codUni" = u."codUni"
GROUP BY h."nomHos", d."nomDep", u."codUni", u."nomUni";

COMMENT ON VIEW public."vw_OcupacionUnidad" IS
    'Cantidad de médicos por unidad y cupos disponibles antes del límite de 10.';

COMMIT;
