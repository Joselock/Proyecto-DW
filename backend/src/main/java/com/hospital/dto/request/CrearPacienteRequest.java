package com.hospital.dto.request;

public record CrearPacienteRequest(
        String documento,
        String nombre,
        String direccion,
        String codUni,
        String estado,
        String fechaNacimiento
) {}
