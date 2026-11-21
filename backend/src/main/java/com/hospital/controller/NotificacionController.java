package com.hospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.dto.common.ApiResponse;
import com.hospital.dto.request.MarcarNotificacionesRequest;
import com.hospital.dto.response.MarcarNotificacionesResponse;
import com.hospital.dto.response.NotificacionesResponse;
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
    public ResponseEntity<ApiResponse<NotificacionesResponse>> listar(
            @RequestParam(required = false, defaultValue = "30") Integer limite,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();
        return ResponseEntity.ok(ApiResponse.ok(notificacionService.listar(username, role(request), limite)));
    }

    @PostMapping("/leer")
    public ResponseEntity<ApiResponse<MarcarNotificacionesResponse>> marcarLeidas(
            @RequestBody(required = false) MarcarNotificacionesRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null) return noAutorizado();

        int actualizadas = notificacionService.marcarLeidas(
                username, role(request), body == null ? null : body.ids());
        return ResponseEntity.ok(ApiResponse.ok(new MarcarNotificacionesResponse(actualizadas)));
    }

    private <T> ResponseEntity<ApiResponse<T>> noAutorizado() {
        return ResponseEntity.status(401).body(ApiResponse.error(
                "Debes iniciar sesión para ver las notificaciones."));
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
