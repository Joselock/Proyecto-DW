package com.hospital.service.REPORTES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;


@org.springframework.stereotype.Service
public class TurnosRevisadosService extends com.hospital.service.JdbcServiceSupport {
    
    private final Set<String> cacheTurnosRevisados;
    private volatile boolean cargado = false;
    
    public TurnosRevisadosService(javax.sql.DataSource dataSource) {
        super(dataSource);
        cacheTurnosRevisados = ConcurrentHashMap.newKeySet();
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
                           "JOIN public.\"Medico\" m ON m.\"codUni\" = u.\"codUni\" " +
                           "WHERE h.\"nomHos\" = ? " +
                           "AND d.\"nomDep\" = ? " +
                           "AND u.\"nomUni\" = ? " +
                           "AND m.\"nomMed\" = ? " +
                           "LIMIT 1";
        
        String sqlActualizar = "UPDATE public.\"Turno\" t " +
                               "SET \"estTur\" = 'Revisado' " +
                               "WHERE t.\"codUni\" = ? " +
                               "AND t.\"codMed\" = ? " +
                               "AND t.\"estTur\" = 'Revisar'";
        
        try (java.sql.Connection con = getConnection()) {
            // Buscar los códigos
            String codUni = null;
            String codMed = null;
            
            try (PreparedStatement ps = con.prepareStatement(sqlBuscar)) {
                ps.setString(1, hospital);
                ps.setString(2, departamento);
                ps.setString(3, unidad);
                ps.setString(4, medico);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        codUni = rs.getString("codUni");
                        codMed = rs.getString("codMed");
                    }
                }
            }
            
            if (codUni == null || codMed == null) {
                return false;
            }
            
            // Actualizar el turno
            try (PreparedStatement psActualizar = con.prepareStatement(sqlActualizar)) {
                psActualizar.setString(1, codUni);
                psActualizar.setString(2, codMed);
                
                int result = psActualizar.executeUpdate();
                
                if (result > 0) {
                    String clave = generarClave(hospital, departamento, unidad, medico);
                    cacheTurnosRevisados.add(clave);
                    return true;
                }
                return false;
            }
            
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo marcar el turno como revisado.", e);
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
    public synchronized void cargar() {
        cacheTurnosRevisados.clear();
        
        String sql = "SELECT h.\"nomHos\", d.\"nomDep\", u.\"nomUni\", m.\"nomMed\" " +
                     "FROM public.\"Turno\" t " +
                     "JOIN public.\"Unidad\" u ON t.\"codUni\" = u.\"codUni\" " +
                     "JOIN public.\"Departamento\" d ON u.\"codDep\" = d.\"codDep\" " +
                     "JOIN public.\"Hospital\" h ON d.\"codHos\" = h.\"codHos\" " +
                     "JOIN public.\"Medico\" m ON t.\"codMed\" = m.\"codMed\" " +
                     "WHERE LOWER(t.\"estTur\") IN ('revisado', 'exitoso', 'extioso')";
        
        try (java.sql.Connection con = getConnection();
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
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los turnos revisados.", e);
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