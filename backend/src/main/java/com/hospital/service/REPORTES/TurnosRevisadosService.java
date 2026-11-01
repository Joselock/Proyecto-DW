package com.mycompany.gestorui.model.services.reportes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.mycompany.gestorui.model.utils.BaseDatos;

public class TurnosRevisadosService {
    
    private static TurnosRevisadosService instance;
    private final Set<String> cacheTurnosRevisados;
    private boolean cargado = false;
    
    private TurnosRevisadosService() {
        cacheTurnosRevisados = new HashSet<>();
    }
    
    public static TurnosRevisadosService getInstance() {
        if (instance == null) {
            instance = new TurnosRevisadosService();
        }
        return instance;
    }
    
    /**
     * Marca un turno como revisado actualizando su estado en la tabla Turno
     */
    public boolean marcarComoRevisado(String hospital, String departamento, String unidad, String medico) {
        // Primero obtener el codUni y codMed basado en los nombres
        String sqlBuscar = "SELECT u.\"codUni\", m.\"codMed\" " +
                           "FROM public.\"Unidad\" u " +
                           "JOIN public.\"Departamento\" d ON u.\"codDep\" = d.\"codDep\" " +
                           "JOIN public.\"Hospital\" h ON d.\"codHos\" = h.\"codHos\" " +
                           "JOIN public.\"Medico\" m ON m.\"nomMed\" = ? " +
                           "WHERE h.\"nomHos\" = ? " +
                           "AND d.\"nomDep\" = ? " +
                           "AND u.\"nomUni\" = ? " +
                           "LIMIT 1";
        
        String sqlActualizar = "UPDATE public.\"Turno\" t " +
                               "SET \"estTur\" = 'Revisado' " +
                               "WHERE t.\"codUni\" = ? " +
                               "AND t.\"codMed\" = ? " +
                               "AND t.\"estTur\" = 'Revisar'";
        
        try (java.sql.Connection con = BaseDatos.getConnection()) {
            // Buscar los códigos
            String codUni = null;
            String codMed = null;
            
            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
                ps.setString(1, medico);
                ps.setString(2, hospital);
                ps.setString(3, departamento);
                ps.setString(4, unidad);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        codUni = rs.getString("codUni");
                        codMed = rs.getString("codMed");
                    }
                }
            }
            
            if (codUni == null || codMed == null) {
                System.err.println("⚠️ No se encontró la unidad o médico para: " + hospital + " | " + departamento + " | " + unidad + " | " + medico);
                String clave = generarClave(hospital, departamento, unidad, medico);
                cacheTurnosRevisados.add(clave);
                return true;
            }
            
            // Actualizar el turno
            try (PreparedStatement psActualizar = con.prepareStatement(sqlActualizar)) {
                psActualizar.setString(1, codUni);
                psActualizar.setString(2, codMed);
                
                int result = psActualizar.executeUpdate();
                
                if (result > 0) {
                    String clave = generarClave(hospital, departamento, unidad, medico);
                    cacheTurnosRevisados.add(clave);
                    System.out.println("✅ Turno marcado como Revisado: " + hospital + " | " + departamento + " | " + unidad + " | " + medico);
                    return true;
                } else {
                    // Si no hay turno pendiente, lo marcamos en caché igual
                    String clave = generarClave(hospital, departamento, unidad, medico);
                    cacheTurnosRevisados.add(clave);
                    System.out.println("⚠️ No se encontró turno pendiente, pero se marcó en caché: " + clave);
                    return true;
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica si un turno ya fue revisado
     */
    public boolean esRevisado(String hospital, String departamento, String unidad, String medico) {
        if (!cargado) {
            cargar();
        }
        
        String clave = generarClave(hospital, departamento, unidad, medico);
        return cacheTurnosRevisados.contains(clave);
    }
    
    /**
     * Carga todos los turnos que ya están revisados desde la base de datos
     */
    public void cargar() {
        cacheTurnosRevisados.clear();
        
        String sql = "SELECT h.\"nomHos\", d.\"nomDep\", u.\"nomUni\", m.\"nomMed\" " +
                     "FROM public.\"Turno\" t " +
                     "JOIN public.\"Unidad\" u ON t.\"codUni\" = u.\"codUni\" " +
                     "JOIN public.\"Departamento\" d ON u.\"codDep\" = d.\"codDep\" " +
                     "JOIN public.\"Hospital\" h ON d.\"codHos\" = h.\"codHos\" " +
                     "JOIN public.\"Medico\" m ON t.\"codMed\" = m.\"codMed\" " +
                     "WHERE t.\"estTur\" IN ('Revisado', 'Extioso')";
        
        try (java.sql.Connection con = BaseDatos.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String hospital = rs.getString("nomHos");
                String departamento = rs.getString("nomDep");
                String unidad = rs.getString("nomUni");
                String medico = rs.getString("nomMed");
                
                String clave = generarClave(hospital, departamento, unidad, medico);
                cacheTurnosRevisados.add(clave);
            }
            
            cargado = true;
            System.out.println("📦 Cargados " + cacheTurnosRevisados.size() + " turnos revisados desde la base de datos");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void limpiar() {
        cargado = false;
        cacheTurnosRevisados.clear();
    }
    
    private String generarClave(String hospital, String departamento, String unidad, String medico) {
        return hospital + "|" + departamento + "|" + unidad + "|" + medico;
    }
}