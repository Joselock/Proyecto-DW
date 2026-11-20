package com.hospital.controller.JPA;

import com.hospital.model.EntidadesJPA.User;
import com.hospital.service.JPA.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (password == null || password.isBlank()
                || ((username == null || username.isBlank())
                    && (email == null || email.isBlank()))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "Completa el correo y la contraseña."));
        }

        try {
            User user = username != null && !username.isBlank()
                    ? authService.autenticar(username.trim(), password)
                    : authService.autenticarPorEmail(email.trim().toLowerCase(Locale.ROOT), password);

            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute("username", user.getUsername());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", authService.rolEfectivo(user));

            return ResponseEntity.ok(Map.of("ok", true, "user", datosUsuario(user)));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "error", "Correo o contraseña incorrectos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            authService.registrar(
                    body.get("username"),
                    body.get("email"),
                    body.get("password"),
                    body.get("codMed"));
            return ResponseEntity.status(201)
                    .body(Map.of("ok", true, "message", "Registro completado."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    /**
     * Opciones para el formulario de registro: si es el primer usuario,
     * hospitales, unidades y médicos que todavía no tienen cuenta.
     */
    @GetMapping("/registro/opciones")
    public ResponseEntity<?> opcionesRegistro() {
        return ResponseEntity.ok(Map.of("ok", true, "data", authService.opcionesRegistro()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "error", "Debes iniciar sesión."));
        }

        User user = authService.obtenerPorUsername((String) session.getAttribute("username"));
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "error", "Debes iniciar sesión."));
        }

        return ResponseEntity.ok(Map.of("ok", true, "user", datosUsuario(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String, String> body,
                                      HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "error", "Debes iniciar sesión."));
        }

        User user = authService.obtenerPorUsername((String) session.getAttribute("username"));
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "error", "Debes iniciar sesión."));
        }

        try {
            User updated = authService.actualizarPerfil(
                    user.getUsername(),
                    body.get("nombre"),
                    body.get("especialidad"),
                    body.get("direccion"),
                    body.get("telefono"));
            return ResponseEntity.ok(Map.of("ok", true, "user", datosUsuario(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                            ? "" : ":" + request.getServerPort());
            Map<String, Object> data = authService.solicitarRecuperacion(
                    body == null ? null : body.get("email"), baseUrl);
            return ResponseEntity.ok(Map.of("ok", true, "data", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            authService.restablecerContrasena(
                    body == null ? null : body.get("email"),
                    body == null ? null : body.get("token"),
                    body == null ? null : body.get("password"));
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "data", Map.of("message", "Contraseña actualizada. Ya puedes iniciar sesión.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    private Map<String, Object> datosUsuario(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("role", authService.rolEfectivo(user));

        // El perfil del usuario son los datos del médico vinculado.
        Map<String, Object> perfil = authService.perfilMedico(user.getUsername());
        if (perfil != null) {
            userData.putAll(perfil);
        }
        return userData;
    }
}
