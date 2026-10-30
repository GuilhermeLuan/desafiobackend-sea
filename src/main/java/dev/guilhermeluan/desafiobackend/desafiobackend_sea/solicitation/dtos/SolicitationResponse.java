package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.ServiceType;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Solicitation;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SolicitationResponse(
        Long id,
        Long ownerId,
        Status status,
        int currentStep,
        ServiceType serviceType,
        String title,
        String description,
        String cep,
        String street,
        String neighborhood,
        String city,
        String state,
        String number,
        String complement,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        boolean termsAccepted,
        Long decidedBy,
        String decisionComment,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt
) {

    public static SolicitationResponse from(Solicitation s) {
        return new SolicitationResponse(
                s.getId(), s.getOwnerId(), s.getStatus(), s.getCurrentStep(),
                s.getServiceType(), s.getTitle(), s.getDescription(),
                s.getCep(), s.getStreet(), s.getNeighborhood(), s.getCity(), s.getState(), s.getNumber(), s.getComplement(),
                s.getPriority(), s.getPreferredDate(), s.getEstimatedValue(), s.isTermsAccepted(),
                s.getDecidedBy(), s.getDecisionComment(),
                s.getCreatedAt(), s.getUpdatedAt(), s.getSubmittedAt());
    }
}
