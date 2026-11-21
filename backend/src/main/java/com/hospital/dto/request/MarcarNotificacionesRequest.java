package com.hospital.dto.request;

import java.util.List;

public record MarcarNotificacionesRequest(
        List<Long> ids
) {}
