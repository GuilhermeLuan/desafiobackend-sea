package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import java.util.Optional;

public interface CepClient {

    Optional<CepResult> findByCep(String cep);
}
