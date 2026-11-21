package com.hospital.controller.JPA;

import com.hospital.dto.common.ApiResponse;
import com.hospital.dto.request.ForgotPasswordRequest;
import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.request.ResetPasswordRequest;
import com.hospital.dto.request.UpdateProfileRequest;
import com.hospital.dto.response.UserProfileResponse;
import com.hospital.model.EntidadesJPA.User;
import com.hospital.service.JPA.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody LoginRequest body,
                                                   HttpServletRequest request) {
        String username = body.username();
        String email = body.email();
        String password = body.password();

        if (password == null || password.isBlank()
                || ((username == null || username.isBlank())
                    && (email == null || email.isBlank()))) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Completa el correo y la contraseña."));
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

            return ResponseEntity.ok(ApiResponse.okUser(datosUsuario(user)));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Correo o contraseña incorrectos."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody RegisterRequest body) {
        try {
            authService.registrar(
                    body.username(),
                    body.email(),
                    body.password(),
                    body.codMed());
            return ResponseEntity.status(201)
                    .body(ApiResponse.okMessage("Registro completado."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Opciones para el formulario de registro: si es el primer usuario,
     * hospitales, unidades y médicos que todavía no tienen cuenta.
     */
    @GetMapping("/registro/opciones")
    public ResponseEntity<ApiResponse<Map<String, Object>>> opcionesRegistro() {
        return ResponseEntity.ok(ApiResponse.ok(authService.opcionesRegistro()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Void>> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Debes iniciar sesión."));
        }

        User user = authService.obtenerPorUsername((String) session.getAttribute("username"));
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Debes iniciar sesión."));
        }

        return ResponseEntity.ok(ApiResponse.okUser(datosUsuario(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody UpdateProfileRequest body,
                                                      HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Debes iniciar sesión."));
        }

        User user = authService.obtenerPorUsername((String) session.getAttribute("username"));
        if (user == null) {
            session.invalidate();
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Debes iniciar sesión."));
        }

        try {
            User updated = authService.actualizarPerfil(
                    user.getUsername(),
                    body.nombre(),
                    body.especialidad(),
                    body.direccion(),
                    body.telefono());
            return ResponseEntity.ok(ApiResponse.okUser(datosUsuario(updated)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(
            @RequestBody(required = false) ForgotPasswordRequest body,
            HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                            ? "" : ":" + request.getServerPort());
            Map<String, Object> data = authService.solicitarRecuperacion(
                    body == null ? null : body.email(), baseUrl);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(
            @RequestBody(required = false) ResetPasswordRequest body) {
        try {
            authService.restablecerContrasena(
                    body == null ? null : body.email(),
                    body == null ? null : body.token(),
                    body == null ? null : body.password());
            return ResponseEntity.ok(ApiResponse.ok(
                    Map.of("message", "Contraseña actualizada. Ya puedes iniciar sesión.")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private UserProfileResponse datosUsuario(User user) {
        Map<String, Object> perfil = authService.perfilMedico(user.getUsername());
        return new UserProfileResponse(
                user.getUsername(),
                user.getEmail(),
                authService.rolEfectivo(user),
                perfil == null ? null : str(perfil.get("codMed")),
                perfil == null ? null : str(perfil.get("nombre")),
                perfil == null ? null : str(perfil.get("especialidad")),
                perfil == null ? null : str(perfil.get("direccion")),
                perfil == null ? null : str(perfil.get("telefono")),
                perfil == null ? null : str(perfil.get("codUni")),
                perfil == null ? null : str(perfil.get("unidad")),
                perfil == null ? null : str(perfil.get("codHos")),
                perfil == null ? null : str(perfil.get("hospital"))
        );
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
