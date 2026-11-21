package com.hospital.dto.response;

public record CatalogoMedicoResponse(
        String codMed,
        String nombre,
        String especialidad,
        String codUni,
        String unidad,
        String codDep,
        String departamento,
        String codHos,
        String hospital
) {}
