package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        String username,
        String email,
        String role,
        String codMed,
        String nombre,
        String especialidad,
        String direccion,
        String telefono,
        String codUni,
        String unidad,
        String codHos,
        String hospital
) {}
