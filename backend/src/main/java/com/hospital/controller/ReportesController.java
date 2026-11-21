package com.hospital.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.dto.common.ApiResponse;
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
    public ApiResponse<Object> contexto(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute("username");

        Map<String, Object> data = new LinkedHashMap<>(contextoService.contextoUsuario(username));
        data.put("role", session == null ? null : session.getAttribute("role"));
        data.put("autenticado", username != null);
        return ApiResponse.ok(data);
    }

    @GetMapping("/profesionales")
    public ApiResponse<Object> profesionales(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String username = session == null ? null : (String) session.getAttribute("username");
        return ApiResponse.ok(contextoService.profesionalesDelUsuario(username));
    }

    @GetMapping("/hospitales/resumen")
    public ApiResponse<Object> resumenHospitales() {
        return ApiResponse.ok(hospitalService.resumenHospitales());
    }

    @GetMapping("/hospitales/mayores-de-cien")
    public ApiResponse<Object> hospitalesMayorCantidadPacientes() {
        return ApiResponse.ok(hospitalService.hospitalesMayorCantidadPacientes());
    }

    @GetMapping("/proceso")
    public ApiResponse<Object> resumenProceso() {
        return ApiResponse.ok(hospitalService.resumenProceso());
    }

    @GetMapping("/medicos")
    public ApiResponse<Object> listadoMedicos() {
        return ApiResponse.ok(medicoService.obtenerListadosMedicos());
    }

    @GetMapping("/pacientes")
    public ApiResponse<Object> listadoPacientes(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ApiResponse.ok(pacienteService.obtenerPacientesDepartamento(area, codigo));
    }

    @GetMapping("/pacientes/no-atendidos")
    public ApiResponse<Object> pacientesNoAtendidos(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ApiResponse.ok(pacienteService.listadoNoAtendidos(area, codigo));
    }

    @GetMapping("/consultas/informe")
    public ApiResponse<Object> informeDuranteConsultas(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ApiResponse.ok(consultaService.obtenerInformeHospital(area, codigo));
    }

    @GetMapping("/consultas/exitosas")
    public ApiResponse<Object> consultasExitosas(
            @RequestParam String area,
            @RequestParam String codigo) {
        return ApiResponse.ok(consultaService.resumenConsultasExitosas(area, codigo));
    }

    @GetMapping("/unidades/turnos-por-revisar")
    public ApiResponse<Object> turnosPorRevisar() {
        return ApiResponse.ok(unidadService.listadoUnidades());
    }
}
