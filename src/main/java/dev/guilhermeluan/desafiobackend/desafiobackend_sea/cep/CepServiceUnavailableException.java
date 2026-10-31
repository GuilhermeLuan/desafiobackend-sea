package dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep;

public class CepServiceUnavailableException extends RuntimeException {

    public CepServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
