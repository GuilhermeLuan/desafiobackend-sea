package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.ValidUf;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record UpdateCoverageRequest(
        @Schema(example = "[\"SP\", \"DF\"]")
        Set<@ValidUf String> coverageUfs
) {

    public UpdateCoverageRequest {
        coverageUfs = coverageUfs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(coverageUfs));
    }
}
