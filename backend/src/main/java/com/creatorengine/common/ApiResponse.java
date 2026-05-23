package com.creatorengine.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Uniform response envelope returned by every controller.
 *
 * <pre>
 * {
 *   "success":  true,
 *   "message":  "OK",
 *   "data":     { ... payload ... },
 *   "errors":   [ "field: reason", ... ],   // only on validation failure
 *   "timestamp": "2025-..."
 * }
 * </pre>
 *
 * The frontend axios interceptor unwraps {@code data} so component code
 * never has to handle the envelope directly.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final List<String> errors;
    private final Instant timestamp = Instant.now();

    private ApiResponse(boolean success, String message, T data, List<String> errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
