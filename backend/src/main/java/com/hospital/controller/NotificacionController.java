package com.hospital.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.service.NotificacionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Avisos de inicio y fin de consulta visibles según el rol de la sesión. */
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false, defaultValue = "30") Integer limite,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();
        return ok(notificacionService.listar(username, role(request), limite));
    }

    @PostMapping("/leer")
    public ResponseEntity<?> marcarLeidas(@RequestBody(required = false) Map<String, Object> body,
                                          HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();

        List<Long> ids = new ArrayList<>();
        Object valores = body == null ? null : body.get("ids");
        if (valores instanceof List<?> lista) {
            for (Object valor : lista) {
                try {
                    if (valor instanceof Number number) ids.add(number.longValue());
                    else ids.add(Long.parseLong(String.valueOf(valor).trim()));
                } catch (NumberFormatException ignored) {
                    // Se ignoran identificadores no numéricos.
                }
            }
        }

        int actualizadas = notificacionService.marcarLeidas(username, role(request), ids);
        return ok(Map.of("actualizadas", actualizadas));
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
                "error", "Debes iniciar sesión para ver las notificaciones."));
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
