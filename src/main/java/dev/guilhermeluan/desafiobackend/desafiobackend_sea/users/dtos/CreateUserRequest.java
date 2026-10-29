package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Set<@Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                message = "UF inválida: use a sigla de duas letras de um estado brasileiro")
        String> coverageUfs
) {

    public CreateUserRequest {
        coverageUfs = coverageUfs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(coverageUfs));
    }
}

