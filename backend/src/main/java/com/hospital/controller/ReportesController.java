package com.hospital.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.service.REPORTES.ConsultaService;
import com.hospital.service.REPORTES.HospitalService;
import com.hospital.service.REPORTES.MedicoService;
import com.hospital.service.REPORTES.PacienteService;
import com.hospital.service.REPORTES.UnidadService;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final ConsultaService consultaService;
    private final HospitalService hospitalService;
    private final MedicoService medicoService;
    private final PacienteService pacienteService;
    private final UnidadService unidadService;

    public ReportesController(
            ConsultaService consultaService,
            HospitalService hospitalService,
            MedicoService medicoService,
            PacienteService pacienteService,
            UnidadService unidadService) {
        this.consultaService = consultaService;
        this.hospitalService = hospitalService;
        this.medicoService = medicoService;
        this.pacienteService = pacienteService;
        this.unidadService = unidadService;
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
