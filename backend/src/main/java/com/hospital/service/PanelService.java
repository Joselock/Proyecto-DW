package com.hospital.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Datos que alimentan las secciones "Pacientes" y "Turnos" del dashboard.
 *
 * Regla de visibilidad:
 *   - ADMIN: ve todos los pacientes y turnos de todos los hospitales.
 *   - MEDICO: ve solo los turnos asignados a su codMed y los pacientes que
 *     atendió en esos turnos o que están ingresados en su unidad.
 *
 * El perfil del médico se resuelve desde la relación 1:1
 * Medico.username -> user.username. Si la cuenta no tiene médico vinculado,
 * el usuario no ve filas (no hay forma de saber a qué unidad pertenece).
 */
@Service
public class PanelService {

    private static final int DEFAULT_LIMIT = 5000;
    private static final int MAX_LIMIT = 20000;

    private final JdbcTemplate jdbcTemplate;

    public PanelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> pacientes(String username, String role, String q, Integer limit, Integer offset) {
        Perfil perfil = resolverPerfil(username, role);

        StringBuilder sql = new StringBuilder("""
                SELECT p."numHisCli" AS id,
                       p."nomPac" AS nombre,
                       to_char(p."fechaN", 'YYYY-MM-DD') AS nacimiento,
                       to_char(r."fecha_admision", 'YYYY-MM-DD') AS admision,
                       COALESCE(p."dirPac", '') AS direccion,
                       r."estPac" AS estado,
                       u."nomUni" AS unidad,
                       d."nomDep" AS departamento,
                       h."nomHos" AS hospital,
                       COALESCE(ult.medico, '') AS medico,
                       to_char(ult.fecha, 'YYYY-MM-DD') AS ultima_visita,
                       EXISTS (
                           SELECT 1
                           FROM public."Consulta" cp
                           JOIN public."Turno" tp ON tp."numTur" = cp."numTur"
                           WHERE cp."numHisCli" = p."numHisCli"
                             AND lower(tp."estTur") LIKE '%pendiente%'
                       ) AS pendiente,
                       COUNT(*) OVER () AS total
                FROM public."Registro" r
                JOIN public."Paciente" p ON p."numHisCli" = r."numHisCli"
                JOIN public."Unidad" u ON u."codUni" = r."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                LEFT JOIN LATERAL (
                    SELECT m."nomMed" AS medico, i."fecha" AS fecha
                    FROM public."Consulta" c
                    JOIN public."Turno" t ON t."numTur" = c."numTur"
                    LEFT JOIN public."Medico" m ON m."codMed" = t."codMed"
                    LEFT JOIN public."Informe" i ON i."numTur" = t."numTur"
                    WHERE c."numHisCli" = p."numHisCli"
                    ORDER BY t."numTur" DESC
                    LIMIT 1
                ) ult ON true
                """);

        List<Object> args = new ArrayList<>();
        boolean tieneWhere = false;

        if (!perfil.admin()) {
            sql.append("""
                    WHERE (r."codUni" = ?
                           OR EXISTS (
                               SELECT 1
                               FROM public."Consulta" cm
                               JOIN public."Turno" tm ON tm."numTur" = cm."numTur"
                               WHERE cm."numHisCli" = p."numHisCli"
                                 AND tm."codMed" = ?
                           ))
                    """);
            args.add(perfil.codUni());
            args.add(perfil.codMed());
            tieneWhere = true;
        }

        if (q != null && !q.isBlank()) {
            sql.append(tieneWhere ? " AND " : " WHERE ");
            sql.append("""
                    (lower(p."nomPac") LIKE ? OR lower(p."numHisCli") LIKE ?
                     OR lower(u."nomUni") LIKE ? OR lower(d."nomDep") LIKE ?
                     OR lower(h."nomHos") LIKE ?)
                    """);
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            for (int i = 0; i < 5; i++) {
                args.add(like);
            }
        }

        sql.append(" ORDER BY p.\"numHisCli\" DESC LIMIT ? OFFSET ? ");
        args.add(normalizarLimit(limit));
        args.add(Math.max(0, offset == null ? 0 : offset));

        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            Map<String, Object> row = new LinkedHashMap<>();
            LocalDate nacimiento = fecha(fila.get("nacimiento"));
            LocalDate admision = fecha(fila.get("admision"));

            row.put("id", texto(fila.get("id")));
            row.put("nombre", texto(fila.get("nombre")));
            row.put("nacimiento", fila.get("nacimiento"));
            row.put("admision", fila.get("admision"));
            row.put("direccion", texto(fila.get("direccion")));
            row.put("estado", texto(fila.get("estado")));
            row.put("estatus", estadoPaciente(fila));
            row.put("edad", nacimiento == null ? null : Period.between(nacimiento, LocalDate.now()).getYears());
            row.put("esNuevo", admision != null && !admision.isBefore(LocalDate.now().minusDays(30)));
            row.put("unidad", texto(fila.get("unidad")));
            row.put("departamento", texto(fila.get("departamento")));
            row.put("hospital", texto(fila.get("hospital")));
            row.put("medico", texto(fila.get("medico")));
            row.put("ultimaVisita", fila.get("ultima_visita"));
            rows.add(row);
        }

        return respuesta(perfil, rows, total(filas));
    }

    public Map<String, Object> turnos(String username, String role, String q, Integer limit, Integer offset) {
        Perfil perfil = resolverPerfil(username, role);

        StringBuilder sql = new StringBuilder("""
                SELECT t."numTur" AS id,
                       t."estTur" AS estado,
                       t."cantAtenTur" AS cant_aten,
                       m."codMed" AS cod_med,
                       m."nomMed" AS profesional,
                       m."espMed" AS especialidad,
                       u."codUni" AS cod_uni,
                       u."nomUni" AS unidad,
                       d."nomDep" AS departamento,
                       h."nomHos" AS hospital,
                       COALESCE(p."numHisCli", '') AS paciente_id,
                       COALESCE(p."nomPac", 'Sin paciente') AS paciente,
                       to_char(COALESCE(i."fecha", CURRENT_DATE), 'YYYY-MM-DD') AS fecha,
                       to_char(make_time(8 + ((t."numTur" % 20) / 2),
                                         (t."numTur" % 2) * 30, 0), 'HH24:MI') AS hora,
                       (SELECT count(*) FROM public."Consulta" cc WHERE cc."numTur" = t."numTur") AS consultas,
                       (SELECT count(*) FROM public."Consulta" ca
                         WHERE ca."numTur" = t."numTur" AND ca.atendido) AS atendidos,
                       COUNT(*) OVER () AS total
                FROM public."Turno" t
                JOIN public."Medico" m ON m."codMed" = t."codMed"
                JOIN public."Unidad" u ON u."codUni" = t."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                LEFT JOIN public."Informe" i ON i."numTur" = t."numTur"
                LEFT JOIN LATERAL (
                    SELECT c."numHisCli"
                    FROM public."Consulta" c
                    WHERE c."numTur" = t."numTur"
                    ORDER BY c."numHisCli"
                    LIMIT 1
                ) primero ON true
                LEFT JOIN public."Paciente" p ON p."numHisCli" = primero."numHisCli"
                """);

        List<Object> args = new ArrayList<>();
        boolean tieneWhere = false;

        if (!perfil.admin()) {
            sql.append(" WHERE t.\"codMed\" = ? ");
            args.add(perfil.codMed());
            tieneWhere = true;
        }

        if (q != null && !q.isBlank()) {
            sql.append(tieneWhere ? " AND " : " WHERE ");
            sql.append("""
                    (lower(p."nomPac") LIKE ? OR lower(p."numHisCli") LIKE ?
                     OR lower(m."nomMed") LIKE ? OR lower(m."espMed") LIKE ?
                     OR lower(u."nomUni") LIKE ? OR lower(h."nomHos") LIKE ?
                     OR lower(CAST(t."numTur" AS text)) LIKE ?)
                    """);
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            for (int i = 0; i < 7; i++) {
                args.add(like);
            }
        }

        sql.append(" ORDER BY t.\"numTur\" DESC LIMIT ? OFFSET ? ");
        args.add(normalizarLimit(limit));
        args.add(Math.max(0, offset == null ? 0 : offset));

        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            Map<String, Object> row = new LinkedHashMap<>();
            int numero = numero(fila.get("id"));
            row.put("id", "T-" + numero);
            row.put("numero", numero);
            row.put("paciente", texto(fila.get("paciente")));
            row.put("pacienteId", texto(fila.get("paciente_id")));
            row.put("profesional", texto(fila.get("profesional")));
            row.put("especialidad", texto(fila.get("especialidad")));
            row.put("estatus", estadoTurno(texto(fila.get("estado"))));
            row.put("estadoOriginal", texto(fila.get("estado")));
            row.put("hora", texto(fila.get("hora")));
            row.put("fecha", fila.get("fecha"));
            row.put("consultorio", "Consultorio " + ((numero % 4) + 1));
            row.put("unidad", texto(fila.get("unidad")));
            row.put("departamento", texto(fila.get("departamento")));
            row.put("hospital", texto(fila.get("hospital")));
            row.put("atendidos", numero(fila.get("atendidos")));
            row.put("cantidadAtendida", numero(fila.get("cant_aten")));
            rows.add(row);
        }

        return respuesta(perfil, rows, total(filas));
    }

    /** Resuelve el médico vinculado a la sesión y si el usuario es administrador. */
    private Perfil resolverPerfil(String username, String role) {
        boolean admin = "ADMIN".equalsIgnoreCase(role == null ? "" : role);
        if (admin || username == null || username.isBlank()) {
            return new Perfil(admin, null, null, null);
        }

        List<Map<String, Object>> filas = jdbcTemplate.queryForList("""
                SELECT m."codMed" AS cod_med, m."nomMed" AS nombre, m."codUni" AS cod_uni
                FROM public."Medico" m
                WHERE m.username = ?
                LIMIT 1
                """, username);

        if (filas.isEmpty()) {
            return new Perfil(false, null, null, null);
        }

        Map<String, Object> fila = filas.get(0);
        return new Perfil(false, texto(fila.get("cod_med")), texto(fila.get("nombre")), texto(fila.get("cod_uni")));
    }

    private Map<String, Object> respuesta(Perfil perfil, List<Map<String, Object>> rows, long total) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        data.put("alcance", perfil.admin() ? "global" : "medico");
        data.put("medico", perfil.nombre());
        data.put("codMed", perfil.codMed());
        return data;
    }

    private static long total(List<Map<String, Object>> filas) {
        if (filas.isEmpty()) return 0;
        Object valor = filas.get(0).get("total");
        if (valor instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(valor));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String estadoPaciente(Map<String, Object> fila) {
        String estado = texto(fila.get("estado")).toLowerCase(Locale.ROOT);
        if (estado.contains("alta")) return "inactive";
        if (Boolean.TRUE.equals(fila.get("pendiente")) || estado.contains("pendiente")) return "pending";
        return "active";
    }

    private static String estadoTurno(String estado) {
        String value = estado.toLowerCase(Locale.ROOT);
        if (value.contains("finaliz") || value.contains("atendid") || value.contains("done")) return "done";
        if (value.contains("confirm")) return "confirmed";
        return "pending";
    }

    private static int normalizarLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    private static int numero(Object valor) {
        if (valor == null) return 0;
        if (valor instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(valor));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static LocalDate fecha(Object valor) {
        if (valor instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (valor instanceof LocalDate localDate) return localDate;
        if (valor == null) return null;
        try {
            return LocalDate.parse(String.valueOf(valor).substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private record Perfil(boolean admin, String codMed, String nombre, String codUni) {
    }
}
