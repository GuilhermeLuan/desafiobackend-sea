package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.ValidUf;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.ServiceType;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Status;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record SolicitationSearchParams(
        String q,
        Status status,
        ServiceType serviceType,
        Priority priority,
        @ValidUf String state,
        LocalDate dateFrom,
        LocalDate dateTo,
        @Min(value = 0, message = "page deve ser maior ou igual a 0") Integer page,
        @Min(value = 1, message = "size deve ser no mínimo 1")
        @Max(value = 100, message = "size deve ser no máximo 100") Integer size,
        @Pattern(regexp = "^(submittedAt|createdAt|estimatedValue),(asc|desc)$",
                message = "sort deve ser <campo>,<asc|desc> com campo em submittedAt, createdAt ou estimatedValue") String sort
) {

    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT = "submittedAt,desc";

    public SolicitationSearchParams {
        q = q == null || q.isBlank() ? null : q.trim();
        page = page == null ? 0 : page;
        size = size == null ? DEFAULT_SIZE : size;
        sort = sort == null || sort.isBlank() ? DEFAULT_SORT : sort.trim();
    }

    public String sortField() {
        return sort.split(",")[0];
    }

    public boolean ascending() {
        return sort.endsWith(",asc");
    }
}
