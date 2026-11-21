package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TurnoPanelRow(
        String id,
        int numero,
        String codMed,
        String paciente,
        String pacienteId,
        String profesional,
        String especialidad,
        String estatus,
        String estadoOriginal,
        String hora,
        String fin,
        int duracion,
        String notaClinica,
        String fecha,
        String consultorio,
        String unidad,
        String departamento,
        String hospital,
        int atendidos,
        int consultas,
        int cantidadAtendida
) {}
