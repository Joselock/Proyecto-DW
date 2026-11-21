package com.hospital.dto.response;

public record NotificacionRow(
        int id,
        int numero,
        String tipo,
        String mensaje,
        String fecha,
        boolean leida,
        String fechaTurno,
        String hora,
        String paciente
) {}
