package com.hospital.dto.request;

public record LoginRequest(
        String username,
        String email,
        String password
) {}
