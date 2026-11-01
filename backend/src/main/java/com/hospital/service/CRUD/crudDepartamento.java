package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Departamento;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudDepartamento {

    public static List<Departamento> obtenerDepartamentos() {
        List<Departamento> departamentos = new ArrayList<>();
        String sql = "SELECT * FROM public.\"Departamento\" ORDER BY \"codDep\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return departamentos;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Departamento dep = new Departamento(null, null, "");
                dep.setCodigoDep(rs.getString("codDep"));
                dep.setNombreDep(rs.getString("nomDep"));
                dep.setCodigoHos(rs.getString("codHos"));
                departamentos.add(dep);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener departamentos: " + e.getMessage());
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
        
        return departamentos;
    }

    //Funcion para insertar un departamento
    public static Map<String, Object> insertarDepartamento(String codDep, String nomDep, String codHos) {
        java.sql.Connection con = BaseDatos.getConnection();
        
        String function = "SELECT * FROM public.insertarDepartamento(?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, codDep);
            ps.setString(2, nomDep);
            ps.setString(3, codHos);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                        "existe", rs.getBoolean("existe"),
                        "mensaje", rs.getString("mensaje"),
                        "codDep", rs.getString("codDep"),
                        "nomDep", rs.getString("nomDep"),
                        "codHos", rs.getString("codHos")
                    );
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    
    //Funcion para modificar un departamento
    public static Map<String, Object> modificarDepartamento(String oldCod, String newCod, String nomDep, String codHos) {
        java.sql.Connection con = BaseDatos.getConnection();

        String function = "SELECT * FROM public.modificarDepartamento(?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, oldCod);
            ps.setString(2, newCod);
            ps.setString(3, nomDep);
            ps.setString(4, codHos);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                        "existe", rs.getBoolean("existe"),
                        "mensaje", rs.getString("mensaje"),
                        "codDep", rs.getString("codDep"),
                        "nomDep", rs.getString("nomDep"),
                        "codHos", rs.getString("codHos")
                    );
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "Sin resultado");
    }


    //Funcion para eliminar un departamento
    public static String eliminarDepartamento(String codDep) {
        java.sql.Connection con = BaseDatos.getConnection();

        String function = "SELECT public.eliminarDepartamento(?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, codDep);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
        
        return "Error: sin respuesta";
    }

}
