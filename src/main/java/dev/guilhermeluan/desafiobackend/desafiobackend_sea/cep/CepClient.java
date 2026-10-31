package dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep;

import java.util.Optional;

public interface CepClient {

    Optional<CepResult> findByCep(String cep);
}
