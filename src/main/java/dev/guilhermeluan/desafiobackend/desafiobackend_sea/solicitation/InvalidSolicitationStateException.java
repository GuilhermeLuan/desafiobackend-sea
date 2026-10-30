package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

public class InvalidSolicitationStateException extends RuntimeException {

    public InvalidSolicitationStateException(String message) {
        super(message);
    }
}
