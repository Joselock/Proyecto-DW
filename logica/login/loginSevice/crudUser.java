package com.mycompany.gestorui.model.login.loginSevice;

import com.mycompany.gestorui.model.utils.BaseDatosLogin;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class crudUser {

    public static boolean registrarUsuario(String username, String gmail, String password, String nombreC,
            String especialidad, String direccion, String telefono) throws SQLException {
        
        
        boolean insertado = false;
        String function = "SELECT * FROM public.func_crear_usuario(?,?,?,?,?,?,?)";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement ps = con.prepareStatement(function)) {
            
            ps.setString(1, username);
            ps.setString(2, gmail);
            ps.setString(3, password);
            ps.setString(4, nombreC);
            ps.setString(5, especialidad);
            ps.setString(6, direccion);
            ps.setString(7, telefono);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    insertado = rs.getBoolean(1);
                }
            }
        }
        return insertado;
    }

    public static boolean modificarPassword(String username, String passwordActual, String passwordNueva) 
            throws SQLException {
        
        boolean modificada = false;
        String function = "SELECT * FROM public.func_modificar_contrasena(?,?,?)";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement ps = con.prepareStatement(function)) {
            
            ps.setString(1, username);
            ps.setString(2, passwordActual);
            ps.setString(3, passwordNueva);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    modificada = rs.getBoolean(1);
                }
            }
        }
        return modificada;
    }


    public static boolean modificarDatos(String usernameOriginal, String usernameNuevo, String emailNuevo, 
            String nombreC, String especialidad, String direccion, String telefono) throws SQLException {
        
        boolean modificados = false;
        String function = "SELECT * FROM public.func_modificar_datos_usuario(?,?,?,?,?,?,?)";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement ps = con.prepareStatement(function)) {
            
            ps.setString(1, usernameOriginal);
            ps.setString(2, usernameNuevo);
            ps.setString(3, emailNuevo);
            ps.setString(4, nombreC);
            ps.setString(5, especialidad);
            ps.setString(6, direccion);
            ps.setString(7, telefono);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    modificados = rs.getBoolean(1);
                }
            }
        }
        return modificados;
    }

    public static boolean eliminarUsuario(String username) throws SQLException {
        boolean eliminado = false;
        String function = "SELECT * FROM public.func_eliminar_usuario(?)";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement ps = con.prepareStatement(function)) {
            
            ps.setString(1, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    eliminado = rs.getBoolean(1);
                }
            }
        }
        return eliminado;
    }


    public static boolean actualizarContraseñaPorEmail(String email, String nuevaPassword) throws SQLException {
        boolean actualizada = false;
        String function = "SELECT * FROM public.func_actualizar_contrasena_por_email(?, ?)";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement ps = con.prepareStatement(function)) {
            
            ps.setString(1, email);
            ps.setString(2, nuevaPassword);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    actualizada = rs.getBoolean(1);
                }
            }
        }
        return actualizada;
    }

}