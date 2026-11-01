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

            Map<String, Object> userData = new HashMap<>();
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            if (user.getDataUser() != null) {
                userData.put("nombre", user.getDataUser().getNombre());
                userData.put("especialidad", user.getDataUser().getEspecialidad());
            }

            return ResponseEntity.ok(Map.of("ok", true, "user", userData));

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
                    body.get("nombre"),
                    body.get("especialidad"),
                    body.get("direccion"),
                    body.get("telefono"));
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

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("ok", false, "error", "Debes iniciar sesión."));
        }
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "user", Map.of(
                        "username", session.getAttribute("username"),
                        "email", session.getAttribute("email"))));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
