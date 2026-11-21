package com.hospital.dto.request;

public record CrearMedicoRequest(
        String codMed,
        String nombre,
        String especialidad,
        String licencia,
        String telefono,
        String datosContacto,
        String direccion,
        String codUni,
        Integer experiencia
) {}
