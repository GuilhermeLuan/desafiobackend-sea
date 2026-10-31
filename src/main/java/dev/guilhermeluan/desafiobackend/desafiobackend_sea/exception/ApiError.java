package dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        List<FieldError> fieldErrors,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<Integer> missingSteps
) {

    public static ApiError of(int status, ErrorCode code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, List.of(), path, null);
    }

    public ApiError withFieldErrors(List<FieldError> fieldErrors) {
        return new ApiError(timestamp, status, code, message, List.copyOf(fieldErrors), path, missingSteps);
    }

    public ApiError withMissingSteps(List<Integer> missingSteps) {
        return new ApiError(timestamp, status, code, message, fieldErrors, path, List.copyOf(missingSteps));
    }

    public record FieldError(String field, String message) {
    }
}
