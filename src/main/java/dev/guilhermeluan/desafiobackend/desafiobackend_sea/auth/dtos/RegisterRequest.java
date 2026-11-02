package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "cliente@example.com")
        @NotBlank @Email String email,
        @Schema(example = "password123")
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
