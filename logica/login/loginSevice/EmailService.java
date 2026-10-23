package com.mycompany.gestorui.model.login.loginSevice;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {
    
     // 🔥 CAMBIA ESTO: true = MODO PRUEBA (solo consola), false = MODO REAL (envía correos)
    private static final boolean MODO_PRUEBA = true;
    
    // Configuración para MODO REAL (solo se usa si MODO_PRUEBA = false)
    private static final String HOST = "smtp.gmail.com";
    private static final String PORT = "587";
    private static final String USERNAME = "tu_correo@gmail.com";
    private static final String PASSWORD = "xxxx xxxx xxxx xxxx";
    private static final boolean AUTH = true;
    private static final boolean TLS = true;
    
    public boolean enviarCorreo(String destinatario, String asunto, String mensaje) {
        if (MODO_PRUEBA) {
            return enviarCorreoPrueba(destinatario, asunto, mensaje);
        } else {
            return enviarCorreoReal(destinatario, asunto, mensaje);
        }
    }
    
    /**
     * MODO PRUEBA - Solo muestra el código en consola
     */
    private boolean enviarCorreoPrueba(String destinatario, String asunto, String mensaje) {
        // Extraer el código
        String codigo = "CÓDIGO_NO_ENCONTRADO";
        if (mensaje.contains("código de verificación es:")) {
            String[] partes = mensaje.split("código de verificación es: ");
            if (partes.length > 1) {
                String posibleCodigo = partes[1].split("\n")[0].trim();
                if (posibleCodigo.matches("\\d{6}")) {
                    codigo = posibleCodigo;
                }
            }
        }
        
        System.out.println("\n==============================================");
        System.out.println("📧 MODO PRUEBA - NO SE ENVIÓ CORREO REAL");
        System.out.println("==============================================");
        System.out.println("📧 PARA: " + destinatario);
        System.out.println("📧 ASUNTO: " + asunto);
        System.out.println("📧 CÓDIGO DE VERIFICACIÓN: " + codigo);
        System.out.println("==============================================");
        System.out.println("📧 Copia este código y pégalo en la aplicación");
        System.out.println("==============================================\n");
        
        return true;
    }
    
    /**
     * MODO REAL - Envía correos usando SMTP
     */
    private boolean enviarCorreoReal(String destinatario, String asunto, String mensaje) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", HOST);
            props.put("mail.smtp.port", PORT);
            props.put("mail.smtp.auth", AUTH);
            
            if (TLS) {
                props.put("mail.smtp.starttls.enable", "true");
            }
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, PASSWORD);
                }
            });
            
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress(USERNAME));
            email.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
            email.setSubject(asunto);
            email.setText(mensaje);
            
            Transport.send(email);
            System.out.println("✅ Correo enviado a: " + destinatario);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("❌ Error al enviar correo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}