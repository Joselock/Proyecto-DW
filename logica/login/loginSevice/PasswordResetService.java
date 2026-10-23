package com.mycompany.gestorui.model.login.loginSevice;

import com.mycompany.gestorui.model.utils.BaseDatosLogin;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

public class PasswordResetService {
    
    private static PasswordResetService instance;
    private final EmailService emailService;
    
    private PasswordResetService() {
        emailService = new EmailService();
    }
    
    public static PasswordResetService getInstance() {
        if (instance == null) {
            instance = new PasswordResetService();
        }
        return instance;
    }
    
    /**
     * Genera un código de 6 dígitos aleatorios
     */
    private String generarCodigo() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }
    
    /**
     * Solicita recuperación de contraseña para un correo
     * @return true si se envió el correo, false si el correo no existe
     */
    public boolean solicitarRecuperacion(String email) {
        // Verificar si el correo existe en la base de datos
        if (!existeUsuario(email)) {
            System.out.println("❌ Correo no registrado: " + email);
            return false;
        }
        
        // Generar código de verificación
        String codigo = generarCodigo();
        
        // Guardar el código en la base de datos
        if (!guardarToken(email, codigo)) {
            System.out.println("❌ Error al guardar el token");
            return false;
        }
        
        // Enviar el código por correo
        String mensaje = "Hola,\n\nHas solicitado recuperar tu contraseña. "
                + "Tu código de verificación es: " + codigo 
                + "\n\nEste código expirará en 15 minutos.\n\n"
                + "Si no solicitaste este cambio, ignora este mensaje.";
        
        boolean enviado = emailService.enviarCorreo(email, "Recuperación de Contraseña - Gestor Hospitalario", mensaje);
        
        if (enviado) {
            System.out.println("✅ Código enviado a: " + email + " - Código: " + codigo);
        }
        
        return enviado;
    }
    
    /**
     * Verifica si un correo existe en la base de datos
     */
    private boolean existeUsuario(String email) {
        String sql = "SELECT COUNT(*) FROM public.\"User\" WHERE \"email\" = ?";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Guarda el token en la base de datos
     */
    private boolean guardarToken(String email, String token) {
        // Primero eliminar tokens anteriores no usados para este email
        eliminarTokensExpirados(email);
        
        String sql = "INSERT INTO reset_tokens (email, token, fecha_creacion, fecha_expiracion) "
                + "VALUES (?, ?, ?, ?)";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime expiracion = ahora.plusMinutes(15);
            
            pstmt.setString(1, email);
            pstmt.setString(2, token);
            pstmt.setTimestamp(3, Timestamp.valueOf(ahora));
            pstmt.setTimestamp(4, Timestamp.valueOf(expiracion));
            
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Elimina tokens expirados para un email
     */
    private void eliminarTokensExpirados(String email) {
        String sql = "DELETE FROM reset_tokens WHERE email = ? AND fecha_expiracion < ?";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Verifica si un token es válido para un email
     */
    public boolean verificarToken(String email, String token) {
        String sql = "SELECT * FROM reset_tokens WHERE email = ? AND token = ? "
                + "AND usado = FALSE AND fecha_expiracion > ?";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, token);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // Marcar el token como usado
                marcarTokenComoUsado(rs.getInt("id"));
                return true;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Marca un token como usado
     */
    private void marcarTokenComoUsado(int id) {
        String sql = "UPDATE reset_tokens SET usado = TRUE WHERE id = ?";
        
        try (java.sql.Connection con = BaseDatosLogin.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Cambia la contraseña de un usuario usando la función de la base de datos
     */
    public boolean cambiarContraseña(String email, String nuevaContraseña) {
        try {
            // Usar tu método existente de crudUser
            return crudUser.actualizarContraseñaPorEmail(email, nuevaContraseña);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}