package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.ValidUf;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record UpdateCoverageRequest(
        Set<@ValidUf String> coverageUfs
) {

    public UpdateCoverageRequest {
        coverageUfs = coverageUfs == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(coverageUfs));
    }
}
