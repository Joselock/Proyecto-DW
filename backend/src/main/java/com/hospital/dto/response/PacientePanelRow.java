package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PacientePanelRow(
        String id,
        String nombre,
        String nacimiento,
        String admision,
        String direccion,
        String estado,
        String estatus,
        Integer edad,
        boolean esNuevo,
        String codUni,
        String unidad,
        String departamento,
        String hospital,
        String medico,
        String ultimaVisita
) {}
