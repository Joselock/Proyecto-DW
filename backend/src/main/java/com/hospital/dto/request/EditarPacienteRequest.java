package com.hospital.dto.request;

public record EditarPacienteRequest(
        String documento,
        String nombre,
        String direccion,
        String codUni,
        String estado,
        String fechaNacimiento
) {}
