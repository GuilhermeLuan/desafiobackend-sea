package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Só o que o cliente informa; rua, bairro, cidade e UF vêm do ViaCEP.
 */
public record AddressRequest(
        @Schema(description = "CEP com 8 dígitos (o hífen é opcional)", example = "01310-100")
        @NotBlank(message = "cep é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "cep deve ter 8 dígitos") String cep,
        @NotBlank(message = "number é obrigatório")
        @Size(max = 20, message = "number deve ter no máximo 20 caracteres") String number,
        @Size(max = 255, message = "complement deve ter no máximo 255 caracteres") String complement
) {

    public AddressRequest {
        cep = cep == null ? null : cep.trim().replace("-", "");
        number = number == null ? null : number.trim();
        complement = complement == null || complement.isBlank() ? null : complement.trim();
    }
}
