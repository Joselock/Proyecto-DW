package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Consulta;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudConsulta {

    
    public static List<Consulta> obtenerConsultas() {
        List<Consulta> consultas = new ArrayList<>();
        String sql = "SELECT * FROM public.\"Consulta\" ORDER BY \"numTur\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return consultas;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Consulta consulta = new Consulta(null, 0, false, null, null);
                consulta.setNumeroT(rs.getInt("numTur"));
                consulta.setNumH(rs.getString("numHisCli"));
                consulta.setAtend(rs.getBoolean("atendido"));
                consulta.setCausa(rs.getString("causa"));
                consultas.add(consulta);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener consultas: " + e.getMessage());
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
        
        return consultas;
    }

    public static Map<String, Object> insertarConsulta(int numTur, String numHistoria, boolean atendido, String causa) {

        java.sql.Connection con = BaseDatos.getConnection();  
        String function = "SELECT * FROM public.insertarConsulta(?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setInt(1, numTur);
            ps.setString(2, numHistoria);
            ps.setBoolean(3, atendido);
            ps.setString(4, causa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numTur", rs.getInt("numTur"),
                            "numHistoria", rs.getString("numHistoria"),
                            "atendido", rs.getBoolean("atendido"),
                            "causa", rs.getString("causa"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }
        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static Map<String, Object> modificarConsulta(int numActual, int numNuevo, String numHistoria, boolean atendido,
            String causa) {

        java.sql.Connection con = BaseDatos.getConnection();          
        String function = "SELECT * FROM public.modificarConsulta(?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setInt(1, numActual);
            ps.setInt(2, numNuevo);
            ps.setString(3, numHistoria);
            ps.setBoolean(4, atendido);
            ps.setString(5, causa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numTur", rs.getInt("numTur"),
                            "numHistoria", rs.getString("numHistoria"),
                            "atendido", rs.getBoolean("atendido"),
                            "causa", rs.getString("causa"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }
        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static Map<String,Object> eliminarConsulta(int numTurno,String numHistoria){

        java.sql.Connection con = BaseDatos.getConnection();          
        String function = "SELECT * FROM public.eliminarConsulta(?,?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setInt(1, numTurno);
            ps.setString(2, numHistoria);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numTur", rs.getInt("numTur"),
                            "numHistoria", rs.getString("numHistoria"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

}
