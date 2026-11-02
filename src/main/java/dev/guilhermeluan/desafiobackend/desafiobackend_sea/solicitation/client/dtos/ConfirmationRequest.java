package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmationRequest(
        @Schema(example = "MEDIUM")
        @NotNull(message = "priority é obrigatório") Priority priority,
        @Schema(example = "2030-12-31")
        @FutureOrPresent(message = "preferredDate não pode ser uma data passada") LocalDate preferredDate,
        @Schema(description = "Obrigatório e maior ou igual a 0; precisa ser pelo menos 100 quando a prioridade é HIGH", example = "250.00")
        @NotNull(message = "estimatedValue é obrigatório")
        @PositiveOrZero(message = "estimatedValue não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "estimatedValue aceita até 10 dígitos inteiros e 2 decimais") BigDecimal estimatedValue,
        @Schema(example = "true")
        @AssertTrue(message = "os termos devem ser aceitos") boolean termsAccepted
) {
}
