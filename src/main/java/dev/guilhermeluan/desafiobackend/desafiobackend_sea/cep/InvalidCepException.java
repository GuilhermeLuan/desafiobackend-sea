package dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep;

public class InvalidCepException extends RuntimeException {

    public InvalidCepException(String cep) {
        super("CEP not found: " + cep);
    }
}
