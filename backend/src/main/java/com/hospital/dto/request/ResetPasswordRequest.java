package com.hospital.dto.request;

public record ResetPasswordRequest(
        String email,
        String token,
        String password
) {}
