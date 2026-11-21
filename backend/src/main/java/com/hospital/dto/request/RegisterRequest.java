package com.hospital.dto.request;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String codMed
) {}
