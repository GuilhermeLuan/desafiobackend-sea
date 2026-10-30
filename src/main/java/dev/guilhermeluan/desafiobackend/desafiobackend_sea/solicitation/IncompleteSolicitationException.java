package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import java.util.List;

/**
 * Submit de um rascunho que ainda tem etapas ou campos obrigatórios por preencher.
 */
public class IncompleteSolicitationException extends RuntimeException {

    private final List<Integer> missingSteps;
    private final List<String> missingFields;

    public IncompleteSolicitationException(List<Integer> missingSteps, List<String> missingFields) {
        super("Solicitation is incomplete. Missing steps: " + missingSteps + ", missing fields: " + missingFields);
        this.missingSteps = List.copyOf(missingSteps);
        this.missingFields = List.copyOf(missingFields);
    }

    public List<Integer> getMissingSteps() {
        return missingSteps;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
