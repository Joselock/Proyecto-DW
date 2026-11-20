package com.hospital.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Notificaciones generadas por el motor de estados de los turnos.
 *
 * ADMIN ve las notificaciones de toda la red; MEDICO solamente las de su
 * ficha profesional. Los avisos se crean en PostgreSQL con
 * public.actualizar_turnos_en_vivo() y se consultan desde aquí.
 */
@Service
public class NotificacionService {

    private static final int MAX_LIMIT = 100;

    private final JdbcTemplate jdbc;

    public NotificacionService(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    /** Ejecuta el motor de estados. Devuelve cuántos turnos cambiaron. */
    public int sincronizar() {
        try {
            Integer cambios = jdbc.queryForObject(
                    "SELECT public.actualizar_turnos_en_vivo()", Integer.class);
            return cambios == null ? 0 : cambios;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    /** Listado de avisos visibles para la sesión actual. */
    public Map<String, Object> listar(String username, String role, Integer limite) {
        sincronizar();
        String codMed = codMedSesion(username, role);
        boolean admin = "ADMIN".equalsIgnoreCase(role == null ? "" : role);

        List<Object> args = new ArrayList<>();
        String filtro = "";
        if (!admin) {
            if (codMed == null) {
                Map<String, Object> vacio = new LinkedHashMap<>();
                vacio.put("rows", List.of());
                vacio.put("noLeidas", 0);
                vacio.put("ultimoId", 0);
                vacio.put("alcance", "medico");
                return vacio;
            }
            filtro = " WHERE n.\"codMed\" = ? ";
            args.add(codMed);
        }

        int tope = limite == null || limite <= 0 ? 30 : Math.min(limite, MAX_LIMIT);

        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT n."id" AS id, n."numTur" AS numero, n."tipo" AS tipo,
                       n."mensaje" AS mensaje, n."fecha" AS fecha, n."leida" AS leida,
                       to_char(t."fecha", 'YYYY-MM-DD') AS fecha_turno,
                       to_char(t."hora", 'HH24:MI') AS hora,
                       (SELECT COALESCE(p."nomPac", 'Sin paciente')
                          FROM public."Consulta" c
                          LEFT JOIN public."Paciente" p ON p."numHisCli" = c."numHisCli"
                          WHERE c."numTur" = n."numTur"
                          LIMIT 1) AS paciente
                FROM public."Notificacion" n
                JOIN public."Turno" t ON t."numTur" = n."numTur"
                """ + filtro + """
                ORDER BY n."fecha" DESC, n."id" DESC
                LIMIT ?
                """, concat(args, tope));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", numero(fila.get("id")));
            row.put("numero", numero(fila.get("numero")));
            row.put("tipo", texto(fila.get("tipo")));
            row.put("mensaje", texto(fila.get("mensaje")));
            row.put("fecha", String.valueOf(fila.get("fecha")));
            row.put("leida", Boolean.TRUE.equals(fila.get("leida")));
            row.put("fechaTurno", texto(fila.get("fecha_turno")));
            row.put("hora", texto(fila.get("hora")));
            row.put("paciente", texto(fila.get("paciente")));
            rows.add(row);
        }

        StringBuilder conteoSql = new StringBuilder("""
                SELECT count(*) FROM public."Notificacion" n WHERE 1 = 1
                """);
        List<Object> conteoArgs = new ArrayList<>();
        if (!admin) {
            conteoSql.append(" AND n.\"codMed\" = ? ");
            conteoArgs.add(codMed);
        }
        conteoSql.append(" AND n.\"leida\" = false ");
        Integer noLeidas = jdbc.queryForObject(conteoSql.toString(), Integer.class, conteoArgs.toArray());

        Integer ultimoId = jdbc.queryForObject("""
                SELECT COALESCE(max(n."id"), 0) FROM public."Notificacion" n
                """ + filtro, Integer.class, admin ? new Object[0] : new Object[] { codMed });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows);
        data.put("noLeidas", noLeidas == null ? 0 : noLeidas);
        data.put("ultimoId", ultimoId == null ? 0 : ultimoId);
        data.put("alcance", admin ? "global" : "medico");
        return data;
    }

    /** Marca como leídas las notificaciones indicadas (o todas si no hay ids). */
    public int marcarLeidas(String username, String role, List<Long> ids) {
        String codMed = codMedSesion(username, role);
        boolean admin = "ADMIN".equalsIgnoreCase(role == null ? "" : role);

        StringBuilder sql = new StringBuilder("""
                UPDATE public."Notificacion" SET "leida" = true WHERE "leida" = false
                """);
        List<Object> args = new ArrayList<>();
        if (!admin) {
            if (codMed == null) return 0;
            sql.append(" AND \"codMed\" = ? ");
            args.add(codMed);
        }
        if (ids != null && !ids.isEmpty()) {
            sql.append(" AND \"id\" IN (");
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append('?');
                args.add(ids.get(i));
            }
            sql.append(") ");
        }
        return jdbc.update(sql.toString(), args.toArray());
    }

    private String codMedSesion(String username, String role) {
        if ("ADMIN".equalsIgnoreCase(role == null ? "" : role)) return null;
        if (username == null || username.isBlank()) return null;
        List<String> filas = jdbc.queryForList("""
                SELECT m."codMed" FROM public."Medico" m WHERE m.username = ? LIMIT 1
                """, String.class, username);
        return filas.isEmpty() ? null : filas.get(0);
    }

    private static Object[] concat(List<Object> args, Object extra) {
        List<Object> copia = new ArrayList<>(args);
        copia.add(extra);
        return copia.toArray();
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
}
