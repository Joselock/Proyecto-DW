package com.mycompany.gestorui.model.services.crud;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Paciente;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudPaciente {

    public static List<Paciente> obtenerPacientes() {
        List<Paciente> pacientes = new ArrayList<>();
        // JOIN para obtener estPac desde la tabla Registro
        String sql = "SELECT p.\"numHisCli\", p.\"nomPac\", p.\"dirPac\", p.\"fechaN\", r.\"codUni\", r.\"estPac\" " +
                     "FROM public.\"Paciente\" p " +
                     "LEFT JOIN public.\"Registro\" r ON p.\"numHisCli\" = r.\"numHisCli\" " +
                     "ORDER BY p.\"numHisCli\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return pacientes;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Paciente paciente = new Paciente();
                paciente.setNumHisCli(rs.getString("numHisCLi"));
                paciente.setNombrePac(rs.getString("nomPac"));
                paciente.setDireccionP(rs.getString("dirPac"));
                paciente.setFechaN(rs.getDate("fechaN"));
                paciente.setEstado(rs.getString("estPac")); 
                paciente.setCodigoUni(rs.getString("codUni"));
                pacientes.add(paciente);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener pacientes: " + e.getMessage());
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
        
        return pacientes;
    }

    public static Map<String, Object> insertarPaciente(String numHistoria, String nomPac, String direccion,
            LocalDate fechaNac, String estadoPac, String codUni) {

        java.sql.Connection con = BaseDatos.getConnection();        
        String function = "SELECT * FROM public.insertarPaciente(?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, numHistoria);
            ps.setString(2, nomPac);
            ps.setString(3, direccion);
            ps.setDate(4, Date.valueOf(fechaNac));
            ps.setString(5, estadoPac);
            ps.setString(6, codUni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numHistoria", rs.getString("numHisCli"),
                            "nomPac", rs.getString("nomPac"),
                            "direccion", rs.getString("dirPac"),
                            "fecha", rs.getDate("fecha"),
                            "estadoPac", rs.getString("estPac"),
                            "codUni", rs.getString("codUni"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }
        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static Map<String, Object> modificarPaciente(String oldHistoria, String newHistoria, String estPac,
            String nomPac, String direccion, LocalDate fechaN, String codUni) {
        
        java.sql.Connection con = BaseDatos.getConnection();          
        String function = "SELECT * FROM public.modificarPaciente(?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, oldHistoria);
            ps.setString(2, newHistoria);
            ps.setString(3, estPac);
            ps.setString(4, nomPac);
            ps.setString(5, direccion);
            ps.setDate(6, Date.valueOf(fechaN));
            ps.setString(7, codUni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numHistoria", rs.getString("numHisCli"),
                            "estPac", rs.getString("estPac"),
                            "nomPac", rs.getString("nomPac"),
                            "direccion", rs.getString("dirPac"),
                            "fechaN", rs.getDate("fechaN"),
                            "codUni", rs.getString("codUni"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }
        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static String eliminarPaciente(String numHistoria) {

        java.sql.Connection con = BaseDatos.getConnection();  
        String function = "SELECT public.eliminarPaciente(?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, numHistoria);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getString(1);
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
        return "Error: sin respuesta";
    }

}
