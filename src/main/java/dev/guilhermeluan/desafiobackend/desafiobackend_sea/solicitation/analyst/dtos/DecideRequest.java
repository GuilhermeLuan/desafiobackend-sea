package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideRequest(
        @NotNull(message = "decision é obrigatório") Decision decision,
        @NotBlank(message = "comment é obrigatório")
        @Size(min = 10, max = 1000, message = "comment deve ter entre 10 e 1000 caracteres") String comment
) {

    public DecideRequest {
        comment = comment == null ? null : comment.trim();
    }
}
