package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitationStateMachineTest {

    private static final Long OWNER_ID = 10L;
    private static final Long ANALYST_ID = 20L;
    private static final String COMMENT = "Documentação conferida e aprovada";

    /** Leva uma solicitação nova até o status pedido, usando só transições válidas. */
    private static Solicitation solicitationIn(Status status) {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        switch (status) {
            case DRAFT -> { }
            case SUBMITTED -> solicitation.submit();
            case IN_REVIEW -> {
                solicitation.submit();
                solicitation.start();
            }
            case APPROVED -> {
                solicitation.submit();
                solicitation.decide(Decision.APPROVE, ANALYST_ID, COMMENT);
            }
            case REJECTED -> {
                solicitation.submit();
                solicitation.decide(Decision.REJECT, ANALYST_ID, COMMENT);
            }
        }

        return solicitation;
    }

    @Test
    void newSolicitationIsADraftOwnedByTheClient() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        assertEquals(Status.DRAFT, solicitation.getStatus());
        assertEquals(OWNER_ID, solicitation.getOwnerId());
    }

    // submit: DRAFT -> SUBMITTED

    @Test
    void submitMovesDraftToSubmitted() {
        Solicitation solicitation = solicitationIn(Status.DRAFT);

        solicitation.submit();

        assertEquals(Status.SUBMITTED, solicitation.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void submitIsRejectedOutsideDraftAndKeepsTheStatus(Status status) {
        Solicitation solicitation = solicitationIn(status);

        assertThrows(InvalidSolicitationStateException.class, solicitation::submit);

        assertEquals(status, solicitation.getStatus());
    }

    // start: SUBMITTED -> IN_REVIEW

    @Test
    void startMovesSubmittedToInReview() {
        Solicitation solicitation = solicitationIn(Status.SUBMITTED);

        solicitation.start();

        assertEquals(Status.IN_REVIEW, solicitation.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "SUBMITTED", mode = EnumSource.Mode.EXCLUDE)
    void startIsRejectedOutsideSubmittedAndKeepsTheStatus(Status status) {
        Solicitation solicitation = solicitationIn(status);

        assertThrows(InvalidSolicitationStateException.class, solicitation::start);

        assertEquals(status, solicitation.getStatus());
    }

    // decide: SUBMITTED | IN_REVIEW -> APPROVED | REJECTED

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"SUBMITTED", "IN_REVIEW"})
    void approveIsAllowedFromSubmittedAndInReview(Status status) {
        Solicitation solicitation = solicitationIn(status);

        solicitation.decide(Decision.APPROVE, ANALYST_ID, COMMENT);

        assertEquals(Status.APPROVED, solicitation.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"SUBMITTED", "IN_REVIEW"})
    void rejectIsAllowedFromSubmittedAndInReview(Status status) {
        Solicitation solicitation = solicitationIn(status);

        solicitation.decide(Decision.REJECT, ANALYST_ID, COMMENT);

        assertEquals(Status.REJECTED, solicitation.getStatus());
    }

    @Test
    void decideRecordsWhoDecidedAndWhy() {
        Solicitation solicitation = solicitationIn(Status.IN_REVIEW);

        solicitation.decide(Decision.REJECT, ANALYST_ID, "Endereço fora da área de atendimento");

        assertEquals(ANALYST_ID, solicitation.getDecidedBy());
        assertEquals("Endereço fora da área de atendimento", solicitation.getDecisionComment());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"DRAFT", "APPROVED", "REJECTED"})
    void decideIsRejectedFromDraftAndFinalStatuses(Status status) {
        Solicitation solicitation = solicitationIn(status);
        Long decidedByBefore = solicitation.getDecidedBy();
        String commentBefore = solicitation.getDecisionComment();

        assertThrows(InvalidSolicitationStateException.class,
                () -> solicitation.decide(Decision.APPROVE, 99L, "Tentando decidir de novo"));
        assertThrows(InvalidSolicitationStateException.class,
                () -> solicitation.decide(Decision.REJECT, 99L, "Tentando decidir de novo"));

        assertEquals(status, solicitation.getStatus());
        assertEquals(decidedByBefore, solicitation.getDecidedBy());
        assertEquals(commentBefore, solicitation.getDecisionComment());
    }

    @Test
    void draftHasNoDecisionData() {
        Solicitation solicitation = solicitationIn(Status.DRAFT);

        assertNull(solicitation.getDecidedBy());
        assertNull(solicitation.getDecisionComment());
    }

    // decide: argumentos obrigatórios

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"SUBMITTED", "IN_REVIEW"})
    void decideWithoutDecisionIsRejectedAndChangesNothing(Status status) {
        Solicitation solicitation = solicitationIn(status);

        assertThrows(NullPointerException.class, () -> solicitation.decide(null, ANALYST_ID, COMMENT));

        assertUntouchedBy(solicitation, status);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"SUBMITTED", "IN_REVIEW"})
    void decideWithoutAnalystIsRejectedAndChangesNothing(Status status) {
        Solicitation solicitation = solicitationIn(status);

        assertThrows(NullPointerException.class, () -> solicitation.decide(Decision.APPROVE, null, COMMENT));

        assertUntouchedBy(solicitation, status);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"SUBMITTED", "IN_REVIEW"})
    void decideWithoutCommentIsRejectedAndChangesNothing(Status status) {
        Solicitation solicitation = solicitationIn(status);

        assertThrows(NullPointerException.class, () -> solicitation.decide(Decision.REJECT, ANALYST_ID, null));

        assertUntouchedBy(solicitation, status);
    }

    private static void assertUntouchedBy(Solicitation solicitation, Status expectedStatus) {
        assertEquals(expectedStatus, solicitation.getStatus());
        assertNull(solicitation.getDecidedBy());
        assertNull(solicitation.getDecisionComment());
    }

    // edição: só em DRAFT

    @Test
    void draftIsEditable() {
        Solicitation solicitation = solicitationIn(Status.DRAFT);

        assertDoesNotThrow(solicitation::assertEditable);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void nothingButDraftIsEditable(Status status) {
        Solicitation solicitation = solicitationIn(status);

        assertThrows(InvalidSolicitationStateException.class, solicitation::assertEditable);
    }

    // caminhos completos

    @Test
    void fullPathThroughReviewEndsApproved() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        solicitation.submit();
        solicitation.start();
        solicitation.decide(Decision.APPROVE, ANALYST_ID, COMMENT);

        assertEquals(Status.APPROVED, solicitation.getStatus());
    }

    @Test
    void startIsOptionalSoAnalystCanDecideStraightFromSubmitted() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        solicitation.submit();
        solicitation.decide(Decision.REJECT, ANALYST_ID, COMMENT);

        assertEquals(Status.REJECTED, solicitation.getStatus());
    }
}
