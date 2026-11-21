package com.hospital.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PanelTurnosResponse(
        List<TurnoPanelRow> rows,
        long total,
        String alcance,
        String medico,
        String codMed,
        String fecha
) {}
