package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Medico;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudMedico {

    public static List<Medico> obtenerMedicos() {
        List<Medico> medicos = new ArrayList<>();
        String sql = "SELECT * FROM public.\"Medico\" ORDER BY \"codMed\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return medicos;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Medico medico = new Medico(null, null, null, null, null, 0, null, null);
                medico.setCodigoMed(rs.getString("codMed"));
                medico.setNombreMed(rs.getString("nomMed"));
                medico.setEspecialidad(rs.getString("espMed"));
                medico.setNumeroLic(rs.getString("numLic"));
                medico.setTelefono(rs.getString("telMed"));
                medico.setExperiencia(rs.getInt("exp"));
                medico.setDatosC(rs.getString("datCon"));
                medico.setCodigoUni(rs.getString("codUni"));
                medicos.add(medico);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener médicos: " + e.getMessage());
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
        
        return medicos;
    }

    public static Map<String, Object> insertarMedico(String codMed, String nomMed, String especialidad,
            String licencia, String telefono, int experiencia,
            String datosCon, String codUni) {

        java.sql.Connection con = BaseDatos.getConnection();        
        String function = "SELECT * FROM public.insertarMedico(?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, codMed);
            ps.setString(2, nomMed);
            ps.setString(3, especialidad);
            ps.setString(4, licencia);
            ps.setString(5, telefono);
            ps.setInt(6, experiencia);
            ps.setString(7, datosCon);
            ps.setString(8, codUni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "codMed", rs.getString("codMed"),
                            "nomMed", rs.getString("nomMed"),
                            "espMed", rs.getString("espMed"),
                            "numLic", rs.getString("numLic"),
                            "telefono", rs.getString("telMed"),
                            "experiencia", rs.getInt("exp"),
                            "datosCon", rs.getString("datCon"),
                            "codUni", rs.getString("codUni"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static Map<String, Object> modificarMedico(String oldCod, String newCod, String nomMed, String espMed,
            String numLic, String telefono, int experiencia,
            String datos, String codUni) {

        java.sql.Connection con = BaseDatos.getConnection();          
        String function  = "SELECT * FROM public.modificarMedico(?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function )) {
            ps.setString(1, oldCod);
            ps.setString(2, newCod);
            ps.setString(3, nomMed);
            ps.setString(4, espMed);
            ps.setString(5, numLic);
            ps.setString(6, telefono);
            ps.setInt(7, experiencia);
            ps.setString(8, datos);
            ps.setString(9, codUni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "codMed", rs.getString("codMed"),
                            "nomMed", rs.getString("nomMed"),
                            "espMed", rs.getString("espMed"),
                            "numLic", rs.getString("numLic"),
                            "telefono", rs.getString("telMed`"),
                            "experiencia", rs.getInt("exp"),
                            "datos", rs.getString("datCon"),
                            "codUni", rs.getString("codUni"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    public static String eliminarMedico(String codMed) {

        java.sql.Connection con = BaseDatos.getConnection();  
        String function  = "SELECT public.eliminarMedico(?)";

        try (PreparedStatement ps = con.prepareStatement(function )) {
            ps.setString(1, codMed);
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
