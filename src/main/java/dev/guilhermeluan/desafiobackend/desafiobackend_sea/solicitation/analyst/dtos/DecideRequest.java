package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideRequest(
        @Schema(example = "APPROVE")
        @NotNull(message = "decision é obrigatório") Decision decision,
        @Schema(description = "Justificativa da decisão, de 10 a 1000 caracteres", example = "Documentação conferida e aprovada")
        @NotBlank(message = "comment é obrigatório")
        @Size(min = 10, max = 1000, message = "comment deve ter entre 10 e 1000 caracteres") String comment
) {

    public DecideRequest {
        comment = comment == null ? null : comment.trim();
    }
}
