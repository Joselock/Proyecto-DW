package com.hospital.dto.response;

import java.util.List;

public record NotificacionesResponse(
        List<NotificacionRow> rows,
        int noLeidas,
        int ultimoId,
        String alcance
) {}
