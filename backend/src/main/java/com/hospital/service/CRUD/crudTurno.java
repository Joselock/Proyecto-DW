package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Medico;
import com.mycompany.gestorui.model.entidades.Turno;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudTurno {

    public static List<Turno> obtenerTurnos() {
        List<Turno> turnos = new ArrayList<>();
        String sql = "SELECT * FROM public.\"Turno\" ORDER BY \"numTur\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return turnos;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            // Obtener todos los médicos para referencia
            List<Medico> medicos = crudMedico.obtenerMedicos();
            
            while (rs.next()) {
                Turno turno = new Turno(0, 0, null, null, null, null);
                turno.setNumTurn(rs.getInt("numTur"));
                turno.setCantAten(rs.getInt("cantAtenTur"));
                turno.setEstTur(rs.getString("estTur"));
                turno.setCodUni(rs.getString("codUni"));
                
                // Buscar el médico por código
                String codMed = rs.getString("codMed");
                if (codMed != null) {
                    Medico medico = medicos.stream()
                        .filter(m -> m.getCodigoMed().equals(codMed))
                        .findFirst()
                        .orElse(null);
                    turno.setMedico(medico);
                }
                
                turnos.add(turno);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener turnos: " + e.getMessage());
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
        
        return turnos;
    }

    public static Map<String, Object> insertarTurno(int numTur, int cantAtendidos, String estadoTur, String codUni,
            String codMed) {
        
        java.sql.Connection con = BaseDatos.getConnection();          
        String function = "SELECT * FROM public.insertarTurno(?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setInt(1, numTur);
            ps.setInt(2, cantAtendidos);
            ps.setString(3, estadoTur);
            ps.setString(4, codUni);
            ps.setString(5, codMed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numTur", rs.getInt("numTur"),
                            "cantAtendidos", rs.getInt("cantAtenTur"),
                            "estadoTur", rs.getString("estTur"),
                            "codUni", rs.getString("codUni"),
                            "codMed", rs.getString("codMed"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }
        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static Map<String, Object> modificarTurno(int numActual, int numNuevo, int cantAtendidos, String estadoTur,
            String codUni, String codMed) {

        java.sql.Connection con = BaseDatos.getConnection();          
        String function = "SELECT * FROM public.modificarTurno(?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setInt(1, numActual);
            ps.setInt(2, numNuevo);
            ps.setInt(3, cantAtendidos);
            ps.setString(4, estadoTur);
            ps.setString(5, codUni);
            ps.setString(6, codMed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "numTur", rs.getInt("numTur"),
                            "cantAtendidos", rs.getInt("cantAtenTur"),
                            "estadoTur", rs.getString("estTur"),
                            "codUni", rs.getString("codUni"),
                            "codMed", rs.getString("codMed"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }
        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static String eliminarTurno(int numTur) {

        java.sql.Connection con = BaseDatos.getConnection();  
        String function = "SELECT public.eliminarTurno(?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setInt(1, numTur);
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
