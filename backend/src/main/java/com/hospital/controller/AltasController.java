package com.hospital.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.service.AltasService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Altas de pacientes, turnos y médicos protegidas por la sesión HTTP. */
@RestController
@RequestMapping("/api/altas")
public class AltasController {

    private final AltasService altasService;

    public AltasController(AltasService altasService) {
        this.altasService = altasService;
    }

    @GetMapping("/catalogos")
    public ResponseEntity<?> catalogos(HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();
        return ok(altasService.catalogos(username, role(request)));
    }

    @PostMapping("/pacientes")
    public ResponseEntity<?> crearPaciente(@RequestBody Map<String, String> body,
                                           HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();
        try {
            return ok(altasService.crearPaciente(username, role(request), body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/turnos")
    public ResponseEntity<?> crearTurno(@RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();
        try {
            return ok(altasService.crearTurno(username, role(request), body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/medicos")
    public ResponseEntity<?> crearMedico(@RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();
        try {
            return ok(altasService.crearMedico(username, role(request), body));
        } catch (Exception e) {
            return error(e);
        }
    }

    private ResponseEntity<?> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("data", data);
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<?> noAutorizado() {
        return ResponseEntity.status(401).body(Map.of(
                "ok", false,
                "error", "Debes iniciar sesión para realizar esta operación."));
    }

    private ResponseEntity<?> error(Exception e) {
        int status = 500;
        if (e instanceof IllegalArgumentException) {
            status = 400;
        } else if (e instanceof SecurityException) {
            status = 403;
        } else if (e instanceof IllegalStateException) {
            status = 409;
        }
        String mensaje = e.getMessage() == null || e.getMessage().isBlank()
                ? "No se pudo completar la operación."
                : e.getMessage();
        return ResponseEntity.status(status).body(Map.of("ok", false, "error", mensaje));
    }

    private String username(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute("username");
    }

    private String role(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object role = session.getAttribute("role");
        return role == null ? null : String.valueOf(role);
    }
}
