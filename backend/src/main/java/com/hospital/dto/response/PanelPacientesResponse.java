package com.hospital.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PanelPacientesResponse(
        List<PacientePanelRow> rows,
        long total,
        String alcance,
        String medico,
        String codMed,
        String fecha
) {}
