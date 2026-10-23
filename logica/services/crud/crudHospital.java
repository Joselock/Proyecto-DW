package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mycompany.gestorui.model.entidades.Hospital;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudHospital {

    public static List<Hospital> obtenerHospitales() {
        List<Hospital> hospitales = new ArrayList<>();
       String function = "SELECT * FROM public.\"Hospital\" ORDER BY \"codHos\"";

        try (java.sql.Connection con = BaseDatos.getConnection();
                PreparedStatement ps = con.prepareStatement(function);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Hospital hospital = new Hospital();
                hospital.setCodigoHos(rs.getString("codHos"));
                hospital.setNombreHos(rs.getString("nomHos"));
               

                hospitales.add(hospital);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener hospitales: " + e.getMessage());
            e.printStackTrace();
        }

        return hospitales;
    }

    // Funcion para insertar un hospital
    public static Map<String, Object> insertarHospital(String codHos, String nomHos) {
        java.sql.Connection con = BaseDatos.getConnection();

        String function = "SELECT * FROM public.insertarHospital(?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, codHos);
            ps.setString(2, nomHos);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("existe", rs.getBoolean("existe"));
                    result.put("mensaje", rs.getString("mensaje"));
                    result.put("codHos", rs.getString("codHos"));
                    result.put("nomHos", rs.getString("nomHos"));
                    return result;
                }
            }

        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", "Error de JDBC: " + e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "No se obtuvo respuesta de la función");

    }

    // Funcion para modificar un hospital
    public static Map<String, Object> modificarHospital(String oldCod, String newCod, String nomHos) {
        java.sql.Connection con = BaseDatos.getConnection();

        String function = "SELECT * FROM public.modificarHospital(?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, oldCod);
            ps.setString(2, newCod);
            ps.setString(3, nomHos);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Map.of(
                            "existe", rs.getBoolean("existe"),
                            "mensaje", rs.getString("mensaje"),
                            "codHos", rs.getString("codHos"),
                            "nomHos", rs.getString("nomHos"));
                }
            }
        } catch (SQLException e) {
            return Map.of("existe", false, "mensaje", "Error: " + e.getMessage());
        }

        return Map.of("existe", false, "mensaje", "Sin resultado");
    }

    // Funcion para eliminar un hospital
    public static String eliminarHospital(String codHos) {
        java.sql.Connection con = BaseDatos.getConnection();

        String sql = "SELECT public.eliminarHospital(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codHos);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1); // la función devuelve TEXT
                }
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }

        return "Error: no se obtuvo respuesta";
    }

}
