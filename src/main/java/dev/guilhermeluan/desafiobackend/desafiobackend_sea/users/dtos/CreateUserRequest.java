package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.ValidUf;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Set<@ValidUf String> coverageUfs
) {

    public CreateUserRequest {
        coverageUfs = coverageUfs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(coverageUfs));
    }
}

