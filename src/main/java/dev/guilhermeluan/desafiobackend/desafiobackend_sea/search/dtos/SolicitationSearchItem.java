package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.SolicitationDocument;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.ServiceType;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Status;

import java.math.BigDecimal;
import java.time.Instant;

public record SolicitationSearchItem(
        Long id,
        String title,
        Status status,
        ServiceType serviceType,
        Priority priority,
        String state,
        String city,
        BigDecimal estimatedValue,
        Instant createdAt,
        Instant submittedAt
) {

    public static SolicitationSearchItem from(SolicitationDocument document) {
        return new SolicitationSearchItem(document.getId(), document.getTitle(), document.getStatus(),
                document.getServiceType(), document.getPriority(), document.getState(), document.getCity(),
                document.getEstimatedValue(), document.getCreatedAt(), document.getSubmittedAt());
    }
}
