package com.hospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.dto.common.ApiResponse;
import com.hospital.dto.response.PanelPacientesResponse;
import com.hospital.dto.response.PanelTurnosResponse;
import com.hospital.service.PanelService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Listados usados por las secciones Pacientes y Turnos del dashboard.
 * La visibilidad se decide en el servidor a partir de la sesión: un ADMIN ve
 * toda la red hospitalaria y un MEDICO solo sus propios datos.
 */
@RestController
@RequestMapping("/api/panel")
public class PanelController {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/pacientes")
    public ResponseEntity<ApiResponse<PanelPacientesResponse>> pacientes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            HttpServletRequest request) {
        if (username(request) == null) return noAutorizado();
        return ResponseEntity.ok(ApiResponse.ok(
                panelService.pacientes(username(request), role(request), q, fecha, limit, offset)));
    }

    @GetMapping("/turnos")
    public ResponseEntity<ApiResponse<PanelTurnosResponse>> turnos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset,
            HttpServletRequest request) {
        if (username(request) == null) return noAutorizado();
        return ResponseEntity.ok(ApiResponse.ok(
                panelService.turnos(username(request), role(request), q, fecha, limit, offset)));
    }

    private <T> ResponseEntity<ApiResponse<T>> noAutorizado() {
        return ResponseEntity.status(401)
                .body(ApiResponse.error("Debes iniciar sesión para ver esta información."));
    }

    private HttpSession sesion(HttpServletRequest request) {
        return request.getSession(false);
    }

    private String username(HttpServletRequest request) {
        HttpSession session = sesion(request);
        return session == null ? null : (String) session.getAttribute("username");
    }

    private String role(HttpServletRequest request) {
        HttpSession session = sesion(request);
        if (session == null) return null;
        Object role = session.getAttribute("role");
        return role == null ? null : String.valueOf(role);
    }
}
