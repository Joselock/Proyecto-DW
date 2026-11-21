package com.hospital.dto.request;

public record EditarTurnoRequest(
        String fecha,
        String hora,
        String estatus,
        String estadoOriginal,
        String notaClinica,
        Boolean atendido
) {}
