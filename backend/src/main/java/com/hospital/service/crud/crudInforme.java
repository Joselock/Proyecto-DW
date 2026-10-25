package com.mycompany.gestorui.model.services.crud;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mycompany.gestorui.model.entidades.Informe;
import com.mycompany.gestorui.model.utils.BaseDatos;

public class crudInforme {

     public static List<Informe> obtenerInformes() {
        List<Informe> informes = new ArrayList<>();
        String sql = "SELECT * FROM public.\"Informe\" ORDER BY \"numIn\"";
        
        java.sql.Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = BaseDatos.getConnection();
            if (con == null) {
                System.err.println("Error: No se pudo conectar a la base de datos");
                return informes;
            }
            
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Informe informe = new Informe(null, null, sql, 0, 0, 0, 0, 0, 0, 0, null);
                informe.setNumIn(rs.getString("numIn"));
                informe.setHora(rs.getTime("hora"));
                informe.setFecha(rs.getDate("fecha"));
                informe.setPacAtend(rs.getInt("pacAten"));
                informe.setPacAlta(rs.getInt("pacAlt"));
                informe.setCantAdm(rs.getInt("cantAdm"));
                informe.setTotal(rs.getInt("total"));
                informe.setCodigoUni(rs.getString("codUni"));
                informe.setNumeroTurno(rs.getInt("numTur"));
        
                informes.add(informe);
            }
            
        } catch (SQLException e) {
            System.err.println("Error al obtener informes: " + e.getMessage());
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
        
        return informes;
    }

     public static String eliminarInforme(String numIn) {

        java.sql.Connection con = BaseDatos.getConnection();
        String function = "SELECT public.eliminarInforme(?)";

        try (PreparedStatement ps = con.prepareStatement(function)) {
            ps.setString(1, numIn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
        return "Error: sin respuesta";
    }

}
