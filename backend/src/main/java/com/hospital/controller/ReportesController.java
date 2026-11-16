package com.hospital.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.service.REPORTES.ConsultaService;
import com.hospital.service.REPORTES.HospitalService;
import com.hospital.service.REPORTES.MedicoService;
import com.hospital.service.REPORTES.PacienteService;
import com.hospital.service.REPORTES.ReportesContextoService;
import com.hospital.service.REPORTES.UnidadService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final ConsultaService consultaService;
    private final HospitalService hospitalService;
    private final MedicoService medicoService;
    private final PacienteService pacienteService;
    private final UnidadService unidadService;
    private final ReportesContextoService contextoService;

    public ReportesController(
            ConsultaService consultaService,
            HospitalService hospitalService,
            MedicoService medicoService,
            PacienteService pacienteService,
            UnidadService unidadService,
            ReportesContextoService contextoService) {
        this.consultaService = consultaService;
        this.hospitalService = hospitalService;
        this.medicoService = medicoService;
        this.pacienteService = pacienteService;
        this.unidadService = unidadService;
        this.contextoService = contextoService;
    }

    @GetMapping("/contexto")
    public Map<String, Object> contexto(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute("username");

        Map<String, Object> data = new LinkedHashMap<>(contextoService.contextoUsuario(username));
        data.put("role", session == null ? null : session.getAttribute("role"));
        data.put("autenticado", username != null);
        return ok(data);
    }

    @GetMapping("/profesionales")
    public Map<String, Object> profesionales(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute("username");
        return ok(contextoService.profesionalesDelUsuario(username));
    }

    @GetMapping("/hospitales/resumen")
    public Map<String, Object> resumenHospitales() {
        return ok(hospitalService.resumenHospitales());
    }

    @GetMapping("/hospitales/mayores-de-cien")
    public Map<String, Object> hospitalesMayorCantidadPacientes() {
        return ok(hospitalService.hospitalesMayorCantidadPacientes());
    }

    @GetMapping("/proceso")
    public Map<String, Object> resumenProceso() {
        return ok(hospitalService.resumenProceso());
    }

    @GetMapping("/medicos")
    public Map<String, Object> listadoMedicos() {
        return ok(medicoService.obtenerListadosMedicos());
    }

    @GetMapping("/pacientes")
    public Map<String, Object> listadoPacientes(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ok(pacienteService.obtenerPacientesDepartamento(area, codigo));
    }

    @GetMapping("/pacientes/no-atendidos")
    public Map<String, Object> pacientesNoAtendidos(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ok(pacienteService.listadoNoAtendidos(area, codigo));
    }

    @GetMapping("/consultas/informe")
    public Map<String, Object> informeDuranteConsultas(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ok(consultaService.obtenerInformeHospital(area, codigo));
    }

    @GetMapping("/consultas/exitosas")
    public Map<String, Object> consultasExitosas(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ok(consultaService.resumenConsultasExitosas(area, codigo));
    }

    @GetMapping("/unidades/turnos-por-revisar")
    public Map<String, Object> turnosPorRevisar() {
        return ok(unidadService.listadoUnidades());
    }

    private static Map<String, Object> ok(Object data) {
        return Map.of("ok", true, "data", data);
    }
}
