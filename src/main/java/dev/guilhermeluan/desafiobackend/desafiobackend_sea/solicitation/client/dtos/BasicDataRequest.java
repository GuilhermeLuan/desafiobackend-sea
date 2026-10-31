package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BasicDataRequest(
        @NotNull(message = "serviceType é obrigatório") ServiceType serviceType,
        @NotBlank(message = "title é obrigatório")
        @Size(min = 3, max = 80, message = "title deve ter entre 3 e 80 caracteres") String title,
        @NotBlank(message = "description é obrigatório")
        @Size(min = 20, max = 1000, message = "description deve ter entre 20 e 1000 caracteres") String description
) {

    public BasicDataRequest {
        title = title == null ? null : title.trim();
        description = description == null ? null : description.trim();
    }
}
