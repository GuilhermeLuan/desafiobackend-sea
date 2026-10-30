package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SolicitationStepsTest {

    private static final Long OWNER_ID = 10L;
    private static final Long ANALYST_ID = 20L;
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    private static final Address ADDRESS = new Address(
            "01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP", "1578", "Sala 12");

    private static Solicitation saveStep1(Solicitation solicitation) {
        solicitation.saveBasicData(ServiceType.INSTALLATION, "Instalar ar-condicionado",
                "Instalação de um split na sala de estar do apartamento");
        return solicitation;
    }

    private static Solicitation saveStep2(Solicitation solicitation) {
        solicitation.saveAddress(ADDRESS);
        return solicitation;
    }

    private static Solicitation saveStep3(Solicitation solicitation) {
        solicitation.saveConfirmation(Priority.MEDIUM, TOMORROW, new BigDecimal("250.00"), true);
        return solicitation;
    }

    private static Solicitation filledDraft() {
        return saveStep3(saveStep2(saveStep1(Solicitation.draft(OWNER_ID))));
    }

    /** Solicitação com as três etapas preenchidas, levada ao status pedido por transições válidas. */
    private static Solicitation filledIn(Status status) {
        Solicitation solicitation = filledDraft();

        switch (status) {
            case DRAFT -> { }
            case SUBMITTED -> solicitation.submit();
            case IN_REVIEW -> {
                solicitation.submit();
                solicitation.start();
            }
            case APPROVED -> {
                solicitation.submit();
                solicitation.decide(Decision.APPROVE, ANALYST_ID, "Documentação conferida e aprovada");
            }
            case REJECTED -> {
                solicitation.submit();
                solicitation.decide(Decision.REJECT, ANALYST_ID, "Endereço fora da área de atendimento");
            }
        }

        return solicitation;
    }

    // estado inicial

    @Test
    void newDraftStartsAtStepOneWithNoData() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        assertEquals(1, solicitation.getCurrentStep());
        assertNull(solicitation.getServiceType());
        assertNull(solicitation.getTitle());
        assertNull(solicitation.getDescription());
        assertNull(solicitation.getCep());
        assertNull(solicitation.getState());
        assertNull(solicitation.getPriority());
        assertNull(solicitation.getPreferredDate());
        assertNull(solicitation.getEstimatedValue());
        assertFalse(solicitation.isTermsAccepted());
    }

    // etapa 1: dados básicos

    @Test
    void saveBasicDataStoresTheFieldsAndMovesToStepTwo() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        solicitation.saveBasicData(ServiceType.MAINTENANCE, "Revisar elevador", "Revisão preventiva do elevador social do prédio");

        assertEquals(ServiceType.MAINTENANCE, solicitation.getServiceType());
        assertEquals("Revisar elevador", solicitation.getTitle());
        assertEquals("Revisão preventiva do elevador social do prédio", solicitation.getDescription());
        assertEquals(2, solicitation.getCurrentStep());
    }

    @Test
    void saveBasicDataTrimsTitleAndDescription() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        solicitation.saveBasicData(ServiceType.INSPECTION, "  Vistoria anual  ", "  Vistoria anual das instalações elétricas  ");

        assertEquals("Vistoria anual", solicitation.getTitle());
        assertEquals("Vistoria anual das instalações elétricas", solicitation.getDescription());
    }

    // etapa 2: endereço

    @Test
    void saveAddressStoresTheResolvedAddressAndMovesToStepThree() {
        Solicitation solicitation = saveStep1(Solicitation.draft(OWNER_ID));

        solicitation.saveAddress(ADDRESS);

        assertEquals("01310100", solicitation.getCep());
        assertEquals("Avenida Paulista", solicitation.getStreet());
        assertEquals("Bela Vista", solicitation.getNeighborhood());
        assertEquals("São Paulo", solicitation.getCity());
        assertEquals("SP", solicitation.getState());
        assertEquals("1578", solicitation.getNumber());
        assertEquals("Sala 12", solicitation.getComplement());
        assertEquals(3, solicitation.getCurrentStep());
    }

    @Test
    void complementIsOptional() {
        Solicitation solicitation = saveStep1(Solicitation.draft(OWNER_ID));

        solicitation.saveAddress(new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP", "12A", null));

        assertEquals("12A", solicitation.getNumber());
        assertNull(solicitation.getComplement());
        assertEquals(3, solicitation.getCurrentStep());
    }

    @Test
    void saveAddressRequiresStepOneFirstAndChangesNothing() {
        Solicitation solicitation = Solicitation.draft(OWNER_ID);

        assertThrows(InvalidSolicitationStateException.class, () -> solicitation.saveAddress(ADDRESS));

        assertNull(solicitation.getCep());
        assertNull(solicitation.getState());
        assertEquals(1, solicitation.getCurrentStep());
    }

    // etapa 3: confirmação

    @Test
    void saveConfirmationStoresTheFieldsAndStaysAtStepThree() {
        Solicitation solicitation = saveStep2(saveStep1(Solicitation.draft(OWNER_ID)));

        solicitation.saveConfirmation(Priority.LOW, TOMORROW, new BigDecimal("80.50"), true);

        assertEquals(Priority.LOW, solicitation.getPriority());
        assertEquals(TOMORROW, solicitation.getPreferredDate());
        assertEquals(new BigDecimal("80.50"), solicitation.getEstimatedValue());
        assertTrue(solicitation.isTermsAccepted());
        assertEquals(3, solicitation.getCurrentStep());
    }

    @Test
    void saveConfirmationRequiresStepTwoFirstAndChangesNothing() {
        Solicitation solicitation = saveStep1(Solicitation.draft(OWNER_ID));

        assertThrows(InvalidSolicitationStateException.class,
                () -> solicitation.saveConfirmation(Priority.LOW, TOMORROW, BigDecimal.TEN, true));

        assertNull(solicitation.getPriority());
        assertNull(solicitation.getEstimatedValue());
        assertFalse(solicitation.isTermsAccepted());
        assertEquals(2, solicitation.getCurrentStep());
    }

    // etapa 3: regra cruzada, prioridade HIGH exige valor estimado >= 100

    @Test
    void highPriorityBelowOneHundredIsRejectedAndChangesNothing() {
        Solicitation solicitation = saveStep2(saveStep1(Solicitation.draft(OWNER_ID)));

        assertThrows(InvalidSolicitationDataException.class,
                () -> solicitation.saveConfirmation(Priority.HIGH, TOMORROW, new BigDecimal("99.99"), true));

        assertNull(solicitation.getPriority());
        assertNull(solicitation.getEstimatedValue());
        assertEquals(3, solicitation.getCurrentStep());
    }

    @Test
    void highPriorityWithoutEstimatedValueIsRejected() {
        Solicitation solicitation = saveStep2(saveStep1(Solicitation.draft(OWNER_ID)));

        assertThrows(InvalidSolicitationDataException.class,
                () -> solicitation.saveConfirmation(Priority.HIGH, TOMORROW, null, true));
    }

    @Test
    void highPriorityAtOrAboveOneHundredIsAccepted() {
        Solicitation exactly = saveStep2(saveStep1(Solicitation.draft(OWNER_ID)));
        Solicitation above = saveStep2(saveStep1(Solicitation.draft(OWNER_ID)));

        assertDoesNotThrow(() -> exactly.saveConfirmation(Priority.HIGH, TOMORROW, new BigDecimal("100"), true));
        assertDoesNotThrow(() -> above.saveConfirmation(Priority.HIGH, TOMORROW, new BigDecimal("100.01"), true));

        assertEquals(Priority.HIGH, exactly.getPriority());
        assertEquals(Priority.HIGH, above.getPriority());
    }

    @ParameterizedTest
    @EnumSource(value = Priority.class, names = "HIGH", mode = EnumSource.Mode.EXCLUDE)
    void otherPrioritiesAcceptAnyNonNegativeValueIncludingZero(Priority priority) {
        Solicitation solicitation = saveStep2(saveStep1(Solicitation.draft(OWNER_ID)));

        assertDoesNotThrow(() -> solicitation.saveConfirmation(priority, TOMORROW, BigDecimal.ZERO, true));

        assertEquals(priority, solicitation.getPriority());
    }

    // reedição e currentStep

    @Test
    void resavingAnEarlierStepUpdatesTheDataButNeverMovesCurrentStepBack() {
        Solicitation solicitation = filledDraft();

        solicitation.saveBasicData(ServiceType.INSPECTION, "Novo título", "Descrição atualizada da solicitação de vistoria");

        assertEquals(ServiceType.INSPECTION, solicitation.getServiceType());
        assertEquals("Novo título", solicitation.getTitle());
        assertEquals(3, solicitation.getCurrentStep());
        assertEquals("SP", solicitation.getState());
        assertEquals(Priority.MEDIUM, solicitation.getPriority());
    }

    @Test
    void resavingTheAddressReplacesItIncludingTheUf() {
        Solicitation solicitation = filledDraft();

        solicitation.saveAddress(new Address("20040020", "Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ", "10", null));

        assertEquals("RJ", solicitation.getState());
        assertEquals("Rio de Janeiro", solicitation.getCity());
        assertNull(solicitation.getComplement());
        assertEquals(3, solicitation.getCurrentStep());
    }

    // edição só em DRAFT

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void saveBasicDataIsRejectedOutsideDraftAndChangesNothing(Status status) {
        Solicitation solicitation = filledIn(status);
        String titleBefore = solicitation.getTitle();

        assertThrows(InvalidSolicitationStateException.class,
                () -> solicitation.saveBasicData(ServiceType.INSPECTION, "Outro título", "Outra descrição com mais de vinte caracteres"));

        assertEquals(titleBefore, solicitation.getTitle());
        assertEquals(status, solicitation.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void saveAddressIsRejectedOutsideDraftAndChangesNothing(Status status) {
        Solicitation solicitation = filledIn(status);

        assertThrows(InvalidSolicitationStateException.class,
                () -> solicitation.saveAddress(new Address("20040020", "Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ", "10", null)));

        assertEquals("SP", solicitation.getState());
        assertEquals("Avenida Paulista", solicitation.getStreet());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
    void saveConfirmationIsRejectedOutsideDraftAndChangesNothing(Status status) {
        Solicitation solicitation = filledIn(status);

        assertThrows(InvalidSolicitationStateException.class,
                () -> solicitation.saveConfirmation(Priority.LOW, TOMORROW, BigDecimal.ONE, true));

        assertEquals(Priority.MEDIUM, solicitation.getPriority());
        assertEquals(new BigDecimal("250.00"), solicitation.getEstimatedValue());
    }
}
