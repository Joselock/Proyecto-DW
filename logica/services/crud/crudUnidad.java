package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Unidad;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudUnidad {

    public static List<Unidad> obtenerUnidades() {
        List<Unidad> unidades = new ArrayList<>();
        String sql = "SELECT * FROM public.\"Unidad\" ORDER BY \"codUni\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return unidades;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Unidad unidad = new Unidad(
                    rs.getString("codUni"),
                    rs.getString("nomUni"),
                    rs.getString("ubiUni"),
                    rs.getString("codDep")
                );
                unidades.add(unidad);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener unidades: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return unidades;
    }

    // Función para insertar una unidad
    public static Map<String, Object> insertarUnidad(String codUni, String nomUni, String ubicacion, String codDep) {
        Map<String, Object> resultado = new HashMap<>();
        java.sql.Connection con = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                resultado.put("existe", false);
                resultado.put("mensaje", "Error de conexión a la base de datos");
                return resultado;
            }

            String function = "SELECT * FROM public.insertarUnidad(?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(function)) {
                ps.setString(1, codUni);
                ps.setString(2, nomUni);
                ps.setString(3, ubicacion);
                // Manejar codDep que puede ser null
                if (codDep != null && !codDep.isEmpty()) {
                    ps.setString(4, codDep);
                } else {
                    ps.setNull(4, java.sql.Types.VARCHAR);
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resultado.put("existe", rs.getBoolean("existe"));
                        resultado.put("mensaje", rs.getString("mensaje"));
                        resultado.put("codUni", rs.getString("codUni"));
                        resultado.put("nomUni", rs.getString("nomUni"));
                        resultado.put("ubiUni", rs.getString("ubiUni"));
                        resultado.put("codDep", rs.getString("codDep"));
                    } else {
                        resultado.put("existe", false);
                        resultado.put("mensaje", "No se obtuvo respuesta de la base de datos");
                    }
                }
            }
        } catch (SQLException e) {
            resultado.put("existe", false);
            resultado.put("mensaje", "Error SQL: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultado;
    }

    // Función para modificar una unidad
    public static Map<String, Object> modificarUnidad(String oldCod, String newCod, String nomUni, String ubicacion,
            String codDep) {
        Map<String, Object> resultado = new HashMap<>();
        java.sql.Connection con = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                resultado.put("existe", false);
                resultado.put("mensaje", "Error de conexión a la base de datos");
                return resultado;
            }

            String function = "SELECT * FROM public.modificarUnidad(?, ?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(function)) {
                ps.setString(1, oldCod);
                ps.setString(2, newCod);
                ps.setString(3, nomUni);
                ps.setString(4, ubicacion);
                // Manejar codDep que puede ser null
                if (codDep != null && !codDep.isEmpty()) {
                    ps.setString(5, codDep);
                } else {
                    ps.setNull(5, java.sql.Types.VARCHAR);
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resultado.put("existe", rs.getBoolean("existe"));
                        resultado.put("mensaje", rs.getString("mensaje"));
                        resultado.put("codUni", rs.getString("codUni"));
                        resultado.put("nomUni", rs.getString("nomUni"));
                        resultado.put("ubiUni", rs.getString("ubiUni"));
                        resultado.put("codDep", rs.getString("codDep"));
                    } else {
                        resultado.put("existe", false);
                        resultado.put("mensaje", "No se obtuvo respuesta de la base de datos");
                    }
                }
            }
        } catch (SQLException e) {
            resultado.put("existe", false);
            resultado.put("mensaje", "Error SQL: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultado;
    }

    // Función para eliminar una unidad
    public static String eliminarUnidad(String codUni) {
        java.sql.Connection con = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                return "Error: No se pudo conectar a la base de datos";
            }

            String function = "SELECT public.eliminarUnidad(?)";

            try (PreparedStatement ps = con.prepareStatement(function)) {
                ps.setString(1, codUni);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return "Error: sin respuesta";
    }
}