package com.hospital.dto.request;

public record UpdateProfileRequest(
        String nombre,
        String especialidad,
        String direccion,
        String telefono
) {}
