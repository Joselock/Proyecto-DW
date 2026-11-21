package com.hospital.dto.response;

import java.util.List;

public record CatalogosAltasResponse(
        List<CatalogoHospitalResponse> hospitales,
        List<CatalogoDepartamentoResponse> departamentos,
        List<CatalogoUnidadResponse> unidades,
        List<CatalogoMedicoResponse> medicos,
        CatalogoPerfilResponse perfil
) {}
