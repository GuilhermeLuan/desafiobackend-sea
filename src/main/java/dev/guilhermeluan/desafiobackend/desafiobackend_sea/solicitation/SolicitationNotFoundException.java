package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

public class SolicitationNotFoundException extends RuntimeException {

    public SolicitationNotFoundException(Long id) {
        super("Solicitation not found: " + id);
    }
}
