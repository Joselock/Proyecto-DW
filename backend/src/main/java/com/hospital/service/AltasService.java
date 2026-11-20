package com.hospital.service;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Altas reales de pacientes, turnos y médicos.
 *
 * La regla de visibilidad es la misma que en PanelService:
 *   - ADMIN: puede elegir cualquier hospital, unidad, médico o paciente.
 *   - MEDICO: solo puede crear en su unidad y con su ficha profesional.
 * Crear médicos es exclusivo de ADMIN.
 */
@Service
public class AltasService {

    private final JdbcTemplate jdbc;

    public AltasService(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    /* ------------------------------------------------------------------ */
    /* Catálogos para los formularios                                      */
    /* ------------------------------------------------------------------ */

    @Transactional(readOnly = true)
    public Map<String, Object> catalogos(String username, String role) {
        Perfil perfil = perfil(username, role);
        List<Object> hospitalArgs = new ArrayList<>();
        List<Object> departamentoArgs = new ArrayList<>();
        List<Object> unidadArgs = new ArrayList<>();
        List<Object> medicoArgs = new ArrayList<>();

        String filtroHospital = "";
        String filtroUnidad = "";
        String filtroMedico = "";
        if (!perfil.admin()) {
            filtroHospital = " WHERE h.\"codHos\" = ? ";
            hospitalArgs.add(perfil.codHos());
            departamentoArgs.add(perfil.codHos());
            filtroUnidad = " WHERE u.\"codUni\" = ? ";
            unidadArgs.add(perfil.codUni());
            filtroMedico = " WHERE h.\"codHos\" = ? AND m.\"codMed\" = ? ";
            medicoArgs.add(perfil.codHos());
            medicoArgs.add(perfil.codMed());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hospitales", jdbc.queryForList("""
                SELECT h."codHos" AS codigo, h."nomHos" AS nombre
                FROM public."Hospital" h
                """ + filtroHospital + """
                ORDER BY h."nomHos"
                """, hospitalArgs.toArray()));

        data.put("departamentos", jdbc.queryForList("""
                SELECT d."codDep" AS codigo, d."nomDep" AS nombre, d."codHos" AS "codHos"
                FROM public."Departamento" d
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                """ + filtroHospital + """
                ORDER BY d."nomDep"
                """, departamentoArgs.toArray()));

        data.put("unidades", jdbc.queryForList("""
                SELECT u."codUni" AS codigo, u."nomUni" AS nombre,
                       u."codDep" AS "codDep", d."codHos" AS "codHos"
                FROM public."Unidad" u
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                """ + (perfil.admin() ? "" : filtroUnidad) + """
                ORDER BY u."nomUni"
                """, perfil.admin() ? new Object[0] : unidadArgs.toArray()));

        data.put("medicos", jdbc.queryForList("""
                SELECT m."codMed" AS "codMed", m."nomMed" AS nombre,
                       m."espMed" AS especialidad, m."codUni" AS "codUni",
                       u."nomUni" AS unidad, d."codDep" AS "codDep",
                       d."nomDep" AS departamento, h."codHos" AS "codHos",
                       h."nomHos" AS hospital
                FROM public."Medico" m
                JOIN public."Unidad" u ON u."codUni" = m."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                """ + filtroMedico + """
                ORDER BY h."nomHos", d."nomDep", u."nomUni", m."nomMed"
                """, medicoArgs.toArray()));

        Map<String, Object> perfilData = new LinkedHashMap<>();
        perfilData.put("rol", perfil.admin() ? "ADMIN" : "MEDICO");
        perfilData.put("codMed", perfil.codMed());
        perfilData.put("codUni", perfil.codUni());
        perfilData.put("codHos", perfil.codHos());
        perfilData.put("nombre", perfil.nombre());
        data.put("perfil", perfilData);

        return data;
    }

    /* ------------------------------------------------------------------ */
    /* Pacientes                                                           */
    /* ------------------------------------------------------------------ */

    @Transactional
    public Map<String, Object> crearPaciente(String username, String role, Map<String, String> body) {
        Perfil perfil = perfil(username, role);

        String numero = texto(body.get("documento"));
        if (numero.isEmpty()) {
            numero = siguientePaciente();
        }
        String nombre = texto(body.get("nombre"));
        String direccion = texto(body.get("direccion"));
        String codUni = texto(body.get("codUni"));
        String estado = texto(body.get("estado"));
        if (estado.isEmpty()) estado = "Internado";

        LocalDate fechaNac = fecha(body.get("fechaNacimiento"));
        if (nombre.isEmpty()) throw new IllegalArgumentException("Escribe el nombre del paciente.");
        if (fechaNac == null) throw new IllegalArgumentException("Indica la fecha de nacimiento.");
        if (Period.between(fechaNac, LocalDate.now()).getYears() < 16) {
            throw new IllegalArgumentException("El paciente debe tener al menos 16 años.");
        }
        if (direccion.isEmpty()) throw new IllegalArgumentException("Indica la dirección del paciente.");
        if (!List.of("Internado", "Pendiente", "Alta").contains(estado)) {
            throw new IllegalArgumentException("Selecciona un estado válido.");
        }
        if (codUni.isEmpty()) throw new IllegalArgumentException("Selecciona la unidad de admisión.");

        validarUnidad(codUni, perfil, "pacientes");

        Map<String, Object> resultado = llamarFuncion(
                "SELECT * FROM public.insertarPaciente(?, ?, ?, ?, ?, ?)",
                numero, nombre, direccion, Date.valueOf(fechaNac), estado, codUni);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", numero);
        respuesta.put("nombre", nombre);
        respuesta.put("direccion", direccion);
        respuesta.put("fechaNacimiento", fechaNac.toString());
        respuesta.put("estado", estado);
        respuesta.put("codUni", codUni);
        respuesta.put("mensaje", texto(resultado.get("mensaje")));
        return respuesta;
    }

    /* ------------------------------------------------------------------ */
    /* Turnos                                                              */
    /* ------------------------------------------------------------------ */

    @Transactional
    public Map<String, Object> crearTurno(String username, String role, Map<String, String> body) {
        Perfil perfil = perfil(username, role);

        String pacienteId = texto(body.get("pacienteId"));
        String codMed = texto(body.get("codMed"));
        String codUni = texto(body.get("codUni"));
        String estado = texto(body.get("estado"));
        if (estado.isEmpty()) estado = "pendiente";
        estado = estado.toLowerCase(Locale.ROOT);
        if (!List.of("pendiente", "confirmado").contains(estado)) {
            throw new IllegalArgumentException("Selecciona un estado válido para el turno.");
        }

        if (!perfil.admin()) {
            codMed = perfil.codMed();
            codUni = perfil.codUni();
        }
        if (pacienteId.isEmpty()) throw new IllegalArgumentException("Selecciona un paciente.");
        if (codMed == null || codMed.isEmpty()) {
            throw new IllegalArgumentException("Tu cuenta no tiene una ficha médica vinculada.");
        }

        Map<String, Object> medico = jdbc.queryForList("""
                SELECT m."codMed", m."nomMed", m."codUni"
                FROM public."Medico" m
                WHERE m."codMed" = ?
                LIMIT 1
                """, codMed).stream().findFirst().orElse(null);
        if (medico == null) throw new IllegalArgumentException("El profesional seleccionado no existe.");

        // La unidad del turno siempre es la del médico que lo atiende.
        codUni = texto(medico.get("codUni"));
        if (!perfil.admin() && !perfil.codUni().equals(codUni)) {
            throw new SecurityException("Solo puedes crear turnos en tu unidad.");
        }

        validarPaciente(pacienteId, perfil);

        LocalDate fechaTurno = fecha(body.get("fecha"));
        if (fechaTurno == null) fechaTurno = LocalDate.now();
        Time horaTurno = hora(body.get("hora"));
        if (horaTurno == null) {
            horaTurno = Time.valueOf(LocalTime.now().withNano(0)
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        String notaClinica = texto(body.get("notaClinica"));
        if (notaClinica.isEmpty()) notaClinica = texto(body.get("nota"));

        int numero = siguienteTurno();
        Map<String, Object> turno = llamarFuncion(
                "SELECT * FROM public.insertarTurno(?, ?, ?, ?, ?, ?, ?, ?)",
                numero, 0, estado, codUni, codMed,
                Date.valueOf(fechaTurno), horaTurno, notaClinica);

        String causa = notaClinica.isEmpty() ? "OtrasCausas" : notaClinica;
        llamarFuncion(
                "SELECT * FROM public.insertarConsulta(?, ?, ?, ?)",
                numero, pacienteId, false, causa);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", "T-" + numero);
        respuesta.put("numero", numero);
        respuesta.put("pacienteId", pacienteId);
        respuesta.put("codMed", codMed);
        respuesta.put("profesional", texto(medico.get("nomMed")));
        respuesta.put("codUni", codUni);
        respuesta.put("estado", estado);
        respuesta.put("fecha", fechaTurno.toString());
        respuesta.put("hora", horaTurno.toLocalTime().toString().substring(0, 5));
        respuesta.put("notaClinica", notaClinica);
        respuesta.put("mensaje", texto(turno.get("mensaje")));
        return respuesta;
    }

    /**
     * Edita un turno desde el menú de tres puntos: permite cambiar día, hora,
     * nota clínica y estado. Marcar "no_atendido" revierte la consulta a no
     * atendida; marcar "finalizado" la deja como exitosa salvo indicación
     * contraria en el campo "atendido".
     */
    @Transactional
    public Map<String, Object> editarTurno(String username, String role, int numero,
            Map<String, String> body) {
        Perfil perfil = perfil(username, role);

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT t."codMed" AS cod_med, t."codUni" AS cod_uni,
                       t."estTur" AS estado,
                       COALESCE(to_char(t."fecha", 'YYYY-MM-DD'), '') AS fecha,
                       COALESCE(to_char(t."hora", 'HH24:MI'), '') AS hora,
                       COALESCE(t."nota_clinica", '') AS nota
                FROM public."Turno" t
                WHERE t."numTur" = ?
                """, numero);
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("El turno que intentas editar no existe.");
        }

        Map<String, Object> actual = filas.get(0);
        String codMedActual = texto(actual.get("cod_med"));
        if (!perfil.admin() && (perfil.codMed() == null || !perfil.codMed().equals(codMedActual))) {
            throw new SecurityException("Solo puedes editar tus propios turnos.");
        }

        LocalDate fechaTurno = body.containsKey("fecha")
                ? fecha(body.get("fecha"))
                : fecha(texto(actual.get("fecha")));
        if (fechaTurno == null) {
            throw new IllegalArgumentException("Indica una fecha válida para el turno (AAAA-MM-DD).");
        }

        Time horaTurno = body.containsKey("hora")
                ? hora(body.get("hora"))
                : hora(texto(actual.get("hora")));
        if (horaTurno == null) {
            throw new IllegalArgumentException("Indica una hora válida para el turno (HH:MM).");
        }

        String notaClinica = body.containsKey("notaClinica")
                ? texto(body.get("notaClinica"))
                : texto(actual.get("nota"));

        String estado = texto(body.get("estatus"));
        if (estado.isEmpty()) estado = texto(body.get("estadoOriginal"));
        if (estado.isEmpty()) estado = texto(actual.get("estado"));
        String estadoDb = estadoBase(estado);

        jdbc.update("""
                UPDATE public."Turno"
                SET "fecha" = ?, "hora" = ?, "nota_clinica" = ?, "estTur" = ?
                WHERE "numTur" = ?
                """, Date.valueOf(fechaTurno), horaTurno,
                notaClinica.isEmpty() ? null : notaClinica, estadoDb, numero);

        if ("no_atendido".equals(estadoDb)) {
            jdbc.update("""
                    UPDATE public."Consulta"
                    SET atendido = false, causa = ?
                    WHERE "numTur" = ?
                    """, notaClinica.isEmpty() ? "No atendido" : notaClinica, numero);
        } else if (body.containsKey("atendido")) {
            boolean atendido = Boolean.parseBoolean(texto(body.get("atendido")));
            jdbc.update("""
                    UPDATE public."Consulta" SET atendido = ? WHERE "numTur" = ?
                    """, atendido, numero);
        } else if ("finalizado".equals(estadoDb)) {
            jdbc.update("""
                    UPDATE public."Consulta" SET atendido = true WHERE "numTur" = ?
                    """, numero);
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", "T-" + numero);
        respuesta.put("numero", numero);
        respuesta.put("fecha", fechaTurno.toString());
        respuesta.put("hora", horaTurno.toLocalTime().toString().substring(0, 5));
        respuesta.put("notaClinica", notaClinica);
        respuesta.put("estatus", estadoDb);
        respuesta.put("mensaje", "Turno actualizado correctamente");
        return respuesta;
    }

    /** Convierte los estados de la interfaz a los valores guardados en la BD. */
    private static String estadoBase(String valor) {
        String estado = valor.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
        if (estado.contains("no atendido")) return "no_atendido";
        if (estado.contains("progreso") || estado.contains("curso")) return "en_progreso";
        if (estado.contains("finaliz") || estado.contains("atendid") || estado.equals("done")) return "finalizado";
        if (estado.contains("confirm")) return "confirmado";
        return "pendiente";
    }

    /* ------------------------------------------------------------------ */
    /* Médicos (solo ADMIN)                                                */
    /* ------------------------------------------------------------------ */

    @Transactional
    public Map<String, Object> crearMedico(String username, String role, Map<String, String> body) {
        Perfil perfil = perfil(username, role);
        if (!perfil.admin()) {
            throw new SecurityException("Solo un administrador puede agendar médicos.");
        }

        String codMed = texto(body.get("codMed"));
        if (codMed.isEmpty()) codMed = siguienteMedico();
        String nombre = texto(body.get("nombre"));
        String especialidad = texto(body.get("especialidad"));
        String licencia = texto(body.get("licencia"));
        String telefono = texto(body.get("telefono"));
        String datosContacto = texto(body.get("datosContacto"));
        String direccion = texto(body.get("direccion"));
        String codUni = texto(body.get("codUni"));
        int experiencia = entero(body.get("experiencia"));

        if (nombre.isEmpty()) throw new IllegalArgumentException("Escribe el nombre del médico.");
        if (especialidad.isEmpty()) throw new IllegalArgumentException("Indica la especialidad.");
        if (licencia.isEmpty()) throw new IllegalArgumentException("Indica el número de licencia.");
        if (telefono.isEmpty()) throw new IllegalArgumentException("Indica el teléfono.");
        if (experiencia < 0) throw new IllegalArgumentException("La experiencia no puede ser negativa.");
        if (datosContacto.isEmpty()) datosContacto = "Datos de contacto pendientes";
        if (codUni.isEmpty()) throw new IllegalArgumentException("Selecciona la unidad asignada.");

        validarUnidad(codUni, perfil, "médicos");

        llamarFuncion(
                "SELECT * FROM public.insertarMedico(?, ?, ?, ?, ?, ?, ?, ?)",
                codMed, nombre, especialidad, licencia, telefono,
                experiencia, datosContacto, codUni);

        if (!direccion.isEmpty()) {
            jdbc.update("UPDATE public.\"Medico\" SET \"dirMed\" = ? WHERE \"codMed\" = ?",
                    direccion, codMed);
        }

        Map<String, Object> ubicacion = jdbc.queryForList("""
                SELECT u."nomUni" AS unidad, d."nomDep" AS departamento,
                       d."codHos" AS "codHos", h."nomHos" AS hospital
                FROM public."Medico" m
                JOIN public."Unidad" u ON u."codUni" = m."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                WHERE m."codMed" = ?
                """, codMed).stream().findFirst().orElse(Map.of());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("codMed", codMed);
        respuesta.put("nombre", nombre);
        respuesta.put("especialidad", especialidad);
        respuesta.put("licencia", licencia);
        respuesta.put("telefono", telefono);
        respuesta.put("experiencia", experiencia);
        respuesta.put("datosContacto", datosContacto);
        respuesta.put("direccion", direccion);
        respuesta.put("codUni", codUni);
        respuesta.put("unidad", ubicacion.get("unidad"));
        respuesta.put("departamento", ubicacion.get("departamento"));
        respuesta.put("codHos", ubicacion.get("codHos"));
        respuesta.put("hospital", ubicacion.get("hospital"));
        respuesta.put("mensaje", "Médico registrado correctamente");
        return respuesta;
    }

    /* ------------------------------------------------------------------ */
    /* Apoyo                                                               */
    /* ------------------------------------------------------------------ */

    private void validarUnidad(String codUni, Perfil perfil, String recurso) {
        if (!perfil.admin() && codUni.equals(perfil.codUni())) {
            return;
        }
        Integer existe = jdbc.queryForObject(
                "SELECT count(*) FROM public.\"Unidad\" WHERE \"codUni\" = ?",
                Integer.class, codUni);
        if (existe == null || existe == 0) {
            throw new IllegalArgumentException("La unidad seleccionada no existe.");
        }
        if (!perfil.admin()) {
            throw new SecurityException("Solo puedes registrar " + recurso + " en tu unidad.");
        }
    }

    private void validarPaciente(String pacienteId, Perfil perfil) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT r."codUni"
                FROM public."Registro" r
                WHERE r."numHisCli" = ?
                LIMIT 1
                """, pacienteId);
        if (filas.isEmpty()) {
            throw new IllegalArgumentException("El paciente seleccionado no existe.");
        }
        if (!perfil.admin()) {
            String codUni = texto(filas.get(0).get("codUni"));
            boolean propio = perfil.codUni().equals(codUni) || jdbc.queryForObject("""
                    SELECT count(*) > 0
                    FROM public."Consulta" c
                    JOIN public."Turno" t ON t."numTur" = c."numTur"
                    WHERE c."numHisCli" = ? AND t."codMed" = ?
                    """, Boolean.class, pacienteId, perfil.codMed());
            if (!Boolean.TRUE.equals(propio)) {
                throw new SecurityException("Ese paciente no está bajo tu atención.");
            }
        }
    }

    private Map<String, Object> llamarFuncion(String sql, Object... args) {
        Map<String, Object> resultado;
        try {
            resultado = jdbc.queryForMap(sql, args);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new IllegalStateException("La base de datos no devolvió confirmación de la operación.");
        }
        if (!Boolean.TRUE.equals(resultado.get("existe"))) {
            String mensaje = texto(resultado.get("mensaje"));
            throw new IllegalStateException(mensaje.isEmpty() ? "No se pudo completar la operación." : mensaje);
        }
        return resultado;
    }

    private String siguientePaciente() {
        Integer maximo = jdbc.queryForObject("""
                SELECT COALESCE(max(nullif(regexp_replace("numHisCli", '[^0-9]', '', 'g'), '')::int), 1000) + 1
                FROM public."Paciente"
                """, Integer.class);
        return String.format("P%04d", maximo == null ? 1001 : maximo);
    }

    private int siguienteTurno() {
        Integer maximo = jdbc.queryForObject(
                "SELECT COALESCE(max(\"numTur\"), 0) + 1 FROM public.\"Turno\"",
                Integer.class);
        return maximo == null ? 1 : maximo;
    }

    private String siguienteMedico() {
        Integer maximo = jdbc.queryForObject("""
                SELECT COALESCE(max(nullif(regexp_replace("codMed", '[^0-9]', '', 'g'), '')::int), 0) + 1
                FROM public."Medico"
                """, Integer.class);
        return String.format("M%03d", maximo == null ? 1 : maximo);
    }

    private Perfil perfil(String username, String role) {
        boolean admin = "ADMIN".equalsIgnoreCase(role == null ? "" : role);
        if (admin || username == null || username.isBlank()) {
            return new Perfil(admin, null, null, null, null);
        }

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT m."codMed" AS cod_med, m."nomMed" AS nombre,
                       m."codUni" AS cod_uni, d."codHos" AS cod_hos
                FROM public."Medico" m
                JOIN public."Unidad" u ON u."codUni" = m."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                WHERE m.username = ?
                LIMIT 1
                """, username);

        if (filas.isEmpty()) {
            return new Perfil(false, null, null, null, null);
        }
        Map<String, Object> fila = filas.get(0);
        return new Perfil(false, texto(fila.get("cod_med")), texto(fila.get("nombre")),
                texto(fila.get("cod_uni")), texto(fila.get("cod_hos")));
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDate.parse(valor.trim().substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Time hora(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String textoHora = valor.trim();
        if (textoHora.matches("\\d{1,2}:\\d{2}")) textoHora += ":00";
        try {
            return Time.valueOf(textoHora);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Indica una hora válida en formato HH:MM.");
        }
    }

    private static int entero(String valor) {
        if (valor == null || valor.isBlank()) return 0;
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    private record Perfil(boolean admin, String codMed, String nombre, String codUni, String codHos) {
    }
}
