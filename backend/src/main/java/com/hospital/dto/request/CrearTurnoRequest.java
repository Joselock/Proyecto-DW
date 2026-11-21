package com.hospital.dto.request;

public record CrearTurnoRequest(
        String fecha,
        String hora,
        String pacienteId,
        String codMed,
        String codUni,
        String estado,
        String notaClinica,
        String nota
) {}
