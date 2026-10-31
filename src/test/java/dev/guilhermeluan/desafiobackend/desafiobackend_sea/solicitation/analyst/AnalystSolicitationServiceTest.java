package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageAccessDeniedException;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage.CoverageGuard;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.*;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalystSolicitationServiceTest {

    private static final String ANALYST_EMAIL = "analyst@example.com";
    private static final Long ANALYST_ID = 20L;
    private static final Long OWNER_ID = 10L;
    private static final Long ID = 1L;
    private static final String COMMENT = "Documentação conferida e aprovada";

    @Mock
    private UserRepository userRepository;
    @Mock
    private SolicitationRepository solicitationRepository;

    private AnalystSolicitationService service;

    @BeforeEach
    void setUp() {
        // o analista cobre RJ e SP; o guard é o real, só o acesso ao banco é simulado
        lenient().when(userRepository.findCoverageUfsByEmail(ANALYST_EMAIL)).thenReturn(Set.of("RJ", "SP"));
        service = new AnalystSolicitationService(solicitationRepository, new CoverageGuard(userRepository));
    }

    /**
     * Solicitação completa na UF pedida, ainda em DRAFT, disponível no repositório com o id {@code ID}.
     */
    private Solicitation draftIn(String uf) {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);
        solicitation.saveBasicData(ServiceType.INSTALLATION, "Instalar ar-condicionado",
                "Instalação de um split na sala de estar do apartamento");
        solicitation.saveAddress(new Address("22785275", "Estrada dos Bandeirantes", "Vargem Grande",
                "Rio de Janeiro", uf, "25997", null));
        solicitation.saveConfirmation(Priority.MEDIUM, LocalDate.now().plusDays(1), new BigDecimal("250.00"), true);
        ReflectionTestUtils.setField(solicitation, "id", ID);
        when(solicitationRepository.findById(ID)).thenReturn(Optional.of(solicitation));
        return solicitation;
    }

    private Solicitation submittedIn(String uf) {
        Solicitation solicitation = draftIn(uf);
        solicitation.submit();
        return solicitation;
    }

    private Solicitation inReviewIn(String uf) {
        Solicitation solicitation = submittedIn(uf);
        solicitation.start();
        return solicitation;
    }

    // get

    @Test
    void getReturnsASubmittedSolicitationInACoveredUf() {
        Solicitation solicitation = submittedIn("RJ");

        assertSame(solicitation, service.get(ANALYST_EMAIL, ID));
    }

    @Test
    void getIsDeniedForAnUfOutsideTheCoverage() {
        submittedIn("MG");

        assertThrows(CoverageAccessDeniedException.class, () -> service.get(ANALYST_EMAIL, ID));
    }

    @Test
    void getFailsForAnIdThatDoesNotExist() {
        when(solicitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitationNotFoundException.class, () -> service.get(ANALYST_EMAIL, 99L));
    }

    @Test
    void getHidesDraftsEvenInACoveredUf() {
        draftIn("RJ");

        assertThrows(SolicitationNotFoundException.class, () -> service.get(ANALYST_EMAIL, ID));
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void getShowsEverySubmittedStatus(Status status) {
        Solicitation solicitation = submittedIn("SP");
        switch (status) {
            case SUBMITTED -> {
            }
            case IN_REVIEW -> solicitation.start();
            case APPROVED -> solicitation.decide(Decision.APPROVE, ANALYST_ID, COMMENT);
            case REJECTED -> solicitation.decide(Decision.REJECT, ANALYST_ID, COMMENT);
            default -> throw new IllegalStateException(status.name());
        }

        assertEquals(status, service.get(ANALYST_EMAIL, ID).getStatus());
    }

    // start

    @Test
    void startMovesASubmittedSolicitationToInReview() {
        submittedIn("RJ");

        assertEquals(Status.IN_REVIEW, service.start(ANALYST_EMAIL, ID).getStatus());
    }

    @Test
    void startIsDeniedForAnUfOutsideTheCoverageAndChangesNothing() {
        Solicitation solicitation = submittedIn("MG");

        assertThrows(CoverageAccessDeniedException.class, () -> service.start(ANALYST_EMAIL, ID));

        assertEquals(Status.SUBMITTED, solicitation.getStatus());
    }

    @Test
    void startHidesDrafts() {
        draftIn("RJ");

        assertThrows(SolicitationNotFoundException.class, () -> service.start(ANALYST_EMAIL, ID));
    }

    @Test
    void startFailsForAnIdThatDoesNotExist() {
        when(solicitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitationNotFoundException.class, () -> service.start(ANALYST_EMAIL, 99L));
    }

    @Test
    void startTwiceIsRejected() {
        Solicitation solicitation = inReviewIn("RJ");

        assertThrows(InvalidSolicitationStateException.class, () -> service.start(ANALYST_EMAIL, ID));

        assertEquals(Status.IN_REVIEW, solicitation.getStatus());
    }

    // decide

    @ParameterizedTest
    @EnumSource(Decision.class)
    void decideRecordsTheDecisionTheAnalystAndTheComment(Decision decision) {
        submittedIn("SP");

        Solicitation result = service.decide(ANALYST_EMAIL, ANALYST_ID, ID, decision, COMMENT);

        assertEquals(decision == Decision.APPROVE ? Status.APPROVED : Status.REJECTED, result.getStatus());
        assertEquals(ANALYST_ID, result.getDecidedBy());
        assertEquals(COMMENT, result.getDecisionComment());
    }

    @Test
    void decideIsAllowedFromInReview() {
        inReviewIn("RJ");

        assertEquals(Status.REJECTED,
                service.decide(ANALYST_EMAIL, ANALYST_ID, ID, Decision.REJECT, COMMENT).getStatus());
    }

    @Test
    void decideIsDeniedForAnUfOutsideTheCoverageAndChangesNothing() {
        Solicitation solicitation = submittedIn("MG");

        assertThrows(CoverageAccessDeniedException.class,
                () -> service.decide(ANALYST_EMAIL, ANALYST_ID, ID, Decision.APPROVE, COMMENT));

        assertEquals(Status.SUBMITTED, solicitation.getStatus());
        assertNull(solicitation.getDecidedBy());
        assertNull(solicitation.getDecisionComment());
    }

    @Test
    void decideHidesDraftsAndLeavesThemUntouched() {
        Solicitation solicitation = draftIn("RJ");

        assertThrows(SolicitationNotFoundException.class,
                () -> service.decide(ANALYST_EMAIL, ANALYST_ID, ID, Decision.APPROVE, COMMENT));

        assertEquals(Status.DRAFT, solicitation.getStatus());
    }

    @Test
    void decideFailsForAnIdThatDoesNotExist() {
        when(solicitationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitationNotFoundException.class,
                () -> service.decide(ANALYST_EMAIL, ANALYST_ID, 99L, Decision.APPROVE, COMMENT));
    }

    @Test
    void decidingAnAlreadyDecidedSolicitationIsRejectedAndKeepsTheFirstDecision() {
        Solicitation solicitation = submittedIn("RJ");
        solicitation.decide(Decision.APPROVE, ANALYST_ID, COMMENT);

        assertThrows(InvalidSolicitationStateException.class,
                () -> service.decide(ANALYST_EMAIL, 99L, ID, Decision.REJECT, "Mudei de ideia sobre isso"));

        assertEquals(Status.APPROVED, solicitation.getStatus());
        assertEquals(ANALYST_ID, solicitation.getDecidedBy());
        assertEquals(COMMENT, solicitation.getDecisionComment());
    }
}
