package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

public record Address(
        String cep,
        String street,
        String neighborhood,
        String city,
        String state,
        String number,
        String complement
) {
}
