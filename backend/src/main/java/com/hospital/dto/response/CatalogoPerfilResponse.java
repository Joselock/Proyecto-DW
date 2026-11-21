package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogoPerfilResponse(
        String rol,
        String codMed,
        String codUni,
        String codHos,
        String nombre
) {}
