package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.ValidUf;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record CreateUserRequest(
        @Schema(example = "analista@example.com")
        @NotBlank @Email String email,
        @Schema(example = "password123")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(example = "ANALYST")
        @NotNull Role role,
        @Schema(example = "[\"SP\", \"DF\"]")
        Set<@ValidUf String> coverageUfs
) {

    public CreateUserRequest {
        coverageUfs = coverageUfs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(coverageUfs));
    }
}

