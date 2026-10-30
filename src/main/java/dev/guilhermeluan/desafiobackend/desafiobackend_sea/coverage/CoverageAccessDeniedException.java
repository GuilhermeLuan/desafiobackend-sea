package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

public class CoverageAccessDeniedException extends RuntimeException {

    public CoverageAccessDeniedException(String uf) {
        super("User does not cover UF: " + uf);
    }
}
