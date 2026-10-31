package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.Decision;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecideRequestValidationTest {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static boolean valid(Object dto) {
        return validator.validate(dto).isEmpty();
    }

    @Test
    void decideAcceptsAValidDecisionAndComment() {
        assertTrue(valid(new DecideRequest(Decision.APPROVE, "Documentação conferida")));
    }

    @Test
    void decideRequiresTheDecision() {
        assertEquals(false, valid(new DecideRequest(null, "Documentação conferida")));
    }

    @Test
    void commentMustHaveBetweenTenAndOneThousandCharactersAfterTrim() {
        assertEquals(false, valid(new DecideRequest(Decision.REJECT, null)));
        assertEquals(false, valid(new DecideRequest(Decision.REJECT, "   ")));
        assertEquals(false, valid(new DecideRequest(Decision.REJECT, "a".repeat(9))));
        assertEquals(false, valid(new DecideRequest(Decision.REJECT, "  " + "a".repeat(9) + "  ")));
        assertEquals(false, valid(new DecideRequest(Decision.REJECT, "a".repeat(1001))));
        assertTrue(valid(new DecideRequest(Decision.REJECT, "a".repeat(10))));
        assertTrue(valid(new DecideRequest(Decision.REJECT, "a".repeat(1000))));
    }
}
