package com.hospital.service.REPORTES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resuelve el contexto hospitalario de la sesión actual.
 *
 * Un usuario se vincula con un hospital a través de la relación 1:1
 * Medico.username -> user.username. Si la cuenta no tiene un médico
 * asociado, el contexto queda en modo global para que un administrador
 * pueda consultar toda la red hospitalaria.
 */
@org.springframework.stereotype.Service
public class ReportesContextoService extends com.hospital.service.JdbcServiceSupport {

    public ReportesContextoService(javax.sql.DataSource dataSource) {
        super(dataSource);
    }

    public Map<String, Object> contextoUsuario(String username) {
        Map<String, Object> contexto = new LinkedHashMap<>();
        contexto.put("hospital", hospitalDeUsuario(username));
        contexto.put("hospitales", listarHospitales());
        contexto.put("departamentos", listarDepartamentos());
        contexto.put("unidades", listarUnidades());
        return contexto;
    }

    public Map<String, Object> profesionalesDelUsuario(String username) {
        Map<String, Object> hospital = hospitalDeUsuario(username);
        String codHos = hospital == null ? null : (String) hospital.get("codigo");

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("hospital", hospital);
        resultado.put("alcance", codHos == null ? "global" : "hospital");
        resultado.put("medicos", listarMedicos(codHos));
        return resultado;
    }

    private Map<String, Object> hospitalDeUsuario(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String sql = """
                SELECT h."codHos" AS codigo, h."nomHos" AS nombre
                FROM public."Medico" m
                INNER JOIN public."Unidad" u ON m."codUni" = u."codUni"
                INNER JOIN public."Departamento" d ON u."codDep" = d."codDep"
                INNER JOIN public."Hospital" h ON d."codHos" = h."codHos"
                WHERE m.username = ?
                LIMIT 1
                """;

        try (java.sql.Connection con = getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs, "codigo", "nombre");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo resolver el hospital del usuario.", ex);
        }

        return null;
    }

    private List<Map<String, Object>> listarHospitales() {
        String sql = """
                SELECT "codHos" AS codigo, "nomHos" AS nombre
                FROM public."Hospital"
                ORDER BY "nomHos"
                """;
        return consultar(sql, null, "codigo", "nombre");
    }

    private List<Map<String, Object>> listarDepartamentos() {
        String sql = """
                SELECT "codDep" AS codigo, "nomDep" AS nombre, "codHos" AS codHos
                FROM public."Departamento"
                ORDER BY "nomDep"
                """;
        return consultar(sql, null, "codigo", "nombre", "codHos");
    }

    private List<Map<String, Object>> listarUnidades() {
        String sql = """
                SELECT "codUni" AS codigo, "nomUni" AS nombre, "codDep" AS codDep
                FROM public."Unidad"
                ORDER BY "nomUni"
                """;
        return consultar(sql, null, "codigo", "nombre", "codDep");
    }

    private List<Map<String, Object>> listarMedicos(String codHos) {
        StringBuilder sql = new StringBuilder("""
                SELECT m."codMed" AS codMed,
                       m."nomMed" AS nombre,
                       m."espMed" AS especialidad,
                       m."numLic" AS licencia,
                       m."telMed" AS telefono,
                       m."exp" AS experiencia,
                       m."datCon" AS datosContacto,
                       COALESCE(m."dirMed", '') AS direccion,
                       m.username AS username,
                       us.email AS email,
                       u."codUni" AS codUni,
                       u."nomUni" AS unidad,
                       d."codDep" AS codDep,
                       d."nomDep" AS departamento,
                       h."codHos" AS codHos,
                       h."nomHos" AS hospital
                FROM public."Medico" m
                INNER JOIN public."Unidad" u ON m."codUni" = u."codUni"
                INNER JOIN public."Departamento" d ON u."codDep" = d."codDep"
                INNER JOIN public."Hospital" h ON d."codHos" = h."codHos"
                LEFT JOIN public."user" us ON us.username = m.username
                """);

        if (codHos != null) {
            sql.append(" WHERE h.\"codHos\" = ? ");
        }
        sql.append(" ORDER BY h.\"nomHos\", d.\"nomDep\", u.\"nomUni\", m.\"nomMed\" ");

        List<Map<String, Object>> medicos = new ArrayList<>();

        try (java.sql.Connection con = getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (codHos != null) {
                ps.setString(1, codHos);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> medico = new LinkedHashMap<>();
                    medico.put("codMed", rs.getString("codMed"));
                    medico.put("nombre", rs.getString("nombre"));
                    medico.put("especialidad", rs.getString("especialidad"));
                    medico.put("licencia", rs.getString("licencia"));
                    medico.put("telefono", rs.getString("telefono"));
                    medico.put("experiencia", rs.getInt("experiencia"));
                    medico.put("datosContacto", rs.getString("datosContacto"));
                    medico.put("direccion", rs.getString("direccion"));
                    medico.put("username", rs.getString("username"));
                    medico.put("email", rs.getString("email"));
                    medico.put("codUni", rs.getString("codUni"));
                    medico.put("unidad", rs.getString("unidad"));
                    medico.put("codDep", rs.getString("codDep"));
                    medico.put("departamento", rs.getString("departamento"));
                    medico.put("codHos", rs.getString("codHos"));
                    medico.put("hospital", rs.getString("hospital"));
                    medicos.add(medico);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el listado de profesionales.", ex);
        }

        return medicos;
    }

    private List<Map<String, Object>> consultar(String sql, String parametro, String... columnas) {
        List<Map<String, Object>> filas = new ArrayList<>();

        try (java.sql.Connection con = getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setString(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(mapRow(rs, columnas));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo consultar el contexto hospitalario.", ex);
        }

        return filas;
    }

    private Map<String, Object> mapRow(ResultSet rs, String... columnas) throws SQLException {
        Map<String, Object> fila = new LinkedHashMap<>();
        for (String columna : columnas) {
            fila.put(columna, rs.getString(columna));
        }
        return fila;
    }
}
