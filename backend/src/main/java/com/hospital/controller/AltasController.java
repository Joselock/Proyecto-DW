package com.hospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.dto.common.ApiResponse;
import com.hospital.dto.request.CrearMedicoRequest;
import com.hospital.dto.request.CrearPacienteRequest;
import com.hospital.dto.request.CrearTurnoRequest;
import com.hospital.dto.request.EditarMedicoRequest;
import com.hospital.dto.request.EditarPacienteRequest;
import com.hospital.dto.request.EditarTurnoRequest;
import com.hospital.dto.response.CatalogosAltasResponse;
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
    public ResponseEntity<ApiResponse<CatalogosAltasResponse>> catalogos(HttpServletRequest request) {
        String username = username(request);
        if (username == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(
                    "Debes iniciar sesión para realizar esta operación."));
        }
        return ResponseEntity.ok(ApiResponse.ok(altasService.catalogos(username, role(request))));
    }

    @PostMapping("/pacientes")
    public ResponseEntity<ApiResponse<Object>> crearPaciente(@RequestBody CrearPacienteRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.crearPaciente(username, role(request), body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PutMapping("/pacientes/{numHistoria}")
    public ResponseEntity<ApiResponse<Object>> editarPaciente(@PathVariable String numHistoria,
            @RequestBody EditarPacienteRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.editarPaciente(username, role(request), numHistoria, body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @DeleteMapping("/pacientes/{numHistoria}")
    public ResponseEntity<ApiResponse<Object>> eliminarPaciente(@PathVariable String numHistoria,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.eliminarPaciente(username, role(request), numHistoria));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/turnos")
    public ResponseEntity<ApiResponse<Object>> crearTurno(@RequestBody CrearTurnoRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.crearTurno(username, role(request), body));
        } catch (Exception e) {
            return error(e);
        }
    }

    /**
     * Edita fecha, hora, nota clínica y estado de un turno (menú de tres puntos).
     */
    @PutMapping("/turnos/{numero}")
    public ResponseEntity<ApiResponse<Object>> editarTurno(@PathVariable int numero,
            @RequestBody EditarTurnoRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.editarTurno(username, role(request), numero, body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/medicos")
    public ResponseEntity<ApiResponse<Object>> crearMedico(@RequestBody CrearMedicoRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.crearMedico(username, role(request), body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PutMapping("/medicos/{codMed}")
    public ResponseEntity<ApiResponse<Object>> editarMedico(@PathVariable String codMed,
            @RequestBody EditarMedicoRequest body,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.editarMedico(username, role(request), codMed, body));
        } catch (Exception e) {
            return error(e);
        }
    }

    @DeleteMapping("/medicos/{codMed}")
    public ResponseEntity<ApiResponse<Object>> eliminarMedico(@PathVariable String codMed,
            HttpServletRequest request) {
        String username = username(request);
        if (username == null)
            return noAutorizado();
        try {
            return ok(altasService.eliminarMedico(username, role(request), codMed));
        } catch (Exception e) {
            return error(e);
        }
    }

    private ResponseEntity<ApiResponse<Object>> ok(Object data) {
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private ResponseEntity<ApiResponse<Object>> noAutorizado() {
        return ResponseEntity.status(401).body(ApiResponse.error(
                "Debes iniciar sesión para realizar esta operación."));
    }

    private ResponseEntity<ApiResponse<Object>> error(Exception e) {
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
        return ResponseEntity.status(status).body(ApiResponse.error(mensaje));
    }

    private String username(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute("username");
    }

    private String role(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null)
            return null;
        Object role = session.getAttribute("role");
        return role == null ? null : String.valueOf(role);
    }
}
