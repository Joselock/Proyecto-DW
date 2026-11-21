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

import com.hospital.dto.response.PacientePanelRow;
import com.hospital.dto.response.PanelPacientesResponse;
import com.hospital.dto.response.PanelTurnosResponse;
import com.hospital.dto.response.TurnoPanelRow;

/**
 * Datos que alimentan las secciones "Pacientes" y "Turnos" del dashboard.
 *
 * Regla de visibilidad:
 *   - ADMIN: ve todos los pacientes y turnos de todos los hospitales.
 *   - MEDICO: ve solo los turnos asignados a su codMed y los pacientes que
 *     atendió en esos turnos o que están ingresados en su unidad.
 *
 * El parámetro fecha permite recorrer la agenda hacia atrás y hacia adelante.
 * Cuando se recibe, los turnos se filtran por ese día y el listado de
 * pacientes se reduce a quienes tienen un turno ese mismo día. Sin fecha se
 * mantiene el comportamiento original (todo el histórico).
 *
 * Antes de cada consulta se ejecuta el motor de estados de PostgreSQL, de modo
 * que un turno cuya hora ya llegó aparece como "en_progreso" (o "finalizado")
 * en cuanto se abre el panel, aunque el scheduler todavía no haya corrido.
 */
@Service
public class PanelService {

    private static final int DEFAULT_LIMIT = 5000;
    private static final int MAX_LIMIT = 20000;

    private final JdbcTemplate jdbcTemplate;

    public PanelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Aplica las transiciones pendiente/confirmado -> en_progreso -> finalizado
     * según la hora del servidor. Fallar aquí no debe impedir ver el panel.
     */
    private void sincronizarTurnos() {
        try {
            jdbcTemplate.queryForObject("SELECT public.actualizar_turnos_en_vivo()", Integer.class);
        } catch (RuntimeException exception) {
            // El panel sigue funcionando con el último estado calculado.
        }
    }

    public PanelPacientesResponse pacientes(String username, String role, String q, String fecha,
            Integer limit, Integer offset) {
        sincronizarTurnos();
        Perfil perfil = resolverPerfil(username, role);

        StringBuilder sql = new StringBuilder("""
                SELECT p."numHisCli" AS id,
                       p."nomPac" AS nombre,
                       to_char(p."fechaN", 'YYYY-MM-DD') AS nacimiento,
                       to_char(r."fecha_admision", 'YYYY-MM-DD') AS admision,
                       COALESCE(p."dirPac", '') AS direccion,
                       r."estPac" AS estado,
                       r."codUni" AS cod_uni,
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
                             AND lower(tp."estTur") IN ('pendiente', 'confirmado', 'en_progreso')
                       ) AS pendiente,
                       COUNT(*) OVER () AS total
                FROM public."Registro" r
                JOIN public."Paciente" p ON p."numHisCli" = r."numHisCli"
                JOIN public."Unidad" u ON u."codUni" = r."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                LEFT JOIN LATERAL (
                    SELECT m."nomMed" AS medico, t."fecha" AS fecha
                    FROM public."Consulta" c
                    JOIN public."Turno" t ON t."numTur" = c."numTur"
                    LEFT JOIN public."Medico" m ON m."codMed" = t."codMed"
                    WHERE c."numHisCli" = p."numHisCli"
                    ORDER BY t."fecha" DESC, t."hora" DESC, t."numTur" DESC
                    LIMIT 1
                ) ult ON true
                """);

        List<String> condiciones = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (!perfil.admin()) {
            condiciones.add("""
                    (r."codUni" = ?
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
        }

        String dia = fechaValida(fecha);
        if (dia != null) {
            if (perfil.admin()) {
                condiciones.add("""
                        EXISTS (
                            SELECT 1
                            FROM public."Consulta" cd
                            JOIN public."Turno" td ON td."numTur" = cd."numTur"
                            WHERE cd."numHisCli" = p."numHisCli"
                              AND td."fecha" = ?::date
                        )
                        """);
                args.add(dia);
            } else {
                condiciones.add("""
                        EXISTS (
                            SELECT 1
                            FROM public."Consulta" cd
                            JOIN public."Turno" td ON td."numTur" = cd."numTur"
                            WHERE cd."numHisCli" = p."numHisCli"
                              AND td."fecha" = ?::date
                              AND td."codMed" = ?
                        )
                        """);
                args.add(dia);
                args.add(perfil.codMed());
            }
        }

        if (q != null && !q.isBlank()) {
            condiciones.add("""
                    (lower(p."nomPac") LIKE ? OR lower(p."numHisCli") LIKE ?
                     OR lower(u."nomUni") LIKE ? OR lower(d."nomDep") LIKE ?
                     OR lower(h."nomHos") LIKE ?)
                    """);
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            for (int i = 0; i < 5; i++) {
                args.add(like);
            }
        }

        if (!condiciones.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", condiciones));
        }
        sql.append(" ORDER BY p.\"numHisCli\" DESC LIMIT ? OFFSET ? ");
        args.add(normalizarLimit(limit));
        args.add(Math.max(0, offset == null ? 0 : offset));

        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        List<PacientePanelRow> rows = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            LocalDate nacimiento = fecha(fila.get("nacimiento"));
            LocalDate admision = fecha(fila.get("admision"));

            rows.add(new PacientePanelRow(
                    texto(fila.get("id")),
                    texto(fila.get("nombre")),
                    texto(fila.get("nacimiento")),
                    texto(fila.get("admision")),
                    texto(fila.get("direccion")),
                    texto(fila.get("estado")),
                    estadoPaciente(fila),
                    nacimiento == null ? null : Period.between(nacimiento, LocalDate.now()).getYears(),
                    admision != null && !admision.isBefore(LocalDate.now().minusDays(30)),
                    texto(fila.get("cod_uni")),
                    texto(fila.get("unidad")),
                    texto(fila.get("departamento")),
                    texto(fila.get("hospital")),
                    texto(fila.get("medico")),
                    texto(fila.get("ultima_visita"))
            ));
        }

        return new PanelPacientesResponse(
                rows,
                total(filas),
                perfil.admin() ? "global" : "medico",
                perfil.nombre(),
                perfil.codMed(),
                dia
        );
    }

    public PanelTurnosResponse turnos(String username, String role, String q, String fecha,
            Integer limit, Integer offset) {
        sincronizarTurnos();
        Perfil perfil = resolverPerfil(username, role);

        // La ventana de cada consulta termina cuando empieza el siguiente turno
        // del mismo médico y día; si es el último, se estiman 30 minutos. El
        // mínimo es 1 minuto para que el turno nunca termine antes de empezar.
        StringBuilder sql = new StringBuilder("""
                WITH agenda AS (
                    SELECT t.*,
                           GREATEST(
                               COALESCE(
                                   LEAD(t."fecha" + t."hora") OVER (
                                       PARTITION BY t."codMed", t."fecha"
                                       ORDER BY t."hora", t."numTur"
                                   ),
                                   t."fecha" + t."hora" + interval '30 minutes'
                               ),
                               t."fecha" + t."hora" + interval '1 minute'
                           ) AS fin_calc
                    FROM public."Turno" t
                )
                SELECT a."numTur" AS id,
                       a."codMed" AS cod_med,
                       a."estTur" AS estado,
                       a."cantAtenTur" AS cant_aten,
                       a."nota_clinica" AS nota_clinica,
                       to_char(a."fecha", 'YYYY-MM-DD') AS fecha,
                       to_char(a."hora", 'HH24:MI') AS hora,
                       to_char(a.fin_calc, 'HH24:MI') AS fin,
                       floor(EXTRACT(EPOCH FROM (a.fin_calc - (a."fecha" + a."hora"))) / 60)::int AS duracion,
                       m."nomMed" AS profesional,
                       m."espMed" AS especialidad,
                       u."codUni" AS cod_uni,
                       u."nomUni" AS unidad,
                       d."nomDep" AS departamento,
                       h."nomHos" AS hospital,
                       COALESCE(p."numHisCli", '') AS paciente_id,
                       COALESCE(p."nomPac", 'Sin paciente') AS paciente,
                       (SELECT count(*) FROM public."Consulta" cc WHERE cc."numTur" = a."numTur") AS consultas,
                       (SELECT count(*) FROM public."Consulta" ca
                         WHERE ca."numTur" = a."numTur" AND ca.atendido) AS atendidos,
                       COUNT(*) OVER () AS total
                FROM agenda a
                JOIN public."Medico" m ON m."codMed" = a."codMed"
                JOIN public."Unidad" u ON u."codUni" = a."codUni"
                JOIN public."Departamento" d ON d."codDep" = u."codDep"
                JOIN public."Hospital" h ON h."codHos" = d."codHos"
                LEFT JOIN LATERAL (
                    SELECT c."numHisCli"
                    FROM public."Consulta" c
                    WHERE c."numTur" = a."numTur"
                    ORDER BY c."numHisCli"
                    LIMIT 1
                ) primero ON true
                LEFT JOIN public."Paciente" p ON p."numHisCli" = primero."numHisCli"
                """);

        List<String> condiciones = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (!perfil.admin()) {
            condiciones.add(" a.\"codMed\" = ? ");
            args.add(perfil.codMed());
        }

        String dia = fechaValida(fecha);
        if (dia != null) {
            condiciones.add(" a.\"fecha\" = ?::date ");
            args.add(dia);
        }

        if (q != null && !q.isBlank()) {
            condiciones.add("""
                    (lower(p."nomPac") LIKE ? OR lower(p."numHisCli") LIKE ?
                     OR lower(m."nomMed") LIKE ? OR lower(m."espMed") LIKE ?
                     OR lower(u."nomUni") LIKE ? OR lower(h."nomHos") LIKE ?
                     OR lower(CAST(a."numTur" AS text)) LIKE ?)
                    """);
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            for (int i = 0; i < 7; i++) {
                args.add(like);
            }
        }

        if (!condiciones.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", condiciones));
        }
        sql.append(" ORDER BY a.\"fecha\" DESC, a.\"hora\" DESC, a.\"numTur\" DESC LIMIT ? OFFSET ? ");
        args.add(normalizarLimit(limit));
        args.add(Math.max(0, offset == null ? 0 : offset));

        List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        List<TurnoPanelRow> rows = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            int numero = numero(fila.get("id"));
            rows.add(new TurnoPanelRow(
                    "T-" + numero,
                    numero,
                    texto(fila.get("cod_med")),
                    texto(fila.get("paciente")),
                    texto(fila.get("paciente_id")),
                    texto(fila.get("profesional")),
                    texto(fila.get("especialidad")),
                    estadoTurno(texto(fila.get("estado"))),
                    texto(fila.get("estado")),
                    texto(fila.get("hora")),
                    texto(fila.get("fin")),
                    numero(fila.get("duracion")),
                    texto(fila.get("nota_clinica")),
                    texto(fila.get("fecha")),
                    "Consultorio " + ((numero % 4) + 1),
                    texto(fila.get("unidad")),
                    texto(fila.get("departamento")),
                    texto(fila.get("hospital")),
                    numero(fila.get("atendidos")),
                    numero(fila.get("consultas")),
                    numero(fila.get("cant_aten"))
            ));
        }

        return new PanelTurnosResponse(
                rows,
                total(filas),
                perfil.admin() ? "global" : "medico",
                perfil.nombre(),
                perfil.codMed(),
                dia
        );
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
        String value = estado.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
        if (value.contains("no atendido")) return "no_atendido";
        if (value.contains("progreso") || value.contains("curso")) return "in_progress";
        if (value.contains("finaliz") || value.contains("atendid") || value.contains("done")) return "done";
        if (value.contains("confirm")) return "confirmed";
        return "pending";
    }

    private static String fechaValida(String fecha) {
        if (fecha == null || !fecha.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        try {
            return LocalDate.parse(fecha).toString();
        } catch (RuntimeException e) {
            return null;
        }
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
