package com.hospital.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envoltorio estándar de respuestas de la API.
 * Mantiene compatibilidad con el formato existente: {ok, data/error/...}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean ok,
        T data,
        String error,
        String message,
        Object user
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static <T> ApiResponse<T> okMessage(String message) {
        return new ApiResponse<>(true, null, null, message, null);
    }

    public static <T> ApiResponse<T> okUser(Object user) {
        return new ApiResponse<>(true, null, null, null, user);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, error, null, null);
    }
}
