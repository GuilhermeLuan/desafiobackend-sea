package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Priority;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfirmationRequest(
        @NotNull(message = "priority é obrigatório") Priority priority,
        @FutureOrPresent(message = "preferredDate não pode ser uma data passada") LocalDate preferredDate,
        @NotNull(message = "estimatedValue é obrigatório")
        @PositiveOrZero(message = "estimatedValue não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "estimatedValue aceita até 10 dígitos inteiros e 2 decimais") BigDecimal estimatedValue,
        @AssertTrue(message = "os termos devem ser aceitos") boolean termsAccepted
) {
}
